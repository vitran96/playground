package tech.kingoyster.spring_1.common;

import jakarta.servlet.http.Cookie;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CookieServiceImpl implements CookieService {
    private final int refreshTokenExpiry;
    private final boolean secure;

    public CookieServiceImpl(
            @Value("${app.jwt.refresh.expiry}") int refreshTokenExpiry,
            @Value("${app.cookie.secure}") boolean secure
    ) {
        this.refreshTokenExpiry = refreshTokenExpiry;
        this.secure = secure;
    }

    @Override
    public Cookie createCookie(String key, String value, int expiry) {
        Cookie cookie = new Cookie(key, value);
        cookie.setMaxAge(expiry);
        cookie.setSecure(this.secure);
        cookie.setHttpOnly(true);
        cookie.setPath("/");

        return cookie;
    }

    @Override
    public Cookie createCookie(String key, String value) {
        return createCookie(key, value, this.refreshTokenExpiry);
    }

    @Override
    public Cookie removeCookie(String key) {
        // set content to null and expiry=0 to remove cookie
        return createCookie(key, null, 0);
    }
}
