package io.stepprflow.monitor.health;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Health indicator for circuit breaker states.
 *
 * <p>This indicator reports the state of all circuit breakers registered in the
 * application. It helps identify when circuit breakers are protecting the system
 * from cascading failures.
 *
 * <p>Health states:
 * <ul>
 *   <li>UP - All circuit breakers are CLOSED or HALF_OPEN</li>
 *   <li>DOWN - At least one circuit breaker is OPEN</li>
 * </ul>
 *
 * <p>Circuit breaker states:
 * <ul>
 *   <li>CLOSED - Normal operation, requests flow through</li>
 *   <li>OPEN - Failure threshold exceeded, requests are rejected</li>
 *   <li>HALF_OPEN - Testing if the downstream service has recovered</li>
 *   <li>DISABLED - Circuit breaker is disabled</li>
 *   <li>FORCED_OPEN - Manually forced open</li>
 * </ul>
 */
@Component
@ConditionalOnBean(CircuitBreakerRegistry.class)
public class CircuitBreakerHealthIndicator implements HealthIndicator {

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    /**
     * Creates a new circuit breaker health indicator.
     *
     * @param circuitBreakerRegistry the circuit breaker registry
     */
    public CircuitBreakerHealthIndicator(CircuitBreakerRegistry circuitBreakerRegistry) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    @Override
    public Health health() {
        Map<String, Object> details = new HashMap<>();
        boolean hasOpenCircuit = false;
        int openCount = 0;
        int totalCount = 0;

        for (CircuitBreaker cb : circuitBreakerRegistry.getAllCircuitBreakers()) {
            CircuitBreaker.State state = cb.getState();
            details.put(cb.getName(), state.name());
            totalCount++;

            if (state == CircuitBreaker.State.OPEN
                    || state == CircuitBreaker.State.FORCED_OPEN) {
                hasOpenCircuit = true;
                openCount++;
            }
        }

        details.put("totalCircuitBreakers", totalCount);
        details.put("openCircuitBreakers", openCount);

        if (totalCount == 0) {
            return Health.up()
                    .withDetail("message", "No circuit breakers registered")
                    .build();
        }

        if (hasOpenCircuit) {
            return Health.down()
                    .withDetails(details)
                    .withDetail("reason", openCount + " circuit breaker(s) are OPEN")
                    .build();
        }

        return Health.up()
                .withDetails(details)
                .build();
    }
}
