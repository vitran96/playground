package tech.kingoyster.spring_1.authentication;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class JwtProviderTest {
    @InjectMocks private JwtProvider jwtProvider;

    // TODO: expired token
    // TODO: malformed token
    // TODO: create/refresh token
}
