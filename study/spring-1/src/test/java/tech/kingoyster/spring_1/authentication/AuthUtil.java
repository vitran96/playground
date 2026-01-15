package tech.kingoyster.spring_1.authentication;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

public class AuthUtil {
    AuthUtil() {}

    public static Authentication getDummyAuth(String username) {
        UserDetails principal = User.builder()
                .username(username)
                // Password cannot be null
                .password("")
                .build();
        return UsernamePasswordAuthenticationToken.authenticated(
                principal,
                "",
                null
        );
    }
}
