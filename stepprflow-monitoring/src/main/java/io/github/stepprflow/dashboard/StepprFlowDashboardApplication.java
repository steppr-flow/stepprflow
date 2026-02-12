package io.github.stepprflow.dashboard;

import io.github.stepprflow.dashboard.config.UiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * StepprFlow Monitoring Server.
 *
 * This is a standalone Spring Boot application that provides centralized
 * monitoring for all StepprFlow-enabled microservices.
 *
 * Features:
 * - REST API for workflow monitoring (/api/workflows, /api/metrics, /api/dashboard)
 * - WebSocket for real-time updates
 * - MongoDB persistence for workflow execution history
 * - Kafka or RabbitMQ consumer for receiving workflow events
 * - Retry scheduler for automatic workflow retries
 *
 * Deploy this as a Docker container alongside your broker (Kafka or RabbitMQ) and MongoDB.
 * The dashboard auto-detects the broker type based on classpath dependencies.
 */
@SpringBootApplication(scanBasePackages = {
        "io.github.stepprflow.dashboard",
        "io.github.stepprflow.monitor",
        "io.github.stepprflow.core"
})
@EnableConfigurationProperties(UiProperties.class)
@EnableScheduling
public class StepprFlowDashboardApplication {

    /**
     * Protected constructor to satisfy checkstyle HideUtilityClassConstructor rule.
     * Spring Boot applications are not utility classes but checkstyle cannot detect this.
     */
    protected StepprFlowDashboardApplication() {
        // Spring Boot entry point
    }

    /**
     * Application entry point.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(StepprFlowDashboardApplication.class, args);
    }
}
