package com.example.auth.session;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Swap the in-memory implementation for JPA or Redis without touching callers. */
public interface RefreshSessionStore {
    void save(RefreshSession session);

    Optional<RefreshSession> findByTokenHash(String tokenHash);

    Optional<RefreshSession> findById(UUID id);

    List<RefreshSession> findByUsername(String username);

    /** @return true only if this call moved the session from active to revoked. */
    boolean revoke(UUID id, Instant at);

    int deleteExpired(Instant now);
}
