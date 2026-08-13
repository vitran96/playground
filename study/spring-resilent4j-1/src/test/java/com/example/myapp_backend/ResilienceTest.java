package com.example.myapp_backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class ResilienceTest {

    // ponytail: raw RestClient, no TestRestTemplate bean wiring needed
    private final RestClient client = RestClient.builder()
            .baseUrl("http://localhost:8080")
            .build();

    @Test
    void circuitBreakerTripsOnAlwaysFailing() {
        for (int i = 0; i < 10; i++) {
            String body = client.get().uri("/api/client/call-failing")
                    .retrieve().body(String.class);
            assertTrue(body.contains("Fallback"));
        }
    }

    @Test
    void healthyServiceStaysClosed() {
        for (int i = 0; i < 10; i++) {
            String body = client.get().uri("/api/client/call-healthy")
                    .retrieve().body(String.class);
            assertTrue(body.contains("result"));
        }
    }

    @Test
    void rateLimiterReturns429WhenExceeded() {
        // First 5 should succeed
        for (int i = 0; i < 5; i++) {
            String body = client.get().uri("/api/client/rate-limited")
                    .retrieve().body(String.class);
            assertTrue(body.contains("result"));
        }
        // 6th call should be rate-limited — server returns 429
        try {
            client.get().uri("/api/client/rate-limited")
                    .retrieve().body(String.class);
            fail("Expected 429 response");
        } catch (org.springframework.web.client.HttpClientErrorException.TooManyRequests e) {
            assertTrue(e.getResponseBodyAsString().contains("Rate limit exceeded"));
        }
    }
}
