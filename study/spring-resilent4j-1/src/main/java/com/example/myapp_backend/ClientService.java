package com.example.myapp_backend;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

@Service
public class ClientService {

    private final OkHttpClient httpClient;
    private final String baseUrl;

    public ClientService(OkHttpClient httpClient,
                         @Value("${app.base-url:http://localhost:${server.port:8080}}") String baseUrl) {
        this.httpClient = httpClient;
        this.baseUrl = baseUrl;
    }

    @CircuitBreaker(name = "alwaysFailingService", fallbackMethod = "fallback")
    public Map<String, String> callFailing() throws IOException {
        return call("/api/mock/external/always-fail");
    }

    @CircuitBreaker(name = "flakyService", fallbackMethod = "fallback")
    public Map<String, String> callFlaky() throws IOException {
        return call("/api/mock/external/flaky");
    }

    @CircuitBreaker(name = "healthyService", fallbackMethod = "fallback")
    public Map<String, String> callHealthy() throws IOException {
        return call("/api/mock/external/healthy");
    }

    @RateLimiter(name = "rateLimitedService", fallbackMethod = "rateLimitFallback")
    public Map<String, String> callRateLimited() throws IOException {
        return call("/api/mock/external/healthy");
    }

    private Map<String, String> call(String path) throws IOException {
        Request request = new Request.Builder().url(baseUrl + path).build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Downstream returned " + response.code());
            }
            return Map.of("result", response.body().string());
        }
    }

    private Map<String, String> fallback(Throwable t) {
        return Map.of("status", "Fallback: Downstream service is currently unavailable");
    }

    private Map<String, String> rateLimitFallback(Throwable t) {
        return Map.of("status", "Rate limit exceeded. Try again later.");
    }
}
