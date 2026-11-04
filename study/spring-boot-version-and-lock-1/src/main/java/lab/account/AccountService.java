package lab.account;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {
    private final AccountRepository accountRepo;
    private final EntityManager entityManager;

    public AccountService(AccountRepository accountRepo, EntityManager entityManager) {
        this.accountRepo = accountRepo;
        this.entityManager = entityManager;
    }

    @Transactional
    public void transfer(long from, long to, int amount) {
        Account a = accountRepo.findByIdForUpdate(from).orElseThrow();
        sleep(100);
        Account b = accountRepo.findByIdForUpdate(to).orElseThrow();
        a.setBalance(a.getBalance() - amount);
        b.setBalance(b.getBalance() + amount);
    }

    @Transactional
    public void transferOrdered(long from, long to, int amount) {
        long first = Math.min(from, to);
        long second = Math.max(from, to);
        accountRepo.findByIdForUpdate(first).orElseThrow();
        accountRepo.findByIdForUpdate(second).orElseThrow();

        Account a = accountRepo.findById(from).orElseThrow();
        Account b = accountRepo.findById(to).orElseThrow();
        a.setBalance(a.getBalance() - amount);
        b.setBalance(b.getBalance() + amount);
    }

    @Transactional
    public void lockNowait(long id) {
        accountRepo.findByIdNowait(id).orElseThrow();
    }

    @Transactional
    public void lockWithTimeout(long id, int seconds) {
        entityManager.createNativeQuery("set local lock_timeout = '" + seconds + "s'").executeUpdate();
        accountRepo.findByIdForUpdate(id).orElseThrow();
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
