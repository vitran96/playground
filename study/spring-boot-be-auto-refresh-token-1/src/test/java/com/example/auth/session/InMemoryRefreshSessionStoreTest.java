package com.example.auth.session;

import static com.example.auth.TestProps.T0;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InMemoryRefreshSessionStoreTest {

    private final InMemoryRefreshSessionStore store = new InMemoryRefreshSessionStore();

    private static RefreshSession session(String user, String hash, Duration ttl) {
        return new RefreshSession(UUID.randomUUID(), user, List.of("USER"), hash, T0, T0.plus(ttl), null);
    }

    @Test
    void findsByHashAndById() {
        RefreshSession s = session("alice", "h1", Duration.ofDays(1));
        store.save(s);

        assertThat(store.findByTokenHash("h1")).contains(s);
        assertThat(store.findById(s.id())).contains(s);
        assertThat(store.findByTokenHash("unknown")).isEmpty();
    }

    @Test
    void revokeReportsTrueOnlyOnce() {
        RefreshSession s = session("alice", "h1", Duration.ofDays(1));
        store.save(s);

        assertThat(store.revoke(s.id(), T0)).isTrue();
        assertThat(store.revoke(s.id(), T0.plusSeconds(5))).isFalse();
        assertThat(store.findById(s.id()).orElseThrow().revokedAt()).isEqualTo(T0);
        assertThat(store.revoke(UUID.randomUUID(), T0)).isFalse();
    }

    @Test
    void findByUsernameReturnsOnlyThatUser() {
        store.save(session("alice", "h1", Duration.ofDays(1)));
        store.save(session("alice", "h2", Duration.ofDays(1)));
        store.save(session("bob", "h3", Duration.ofDays(1)));

        assertThat(store.findByUsername("alice")).hasSize(2);
        assertThat(store.findByUsername("bob")).hasSize(1);
    }

    @Test
    void deleteExpiredRemovesSessionAndItsHash() {
        RefreshSession expired = session("alice", "old", Duration.ofMinutes(1));
        RefreshSession live = session("alice", "new", Duration.ofDays(1));
        store.save(expired);
        store.save(live);

        assertThat(store.deleteExpired(T0.plus(Duration.ofMinutes(1)))).isEqualTo(1);

        assertThat(store.findByTokenHash("old")).isEmpty();
        assertThat(store.findById(expired.id())).isEmpty();
        assertThat(store.findByTokenHash("new")).contains(live);
    }
}
