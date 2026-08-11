package com.example.myapp_backend.temporal;

import com.example.myapp_backend.model.OrderItem;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

import java.util.List;

@ActivityInterface
public interface InventoryActivities {

    @ActivityMethod
    void reserveInventory(String orderId, List<OrderItem> items, boolean simulateFailure);

    @ActivityMethod
    void releaseInventory(String orderId, List<OrderItem> items);
}
