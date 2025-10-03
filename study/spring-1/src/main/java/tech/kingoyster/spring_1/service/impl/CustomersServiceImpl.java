package tech.kingoyster.spring_1.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tech.kingoyster.spring_1.repository.CustomersRepository;
import tech.kingoyster.spring_1.service.ICustomersService;

@RequiredArgsConstructor
@Service
public class CustomersServiceImpl implements ICustomersService {
    private final CustomersRepository customersRepository;
}
