package com.example.myapp_backend.controller;

import com.example.myapp_backend.model.Notification;
import com.example.myapp_backend.repository.NotificationRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
}
