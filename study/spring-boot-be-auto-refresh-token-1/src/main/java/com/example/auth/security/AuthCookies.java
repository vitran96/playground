package com.example.auth.security;

import com.example.auth.config.AppProperties;
import com.example.auth.session.RefreshSession;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class AuthCookies {

    public static final String ACCESS = "access_token";
    public static final String REFRESH = "refresh_token";

    private final boolean secure;
    private final String sameSite;
    private final Clock clock;
    public AuthCookies(AppProperties props, Clock clock) {
        this.secure = props.cookie().secure();
        this.sameSite = props.cookie().sameSite();
        this.clock = clock;
    }

    public ResponseCookie access(Jwt jwt) {
        return build(ACCESS, jwt.getTokenValue(), until(jwt.getExpiresAt()));
    }

    public ResponseCookie refresh(String rawToken, RefreshSession session) {
        return build(REFRESH, rawToken, until(session.expiresAt()));
    }

    public List<ResponseCookie> clearAll() {
        return List.of(build(ACCESS, "", Duration.ZERO), build(REFRESH, "", Duration.ZERO));
    }

    public void write(HttpServletResponse response, ResponseCookie cookie) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public static Optional<String> read(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return Optional.empty();
        for (Cookie c : cookies) {
            if (c.getName().equals(name) && !c.getValue().isBlank()) return Optional.of(c.getValue());
        }
        return Optional.empty();
    }

    private Duration until(Instant instant) {
        Duration d = Duration.between(clock.instant(), instant);
        return d.isNegative() ? Duration.ZERO : d;
    }

    private ResponseCookie build(String name, String value, Duration maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/")
                .maxAge(maxAge)
                .build();
    }
}
