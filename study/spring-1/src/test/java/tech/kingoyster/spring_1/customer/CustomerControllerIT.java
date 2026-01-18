package tech.kingoyster.spring_1.customer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.MySQLContainer;
import tech.kingoyster.spring_1.MySqlTestConfiguration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(MySqlTestConfiguration.class)
public class CustomerControllerIT {
//    TODO: use mock mvc
    @Autowired private MySQLContainer<?> mySQLContainer;

    @Test
    public void whenGetAll_thenReturnEmpty() {
        Assertions.fail("not implemented");
    }

    @Test
    public void whenGetAll_thenReturn2Customers() {
        Assertions.fail("not implemented");
    }

    @Test
    public void whenGetNonExistsCustomer_thenThrowError(int randomId) {
        Assertions.fail("not implemented");
    }

    @Test
    public void whenGetCustomerById_thenReturnTheCustomer() {
        Assertions.fail("not implemented");
    }

    @Test
    public void whenCreateNewCustomer_thenCustomerIsCreated() {
        Assertions.fail("not implemented");
    }

    @Test
    public void whenCreateExistedCustomerByEmail_thenThrowError() {
        Assertions.fail("not implemented");
    }

    @Test
    public void whenDeleteCustomerById_thenCustomerDeleted() {
        Assertions.fail("not implemented");
    }

    @Test
    public void whenDeleteNonExistCustomer_thenThrowError() {
        Assertions.fail("not implemented");
    }
}
