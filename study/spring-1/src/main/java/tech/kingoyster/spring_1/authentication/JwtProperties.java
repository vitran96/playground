package tech.kingoyster.spring_1.authentication;

import lombok.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.jwt")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class JwtProperties {
    private String secret;
    private TokenInfo access;
    private TokenInfo refresh;

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    public static class TokenInfo {
        private int expiry;
    }
}
