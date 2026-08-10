package com.example.myapp_backend.controller;

import com.example.myapp_backend.model.Notification;
import com.example.myapp_backend.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationRepository notificationRepository;

    @GetMapping
    public List<Notification> getNotifications() {
        log.info("NotificationController.getNotifications: Fetching all notifications");
        return notificationRepository.findAll();
    }

    @DeleteMapping
    public void clearNotifications() {
        log.info("NotificationController.clearNotifications: Clearing all notifications");
        notificationRepository.clear();
    }
}
