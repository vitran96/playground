package com.example.myapp_backend.controller;

import com.example.myapp_backend.model.CheckoutRequest;
import com.example.myapp_backend.model.Order;
import com.example.myapp_backend.repository.OrderRepository;
import com.example.myapp_backend.temporal.OrderSagaWorkflow;
import com.example.myapp_backend.temporal.TemporalConfig;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final WorkflowClient workflowClient;
    private final OrderRepository orderRepository;

    public OrderController(WorkflowClient workflowClient, OrderRepository orderRepository) {
        this.workflowClient = workflowClient;
        this.orderRepository = orderRepository;
    }

    @PostMapping("/checkout")
    public ResponseEntity<Order> checkout(@RequestBody CheckoutRequest request) {
        String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8);

        double totalAmount = request.getItems() != null ? request.getItems().stream()
                .mapToDouble(i -> i.getUnitPrice() * i.getQuantity())
                .sum() : 0.0;

        Order order = new Order(
                orderId,
                request.getUserId(),
                request.getItems(),
                totalAmount,
                Order.OrderStatus.PENDING,
                null,
                null,
                Instant.now()
        );

        orderRepository.save(order);

        WorkflowOptions workflowOptions = WorkflowOptions.newBuilder()
                .setTaskQueue(TemporalConfig.TASK_QUEUE)
                .setWorkflowId("checkout-workflow-" + orderId)
                .build();

        OrderSagaWorkflow workflow = workflowClient.newWorkflowStub(OrderSagaWorkflow.class, workflowOptions);

        WorkflowClient.start(workflow::executeCheckout, orderId, request);

        return ResponseEntity.accepted().body(order);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrder(@PathVariable String id) {
        return orderRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }
}
