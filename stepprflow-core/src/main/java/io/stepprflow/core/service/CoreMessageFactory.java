package io.stepprflow.core.service;

import io.stepprflow.core.model.ErrorInfo;
import io.stepprflow.core.model.RetryInfo;
import io.stepprflow.core.model.StepDefinition;
import io.stepprflow.core.model.WorkflowMessage;
import io.stepprflow.core.model.WorkflowStatus;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;

/**
 * Factory for creating WorkflowMessage instances for retry and DLQ operations.
 * Centralizes message construction to avoid code duplication in core module.
 */
@Component
public class CoreMessageFactory {

    /** Maximum length for stack trace strings. */
    private static final int MAX_STACK_TRACE_LENGTH = 2000;

    /**
     * Create a retry message from an existing workflow message.
     *
     * @param original  the original workflow message
     * @param retryInfo the retry information to include
     * @return a new WorkflowMessage with RETRY_PENDING status
     */
    public WorkflowMessage createRetryMessage(
            final WorkflowMessage original,
            final RetryInfo retryInfo) {
        return WorkflowMessage.builder()
                .executionId(original.getExecutionId())
                .correlationId(original.getCorrelationId())
                .topic(original.getTopic())
                .currentStep(original.getCurrentStep())
                .totalSteps(original.getTotalSteps())
                .status(WorkflowStatus.RETRY_PENDING)
                .payload(original.getPayload())
                .payloadType(original.getPayloadType())
                .securityContext(original.getSecurityContext())
                .metadata(original.getMetadata())
                .retryInfo(retryInfo)
                .createdAt(original.getCreatedAt())
                .updatedAt(Instant.now())
                .build();
    }

    /**
     * Create a DLQ (Dead Letter Queue) message from an existing workflow message.
     *
     * @param original  the original workflow message
     * @param errorInfo the error information to include
     * @return a new WorkflowMessage with FAILED status and error info
     */
    public WorkflowMessage createDlqMessage(
            final WorkflowMessage original,
            final ErrorInfo errorInfo) {
        return WorkflowMessage.builder()
                .executionId(original.getExecutionId())
                .correlationId(original.getCorrelationId())
                .topic(original.getTopic())
                .currentStep(original.getCurrentStep())
                .totalSteps(original.getTotalSteps())
                .status(WorkflowStatus.FAILED)
                .payload(original.getPayload())
                .payloadType(original.getPayloadType())
                .securityContext(original.getSecurityContext())
                .metadata(original.getMetadata())
                .retryInfo(original.getRetryInfo())
                .errorInfo(errorInfo)
                .createdAt(original.getCreatedAt())
                .updatedAt(Instant.now())
                .build();
    }

    /**
     * Create error info from an exception and step definition.
     *
     * @param cause the exception that caused the error
     * @param step  the step definition where the error occurred
     * @return an ErrorInfo instance with details about the error
     */
    public ErrorInfo createErrorInfo(final Throwable cause, final StepDefinition step) {
        return ErrorInfo.builder()
                .code("STEP_EXECUTION_FAILED")
                .message(cause.getMessage())
                .exceptionType(cause.getClass().getName())
                .stackTrace(getStackTrace(cause))
                .stepId(step.getId())
                .stepLabel(step.getLabel())
                .build();
    }

    private String getStackTrace(final Throwable cause) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        cause.printStackTrace(pw);
        String stackTrace = sw.toString();

        // Truncate if too long
        if (stackTrace.length() > MAX_STACK_TRACE_LENGTH) {
            stackTrace = stackTrace.substring(0, MAX_STACK_TRACE_LENGTH) + "...";
        }

        return stackTrace;
    }
}
