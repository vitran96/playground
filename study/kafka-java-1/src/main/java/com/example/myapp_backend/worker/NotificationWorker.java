package com.example.myapp_backend.worker;

import com.example.myapp_backend.config.KafkaConfig;
import com.example.myapp_backend.model.Events;
import com.example.myapp_backend.model.Notification;
import com.example.myapp_backend.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationWorker {

    private final NotificationRepository notificationRepository;

    @KafkaListener(topics = KafkaConfig.TOPIC_NOTIFICATION, groupId = "notification-group")
    public void handleNotification(Events.NotificationEvent event) {
        log.info("NotificationWorker: Recording notification for order {}", event.getOrderId());
        notificationRepository.add(new Notification(
                UUID.randomUUID().toString(),
                event.getOrderId(),
                event.getMessage(),
                event.getType(),
                event.getTimestamp() != null ? event.getTimestamp() : LocalDateTime.now()
        ));
    }
}
