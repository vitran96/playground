package com.example.myapp_backend.controller;

import com.example.myapp_backend.model.Product;
import com.example.myapp_backend.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    private final ProductRepository productRepository;

    @GetMapping
    public Collection<Product> getProducts() {
        log.info("ProductController.getProducts: Fetching all products");
        return productRepository.findAll();
    }

    @PostMapping
    public Product createProduct(@RequestBody Product product) {
        log.info("ProductController.createProduct: Creating product {}", product != null ? product.getId() : null);
        return productRepository.save(product);
    }
}
