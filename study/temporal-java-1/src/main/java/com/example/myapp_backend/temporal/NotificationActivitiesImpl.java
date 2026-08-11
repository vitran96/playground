package com.example.myapp_backend.temporal;

import com.example.myapp_backend.model.Notification;
import com.example.myapp_backend.repository.NotificationRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class NotificationActivitiesImpl implements NotificationActivities {

    private final NotificationRepository notificationRepository;

    public NotificationActivitiesImpl(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
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
