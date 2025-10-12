package tech.kingoyster.spring_1.customer;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import tech.kingoyster.spring_1.customer.impl.CustomerServiceImpl;

import java.util.List;

@ExtendWith(MockitoExtension.class)
public class CustomerServiceTest {

    @InjectMocks
    private CustomerServiceImpl customerService;

    @Mock
    private CustomerRepository customerRepository;

//    @Before
//    public void setup() {
//        MockitoAnnotations.openMocks(this);
//    }

    @Test
    public void returnEmptyListOfUser() {
        Mockito.when(customerRepository.findAll()).thenReturn(List.of());

        List<Customer> list = customerService.getAll();

        Assert.assertEquals(0, list.size());
    }
}
