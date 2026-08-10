package com.example.myapp_backend.controller;

import com.example.myapp_backend.config.KafkaConfig;
import com.example.myapp_backend.model.Events;
import com.example.myapp_backend.model.Notification;
import com.example.myapp_backend.model.Order;
import com.example.myapp_backend.model.OrderItem;
import com.example.myapp_backend.repository.NotificationRepository;
import com.example.myapp_backend.repository.OrderRepository;
import com.example.myapp_backend.repository.ProductRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final NotificationRepository notificationRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Data
    public static class CreateOrderRequest {
        private String customerId;
        private List<OrderItemRequest> items;
        private String simulateFailure; // "NONE", "INVENTORY", "PAYMENT"
    }

    @Data
    public static class OrderItemRequest {
        private String productId;
        private int quantity;
    }

    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody CreateOrderRequest req) {
        String orderId = UUID.randomUUID().toString();
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        if (req.getItems() != null) {
            for (OrderItemRequest itemReq : req.getItems()) {
                BigDecimal unitPrice = productRepository.findById(itemReq.getProductId())
                        .map(p -> p.getPrice())
                        .orElse(BigDecimal.ZERO);

                OrderItem item = new OrderItem(itemReq.getProductId(), itemReq.getQuantity(), unitPrice);
                orderItems.add(item);
                totalAmount = totalAmount.add(unitPrice.multiply(BigDecimal.valueOf(itemReq.getQuantity())));
            }
        }

        Order order = new Order(
                orderId,
                req.getCustomerId() != null ? req.getCustomerId() : "CUST-001",
                orderItems,
                totalAmount,
                Order.OrderStatus.PENDING,
                null,
                req.getSimulateFailure() != null ? req.getSimulateFailure() : "NONE",
                LocalDateTime.now()
        );

        orderRepository.save(order);

        notificationRepository.add(new Notification(
                UUID.randomUUID().toString(),
                orderId,
                "Order created (PENDING). Triggering Saga...",
                "INFO",
                LocalDateTime.now()
        ));

        // Publish OrderCreated event to Kafka
        kafkaTemplate.send(KafkaConfig.TOPIC_ORDER_CREATED, orderId,
                new Events.OrderCreatedEvent(orderId, orderItems, order.getSimulateFailure()));

        return ResponseEntity.ok(order);
    }

    @GetMapping
    public Collection<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable String id) {
        return orderRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
