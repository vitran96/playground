package tech.kingoyster.spring_1.authentication;

public interface AuthenticationService {
    LoginResponse authenticate(LoginRequest loginRequest);
    RefreshDto refreshToken(String refreshToken, String accessToken);
}
