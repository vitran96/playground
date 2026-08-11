package com.example.myapp_backend.temporal;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface PaymentActivities {

    @ActivityMethod
    String processPayment(String orderId, double amount, boolean simulateFailure);

    @ActivityMethod
    void refundPayment(String orderId, String paymentId);
}
