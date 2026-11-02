package com.example.auth.session;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Immutable. Only the SHA-256 hash of the refresh token is kept. */
public record RefreshSession(UUID id, String username, List<String> roles, String tokenHash,
                             Instant createdAt, Instant expiresAt, Instant revokedAt) {

    public RefreshSession {
        roles = List.copyOf(roles);
    }

    public boolean isActive(Instant now) {
        return revokedAt == null && now.isBefore(expiresAt);
    }

    public RefreshSession revokedAt(Instant at) {
        return revokedAt != null ? this
                : new RefreshSession(id, username, roles, tokenHash, createdAt, expiresAt, at);
    }
}
