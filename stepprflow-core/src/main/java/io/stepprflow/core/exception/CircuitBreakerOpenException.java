package io.stepprflow.core.exception;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import lombok.Getter;

/**
 * Exception thrown when a circuit breaker is open and rejects calls.
 *
 * <p>This exception indicates that the broker is experiencing failures
 * and the circuit breaker has opened to prevent cascading failures.
 */
@Getter
public class CircuitBreakerOpenException extends MessageBrokerException {

    /** The name of the circuit breaker. */
    private final String circuitBreakerName;

    /** The current state of the circuit breaker. */
    private final CircuitBreaker.State state;

    /**
     * Constructs a new circuit breaker open exception.
     *
     * @param circuitBreakerName the name of the circuit breaker
     * @param circuitBreakerState the current state of the circuit breaker
     */
    public CircuitBreakerOpenException(
            final String circuitBreakerName,
            final CircuitBreaker.State circuitBreakerState) {
        super(String.format("Circuit breaker '%s' is %s - calls are not permitted",
                circuitBreakerName, circuitBreakerState));
        this.circuitBreakerName = circuitBreakerName;
        this.state = circuitBreakerState;
    }

    /**
     * Constructs a new circuit breaker open exception with cause.
     *
     * @param circuitBreakerName the name of the circuit breaker
     * @param circuitBreakerState the current state of the circuit breaker
     * @param cause the cause of this exception
     */
    public CircuitBreakerOpenException(
            final String circuitBreakerName,
            final CircuitBreaker.State circuitBreakerState,
            final Throwable cause) {
        super(String.format("Circuit breaker '%s' is %s - calls are not permitted",
                circuitBreakerName, circuitBreakerState), cause);
        this.circuitBreakerName = circuitBreakerName;
        this.state = circuitBreakerState;
    }
}
