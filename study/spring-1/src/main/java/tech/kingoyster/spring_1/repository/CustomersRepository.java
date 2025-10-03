package tech.kingoyster.spring_1.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tech.kingoyster.spring_1.model.Customers;

@Repository
public interface CustomersRepository extends JpaRepository<Customers, Long> {
}
