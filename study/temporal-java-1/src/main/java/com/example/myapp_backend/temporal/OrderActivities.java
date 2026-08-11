package com.example.myapp_backend.temporal;

import com.example.myapp_backend.model.Order;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface OrderActivities {

    @ActivityMethod
    void updateOrderStatus(String orderId, Order.OrderStatus status);

    @ActivityMethod
    void recordPayment(String orderId, String paymentId);

    @ActivityMethod
    void confirmOrder(String orderId);

    @ActivityMethod
    void failOrder(String orderId, String reason);
}
