package io.stepprflow.core.service;

import io.stepprflow.core.broker.MessageBroker;
import io.stepprflow.core.exception.WorkflowException;
import io.stepprflow.core.model.WorkflowDefinition;
import io.stepprflow.core.model.WorkflowMessage;
import io.stepprflow.core.model.WorkflowStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Implementation of WorkflowStarter.
 */
@Service
@Slf4j
public class WorkflowStarterImpl implements WorkflowStarter {

    /** The workflow registry. */
    private final WorkflowRegistry registry;

    /** The message broker. */
    private final MessageBroker messageBroker;

    /** The service name. */
    private final String serviceName;

    /**
     * Constructs a new WorkflowStarterImpl.
     *
     * @param workflowRegistry the workflow registry
     * @param broker the message broker
     * @param appName the service name
     */
    public WorkflowStarterImpl(
            final WorkflowRegistry workflowRegistry,
            final MessageBroker broker,
            @Value("${spring.application.name:unknown}") final String appName) {
        this.registry = workflowRegistry;
        this.messageBroker = broker;
        this.serviceName = appName;
    }

    @Override
    public String start(final String topic, final Object payload) {
        return start(topic, payload, null);
    }

    @Override
    public String start(
            final String topic,
            final Object payload,
            final Map<String, Object> metadata) {
        WorkflowDefinition definition = registry.getDefinition(topic);
        if (definition == null) {
            throw new WorkflowException("Unknown workflow topic: " + topic);
        }

        String executionId = UUID.randomUUID().toString();

        WorkflowMessage message = WorkflowMessage.builder()
                .executionId(executionId)
                .correlationId(UUID.randomUUID().toString())
                .topic(topic)
                .serviceName(serviceName)
                .currentStep(1)
                .totalSteps(definition.getTotalSteps())
                .status(WorkflowStatus.PENDING)
                .payload(payload)
                .payloadType(payload.getClass().getName())
                .metadata(metadata)
                .build();

        log.info("Starting workflow: topic={}, serviceName={}, executionId={}",
                 topic, serviceName, executionId);
        messageBroker.send(topic, message);

        return executionId;
    }

    @Override
    public CompletableFuture<String> startAsync(
            final String topic,
            final Object payload) {
        return CompletableFuture.supplyAsync(() -> start(topic, payload));
    }

    @Override
    public WorkflowMessage startAndGetMessage(
            final String topic,
            final Object payload) {
        WorkflowDefinition definition = registry.getDefinition(topic);
        if (definition == null) {
            throw new WorkflowException("Unknown workflow topic: " + topic);
        }

        String executionId = UUID.randomUUID().toString();

        WorkflowMessage message = WorkflowMessage.builder()
                .executionId(executionId)
                .correlationId(UUID.randomUUID().toString())
                .topic(topic)
                .serviceName(serviceName)
                .currentStep(1)
                .totalSteps(definition.getTotalSteps())
                .status(WorkflowStatus.PENDING)
                .payload(payload)
                .payloadType(payload.getClass().getName())
                .build();

        log.info("Starting workflow: topic={}, executionId={}", topic, executionId);
        messageBroker.send(topic, message);

        return message;
    }

    @Override
    public void resume(final String executionId, final Integer stepId) {
        log.info("Resume workflow is not yet implemented");
        throw new UnsupportedOperationException(
                "Resume is implemented in async-workflow-monitor");
    }

    @Override
    public void cancel(final String executionId) {
        log.info("Cancel workflow is not yet implemented");
        throw new UnsupportedOperationException(
                "Cancel is implemented in async-workflow-monitor");
    }
}
