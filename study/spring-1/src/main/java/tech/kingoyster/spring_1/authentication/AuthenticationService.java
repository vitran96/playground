package tech.kingoyster.spring_1.authentication;

public interface AuthenticationService {
    LoginResponse authenticate(LoginRequest loginRequest);
//    boolean canRefresh(String token);
//    int getUserIdFromAccessToken(String accessToken);
    RefreshDto refreshToken(String refreshToken, String accessToken);
}
