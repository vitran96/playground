package com.example.myapp_backend.worker;

import com.example.myapp_backend.config.KafkaConfig;
import com.example.myapp_backend.model.Events;
import com.example.myapp_backend.model.Order;
import com.example.myapp_backend.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderWorker {

    private final OrderRepository orderRepository;

    @KafkaListener(topics = KafkaConfig.TOPIC_ORDER_COMPLETED, groupId = "order-group")
    public void handleOrderCompleted(Events.OrderCompletedEvent event) {
        log.info("OrderWorker: Processing OrderCompletedEvent for order {}", event.getOrderId());
        orderRepository.findById(event.getOrderId()).ifPresent(order -> {
            order.setStatus(Order.OrderStatus.COMPLETED);
            orderRepository.save(order);
            log.info("OrderWorker: Order {} updated to COMPLETED", order.getId());
        });
    }

    @KafkaListener(topics = KafkaConfig.TOPIC_ORDER_FAILED, groupId = "order-group")
    public void handleOrderFailed(Events.OrderFailedEvent event) {
        log.info("OrderWorker: Processing OrderFailedEvent for order {}", event.getOrderId());
        orderRepository.findById(event.getOrderId()).ifPresent(order -> {
            order.setStatus(Order.OrderStatus.FAILED);
            order.setFailureReason(event.getReason());
            orderRepository.save(order);
            log.info("OrderWorker: Order {} updated to FAILED", order.getId());
        });
    }
}
