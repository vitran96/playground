package tech.kingoyster.spring_1.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tech.kingoyster.spring_1.model.Customers;
import tech.kingoyster.spring_1.repository.CustomersRepository;
import tech.kingoyster.spring_1.service.ICustomersService;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomersServiceImpl implements ICustomersService {
    private final CustomersRepository customersRepository;

    @Override
    public List<Customers> getAll() {
        return customersRepository.findAll();
    }

    @Override
    public Optional<Customers> getById(Long id) {
        return customersRepository.findById(id);
    }

    @Override
    public Customers create(Customers customer) {
        return customersRepository.save(customer);
    }

    @Override
    public void deleteById(Long id) {
        customersRepository.deleteById(id);
    }
}
