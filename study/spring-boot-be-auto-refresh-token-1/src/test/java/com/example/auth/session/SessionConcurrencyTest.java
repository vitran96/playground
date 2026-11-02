package com.example.auth.session;

import static com.example.auth.TestProps.T0;
import static org.assertj.core.api.Assertions.assertThat;

import com.example.auth.MutableClock;
import com.example.auth.TestProps;
import com.example.auth.config.CacheConfig;
import com.example.auth.security.TokenService;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.cache.caffeine.CaffeineCacheManager;

class SessionConcurrencyTest {

    private static final int THREADS = 64;

    private final MutableClock clock = new MutableClock(T0);
    private final SessionService sessions = new SessionService(new InMemoryRefreshSessionStore(),
            new CaffeineCacheManager(CacheConfig.REVOKED_SIDS), clock, TestProps.props());
    private final TokenService tokens = new TokenService(TestProps.props(), clock);

    /** Starts all tasks at the same instant; a deadlock surfaces as a TimeoutException. */
    private static <T> List<T> runConcurrently(Callable<T> task) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        try {
            var ready = new CountDownLatch(THREADS);
            var go = new CountDownLatch(1);
            List<Future<T>> futures = new ArrayList<>();
            for (int i = 0; i < THREADS; i++) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    go.await();
                    return task.call();
                }));
            }
            ready.await();
            go.countDown();
            List<T> results = new ArrayList<>();
            for (Future<T> f : futures) results.add(f.get(20, TimeUnit.SECONDS));
            return results;
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void sameRefreshTokenUsedConcurrentlyAlwaysSucceeds() throws Exception {
        var created = sessions.create("alice", List.of("USER"));
        int rounds = 200;

        List<Integer> successes = runConcurrently(() -> {
            int ok = 0;
            for (int i = 0; i < rounds; i++) {
                RefreshSession s = sessions.findValid(created.rawToken()).orElseThrow();
                tokens.decode(tokens.issue(s).getTokenValue()).orElseThrow();
                ok++;
            }
            return ok;
        });

        assertThat(successes.stream().mapToInt(Integer::intValue).sum()).isEqualTo(THREADS * rounds);
        assertThat(sessions.findValid(created.rawToken())).contains(created.session()); // untouched
    }

    @Test
    void revokeRacingRefreshesIsSafeAndEndsRevoked() throws Exception {
        var created = sessions.create("alice", List.of("USER"));
        var sid = created.session().id();
        var revokedHere = new AtomicInteger();
        var index = new AtomicInteger();

        runConcurrently(() -> {
            if (index.getAndIncrement() % 2 == 0) {
                if (sessions.revoke(sid)) revokedHere.incrementAndGet();
                return 0;
            }
            int ok = 0;
            for (int i = 0; i < 500; i++) {
                if (sessions.findValid(created.rawToken()).isPresent()) ok++;
            }
            return ok;
        });

        assertThat(revokedHere).hasValue(1); // exactly one caller did the revoking
        assertThat(sessions.findValid(created.rawToken())).isEmpty();
        assertThat(sessions.isRevoked(sid.toString())).isTrue();
    }

    @Test
    void concurrentLoginsCreateDistinctWorkingSessions() throws Exception {
        List<SessionService.Created> created = runConcurrently(() -> sessions.create("alice", List.of("USER")));

        assertThat(created).extracting(c -> c.session().id()).doesNotHaveDuplicates();
        assertThat(created).extracting(SessionService.Created::rawToken).doesNotHaveDuplicates();
        assertThat(sessions.sessionsOf("alice")).hasSize(THREADS);
        assertThat(created).allSatisfy(c ->
                assertThat(sessions.findValid(c.rawToken())).contains(c.session()));
    }
}
