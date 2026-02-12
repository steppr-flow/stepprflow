package io.github.stepprflow.core.security;

/**
 * Interface for propagating security context across workflow steps.
 * <p>
 * Implementations are responsible for:
 * <ul>
 *   <li>Capturing the current security context (e.g., JWT token, authentication)</li>
 *   <li>Restoring the security context before step execution</li>
 *   <li>Clearing the security context after step execution</li>
 * </ul>
 * </p>
 *
 * @see NoOpSecurityContextPropagator
 */
public interface SecurityContextPropagator {

    /**
     * Capture the current security context.
     * <p>
     * This is typically called when starting a workflow to capture
     * the caller's authentication/authorization context.
     * </p>
     *
     * @return the serialized security context (e.g., JWT token), or null if none
     */
    String capture();

    /**
     * Restore the security context before executing a workflow step.
     * <p>
     * This should set up the security context so that the step method
     * can access the authenticated principal and authorities.
     * </p>
     *
     * @param securityContext the serialized security context to restore
     */
    void restore(String securityContext);

    /**
     * Clear the security context after step execution.
     * <p>
     * This should clean up any thread-local or context state
     * to prevent security context leakage.
     * </p>
     */
    void clear();

    /**
     * Check if security propagation is enabled.
     *
     * @return true if security context should be propagated
     */
    boolean isEnabled();
}
