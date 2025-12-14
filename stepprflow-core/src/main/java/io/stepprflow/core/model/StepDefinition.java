package io.stepprflow.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.lang.reflect.Method;
import java.time.Duration;

/**
 * Definition of a workflow step.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StepDefinition {

    /**
     * Step ID (execution order).
     */
    private int id;

    /**
     * Step label.
     */
    private String label;

    /**
     * Step description.
     */
    private String description;

    /**
     * Method to invoke.
     */
    private Method method;

    /**
     * Whether step is skippable.
     */
    private boolean skippable;

    /**
     * Whether to continue on failure.
     */
    private boolean continueOnFailure;

    /**
     * Step timeout.
     */
    private Duration timeout;
}
