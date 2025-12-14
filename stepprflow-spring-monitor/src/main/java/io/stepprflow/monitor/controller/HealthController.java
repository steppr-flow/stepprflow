package io.stepprflow.monitor.controller;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.stepprflow.core.broker.MessageBroker;
import io.stepprflow.monitor.dto.HealthStatusDto;
import io.stepprflow.monitor.dto.HealthStatusDto.ComponentHealth;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * REST API for health status.
 * Provides a simplified health view for the UI dashboard.
 */
@RestController
@RequestMapping("/api/health")
@Tag(name = "Health", description = "System health status")
public class HealthController {

    private static final String STATUS_UP = "UP";
    private static final String STATUS_DOWN = "DOWN";

    private final MessageBroker messageBroker;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public HealthController(
            MessageBroker messageBroker,
            CircuitBreakerRegistry circuitBreakerRegistry) {
        this.messageBroker = messageBroker;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    @Operation(
            summary = "Get health status",
            description = "Get aggregated health status of all components")
    @ApiResponse(responseCode = "200", description = "System is healthy")
    @ApiResponse(responseCode = "503", description = "System is unhealthy")
    @GetMapping
    public ResponseEntity<HealthStatusDto> getHealth() {
        Map<String, ComponentHealth> components = new HashMap<>();
        boolean overallHealthy = true;

        // Check broker health
        ComponentHealth brokerHealth = checkBrokerHealth();
        components.put("broker", brokerHealth);
        if (STATUS_DOWN.equals(brokerHealth.getStatus())) {
            overallHealthy = false;
        }

        // Check circuit breakers
        ComponentHealth cbHealth = checkCircuitBreakersHealth();
        components.put("circuitBreakers", cbHealth);
        if (STATUS_DOWN.equals(cbHealth.getStatus())) {
            overallHealthy = false;
        }

        String overallStatus = overallHealthy ? STATUS_UP : STATUS_DOWN;
        HealthStatusDto dto = HealthStatusDto.builder()
                .status(overallStatus)
                .components(components)
                .build();

        HttpStatus httpStatus = overallHealthy ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(httpStatus).body(dto);
    }

    private ComponentHealth checkBrokerHealth() {
        Map<String, Object> details = new HashMap<>();
        String brokerType = messageBroker.getBrokerType();
        boolean available = messageBroker.isAvailable();

        details.put("type", brokerType);
        details.put("available", available);

        return ComponentHealth.builder()
                .status(available ? STATUS_UP : STATUS_DOWN)
                .details(details)
                .build();
    }

    private ComponentHealth checkCircuitBreakersHealth() {
        Map<String, Object> details = new HashMap<>();
        boolean allHealthy = true;

        for (CircuitBreaker cb : circuitBreakerRegistry.getAllCircuitBreakers()) {
            CircuitBreaker.State state = cb.getState();
            details.put(cb.getName(), state.name());

            if (state == CircuitBreaker.State.OPEN
                    || state == CircuitBreaker.State.FORCED_OPEN) {
                allHealthy = false;
            }
        }

        return ComponentHealth.builder()
                .status(allHealthy ? STATUS_UP : STATUS_DOWN)
                .details(details)
                .build();
    }
}
