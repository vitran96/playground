package com.example.cartsm;

import java.util.List;

public record OrderResponse(
        Long id,
        OrderState state,
        int itemCount,
        List<OrderEvent> allowedEvents
) {
    public static OrderResponse from(Order order, List<OrderEvent> allowedEvents) {
        return new OrderResponse(order.getId(), order.getState(), order.getItemCount(), allowedEvents);
    }
}
