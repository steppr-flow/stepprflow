package io.stepprflow.monitor.health;

import io.stepprflow.core.broker.MessageBroker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for BrokerHealthIndicator.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BrokerHealthIndicator Tests")
class BrokerHealthIndicatorTest {

    @Mock
    private MessageBroker messageBroker;

    private BrokerHealthIndicator healthIndicator;

    @BeforeEach
    void setUp() {
        healthIndicator = new BrokerHealthIndicator(messageBroker);
    }

    @Nested
    @DisplayName("When broker is available")
    class WhenBrokerAvailable {

        @Test
        @DisplayName("Should return UP status")
        void shouldReturnUpStatus() {
            when(messageBroker.isAvailable()).thenReturn(true);
            when(messageBroker.getBrokerType()).thenReturn("kafka");

            Health health = healthIndicator.health();

            assertThat(health.getStatus()).isEqualTo(Status.UP);
            assertThat(health.getDetails()).containsEntry("brokerType", "kafka");
            assertThat(health.getDetails()).containsEntry("available", true);
        }

        @Test
        @DisplayName("Should include broker type in details")
        void shouldIncludeBrokerType() {
            when(messageBroker.isAvailable()).thenReturn(true);
            when(messageBroker.getBrokerType()).thenReturn("rabbitmq");

            Health health = healthIndicator.health();

            assertThat(health.getDetails()).containsEntry("brokerType", "rabbitmq");
        }
    }

    @Nested
    @DisplayName("When broker is unavailable")
    class WhenBrokerUnavailable {

        @Test
        @DisplayName("Should return DOWN status")
        void shouldReturnDownStatus() {
            when(messageBroker.isAvailable()).thenReturn(false);
            when(messageBroker.getBrokerType()).thenReturn("kafka");

            Health health = healthIndicator.health();

            assertThat(health.getStatus()).isEqualTo(Status.DOWN);
            assertThat(health.getDetails()).containsEntry("available", false);
            assertThat(health.getDetails()).containsKey("reason");
        }

        @Test
        @DisplayName("Should include reason in details")
        void shouldIncludeReason() {
            when(messageBroker.isAvailable()).thenReturn(false);
            when(messageBroker.getBrokerType()).thenReturn("kafka");

            Health health = healthIndicator.health();

            assertThat(health.getDetails().get("reason")).isNotNull();
        }
    }

    @Nested
    @DisplayName("When broker throws exception")
    class WhenBrokerThrowsException {

        @Test
        @DisplayName("Should return DOWN status with error")
        void shouldReturnDownStatusWithError() {
            when(messageBroker.isAvailable()).thenThrow(new RuntimeException("Connection failed"));
            when(messageBroker.getBrokerType()).thenReturn("kafka");

            Health health = healthIndicator.health();

            assertThat(health.getStatus()).isEqualTo(Status.DOWN);
            assertThat(health.getDetails()).containsEntry("available", false);
            assertThat(health.getDetails()).containsEntry("error", "Connection failed");
        }

        @Test
        @DisplayName("Should handle getBrokerType exception gracefully")
        void shouldHandleGetBrokerTypeException() {
            when(messageBroker.isAvailable()).thenThrow(new RuntimeException("Error"));
            when(messageBroker.getBrokerType()).thenThrow(new RuntimeException("Type error"));

            Health health = healthIndicator.health();

            assertThat(health.getStatus()).isEqualTo(Status.DOWN);
            assertThat(health.getDetails()).containsEntry("brokerType", "unknown");
        }
    }
}
