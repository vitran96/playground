package com.example.myapp_backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    public enum OrderStatus {
        PENDING,
        INVENTORY_RESERVED,
        COMPLETED,
        FAILED
    }

    private String id;
    private String customerId;
    private List<OrderItem> items;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private String failureReason;
    private String simulateFailure; // "NONE", "INVENTORY", "PAYMENT"
    private LocalDateTime createdAt;
}
