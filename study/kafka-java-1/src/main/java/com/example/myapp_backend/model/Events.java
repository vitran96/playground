package com.example.myapp_backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    }
}
