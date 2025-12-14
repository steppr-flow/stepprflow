package io.stepprflow.monitor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing the circuit breaker configuration.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CircuitBreakerConfig {

    private boolean enabled;
    private float failureRateThreshold;
    private float slowCallRateThreshold;
    private long slowCallDurationThresholdMs;
    private int slidingWindowSize;
    private int minimumNumberOfCalls;
    private int permittedNumberOfCallsInHalfOpenState;
    private long waitDurationInOpenStateMs;
    private boolean automaticTransitionFromOpenToHalfOpenEnabled;
}
