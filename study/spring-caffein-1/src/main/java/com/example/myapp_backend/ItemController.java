package com.example.myapp_backend;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/items")
public class ItemController {

    private final ItemService service;

    ItemController(ItemService service) { this.service = service; }

    @GetMapping
    List<Item> list() { return service.getItems(); }

    @PostMapping
    Item add(@RequestBody Map<String, String> body) {
        return service.addItem(body.get("name"));
    }
}
