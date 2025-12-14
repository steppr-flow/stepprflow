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
import java.util.Optional;
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
     * Adds a step execution to the history.
     * Initializes the list if null.
     *
     * @param step the step execution to add
     */
    public void addStepExecution(StepExecution step) {
        if (this.stepHistory == null) {
            this.stepHistory = new ArrayList<>();
        }
        this.stepHistory.add(step);
    }

    /**
     * Finds a step execution by step ID.
     *
     * @param stepId the step ID to find
     * @return an Optional containing the step execution if found
     */
    public Optional<StepExecution> findStepByStepId(int stepId) {
        if (this.stepHistory == null) {
            return Optional.empty();
        }
        return this.stepHistory.stream()
                .filter(s -> s.getStepId() == stepId)
                .findFirst();
    }

    /**
     * Marks all previous steps (before currentStepId) as PASSED if they are still IN_PROGRESS or PENDING.
     *
     * @param currentStepId the current step ID
     * @param completedAt the completion timestamp
     */
    public void markPreviousStepsAsPassed(int currentStepId, Instant completedAt) {
        if (this.stepHistory == null) {
            return;
        }
        for (StepExecution prevStep : this.stepHistory) {
            if (prevStep.getStepId() < currentStepId
                    && (prevStep.getStatus() == WorkflowStatus.IN_PROGRESS
                        || prevStep.getStatus() == WorkflowStatus.PENDING)) {
                prevStep.markAsPassed(completedAt);
            }
        }
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
     * Returns the next attempt number (current size + 1).
     *
     * @return the next attempt number
     */
    public int getNextAttemptNumber() {
        return (this.executionAttempts == null ? 0 : this.executionAttempts.size()) + 1;
    }

    /**
     * Adds an execution attempt to the list.
     * Initializes the list if null.
     *
     * @param attempt the execution attempt to add
     */
    public void addExecutionAttempt(ExecutionAttempt attempt) {
        if (this.executionAttempts == null) {
            this.executionAttempts = new ArrayList<>();
        }
        this.executionAttempts.add(attempt);
    }

    /**
     * Returns the current (last) execution attempt if any.
     *
     * @return an Optional containing the current attempt
     */
    public Optional<ExecutionAttempt> getCurrentAttempt() {
        if (this.executionAttempts == null || this.executionAttempts.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(this.executionAttempts.get(this.executionAttempts.size() - 1));
    }

    /**
     * Returns true if there are any execution attempts.
     *
     * @return true if attempts exist
     */
    public boolean hasAttempts() {
        return this.executionAttempts != null && !this.executionAttempts.isEmpty();
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

        /**
         * Marks this step as PASSED with the given completion time.
         *
         * @param completedAt the completion timestamp
         */
        public void markAsPassed(Instant completedAt) {
            this.status = WorkflowStatus.PASSED;
            this.completedAt = completedAt;
            if (this.startedAt != null) {
                this.durationMs = completedAt.toEpochMilli() - this.startedAt.toEpochMilli();
            }
        }
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
