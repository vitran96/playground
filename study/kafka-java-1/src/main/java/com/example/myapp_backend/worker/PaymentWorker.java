package com.example.myapp_backend.worker;

import com.example.myapp_backend.config.KafkaConfig;
import com.example.myapp_backend.model.Events;
import com.example.myapp_backend.model.Notification;
import com.example.myapp_backend.repository.NotificationRepository;
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
public class PaymentWorker {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final NotificationRepository notificationRepository;

    @RetryableTopic(attempts = "3", backOff = @BackOff(delay = 1000))
    @KafkaListener(topics = KafkaConfig.TOPIC_INVENTORY_RESERVED, groupId = "payment-group")
    public void processInventoryReserved(Events.InventoryReservedEvent event) {
        log.info("PaymentWorker: Processing payment for order {}", event.getOrderId());

        if ("PAYMENT".equalsIgnoreCase(event.getSimulateFailure())) {
            notificationRepository.add(new Notification(UUID.randomUUID().toString(), event.getOrderId(),
                    "PaymentWorker: Attempt failed (Simulated failure). Retrying...", "RETRY", LocalDateTime.now()));
            throw new RuntimeException("Simulated payment failure for order " + event.getOrderId());
        }

        log.info("PaymentWorker: Payment successful for order {}", event.getOrderId());
        notificationRepository.add(new Notification(UUID.randomUUID().toString(), event.getOrderId(),
                "PaymentWorker: Payment processed successfully", "SUCCESS", LocalDateTime.now()));

        kafkaTemplate.send(KafkaConfig.TOPIC_PAYMENT_PROCESSED, event.getOrderId(),
                new Events.PaymentProcessedEvent(event.getOrderId()));
    }

    @DltHandler
    public void handleDlt(Events.InventoryReservedEvent event) {
        log.error("PaymentWorker DLT: Order {} failed after 3 attempts", event.getOrderId());
        kafkaTemplate.send(KafkaConfig.TOPIC_PAYMENT_FAILED, event.getOrderId(),
                new Events.PaymentFailedEvent(event.getOrderId(), "Payment processing failed after 3 attempts"));
    }
}
