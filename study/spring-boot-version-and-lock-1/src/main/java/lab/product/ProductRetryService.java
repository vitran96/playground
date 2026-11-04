package lab.product;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ProductRetryService {
    private static final Logger log = LoggerFactory.getLogger(ProductRetryService.class);
    private final ProductService productService;

    public ProductRetryService(ProductService productService) {
        this.productService = productService;
    }

    public int buyOptimisticWithRetry(long id, int maxAttempts, AtomicInteger totalRetries) {
        for (int i = 0; i < maxAttempts; i++) {
            try {
                productService.buyOptimistic(id);
                return i;
            } catch (ObjectOptimisticLockingFailureException e) {
                if (totalRetries != null) {
                    totalRetries.incrementAndGet();
                }
                log.debug("Retry attempt {} for product {}", i + 1, id);
            }
        }
        throw new IllegalStateException("too much contention on product " + id);
    }
}
