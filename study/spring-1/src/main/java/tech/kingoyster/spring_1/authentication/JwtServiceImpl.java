package tech.kingoyster.spring_1.authentication;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtServiceImpl implements JwtService {
    private final String secret;

    public JwtServiceImpl(
            @Value("${app.jwt.secret}") String secret
    ) {
        this.secret = secret;
    }

    @Override
    public boolean isExpired(String token) {
        return false;
    }

    @Override
    public String generateAccessToken(UserTokenData userTokenData, long expiry) {
        return "";
    }

    @Override
    public String generateRefreshToken(long expiry) {
        return "";
    }
}
