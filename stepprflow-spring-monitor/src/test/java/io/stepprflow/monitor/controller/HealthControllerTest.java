package io.stepprflow.monitor.controller;

import io.stepprflow.core.broker.MessageBroker;
import io.stepprflow.monitor.dto.HealthStatusDto;
import io.stepprflow.monitor.dto.HealthStatusDto.ComponentHealth;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * TDD tests for HealthController.
 * Tests written BEFORE implementation.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HealthController Tests")
class HealthControllerTest {

    @Mock
    private MessageBroker messageBroker;

    @Mock
    private CircuitBreakerRegistry circuitBreakerRegistry;

    private HealthController controller;

    @BeforeEach
    void setUp() {
        controller = new HealthController(messageBroker, circuitBreakerRegistry);
    }

    @Nested
    @DisplayName("GET /api/health")
    class GetHealth {

        @Test
        @DisplayName("Should return UP when all components are healthy")
        void shouldReturnUpWhenAllHealthy() {
            // Given
            when(messageBroker.isAvailable()).thenReturn(true);
            when(messageBroker.getBrokerType()).thenReturn("kafka");
            when(circuitBreakerRegistry.getAllCircuitBreakers()).thenReturn(Set.of());

            // When
            ResponseEntity<HealthStatusDto> response = controller.getHealth();

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo("UP");
        }

        @Test
        @DisplayName("Should include broker component health")
        void shouldIncludeBrokerHealth() {
            // Given
            when(messageBroker.isAvailable()).thenReturn(true);
            when(messageBroker.getBrokerType()).thenReturn("kafka");
            when(circuitBreakerRegistry.getAllCircuitBreakers()).thenReturn(Set.of());

            // When
            ResponseEntity<HealthStatusDto> response = controller.getHealth();

            // Then
            assertThat(response.getBody().getComponents()).containsKey("broker");
            ComponentHealth broker = response.getBody().getComponents().get("broker");
            assertThat(broker.getStatus()).isEqualTo("UP");
            assertThat(broker.getDetails()).containsEntry("type", "kafka");
            assertThat(broker.getDetails()).containsEntry("available", true);
        }

        @Test
        @DisplayName("Should return DOWN when broker unavailable")
        void shouldReturnDownWhenBrokerUnavailable() {
            // Given
            when(messageBroker.isAvailable()).thenReturn(false);
            when(messageBroker.getBrokerType()).thenReturn("kafka");
            when(circuitBreakerRegistry.getAllCircuitBreakers()).thenReturn(Set.of());

            // When
            ResponseEntity<HealthStatusDto> response = controller.getHealth();

            // Then
            assertThat(response.getBody().getStatus()).isEqualTo("DOWN");
            ComponentHealth broker = response.getBody().getComponents().get("broker");
            assertThat(broker.getStatus()).isEqualTo("DOWN");
        }

        @Test
        @DisplayName("Should include circuit breaker health when all closed")
        void shouldIncludeCircuitBreakerHealthWhenClosed() {
            // Given
            when(messageBroker.isAvailable()).thenReturn(true);
            when(messageBroker.getBrokerType()).thenReturn("kafka");

            CircuitBreaker cb = mock(CircuitBreaker.class);
            when(cb.getName()).thenReturn("broker-kafka");
            when(cb.getState()).thenReturn(CircuitBreaker.State.CLOSED);
            when(circuitBreakerRegistry.getAllCircuitBreakers()).thenReturn(Set.of(cb));

            // When
            ResponseEntity<HealthStatusDto> response = controller.getHealth();

            // Then
            assertThat(response.getBody().getComponents()).containsKey("circuitBreakers");
            ComponentHealth cbHealth = response.getBody().getComponents().get("circuitBreakers");
            assertThat(cbHealth.getStatus()).isEqualTo("UP");
            assertThat(cbHealth.getDetails()).containsEntry("broker-kafka", "CLOSED");
        }

        @Test
        @DisplayName("Should return DOWN when circuit breaker is open")
        void shouldReturnDownWhenCircuitBreakerOpen() {
            // Given
            when(messageBroker.isAvailable()).thenReturn(true);
            when(messageBroker.getBrokerType()).thenReturn("kafka");

            CircuitBreaker cb = mock(CircuitBreaker.class);
            when(cb.getName()).thenReturn("broker-kafka");
            when(cb.getState()).thenReturn(CircuitBreaker.State.OPEN);
            when(circuitBreakerRegistry.getAllCircuitBreakers()).thenReturn(Set.of(cb));

            // When
            ResponseEntity<HealthStatusDto> response = controller.getHealth();

            // Then
            assertThat(response.getBody().getStatus()).isEqualTo("DOWN");
            ComponentHealth cbHealth = response.getBody().getComponents().get("circuitBreakers");
            assertThat(cbHealth.getStatus()).isEqualTo("DOWN");
        }

        @Test
        @DisplayName("Should return 503 when health is DOWN")
        void shouldReturn503WhenDown() {
            // Given
            when(messageBroker.isAvailable()).thenReturn(false);
            when(messageBroker.getBrokerType()).thenReturn("kafka");
            when(circuitBreakerRegistry.getAllCircuitBreakers()).thenReturn(Set.of());

            // When
            ResponseEntity<HealthStatusDto> response = controller.getHealth();

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
}
