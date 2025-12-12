package io.stepprflow.core.tracing;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.stepprflow.core.model.StepDefinition;
import io.stepprflow.core.model.WorkflowMessage;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

/**
 * Provides tracing capabilities for workflow execution.
 * Uses Micrometer Observation API for distributed tracing.
 */
@Component
@ConditionalOnBean(ObservationRegistry.class)
@RequiredArgsConstructor
@Slf4j
public class WorkflowTracing {

    /**
     * The observation registry.
     */
    private final ObservationRegistry observationRegistry;

    /**
     * Execute a step with tracing observation.
     *
     * @param message the workflow message
     * @param step the step definition
     * @param execution the step execution logic
     * @param <T> the return type
     * @return the result of the execution
     * @throws Exception if the execution fails
     */
    public <T> T traceStep(final WorkflowMessage message,
                           final StepDefinition step,
                           final Supplier<T> execution) throws Exception {
        WorkflowTracingContext context = new WorkflowTracingContext(
                message.getExecutionId(),
                message.getCorrelationId(),
                message.getTopic(),
                step.getId(),
                step.getLabel(),
                message.getTotalSteps()
        );

        Observation observation = Observation.createNotStarted(
                WorkflowTracingObservationConvention.INSTANCE,
                () -> context,
                observationRegistry
        );

        try {
            return observation.observe(() -> {
                try {
                    T result = execution.get();
                    context.markSuccess();
                    return result;
                } catch (RuntimeException e) {
                    context.markFailed();
                    throw e;
                }
            });
        } catch (RuntimeException e) {
            log.error(String.format("Error occurred while observing step %s", step.getId()), e);
            throw e;
        }
    }

    /**
     * Execute a step with tracing observation (void return).
     *
     * @param message the workflow message
     * @param step the step definition
     * @param execution the step execution logic
     * @throws Exception if the execution fails
     */
    public void traceStep(final WorkflowMessage message,
                         final StepDefinition step,
                         final Runnable execution) throws Exception {
        traceStep(message, step, () -> {
            execution.run();
            return null;
        });
    }

    /**
     * Create a new observation for workflow start.
     *
     * @param message the workflow message
     * @return the observation
     */
    public Observation startWorkflowObservation(
            final WorkflowMessage message) {
        WorkflowTracingContext context = new WorkflowTracingContext(
                message.getExecutionId(),
                message.getCorrelationId(),
                message.getTopic(),
                0,
                "workflow.start",
                message.getTotalSteps()
        );

        return Observation.createNotStarted(
                "stepprflow.workflow",
                () -> context,
                observationRegistry
        ).start();
    }
}
