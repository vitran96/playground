package tech.kingoyster.spring_1.authentication;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Objects;

@Service
public class JwtProvider {
    private final String secret;
    private final String issuer;
    private final int accessTokenExpiry;
    private final int refreshTokenExpiry;

    public JwtProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${spring.application.name}") String issuer,
            @Value("${app.jwt.access.expiry}") int accessTokenExpiry,
            @Value("${app.jwt.refresh.expiry}") int refreshTokenExpiry
    ) {
        this.secret = secret;
        this.issuer = issuer;
        this.accessTokenExpiry = accessTokenExpiry;
        this.refreshTokenExpiry = refreshTokenExpiry;
    }

    public Authentication getAuthentication(String token) {
        DecodedJWT decodedJWT = decodeJwt(token);
        verifyOrThrow(decodedJWT);
        String subject = decodedJWT.getSubject();

        UserDetails principal = User.builder()
                .username(subject)
                .build();
        return UsernamePasswordAuthenticationToken.authenticated(
                principal,
                "",
                null
        );
    }

    public void verifyOrThrow(DecodedJWT decodedJWT) {
        Algorithm algorithm = Algorithm.HMAC512(secret);
        JWTVerifier jwtVerifier = JWT.require(algorithm)
                .withIssuer(issuer)
                .build();

        jwtVerifier.verify(decodedJWT);
    }

    public DecodedJWT decodeJwt(String token) {
        return JWT.decode(token);
    }

    private String generateToken(int expiry, Integer userId) {
        if (Objects.isNull(userId)) {
            String str = null;
            return generateToken(expiry, str);
        }

        return generateToken(expiry, Integer.toString(userId));
    }

    private String generateToken(int expiry, String userId) {
        Instant now = Instant.now();
        Algorithm algorithm = Algorithm.HMAC512(secret);
        JWTCreator.Builder builder = JWT.create()
                .withIssuer(issuer)
                .withIssuedAt(now)
                .withExpiresAt(now.plusMillis(expiry));

        if (Objects.nonNull(userId)) {
            builder = builder
                    .withSubject(userId);
        }

        return builder.sign(algorithm);
    }

    private String generateToken(int expiry) {
        String str = null;
        return generateToken(expiry, str);
    }

    public String generateAccessToken(tech.kingoyster.spring_1.user.User user) {
        return generateAccessToken(Integer.toString(user.getId()));
    }

    public String generateAccessToken(String idStr) {
        return generateToken(accessTokenExpiry, idStr);
    }

    public String generateRefreshToken() {
        return generateToken(refreshTokenExpiry);
    }
}
