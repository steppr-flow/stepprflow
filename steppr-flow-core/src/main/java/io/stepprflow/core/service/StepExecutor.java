package io.stepprflow.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.stepprflow.core.StepprFlowProperties;
import io.stepprflow.core.broker.MessageBroker;
import io.stepprflow.core.model.ErrorInfo;
import io.stepprflow.core.model.RetryInfo;
import io.stepprflow.core.model.StepDefinition;
import io.stepprflow.core.model.WorkflowDefinition;
import io.stepprflow.core.model.WorkflowMessage;
import io.stepprflow.core.model.WorkflowStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;

/**
 * Executes workflow steps.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StepExecutor {

    /** The workflow registry. */
    private final WorkflowRegistry registry;

    /** The message broker. */
    private final MessageBroker messageBroker;

    /** The steppr-flow properties. */
    private final StepprFlowProperties properties;

    /** The JSON object mapper. */
    private final ObjectMapper objectMapper;

    /** Maximum length for stack trace strings. */
    private static final int MAX_STACK_TRACE_LENGTH = 2000;

    /**
     * Execute a workflow step.
     *
     * @param message the workflow message
     */
    public void execute(final WorkflowMessage message) {
        String topic = message.getTopic();
        int stepId = message.getCurrentStep();

        WorkflowDefinition definition = registry.getDefinition(topic);
        if (definition == null) {
            log.error("Unknown workflow topic: {}", topic);
            return;
        }

        StepDefinition step = definition.getStep(stepId);
        if (step == null) {
            log.error("Unknown step {} for workflow {}", stepId, topic);
            return;
        }

        log.info("Executing step {}/{} ({}) for workflow {} [{}]",
                stepId, message.getTotalSteps(), step.getLabel(),
                topic, message.getExecutionId());

        try {
            // Deserialize payload
            Object payload = deserializePayload(message);

            // Execute step method
            Method method = step.getMethod();
            // TODO throw custom exception when private method
            method.setAccessible(true);
            method.invoke(definition.getHandler(), payload);

            // Check if last step
            if (definition.isLastStep(stepId)) {
                handleCompletion(message, definition);
            } else {
                // Advance to next step
                WorkflowMessage nextMessage = message.nextStep();
                messageBroker.send(topic, nextMessage);
                log.info("Advanced to step {}/{} for workflow {} [{}]",
                        nextMessage.getCurrentStep(), message.getTotalSteps(),
                        topic, message.getExecutionId());
            }

        } catch (Exception e) {
            handleFailure(message, step, definition, e);
        }
    }

    private Object deserializePayload(
            final WorkflowMessage message) throws Exception {
        if (message.getPayload() == null) {
            return null;
        }

        String payloadType = message.getPayloadType();
        if (payloadType == null) {
            return message.getPayload();
        }

        try {
            Class<?> payloadClass = Class.forName(payloadType);
            return objectMapper.convertValue(message.getPayload(), payloadClass);
        } catch (ClassNotFoundException e) {
            log.warn("Could not find payload class {}, using raw payload",
                     payloadType);
            return message.getPayload();
        }
    }

    private void handleCompletion(
            final WorkflowMessage message,
            final WorkflowDefinition definition) {
        log.info("Workflow {} completed successfully [{}]",
                 message.getTopic(), message.getExecutionId());

        // Call success callback if defined
        if (definition.getOnSuccessMethod() != null) {
            try {
                definition.getOnSuccessMethod().setAccessible(true);
                invokeCallback(definition.getOnSuccessMethod(),
                              definition.getHandler(), message);
            } catch (Exception e) {
                log.error("Error in success callback", e);
            }
        }

        // Send completion message
        WorkflowMessage completedMessage = message.complete();
        messageBroker.send(message.getTopic() + ".completed", completedMessage);
    }

    private void invokeCallback(
            final Method method,
            final Object handler,
            final WorkflowMessage message) throws Exception {
        invokeCallback(method, handler, message, null);
    }

    private void invokeCallback(
            final Method method,
            final Object handler,
            final WorkflowMessage message,
            final Throwable error) throws Exception {
        Class<?>[] paramTypes = method.getParameterTypes();

        if (paramTypes.length == 0) {
            method.invoke(handler);
        } else if (paramTypes.length == 1) {
            Class<?> paramType = paramTypes[0];
            if (WorkflowMessage.class.isAssignableFrom(paramType)) {
                method.invoke(handler, message);
            } else {
                // Assume it's the payload type
                Object payload = deserializePayload(message);
                method.invoke(handler, payload);
            }
        } else if (paramTypes.length == 2
                   && Throwable.class.isAssignableFrom(paramTypes[1])) {
            // Failure callback with payload and error
            Object payload = deserializePayload(message);
            method.invoke(handler, payload, error);
        } else {
            log.warn("Callback method {} has unsupported parameter count: {}",
                     method.getName(), paramTypes.length);
        }
    }

    private void handleFailure(
            final WorkflowMessage message,
            final StepDefinition step,
            final WorkflowDefinition definition,
            final Exception e) {
        Throwable cause = e instanceof InvocationTargetException
                ? e.getCause() : e;
        String errorMessage = cause.getMessage();

        log.error("Step {}/{} ({}) failed for workflow {} [{}]: {}",
                step.getId(), message.getTotalSteps(), step.getLabel(),
                message.getTopic(), message.getExecutionId(), errorMessage, cause);

        // Check if should continue on failure
        if (step.isContinueOnFailure() && !definition.isLastStep(step.getId())) {
            log.info("Continuing to next step despite failure (continueOnFailure=true)");
            WorkflowMessage nextMessage = message.nextStep();
            messageBroker.send(message.getTopic(), nextMessage);
            return;
        }

        // Check if should retry
        RetryInfo retryInfo = message.getRetryInfo();
        if (retryInfo == null) {
            retryInfo = RetryInfo.builder()
                    .attempt(1)
                    .maxAttempts(properties.getRetry().getMaxAttempts())
                    .build();
        }

        if (!retryInfo.isExhausted() && isRetryable(cause)) {
            scheduleRetry(message, retryInfo, errorMessage);
        } else {
            // Send to DLQ
            sendToDlq(message, step, cause);

            // Call failure callback
            if (definition.getOnFailureMethod() != null) {
                try {
                    definition.getOnFailureMethod().setAccessible(true);
                    invokeCallback(definition.getOnFailureMethod(),
                                  definition.getHandler(), message, cause);
                } catch (Exception ex) {
                    log.error("Error in failure callback", ex);
                }
            }
        }
    }

    private boolean isRetryable(final Throwable cause) {
        String exceptionType = cause.getClass().getName();
        return !properties.getRetry().getNonRetryableExceptions().contains(exceptionType);
    }

    private void scheduleRetry(
            final WorkflowMessage message,
            final RetryInfo retryInfo,
            final String errorMessage) {
        Duration delay = calculateBackoff(retryInfo.getAttempt());
        Instant nextRetry = Instant.now().plus(delay);

        RetryInfo newRetryInfo = retryInfo.nextAttempt(nextRetry, errorMessage);

        WorkflowMessage retryMessage = WorkflowMessage.builder()
                .executionId(message.getExecutionId())
                .correlationId(message.getCorrelationId())
                .topic(message.getTopic())
                .currentStep(message.getCurrentStep())
                .totalSteps(message.getTotalSteps())
                .status(WorkflowStatus.RETRY_PENDING)
                .payload(message.getPayload())
                .payloadType(message.getPayloadType())
                .securityContext(message.getSecurityContext())
                .metadata(message.getMetadata())
                .retryInfo(newRetryInfo)
                .createdAt(message.getCreatedAt())
                .updatedAt(Instant.now())
                .build();

        log.info("Scheduling retry {}/{} for workflow {} [{}] at {}",
                newRetryInfo.getAttempt(), newRetryInfo.getMaxAttempts(),
                message.getTopic(), message.getExecutionId(), nextRetry);

        // In core module, we just send to retry topic
        // The monitor module handles the scheduled retry
        messageBroker.send(message.getTopic() + ".retry", retryMessage);
    }

    private Duration calculateBackoff(final int attempt) {
        StepprFlowProperties.Retry retryConfig = properties.getRetry();
        long initialMs = retryConfig.getInitialDelay().toMillis();
        double multiplier = retryConfig.getMultiplier();
        long maxMs = retryConfig.getMaxDelay().toMillis();

        long delayMs = (long) (initialMs * Math.pow(multiplier, attempt - 1));
        delayMs = Math.min(delayMs, maxMs);

        return Duration.ofMillis(delayMs);
    }

    private void sendToDlq(
            final WorkflowMessage message,
            final StepDefinition step,
            final Throwable cause) {
        if (!properties.getDlq().isEnabled()) {
            return;
        }

        ErrorInfo errorInfo = ErrorInfo.builder()
                .code("STEP_EXECUTION_FAILED")
                .message(cause.getMessage())
                .exceptionType(cause.getClass().getName())
                .stackTrace(getStackTrace(cause))
                .stepId(step.getId())
                .stepLabel(step.getLabel())
                .build();

        WorkflowMessage dlqMessage = WorkflowMessage.builder()
                .executionId(message.getExecutionId())
                .correlationId(message.getCorrelationId())
                .topic(message.getTopic())
                .currentStep(message.getCurrentStep())
                .totalSteps(message.getTotalSteps())
                .status(WorkflowStatus.FAILED)
                .payload(message.getPayload())
                .payloadType(message.getPayloadType())
                .securityContext(message.getSecurityContext())
                .metadata(message.getMetadata())
                .retryInfo(message.getRetryInfo())
                .errorInfo(errorInfo)
                .createdAt(message.getCreatedAt())
                .updatedAt(Instant.now())
                .build();

        String dlqTopic = message.getTopic() + properties.getDlq().getSuffix();
        messageBroker.send(dlqTopic, dlqMessage);

        log.info("Sent workflow {} [{}] to DLQ: {}",
                 message.getTopic(), message.getExecutionId(), dlqTopic);
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
