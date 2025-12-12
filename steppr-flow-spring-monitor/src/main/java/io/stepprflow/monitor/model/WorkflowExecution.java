package io.stepprflow.monitor.model;

import io.stepprflow.core.model.ErrorInfo;
import io.stepprflow.core.model.RetryInfo;
import io.stepprflow.core.model.WorkflowStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MongoDB document for workflow execution.
 */
@Document(collection = "workflow_executions")
@CompoundIndex(name = "topic_status", def = "{'topic': 1, 'status': 1}")
@CompoundIndex(name = "status_createdAt", def = "{'status': 1, 'createdAt': -1}")
@CompoundIndex(name = "status_completedAt", def = "{'status': 1, 'completedAt': 1}")
@CompoundIndex(name = "status_nextRetry", def = "{'status': 1, 'retryInfo.nextRetryAt': 1}")
@Getter
@Setter
@EqualsAndHashCode
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowExecution {

    @Id
    private String executionId;

    /**
     * Version field for optimistic locking.
     * Automatically managed by Spring Data MongoDB.
     */
    @Version
    private Long version;

    private String correlationId;

    @Indexed
    private String topic;

    @Indexed
    private WorkflowStatus status;

    private int currentStep;

    private int totalSteps;

    private Object payload;

    private String payloadType;

    private String securityContext;

    @Getter(lombok.AccessLevel.NONE)
    @Setter(lombok.AccessLevel.NONE)
    private Map<String, Object> metadata;

    private RetryInfo retryInfo;

    private ErrorInfo errorInfo;

    /**
     * History of step executions.
     */
    @Getter(lombok.AccessLevel.NONE)
    @Setter(lombok.AccessLevel.NONE)
    private List<StepExecution> stepHistory;

    /**
     * History of payload changes (pending changes before next resume).
     */
    @Getter(lombok.AccessLevel.NONE)
    @Setter(lombok.AccessLevel.NONE)
    private List<PayloadChange> payloadHistory;

    /**
     * History of execution attempts.
     */
    @Getter(lombok.AccessLevel.NONE)
    @Setter(lombok.AccessLevel.NONE)
    private List<ExecutionAttempt> executionAttempts;

    @Indexed
    private Instant createdAt;

    private Instant updatedAt;

    private Instant completedAt;

    /**
     * User who started the workflow.
     */
    private String initiatedBy;

    /**
     * Duration in milliseconds.
     */
    private Long durationMs;

    // Defensive copy getters and setters for mutable objects

    /**
     * Returns a defensive copy of metadata, or null if not set.
     */
    public Map<String, Object> getMetadata() {
        return metadata == null ? null : new HashMap<>(metadata);
    }

    /**
     * Sets metadata with a defensive copy.
     */
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata == null ? null : new HashMap<>(metadata);
    }


    /**
     * Returns a defensive copy of step history.
     */
    public List<StepExecution> getStepHistory() {
        return stepHistory == null ? List.of() : new ArrayList<>(stepHistory);
    }

    /**
     * Sets step history with a defensive copy.
     */
    public void setStepHistory(List<StepExecution> stepHistory) {
        this.stepHistory = stepHistory == null ? null : new ArrayList<>(stepHistory);
    }

    /**
     * Returns a mutable list for modifying step history.
     * Creates the list if null.
     */
    public List<StepExecution> getStepHistoryMutable() {
        if (this.stepHistory == null) {
            this.stepHistory = new ArrayList<>();
        }
        return this.stepHistory;
    }

    /**
     * Returns a defensive copy of payload history.
     */
    public List<PayloadChange> getPayloadHistory() {
        return payloadHistory == null ? List.of() : new ArrayList<>(payloadHistory);
    }

    /**
     * Sets payload history with a defensive copy.
     */
    public void setPayloadHistory(List<PayloadChange> payloadHistory) {
        this.payloadHistory = payloadHistory == null ? null : new ArrayList<>(payloadHistory);
    }

    /**
     * Adds a payload change to the history.
     * Initializes the list if null.
     *
     * @param change the payload change to add
     */
    public void addPayloadChange(PayloadChange change) {
        if (this.payloadHistory == null) {
            this.payloadHistory = new ArrayList<>();
        }
        this.payloadHistory.add(change);
    }

    /**
     * Returns a defensive copy of execution attempts.
     */
    public List<ExecutionAttempt> getExecutionAttempts() {
        return executionAttempts == null ? List.of() : new ArrayList<>(executionAttempts);
    }

    /**
     * Sets execution attempts with a defensive copy.
     */
    public void setExecutionAttempts(List<ExecutionAttempt> executionAttempts) {
        this.executionAttempts = executionAttempts == null ? null : new ArrayList<>(executionAttempts);
    }

    /**
     * Returns a mutable list for modifying execution attempts.
     * Creates the list if null.
     */
    public List<ExecutionAttempt> getExecutionAttemptsMutable() {
        if (this.executionAttempts == null) {
            this.executionAttempts = new ArrayList<>();
        }
        return this.executionAttempts;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StepExecution {
        private int stepId;
        private String stepLabel;
        private WorkflowStatus status;
        private Instant startedAt;
        private Instant completedAt;
        private Long durationMs;
        private String errorMessage;
        private int attempt;
    }

    /**
     * Record of a payload field change.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PayloadChange {
        private String fieldPath;
        private Object oldValue;
        private Object newValue;
        private Instant changedAt;
        private String changedBy;
        private String reason;
    }

    /**
     * Record of an execution attempt.
     */
    @Getter
    @Setter
    @EqualsAndHashCode
    @ToString
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExecutionAttempt {
        private int attemptNumber;
        private Instant startedAt;
        private Instant endedAt;
        private WorkflowStatus result;
        private int startStep;
        private int endStep;
        private String errorMessage;
        private String resumedBy;
        /**
         * Payload changes applied before this attempt.
         */
        @Getter(lombok.AccessLevel.NONE)
        @Setter(lombok.AccessLevel.NONE)
        private List<PayloadChange> payloadChanges;

        /**
         * Returns a defensive copy of payload changes.
         */
        public List<PayloadChange> getPayloadChanges() {
            return payloadChanges == null ? List.of() : new ArrayList<>(payloadChanges);
        }

        /**
         * Sets payload changes with a defensive copy.
         */
        public void setPayloadChanges(List<PayloadChange> payloadChanges) {
            this.payloadChanges = payloadChanges == null ? null : new ArrayList<>(payloadChanges);
        }
    }
}
