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
public class JwtProviderImpl implements JwtProvider {
    private final String secret;
    private final String issuer;
    private final int accessTokenExpiry;
    private final int refreshTokenExpiry;

    public JwtProviderImpl(
            @Value("${spring.application.name}") String issuer,
            JwtProperties jwtProperties
    ) {
        this.issuer = issuer;
        this.secret = jwtProperties.getSecret();
        this.accessTokenExpiry = jwtProperties.getAccess().getExpiry();
        this.refreshTokenExpiry = jwtProperties.getRefresh().getExpiry();
    }

    @Override
    public String generateToken(int expiry, String subject) {
        Instant now = Instant.now();
        Algorithm algorithm = Algorithm.HMAC512(secret);
        JWTCreator.Builder builder = JWT.create()
                .withIssuer(issuer)
                .withIssuedAt(now)
                .withExpiresAt(now.plusMillis(expiry));

        if (Objects.nonNull(subject)) {
            builder = builder
                    .withSubject(subject);
        }

        return builder.sign(algorithm);
    }

    @Override
    public Authentication getAuthentication(String token) {
        DecodedJWT decodedJWT = decodeJwt(token);
        verifyOrThrow(decodedJWT);
        String subject = decodedJWT.getSubject();

        UserDetails principal = User.builder()
                .username(subject)
                // Password cannot be null
                .password("")
                .build();
        return UsernamePasswordAuthenticationToken.authenticated(
                principal,
                "",
                null
        );
    }

    @Override
    public void verifyOrThrow(DecodedJWT decodedJWT) {
        Algorithm algorithm = Algorithm.HMAC512(secret);
        JWTVerifier jwtVerifier = JWT.require(algorithm)
                .withIssuer(issuer)
                .build();

        jwtVerifier.verify(decodedJWT);
    }

    @Override
    public DecodedJWT decodeJwt(String token) {
        return JWT.decode(token);
    }

    @Override
    public String generateAccessToken(String idStr) {
        return generateToken(accessTokenExpiry, idStr);
    }

    @Override
    public String generateRefreshToken(String idStr) {
        return generateToken(refreshTokenExpiry, idStr);
    }
}
