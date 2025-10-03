package tech.kingoyster.spring_1.controller;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.web.bind.annotation.*;
import tech.kingoyster.spring_1.model.Customers;
import tech.kingoyster.spring_1.service.ICustomersService;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/customers")
public class CustomersController {
    private final ICustomersService customersService;

    @GetMapping
    public List<Customers> getAll() {
        throw new NotImplementedException();
    }

    @GetMapping("/{id}")
    public Customers getById(@PathVariable Long id) {
        throw new NotImplementedException();
    }

    @PostMapping
    public Customers create(@RequestBody Customers customer) {
        throw new NotImplementedException();
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        throw new NotImplementedException();
    }
}
