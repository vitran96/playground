package com.example.myapp_backend;

import com.example.myapp_backend.model.OrderItem;
import com.example.myapp_backend.model.Product;
import com.example.myapp_backend.repository.NotificationRepository;
import com.example.myapp_backend.repository.OrderRepository;
import com.example.myapp_backend.repository.ProductRepository;
import com.example.myapp_backend.temporal.OrderActivitiesImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ShoppingBackendTest {

    private ProductRepository productRepository;
    private OrderRepository orderRepository;
    private NotificationRepository notificationRepository;
    private OrderActivitiesImpl activities;

    @BeforeEach
    void setUp() {
        productRepository = new ProductRepository();
        productRepository.init();
        orderRepository = new OrderRepository();
        notificationRepository = new NotificationRepository();
        activities = new OrderActivitiesImpl(productRepository, orderRepository, notificationRepository);
    }

    @Test
    void testSuccessfulInventoryReservationAndRelease() {
        List<OrderItem> items = List.of(new OrderItem("prod-1", 2, 1200.0));
        activities.reserveInventory("ORD-123", items, false);

        Product prod = productRepository.findById("prod-1").orElseThrow();
        assertEquals(8, prod.getStockQuantity());

        activities.releaseInventory("ORD-123", items);
        assertEquals(10, prod.getStockQuantity());
    }

    @Test
    void testInventoryFailureTriggersException() {
        List<OrderItem> items = List.of(new OrderItem("prod-1", 2, 1200.0));
        assertThrows(RuntimeException.class, () ->
                activities.reserveInventory("ORD-456", items, true));
    }

    @Test
    void testPaymentFailureTriggersException() {
        assertThrows(RuntimeException.class, () ->
                activities.processPayment("ORD-789", 100.0, true));
    }
}
