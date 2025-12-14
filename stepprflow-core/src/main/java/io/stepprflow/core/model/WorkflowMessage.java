package io.stepprflow.core.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Message structure for workflow communication via Kafka.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkflowMessage {

    /**
     * Unique execution ID for this workflow instance.
     */
    private String executionId;

    /**
     * Correlation ID for tracing.
     */
    private String correlationId;

    /**
     * Topic/workflow name.
     */
    private String topic;

    /**
     * Service name that owns this workflow.
     */
    private String serviceName;

    /**
     * Current step ID.
     */
    private int currentStep;

    /**
     * Total number of steps.
     */
    private int totalSteps;

    /**
     * Workflow status.
     */
    private WorkflowStatus status;

    /**
     * Payload data (JSON object).
     */
    private Object payload;

    /**
     * Payload class name for deserialization.
     */
    private String payloadType;

    /**
     * Security context (encrypted or encoded token).
     */
    private String securityContext;

    /**
     * Additional metadata.
     */
    private Map<String, Object> metadata;

    /**
     * Retry information.
     */
    private RetryInfo retryInfo;

    /**
     * Error information if failed.
     */
    private ErrorInfo errorInfo;

    /**
     * Timestamp when message was created.
     */
    @Builder.Default
    private Instant createdAt = Instant.now();

    /**
     * Timestamp when message was last updated.
     */
    private Instant updatedAt;

    /**
     * Create a new execution message.
     *
     * @param topicName the topic name
     * @param payloadData the payload data
     * @return a new workflow message
     */
    public static WorkflowMessage createNew(
            final String topicName,
            final Object payloadData) {
        return WorkflowMessage.builder()
                .executionId(UUID.randomUUID().toString())
                .correlationId(UUID.randomUUID().toString())
                .topic(topicName)
                .currentStep(1)
                .status(WorkflowStatus.PENDING)
                .payload(payloadData)
                .payloadType(payloadData.getClass().getName())
                .build();
    }

    /**
     * Create next step message.
     *
     * @return a new message for the next step
     */
    public WorkflowMessage nextStep() {
        return WorkflowMessage.builder()
                .executionId(this.executionId)
                .correlationId(this.correlationId)
                .topic(this.topic)
                .currentStep(this.currentStep + 1)
                .totalSteps(this.totalSteps)
                .status(WorkflowStatus.IN_PROGRESS)
                .payload(this.payload)
                .payloadType(this.payloadType)
                .securityContext(this.securityContext)
                .metadata(this.metadata)
                .createdAt(this.createdAt)
                .updatedAt(Instant.now())
                .build();
    }

    /**
     * Mark as failed.
     *
     * @param errorMessage the error message
     * @param errorCode the error code
     * @return a new message marked as failed
     */
    public WorkflowMessage fail(final String errorMessage, final String errorCode) {
        return WorkflowMessage.builder()
                .executionId(this.executionId)
                .correlationId(this.correlationId)
                .topic(this.topic)
                .currentStep(this.currentStep)
                .totalSteps(this.totalSteps)
                .status(WorkflowStatus.FAILED)
                .payload(this.payload)
                .payloadType(this.payloadType)
                .securityContext(this.securityContext)
                .metadata(this.metadata)
                .retryInfo(this.retryInfo)
                .errorInfo(ErrorInfo.builder()
                        .message(errorMessage)
                        .code(errorCode)
                        .timestamp(Instant.now())
                        .build())
                .createdAt(this.createdAt)
                .updatedAt(Instant.now())
                .build();
    }

    /**
     * Mark as completed.
     *
     * @return a new message marked as completed
     */
    public WorkflowMessage complete() {
        return WorkflowMessage.builder()
                .executionId(this.executionId)
                .correlationId(this.correlationId)
                .topic(this.topic)
                .currentStep(this.currentStep)
                .totalSteps(this.totalSteps)
                .status(WorkflowStatus.COMPLETED)
                .payload(this.payload)
                .payloadType(this.payloadType)
                .securityContext(this.securityContext)
                .metadata(this.metadata)
                .createdAt(this.createdAt)
                .updatedAt(Instant.now())
                .build();
    }
}
