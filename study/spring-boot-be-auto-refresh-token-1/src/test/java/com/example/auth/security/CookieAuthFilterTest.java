package com.example.auth.security;

import static com.example.auth.TestProps.T0;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.auth.session.RefreshSession;
import com.example.auth.session.SessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class CookieAuthFilterTest {

    private static final UUID SID = UUID.randomUUID();

    @Mock TokenService tokens;
    @Mock SessionService sessions;
    @Mock AuthCookies cookies;
    @Mock FilterChain chain;

    private CookieAuthFilter filter;
    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/me");
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @BeforeEach
    void setUp() {
        filter = new CookieAuthFilter(tokens, sessions, cookies);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private static Jwt jwt(String value) {
        return Jwt.withTokenValue(value)
                .header("alg", "HS256")
                .subject("alice")
                .claim("sid", SID.toString())
                .claim("roles", List.of("USER"))
                .issuedAt(T0)
                .expiresAt(T0.plus(Duration.ofMinutes(10)))
                .build();
    }

    private static RefreshSession session() {
        return new RefreshSession(SID, "alice", List.of("USER"), "hash", T0, T0.plus(Duration.ofDays(7)), null);
    }

    private static Authentication currentAuth() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    @Test
    void validAccessTokenAuthenticatesWithoutRefreshing() throws Exception {
        request.setCookies(new Cookie(AuthCookies.ACCESS, "good"), new Cookie(AuthCookies.REFRESH, "raw"));
        when(tokens.decode("good")).thenReturn(Optional.of(jwt("good")));
        when(sessions.isRevoked(SID.toString())).thenReturn(false);

        filter.doFilter(request, response, chain);

        assertThat(((AuthPrincipal) currentAuth().getPrincipal()).username()).isEqualTo("alice");
        assertThat(response.getHeader(CookieAuthFilter.REFRESHED_HEADER)).isNull();
        verify(sessions, never()).findValid(any());
        verify(cookies, never()).write(any(), any());
        verify(chain).doFilter(request, response);
    }

    @Test
    void missingOrExpiredAccessIsRefreshedInTheSameRequest() throws Exception {
        request.setCookies(new Cookie(AuthCookies.ACCESS, "expired"), new Cookie(AuthCookies.REFRESH, "raw"));
        Jwt fresh = jwt("fresh");
        ResponseCookie freshCookie = ResponseCookie.from(AuthCookies.ACCESS, "fresh").build();
        when(tokens.decode("expired")).thenReturn(Optional.empty());
        when(sessions.findValid("raw")).thenReturn(Optional.of(session()));
        when(tokens.issue(any())).thenReturn(fresh);
        when(cookies.access(fresh)).thenReturn(freshCookie);

        filter.doFilter(request, response, chain);

        verify(cookies).write(response, freshCookie);
        assertThat(response.getHeader(CookieAuthFilter.REFRESHED_HEADER)).isEqualTo("true");
        assertThat(currentAuth()).isNotNull();
        assertThat(currentAuth().getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
        verify(chain).doFilter(request, response);
    }

    @Test
    void revokedAccessTokenFallsBackToRefreshWhichAlsoFails() throws Exception {
        request.setCookies(new Cookie(AuthCookies.ACCESS, "good"), new Cookie(AuthCookies.REFRESH, "raw"));
        when(tokens.decode("good")).thenReturn(Optional.of(jwt("good")));
        when(sessions.isRevoked(SID.toString())).thenReturn(true);
        when(sessions.findValid("raw")).thenReturn(Optional.empty());
        when(cookies.clearAll()).thenReturn(List.of(
                ResponseCookie.from(AuthCookies.ACCESS, "").maxAge(0).build(),
                ResponseCookie.from(AuthCookies.REFRESH, "").maxAge(0).build()));

        filter.doFilter(request, response, chain);

        assertThat(currentAuth()).isNull();
        verify(tokens, never()).issue(any());
        verify(chain).doFilter(request, response);
    }

    @Test
    void invalidRefreshClearsBothCookies() throws Exception {
        request.setCookies(new Cookie(AuthCookies.REFRESH, "stale"));
        ResponseCookie clearAccess = ResponseCookie.from(AuthCookies.ACCESS, "").maxAge(0).build();
        ResponseCookie clearRefresh = ResponseCookie.from(AuthCookies.REFRESH, "").maxAge(0).build();
        when(sessions.findValid("stale")).thenReturn(Optional.empty());
        when(cookies.clearAll()).thenReturn(List.of(clearAccess, clearRefresh));

        filter.doFilter(request, response, chain);

        verify(cookies).write(response, clearAccess);
        verify(cookies).write(response, clearRefresh);
        assertThat(currentAuth()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    void noCookiesMeansAnonymousAndNoWork() throws Exception {
        filter.doFilter(request, response, chain);

        assertThat(currentAuth()).isNull();
        verifyNoInteractions(tokens, sessions, cookies);
        verify(chain).doFilter(request, response);
    }
}
