package com.example.myapp_backend;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/api/mock/external")
public class MockExternalController {

    private final AtomicInteger flakyCounter = new AtomicInteger(0);

    @GetMapping("/always-fail")
    public ResponseEntity<Map<String, String>> alwaysFail() {
        return ResponseEntity.status(500).body(Map.of("error", "Service unavailable"));
    }

    @GetMapping("/flaky")
    public ResponseEntity<Map<String, String>> flaky() {
        if (flakyCounter.incrementAndGet() % 5 == 0) {
            return ResponseEntity.status(500).body(Map.of("error", "Intermittent failure"));
        }
        return ResponseEntity.ok(Map.of("message", "Hello World from Flaky Service"));
    }

    @GetMapping("/healthy")
    public ResponseEntity<Map<String, String>> healthy() {
        return ResponseEntity.ok(Map.of("message", "Hello World from Healthy Service"));
    }
}
