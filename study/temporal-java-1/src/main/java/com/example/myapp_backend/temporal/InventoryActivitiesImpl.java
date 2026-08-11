package com.example.myapp_backend.temporal;

import com.example.myapp_backend.model.OrderItem;
import com.example.myapp_backend.repository.ProductRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InventoryActivitiesImpl implements InventoryActivities {

    private final ProductRepository productRepository;

    public InventoryActivitiesImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public void reserveInventory(String orderId, List<OrderItem> items, boolean simulateFailure) {
        if (simulateFailure) {
            throw new RuntimeException("Simulated inventory failure for order " + orderId);
        }

        if (items != null) {
            for (OrderItem item : items) {
                boolean reserved = productRepository.reserveStock(item.getProductId(), item.getQuantity());
                if (!reserved) {
                    throw new RuntimeException("Insufficient stock for product: " + item.getProductId());
                }
            }
        }
    }

    @Override
    public void releaseInventory(String orderId, List<OrderItem> items) {
        if (items != null) {
            for (OrderItem item : items) {
                productRepository.restoreStock(item.getProductId(), item.getQuantity());
            }
        }
    }
}
