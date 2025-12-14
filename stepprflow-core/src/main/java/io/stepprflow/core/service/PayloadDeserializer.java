package io.stepprflow.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.stepprflow.core.model.WorkflowMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Service for deserializing workflow message payloads to their original types.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PayloadDeserializer {

    /** The JSON object mapper. */
    private final ObjectMapper objectMapper;

    /**
     * Deserialize the payload from a workflow message to its original type.
     *
     * @param message the workflow message containing the payload
     * @return the deserialized payload object, or null if payload is null
     * @throws Exception if deserialization fails
     */
    public Object deserialize(final WorkflowMessage message) throws Exception {
        if (message.getPayload() == null) {
            return null;
        }

        String payloadType = message.getPayloadType();
        if (payloadType == null) {
            return message.getPayload();
        }

        try {
            Class<?> payloadClass = Class.forName(payloadType);
            return objectMapper.convertValue(message.getPayload(), payloadClass);
        } catch (ClassNotFoundException e) {
            log.warn("Could not find payload class {}, using raw payload",
                     payloadType);
            return message.getPayload();
        }
    }
}
