package io.stepprflow.agent.registration;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.stepprflow.agent.AgentProperties;
import io.stepprflow.core.model.WorkflowDefinition;
import io.stepprflow.core.model.WorkflowRegistrationRequest;
import io.stepprflow.core.service.WorkflowRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Client that registers workflows with the StepprFlow monitoring dashboard.
 */
@Component
@ConditionalOnProperty(name = "stepprflow.agent.server-url")
@RequiredArgsConstructor
@Slf4j
public class WorkflowRegistrationClient {

    private final AgentProperties properties;
    private final WorkflowRegistry workflowRegistry;
    private final ObjectMapper objectMapper;

    @Value("${spring.application.name:unknown}")
    private String applicationName;

    @Value("${server.port:8080}")
    private int serverPort;

    private final String instanceId = UUID.randomUUID().toString().substring(0, 8);
    private HttpClient httpClient;
    private String hostname;
    private volatile boolean registered = false;
    private volatile boolean serverAvailable = true;

    @PostConstruct
    public void init() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()))
                .build();

        try {
            this.hostname = InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            this.hostname = "localhost";
        }

        if (properties.isAutoRegister()) {
            // Delay registration to ensure all workflows are registered
            new Thread(() -> {
                try {
                    Thread.sleep(5000); // Wait for Spring context to fully initialize
                    registerWorkflows();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
    }

    @PreDestroy
    public void shutdown() {
        try {
            unregister();
        } catch (Exception e) {
            log.warn("Failed to unregister from dashboard: {}", e.getMessage());
        }
    }

    /**
     * Register all workflows with the dashboard.
     */
    public void registerWorkflows() {
        if (properties.getServerUrl() == null || properties.getServerUrl().isBlank()) {
            log.debug("Server URL not configured, skipping registration");
            return;
        }

        List<WorkflowDefinition> definitions = workflowRegistry.getAllDefinitions();
        if (definitions.isEmpty()) {
            log.debug("No workflows to register");
            return;
        }

        List<WorkflowRegistrationRequest.WorkflowInfo> workflows = definitions.stream()
                .map(this::toWorkflowInfo)
                .collect(Collectors.toList());

        WorkflowRegistrationRequest request = WorkflowRegistrationRequest.builder()
                .serviceName(applicationName)
                .instanceId(instanceId)
                .host(hostname)
                .port(serverPort)
                .workflows(workflows)
                .build();

        try {
            String json = objectMapper.writeValueAsString(request);
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getServerUrl() + "/api/registry/workflows"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofMillis(properties.getReadTimeoutMs()))
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                registered = true;
                serverAvailable = true;
                log.info("Successfully registered {} workflows with dashboard at {}",
                        workflows.size(), properties.getServerUrl());
            } else {
                registered = false;
                log.warn("Failed to register workflows: HTTP {} - {}",
                        response.statusCode(), response.body());
            }
        } catch (Exception e) {
            registered = false;
            serverAvailable = false;
            log.warn("Failed to register workflows with dashboard {}: {}",
                    properties.getServerUrl(), e.getMessage());
        }
    }

    /**
     * Unregister this service instance.
     */
    public void unregister() {
        if (properties.getServerUrl() == null || properties.getServerUrl().isBlank()) {
            return;
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getServerUrl() +
                            "/api/registry/services/" + applicationName +
                            "/instances/" + instanceId))
                    .timeout(Duration.ofMillis(properties.getReadTimeoutMs()))
                    .DELETE()
                    .build();

            httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("Unregistered from dashboard");
        } catch (Exception e) {
            log.debug("Failed to unregister: {}", e.getMessage());
        }
    }

    /**
     * Send heartbeat to dashboard.
     * If the dashboard was down and comes back up, or if the dashboard doesn't recognize
     * this instance (returns 404), re-register automatically.
     */
    @Scheduled(fixedRateString = "${stepprflow.agent.heartbeat-interval-seconds:30}000")
    public void heartbeat() {
        if (properties.getServerUrl() == null ||
                properties.getServerUrl().isBlank() ||
                properties.getHeartbeatIntervalSeconds() <= 0) {
            return;
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getServerUrl() +
                            "/api/registry/services/" + applicationName +
                            "/instances/" + instanceId + "/heartbeat"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofMillis(properties.getReadTimeoutMs()))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 404) {
                // Server doesn't know about this instance - re-register
                log.info("Server returned 404, instance not found - re-registering workflows");
                registered = false;
                registerWorkflows();
            } else if (response.statusCode() == 200) {
                if (!serverAvailable) {
                    log.info("Server is back online");
                    serverAvailable = true;
                }
                log.trace("Heartbeat sent to dashboard");
            }
        } catch (Exception e) {
            if (serverAvailable) {
                log.warn("Server became unavailable: {}", e.getMessage());
                serverAvailable = false;
                registered = false;
            } else {
                // Server still down, try to re-register in case it comes back
                log.debug("Failed to send heartbeat (dashboard unavailable): {}", e.getMessage());
                // Attempt re-registration - if dashboard is back up, this will succeed
                registerWorkflows();
            }
        }
    }

    private WorkflowRegistrationRequest.WorkflowInfo toWorkflowInfo(WorkflowDefinition def) {
        List<WorkflowRegistrationRequest.StepInfo> steps = def.getSteps().stream()
                .map(s -> WorkflowRegistrationRequest.StepInfo.builder()
                        .id(s.getId())
                        .label(s.getLabel())
                        .description(s.getDescription())
                        .skippable(s.isSkippable())
                        .continueOnFailure(s.isContinueOnFailure())
                        .timeoutMs(s.getTimeout() != null ? s.getTimeout().toMillis() : null)
                        .build())
                .collect(Collectors.toList());

        return WorkflowRegistrationRequest.WorkflowInfo.builder()
                .topic(def.getTopic())
                .description(def.getDescription())
                .steps(steps)
                .partitions(def.getPartitions())
                .replication(def.getReplication())
                .timeoutMs(def.getTimeout() != null ? def.getTimeout().toMillis() : null)
                .build();
    }
}
