package tech.kingoyster.spring_1.authentication;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.MySQLContainer;
import tech.kingoyster.spring_1.MySqlTestConfiguration;
import tech.kingoyster.spring_1.user.AdminProperties;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(MySqlTestConfiguration.class)
public class AuthControllerIntegrationTest {

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private AdminProperties adminProperties;

    @Autowired
    private MySQLContainer<?> mySQLContainer;

    @Test
    public void shouldLoginSuperAdmin() {
        var res = testRestTemplate.postForObject(
                "/api/v1/auth/login",
                new LoginRequest(adminProperties.getEmail(), adminProperties.getPassword()),
                LoginResponse.class
        );

        Assertions.assertNotNull(res);
        Assertions.assertNotNull(res.accessToken());
        Assertions.assertNotNull(res.refreshToken());

    }
}
