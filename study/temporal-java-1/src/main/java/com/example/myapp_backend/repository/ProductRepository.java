package com.example.myapp_backend.repository;

import com.example.myapp_backend.model.Product;
import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class ProductRepository {
    private final Map<String, Product> products = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        products.put("prod-1", new Product("prod-1", "Laptop Pro", 1200.0, 10));
        products.put("prod-2", new Product("prod-2", "Wireless Headphones", 150.0, 20));
        products.put("prod-3", new Product("prod-3", "Smart Watch", 300.0, 5));
    }

    public List<Product> findAll() {
        return new ArrayList<>(products.values());
    }

    public Optional<Product> findById(String id) {
        return Optional.ofNullable(products.get(id));
    }

    public synchronized boolean reserveStock(String productId, int quantity) {
        Product p = products.get(productId);
        if (p == null || p.getStockQuantity() < quantity) {
            return false;
        }
        p.setStockQuantity(p.getStockQuantity() - quantity);
        return true;
    }

    public synchronized void restoreStock(String productId, int quantity) {
        Product p = products.get(productId);
        if (p != null) {
            p.setStockQuantity(p.getStockQuantity() + quantity);
        }
    }
}
