package io.stepprflow.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for registering workflow definitions with the monitoring dashboard.
 *
 * <p>This is a serializable version of WorkflowDefinition
 * without runtime references.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowRegistrationRequest {

    /**
     * Service name (application name).
     */
    private String serviceName;

    /**
     * Service instance ID (for multiple instances).
     */
    private String instanceId;

    /**
     * Service host/address.
     */
    private String host;

    /**
     * Service port.
     */
    private int port;

    /**
     * List of workflow definitions.
     */
    private List<WorkflowInfo> workflows;

    /**
     * Workflow information for registration.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkflowInfo {
        /** Topic name. */
        private String topic;
        /** Workflow description. */
        private String description;
        /** Step definitions. */
        private List<StepInfo> steps;
        /** Number of partitions. */
        private int partitions;
        /** Replication factor. */
        private short replication;
        /** Timeout in milliseconds. */
        private Long timeoutMs;
    }

    /**
     * Step information for registration.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StepInfo {
        /** Step ID. */
        private int id;
        /** Step label. */
        private String label;
        /** Step description. */
        private String description;
        /** Whether step is skippable. */
        private boolean skippable;
        /** Whether to continue on failure. */
        private boolean continueOnFailure;
        /** Timeout in milliseconds. */
        private Long timeoutMs;
    }
}
