package io.github.stepprflow.monitor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.stepprflow.core.model.WorkflowMessage;
import io.github.stepprflow.core.model.WorkflowRegistrationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Handles workflow registration messages received via the broker.
 *
 * <p>Extracts the registration action from the message metadata and
 * delegates to {@link WorkflowRegistryService} for REGISTER, HEARTBEAT,
 * and DEREGISTER actions.
 */
@Component
@Slf4j
public class RegistrationMessageHandler {

    private final WorkflowRegistryService registryService;
    private final ObjectMapper objectMapper;

    /**
     * Creates a new RegistrationMessageHandler.
     *
     * @param registryService the workflow registry service
     * @param objectMapper    the object mapper for payload conversion
     */
    public RegistrationMessageHandler(
            final WorkflowRegistryService registryService,
            @Qualifier("stepprflowObjectMapper") final ObjectMapper objectMapper) {
        this.registryService = registryService;
        this.objectMapper = objectMapper;
    }

    /**
     * Handle a registration message from the broker.
     *
     * @param message the workflow message with registration metadata
     */
    public void handle(final WorkflowMessage message) {
        Map<String, Object> metadata = message.getMetadata();
        if (metadata == null) {
            log.warn("Registration message has no metadata, ignoring");
            return;
        }

        String action = String.valueOf(metadata.get(WorkflowRegistrationRequest.METADATA_ACTION));
        String serviceName = message.getServiceName();
        String instanceId = String.valueOf(
                metadata.get(WorkflowRegistrationRequest.METADATA_INSTANCE_ID));

        log.debug("Registration message: action={}, service={}, instance={}",
                action, serviceName, instanceId);

        switch (action) {
            case WorkflowRegistrationRequest.ACTION_REGISTER:
                handleRegister(message);
                break;
            case WorkflowRegistrationRequest.ACTION_HEARTBEAT:
                registryService.heartbeat(serviceName, instanceId);
                break;
            case WorkflowRegistrationRequest.ACTION_DEREGISTER:
                registryService.unregisterService(serviceName, instanceId);
                break;
            default:
                log.warn("Unknown registration action: {}", action);
        }
    }

    private void handleRegister(final WorkflowMessage message) {
        Object payload = message.getPayload();
        if (payload == null) {
            log.warn("REGISTER message has no payload, ignoring");
            return;
        }

        WorkflowRegistrationRequest request = objectMapper.convertValue(
                payload, WorkflowRegistrationRequest.class);
        registryService.registerWorkflows(request);
    }
}
