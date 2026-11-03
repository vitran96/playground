package com.example.cartsm;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class OrderRepositoryTest {

    @Autowired
    private OrderRepository repository;

    @Test
    void savesAndReloadsOrder() {
        Order order = new Order(OrderState.CART, 2);
        Order saved = repository.save(order);

        assertThat(saved.getId()).isNotNull();

        Order reloaded = repository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getState()).isEqualTo(OrderState.CART);
        assertThat(reloaded.getItemCount()).isEqualTo(2);
        assertThat(reloaded.getUpdatedAt()).isNotNull();
    }
}
