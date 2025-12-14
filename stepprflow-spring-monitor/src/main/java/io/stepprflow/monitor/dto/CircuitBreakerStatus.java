package io.stepprflow.monitor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing the status of a circuit breaker.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CircuitBreakerStatus {

    private String name;
    private String state;

    // Metrics
    private long successfulCalls;
    private long failedCalls;
    private long notPermittedCalls;
    private long bufferedCalls;
    private long slowCalls;
    private long slowSuccessfulCalls;
    private long slowFailedCalls;

    // Rates
    private float failureRate;
    private float slowCallRate;
}
