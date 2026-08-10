package com.example.myapp_backend.repository;

import com.example.myapp_backend.model.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Repository
@Slf4j
public class NotificationRepository {
    private final List<Notification> notifications = new CopyOnWriteArrayList<>();

    public void add(Notification notification) {
        log.info("NotificationRepository.add: Adding notification {}", notification);
        notifications.add(0, notification); // Keep latest first
    }

    public List<Notification> findAll() {
        log.info("NotificationRepository.findAll: Retrieving all notifications");
        return new ArrayList<>(notifications);
    }

    public void clear() {
        log.info("NotificationRepository.clear: Clearing all notifications");
        notifications.clear();
    }
}
