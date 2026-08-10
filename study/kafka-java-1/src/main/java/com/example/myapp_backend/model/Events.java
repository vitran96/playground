package com.example.myapp_backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

public class Events {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderCreatedEvent {
        private String orderId;
        private List<OrderItem> items;
        private String simulateFailure;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InventoryReservedEvent {
        private String orderId;
        private List<OrderItem> items;
        private String simulateFailure;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InventoryFailedEvent {
        private String orderId;
        private String reason;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentProcessedEvent {
        private String orderId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentFailedEvent {
        private String orderId;
        private String reason;
        private List<OrderItem> items;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReleaseStockEvent {
        private String orderId;
        private List<OrderItem> items;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderCompletedEvent {
        private String orderId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderFailedEvent {
        private String orderId;
        private String reason;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NotificationEvent {
        private String orderId;
        private String message;
        private String type; // SUCCESS, FAILURE, RETRY, INFO
        private LocalDateTime timestamp;
    }
}

