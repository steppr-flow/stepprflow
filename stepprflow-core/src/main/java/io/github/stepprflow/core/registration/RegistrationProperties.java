package io.github.stepprflow.core.registration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for workflow registration via the message broker.
 *
 * <p>Registration uses the shared broker (Kafka or RabbitMQ) to send
 * workflow definitions to the monitoring dashboard. No additional
 * server URL configuration is required.
 */
@ConfigurationProperties(prefix = "stepprflow.registration")
public class RegistrationProperties {

    /**
     * Whether registration is enabled.
     */
    private boolean enabled = true;

    /**
     * Interval in seconds between heartbeat signals.
     */
    private int heartbeatIntervalSeconds = 30;

    /**
     * Whether registration is enabled.
     *
     * @return true if registration is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Set whether registration is enabled.
     *
     * @param enabled true to enable registration
     */
    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Get the heartbeat interval in seconds.
     *
     * @return the heartbeat interval
     */
    public int getHeartbeatIntervalSeconds() {
        return heartbeatIntervalSeconds;
    }

    /**
     * Set the heartbeat interval in seconds.
     *
     * @param heartbeatIntervalSeconds the heartbeat interval
     */
    public void setHeartbeatIntervalSeconds(final int heartbeatIntervalSeconds) {
        this.heartbeatIntervalSeconds = heartbeatIntervalSeconds;
    }
}
