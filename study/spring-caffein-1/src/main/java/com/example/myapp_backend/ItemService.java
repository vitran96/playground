package com.example.myapp_backend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ItemService {

    private static final Logger log = LoggerFactory.getLogger(ItemService.class);

    private final AtomicLong idGen = new AtomicLong();
    private final List<Item> items = new CopyOnWriteArrayList<>(List.of(
            new Item(idGen.incrementAndGet(), "Apple"),
            new Item(idGen.incrementAndGet(), "Banana")));

    @Cacheable("itemsCache")
    public List<Item> getItems() {
        log.info("Cache MISS – loading items");
        try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        return List.copyOf(items);
    }

    @CacheEvict(value = "itemsCache", allEntries = true)
    public Item addItem(String name) {
        var item = new Item(idGen.incrementAndGet(), name);
        items.add(item);
        log.info("Added {} – cache evicted", item);
        return item;
    }
}
