package com.example.myapp_backend.temporal;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class PaymentActivitiesImpl implements PaymentActivities {

    @Override
    public String processPayment(String orderId, double amount, boolean simulateFailure) {
        if (simulateFailure) {
            throw new RuntimeException("Simulated payment failure for order " + orderId);
        }

        return "PAY-" + UUID.randomUUID().toString().substring(0, 8);
    }

    @Override
    public void refundPayment(String orderId, String paymentId) {
        if (paymentId != null) {
            System.out.println("Payment " + paymentId + " refunded for order " + orderId);
        }
    }
}
