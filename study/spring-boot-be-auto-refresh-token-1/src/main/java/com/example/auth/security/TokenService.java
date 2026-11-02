package com.example.auth.security;

import com.example.auth.config.AppProperties;
import com.example.auth.session.RefreshSession;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

@Service
public class TokenService {

    private static final Logger log = LoggerFactory.getLogger(TokenService.class);

    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final Clock clock;
    private final Duration accessTtl;

    public TokenService(AppProperties props, Clock clock) {
        this.clock = clock;
        this.accessTtl = props.auth().accessTtl();
        SecretKey key = signingKey(props.auth().jwtSecret());
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        NimbusJwtDecoder nimbus = NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256).build();
        var timestamps = new JwtTimestampValidator(Duration.ZERO);
        timestamps.setClock(clock);
        nimbus.setJwtValidator(timestamps);
        this.decoder = nimbus;
    }

    /** Access token never outlives its refresh session. */
    public Jwt issue(RefreshSession session) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(accessTtl);
        if (session.expiresAt().isBefore(expiresAt)) expiresAt = session.expiresAt();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(session.username())
                .claim("sid", session.id().toString())
                .claim("roles", session.roles())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims));
    }

    /** Empty when missing, malformed, wrongly signed or expired. */
    public Optional<Jwt> decode(String token) {
        if (token == null || token.isBlank()) return Optional.empty();
        try {
            return Optional.of(decoder.decode(token));
        } catch (JwtException e) {
            return Optional.empty();
        }
    }

    private static SecretKey signingKey(String secret) {
        byte[] bytes;
        if (secret == null || secret.isBlank()) {
            bytes = new byte[32];
            new SecureRandom().nextBytes(bytes);
            log.warn("app.auth.jwt-secret is blank: using a random key; tokens end on restart");
        } else {
            bytes = secret.getBytes(StandardCharsets.UTF_8);
            if (bytes.length < 32) {
                throw new IllegalStateException("app.auth.jwt-secret must be at least 32 bytes");
            }
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }
}
