package io.github.stepprflow.monitor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.stepprflow.core.model.WorkflowMessage;
import io.github.stepprflow.core.model.WorkflowRegistrationRequest;
import io.github.stepprflow.core.model.WorkflowStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DisplayName("RegistrationMessageHandler Tests")
@ExtendWith(MockitoExtension.class)
class RegistrationMessageHandlerTest {

    @Mock
    private WorkflowRegistryService registryService;

    private RegistrationMessageHandler handler;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        handler = new RegistrationMessageHandler(registryService, objectMapper);
    }

    @Test
    @DisplayName("Should handle REGISTER action")
    void shouldHandleRegisterAction() {
        WorkflowRegistrationRequest request = WorkflowRegistrationRequest.builder()
                .serviceName("order-service")
                .instanceId("inst-1")
                .host("localhost")
                .port(8080)
                .workflows(List.of(
                        WorkflowRegistrationRequest.WorkflowInfo.builder()
                                .topic("order-workflow")
                                .description("Order processing")
                                .steps(List.of(
                                        WorkflowRegistrationRequest.StepInfo.builder()
                                                .id(1)
                                                .label("Validate")
                                                .build()
                                ))
                                .build()
                ))
                .build();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put(WorkflowRegistrationRequest.METADATA_ACTION,
                WorkflowRegistrationRequest.ACTION_REGISTER);
        metadata.put(WorkflowRegistrationRequest.METADATA_INSTANCE_ID, "inst-1");

        // Convert request to a Map (simulating Jackson deserialization from broker)
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        Object payload = mapper.convertValue(request, Map.class);

        WorkflowMessage message = WorkflowMessage.builder()
                .executionId("exec-1")
                .topic(WorkflowRegistrationRequest.REGISTRATION_TOPIC)
                .serviceName("order-service")
                .status(WorkflowStatus.COMPLETED)
                .payload(payload)
                .metadata(metadata)
                .build();

        handler.handle(message);

        ArgumentCaptor<WorkflowRegistrationRequest> captor =
                ArgumentCaptor.forClass(WorkflowRegistrationRequest.class);
        verify(registryService).registerWorkflows(captor.capture());

        WorkflowRegistrationRequest captured = captor.getValue();
        assertThat(captured.getServiceName()).isEqualTo("order-service");
        assertThat(captured.getInstanceId()).isEqualTo("inst-1");
        assertThat(captured.getWorkflows()).hasSize(1);
        assertThat(captured.getWorkflows().get(0).getTopic()).isEqualTo("order-workflow");
    }

    @Test
    @DisplayName("Should handle HEARTBEAT action")
    void shouldHandleHeartbeatAction() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put(WorkflowRegistrationRequest.METADATA_ACTION,
                WorkflowRegistrationRequest.ACTION_HEARTBEAT);
        metadata.put(WorkflowRegistrationRequest.METADATA_INSTANCE_ID, "inst-1");

        WorkflowMessage message = WorkflowMessage.builder()
                .executionId("exec-2")
                .topic(WorkflowRegistrationRequest.REGISTRATION_TOPIC)
                .serviceName("order-service")
                .status(WorkflowStatus.COMPLETED)
                .metadata(metadata)
                .build();

        handler.handle(message);

        verify(registryService).heartbeat("order-service", "inst-1");
        verify(registryService, never()).registerWorkflows(any());
        verify(registryService, never()).unregisterService(any(), any());
    }

    @Test
    @DisplayName("Should handle DEREGISTER action")
    void shouldHandleDeregisterAction() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put(WorkflowRegistrationRequest.METADATA_ACTION,
                WorkflowRegistrationRequest.ACTION_DEREGISTER);
        metadata.put(WorkflowRegistrationRequest.METADATA_INSTANCE_ID, "inst-1");

        WorkflowMessage message = WorkflowMessage.builder()
                .executionId("exec-3")
                .topic(WorkflowRegistrationRequest.REGISTRATION_TOPIC)
                .serviceName("order-service")
                .status(WorkflowStatus.COMPLETED)
                .metadata(metadata)
                .build();

        handler.handle(message);

        verify(registryService).unregisterService("order-service", "inst-1");
        verify(registryService, never()).registerWorkflows(any());
        verify(registryService, never()).heartbeat(any(), any());
    }

    @Test
    @DisplayName("Should ignore message with no metadata")
    void shouldIgnoreMessageWithNoMetadata() {
        WorkflowMessage message = WorkflowMessage.builder()
                .executionId("exec-4")
                .topic(WorkflowRegistrationRequest.REGISTRATION_TOPIC)
                .serviceName("order-service")
                .status(WorkflowStatus.COMPLETED)
                .build();

        handler.handle(message);

        verify(registryService, never()).registerWorkflows(any());
        verify(registryService, never()).heartbeat(any(), any());
        verify(registryService, never()).unregisterService(any(), any());
    }

    @Test
    @DisplayName("Should ignore REGISTER message with no payload")
    void shouldIgnoreRegisterWithNoPayload() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put(WorkflowRegistrationRequest.METADATA_ACTION,
                WorkflowRegistrationRequest.ACTION_REGISTER);
        metadata.put(WorkflowRegistrationRequest.METADATA_INSTANCE_ID, "inst-1");

        WorkflowMessage message = WorkflowMessage.builder()
                .executionId("exec-5")
                .topic(WorkflowRegistrationRequest.REGISTRATION_TOPIC)
                .serviceName("order-service")
                .status(WorkflowStatus.COMPLETED)
                .metadata(metadata)
                .build();

        handler.handle(message);

        verify(registryService, never()).registerWorkflows(any());
    }
}
