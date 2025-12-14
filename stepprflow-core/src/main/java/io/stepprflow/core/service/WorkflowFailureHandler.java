package io.stepprflow.core.service;

import io.stepprflow.core.StepprFlowProperties;
import io.stepprflow.core.broker.MessageBroker;
import io.stepprflow.core.model.ErrorInfo;
import io.stepprflow.core.model.RetryInfo;
import io.stepprflow.core.model.StepDefinition;
import io.stepprflow.core.model.WorkflowDefinition;
import io.stepprflow.core.model.WorkflowMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.time.Instant;

/**
 * Handles workflow step failures including retry scheduling and DLQ routing.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WorkflowFailureHandler {

    /** The message broker. */
    private final MessageBroker messageBroker;

    /** The steppr-flow properties. */
    private final StepprFlowProperties properties;

    /** The callback method invoker. */
    private final CallbackMethodInvoker callbackInvoker;

    /** The backoff calculator. */
    private final BackoffCalculator backoffCalculator;

    /** The message factory. */
    private final CoreMessageFactory messageFactory;

    /**
     * Handle a step execution failure.
     *
     * @param message    the workflow message
     * @param step       the step that failed
     * @param definition the workflow definition
     * @param e          the exception that caused the failure
     */
    public void handleFailure(
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
                    callbackInvoker.invokeRaw(
                            definition.getOnFailureMethod(),
                            definition.getHandler(),
                            message,
                            cause);
                } catch (Exception ex) {
                    log.error("Error in failure callback", ex);
                }
            }
        }
    }

    /**
     * Check if an exception is retryable.
     *
     * @param cause the exception to check
     * @return true if the exception is retryable
     */
    public boolean isRetryable(final Throwable cause) {
        String exceptionType = cause.getClass().getName();
        return !properties.getRetry().getNonRetryableExceptions().contains(exceptionType);
    }

    /**
     * Calculate the backoff duration for a retry attempt.
     *
     * @param attempt the current attempt number
     * @return the backoff duration
     */
    public Duration calculateBackoff(final int attempt) {
        return backoffCalculator.calculate(attempt);
    }

    private void scheduleRetry(
            final WorkflowMessage message,
            final RetryInfo retryInfo,
            final String errorMessage) {
        Duration delay = backoffCalculator.calculate(retryInfo.getAttempt());
        Instant nextRetry = Instant.now().plus(delay);

        RetryInfo newRetryInfo = retryInfo.nextAttempt(nextRetry, errorMessage);
        WorkflowMessage retryMessage = messageFactory.createRetryMessage(
                message, newRetryInfo);

        log.info("Scheduling retry {}/{} for workflow {} [{}] at {}",
                newRetryInfo.getAttempt(), newRetryInfo.getMaxAttempts(),
                message.getTopic(), message.getExecutionId(), nextRetry);

        // In core module, we just send to retry topic
        // The monitor module handles the scheduled retry
        messageBroker.send(message.getTopic() + ".retry", retryMessage);
    }

    private void sendToDlq(
            final WorkflowMessage message,
            final StepDefinition step,
            final Throwable cause) {
        if (!properties.getDlq().isEnabled()) {
            return;
        }

        ErrorInfo errorInfo = messageFactory.createErrorInfo(cause, step);
        WorkflowMessage dlqMessage = messageFactory.createDlqMessage(message, errorInfo);

        String dlqTopic = message.getTopic() + properties.getDlq().getSuffix();
        messageBroker.send(dlqTopic, dlqMessage);

        log.info("Sent workflow {} [{}] to DLQ: {}",
                 message.getTopic(), message.getExecutionId(), dlqTopic);
    }
}
