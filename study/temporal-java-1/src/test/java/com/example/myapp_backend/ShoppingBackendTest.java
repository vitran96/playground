package com.example.myapp_backend;

import com.example.myapp_backend.model.Order;
import com.example.myapp_backend.model.OrderItem;
import com.example.myapp_backend.model.Product;
import com.example.myapp_backend.repository.NotificationRepository;
import com.example.myapp_backend.repository.OrderRepository;
import com.example.myapp_backend.repository.ProductRepository;
import com.example.myapp_backend.temporal.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ShoppingBackendTest {

    private ProductRepository productRepository;
    private OrderRepository orderRepository;
    private NotificationRepository notificationRepository;

    private InventoryActivitiesImpl inventoryActivities;
    private PaymentActivitiesImpl paymentActivities;
    private OrderActivitiesImpl orderActivities;
    private NotificationActivitiesImpl notificationActivities;

    @BeforeEach
    void setUp() {
        productRepository = new ProductRepository();
        productRepository.init();
        orderRepository = new OrderRepository();
        notificationRepository = new NotificationRepository();

        inventoryActivities = new InventoryActivitiesImpl(productRepository);
        paymentActivities = new PaymentActivitiesImpl();
        orderActivities = new OrderActivitiesImpl(orderRepository);
        notificationActivities = new NotificationActivitiesImpl(notificationRepository);
    }

    @Test
    void testSuccessfulInventoryReservationAndRelease() {
        List<OrderItem> items = List.of(new OrderItem("prod-1", 2, 1200.0));
        inventoryActivities.reserveInventory("ORD-123", items, false);

        Product prod = productRepository.findById("prod-1").orElseThrow();
        assertEquals(8, prod.getStockQuantity());

        inventoryActivities.releaseInventory("ORD-123", items);
        assertEquals(10, prod.getStockQuantity());
    }

    @Test
    void testInventoryFailureTriggersException() {
        List<OrderItem> items = List.of(new OrderItem("prod-1", 2, 1200.0));
        assertThrows(RuntimeException.class, () ->
                inventoryActivities.reserveInventory("ORD-456", items, true));
    }

    @Test
    void testPaymentFailureTriggersException() {
        assertThrows(RuntimeException.class, () ->
                paymentActivities.processPayment("ORD-789", 100.0, true));
    }

    @Test
    void testOrderAndNotificationDomainIsolation() {
        Order order = new Order("ORD-001", "user-1", List.of(), 100.0, Order.OrderStatus.PENDING, null, null, null);
        orderRepository.save(order);

        orderActivities.updateOrderStatus("ORD-001", Order.OrderStatus.COMPLETED);
        assertEquals(Order.OrderStatus.COMPLETED, orderRepository.findById("ORD-001").orElseThrow().getStatus());

        notificationActivities.sendNotification("user-1", "ORD-001", "Order placed", "INFO");
        assertEquals(1, notificationRepository.findByUserId("user-1").size());
    }
}
