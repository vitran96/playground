package com.example.myapp_backend.worker;

import com.example.myapp_backend.config.KafkaConfig;
import com.example.myapp_backend.model.Events;
import com.example.myapp_backend.model.Notification;
import com.example.myapp_backend.model.OrderItem;
import com.example.myapp_backend.repository.NotificationRepository;
import com.example.myapp_backend.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class InventoryWorker {

    private final ProductRepository productRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final NotificationRepository notificationRepository;

    @RetryableTopic(attempts = "3", backOff = @BackOff(delay = 1000))
    @KafkaListener(topics = KafkaConfig.TOPIC_ORDER_CREATED, groupId = "inventory-group")
    public void processOrderCreated(Events.OrderCreatedEvent event) {
        log.info("InventoryWorker: Processing OrderCreatedEvent for order {}", event.getOrderId());

        if ("INVENTORY".equalsIgnoreCase(event.getSimulateFailure())) {
            notificationRepository.add(new Notification(UUID.randomUUID().toString(), event.getOrderId(),
                    "InventoryWorker: Attempt failed (Simulated failure). Retrying...", "RETRY", LocalDateTime.now()));
            throw new RuntimeException("Simulated inventory failure for order " + event.getOrderId());
        }

        if (event.getItems() != null) {
            for (OrderItem item : event.getItems()) {
                boolean reserved = productRepository.reserveStock(item.getProductId(), item.getQuantity());
                if (!reserved) {
                    notificationRepository.add(new Notification(UUID.randomUUID().toString(), event.getOrderId(),
                            "InventoryWorker: Stock reservation failed for product " + item.getProductId() + ". Retrying...", "RETRY", LocalDateTime.now()));
                    throw new RuntimeException("Insufficient stock for product " + item.getProductId());
                }
            }
        }

        log.info("InventoryWorker: Successfully reserved stock for order {}", event.getOrderId());
        notificationRepository.add(new Notification(UUID.randomUUID().toString(), event.getOrderId(),
                "InventoryWorker: Stock reserved successfully", "SUCCESS", LocalDateTime.now()));

        kafkaTemplate.send(KafkaConfig.TOPIC_INVENTORY_RESERVED, event.getOrderId(),
                new Events.InventoryReservedEvent(event.getOrderId(), event.getSimulateFailure()));
    }

    @DltHandler
    public void handleDlt(Events.OrderCreatedEvent event) {
        log.error("InventoryWorker DLT: Order {} failed after 3 attempts", event.getOrderId());
        kafkaTemplate.send(KafkaConfig.TOPIC_INVENTORY_FAILED, event.getOrderId(),
                new Events.InventoryFailedEvent(event.getOrderId(), "Inventory reservation failed after 3 attempts"));
    }
}
