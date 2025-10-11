package tech.kingoyster.spring_1.service;

import tech.kingoyster.spring_1.model.Customer;

import java.util.List;
import java.util.Optional;

public interface ICustomerService {
    List<Customer> getAll();

    Customer getById(Long id);

    Customer create(Customer customer);

    void deleteById(Long id);
}
