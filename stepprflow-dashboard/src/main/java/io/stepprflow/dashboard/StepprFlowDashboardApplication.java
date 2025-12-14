package io.stepprflow.dashboard;

import io.stepprflow.dashboard.config.UiProperties;
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
 * - Kafka consumer for receiving workflow events from microservices
 * - Retry scheduler for automatic workflow retries
 *
 * Deploy this as a Docker container alongside Kafka and MongoDB.
 */
@SpringBootApplication(scanBasePackages = {
        "io.stepprflow.dashboard",
        "io.stepprflow.monitor",
        "io.stepprflow.broker.kafka",
        "io.stepprflow.core"
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
