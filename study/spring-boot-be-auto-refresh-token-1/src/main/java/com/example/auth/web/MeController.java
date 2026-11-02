package com.example.auth.web;

import com.example.auth.security.AuthPrincipal;
import com.example.auth.web.Dtos.MeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Me")
public class MeController {

    @GetMapping("/api/me")
    @Operation(summary = "Who am I; silently refreshes the access token when needed")
    public MeResponse me(@AuthenticationPrincipal AuthPrincipal principal, Authentication authentication) {
        var roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.substring("ROLE_".length()))
                .sorted()
                .toList();
        return new MeResponse(principal.username(), roles, principal.sessionId());
    }
}
