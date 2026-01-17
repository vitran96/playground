package tech.kingoyster.spring_1.authentication;

import lombok.Builder;

@Builder
public record LoginResponse(String accessToken, String refreshToken) {}
