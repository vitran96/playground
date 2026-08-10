package com.example.myapp_backend.temporal;

import com.example.myapp_backend.model.OrderItem;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

import java.util.List;

@ActivityInterface
public interface OrderActivities {

    @ActivityMethod
    void reserveInventory(String orderId, List<OrderItem> items, boolean simulateFailure);

    @ActivityMethod
    void releaseInventory(String orderId, List<OrderItem> items);

    @ActivityMethod
    String processPayment(String orderId, double amount, boolean simulateFailure);

    @ActivityMethod
    void refundPayment(String orderId, String paymentId);

    @ActivityMethod
    void confirmOrder(String orderId);

    @ActivityMethod
    void failOrder(String orderId, String reason);

    @ActivityMethod
    void sendNotification(String userId, String orderId, String message, String type);
}
