package com.example.auth.config;

import com.example.auth.security.AuthCookies;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info().title("Cookie Auth API").version("v1").description(
                        "Call POST /api/auth/login first (alice / password). The browser stores the "
                        + "HttpOnly cookies, so every later call from this page is authenticated."))
                .components(new Components().addSecuritySchemes("cookieAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.COOKIE)
                        .name(AuthCookies.ACCESS)));
    }
}
