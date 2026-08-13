package com.example.myapp_backend;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/client")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @GetMapping("/call-failing")
    public ResponseEntity<Map<String, String>> callFailing() throws IOException {
        return ResponseEntity.ok(clientService.callFailing());
    }

    @GetMapping("/call-flaky")
    public ResponseEntity<Map<String, String>> callFlaky() throws IOException {
        return ResponseEntity.ok(clientService.callFlaky());
    }

    @GetMapping("/call-healthy")
    public ResponseEntity<Map<String, String>> callHealthy() throws IOException {
        return ResponseEntity.ok(clientService.callHealthy());
    }

    @GetMapping("/rate-limited")
    public ResponseEntity<Map<String, String>> rateLimited() throws IOException {
        Map<String, String> result = clientService.callRateLimited();
        if (result.containsKey("status") && result.get("status").contains("Rate limit")) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(result);
        }
        return ResponseEntity.ok(result);
    }
}
