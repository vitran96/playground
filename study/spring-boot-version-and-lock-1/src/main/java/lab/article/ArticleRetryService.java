package lab.article;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ArticleRetryService {
    private static final Logger log = LoggerFactory.getLogger(ArticleRetryService.class);
    private final ArticleService articleService;

    public ArticleRetryService(ArticleService articleService) {
        this.articleService = articleService;
    }

    public int likeWithRetry(long id, int maxAttempts, AtomicInteger totalRetries) {
        for (int i = 0; i < maxAttempts; i++) {
            try {
                articleService.like(id);
                return i;
            } catch (ObjectOptimisticLockingFailureException e) {
                if (totalRetries != null) {
                    totalRetries.incrementAndGet();
                }
                log.debug("Retry attempt {} for article {}", i + 1, id);
            }
        }
        throw new IllegalStateException("too much contention on article " + id);
    }
}
