package io.github.stepprflow.monitor.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * MongoDB document for registered workflow definitions.
 * Workflows are registered by services using stepprflow-agent at startup.
 */
@Document(collection = "registered_workflows")
@CompoundIndex(name = "topic_serviceName_idx", def = "{'topic': 1, 'serviceName': 1}", unique = true)
@Getter
@Setter
@EqualsAndHashCode
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisteredWorkflow {

    @Id
    private String id;

    /**
     * Workflow topic.
     */
    @Indexed
    private String topic;

    /**
     * Service name that provides this workflow.
     * Together with topic, forms a unique composite key.
     */
    @Indexed
    private String serviceName;

    /**
     * Workflow description.
     */
    private String description;

    /**
     * List of steps in this workflow.
     */
    @Getter(lombok.AccessLevel.NONE)
    @Setter(lombok.AccessLevel.NONE)
    private List<StepInfo> steps;

    /**
     * Number of Kafka partitions.
     */
    private int partitions;

    /**
     * Kafka replication factor.
     */
    private short replication;

    /**
     * Workflow timeout in milliseconds.
     */
    private Long timeoutMs;

    /**
     * Services that provide this workflow.
     */
    @Getter(lombok.AccessLevel.NONE)
    @Setter(lombok.AccessLevel.NONE)
    private Set<ServiceInstance> registeredBy;

    /**
     * Workflow status (ACTIVE when at least one instance is alive, INACTIVE otherwise).
     */
    @Builder.Default
    private Status status = Status.ACTIVE;

    /**
     * First registration time.
     */
    private Instant createdAt;

    /**
     * Last update time.
     */
    @Indexed
    private Instant updatedAt;

    /**
     * Returns a defensive copy of the steps list.
     *
     * @return a new list containing the steps, or empty list if null
     */
    public List<StepInfo> getSteps() {
        return steps == null ? List.of() : new ArrayList<>(steps);
    }

    /**
     * Sets the steps list with a defensive copy.
     *
     * @param steps the steps to set
     */
    public void setSteps(List<StepInfo> steps) {
        this.steps = steps == null ? null : new ArrayList<>(steps);
    }

    /**
     * Returns a defensive copy of the registeredBy set.
     *
     * @return a new set containing the service instances, or empty set if null
     */
    public Set<ServiceInstance> getRegisteredBy() {
        return registeredBy == null ? Set.of() : new HashSet<>(registeredBy);
    }

    /**
     * Sets the registeredBy set with a defensive copy.
     *
     * @param registeredBy the service instances to set
     */
    public void setRegisteredBy(Set<ServiceInstance> registeredBy) {
        this.registeredBy = registeredBy == null ? null : new HashSet<>(registeredBy);
    }

    /**
     * Adds a service instance to the registeredBy set.
     *
     * @param instance the service instance to add
     */
    public void addServiceInstance(ServiceInstance instance) {
        if (this.registeredBy == null) {
            this.registeredBy = new HashSet<>();
        }
        this.registeredBy.add(instance);
    }

    /**
     * Removes service instances matching the predicate.
     *
     * @param filter the predicate to match instances to remove
     * @return true if any instances were removed
     */
    public boolean removeServiceInstancesIf(java.util.function.Predicate<ServiceInstance> filter) {
        if (this.registeredBy == null) {
            return false;
        }
        return this.registeredBy.removeIf(filter);
    }

    /**
     * Checks if registeredBy is empty or null.
     *
     * @return true if there are no registered service instances
     */
    public boolean hasNoServiceInstances() {
        return this.registeredBy == null || this.registeredBy.isEmpty();
    }

    /**
     * Returns the internal registeredBy set for direct iteration.
     * Use with caution - modifications will affect the internal state.
     *
     * @return the internal set, or null if not initialized
     */
    public Set<ServiceInstance> getRegisteredByInternal() {
        return this.registeredBy;
    }

    /**
     * Step information (serializable).
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StepInfo {
        private int id;
        private String label;
        private String description;
        private boolean skippable;
        private boolean continueOnFailure;
        private Long timeoutMs;
    }

    /**
     * Service instance information.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServiceInstance {
        private String serviceName;
        private String instanceId;
        private String host;
        private int port;
        private Instant lastHeartbeat;
    }

    /**
     * Workflow registration status.
     */
    public enum Status {
        /**
         * At least one service instance is actively providing this workflow.
         */
        ACTIVE,

        /**
         * No active service instances are providing this workflow.
         * The workflow definition is preserved for reference.
         */
        INACTIVE
    }
}
