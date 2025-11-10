package tech.kingoyster.spring_1.authentication;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthenticationController {
    private static final String REFRESH_TOKEN_COOKIE_NAME = "refresh";

    private final long refreshTokenExpiry;

    public AuthenticationController(
            @Value("${app.jwt.refresh.expiry}") long refreshTokenExpiry
    ) {
        this.refreshTokenExpiry = refreshTokenExpiry;
    }

    @PostMapping("/login")
    public LoginResponse login(
            HttpServletResponse response,
            @RequestBody @Valid LoginRequest loginRequest) {
        // set refresh token to cookie
        // return access token
        throw new NotImplementedException();
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletResponse response) {
        // NOTE: remove by set max-age -> 0 + null content
        // create a cookie
//        Cookie cookie = new Cookie("username", null);
//        cookie.setMaxAge(0);
//        cookie.setSecure(true);
//        cookie.setHttpOnly(true);
//
////add cookie to response
//        response.addCookie(cookie);

        throw new NotImplementedException();
    }

    @PostMapping("/refresh")
    public AccessTokenResponse refresh(
            @CookieValue(REFRESH_TOKEN_COOKIE_NAME) String refreshToken,
            @RequestBody RefreshRequest refreshRequest) {
        throw new NotImplementedException();
    }
}
