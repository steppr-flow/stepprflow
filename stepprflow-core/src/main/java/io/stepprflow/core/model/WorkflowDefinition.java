package io.stepprflow.core.model;

import io.stepprflow.core.service.StepprFlow;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;

/**
 * Definition of a workflow.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowDefinition {

    /**
     * Topic name.
     */
    private String topic;

    /**
     * Description.
     */
    private String description;

    /**
     * Workflow handler instance.
     */
    private StepprFlow handler;

    /**
     * Handler class.
     */
    private Class<? extends StepprFlow> handlerClass;

    /**
     * Ordered list of step definitions.
     */
    private List<StepDefinition> steps;

    /**
     * Success callback method.
     */
    private Method onSuccessMethod;

    /**
     * Failure callback method.
     */
    private Method onFailureMethod;

    /**
     * Workflow timeout.
     */
    private Duration timeout;

    /**
     * Number of partitions.
     */
    private int partitions;

    /**
     * Replication factor.
     */
    private short replication;

    /**
     * Get step by ID.
     *
     * @param stepId the step ID
     * @return the step definition or null if not found
     */
    public StepDefinition getStep(final int stepId) {
        return steps.stream()
                .filter(s -> s.getId() == stepId)
                .findFirst()
                .orElse(null);
    }

    /**
     * Get total number of steps.
     *
     * @return the total number of steps
     */
    public int getTotalSteps() {
        return steps.size();
    }

    /**
     * Check if step ID is the last step.
     *
     * @param stepId the step ID
     * @return true if this is the last step
     */
    public boolean isLastStep(final int stepId) {
        return steps.stream()
                .mapToInt(StepDefinition::getId)
                .max()
                .orElse(0) == stepId;
    }
}
