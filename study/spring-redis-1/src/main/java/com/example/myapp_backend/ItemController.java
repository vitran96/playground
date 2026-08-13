package com.example.myapp_backend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/items")
public class ItemController {

    private static final Logger log = LoggerFactory.getLogger(ItemController.class);

    private final ItemRepository repository;

    public ItemController(ItemRepository repository) {
        log.info("ItemController initialized");
        this.repository = repository;
    }

    // Strategy 1: Standard Read-Through
    @GetMapping("/standard")
    @Cacheable(cacheNames = "items-default")
    public List<Item> getItems() {
        log.info("GET /api/v1/items/standard - cache miss, hitting repository");
        return repository.findAll();
    }

    // Strategy 2: Conditional Caching
    @GetMapping("/conditional")
    @Cacheable(
            cacheNames = "items-conditional",
            key = "#category",
            condition = "#category != 'uncached'",
            unless = "#result.isEmpty()"
    )
    public List<Item> getItemsByCategory(@RequestParam String category) {
        log.info("GET /api/v1/items/conditional?category={} - cache miss, hitting repository", category);
        return repository.findByCategory(category);
    }

    // Strategy 3: Short-lived TTL cache
    @GetMapping("/short-lived")
    @Cacheable(cacheNames = "items-short-ttl")
    public List<Item> getShortLivedItems() {
        log.info("GET /api/v1/items/short-lived - cache miss, hitting repository");
        return repository.findAll();
    }

    // Strategy 4: Cache Update / Write-Through
    @PutMapping("/update")
    @CachePut(cacheNames = "items-default", key = "#item.id()")
    public Item updateItem(@RequestBody Item item) {
        log.info("PUT /api/v1/items/update - updating cache for item id={}", item.id());
        return repository.save(item);
    }

    // Strategy 5: Cache Eviction
    @DeleteMapping("/clear")
    @CacheEvict(cacheNames = "items-default", allEntries = true)
    public String clearCache() {
        log.info("DELETE /api/v1/items/clear - evicting all entries from items-default");
        return "Cache 'items-default' cleared";
    }

    // Strategy 6: Combined Multi-Cache Action
    @PostMapping("/bulk-update")
    @Caching(
            evict = @CacheEvict(cacheNames = "items-default", allEntries = true),
            put = @CachePut(cacheNames = "items-conditional", key = "#item.category()")
    )
    public Item bulkUpdate(@RequestBody Item item) {
        log.info("POST /api/v1/items/bulk-update - evicting items-default, updating items-conditional for category={}", item.category());
        return repository.save(item);
    }
}
