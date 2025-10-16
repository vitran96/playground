package tech.kingoyster.spring_1;

//import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
//import org.springframework.http.HttpStatus;
//import org.springframework.security.config.Customizer;
//import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
//import org.springframework.security.config.annotation.web.configurers.RequestCacheConfigurer;
//import org.springframework.security.core.userdetails.User;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.security.core.userdetails.UserDetailsService;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.security.provisioning.InMemoryUserDetailsManager;
//import org.springframework.security.web.SecurityFilterChain;
//import org.springframework.security.web.authentication.HttpStatusEntryPoint;
//import org.springframework.web.cors.*;

import java.util.List;

//@EnableWebSecurity
//@EnableMethodSecurity
@Configuration
public class SecurityConfig {

//    @Bean
//    public SecurityFilterChain apiSecurityChain(HttpSecurity http) throws Exception {
//        http
//                // NOTE: why disable for REST?
//                .csrf(AbstractHttpConfigurer::disable)
//                // NOTE: why does this affect redirect?
//                .requestCache(RequestCacheConfigurer::disable)
//                .cors(Customizer.withDefaults())
//                .authorizeHttpRequests(auth -> auth
//                        // NOTE: this allow create internal only API.
//                        // TODO: But how about internal between service?
//                        .requestMatchers("/internal/**").denyAll()
//                        .requestMatchers(
//                                "/v3/api-docs/**",
//                                // NOTE: what is scalar?
//                                "/scalar/**",
//                                "/actuator/health"
////                                "/h2-console/**"
//                        ).permitAll()
//                        .anyRequest().authenticated()
//                )
//                // NOTE: basic auth is now (simple), no "formLogin()" -> no redirect
//                .httpBasic(Customizer.withDefaults())
//                .exceptionHandling(ex -> ex
//                        // NOTE: return 401
//                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
//                        .accessDeniedHandler((request, response, accessDeniedException) -> {
//                            // TODO: why do it this way??
//                            response.setStatus(HttpStatus.FORBIDDEN.value());
//                            response.setContentType("application/json");
//                            response.getWriter().write("{\"error\":\"forbidden\"}");
//                        })
//                );
//
//        // NOTE: this is for H2 console frame
////        http.headers(
////                headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
////        );
//
//        return http.build();
//    }
//
//    // TODO: replace this with service to interact with User table in DB
//    @Bean
//    UserDetailsService users(PasswordEncoder encoder) {
//        UserDetails admin = User.withUsername("admin")
//                .password(encoder.encode("Admin@123"))
//                .roles("ADMIN")
//                .build();
//
//        UserDetails user = User.withUsername("user")
//                .password(encoder.encode("User@123"))
//                .roles("USER")
//                .build();
//
//        return new InMemoryUserDetailsManager(admin, user);
//    }
//
//    @Bean
//    PasswordEncoder passwordEncoder() {
//        return new BCryptPasswordEncoder();
//    }
//
//    // TODO: this should come from application.yml & ENV
//    @Bean
//    CorsConfigurationSource corsConfigurationSource() {
//        CorsConfiguration cfg = new CorsConfiguration();
//        cfg.setAllowedOrigins(List.of("http://localhost:5173")); // React dev origin
//        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
//        cfg.setAllowedHeaders(List.of("*"));
//        cfg.setAllowCredentials(true);
//
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        source.registerCorsConfiguration("/**", cfg);
//        return source;
//    }
}
