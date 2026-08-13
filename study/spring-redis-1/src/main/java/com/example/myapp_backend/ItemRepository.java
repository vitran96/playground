package com.example.myapp_backend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class ItemRepository {

    private static final Logger log = LoggerFactory.getLogger(ItemRepository.class);

    private final Map<Long, Item> items = new ConcurrentHashMap<>();

    public ItemRepository() {
        log.info("ItemRepository initialized with sample data");
        items.put(1L, new Item(1L, "Laptop", "electronics", 999.99));
        items.put(2L, new Item(2L, "Headphones", "electronics", 149.99));
        items.put(3L, new Item(3L, "Coffee Mug", "kitchen", 12.99));
        items.put(4L, new Item(4L, "Notebook", "office", 5.99));
        items.put(5L, new Item(5L, "Desk Lamp", "office", 34.99));
    }

    public List<Item> findAll() {
        log.info("ItemRepository.findAll() - simulating slow query");
        simulateSlowQuery();
        return new ArrayList<>(items.values());
    }

    public List<Item> findByCategory(String category) {
        log.info("ItemRepository.findByCategory({}) - simulating slow query", category);
        simulateSlowQuery();
        return items.values().stream()
                .filter(item -> item.category() != null && item.category().equalsIgnoreCase(category))
                .collect(Collectors.toList());
    }

    public Item findById(Long id) {
        log.info("ItemRepository.findById({}) - simulating slow query", id);
        simulateSlowQuery();
        return items.get(id);
    }

    public Item save(Item item) {
        log.info("ItemRepository.save({}) - simulating slow query", item);
        simulateSlowQuery();
        items.put(item.id(), item);
        return item;
    }

    public void clearData() {
        log.info("ItemRepository.clearData()");
    }

    private void simulateSlowQuery() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
