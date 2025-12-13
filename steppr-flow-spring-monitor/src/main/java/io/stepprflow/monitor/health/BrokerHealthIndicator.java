package io.stepprflow.monitor.health;

import io.stepprflow.core.broker.MessageBroker;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Health indicator for message broker connectivity.
 *
 * <p>This indicator checks if the message broker (Kafka or RabbitMQ) is available
 * and able to accept messages. It uses the {@link MessageBroker#isAvailable()} method
 * which also considers circuit breaker state.
 *
 * <p>Health states:
 * <ul>
 *   <li>UP - Broker is connected and available</li>
 *   <li>DOWN - Broker is unavailable (disconnected or circuit breaker open)</li>
 * </ul>
 */
@Component
public class BrokerHealthIndicator implements HealthIndicator {

    private final MessageBroker messageBroker;

    /**
     * Creates a new broker health indicator.
     *
     * @param messageBroker the message broker to monitor
     */
    public BrokerHealthIndicator(MessageBroker messageBroker) {
        this.messageBroker = messageBroker;
    }

    @Override
    public Health health() {
        try {
            boolean available = messageBroker.isAvailable();
            String brokerType = messageBroker.getBrokerType();

            if (available) {
                return Health.up()
                        .withDetail("brokerType", brokerType)
                        .withDetail("available", true)
                        .build();
            } else {
                return Health.down()
                        .withDetail("brokerType", brokerType)
                        .withDetail("available", false)
                        .withDetail("reason", "Broker is not available")
                        .build();
            }
        } catch (Exception e) {
            return Health.down()
                    .withDetail("brokerType", getBrokerTypeSafe())
                    .withDetail("available", false)
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }

    private String getBrokerTypeSafe() {
        try {
            return messageBroker.getBrokerType();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
