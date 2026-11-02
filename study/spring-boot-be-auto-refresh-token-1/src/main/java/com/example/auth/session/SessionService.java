package com.example.auth.session;

import com.example.auth.config.AppProperties;
import com.example.auth.config.CacheConfig;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Collection;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class SessionService {

    private static final Logger log = LoggerFactory.getLogger(SessionService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshSessionStore store;
    private final Cache revokedSids;
    private final Clock clock;
    private final Duration refreshTtl;

    public SessionService(RefreshSessionStore store, CacheManager cacheManager,
                          Clock clock, AppProperties props) {
        this.store = store;
        this.revokedSids = Objects.requireNonNull(
                cacheManager.getCache(CacheConfig.REVOKED_SIDS), "cache " + CacheConfig.REVOKED_SIDS);
        this.clock = clock;
        this.refreshTtl = props.auth().refreshTtl();
    }

    public record Created(String rawToken, RefreshSession session) {}

    /** Called once at login. The raw token goes to the cookie, only its hash is stored. */
    public Created create(String username, Collection<String> roles) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant now = clock.instant();
        var session = new RefreshSession(UUID.randomUUID(), username, List.copyOf(roles),
                hash(raw), now, now.plus(refreshTtl), null);
        store.save(session);
        return new Created(raw, session);
    }

    /** Read-only: never rotates or writes, so concurrent refreshes cannot conflict. */
    public Optional<RefreshSession> findValid(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return Optional.empty();
        Instant now = clock.instant();
        return store.findByTokenHash(hash(rawToken)).filter(s -> s.isActive(now));
    }

    public List<RefreshSession> sessionsOf(String username) {
        return store.findByUsername(username);
    }

    /** Idempotent. Returns true if this call did the revoking. */
    public boolean revoke(UUID sessionId) {
        boolean changed = store.revoke(sessionId, clock.instant());
        revokedSids.put(sessionId.toString(), Boolean.TRUE);
        return changed;
    }

    /** Revokes only a session that belongs to the user. False = not found or not theirs. */
    public boolean revokeOwned(String username, UUID sessionId) {
        boolean owned = store.findById(sessionId)
                .map(s -> s.username().equals(username))
                .orElse(false);
        if (owned) revoke(sessionId);
        return owned;
    }

    public int revokeAll(String username) {
        int count = 0;
        for (RefreshSession s : store.findByUsername(username)) {
            if (revoke(s.id())) count++;
        }
        return count;
    }

    public boolean isRevoked(String sessionId) {
        return sessionId != null && revokedSids.get(sessionId) != null;
    }

    @Scheduled(fixedDelay = 10, timeUnit = TimeUnit.MINUTES)
    public void purgeExpired() {
        int removed = store.deleteExpired(clock.instant());
        if (removed > 0) log.debug("Purged {} expired refresh sessions", removed);
    }

    static String hash(String raw) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
