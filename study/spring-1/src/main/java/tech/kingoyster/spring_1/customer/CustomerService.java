package tech.kingoyster.spring_1.customer;

import java.util.List;

public interface CustomerService {
    List<Customer> getAll();

    Customer getById(Integer id);

    Customer create(Customer customer);

    void deleteById(Integer id);
}
