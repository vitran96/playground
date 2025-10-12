package tech.kingoyster.spring_1.customer.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import tech.kingoyster.spring_1.exception.NotFoundException;
import tech.kingoyster.spring_1.exception.CustomerAlreadyExistsException;
import tech.kingoyster.spring_1.customer.Customer;
import tech.kingoyster.spring_1.customer.CustomerRepository;
import tech.kingoyster.spring_1.customer.CustomerService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {
    private final CustomerRepository customerRepository;

    @Override
    public List<Customer> getAll() {
        return customerRepository.findAll();
    }

    @Override
    public Customer getById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User " + id + " not found!"));
    }

    @Override
    public Customer create(Customer customer) {
        try {
            return customerRepository.save(customer);
        } catch (DataIntegrityViolationException e) {
            throw new CustomerAlreadyExistsException(e);
        }
    }

    @Transactional
    @Override
    public void deleteById(Long id) {
        this.getById(id);
        customerRepository.deleteById(id);
    }
}
