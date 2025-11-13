package tech.kingoyster.spring_1.authentication;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import tech.kingoyster.spring_1.exception.NotAuthenticatedException;
import tech.kingoyster.spring_1.exception.NotFoundException;
import tech.kingoyster.spring_1.exception.UserNotAuthenticatedException;
import tech.kingoyster.spring_1.user.User;
import tech.kingoyster.spring_1.user.UserRepository;
import tech.kingoyster.spring_1.user.UserSummary;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {
    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;

    @Override
    public LoginResponse authenticate(LoginRequest loginRequest) {
        User user = userRepository.findOneByEmail(loginRequest.username())
                .orElseThrow(() -> new NotFoundException("User " + loginRequest.username() + " not found!"));

        if (!passwordEncoder.matches(loginRequest.password(), user.getHashedPassword())) {
            throw new UserNotAuthenticatedException("User username or password is not correct!");
        }

        String refreshToken = jwtProvider.generateRefreshToken();
        String accessToken = jwtProvider.generateAccessToken(user.getId().toString());

        return LoginResponse.builder()
                .refreshToken(refreshToken)
                .accessToken(accessToken)
                .build();
    }

    @Override
    public RefreshDto refreshToken(String refreshToken, String accessToken) {
        if (!canRefresh(refreshToken)) {
            throw new NotAuthenticatedException("Refresh token");
        }

        String subject = jwtProvider.decodeJwt(accessToken).getSubject();
        String newAccessToken = jwtProvider.generateAccessToken(subject);
        return RefreshDto.builder()
                .accessToken(newAccessToken)
                .build();
    }

    private boolean canRefresh(String token) {
        try {
            DecodedJWT decodedJWT = jwtProvider.decodeJwt(token);
            jwtProvider.verifyOrThrow(decodedJWT);
            return true;
        } catch (JWTVerificationException e) {
            return false;
        }
    }
}
