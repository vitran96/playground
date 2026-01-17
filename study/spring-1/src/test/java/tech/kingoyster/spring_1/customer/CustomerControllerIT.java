package tech.kingoyster.spring_1.customer;

import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.MySQLContainer;
import tech.kingoyster.spring_1.MySqlTestConfiguration;
import tech.kingoyster.spring_1.TestUtils;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(MySqlTestConfiguration.class)
public class CustomerControllerIT {
    @Autowired private MySQLContainer<?> mySQLContainer;

    @Test
    public void whenGetAll_thenReturnEmpty() {
        Assertions.fail("not implemented");
    }

    @Test
    public void whenGetAll_thenReturn2Customers() {
        Assertions.fail("not implemented");
    }

    @ParameterizedTest
    @MethodSource("randomNumbers")
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

    // TODO: anyway to move this outside?
    static Stream<Integer> randomNumbers() {
        return TestUtils.randomNumbers();
    }

    @ParameterizedTest
    @MethodSource("randomNumbers")
    public void whenDeleteCustomerById_thenCustomerDeleted(int randomId) {
        Assertions.fail("not implemented");
    }

    @ParameterizedTest
    @MethodSource("randomNumbers")
    public void whenDeleteNonExistCustomer_thenThrowError(int randomId) {
        Assertions.fail("not implemented");
    }
}
