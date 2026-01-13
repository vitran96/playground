package tech.kingoyster.spring_1.authentication;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.security.core.Authentication;

import java.time.Instant;
import java.util.Objects;

public interface JwtProvider {
    Authentication getAuthentication(String token);

    void verifyOrThrow(DecodedJWT decodedJWT);

    DecodedJWT decodeJwt(String token);

    String generateToken(int expiry, String subject);

    String generateAccessToken(String idStr);

    String generateRefreshToken(String idStr);
}
