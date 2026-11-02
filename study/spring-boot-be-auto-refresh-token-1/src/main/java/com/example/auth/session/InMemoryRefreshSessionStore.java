package com.example.auth.session;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryRefreshSessionStore implements RefreshSessionStore {

    private final ConcurrentMap<UUID, RefreshSession> byId = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, UUID> idByHash = new ConcurrentHashMap<>();

    @Override
    public void save(RefreshSession session) {
        byId.put(session.id(), session);                  // session first,
        idByHash.put(session.tokenHash(), session.id());  // so a hash never points at nothing
    }

    @Override
    public Optional<RefreshSession> findByTokenHash(String tokenHash) {
        UUID id = idByHash.get(tokenHash);
        return id == null ? Optional.empty() : Optional.ofNullable(byId.get(id));
    }

    @Override
    public Optional<RefreshSession> findById(UUID id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public List<RefreshSession> findByUsername(String username) {
        return byId.values().stream()
                .filter(s -> s.username().equals(username))
                .sorted(Comparator.comparing(RefreshSession::createdAt))
                .toList();
    }

    @Override
    public boolean revoke(UUID id, Instant at) {
        var changed = new AtomicBoolean(false);
        byId.computeIfPresent(id, (key, s) -> {
            if (s.revokedAt() != null) return s;
            changed.set(true);
            return s.revokedAt(at);
        });
        return changed.get();
    }

    @Override
    public int deleteExpired(Instant now) {
        int removed = 0;
        for (RefreshSession s : byId.values()) {
            // remove(key, value) skips a session changed meanwhile; the next run gets it
            if (!now.isBefore(s.expiresAt()) && byId.remove(s.id(), s)) {
                idByHash.remove(s.tokenHash(), s.id());
                removed++;
            }
        }
        return removed;
    }
}
