package io.stepprflow.agent.registration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.stepprflow.agent.AgentProperties;
import io.stepprflow.core.model.StepDefinition;
import io.stepprflow.core.model.WorkflowDefinition;
import io.stepprflow.core.service.WorkflowRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.mockito.Mockito.when;

/**
 * Tests for WorkflowRegistrationClient.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WorkflowRegistrationClient Tests")
class WorkflowRegistrationClientTest {

    @Mock
    private WorkflowRegistry workflowRegistry;

    private AgentProperties properties;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        properties = new AgentProperties();
        objectMapper = new ObjectMapper();
    }

    @Nested
    @DisplayName("registerWorkflows()")
    class RegisterWorkflowsTests {

        @Test
        @DisplayName("should skip registration when serverUrl is null")
        void shouldSkipRegistrationWhenServerUrlIsNull() {
            properties.setServerUrl(null);
            WorkflowRegistrationClient client = new WorkflowRegistrationClient(
                    properties, workflowRegistry, objectMapper);

            // Should not throw and should skip registration
            client.registerWorkflows();
            // No exception means success
        }

        @Test
        @DisplayName("should skip registration when serverUrl is blank")
        void shouldSkipRegistrationWhenServerUrlIsBlank() {
            properties.setServerUrl("   ");
            WorkflowRegistrationClient client = new WorkflowRegistrationClient(
                    properties, workflowRegistry, objectMapper);

            client.registerWorkflows();
            // No exception means success
        }

        @Test
        @DisplayName("should skip registration when no workflows are registered")
        void shouldSkipRegistrationWhenNoWorkflows() {
            properties.setServerUrl("http://localhost:8090");
            when(workflowRegistry.getAllDefinitions()).thenReturn(Collections.emptyList());

            WorkflowRegistrationClient client = new WorkflowRegistrationClient(
                    properties, workflowRegistry, objectMapper);

            client.registerWorkflows();
            // No exception means success
        }
    }

    @Nested
    @DisplayName("unregister()")
    class UnregisterTests {

        @Test
        @DisplayName("should skip unregistration when serverUrl is null")
        void shouldSkipUnregistrationWhenServerUrlIsNull() {
            properties.setServerUrl(null);
            WorkflowRegistrationClient client = new WorkflowRegistrationClient(
                    properties, workflowRegistry, objectMapper);

            client.unregister();
            // No exception means success
        }

        @Test
        @DisplayName("should skip unregistration when serverUrl is blank")
        void shouldSkipUnregistrationWhenServerUrlIsBlank() {
            properties.setServerUrl("   ");
            WorkflowRegistrationClient client = new WorkflowRegistrationClient(
                    properties, workflowRegistry, objectMapper);

            client.unregister();
            // No exception means success
        }
    }

    @Nested
    @DisplayName("heartbeat()")
    class HeartbeatTests {

        @Test
        @DisplayName("should skip heartbeat when serverUrl is null")
        void shouldSkipHeartbeatWhenServerUrlIsNull() {
            properties.setServerUrl(null);
            WorkflowRegistrationClient client = new WorkflowRegistrationClient(
                    properties, workflowRegistry, objectMapper);

            client.heartbeat();
            // No exception means success
        }

        @Test
        @DisplayName("should skip heartbeat when serverUrl is blank")
        void shouldSkipHeartbeatWhenServerUrlIsBlank() {
            properties.setServerUrl("   ");
            WorkflowRegistrationClient client = new WorkflowRegistrationClient(
                    properties, workflowRegistry, objectMapper);

            client.heartbeat();
            // No exception means success
        }

        @Test
        @DisplayName("should skip heartbeat when heartbeat interval is 0")
        void shouldSkipHeartbeatWhenIntervalIsZero() {
            properties.setServerUrl("http://localhost:8090");
            properties.setHeartbeatIntervalSeconds(0);
            WorkflowRegistrationClient client = new WorkflowRegistrationClient(
                    properties, workflowRegistry, objectMapper);

            client.heartbeat();
            // No exception means success
        }

        @Test
        @DisplayName("should skip heartbeat when heartbeat interval is negative")
        void shouldSkipHeartbeatWhenIntervalIsNegative() {
            properties.setServerUrl("http://localhost:8090");
            properties.setHeartbeatIntervalSeconds(-1);
            WorkflowRegistrationClient client = new WorkflowRegistrationClient(
                    properties, workflowRegistry, objectMapper);

            client.heartbeat();
            // No exception means success
        }
    }

    @Nested
    @DisplayName("Workflow definition conversion")
    class WorkflowDefinitionConversionTests {

        @Test
        @DisplayName("should handle workflow with steps")
        void shouldHandleWorkflowWithSteps() {
            properties.setServerUrl("http://localhost:8090");

            StepDefinition step1 = StepDefinition.builder()
                    .id(1)
                    .label("Step 1")
                    .description("First step")
                    .skippable(false)
                    .continueOnFailure(false)
                    .timeout(Duration.ofSeconds(30))
                    .build();

            StepDefinition step2 = StepDefinition.builder()
                    .id(2)
                    .label("Step 2")
                    .description("Second step")
                    .skippable(true)
                    .continueOnFailure(true)
                    .build();

            WorkflowDefinition definition = WorkflowDefinition.builder()
                    .topic("test-workflow")
                    .description("Test workflow")
                    .steps(List.of(step1, step2))
                    .partitions(3)
                    .replication((short) 2)
                    .timeout(Duration.ofMinutes(5))
                    .build();

            when(workflowRegistry.getAllDefinitions()).thenReturn(List.of(definition));

            WorkflowRegistrationClient client = new WorkflowRegistrationClient(
                    properties, workflowRegistry, objectMapper);

            // This will fail to connect but we're testing that conversion works
            client.registerWorkflows();
            // No exception during conversion means success
        }
    }

    @Nested
    @DisplayName("HTTP Integration Tests")
    class HttpIntegrationTests {

        private WireMockServer wireMockServer;

        @BeforeEach
        void startWireMock() {
            wireMockServer = new WireMockServer(0); // Random port
            wireMockServer.start();
            WireMock.configureFor("localhost", wireMockServer.port());
        }

        @AfterEach
        void stopWireMock() {
            if (wireMockServer != null) {
                wireMockServer.stop();
            }
        }

        @Test
        @DisplayName("should successfully register workflows when server returns 200")
        void shouldSuccessfullyRegisterWorkflows() {
            // Setup mock server
            wireMockServer.stubFor(post(urlPathMatching("/api/registry/workflows"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withBody("{\"status\":\"ok\"}")));

            properties.setServerUrl("http://localhost:" + wireMockServer.port());
            properties.setConnectTimeoutMs(5000);
            properties.setReadTimeoutMs(5000);

            WorkflowDefinition definition = WorkflowDefinition.builder()
                    .topic("test-workflow")
                    .description("Test")
                    .steps(List.of(StepDefinition.builder().id(1).label("Step 1").build()))
                    .partitions(1)
                    .replication((short) 1)
                    .build();

            when(workflowRegistry.getAllDefinitions()).thenReturn(List.of(definition));

            WorkflowRegistrationClient client = new WorkflowRegistrationClient(
                    properties, workflowRegistry, objectMapper);
            client.init();
            client.registerWorkflows();

            wireMockServer.verify(1, WireMock.postRequestedFor(
                    urlPathMatching("/api/registry/workflows")));
        }

        @Test
        @DisplayName("should handle registration failure when server returns error")
        void shouldHandleRegistrationFailure() {
            wireMockServer.stubFor(post(urlPathMatching("/api/registry/workflows"))
                    .willReturn(aResponse()
                            .withStatus(500)
                            .withBody("Internal Server Error")));

            properties.setServerUrl("http://localhost:" + wireMockServer.port());
            properties.setConnectTimeoutMs(5000);
            properties.setReadTimeoutMs(5000);

            WorkflowDefinition definition = WorkflowDefinition.builder()
                    .topic("test-workflow")
                    .description("Test")
                    .steps(List.of(StepDefinition.builder().id(1).label("Step 1").build()))
                    .partitions(1)
                    .replication((short) 1)
                    .build();

            when(workflowRegistry.getAllDefinitions()).thenReturn(List.of(definition));

            WorkflowRegistrationClient client = new WorkflowRegistrationClient(
                    properties, workflowRegistry, objectMapper);
            client.init();
            client.registerWorkflows();

            // Should not throw exception
            wireMockServer.verify(1, WireMock.postRequestedFor(
                    urlPathMatching("/api/registry/workflows")));
        }

        @Test
        @DisplayName("should successfully unregister")
        void shouldSuccessfullyUnregister() {
            wireMockServer.stubFor(delete(urlPathMatching("/api/registry/services/.*/instances/.*"))
                    .willReturn(aResponse().withStatus(200)));

            properties.setServerUrl("http://localhost:" + wireMockServer.port());
            properties.setConnectTimeoutMs(5000);
            properties.setReadTimeoutMs(5000);

            WorkflowRegistrationClient client = new WorkflowRegistrationClient(
                    properties, workflowRegistry, objectMapper);
            client.init();
            client.unregister();

            wireMockServer.verify(1, WireMock.deleteRequestedFor(
                    urlPathMatching("/api/registry/services/.*/instances/.*")));
        }

        @Test
        @DisplayName("should handle heartbeat success")
        void shouldHandleHeartbeatSuccess() {
            wireMockServer.stubFor(post(urlPathMatching("/api/registry/services/.*/instances/.*/heartbeat"))
                    .willReturn(aResponse().withStatus(200)));

            properties.setServerUrl("http://localhost:" + wireMockServer.port());
            properties.setConnectTimeoutMs(5000);
            properties.setReadTimeoutMs(5000);
            properties.setHeartbeatIntervalSeconds(30);

            WorkflowRegistrationClient client = new WorkflowRegistrationClient(
                    properties, workflowRegistry, objectMapper);
            client.init();
            client.heartbeat();

            wireMockServer.verify(1, WireMock.postRequestedFor(
                    urlPathMatching("/api/registry/services/.*/instances/.*/heartbeat")));
        }

        @Test
        @DisplayName("should re-register when heartbeat returns 404")
        void shouldReRegisterWhenHeartbeatReturns404() {
            // First heartbeat returns 404
            wireMockServer.stubFor(post(urlPathMatching("/api/registry/services/.*/instances/.*/heartbeat"))
                    .willReturn(aResponse().withStatus(404)));
            // Re-registration succeeds
            wireMockServer.stubFor(post(urlPathMatching("/api/registry/workflows"))
                    .willReturn(aResponse().withStatus(200)));

            properties.setServerUrl("http://localhost:" + wireMockServer.port());
            properties.setConnectTimeoutMs(5000);
            properties.setReadTimeoutMs(5000);
            properties.setHeartbeatIntervalSeconds(30);

            WorkflowDefinition definition = WorkflowDefinition.builder()
                    .topic("test-workflow")
                    .description("Test")
                    .steps(List.of(StepDefinition.builder().id(1).label("Step 1").build()))
                    .partitions(1)
                    .replication((short) 1)
                    .build();

            when(workflowRegistry.getAllDefinitions()).thenReturn(List.of(definition));

            WorkflowRegistrationClient client = new WorkflowRegistrationClient(
                    properties, workflowRegistry, objectMapper);
            client.init();
            client.heartbeat();

            // Should have tried heartbeat and then re-registered
            wireMockServer.verify(1, WireMock.postRequestedFor(
                    urlPathMatching("/api/registry/services/.*/instances/.*/heartbeat")));
            wireMockServer.verify(1, WireMock.postRequestedFor(
                    urlPathMatching("/api/registry/workflows")));
        }

        @Test
        @DisplayName("should handle shutdown gracefully")
        void shouldHandleShutdownGracefully() {
            wireMockServer.stubFor(delete(urlPathMatching("/api/registry/services/.*/instances/.*"))
                    .willReturn(aResponse().withStatus(200)));

            properties.setServerUrl("http://localhost:" + wireMockServer.port());
            properties.setConnectTimeoutMs(5000);
            properties.setReadTimeoutMs(5000);

            WorkflowRegistrationClient client = new WorkflowRegistrationClient(
                    properties, workflowRegistry, objectMapper);
            client.init();
            client.shutdown();

            wireMockServer.verify(1, WireMock.deleteRequestedFor(
                    urlPathMatching("/api/registry/services/.*/instances/.*")));
        }

        @Test
        @DisplayName("should handle shutdown failure gracefully")
        void shouldHandleShutdownFailureGracefully() {
            wireMockServer.stubFor(delete(urlPathMatching("/api/registry/services/.*/instances/.*"))
                    .willReturn(aResponse().withStatus(500)));

            properties.setServerUrl("http://localhost:" + wireMockServer.port());
            properties.setConnectTimeoutMs(5000);
            properties.setReadTimeoutMs(5000);

            WorkflowRegistrationClient client = new WorkflowRegistrationClient(
                    properties, workflowRegistry, objectMapper);
            client.init();
            client.shutdown(); // Should not throw

            wireMockServer.verify(1, WireMock.deleteRequestedFor(
                    urlPathMatching("/api/registry/services/.*/instances/.*")));
        }
    }
}