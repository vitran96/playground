package tech.kingoyster.spring_1;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.RequestCacheConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.cors.*;
import tech.kingoyster.spring_1.authentication.AccessTokenFilter;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final AccessTokenFilter accessTokenFilter;

    @Bean
    public SecurityFilterChain apiSecurityChain(HttpSecurity http) throws Exception {
        http
                // TODO: split and re-enable this
                .csrf(AbstractHttpConfigurer::disable)
                // NOTE: why does this affect redirect?
                .requestCache(RequestCacheConfigurer::disable)
                // TODO: better split this config
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                               "/swagger-ui.html",
                               "/swagger-ui/*",
                               "/v3/api-docs",
                               "/v3/api-docs/swagger-config",
                               "/api/v1/auth/login"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                // NOTE: basic auth is now (simple), no "formLogin()" -> no redirect
                .httpBasic(AbstractHttpConfigurer::disable)
                // NOTE: better add intercepter here to leverage Spring-security auth matcher
                .addFilterBefore(accessTokenFilter, BasicAuthenticationFilter.class)
                .exceptionHandling(ex -> ex
                        // NOTE: return 401
                        // TODO: consider different way to align with central ErrorResponse
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            // TODO: why do it this way??
                            response.setStatus(HttpStatus.FORBIDDEN.value());
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\":\"forbidden\"}");
                        })
                );

        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(List.of("http://localhost:5173")); // TODO: this should come from application.yml & ENV
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*")); // TODO: this should be limited too
        cfg.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
