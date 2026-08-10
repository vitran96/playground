package com.example.myapp_backend.worker;

import com.example.myapp_backend.config.KafkaConfig;
import com.example.myapp_backend.model.Events;
import com.example.myapp_backend.model.Notification;
import com.example.myapp_backend.model.Order;
import com.example.myapp_backend.model.OrderItem;
import com.example.myapp_backend.repository.NotificationRepository;
import com.example.myapp_backend.repository.OrderRepository;
import com.example.myapp_backend.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class SagaCoordinator {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final NotificationRepository notificationRepository;

    @KafkaListener(topics = KafkaConfig.TOPIC_PAYMENT_PROCESSED, groupId = "saga-coordinator")
    public void handlePaymentProcessed(Events.PaymentProcessedEvent event) {
        orderRepository.findById(event.getOrderId()).ifPresent(order -> {
            order.setStatus(Order.OrderStatus.COMPLETED);
            orderRepository.save(order);

            notificationRepository.add(new Notification(UUID.randomUUID().toString(), order.getId(),
                    "SagaCoordinator: Order " + order.getId() + " COMPLETED successfully", "SUCCESS", LocalDateTime.now()));
            log.info("SagaCoordinator: Order {} COMPLETED", order.getId());
        });
    }

    @KafkaListener(topics = KafkaConfig.TOPIC_PAYMENT_FAILED, groupId = "saga-coordinator")
    public void handlePaymentFailed(Events.PaymentFailedEvent event) {
        orderRepository.findById(event.getOrderId()).ifPresent(order -> {
            // Compensating transaction: Release stock reserved in InventoryWorker
            if (order.getItems() != null) {
                for (OrderItem item : order.getItems()) {
                    productRepository.releaseStock(item.getProductId(), item.getQuantity());
                }
            }
            order.setStatus(Order.OrderStatus.FAILED);
            order.setFailureReason(event.getReason());
            orderRepository.save(order);

            notificationRepository.add(new Notification(UUID.randomUUID().toString(), order.getId(),
                    "SagaCoordinator: Order " + order.getId() + " FAILED: " + event.getReason() + " (Compensating action: stock released)", "FAILURE", LocalDateTime.now()));
            log.info("SagaCoordinator: Order {} FAILED (compensated stock)", order.getId());
        });
    }

    @KafkaListener(topics = KafkaConfig.TOPIC_INVENTORY_FAILED, groupId = "saga-coordinator")
    public void handleInventoryFailed(Events.InventoryFailedEvent event) {
        orderRepository.findById(event.getOrderId()).ifPresent(order -> {
            order.setStatus(Order.OrderStatus.FAILED);
            order.setFailureReason(event.getReason());
            orderRepository.save(order);

            notificationRepository.add(new Notification(UUID.randomUUID().toString(), order.getId(),
                    "SagaCoordinator: Order " + order.getId() + " FAILED at inventory stage: " + event.getReason(), "FAILURE", LocalDateTime.now()));
            log.info("SagaCoordinator: Order {} FAILED at inventory stage", order.getId());
        });
    }
}
