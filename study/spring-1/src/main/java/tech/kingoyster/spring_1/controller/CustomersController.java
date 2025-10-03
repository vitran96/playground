package tech.kingoyster.spring_1.controller;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.kingoyster.spring_1.model.Customers;
import tech.kingoyster.spring_1.service.ICustomersService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomersController {
    private final ICustomersService customersService;

    @GetMapping
    public List<Customers> getAll() {
        return customersService.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Customers> getById(@PathVariable Long id) {
        return customersService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Customers create(@RequestBody Customers customer) {
        return customersService.create(customer);
    }

    @DeleteMapping("/{id}")
    public void deleteById(@PathVariable Long id) {
        customersService.deleteById(id);
    }
}
