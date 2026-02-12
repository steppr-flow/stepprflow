package io.github.stepprflow.core.security;

/**
 * No-operation implementation of {@link SecurityContextPropagator}.
 * <p>
 * This implementation does nothing and is used as a default when
 * security propagation is not configured or not needed.
 * </p>
 */
public class NoOpSecurityContextPropagator implements SecurityContextPropagator {

    @Override
    public String capture() {
        return null;
    }

    @Override
    public void restore(final String securityContext) {
        // No-op
    }

    @Override
    public void clear() {
        // No-op
    }

    @Override
    public boolean isEnabled() {
        return false;
    }
}
