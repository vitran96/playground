package tech.kingoyster.spring_1.customer;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import tech.kingoyster.spring_1.MySqlTestConfiguration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(MySqlTestConfiguration.class)
public class CustomerControllerIT {
}
