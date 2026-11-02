package com.example.auth;

import com.example.auth.config.AppProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

public final class TestProps {
    public static final Duration ACCESS_TTL = Duration.ofMinutes(10);
    public static final Duration REFRESH_TTL = Duration.ofDays(7);
    public static final String SECRET = "test-secret-test-secret-test-secret!"; // 36 bytes
    public static final Instant T0 = Instant.parse("2030-01-01T00:00:00Z");

    private TestProps() {}

    public static AppProperties props() {
        return props(SECRET);
    }

    public static AppProperties props(String secret) {
        return new AppProperties(
                new AppProperties.Auth(ACCESS_TTL, REFRESH_TTL, secret),
                new AppProperties.CookieSettings(true, "Strict"),
                new AppProperties.Cors(List.of("http://localhost:5173")),
                List.of());
    }
}
