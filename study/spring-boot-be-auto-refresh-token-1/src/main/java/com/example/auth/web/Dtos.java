package com.example.auth.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class Dtos {
    private Dtos() {}

    public record LoginRequest(
            @NotBlank @Schema(example = "alice") String username,
            @NotBlank @Schema(example = "password") String password) {}

    public record LoginResponse(String username, List<String> roles, UUID sessionId,
                                Instant accessExpiresAt, Instant refreshExpiresAt) {}

    public record SessionView(UUID id, Instant createdAt, Instant expiresAt,
                              Instant revokedAt, boolean current) {}

    public record MeResponse(String username, List<String> roles, UUID sessionId) {}
}
