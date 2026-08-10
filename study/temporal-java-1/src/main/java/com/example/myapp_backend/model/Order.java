package com.example.myapp_backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    public enum OrderStatus {
        PENDING,
        RESERVING_INVENTORY,
        PAYMENT_PROCESSING,
        COMPLETED,
        FAILED,
        CANCELLED
    }

    private String id;
    private String userId;
    private List<OrderItem> items;
    private double totalAmount;
    private OrderStatus status;
    private String failureReason;
    private String paymentId;
    private Instant createdAt;
}
