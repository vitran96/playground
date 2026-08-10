package com.example.myapp_backend.temporal;

import com.example.myapp_backend.model.CheckoutRequest;
import com.example.myapp_backend.model.Order;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface OrderSagaWorkflow {

    @WorkflowMethod
    Order executeCheckout(String orderId, CheckoutRequest request);
}
