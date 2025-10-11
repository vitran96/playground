package tech.kingoyster.spring_1.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.stereotype.Service;
import tech.kingoyster.spring_1.exception.NotFoundException;
import tech.kingoyster.spring_1.exception.UserAlreadyExistsException;
import tech.kingoyster.spring_1.model.Customer;
import tech.kingoyster.spring_1.repository.CustomerRepository;
import tech.kingoyster.spring_1.service.ICustomerService;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements ICustomerService {
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
            throw new UserAlreadyExistsException(e);
        }
    }

    @Transactional
    @Override
    public void deleteById(Long id) {
        this.getById(id);
        customerRepository.deleteById(id);
    }
}
