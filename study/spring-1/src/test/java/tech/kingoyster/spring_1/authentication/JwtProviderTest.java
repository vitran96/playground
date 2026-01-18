package tech.kingoyster.spring_1.authentication;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;

@ExtendWith(MockitoExtension.class)
public class JwtProviderTest {
    @InjectMocks private JwtProvider jwtProvider;

    @Test
    void whenParsingExpiredToken_thenThrowError() {
        Assertions.fail("not implement");
    }

    @Test
    void whenParsingValidToken_thenReturnAuthentication() {
        Assertions.fail("not implement");
    }

    @Test
    void whenParsingMalformedToken_thenThrowError() {
        Assertions.fail("not implement");
    }

    @Test
    void whenCreateAccessToken_thenReturnToken() {
        Assertions.fail("not implement");
    }

    @Test
    void whenCreateRefreshToken_thenReturnToken() {
        Assertions.fail("not implement");
    }

    @Test
    void whenParsingTokenAfterSecretChange_thenThrowError() {
        Assertions.fail("not implement");
    }
}
