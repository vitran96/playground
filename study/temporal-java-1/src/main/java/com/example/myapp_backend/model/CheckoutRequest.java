package com.example.myapp_backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutRequest {
    private String userId;
    private List<OrderItem> items;
    private boolean simulatePaymentFailure;
    private boolean simulateInventoryFailure;
}
