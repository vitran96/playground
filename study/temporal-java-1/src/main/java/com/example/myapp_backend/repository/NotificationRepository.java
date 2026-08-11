package com.example.myapp_backend.repository;

import com.example.myapp_backend.model.Notification;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

@Repository
public class NotificationRepository {
    private final List<Notification> notifications = new CopyOnWriteArrayList<>();

    public void save(Notification notification) {
        notifications.add(0, notification);
    }

    public Optional<Notification> findById(String id) {
        return notifications.stream()
                .filter(n -> n.getId().equals(id))
                .findFirst();
    }

    public List<Notification> findAll() {
        return new ArrayList<>(notifications);
    }

    public List<Notification> findByUserId(String userId) {
        return notifications.stream()
                .filter(n -> n.getUserId().equals(userId))
                .toList();
    }
}
