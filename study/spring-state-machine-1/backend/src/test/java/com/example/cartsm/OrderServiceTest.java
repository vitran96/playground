package com.example.cartsm;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class OrderServiceTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository repository;

    @Test
    void createAddItemsCheckoutPayCycle() {
        Order order = orderService.create();
        assertThat(order.getState()).isEqualTo(OrderState.CART);
        assertThat(order.getItemCount()).isEqualTo(0);

        orderService.send(order.getId(), OrderEvent.ADD_ITEM);
        orderService.send(order.getId(), OrderEvent.ADD_ITEM);

        Order checkedOut = orderService.send(order.getId(), OrderEvent.CHECKOUT);
        assertThat(checkedOut.getState()).isEqualTo(OrderState.CHECKED_OUT);
        assertThat(checkedOut.getItemCount()).isEqualTo(2);

        Order paid = orderService.send(order.getId(), OrderEvent.PAY);
        assertThat(paid.getState()).isEqualTo(OrderState.PAID);
        assertThat(paid.getItemCount()).isEqualTo(2);

        Order reloaded = repository.findById(order.getId()).orElseThrow();
        assertThat(reloaded.getState()).isEqualTo(OrderState.PAID);
        assertThat(reloaded.getItemCount()).isEqualTo(2);
    }

    @Test
    void shipFromCartThrowsIllegalTransition() {
        Order order = orderService.create();

        assertThatThrownBy(() -> orderService.send(order.getId(), OrderEvent.SHIP))
                .isInstanceOf(IllegalTransitionException.class)
                .hasMessageContaining("Event SHIP is not allowed from state CART");

        Order reloaded = repository.findById(order.getId()).orElseThrow();
        assertThat(reloaded.getState()).isEqualTo(OrderState.CART);
    }

    @Test
    void checkoutEmptyCartThrowsIllegalTransition() {
        Order order = orderService.create();

        assertThatThrownBy(() -> orderService.send(order.getId(), OrderEvent.CHECKOUT))
                .isInstanceOf(IllegalTransitionException.class)
                .hasMessageContaining("Event CHECKOUT is not allowed from state CART");

        Order reloaded = repository.findById(order.getId()).orElseThrow();
        assertThat(reloaded.getState()).isEqualTo(OrderState.CART);
    }
}
