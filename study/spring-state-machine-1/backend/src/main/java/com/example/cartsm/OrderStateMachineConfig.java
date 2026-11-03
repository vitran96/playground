package com.example.cartsm;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.guard.Guard;

import java.util.EnumSet;

@Configuration
@EnableStateMachineFactory
public class OrderStateMachineConfig extends EnumStateMachineConfigurerAdapter<OrderState, OrderEvent> {

    @Override
    public void configure(StateMachineConfigurationConfigurer<OrderState, OrderEvent> config) throws Exception {
        config.withConfiguration()
                .autoStartup(false);
    }

    @Override
    public void configure(StateMachineStateConfigurer<OrderState, OrderEvent> states) throws Exception {
        states.withStates()
                .initial(OrderState.CART)
                .states(EnumSet.allOf(OrderState.class));
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<OrderState, OrderEvent> transitions) throws Exception {
        transitions
                .withInternal()
                    .source(OrderState.CART)
                    .event(OrderEvent.ADD_ITEM)
                    .action(addItemAction())
                    .and()
                .withExternal()
                    .source(OrderState.CART).target(OrderState.CHECKED_OUT)
                    .event(OrderEvent.CHECKOUT)
                    .guard(checkoutGuard())
                    .and()
                .withExternal()
                    .source(OrderState.CHECKED_OUT).target(OrderState.PAID)
                    .event(OrderEvent.PAY)
                    .and()
                .withExternal()
                    .source(OrderState.PAID).target(OrderState.SHIPPED)
                    .event(OrderEvent.SHIP)
                    .and()
                .withExternal()
                    .source(OrderState.SHIPPED).target(OrderState.DELIVERED)
                    .event(OrderEvent.DELIVER)
                    .and()
                .withExternal()
                    .source(OrderState.CART).target(OrderState.CANCELLED)
                    .event(OrderEvent.CANCEL)
                    .and()
                .withExternal()
                    .source(OrderState.CHECKED_OUT).target(OrderState.CANCELLED)
                    .event(OrderEvent.CANCEL)
                    .and()
                .withExternal()
                    .source(OrderState.PAID).target(OrderState.CANCELLED)
                    .event(OrderEvent.CANCEL);
    }

    @Bean
    public Action<OrderState, OrderEvent> addItemAction() {
        return context -> {
            Integer count = context.getExtendedState().get("itemCount", Integer.class);
            if (count == null) {
                count = 0;
            }
            context.getExtendedState().getVariables().put("itemCount", count + 1);
        };
    }

    @Bean
    public Guard<OrderState, OrderEvent> checkoutGuard() {
        return context -> {
            Integer count = context.getExtendedState().get("itemCount", Integer.class);
            return count != null && count > 0;
        };
    }
}
