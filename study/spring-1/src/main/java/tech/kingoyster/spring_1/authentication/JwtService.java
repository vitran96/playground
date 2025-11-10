package tech.kingoyster.spring_1.authentication;

import tech.kingoyster.spring_1.user.UserSummary;

public interface JwtService {
    boolean isExpired(String token);
    String generateAccessToken(UserTokenData userTokenData, long expiry);
    String generateRefreshToken(long expiry);
}
