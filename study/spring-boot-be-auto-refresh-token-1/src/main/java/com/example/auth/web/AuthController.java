package com.example.auth.web;

import com.example.auth.security.AuthCookies;
import com.example.auth.security.AuthPrincipal;
import com.example.auth.security.TokenService;
import com.example.auth.session.SessionService;
import com.example.auth.web.Dtos.LoginRequest;
import com.example.auth.web.Dtos.LoginResponse;
import com.example.auth.web.Dtos.SessionView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final SessionService sessions;
    private final TokenService tokens;
    private final AuthCookies cookies;

    public AuthController(AuthenticationManager authenticationManager, SessionService sessions,
                          TokenService tokens, AuthCookies cookies) {
        this.authenticationManager = authenticationManager;
        this.sessions = sessions;
        this.tokens = tokens;
        this.cookies = cookies;
    }

    @PostMapping("/login")
    @Operation(summary = "Log in; sets the access_token and refresh_token cookies")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest body,
                                                HttpServletResponse response) {
        Authentication auth;
        try {
            auth = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(body.username(), body.password()));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<String> roles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5))
                .sorted()
                .toList();

        var created = sessions.create(auth.getName(), roles);
        Jwt access = tokens.issue(created.session());
        cookies.write(response, cookies.access(access));
        cookies.write(response, cookies.refresh(created.rawToken(), created.session()));

        return ResponseEntity.ok(new LoginResponse(auth.getName(), roles, created.session().id(),
                access.getExpiresAt(), created.session().expiresAt()));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke the current session (if any) and clear both cookies")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal AuthPrincipal principal,
                                        HttpServletResponse response) {
        if (principal != null) sessions.revoke(principal.sessionId());
        cookies.clearAll().forEach(c -> cookies.write(response, c));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/sessions")
    @Operation(summary = "List your sessions")
    public List<SessionView> sessions(@AuthenticationPrincipal AuthPrincipal principal) {
        return sessions.sessionsOf(principal.username()).stream()
                .map(s -> new SessionView(s.id(), s.createdAt(), s.expiresAt(), s.revokedAt(),
                        s.id().equals(principal.sessionId())))
                .toList();
    }

    @DeleteMapping("/sessions/{id}")
    @Operation(summary = "Revoke one of your sessions")
    public ResponseEntity<Void> revoke(@PathVariable("id") UUID id,
                                        @AuthenticationPrincipal AuthPrincipal principal,
                                        HttpServletResponse response) {
        if (!sessions.revokeOwned(principal.username(), id)) return ResponseEntity.notFound().build();
        if (id.equals(principal.sessionId())) cookies.clearAll().forEach(c -> cookies.write(response, c));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/sessions/revoke-all")
    @Operation(summary = "Revoke all your sessions, including this one")
    public ResponseEntity<Void> revokeAll(@AuthenticationPrincipal AuthPrincipal principal,
                                           HttpServletResponse response) {
        sessions.revokeAll(principal.username());
        cookies.clearAll().forEach(c -> cookies.write(response, c));
        return ResponseEntity.noContent().build();
    }
}
