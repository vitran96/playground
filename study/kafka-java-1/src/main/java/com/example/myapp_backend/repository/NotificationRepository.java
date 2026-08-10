package com.example.myapp_backend.repository;

import com.example.myapp_backend.model.Notification;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Repository
public class NotificationRepository {
    private final List<Notification> notifications = new CopyOnWriteArrayList<>();

    public void add(Notification notification) {
        notifications.add(0, notification); // Keep latest first
    }

    public List<Notification> findAll() {
        return new ArrayList<>(notifications);
    }
}
