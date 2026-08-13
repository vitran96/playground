package com.example.myapp_backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class ItemCacheTest {

    @Autowired ItemService service;

    @Test
    void secondCallHitsCache() {
        service.getItems(); // cold
        long start = System.nanoTime();
        var items = service.getItems(); // should be cached
        long ms = (System.nanoTime() - start) / 1_000_000;
        assertTrue(!items.isEmpty());
        // cached call should be well under the 2 s simulated delay
        assertTrue(ms < 500, "Expected cache hit but took " + ms + "ms");
    }

    @Test
    void evictOnAdd() {
        var before = service.getItems().size();
        service.addItem("Cherry");
        var after = service.getItems(); // cache was evicted, reloads
        assertTrue(after.size() > before);
    }
}
