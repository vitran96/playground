package lab;

import lab.product.Product;
import lab.product.ProductRepository;
import lab.product.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static lab.ConcurrentHelper.runConcurrently;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class Experiment0NaiveTest {

    @Autowired
    private ProductService service;

    @Autowired
    private ProductRepository repo;

    @Test
    @DisplayName("Exp 0: naive buy causes lost updates and oversells (stock ends > 0 despite 20 buys)")
    void naiveBuyShowsOversell() throws Exception {
        Product product = repo.save(new Product("Naive Mug", 10));
        long id = product.getId();

        var out = runConcurrently(20, () -> service.buyNaive(id));

        // In a race condition without locks, multiple threads read the same stock
        // and overwrite each other's updates. More than 10 purchases succeed,
        // and stock ends greater than 0 instead of 0.
        Product updated = repo.findById(id).orElseThrow();
        System.out.println("Exp 0 Naive Buy -> ok: " + out.ok() + ", errors: " + out.errors().size() + ", final stock: " + updated.getStock());

        assertThat(out.ok()).isGreaterThan(10);
        assertThat(updated.getStock()).isPositive();
    }
}
