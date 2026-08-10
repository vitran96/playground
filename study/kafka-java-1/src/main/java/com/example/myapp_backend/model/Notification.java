package com.example.myapp_backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    private String id;
    private String orderId;
    private String message;
    private String type; // SUCCESS, FAILURE, RETRY
    private LocalDateTime timestamp;
}
