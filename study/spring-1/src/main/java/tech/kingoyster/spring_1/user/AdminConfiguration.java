package tech.kingoyster.spring_1.user;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.admin")
@RequiredArgsConstructor
@Getter
@Setter
public class AdminConfiguration {
    private String fullName;
    private String email;
    private String password;
}
