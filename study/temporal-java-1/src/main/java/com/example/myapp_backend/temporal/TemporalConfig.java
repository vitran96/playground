package com.example.myapp_backend.temporal;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PreDestroy;

@Configuration
public class TemporalConfig {

    public static final String TASK_QUEUE = "ORDER_TASK_QUEUE";

    @Value("${temporal.target:localhost:7233}")
    private String targetEndpoint;

    @Value("${temporal.namespace:default}")
    private String namespace;

    private WorkerFactory workerFactory;

    @Bean
    public WorkflowServiceStubs workflowServiceStubs() {
        return WorkflowServiceStubs.newServiceStubs(
                WorkflowServiceStubsOptions.newBuilder()
                        .setTarget(targetEndpoint)
                        .build()
        );
    }

    @Bean
    public WorkflowClient workflowClient(WorkflowServiceStubs serviceStubs) {
        return WorkflowClient.newInstance(
                serviceStubs,
                WorkflowClientOptions.newBuilder()
                        .setNamespace(namespace)
                        .build()
        );
    }

    @Bean
    public WorkerFactory workerFactory(WorkflowClient workflowClient, OrderActivities orderActivities) {
        this.workerFactory = WorkerFactory.newInstance(workflowClient);
        Worker worker = workerFactory.newWorker(TASK_QUEUE);
        worker.registerWorkflowImplementationTypes(OrderSagaWorkflowImpl.class);
        worker.registerActivitiesImplementations(orderActivities);
        workerFactory.start();
        return workerFactory;
    }

    @PreDestroy
    public void shutdown() {
        if (workerFactory != null) {
            workerFactory.shutdown();
        }
    }
}
