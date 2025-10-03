package tech.kingoyster.spring_1.service;

import tech.kingoyster.spring_1.model.Customers;

import java.util.List;
import java.util.Optional;

public interface ICustomersService {
    List<Customers> getAll();

    Optional<Customers> getById(Long id);

    Customers create(Customers customer);

    void deleteById(Long id);
}
