package io.stepprflow.core.broker;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.stepprflow.core.StepprFlowProperties;
import io.stepprflow.core.exception.CircuitBreakerOpenException;
import io.stepprflow.core.model.WorkflowMessage;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

/**
 * A resilient wrapper around MessageBroker that adds circuit breaker protection.
 *
 * <p>When the underlying broker experiences failures, the circuit breaker opens
 * to prevent cascading failures and allow the broker time to recover.
 */
@Slf4j
public class ResilientMessageBroker implements MessageBroker {

    /** The delegate message broker. */
    private final MessageBroker delegate;

    /** The circuit breaker instance. */
    private final CircuitBreaker circuitBreaker;

    /** Whether circuit breaker is enabled. */
    private final boolean enabled;

    /**
     * Constructs a new resilient message broker.
     *
     * @param delegateBroker the delegate broker
     * @param config the circuit breaker configuration
     * @param registry the circuit breaker registry
     */
    public ResilientMessageBroker(
            final MessageBroker delegateBroker,
            final StepprFlowProperties.CircuitBreaker config,
            final CircuitBreakerRegistry registry) {
        this.delegate = delegateBroker;
        this.enabled = config.isEnabled();

        if (enabled) {
            String cbName = "broker-" + delegateBroker.getBrokerType();

            CircuitBreakerConfig cbConfig = CircuitBreakerConfig.custom()
                    .failureRateThreshold(config.getFailureRateThreshold())
                    .slowCallRateThreshold(config.getSlowCallRateThreshold())
                    .slowCallDurationThreshold(config.getSlowCallDurationThreshold())
                    .slidingWindowSize(config.getSlidingWindowSize())
                    .minimumNumberOfCalls(config.getMinimumNumberOfCalls())
                    .permittedNumberOfCallsInHalfOpenState(
                            config.getPermittedNumberOfCallsInHalfOpenState())
                    .waitDurationInOpenState(config.getWaitDurationInOpenState())
                    .automaticTransitionFromOpenToHalfOpenEnabled(
                            config.isAutomaticTransitionFromOpenToHalfOpenEnabled())
                    .build();

            this.circuitBreaker = registry.circuitBreaker(cbName, cbConfig);

            // Register event handlers for logging
            circuitBreaker.getEventPublisher()
                    .onStateTransition(event ->
                            log.info("Circuit breaker '{}' state changed: {} -> {}",
                                    event.getCircuitBreakerName(),
                                    event.getStateTransition().getFromState(),
                                    event.getStateTransition().getToState()))
                    .onFailureRateExceeded(event ->
                            log.warn("Circuit breaker '{}' failure rate exceeded: {}%",
                                    event.getCircuitBreakerName(),
                                    event.getFailureRate()))
                    .onSlowCallRateExceeded(event ->
                            log.warn("Circuit breaker '{}' slow call rate exceeded: {}%",
                                    event.getCircuitBreakerName(),
                                    event.getSlowCallRate()));

            log.info("Circuit breaker '{}' initialized for broker type '{}'",
                     cbName, delegateBroker.getBrokerType());
        } else {
            this.circuitBreaker = null;
            log.info("Circuit breaker disabled for broker type '{}'",
                     delegateBroker.getBrokerType());
        }
    }

    @Override
    public void send(final String destination, final WorkflowMessage message) {
        if (!enabled) {
            delegate.send(destination, message);
            return;
        }

        try {
            circuitBreaker.executeRunnable(
                    () -> delegate.send(destination, message));
        } catch (CallNotPermittedException e) {
            throw new CircuitBreakerOpenException(
                    circuitBreaker.getName(), circuitBreaker.getState(), e);
        }
    }

    @Override
    public CompletableFuture<Void> sendAsync(
            final String destination,
            final WorkflowMessage message) {
        if (!enabled) {
            return delegate.sendAsync(destination, message);
        }

        try {
            return circuitBreaker.executeSupplier(
                    () -> delegate.sendAsync(destination, message));
        } catch (CallNotPermittedException e) {
            CompletableFuture<Void> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(
                    new CircuitBreakerOpenException(
                            circuitBreaker.getName(),
                            circuitBreaker.getState(),
                            e));
            return failedFuture;
        }
    }

    @Override
    public void sendSync(final String destination, final WorkflowMessage message) {
        if (!enabled) {
            delegate.sendSync(destination, message);
            return;
        }

        try {
            circuitBreaker.executeRunnable(
                    () -> delegate.sendSync(destination, message));
        } catch (CallNotPermittedException e) {
            throw new CircuitBreakerOpenException(
                    circuitBreaker.getName(), circuitBreaker.getState(), e);
        }
    }

    @Override
    public String getBrokerType() {
        return delegate.getBrokerType();
    }

    @Override
    public boolean isAvailable() {
        if (!enabled) {
            return delegate.isAvailable();
        }

        // Circuit is not available if it's open
        if (circuitBreaker.getState() == CircuitBreaker.State.OPEN) {
            return false;
        }

        return delegate.isAvailable();
    }

    /**
     * Get the underlying circuit breaker for monitoring purposes.
     *
     * @return the circuit breaker or null if disabled
     */
    public CircuitBreaker getCircuitBreaker() {
        return circuitBreaker;
    }

    /**
     * Get the delegate broker.
     *
     * @return the delegate message broker
     */
    public MessageBroker getDelegate() {
        return delegate;
    }
}
