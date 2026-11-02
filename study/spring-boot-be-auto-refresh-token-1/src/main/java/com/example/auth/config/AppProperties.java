package com.example.auth.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app")
public record AppProperties(Auth auth, CookieSettings cookie, Cors cors, List<User> users) {

    public record Auth(Duration accessTtl, Duration refreshTtl, String jwtSecret) {}

    public record CookieSettings(boolean secure, String sameSite) {}

    public record Cors(List<String> allowedOrigins) {}

    public record User(String username, String password, List<String> roles) {}
}
