package com.example.cartsm;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.test.StateMachineTestPlan;
import org.springframework.statemachine.test.StateMachineTestPlanBuilder;

@SpringBootTest
public class OrderStateMachineTest {

    @Autowired
    private StateMachineFactory<OrderState, OrderEvent> factory;

    @Test
    void happyPathCartToDelivered() throws Exception {
        StateMachine<OrderState, OrderEvent> machine = factory.getStateMachine();

        StateMachineTestPlan<OrderState, OrderEvent> plan =
                StateMachineTestPlanBuilder.<OrderState, OrderEvent>builder()
                        .defaultAwaitTime(2)
                        .stateMachine(machine)
                        .step()
                            .expectState(OrderState.CART)
                            .and()
                        .step()
                            .sendEvent(OrderEvent.ADD_ITEM)
                            .expectState(OrderState.CART)
                            .expectVariable("itemCount", 1)
                            .and()
                        .step()
                            .sendEvent(OrderEvent.CHECKOUT)
                            .expectState(OrderState.CHECKED_OUT)
                            .and()
                        .step()
                            .sendEvent(OrderEvent.PAY)
                            .expectState(OrderState.PAID)
                            .and()
                        .step()
                            .sendEvent(OrderEvent.SHIP)
                            .expectState(OrderState.SHIPPED)
                            .and()
                        .step()
                            .sendEvent(OrderEvent.DELIVER)
                            .expectState(OrderState.DELIVERED)
                            .and()
                        .build();

        plan.test();
    }

    @Test
    void cancelFromCart() throws Exception {
        StateMachine<OrderState, OrderEvent> machine = factory.getStateMachine();

        StateMachineTestPlan<OrderState, OrderEvent> plan =
                StateMachineTestPlanBuilder.<OrderState, OrderEvent>builder()
                        .defaultAwaitTime(2)
                        .stateMachine(machine)
                        .step()
                            .expectState(OrderState.CART)
                            .and()
                        .step()
                            .sendEvent(OrderEvent.CANCEL)
                            .expectState(OrderState.CANCELLED)
                            .and()
                        .build();

        plan.test();
    }

    @Test
    void cancelFromCheckedOut() throws Exception {
        StateMachine<OrderState, OrderEvent> machine = factory.getStateMachine();

        StateMachineTestPlan<OrderState, OrderEvent> plan =
                StateMachineTestPlanBuilder.<OrderState, OrderEvent>builder()
                        .defaultAwaitTime(2)
                        .stateMachine(machine)
                        .step()
                            .sendEvent(OrderEvent.ADD_ITEM)
                            .expectState(OrderState.CART)
                            .and()
                        .step()
                            .sendEvent(OrderEvent.CHECKOUT)
                            .expectState(OrderState.CHECKED_OUT)
                            .and()
                        .step()
                            .sendEvent(OrderEvent.CANCEL)
                            .expectState(OrderState.CANCELLED)
                            .and()
                        .build();

        plan.test();
    }

    @Test
    void cancelFromPaid() throws Exception {
        StateMachine<OrderState, OrderEvent> machine = factory.getStateMachine();

        StateMachineTestPlan<OrderState, OrderEvent> plan =
                StateMachineTestPlanBuilder.<OrderState, OrderEvent>builder()
                        .defaultAwaitTime(2)
                        .stateMachine(machine)
                        .step()
                            .sendEvent(OrderEvent.ADD_ITEM)
                            .expectState(OrderState.CART)
                            .and()
                        .step()
                            .sendEvent(OrderEvent.CHECKOUT)
                            .expectState(OrderState.CHECKED_OUT)
                            .and()
                        .step()
                            .sendEvent(OrderEvent.PAY)
                            .expectState(OrderState.PAID)
                            .and()
                        .step()
                            .sendEvent(OrderEvent.CANCEL)
                            .expectState(OrderState.CANCELLED)
                            .and()
                        .build();

        plan.test();
    }

    @Test
    void cancelDeniedFromShipped() throws Exception {
        StateMachine<OrderState, OrderEvent> machine = factory.getStateMachine();

        StateMachineTestPlan<OrderState, OrderEvent> plan =
                StateMachineTestPlanBuilder.<OrderState, OrderEvent>builder()
                        .defaultAwaitTime(2)
                        .stateMachine(machine)
                        .step()
                            .sendEvent(OrderEvent.ADD_ITEM)
                            .sendEvent(OrderEvent.CHECKOUT)
                            .sendEvent(OrderEvent.PAY)
                            .sendEvent(OrderEvent.SHIP)
                            .expectState(OrderState.SHIPPED)
                            .and()
                        .step()
                            .sendEvent(OrderEvent.CANCEL)
                            .expectState(OrderState.SHIPPED)
                            .expectEventNotAccepted(1)
                            .and()
                        .build();

        plan.test();
    }

    @Test
    void checkoutDeniedAtZeroItems() throws Exception {
        StateMachine<OrderState, OrderEvent> machine = factory.getStateMachine();

        StateMachineTestPlan<OrderState, OrderEvent> plan =
                StateMachineTestPlanBuilder.<OrderState, OrderEvent>builder()
                        .defaultAwaitTime(2)
                        .stateMachine(machine)
                        .step()
                            .expectState(OrderState.CART)
                            .and()
                        .step()
                            .sendEvent(OrderEvent.CHECKOUT)
                            .expectState(OrderState.CART)
                            .and()
                        .build();

        plan.test();
    }
}
