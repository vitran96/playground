package com.example.myapp_backend.repository;

import com.example.myapp_backend.model.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@Slf4j
public class OrderRepository {
    private final Map<String, Order> store = new ConcurrentHashMap<>();

    public Order save(Order order) {
        log.info("OrderRepository.save: Saving order {}", order != null ? order.getId() : null);
        store.put(order.getId(), order);
        return order;
    }

    public Optional<Order> findById(String id) {
        log.info("OrderRepository.findById: Finding order with id {}", id);
        return Optional.ofNullable(store.get(id));
    }

    public Collection<Order> findAll() {
        log.info("OrderRepository.findAll: Retrieving all orders");
        return store.values();
    }
}
