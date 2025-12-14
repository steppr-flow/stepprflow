package io.stepprflow.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as a workflow step.
 * Steps are executed in order of their ID.
 *
 * <p>Example usage:
 * <pre>
 * &#64;Step(id = 1, label = "Validate order")
 * public void validateOrder(OrderPayload payload) {
 *     // validation logic
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Step {

    /**
     * Step ID determining execution order.
     * Must be unique within a workflow and greater than 0.
     *
     * @return the step ID
     */
    int id();

    /**
     * Human-readable label for this step.
     *
     * @return the step label
     */
    String label();

    /**
     * Optional description.
     *
     * @return the step description
     */
    String description() default "";

    /**
     * Whether this step can be skipped on retry.
     *
     * @return true if the step is skippable
     */
    boolean skippable() default false;

    /**
     * Whether to continue to next step on failure.
     *
     * @return true if should continue on failure
     */
    boolean continueOnFailure() default false;
}
