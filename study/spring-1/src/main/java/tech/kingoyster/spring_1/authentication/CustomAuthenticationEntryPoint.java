package tech.kingoyster.spring_1.authentication;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tech.kingoyster.spring_1.HttpMediaType;
import tech.kingoyster.spring_1.exception.ErrorDetail;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        var message = authException.getMessage();
        var customAuthException = request.getAttribute(AccessTokenFilter.AUTH_EXCEPTION_ATTRIBUTE);
        if (Objects.nonNull(customAuthException) && customAuthException instanceof JWTVerificationException jwtVerificationException) {
            message = jwtVerificationException.getMessage();
        }

        var errorDetail = new ErrorDetail(
                LocalDateTime.now(),
                0,
                message,
                request.getServletPath(),
                null
        );

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(HttpMediaType.APP_JSON.getValue());
        response.getWriter().write(objectMapper.writeValueAsString(errorDetail));
    }
}
