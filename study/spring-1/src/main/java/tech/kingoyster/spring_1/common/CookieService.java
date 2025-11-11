package tech.kingoyster.spring_1.common;

import jakarta.servlet.http.Cookie;

public interface CookieService {
    Cookie createCookie(String key, String value, int expiry);
    Cookie createCookie(String key, String value);
    Cookie removeCookie(String key);
}
