package com.example.myapp_backend;

import com.example.myapp_backend.controller.OrderController;
import com.example.myapp_backend.model.Notification;
import com.example.myapp_backend.model.Order;
import com.example.myapp_backend.model.Product;
import com.example.myapp_backend.repository.NotificationRepository;
import com.example.myapp_backend.repository.OrderRepository;
import com.example.myapp_backend.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class SagaIntegrationTest {

    @Autowired
    private OrderController orderController;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private com.example.myapp_backend.controller.NotificationController notificationController;

    @BeforeEach
    public void setup() throws InterruptedException {
        // Give Kafka consumers time to complete group assignment
        Thread.sleep(2000);
    }

    @Test
    public void testSuccessfulSagaFlow() {
        OrderController.CreateOrderRequest req = new OrderController.CreateOrderRequest();
        req.setCustomerId("USER-1");
        OrderController.OrderItemRequest item = new OrderController.OrderItemRequest();
        item.setProductId("P101");
        item.setQuantity(1);
        req.setItems(List.of(item));
        req.setSimulateFailure("NONE");

        ResponseEntity<Order> response = orderController.createOrder(req);
        Order order = response.getBody();
        assertNotNull(order);
        assertEquals(Order.OrderStatus.PENDING, order.getStatus());

        // Wait for Saga to complete via Kafka
        await().atMost(20, TimeUnit.SECONDS).untilAsserted(() -> {
            Order updated = orderRepository.findById(order.getId()).orElse(null);
            assertNotNull(updated);
            assertEquals(Order.OrderStatus.COMPLETED, updated.getStatus());
        });

        // Verify notification saved
        List<Notification> notifications = notificationRepository.findAll();
        assertFalse(notifications.isEmpty());
    }

    @Test
    public void testPaymentFailureAndCompensationSagaFlow() {
        Product initialP102 = productRepository.findById("P102").orElseThrow();
        int initialStock = initialP102.getStockQuantity();

        OrderController.CreateOrderRequest req = new OrderController.CreateOrderRequest();
        req.setCustomerId("USER-2");
        OrderController.OrderItemRequest item = new OrderController.OrderItemRequest();
        item.setProductId("P102");
        item.setQuantity(2);
        req.setItems(List.of(item));
        req.setSimulateFailure("PAYMENT");

        ResponseEntity<Order> response = orderController.createOrder(req);
        Order order = response.getBody();
        assertNotNull(order);

        // Wait for retries + DLT + Compensation
        await().atMost(20, TimeUnit.SECONDS).untilAsserted(() -> {
            Order updated = orderRepository.findById(order.getId()).orElse(null);
            assertNotNull(updated);
            assertEquals(Order.OrderStatus.FAILED, updated.getStatus());
        });

        // Check compensating transaction released stock back
        Product finalP102 = productRepository.findById("P102").orElseThrow();
        assertEquals(initialStock, finalP102.getStockQuantity());
    }

    @Test
    public void testClearNotifications() {
        notificationRepository.add(new Notification("test-id", "order-1", "Test Msg", "INFO", java.time.LocalDateTime.now()));
        assertFalse(notificationController.getNotifications().isEmpty());

        notificationController.clearNotifications();
        assertTrue(notificationController.getNotifications().isEmpty());
    }
}
