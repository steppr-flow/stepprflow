package io.stepprflow.monitor.controller;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.stepprflow.core.StepprFlowProperties;
import io.stepprflow.monitor.dto.CircuitBreakerConfig;
import io.stepprflow.monitor.dto.CircuitBreakerStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * REST API for circuit breaker monitoring and management.
 */
@RestController
@RequestMapping("/api/circuit-breakers")
@RequiredArgsConstructor
@Tag(name = "Circuit Breaker", description = "Circuit breaker monitoring and management")
public class CircuitBreakerController {

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final StepprFlowProperties properties;

    @Operation(summary = "Get configuration", description = "Get the circuit breaker configuration settings")
    @ApiResponse(responseCode = "200", description = "Configuration retrieved successfully")
    @GetMapping("/config")
    public ResponseEntity<CircuitBreakerConfig> getConfig() {
        StepprFlowProperties.CircuitBreaker cb = properties.getCircuitBreaker();

        CircuitBreakerConfig config = CircuitBreakerConfig.builder()
                .enabled(cb.isEnabled())
                .failureRateThreshold(cb.getFailureRateThreshold())
                .slowCallRateThreshold(cb.getSlowCallRateThreshold())
                .slowCallDurationThresholdMs(cb.getSlowCallDurationThreshold().toMillis())
                .slidingWindowSize(cb.getSlidingWindowSize())
                .minimumNumberOfCalls(cb.getMinimumNumberOfCalls())
                .permittedNumberOfCallsInHalfOpenState(cb.getPermittedNumberOfCallsInHalfOpenState())
                .waitDurationInOpenStateMs(cb.getWaitDurationInOpenState().toMillis())
                .automaticTransitionFromOpenToHalfOpenEnabled(cb.isAutomaticTransitionFromOpenToHalfOpenEnabled())
                .build();

        return ResponseEntity.ok(config);
    }

    @Operation(summary = "List all circuit breakers", description = "Get status of all registered circuit breakers")
    @ApiResponse(responseCode = "200", description = "List of circuit breaker statuses")
    @GetMapping
    public ResponseEntity<List<CircuitBreakerStatus>> getAllCircuitBreakers() {
        List<CircuitBreakerStatus> statuses = circuitBreakerRegistry.getAllCircuitBreakers()
                .stream()
                .map(this::toStatus)
                .collect(Collectors.toList());

        return ResponseEntity.ok(statuses);
    }

    @Operation(summary = "Get circuit breaker by name",
            description = "Get the status of a specific circuit breaker")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Circuit breaker found",
                    content = @Content(schema = @Schema(implementation = CircuitBreakerStatus.class))),
            @ApiResponse(responseCode = "404", description = "Circuit breaker not found",
                    content = @Content)
    })
    @GetMapping("/{name}")
    public ResponseEntity<CircuitBreakerStatus> getCircuitBreaker(
            @Parameter(description = "Circuit breaker name")
            @PathVariable String name) {
        Optional<CircuitBreaker> cb = circuitBreakerRegistry.find(name);

        return cb.map(circuitBreaker -> ResponseEntity.ok(toStatus(circuitBreaker)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Reset circuit breaker",
            description = "Reset a circuit breaker to CLOSED state")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Circuit breaker reset successfully",
                    content = @Content(schema = @Schema(implementation = CircuitBreakerStatus.class))),
            @ApiResponse(responseCode = "404", description = "Circuit breaker not found",
                    content = @Content)
    })
    @PostMapping("/{name}/reset")
    public ResponseEntity<CircuitBreakerStatus> resetCircuitBreaker(
            @Parameter(description = "Circuit breaker name to reset") @PathVariable String name) {
        Optional<CircuitBreaker> cb = circuitBreakerRegistry.find(name);

        if (cb.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        CircuitBreaker circuitBreaker = cb.get();
        circuitBreaker.reset();

        return ResponseEntity.ok(toStatus(circuitBreaker));
    }

    private CircuitBreakerStatus toStatus(CircuitBreaker cb) {
        CircuitBreaker.Metrics metrics = cb.getMetrics();

        return CircuitBreakerStatus.builder()
                .name(cb.getName())
                .state(cb.getState().name())
                .successfulCalls(metrics.getNumberOfSuccessfulCalls())
                .failedCalls(metrics.getNumberOfFailedCalls())
                .notPermittedCalls(metrics.getNumberOfNotPermittedCalls())
                .bufferedCalls(metrics.getNumberOfBufferedCalls())
                .slowCalls(metrics.getNumberOfSlowCalls())
                .slowSuccessfulCalls(metrics.getNumberOfSlowSuccessfulCalls())
                .slowFailedCalls(metrics.getNumberOfSlowFailedCalls())
                .failureRate(metrics.getFailureRate())
                .slowCallRate(metrics.getSlowCallRate())
                .build();
    }
}
