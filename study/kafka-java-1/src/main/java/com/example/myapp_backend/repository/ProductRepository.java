package com.example.myapp_backend.repository;

import com.example.myapp_backend.model.Product;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class ProductRepository {
    private final Map<String, Product> store = new ConcurrentHashMap<>();

    public ProductRepository() {
        // Pre-seed sample products
        store.put("P101", new Product("P101", "Laptop", new BigDecimal("1200.00"), 10));
        store.put("P102", new Product("P102", "Wireless Mouse", new BigDecimal("25.00"), 50));
        store.put("P103", new Product("P103", "Mechanical Keyboard", new BigDecimal("85.00"), 0));
    }

    public Collection<Product> findAll() {
        return store.values();
    }

    public Optional<Product> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    public Product save(Product product) {
        store.put(product.getId(), product);
        return product;
    }

    public synchronized boolean reserveStock(String productId, int quantity) {
        Product p = store.get(productId);
        if (p != null && p.getStockQuantity() >= quantity) {
            p.setStockQuantity(p.getStockQuantity() - quantity);
            return true;
        }
        return false;
    }

    public synchronized void releaseStock(String productId, int quantity) {
        Product p = store.get(productId);
        if (p != null) {
            p.setStockQuantity(p.getStockQuantity() + quantity);
        }
    }
}
