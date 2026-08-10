package com.example.myapp_backend.temporal;

import com.example.myapp_backend.model.CheckoutRequest;
import com.example.myapp_backend.model.Order;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.failure.ActivityFailure;
import io.temporal.workflow.Saga;
import io.temporal.workflow.Workflow;

import java.time.Duration;

public class OrderSagaWorkflowImpl implements OrderSagaWorkflow {

    private final ActivityOptions options = ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(10))
            .setRetryOptions(RetryOptions.newBuilder()
                    .setMaximumAttempts(3)
                    .setInitialInterval(Duration.ofSeconds(1))
                    .setBackoffCoefficient(2.0)
                    .build())
            .build();

    private final OrderActivities activities = Workflow.newActivityStub(OrderActivities.class, options);

    @Override
    public Order executeCheckout(String orderId, CheckoutRequest request) {
        Saga saga = new Saga(new Saga.Options.Builder().setParallelCompensation(false).build());

        try {
            // Step 1: Reserve Inventory
            activities.reserveInventory(orderId, request.getItems(), request.isSimulateInventoryFailure());
            saga.addCompensation(() -> activities.releaseInventory(orderId, request.getItems()));

            // Step 2: Process Payment
            double totalAmount = request.getItems().stream()
                    .mapToDouble(i -> i.getUnitPrice() * i.getQuantity())
                    .sum();
            String paymentId = activities.processPayment(orderId, totalAmount, request.isSimulatePaymentFailure());
            saga.addCompensation(() -> activities.refundPayment(orderId, paymentId));

            // Step 3: Confirm Order
            activities.confirmOrder(orderId);
            activities.sendNotification(request.getUserId(), orderId,
                    "Order " + orderId + " completed successfully!", "SUCCESS");

        } catch (ActivityFailure e) {
            String errorMsg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            saga.compensate();
            activities.failOrder(orderId, errorMsg);
            activities.sendNotification(request.getUserId(), orderId,
                    "Order " + orderId + " failed. Reason: " + errorMsg, "ERROR");
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            saga.compensate();
            activities.failOrder(orderId, errorMsg);
            activities.sendNotification(request.getUserId(), orderId,
                    "Order " + orderId + " failed with unexpected error: " + errorMsg, "ERROR");
        }

        return null;
    }
}
