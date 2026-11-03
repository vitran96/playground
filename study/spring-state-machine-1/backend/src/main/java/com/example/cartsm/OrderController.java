package com.example.cartsm;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public OrderResponse create() {
        Order order = orderService.create();
        return OrderResponse.from(order, orderService.allowedEvents(order.getState()));
    }

    @GetMapping
    public List<OrderResponse> listAll() {
        return orderService.listAll().stream()
                .map(o -> OrderResponse.from(o, orderService.allowedEvents(o.getState())))
                .toList();
    }

    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable Long id) {
        Order order = orderService.get(id);
        return OrderResponse.from(order, orderService.allowedEvents(order.getState()));
    }

    @PostMapping("/{id}/events/{event}")
    public OrderResponse sendEvent(@PathVariable Long id, @PathVariable OrderEvent event) {
        Order order = orderService.send(id, event);
        return OrderResponse.from(order, orderService.allowedEvents(order.getState()));
    }
}
