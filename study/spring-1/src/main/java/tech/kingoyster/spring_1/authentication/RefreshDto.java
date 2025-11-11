package tech.kingoyster.spring_1.authentication;

import lombok.Builder;

@Builder
public record RefreshDto(String accessToken) {
}
