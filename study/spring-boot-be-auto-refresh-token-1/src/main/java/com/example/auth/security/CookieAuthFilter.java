package com.example.auth.security;

import com.example.auth.session.RefreshSession;
import com.example.auth.session.SessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates from the access cookie. When it is missing, expired, invalid or revoked,
 * re-issues it from a valid refresh cookie and continues the same request.
 */
public class CookieAuthFilter extends OncePerRequestFilter {

    public static final String REFRESHED_HEADER = "X-Access-Token-Refreshed";

    private final TokenService tokens;
    private final SessionService sessions;
    private final AuthCookies cookies;

    public CookieAuthFilter(TokenService tokens, SessionService sessions, AuthCookies cookies) {
        this.tokens = tokens;
        this.sessions = sessions;
        this.cookies = cookies;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        Optional<Jwt> access = AuthCookies.read(request, AuthCookies.ACCESS)
                .flatMap(tokens::decode)
                .filter(jwt -> !sessions.isRevoked(jwt.getClaimAsString("sid")));

        if (access.isEmpty()) access = refresh(request, response);

        access.ifPresent(this::authenticate);
        chain.doFilter(request, response);
    }

    private Optional<Jwt> refresh(HttpServletRequest request, HttpServletResponse response) {
        Optional<String> raw = AuthCookies.read(request, AuthCookies.REFRESH);
        if (raw.isEmpty()) return Optional.empty();

        Optional<RefreshSession> session = sessions.findValid(raw.get());
        if (session.isEmpty()) {
            cookies.clearAll().forEach(c -> cookies.write(response, c)); // stop the browser resending it
            return Optional.empty();
        }

        Jwt jwt = tokens.issue(session.get());
        cookies.write(response, cookies.access(jwt));
        response.setHeader(REFRESHED_HEADER, "true");
        return Optional.of(jwt);
    }

    private void authenticate(Jwt jwt) {
        List<String> roles = Optional.ofNullable(jwt.getClaimAsStringList("roles")).orElse(List.of());
        var authorities = roles.stream().map(r -> new SimpleGrantedAuthority("ROLE_" + r)).toList();
        var principal = new AuthPrincipal(jwt.getSubject(), UUID.fromString(jwt.getClaimAsString("sid")));
        var authentication = UsernamePasswordAuthenticationToken.authenticated(principal, null, authorities);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }
}
