package tech.kingoyster.spring_1.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tech.kingoyster.spring_1.model.Customer;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
}
