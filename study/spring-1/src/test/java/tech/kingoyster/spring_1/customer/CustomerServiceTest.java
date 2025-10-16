package tech.kingoyster.spring_1.customer;

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
import tech.kingoyster.spring_1.exception.CustomerAlreadyExistsException;
import tech.kingoyster.spring_1.exception.NotFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
public class CustomerServiceTest {

    @InjectMocks
    private CustomerServiceImpl customerService;

    @Mock
    private CustomerRepository customerRepository;

    @Test
    public void getAllreturnEmptyListOfUser() {
        Mockito.when(customerRepository.findAll()).thenReturn(List.of());

        List<Customer> list = customerService.getAll();

        Assertions.assertEquals(0, list.size());
    }

    @Test
    public void getAllReturnListOf2Customers() {
        Mockito.when(customerRepository.findAll()).thenReturn(
                List.of(
                        new Customer(
                                1,
                                "customer1",
                                "customer1@gmail.com",
                                LocalDateTime.now()
                        ),
                        new Customer(
                                2,
                                "customer2",
                                "customer2@gmail.com",
                                LocalDateTime.now()
                        )
                )
        );

        List<Customer> list = customerService.getAll();

        Assertions.assertEquals(2, list.size());
        Assertions.assertEquals(2, list.get(1).getId());
        Assertions.assertEquals("customer2", list.get(1).getName());
        Assertions.assertEquals("customer2@gmail.com", list.get(1).getEmail());
    }

    static Stream<Long> randomNumbers() {
        Random random = new Random();
        return Stream.generate(() -> random.nextLong(100)) // Generate random integers up to 99
                .limit(10); // Generate 10 random numbers
    }

    @ParameterizedTest
    @MethodSource("randomNumbers")
    public void getNonExistsCustomer(long randomId) {
        Mockito.when(customerRepository.findById(Mockito.anyLong())).thenReturn(Optional.empty());

        Assertions.assertThrows(NotFoundException.class, () -> customerService.getById(randomId));
    }

    @Test
    public void get1Customer() {
        Mockito.when(customerRepository.findById(Mockito.anyLong()))
                .thenReturn(
                        Optional.of(
                                new Customer(
                                    5,
                                    "customer5",
                                    "customer5@gmail.com",
                                    LocalDateTime.now()
                            )
                        )
                );

        Customer customer = customerService.getById(5L);

        Assertions.assertEquals(5, customer.getId());
        Assertions.assertEquals("customer5", customer.getName());
        Assertions.assertEquals("customer5@gmail.com", customer.getEmail());
    }

    @Test
    public void createNewCustomer() {
        LocalDateTime now = LocalDateTime.now();
        Mockito.when(customerRepository.save(Mockito.any()))
                .thenReturn(
                    new Customer(
                            44,
                            "customer4",
                            "customer4@gmail.com",
                            now
                    )
                );

        Customer customer = customerService.create(
                new Customer(
                        null,
                        "customer4",
                        "customer4@gmail.com",
                        null
                )
        );

        Assertions.assertEquals(44, customer.getId());
        Assertions.assertEquals("customer4", customer.getName());
        Assertions.assertEquals("customer4@gmail.com", customer.getEmail());
        Assertions.assertEquals(now, customer.getRegisteredAt());
    }

    @Test
    public void createCustomerWithConflictEmail() {
        Mockito.when(customerRepository.save(Mockito.any()))
                .thenThrow(DataIntegrityViolationException.class);

        Assertions.assertThrows(CustomerAlreadyExistsException.class, () -> customerService.create(new Customer(null, "customer5", "customer5@gmail.com", null)));
    }

    @ParameterizedTest
    @MethodSource("randomNumbers")
    public void delete1Customer(long randomId) {
        Mockito.when(customerRepository.findById(Mockito.anyLong()))
                .thenReturn(
                        Optional.of(
                                new Customer(
                                        5,
                                        "customer5",
                                        "customer5@gmail.com",
                                        LocalDateTime.now()
                                )
                        )
                );

        customerService.deleteById(randomId);

        Mockito.verify(customerRepository, Mockito.times(1)).deleteById(Mockito.anyLong());
    }

    @ParameterizedTest
    @MethodSource("randomNumbers")
    public void deleteNonExistsCustomer(long randomId) {
        Mockito.when(customerRepository.findById(Mockito.anyLong())).thenReturn(Optional.empty());

        Assertions.assertThrows(NotFoundException.class, () -> customerService.deleteById(randomId));
    }
}
