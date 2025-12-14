package io.stepprflow.monitor.controller;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.stepprflow.core.StepprFlowProperties;
import io.stepprflow.monitor.dto.CircuitBreakerStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * TDD tests for CircuitBreakerController.
 */
@ExtendWith(MockitoExtension.class)
class CircuitBreakerControllerTest {

    private CircuitBreakerRegistry registry;
    @Mock
    private StepprFlowProperties properties;
    private CircuitBreakerController controller;

    @BeforeEach
    void setUp() {
        registry = CircuitBreakerRegistry.ofDefaults();
        controller = new CircuitBreakerController(registry, properties);
    }

    @Nested
    @DisplayName("GET /api/circuit-breakers/config")
    class GetConfig {

        @Test
        @DisplayName("should return circuit breaker configuration")
        void shouldReturnConfig() {
            StepprFlowProperties.CircuitBreaker cbConfig = new StepprFlowProperties.CircuitBreaker();
            cbConfig.setEnabled(true);
            cbConfig.setFailureRateThreshold(50);
            cbConfig.setSlowCallRateThreshold(100);
            cbConfig.setSlowCallDurationThreshold(Duration.ofSeconds(10));
            cbConfig.setSlidingWindowSize(10);
            cbConfig.setMinimumNumberOfCalls(5);
            cbConfig.setPermittedNumberOfCallsInHalfOpenState(3);
            cbConfig.setWaitDurationInOpenState(Duration.ofSeconds(30));
            cbConfig.setAutomaticTransitionFromOpenToHalfOpenEnabled(true);

            when(properties.getCircuitBreaker()).thenReturn(cbConfig);

            ResponseEntity<io.stepprflow.monitor.dto.CircuitBreakerConfig> response = controller.getConfig();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isEnabled()).isTrue();
            assertThat(response.getBody().getFailureRateThreshold()).isEqualTo(50);
            assertThat(response.getBody().getSlowCallRateThreshold()).isEqualTo(100);
            assertThat(response.getBody().getSlowCallDurationThresholdMs()).isEqualTo(10000);
            assertThat(response.getBody().getSlidingWindowSize()).isEqualTo(10);
            assertThat(response.getBody().getMinimumNumberOfCalls()).isEqualTo(5);
            assertThat(response.getBody().getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(3);
            assertThat(response.getBody().getWaitDurationInOpenStateMs()).isEqualTo(30000);
            assertThat(response.getBody().isAutomaticTransitionFromOpenToHalfOpenEnabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("GET /api/circuit-breakers")
    class GetAllCircuitBreakers {

        @Test
        @DisplayName("should return empty list when no circuit breakers registered")
        void shouldReturnEmptyList() {
            ResponseEntity<List<CircuitBreakerStatus>> response = controller.getAllCircuitBreakers();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEmpty();
        }

        @Test
        @DisplayName("should return all registered circuit breakers")
        void shouldReturnAllCircuitBreakers() {
            registry.circuitBreaker("broker-kafka");
            registry.circuitBreaker("broker-rabbitmq");

            ResponseEntity<List<CircuitBreakerStatus>> response = controller.getAllCircuitBreakers();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(2);
            assertThat(response.getBody())
                    .extracting(CircuitBreakerStatus::getName)
                    .containsExactlyInAnyOrder("broker-kafka", "broker-rabbitmq");
        }

        @Test
        @DisplayName("should include circuit breaker state")
        void shouldIncludeState() {
            CircuitBreaker cb = registry.circuitBreaker("broker-kafka");

            ResponseEntity<List<CircuitBreakerStatus>> response = controller.getAllCircuitBreakers();

            assertThat(response.getBody()).hasSize(1);
            assertThat(response.getBody().get(0).getState()).isEqualTo("CLOSED");
        }

        @Test
        @DisplayName("should include metrics")
        void shouldIncludeMetrics() {
            CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                    .slidingWindowSize(4)
                    .minimumNumberOfCalls(2)
                    .failureRateThreshold(50)
                    .build();
            CircuitBreaker cb = registry.circuitBreaker("broker-kafka", config);

            // Simulate some calls
            cb.onSuccess(100, java.util.concurrent.TimeUnit.MILLISECONDS);
            cb.onSuccess(200, java.util.concurrent.TimeUnit.MILLISECONDS);
            cb.onError(50, java.util.concurrent.TimeUnit.MILLISECONDS, new RuntimeException("error"));

            ResponseEntity<List<CircuitBreakerStatus>> response = controller.getAllCircuitBreakers();

            CircuitBreakerStatus status = response.getBody().get(0);
            assertThat(status.getSuccessfulCalls()).isEqualTo(2);
            assertThat(status.getFailedCalls()).isEqualTo(1);
            assertThat(status.getFailureRate()).isGreaterThan(0);
        }
    }

    @Nested
    @DisplayName("GET /api/circuit-breakers/{name}")
    class GetCircuitBreakerByName {

        @Test
        @DisplayName("should return 404 when circuit breaker not found")
        void shouldReturn404WhenNotFound() {
            ResponseEntity<CircuitBreakerStatus> response = controller.getCircuitBreaker("unknown");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("should return circuit breaker by name")
        void shouldReturnCircuitBreakerByName() {
            registry.circuitBreaker("broker-kafka");

            ResponseEntity<CircuitBreakerStatus> response = controller.getCircuitBreaker("broker-kafka");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getName()).isEqualTo("broker-kafka");
        }
    }

    @Nested
    @DisplayName("Circuit breaker states")
    class CircuitBreakerStates {

        @Test
        @DisplayName("should reflect OPEN state")
        void shouldReflectOpenState() {
            CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                    .slidingWindowSize(2)
                    .minimumNumberOfCalls(2)
                    .failureRateThreshold(50)
                    .build();
            CircuitBreaker cb = registry.circuitBreaker("broker-kafka", config);

            // Force failures to open circuit
            cb.onError(50, java.util.concurrent.TimeUnit.MILLISECONDS, new RuntimeException("error"));
            cb.onError(50, java.util.concurrent.TimeUnit.MILLISECONDS, new RuntimeException("error"));

            ResponseEntity<List<CircuitBreakerStatus>> response = controller.getAllCircuitBreakers();

            assertThat(response.getBody().get(0).getState()).isEqualTo("OPEN");
        }

        @Test
        @DisplayName("should reflect HALF_OPEN state")
        void shouldReflectHalfOpenState() {
            CircuitBreaker cb = registry.circuitBreaker("broker-kafka");
            cb.transitionToOpenState();
            cb.transitionToHalfOpenState();

            ResponseEntity<List<CircuitBreakerStatus>> response = controller.getAllCircuitBreakers();

            assertThat(response.getBody().get(0).getState()).isEqualTo("HALF_OPEN");
        }
    }

    @Nested
    @DisplayName("Circuit breaker actions")
    class CircuitBreakerActions {

        @Test
        @DisplayName("should reset circuit breaker")
        void shouldResetCircuitBreaker() {
            CircuitBreaker cb = registry.circuitBreaker("broker-kafka");
            cb.transitionToOpenState();

            ResponseEntity<CircuitBreakerStatus> response = controller.resetCircuitBreaker("broker-kafka");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getState()).isEqualTo("CLOSED");
        }

        @Test
        @DisplayName("should return 404 when resetting unknown circuit breaker")
        void shouldReturn404WhenResettingUnknown() {
            ResponseEntity<CircuitBreakerStatus> response = controller.resetCircuitBreaker("unknown");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }
}