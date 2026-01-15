package tech.kingoyster.spring_1;

import com.auth0.jwt.exceptions.JWTDecodeException;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import tech.kingoyster.spring_1.authentication.AuthenticationController;
import tech.kingoyster.spring_1.authentication.JwtProvider;
import tech.kingoyster.spring_1.user.UserRepository;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@SpringBootTest(properties = {
        "spring.liquibase.enabled=false", // Don't run migration
        "spring.jpa.hibernate.ddl-auto=none", // Don't validate JPA
        "spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect",
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
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/customers"))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

//    @Test
//    void whenValidToken_thenReturnsSuccess() throws Exception {
//        // Arrange
//        String fakeToken = "valid.jwt.token";
//        Mockito.when(jwtProvider.getAuthentication(fakeToken)).thenReturn("john_doe");
//
//        // Act & Assert
//        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/customers")
//                                .header("Authorization", "Bearer " + fakeToken))
//                .andExpect(MockMvcResultMatchers.status().isOk());
//    }

    @Test
    void whenMalformedToken_thenReturnsUnauthorized() throws Exception {
        // Arrange
        Mockito.when(jwtProvider.getAuthentication("bad-token")).thenThrow(new JWTDecodeException("Invalid Token"));

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/customers")
                                .header("Authorization", "Bearer bad-token"))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }
}
