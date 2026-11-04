package lab.product;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {
    private final ProductRepository repo;
    private final VersionedProductRepository versionedRepo;

    public ProductService(ProductRepository repo, VersionedProductRepository versionedRepo) {
        this.repo = repo;
        this.versionedRepo = versionedRepo;
    }

    @Transactional
    public void buyNaive(long id) {
        Product p = repo.findById(id).orElseThrow();
        if (p.getStock() <= 0) throw new IllegalStateException("sold out");
        sleep(50);
        p.setStock(p.getStock() - 1);
    }

    @Transactional
    public void buyLocked(long id) {
        Product p = repo.findByIdForUpdate(id).orElseThrow();
        if (p.getStock() <= 0) throw new IllegalStateException("sold out");
        sleep(50);
        p.setStock(p.getStock() - 1);
    }

    @Transactional
    public void buyOptimistic(long id) {
        VersionedProduct p = versionedRepo.findById(id).orElseThrow();
        if (p.getStock() <= 0) throw new IllegalStateException("sold out");
        sleep(20);
        p.setStock(p.getStock() - 1);
    }

    // Intentionally lacks @Transactional to demonstrate TransactionRequiredException
    public void buyLockedWithoutTx(long id) {
        repo.findByIdForUpdate(id);
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
