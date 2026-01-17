package tech.kingoyster.spring_1.customer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import tech.kingoyster.spring_1.TestUtils;
import tech.kingoyster.spring_1.exception.CustomerAlreadyExistsException;
import tech.kingoyster.spring_1.exception.NotFoundException;

@ExtendWith(MockitoExtension.class)
public class CustomerServiceTest {

    @InjectMocks private CustomerServiceImpl customerService;

    @Mock private CustomerRepository customerRepository;

    @Test
    public void whenGetAll_thenReturnEmpty() {
        Mockito.when(customerRepository.findAll()).thenReturn(List.of());

        List<Customer> list = customerService.getAll();

        Assertions.assertEquals(0, list.size());
    }

    @Test
    public void whenGetAll_thenReturn2Customers() {
        Mockito.when(customerRepository.findAll())
                .thenReturn(
                        List.of(
                                new Customer(
                                        1, "customer1", "customer1@gmail.com", LocalDateTime.now()),
                                new Customer(
                                        2,
                                        "customer2",
                                        "customer2@gmail.com",
                                        LocalDateTime.now())));

        List<Customer> list = customerService.getAll();

        Assertions.assertEquals(2, list.size());
        Assertions.assertEquals(2, list.get(1).getId());
        Assertions.assertEquals("customer2", list.get(1).getName());
        Assertions.assertEquals("customer2@gmail.com", list.get(1).getEmail());
    }

    @ParameterizedTest
    @MethodSource("randomNumbers")
    public void whenGetNonExistsCustomer_thenThrowError(int randomId) {
        Mockito.when(customerRepository.findById(Mockito.anyInt())).thenReturn(Optional.empty());

        Assertions.assertThrows(NotFoundException.class, () -> customerService.getById(randomId));
    }

    @Test
    public void whenGetCustomerById_thenReturnTheCustomer() {
        Mockito.when(customerRepository.findById(Mockito.anyInt()))
                .thenReturn(
                        Optional.of(
                                new Customer(
                                        5,
                                        "customer5",
                                        "customer5@gmail.com",
                                        LocalDateTime.now())));

        Customer customer = customerService.getById(5);

        Assertions.assertEquals(5, customer.getId());
        Assertions.assertEquals("customer5", customer.getName());
        Assertions.assertEquals("customer5@gmail.com", customer.getEmail());
    }

    @Test
    public void whenCreateNewCustomer_thenCustomerIsCreated() {
        LocalDateTime now = LocalDateTime.now();
        Mockito.when(customerRepository.save(Mockito.any()))
                .thenReturn(new Customer(44, "customer4", "customer4@gmail.com", now));

        Customer customer =
                customerService.create(
                        new Customer(null, "customer4", "customer4@gmail.com", null));

        Assertions.assertEquals(44, customer.getId());
        Assertions.assertEquals("customer4", customer.getName());
        Assertions.assertEquals("customer4@gmail.com", customer.getEmail());
        Assertions.assertEquals(now, customer.getRegisteredAt());
    }

    @Test
    public void whenCreateExistedCustomerByEmail_thenThrowError() {
        Mockito.when(customerRepository.save(Mockito.any()))
                .thenThrow(DataIntegrityViolationException.class);

        Assertions.assertThrows(
                CustomerAlreadyExistsException.class,
                () ->
                        customerService.create(
                                new Customer(null, "customer5", "customer5@gmail.com", null)));
    }

    // TODO: anyway to move this outside?
    static Stream<Integer> randomNumbers() {
        return TestUtils.randomNumbers();
    }

    @ParameterizedTest
    @MethodSource("randomNumbers")
    public void whenDeleteCustomerById_thenCustomerDeleted(int randomId) {
        Mockito.when(customerRepository.findById(Mockito.anyInt()))
                .thenReturn(
                        Optional.of(
                                new Customer(
                                        5,
                                        "customer5",
                                        "customer5@gmail.com",
                                        LocalDateTime.now())));

        customerService.deleteById(randomId);

        Mockito.verify(customerRepository, Mockito.times(1)).deleteById(Mockito.anyInt());
    }

    @ParameterizedTest
    @MethodSource("randomNumbers")
    public void whenDeleteNonExistCustomer_thenThrowError(int randomId) {
        Mockito.when(customerRepository.findById(Mockito.anyInt())).thenReturn(Optional.empty());

        Assertions.assertThrows(
                NotFoundException.class, () -> customerService.deleteById(randomId));
    }
}
