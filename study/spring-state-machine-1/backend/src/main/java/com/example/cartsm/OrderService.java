package com.example.cartsm;

import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.StateMachineEventResult;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

@Service
public class OrderService {

    private final OrderRepository repository;
    private final StateMachineFactory<OrderState, OrderEvent> factory;

    public OrderService(OrderRepository repository, StateMachineFactory<OrderState, OrderEvent> factory) {
        this.repository = repository;
        this.factory = factory;
    }

    public Order create() {
        return repository.save(new Order(OrderState.CART, 0));
    }

    public List<Order> listAll() {
        return repository.findAllByOrderByIdDesc();
    }

    public Order get(Long id) {
        return repository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
    }

    public List<OrderEvent> allowedEvents(OrderState state) {
        StateMachine<OrderState, OrderEvent> machine = factory.getStateMachine();
        return machine.getTransitions().stream()
                .filter(t -> t.getSource() != null && t.getSource().getId() == state)
                .map(t -> t.getTrigger() != null ? t.getTrigger().getEvent() : null)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    @Transactional
    public Order send(Long id, OrderEvent event) {
        Order order = get(id);
        StateMachine<OrderState, OrderEvent> machine = factory.getStateMachine(String.valueOf(id));

        machine.getStateMachineAccessor().doWithAllRegions(a ->
                a.resetStateMachineReactively(new DefaultStateMachineContext<>(order.getState(), null, null, null)).block()
        );

        java.util.concurrent.atomic.AtomicBoolean transitioned = new java.util.concurrent.atomic.AtomicBoolean(false);
        var listener = new org.springframework.statemachine.listener.StateMachineListenerAdapter<OrderState, OrderEvent>() {
            @Override
            public void transition(org.springframework.statemachine.transition.Transition<OrderState, OrderEvent> transition) {
                transitioned.set(true);
            }
        };
        machine.addStateListener(listener);

        machine.getExtendedState().getVariables().put("itemCount", order.getItemCount());
        machine.startReactively().block();

        List<StateMachineEventResult<OrderState, OrderEvent>> results =
                machine.sendEvent(Mono.just(MessageBuilder.withPayload(event).build()))
                        .collectList()
                        .block();

        boolean eventAccepted = results != null && !results.isEmpty() &&
                results.stream().allMatch(r -> r.getResultType() == StateMachineEventResult.ResultType.ACCEPTED);

        if (!eventAccepted || !transitioned.get()) {
            machine.stopReactively().block();
            throw new IllegalTransitionException("Event " + event + " is not allowed from state " + order.getState());
        }

        OrderState newState = machine.getState().getId();
        Integer newCount = machine.getExtendedState().get("itemCount", Integer.class);

        order.setState(newState);
        if (newCount != null) {
            order.setItemCount(newCount);
        }

        Order saved = repository.save(order);
        machine.stopReactively().block();
        return saved;
    }
}
