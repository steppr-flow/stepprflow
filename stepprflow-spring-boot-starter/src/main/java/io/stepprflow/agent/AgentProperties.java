package io.stepprflow.agent;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for StepprFlow Agent.
 */
@Data
@ConfigurationProperties(prefix = "stepprflow.agent")
public class AgentProperties {

    /**
     * URL of the StepprFlow monitoring dashboard.
     * If set, the agent will register workflows at startup.
     */
    private String serverUrl;

    /**
     * Whether to enable auto-registration with the dashboard.
     */
    private boolean autoRegister = true;

    /**
     * Heartbeat interval in seconds (0 to disable).
     */
    private int heartbeatIntervalSeconds = 30;

    /**
     * Connection timeout in milliseconds.
     */
    private int connectTimeoutMs = 5000;

    /**
     * Read timeout in milliseconds.
     */
    private int readTimeoutMs = 10000;
}
