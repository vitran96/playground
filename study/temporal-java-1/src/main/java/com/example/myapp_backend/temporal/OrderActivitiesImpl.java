package com.example.myapp_backend.temporal;

import com.example.myapp_backend.model.Order;
import com.example.myapp_backend.repository.OrderRepository;
import org.springframework.stereotype.Component;

@Component
public class OrderActivitiesImpl implements OrderActivities {

    private final OrderRepository orderRepository;

    public OrderActivitiesImpl(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public void updateOrderStatus(String orderId, Order.OrderStatus status) {
        orderRepository.findById(orderId).ifPresent(order -> {
            order.setStatus(status);
            orderRepository.save(order);
        });
    }

    @Override
    public void recordPayment(String orderId, String paymentId) {
        orderRepository.findById(orderId).ifPresent(order -> {
            order.setPaymentId(paymentId);
            orderRepository.save(order);
        });
    }

    @Override
    public void confirmOrder(String orderId) {
        orderRepository.findById(orderId).ifPresent(order -> {
            order.setStatus(Order.OrderStatus.COMPLETED);
            orderRepository.save(order);
        });
    }

    @Override
    public void failOrder(String orderId, String reason) {
        orderRepository.findById(orderId).ifPresent(order -> {
            order.setStatus(Order.OrderStatus.FAILED);
            order.setFailureReason(reason);
            orderRepository.save(order);
        });
    }
}
