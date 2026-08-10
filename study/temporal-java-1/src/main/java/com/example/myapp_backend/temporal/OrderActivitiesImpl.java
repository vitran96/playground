package com.example.myapp_backend.temporal;

import com.example.myapp_backend.model.Notification;
import com.example.myapp_backend.model.Order;
import com.example.myapp_backend.model.OrderItem;
import com.example.myapp_backend.repository.NotificationRepository;
import com.example.myapp_backend.repository.OrderRepository;
import com.example.myapp_backend.repository.ProductRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class OrderActivitiesImpl implements OrderActivities {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final NotificationRepository notificationRepository;

    public OrderActivitiesImpl(ProductRepository productRepository,
                               OrderRepository orderRepository,
                               NotificationRepository notificationRepository) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.notificationRepository = notificationRepository;
    }

    @Override
    public void reserveInventory(String orderId, List<OrderItem> items, boolean simulateFailure) {
        if (simulateFailure) {
            throw new RuntimeException("Simulated inventory failure for order " + orderId);
        }

        orderRepository.findById(orderId).ifPresent(order -> {
            order.setStatus(Order.OrderStatus.RESERVING_INVENTORY);
            orderRepository.save(order);
        });

        for (OrderItem item : items) {
            boolean reserved = productRepository.reserveStock(item.getProductId(), item.getQuantity());
            if (!reserved) {
                throw new RuntimeException("Insufficient stock for product: " + item.getProductId());
            }
        }
    }

    @Override
    public void releaseInventory(String orderId, List<OrderItem> items) {
        for (OrderItem item : items) {
            productRepository.restoreStock(item.getProductId(), item.getQuantity());
        }
    }

    @Override
    public String processPayment(String orderId, double amount, boolean simulateFailure) {
        if (simulateFailure) {
            throw new RuntimeException("Simulated payment failure for order " + orderId);
        }

        orderRepository.findById(orderId).ifPresent(order -> {
            order.setStatus(Order.OrderStatus.PAYMENT_PROCESSING);
            orderRepository.save(order);
        });

        String paymentId = "PAY-" + UUID.randomUUID().toString().substring(0, 8);
        orderRepository.findById(orderId).ifPresent(order -> {
            order.setPaymentId(paymentId);
            orderRepository.save(order);
        });
        return paymentId;
    }

    @Override
    public void refundPayment(String orderId, String paymentId) {
        if (paymentId != null) {
            System.out.println("Payment " + paymentId + " refunded for order " + orderId);
        }
    }

    @Override
    public void confirmOrder(String orderId) {
        orderRepository.findById(orderId).ifPresent(order -> {
            order.setStatus(Order.OrderStatus.COMPLETED);
            orderRepository.save(order);
        });
    }

    @Override
    public void failOrder(String orderId, String reason) {
        orderRepository.findById(orderId).ifPresent(order -> {
            order.setStatus(Order.OrderStatus.FAILED);
            order.setFailureReason(reason);
            orderRepository.save(order);
        });
    }

    @Override
    public void sendNotification(String userId, String orderId, String message, String type) {
        Notification notification = new Notification(
                UUID.randomUUID().toString(),
                userId,
                orderId,
                message,
                type,
                Instant.now()
        );
        notificationRepository.save(notification);
    }
}
