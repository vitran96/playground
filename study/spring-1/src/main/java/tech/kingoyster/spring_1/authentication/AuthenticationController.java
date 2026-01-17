package tech.kingoyster.spring_1.authentication;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import tech.kingoyster.spring_1.common.CookieService;
import tech.kingoyster.spring_1.exception.NotAuthenticatedException;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {
    private static final String REFRESH_TOKEN_COOKIE_NAME = "refresh";

    private final AuthenticationService authenticationService;
    private final CookieService cookieService;

    @PostMapping("/login")
    public LoginResponse login(
            HttpServletResponse response, @RequestBody @Valid LoginRequest loginRequest) {
        LoginResponse loginResponse = authenticationService.authenticate(loginRequest);

        Cookie cookie =
                cookieService.createCookie(REFRESH_TOKEN_COOKIE_NAME, loginResponse.refreshToken());
        response.addCookie(cookie);
        return loginResponse;
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = "bearerAuth")
    public void logout(HttpServletResponse response) {
        Cookie removeCookie = cookieService.removeCookie(REFRESH_TOKEN_COOKIE_NAME);
        response.addCookie(removeCookie);
    }

    @PostMapping("/refresh")
    public RefreshDto refresh(@CookieValue(REFRESH_TOKEN_COOKIE_NAME) String refreshToken) {
        if (Objects.isNull(refreshToken)) {
            throw new NotAuthenticatedException("Refresh token not exists!");
        }

        return authenticationService.refreshToken(refreshToken);
    }
}
