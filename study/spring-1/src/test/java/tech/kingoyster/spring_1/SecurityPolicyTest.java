package tech.kingoyster.spring_1;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tech.kingoyster.spring_1.authentication.AuthenticationController;
import tech.kingoyster.spring_1.authentication.JwtProvider;
import tech.kingoyster.spring_1.user.UserRepository;

@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none", // Don't validate or create
})
@AutoConfigureMockMvc
public class SecurityPolicyTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtProvider jwtProvider;

    @Test
    void whenNoToken_thenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/data"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void whenValidToken_thenReturnsSuccess() throws Exception {
        // Arrange: Tell the mock service to return a valid username for this specific string
        String fakeToken = "valid.jwt.token";
        Mockito.when(jwtProvider.extractUsername(fakeToken)).thenReturn("john_doe");
        Mockito.when(jwtProvider.isTokenValid(eq(fakeToken), any())).thenReturn(true);

        // Act & Assert
        mockMvc.perform(get("/api/data")
                                .header("Authorization", "Bearer " + fakeToken))
                .andExpect(status().isOk());
    }

    @Test
    void whenMalformedToken_thenReturnsUnauthorized() throws Exception {
        // Arrange: Simulate a service failure/exception for bad tokens
        Mockito.when(jwtProvider.extractUsername("bad-token")).thenThrow(new RuntimeException("Invalid Token"));

        // Act & Assert
        mockMvc.perform(get("/api/data")
                                .header("Authorization", "Bearer bad-token"))
                .andExpect(status().isUnauthorized());
    }
}
