package com.example.auth.security;

import static com.example.auth.TestProps.ACCESS_TTL;
import static com.example.auth.TestProps.T0;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.auth.MutableClock;
import com.example.auth.TestProps;
import com.example.auth.session.RefreshSession;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class TokenServiceTest {

    private final MutableClock clock = new MutableClock(T0);
    private final TokenService tokens = new TokenService(TestProps.props(), clock);

    private static RefreshSession session(Instant expiresAt) {
        return new RefreshSession(UUID.randomUUID(), "alice", List.of("USER"), "hash", T0, expiresAt, null);
    }

    @Test
    void issuedTokenDecodesWithItsClaims() {
        RefreshSession s = session(T0.plus(Duration.ofDays(7)));

        Jwt decoded = tokens.decode(tokens.issue(s).getTokenValue()).orElseThrow();

        assertThat(decoded.getSubject()).isEqualTo("alice");
        assertThat(decoded.getClaimAsString("sid")).isEqualTo(s.id().toString());
        assertThat(decoded.getClaimAsStringList("roles")).containsExactly("USER");
        assertThat(decoded.getExpiresAt()).isEqualTo(T0.plus(ACCESS_TTL));
    }

    @Test
    void expiredTokenIsRejected() {
        String token = tokens.issue(session(T0.plus(Duration.ofDays(7)))).getTokenValue();

        clock.advance(ACCESS_TTL.plusSeconds(1));

        assertThat(tokens.decode(token)).isEmpty();
    }

    @Test
    void accessTokenNeverOutlivesItsRefreshSession() {
        Instant sessionEnd = T0.plus(Duration.ofMinutes(1));

        assertThat(tokens.issue(session(sessionEnd)).getExpiresAt()).isEqualTo(sessionEnd);
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        var other = new TokenService(TestProps.props("another-secret-another-secret-12345"), clock);
        String foreign = other.issue(session(T0.plus(Duration.ofDays(7)))).getTokenValue();

        assertThat(tokens.decode(foreign)).isEmpty();
    }

    @Test
    void blankSecretGivesEachInstanceItsOwnRandomKey() {
        var a = new TokenService(TestProps.props(""), clock);
        var b = new TokenService(TestProps.props(""), clock);
        String token = a.issue(session(T0.plus(Duration.ofDays(7)))).getTokenValue();

        assertThat(a.decode(token)).isPresent();
        assertThat(b.decode(token)).isEmpty();
    }

    @Test
    void garbageIsRejected() {
        assertThat(tokens.decode(null)).isEmpty();
        assertThat(tokens.decode("")).isEmpty();
        assertThat(tokens.decode("not.a.jwt")).isEmpty();
    }

    @Test
    void shortSecretFailsAtStartup() {
        assertThatThrownBy(() -> new TokenService(TestProps.props("too-short"), clock))
                .isInstanceOf(IllegalStateException.class);
    }
}
