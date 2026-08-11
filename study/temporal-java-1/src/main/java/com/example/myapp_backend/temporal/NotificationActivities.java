package com.example.myapp_backend.temporal;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface NotificationActivities {

    @ActivityMethod
    void sendNotification(String userId, String orderId, String message, String type);
}
