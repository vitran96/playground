package tech.kingoyster.spring_1.authentication;

import com.auth0.jwt.exceptions.JWTVerificationException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class AccessTokenFilter extends OncePerRequestFilter {

    private static final String AUTH_PREFIX = "Bearer ";
    public static final String AUTH_EXCEPTION_ATTRIBUTE = "authException";

    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String accessToken = extractToken(request.getHeader("Authorization"));
        if (StringUtils.isNotEmpty(accessToken)) {
            validateAccessToken(request, accessToken);
        }

        filterChain.doFilter(request, response);
    }

    private void validateAccessToken(HttpServletRequest request, String accessToken) {
        try {
            Authentication authentication = jwtProvider.getAuthentication(accessToken);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (JWTVerificationException e) {
            request.setAttribute(AUTH_EXCEPTION_ATTRIBUTE, e);
        }
    }

    private String extractToken(String fullBearerToken) {
        if (StringUtils.startsWith(fullBearerToken, AUTH_PREFIX)) {
            return StringUtils.removeStart(fullBearerToken, AUTH_PREFIX);
        }

        return null;
    }
}
