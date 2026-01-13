package tech.kingoyster.spring_1.authentication;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.jwt")
@RequiredArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class JwtProperties {
    private String secret;
    private TokenInfo access;
    private TokenInfo refresh;

    @RequiredArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    public static class TokenInfo {
        private int expiry;
    }
}
