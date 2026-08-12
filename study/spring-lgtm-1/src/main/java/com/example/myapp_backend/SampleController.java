package com.example.myapp_backend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class SampleController {
    private static final Logger log = LoggerFactory.getLogger(SampleController.class);

    @GetMapping("/hello")
    public Map<String, String> hello() {
        log.info("Hello endpoint called");
        return Map.of("message", "Hello, World!");
    }

    @GetMapping("/error")
    public Map<String, String> error() {
        log.error("Error endpoint called");
        throw new RuntimeException("Intentional error for testing");
    }
}
