package io.stepprflow.monitor.health;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for CircuitBreakerHealthIndicator.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CircuitBreakerHealthIndicator Tests")
class CircuitBreakerHealthIndicatorTest {

    @Mock
    private CircuitBreakerRegistry registry;

    private CircuitBreakerHealthIndicator healthIndicator;

    @BeforeEach
    void setUp() {
        healthIndicator = new CircuitBreakerHealthIndicator(registry);
    }

    @Nested
    @DisplayName("When no circuit breakers are registered")
    class WhenNoCircuitBreakers {

        @Test
        @DisplayName("Should return UP status")
        void shouldReturnUpStatus() {
            when(registry.getAllCircuitBreakers()).thenReturn(Set.of());

            Health health = healthIndicator.health();

            assertThat(health.getStatus()).isEqualTo(Status.UP);
            assertThat(health.getDetails()).containsEntry("message", "No circuit breakers registered");
        }
    }

    @Nested
    @DisplayName("When all circuit breakers are CLOSED")
    class WhenAllCircuitBreakersClosed {

        @Test
        @DisplayName("Should return UP status")
        void shouldReturnUpStatus() {
            CircuitBreaker cb1 = createCircuitBreaker("broker-kafka", CircuitBreaker.State.CLOSED);
            CircuitBreaker cb2 = createCircuitBreaker("broker-rabbitmq", CircuitBreaker.State.CLOSED);
            when(registry.getAllCircuitBreakers()).thenReturn(Set.of(cb1, cb2));

            Health health = healthIndicator.health();

            assertThat(health.getStatus()).isEqualTo(Status.UP);
            assertThat(health.getDetails()).containsEntry("broker-kafka", "CLOSED");
            assertThat(health.getDetails()).containsEntry("broker-rabbitmq", "CLOSED");
            assertThat(health.getDetails()).containsEntry("totalCircuitBreakers", 2);
            assertThat(health.getDetails()).containsEntry("openCircuitBreakers", 0);
        }
    }

    @Nested
    @DisplayName("When circuit breaker is HALF_OPEN")
    class WhenCircuitBreakerHalfOpen {

        @Test
        @DisplayName("Should return UP status")
        void shouldReturnUpStatus() {
            CircuitBreaker cb = createCircuitBreaker("broker-kafka", CircuitBreaker.State.HALF_OPEN);
            when(registry.getAllCircuitBreakers()).thenReturn(Set.of(cb));

            Health health = healthIndicator.health();

            assertThat(health.getStatus()).isEqualTo(Status.UP);
            assertThat(health.getDetails()).containsEntry("broker-kafka", "HALF_OPEN");
        }
    }

    @Nested
    @DisplayName("When circuit breaker is OPEN")
    class WhenCircuitBreakerOpen {

        @Test
        @DisplayName("Should return DOWN status")
        void shouldReturnDownStatus() {
            CircuitBreaker cb = createCircuitBreaker("broker-kafka", CircuitBreaker.State.OPEN);
            when(registry.getAllCircuitBreakers()).thenReturn(Set.of(cb));

            Health health = healthIndicator.health();

            assertThat(health.getStatus()).isEqualTo(Status.DOWN);
            assertThat(health.getDetails()).containsEntry("broker-kafka", "OPEN");
            assertThat(health.getDetails()).containsEntry("openCircuitBreakers", 1);
            assertThat(health.getDetails()).containsKey("reason");
        }

        @Test
        @DisplayName("Should include reason in details")
        void shouldIncludeReason() {
            CircuitBreaker cb = createCircuitBreaker("broker-kafka", CircuitBreaker.State.OPEN);
            when(registry.getAllCircuitBreakers()).thenReturn(Set.of(cb));

            Health health = healthIndicator.health();

            assertThat(health.getDetails().get("reason").toString())
                    .contains("1 circuit breaker(s) are OPEN");
        }
    }

    @Nested
    @DisplayName("When circuit breaker is FORCED_OPEN")
    class WhenCircuitBreakerForcedOpen {

        @Test
        @DisplayName("Should return DOWN status")
        void shouldReturnDownStatus() {
            CircuitBreaker cb = createCircuitBreaker("broker-kafka", CircuitBreaker.State.FORCED_OPEN);
            when(registry.getAllCircuitBreakers()).thenReturn(Set.of(cb));

            Health health = healthIndicator.health();

            assertThat(health.getStatus()).isEqualTo(Status.DOWN);
            assertThat(health.getDetails()).containsEntry("broker-kafka", "FORCED_OPEN");
        }
    }

    @Nested
    @DisplayName("When mixed circuit breaker states")
    class WhenMixedCircuitBreakerStates {

        @Test
        @DisplayName("Should return DOWN if any circuit breaker is OPEN")
        void shouldReturnDownIfAnyOpen() {
            CircuitBreaker cb1 = createCircuitBreaker("broker-kafka", CircuitBreaker.State.CLOSED);
            CircuitBreaker cb2 = createCircuitBreaker("broker-rabbitmq", CircuitBreaker.State.OPEN);
            when(registry.getAllCircuitBreakers()).thenReturn(Set.of(cb1, cb2));

            Health health = healthIndicator.health();

            assertThat(health.getStatus()).isEqualTo(Status.DOWN);
            assertThat(health.getDetails()).containsEntry("totalCircuitBreakers", 2);
            assertThat(health.getDetails()).containsEntry("openCircuitBreakers", 1);
        }

        @Test
        @DisplayName("Should count multiple open circuit breakers")
        void shouldCountMultipleOpen() {
            CircuitBreaker cb1 = createCircuitBreaker("cb1", CircuitBreaker.State.OPEN);
            CircuitBreaker cb2 = createCircuitBreaker("cb2", CircuitBreaker.State.FORCED_OPEN);
            CircuitBreaker cb3 = createCircuitBreaker("cb3", CircuitBreaker.State.CLOSED);
            when(registry.getAllCircuitBreakers()).thenReturn(Set.of(cb1, cb2, cb3));

            Health health = healthIndicator.health();

            assertThat(health.getStatus()).isEqualTo(Status.DOWN);
            assertThat(health.getDetails()).containsEntry("openCircuitBreakers", 2);
            assertThat(health.getDetails().get("reason").toString())
                    .contains("2 circuit breaker(s) are OPEN");
        }
    }

    private CircuitBreaker createCircuitBreaker(String name, CircuitBreaker.State state) {
        CircuitBreaker cb = mock(CircuitBreaker.class);
        when(cb.getName()).thenReturn(name);
        when(cb.getState()).thenReturn(state);
        return cb;
    }
}
