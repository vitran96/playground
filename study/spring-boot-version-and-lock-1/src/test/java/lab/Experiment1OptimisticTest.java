package lab;

import lab.article.Article;
import lab.article.ArticleRepository;
import lab.article.ArticleRetryService;
import lab.article.ArticleService;
import lab.article.EditArticle;
import lab.product.ProductRetryService;
import lab.product.VersionedProduct;
import lab.product.VersionedProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.concurrent.atomic.AtomicInteger;

import static lab.ConcurrentHelper.runConcurrently;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class Experiment1OptimisticTest {

    @Autowired
    private ArticleService articleService;

    @Autowired
    private ArticleRetryService articleRetryService;

    @Autowired
    private ArticleRepository articleRepo;

    @Autowired
    private VersionedProductRepository versionedProductRepo;

    @Autowired
    private ProductRetryService productRetryService;

    @Test
    @DisplayName("Exp 1A: two concurrent article edits, exactly one commits and one fails with optimistic locking exception")
    void twoConcurrentArticleEditsOneFails() throws Exception {
        Article article = articleRepo.save(new Article("Original Title", "Original Body"));
        long id = article.getId();
        assertThat(article.getVersion()).isZero();

        var out = runConcurrently(2, () -> {
            articleService.edit(id, "Updated by " + Thread.currentThread().getName(), "New Body");
        });

        assertThat(out.ok()).isEqualTo(1);
        assertThat(out.errors()).hasSize(1);
        assertThat(out.errors().getFirst())
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);

        Article updated = articleRepo.findById(id).orElseThrow();
        assertThat(updated.getVersion()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Exp 1B: stale-version edit rejected when checked; lost update occurs when check is omitted")
    void staleVersionEditRejected() {
        Article article = articleRepo.save(new Article("Doc V0", "Initial content"));
        long id = article.getId();
        long staleVersion = article.getVersion(); // 0

        // Colleague saves an intermediate update, bumping version to 1
        articleService.edit(id, "Doc V1 by colleague", "Updated content");
        Article intermediate = articleRepo.findById(id).orElseThrow();
        assertThat(intermediate.getVersion()).isEqualTo(1L);

        // 1. With version check: stale edit rejected
        EditArticle staleCmd = new EditArticle(id, staleVersion, "Stale Doc V2", "Overwritten?");
        assertThatThrownBy(() -> articleService.edit(staleCmd))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);

        // Verify colleague's edit was preserved
        Article afterRejected = articleRepo.findById(id).orElseThrow();
        assertThat(afterRejected.getTitle()).isEqualTo("Doc V1 by colleague");

        // 2. Without version check: stale edit causes silent lost update
        articleService.editWithoutCheck(staleCmd);
        Article afterUnchecked = articleRepo.findById(id).orElseThrow();
        assertThat(afterUnchecked.getTitle()).isEqualTo("Stale Doc V2");
    }

    @Test
    @DisplayName("Exp 1C: optimistic buy with retry is correct under contention; logs retry count")
    void optimisticBuyWithRetryNeverOversells() throws Exception {
        VersionedProduct product = versionedProductRepo.save(new VersionedProduct("Optimistic Mug", 10));
        long id = product.getId();

        var totalRetries = new AtomicInteger();
        var out = runConcurrently(20, () -> {
            productRetryService.buyOptimisticWithRetry(id, 50, totalRetries);
        });

        System.out.println("Exp 1C Optimistic Buy with Retry -> ok: " + out.ok()
                + ", errors: " + out.errors().size()
                + ", total retries: " + totalRetries.get());

        // Exactly 10 succeed, remaining 10 fail with sold out / contention
        assertThat(out.ok()).isEqualTo(10);
        assertThat(out.errors()).hasSize(10);
        assertThat(totalRetries.get()).isPositive();

        VersionedProduct finalProduct = versionedProductRepo.findById(id).orElseThrow();
        assertThat(finalProduct.getStock()).isZero();
    }

    @Test
    @DisplayName("Exp 1C (Bonus): like with retry accurately accumulates likes under contention")
    void optimisticLikeWithRetryAccumulatesCorrectly() throws Exception {
        Article article = articleRepo.save(new Article("Popular Post", "Hello World"));
        long id = article.getId();

        var totalRetries = new AtomicInteger();
        var out = runConcurrently(10, () -> {
            articleRetryService.likeWithRetry(id, 50, totalRetries);
        });

        System.out.println("Exp 1C Optimistic Like with Retry -> ok: " + out.ok()
                + ", errors: " + out.errors().size()
                + ", total retries: " + totalRetries.get());

        assertThat(out.ok()).isEqualTo(10);
        assertThat(out.errors()).isEmpty();

        Article finalArticle = articleRepo.findById(id).orElseThrow();
        assertThat(finalArticle.getLikes()).isEqualTo(10);
    }
}
