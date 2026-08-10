package com.example.myapp_backend.worker;

import com.example.myapp_backend.config.KafkaConfig;
import com.example.myapp_backend.model.Events;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Slf4j
@RequiredArgsConstructor
public class SagaCoordinator {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(topics = KafkaConfig.TOPIC_PAYMENT_PROCESSED, groupId = "saga-coordinator")
    public void handlePaymentProcessed(Events.PaymentProcessedEvent event) {
        log.info("SagaCoordinator: Handling PaymentProcessedEvent for order {}", event.getOrderId());

        kafkaTemplate.send(KafkaConfig.TOPIC_ORDER_COMPLETED, event.getOrderId(),
                new Events.OrderCompletedEvent(event.getOrderId()));

        kafkaTemplate.send(KafkaConfig.TOPIC_NOTIFICATION, event.getOrderId(),
                new Events.NotificationEvent(event.getOrderId(),
                        "SagaCoordinator: Order " + event.getOrderId() + " COMPLETED successfully",
                        "SUCCESS", LocalDateTime.now()));

        log.info("SagaCoordinator: Published OrderCompletedEvent and Notification for order {}", event.getOrderId());
    }

    @KafkaListener(topics = KafkaConfig.TOPIC_PAYMENT_FAILED, groupId = "saga-coordinator")
    public void handlePaymentFailed(Events.PaymentFailedEvent event) {
        log.info("SagaCoordinator: Handling PaymentFailedEvent for order {}", event.getOrderId());

        // Compensating transaction: Trigger stock release in Inventory worker via Kafka
        if (event.getItems() != null && !event.getItems().isEmpty()) {
            kafkaTemplate.send(KafkaConfig.TOPIC_RELEASE_STOCK, event.getOrderId(),
                    new Events.ReleaseStockEvent(event.getOrderId(), event.getItems()));
        }

        kafkaTemplate.send(KafkaConfig.TOPIC_ORDER_FAILED, event.getOrderId(),
                new Events.OrderFailedEvent(event.getOrderId(), event.getReason()));

        kafkaTemplate.send(KafkaConfig.TOPIC_NOTIFICATION, event.getOrderId(),
                new Events.NotificationEvent(event.getOrderId(),
                        "SagaCoordinator: Order " + event.getOrderId() + " FAILED: " + event.getReason() + " (Compensating action: stock released)",
                        "FAILURE", LocalDateTime.now()));

        log.info("SagaCoordinator: Published ReleaseStockEvent, OrderFailedEvent, and Notification for order {}", event.getOrderId());
    }

    @KafkaListener(topics = KafkaConfig.TOPIC_INVENTORY_FAILED, groupId = "saga-coordinator")
    public void handleInventoryFailed(Events.InventoryFailedEvent event) {
        log.info("SagaCoordinator: Handling InventoryFailedEvent for order {}", event.getOrderId());

        kafkaTemplate.send(KafkaConfig.TOPIC_ORDER_FAILED, event.getOrderId(),
                new Events.OrderFailedEvent(event.getOrderId(), event.getReason()));

        kafkaTemplate.send(KafkaConfig.TOPIC_NOTIFICATION, event.getOrderId(),
                new Events.NotificationEvent(event.getOrderId(),
                        "SagaCoordinator: Order " + event.getOrderId() + " FAILED at inventory stage: " + event.getReason(),
                        "FAILURE", LocalDateTime.now()));

        log.info("SagaCoordinator: Published OrderFailedEvent and Notification for order {}", event.getOrderId());
    }
}
