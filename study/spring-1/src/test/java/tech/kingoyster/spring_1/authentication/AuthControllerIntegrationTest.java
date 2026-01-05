package tech.kingoyster.spring_1.authentication;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tech.kingoyster.spring_1.MySqlTestConfiguration;
import tech.kingoyster.spring_1.user.AdminProperties;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(MySqlTestConfiguration.class)
//@AutoConfigureWebTestClient
public class AuthControllerIntegrationTest {

//    @Autowired
//    private WebTestClient webTestClient;

    @Autowired
    private AdminProperties adminProperties;

    @Autowired
    private MySQLContainer<?> mySQLContainer;

    @Test
    public void shouldLoginSuperAdmin() {
//        webTestClient.post()
//                .uri("/api/v1/auth/login")
//                .bodyValue(new LoginRequest(adminProperties.getEmail(), adminProperties.getPassword()))
//                .exchange()
//                .expectStatus().isOk()
//                .expectBody(LoginResponse.class);
    }
}
