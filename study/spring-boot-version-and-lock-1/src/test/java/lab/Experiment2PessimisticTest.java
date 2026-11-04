package lab;

import jakarta.persistence.TransactionRequiredException;
import lab.product.Product;
import lab.product.ProductRepository;
import lab.product.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static lab.ConcurrentHelper.runConcurrently;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class Experiment2PessimisticTest {

    @Autowired
    private ProductService service;

    @Autowired
    private ProductRepository repo;

    @Autowired
    private PlatformTransactionManager txManager;

    @Test
    @DisplayName("Exp 2: pessimistic lock queues buyers, sells exactly 10, 10 get sold out, stock ends at 0")
    void pessimisticLockNeverOversells() throws Exception {
        Product product = repo.save(new Product("Pessimistic Mug", 10));
        long id = product.getId();

        long start = System.currentTimeMillis();
        var out = runConcurrently(20, () -> service.buyLocked(id));
        long elapsed = System.currentTimeMillis() - start;

        System.out.println("Exp 2 Pessimistic Lock -> ok: " + out.ok()
                + ", errors: " + out.errors().size()
                + ", elapsed: " + elapsed + " ms");

        assertThat(out.ok()).isEqualTo(10);
        assertThat(out.errors()).hasSize(10)
                .allMatch(e -> e.getMessage().equals("sold out"));
        assertThat(repo.findById(id).orElseThrow().getStock()).isZero();
        assertThat(elapsed).isGreaterThanOrEqualTo(400);
    }

    @Test
    @DisplayName("Exp 2: calling locked query outside transaction throws TransactionRequiredException")
    void callingLockWithoutTxThrowsException() {
        Product product = repo.save(new Product("NoTx Mug", 5));
        long id = product.getId();

        assertThatThrownBy(() -> service.buyLockedWithoutTx(id))
                .satisfies(e -> assertThat(e).matches(t ->
                        t instanceof TransactionRequiredException ||
                        t instanceof InvalidDataAccessApiUsageException ||
                        t.getCause() instanceof TransactionRequiredException));
    }

    @Test
    @DisplayName("Exp 2: held lock blocks concurrent buyer until the holding transaction commits")
    void heldLockBlocksConcurrentReaderUntilCommit() throws Exception {
        Product product = repo.save(new Product("Held Mug", 5));
        long id = product.getId();

        CountDownLatch lockAcquired = new CountDownLatch(1);
        CountDownLatch releaseLock = new CountDownLatch(1);
        AtomicBoolean secondThreadFinishedAfterRelease = new AtomicBoolean(false);

        // Thread 1: starts transaction, acquires lock, holds it
        Thread holder = new Thread(() -> {
            new TransactionTemplate(txManager).executeWithoutResult(status -> {
                repo.findByIdForUpdate(id).orElseThrow();
                lockAcquired.countDown();
                try {
                    releaseLock.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException ignored) {}
            });
        });
        holder.start();

        lockAcquired.await();

        // Thread 2: attempts to buy while Thread 1 holds lock
        Thread buyer = new Thread(() -> {
            service.buyLocked(id);
            // If releaseLock has already counted down, this buyer was properly blocked
            if (releaseLock.getCount() == 0) {
                secondThreadFinishedAfterRelease.set(true);
            }
        });
        buyer.start();

        // Ensure buyer thread is waiting on the lock
        Thread.sleep(200);
        assertThat(secondThreadFinishedAfterRelease.get()).isFalse();

        // Release the lock by letting Thread 1 commit
        releaseLock.countDown();
        holder.join();
        buyer.join();

        assertThat(secondThreadFinishedAfterRelease.get()).isTrue();
        assertThat(repo.findById(id).orElseThrow().getStock()).isEqualTo(4);
    }
}
