package com.example.myapp_backend.controller;

import com.example.myapp_backend.model.Notification;
import com.example.myapp_backend.model.SendNotificationRequest;
import com.example.myapp_backend.repository.NotificationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationRepository notificationRepository;

    public NotificationController(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @GetMapping
    public List<Notification> getNotifications(@RequestParam(required = false) String userId) {
        if (userId != null && !userId.isBlank()) {
            return notificationRepository.findByUserId(userId);
        }
        return notificationRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Notification> getNotificationById(@PathVariable String id) {
        return notificationRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Notification> sendNotification(@RequestBody SendNotificationRequest request) {
        Notification notification = new Notification(
                UUID.randomUUID().toString(),
                request.getUserId(),
                request.getOrderId(),
                request.getMessage(),
                request.getType() != null ? request.getType() : "INFO",
                Instant.now()
        );
        notificationRepository.save(notification);
        return ResponseEntity.status(HttpStatus.CREATED).body(notification);
    }
}
