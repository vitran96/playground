package com.example.auth.session;

import static com.example.auth.TestProps.REFRESH_TTL;
import static com.example.auth.TestProps.T0;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import com.example.auth.MutableClock;
import com.example.auth.TestProps;
import com.example.auth.config.CacheConfig;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

class SessionServiceTest {

    private final MutableClock clock = new MutableClock(T0);
    private final RefreshSessionStore store = spy(new InMemoryRefreshSessionStore());
    private final SessionService sessions = new SessionService(
            store, new ConcurrentMapCacheManager(CacheConfig.REVOKED_SIDS), clock, TestProps.props());

    @Test
    void createStoresOnlyTheHash() {
        var created = sessions.create("alice", List.of("USER"));

        assertThat(created.session().tokenHash()).isNotEqualTo(created.rawToken());
        assertThat(created.session().tokenHash()).isEqualTo(SessionService.hash(created.rawToken()));
        assertThat(created.session().expiresAt()).isEqualTo(T0.plus(REFRESH_TTL));
    }

    @Test
    void findValidIsReadOnly() {
        var created = sessions.create("alice", List.of("USER"));
        clearInvocations(store);

        for (int i = 0; i < 3; i++) {
            assertThat(sessions.findValid(created.rawToken())).contains(created.session());
        }

        verify(store, never()).save(any());
        verify(store, never()).revoke(any(), any());
    }

    @Test
    void findValidRejectsExpiredUnknownAndBlank() {
        var created = sessions.create("alice", List.of("USER"));

        assertThat(sessions.findValid("unknown")).isEmpty();
        assertThat(sessions.findValid("")).isEmpty();
        assertThat(sessions.findValid(null)).isEmpty();

        clock.advance(REFRESH_TTL);
        assertThat(sessions.findValid(created.rawToken())).isEmpty();
    }

    @Test
    void revokeEndsTheSessionAndMarksItsSid() {
        var created = sessions.create("alice", List.of("USER"));
        var sid = created.session().id();

        assertThat(sessions.revoke(sid)).isTrue();
        assertThat(sessions.revoke(sid)).isFalse(); // idempotent

        assertThat(sessions.findValid(created.rawToken())).isEmpty();
        assertThat(sessions.isRevoked(sid.toString())).isTrue();
    }

    @Test
    void revokeOwnedRefusesAnotherUsersSession() {
        var bobs = sessions.create("bob", List.of("USER"));

        assertThat(sessions.revokeOwned("alice", bobs.session().id())).isFalse();
        assertThat(sessions.findValid(bobs.rawToken())).isPresent();
    }

    @Test
    void revokeAllEndsOnlyThatUsersSessions() {
        var a1 = sessions.create("alice", List.of("USER"));
        var a2 = sessions.create("alice", List.of("USER"));
        var b1 = sessions.create("bob", List.of("USER"));

        assertThat(sessions.revokeAll("alice")).isEqualTo(2);

        assertThat(sessions.findValid(a1.rawToken())).isEmpty();
        assertThat(sessions.findValid(a2.rawToken())).isEmpty();
        assertThat(sessions.findValid(b1.rawToken())).isPresent();
    }

    @Test
    void purgeRemovesExpiredSessions() {
        var created = sessions.create("alice", List.of("USER"));
        clock.advance(REFRESH_TTL);

        sessions.purgeExpired();

        assertThat(store.findById(created.session().id())).isEmpty();
    }
}
