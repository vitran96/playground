package lab;

import lab.account.Account;
import lab.account.AccountRepository;
import lab.account.AccountService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class Experiment4DeadlockTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepo;

    @Autowired
    private PlatformTransactionManager txManager;

    @Test
    @DisplayName("Exp 4A: opposite concurrent transfers cause deadlock and Postgres aborts one transaction")
    void oppositeTransfersDeadlock() throws Exception {
        accountRepo.save(new Account(1L, 1000));
        accountRepo.save(new Account(2L, 1000));

        var pool = Executors.newFixedThreadPool(2);
        var startGun = new CountDownLatch(1);
        var t1Error = new AtomicReference<Throwable>();
        var t2Error = new AtomicReference<Throwable>();

        // Thread 1: transfer 1 -> 2
        pool.submit(() -> {
            try {
                startGun.await();
                accountService.transfer(1L, 2L, 100);
            } catch (Throwable t) {
                t1Error.set(t);
            }
        });

        // Thread 2: transfer 2 -> 1
        pool.submit(() -> {
            try {
                startGun.await();
                accountService.transfer(2L, 1L, 100);
            } catch (Throwable t) {
                t2Error.set(t);
            }
        });

        startGun.countDown();
        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);

        // One must succeed and one must fail with deadlock (PessimisticLockingFailureException / 40P01)
        boolean hasDeadlock = (t1Error.get() != null && isLockFailure(t1Error.get())) ||
                              (t2Error.get() != null && isLockFailure(t2Error.get()));

        System.out.println("Exp 4A Deadlock reproduced -> T1 err: " + (t1Error.get() != null ? t1Error.get().getClass().getSimpleName() : "none")
                + ", T2 err: " + (t2Error.get() != null ? t2Error.get().getClass().getSimpleName() : "none"));

        assertThat(hasDeadlock).isTrue();
    }

    @Test
    @DisplayName("Exp 4A: ordered locking prevents deadlocks completely")
    void orderedTransfersNeverDeadlock() throws Exception {
        accountRepo.save(new Account(1L, 1000));
        accountRepo.save(new Account(2L, 1000));

        var pool = Executors.newFixedThreadPool(2);
        var startGun = new CountDownLatch(1);
        var t1Error = new AtomicReference<Throwable>();
        var t2Error = new AtomicReference<Throwable>();

        pool.submit(() -> {
            try {
                startGun.await();
                accountService.transferOrdered(1L, 2L, 100);
            } catch (Throwable t) {
                t1Error.set(t);
            }
        });

        pool.submit(() -> {
            try {
                startGun.await();
                accountService.transferOrdered(2L, 1L, 100);
            } catch (Throwable t) {
                t2Error.set(t);
            }
        });

        startGun.countDown();
        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);

        System.out.println("Exp 4A Ordered -> T1 err: " + t1Error.get() + ", T2 err: " + t2Error.get());

        assertThat(t1Error.get()).isNull();
        assertThat(t2Error.get()).isNull();

        Account a1 = accountRepo.findById(1L).orElseThrow();
        Account a2 = accountRepo.findById(2L).orElseThrow();
        assertThat(a1.getBalance()).isEqualTo(1000);
        assertThat(a2.getBalance()).isEqualTo(1000);
    }

    @Test
    @DisplayName("Exp 4B: NOWAIT fails immediately if row is locked")
    void nowaitFailsImmediatelyAgainstHeldRow() throws Exception {
        accountRepo.save(new Account(10L, 500));

        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        Thread holder = new Thread(() -> {
            new TransactionTemplate(txManager).executeWithoutResult(status -> {
                accountRepo.findByIdForUpdate(10L).orElseThrow();
                locked.countDown();
                try {
                    release.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException ignored) {}
            });
        });
        holder.start();

        locked.await();

        long start = System.currentTimeMillis();
        AtomicReference<Throwable> nowaitError = new AtomicReference<>();
        try {
            accountService.lockNowait(10L);
        } catch (Throwable t) {
            nowaitError.set(t);
        }
        long elapsed = System.currentTimeMillis() - start;

        release.countDown();
        holder.join();

        System.out.println("Exp 4B NOWAIT -> elapsed: " + elapsed + " ms, err: " + nowaitError.get());

        assertThat(nowaitError.get()).isNotNull();
        assertThat(isLockFailure(nowaitError.get())).isTrue();
        // NOWAIT fails immediately (< 200ms)
        assertThat(elapsed).isLessThan(500);
    }

    @Test
    @DisplayName("Exp 4B: lock_timeout waits at most N seconds before failing fast")
    void lockTimeoutFailsAfterSpecifiedDuration() throws Exception {
        accountRepo.save(new Account(20L, 500));

        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        Thread holder = new Thread(() -> {
            new TransactionTemplate(txManager).executeWithoutResult(status -> {
                accountRepo.findByIdForUpdate(20L).orElseThrow();
                locked.countDown();
                try {
                    release.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException ignored) {}
            });
        });
        holder.start();

        locked.await();

        long start = System.currentTimeMillis();
        AtomicReference<Throwable> timeoutError = new AtomicReference<>();
        try {
            // Wait at most 1 second
            accountService.lockWithTimeout(20L, 1);
        } catch (Throwable t) {
            timeoutError.set(t);
        }
        long elapsed = System.currentTimeMillis() - start;

        release.countDown();
        holder.join();

        System.out.println("Exp 4B lock_timeout=1s -> elapsed: " + elapsed + " ms, err: " + timeoutError.get());

        assertThat(timeoutError.get()).isNotNull();
        assertThat(isLockFailure(timeoutError.get())).isTrue();
        // Should have waited about 1 second (1000ms), failing well before 5 seconds
        assertThat(elapsed).isGreaterThanOrEqualTo(900);
        assertThat(elapsed).isLessThan(3000);
    }

    private static boolean isLockFailure(Throwable t) {
        if (t instanceof PessimisticLockingFailureException || t instanceof CannotAcquireLockException) {
            return true;
        }
        while (t != null) {
            String msg = t.getMessage() != null ? t.getMessage() : "";
            if (msg.contains("deadlock") || msg.contains("could not obtain lock") || msg.contains("lock timeout") || msg.contains("55P03") || msg.contains("40P01")) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }
}
