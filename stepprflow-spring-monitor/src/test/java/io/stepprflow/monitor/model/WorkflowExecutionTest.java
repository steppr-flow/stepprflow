package io.stepprflow.monitor.model;

import io.stepprflow.core.model.WorkflowStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("WorkflowExecution Tests")
class WorkflowExecutionTest {

    @Nested
    @DisplayName("getStepHistory()")
    class GetStepHistoryTests {

        @Test
        @DisplayName("Should return empty list when stepHistory is null")
        void shouldReturnEmptyListWhenStepHistoryIsNull() {
            WorkflowExecution execution = WorkflowExecution.builder()
                    .executionId("exec-1")
                    .build();

            List<WorkflowExecution.StepExecution> result = execution.getStepHistory();

            assertThat(result).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("Should return defensive copy when stepHistory has elements")
        void shouldReturnDefensiveCopyWhenStepHistoryHasElements() {
            WorkflowExecution.StepExecution step = WorkflowExecution.StepExecution.builder()
                    .stepId(1)
                    .status(WorkflowStatus.IN_PROGRESS)
                    .build();

            ArrayList<WorkflowExecution.StepExecution> history = new ArrayList<>();
            history.add(step);

            WorkflowExecution execution = WorkflowExecution.builder()
                    .executionId("exec-1")
                    .stepHistory(history)
                    .build();

            List<WorkflowExecution.StepExecution> result = execution.getStepHistory();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStepId()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("addStepExecution()")
    class AddStepExecutionTests {

        @Test
        @DisplayName("Should initialize list when null and add step")
        void shouldInitializeListWhenNullAndAddStep() {
            WorkflowExecution execution = WorkflowExecution.builder()
                    .executionId("exec-1")
                    .build();

            WorkflowExecution.StepExecution step = WorkflowExecution.StepExecution.builder()
                    .stepId(1)
                    .status(WorkflowStatus.PENDING)
                    .build();

            execution.addStepExecution(step);

            assertThat(execution.getStepHistory()).hasSize(1);
            assertThat(execution.getStepHistory().get(0).getStepId()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should append to existing list when not null")
        void shouldAppendToExistingListWhenNotNull() {
            WorkflowExecution.StepExecution existingStep = WorkflowExecution.StepExecution.builder()
                    .stepId(1)
                    .build();

            ArrayList<WorkflowExecution.StepExecution> history = new ArrayList<>();
            history.add(existingStep);

            WorkflowExecution execution = WorkflowExecution.builder()
                    .executionId("exec-1")
                    .stepHistory(history)
                    .build();

            WorkflowExecution.StepExecution newStep = WorkflowExecution.StepExecution.builder()
                    .stepId(2)
                    .build();

            execution.addStepExecution(newStep);

            assertThat(execution.getStepHistory()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("findStepByStepId()")
    class FindStepByStepIdTests {

        @Test
        @DisplayName("Should return empty when stepHistory is null")
        void shouldReturnEmptyWhenStepHistoryIsNull() {
            WorkflowExecution execution = WorkflowExecution.builder()
                    .executionId("exec-1")
                    .build();

            Optional<WorkflowExecution.StepExecution> result = execution.findStepByStepId(1);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should find step when present")
        void shouldFindStepWhenPresent() {
            WorkflowExecution.StepExecution step = WorkflowExecution.StepExecution.builder()
                    .stepId(2)
                    .status(WorkflowStatus.COMPLETED)
                    .build();

            ArrayList<WorkflowExecution.StepExecution> history = new ArrayList<>();
            history.add(step);

            WorkflowExecution execution = WorkflowExecution.builder()
                    .executionId("exec-1")
                    .stepHistory(history)
                    .build();

            Optional<WorkflowExecution.StepExecution> result = execution.findStepByStepId(2);

            assertThat(result).isPresent();
            assertThat(result.get().getStepId()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should return empty when step not found")
        void shouldReturnEmptyWhenStepNotFound() {
            WorkflowExecution.StepExecution step = WorkflowExecution.StepExecution.builder()
                    .stepId(1)
                    .build();

            ArrayList<WorkflowExecution.StepExecution> history = new ArrayList<>();
            history.add(step);

            WorkflowExecution execution = WorkflowExecution.builder()
                    .executionId("exec-1")
                    .stepHistory(history)
                    .build();

            Optional<WorkflowExecution.StepExecution> result = execution.findStepByStepId(99);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("markPreviousStepsAsPassed()")
    class MarkPreviousStepsAsPassedTests {

        @Test
        @DisplayName("Should do nothing when stepHistory is null")
        void shouldDoNothingWhenStepHistoryIsNull() {
            WorkflowExecution execution = WorkflowExecution.builder()
                    .executionId("exec-1")
                    .build();

            // Should not throw
            execution.markPreviousStepsAsPassed(2, Instant.now());

            assertThat(execution.getStepHistory()).isEmpty();
        }

        @Test
        @DisplayName("Should mark IN_PROGRESS step as PASSED when advancing")
        void shouldMarkInProgressStepAsPassedWhenAdvancing() {
            Instant startTime = Instant.now().minusSeconds(10);
            WorkflowExecution.StepExecution step1 = WorkflowExecution.StepExecution.builder()
                    .stepId(1)
                    .startedAt(startTime)
                    .status(WorkflowStatus.IN_PROGRESS)
                    .build();

            ArrayList<WorkflowExecution.StepExecution> history = new ArrayList<>();
            history.add(step1);

            WorkflowExecution execution = WorkflowExecution.builder()
                    .executionId("exec-1")
                    .stepHistory(history)
                    .build();

            Instant completionTime = Instant.now();
            execution.markPreviousStepsAsPassed(2, completionTime);

            WorkflowExecution.StepExecution updatedStep = execution.getStepHistory().get(0);
            assertThat(updatedStep.getStatus()).isEqualTo(WorkflowStatus.PASSED);
            assertThat(updatedStep.getCompletedAt()).isEqualTo(completionTime);
            assertThat(updatedStep.getDurationMs()).isNotNull().isGreaterThanOrEqualTo(0L);
        }

        @Test
        @DisplayName("Should mark PENDING step as PASSED when advancing")
        void shouldMarkPendingStepAsPassedWhenAdvancing() {
            WorkflowExecution.StepExecution step1 = WorkflowExecution.StepExecution.builder()
                    .stepId(1)
                    .startedAt(Instant.now().minusSeconds(5))
                    .status(WorkflowStatus.PENDING)
                    .build();

            ArrayList<WorkflowExecution.StepExecution> history = new ArrayList<>();
            history.add(step1);

            WorkflowExecution execution = WorkflowExecution.builder()
                    .executionId("exec-1")
                    .stepHistory(history)
                    .build();

            Instant completionTime = Instant.now();
            execution.markPreviousStepsAsPassed(2, completionTime);

            WorkflowExecution.StepExecution updatedStep = execution.getStepHistory().get(0);
            assertThat(updatedStep.getStatus()).isEqualTo(WorkflowStatus.PASSED);
        }

        @Test
        @DisplayName("Should not mark already COMPLETED step")
        void shouldNotMarkAlreadyCompletedStep() {
            WorkflowExecution.StepExecution step1 = WorkflowExecution.StepExecution.builder()
                    .stepId(1)
                    .status(WorkflowStatus.COMPLETED)
                    .completedAt(Instant.now().minusSeconds(100))
                    .build();

            ArrayList<WorkflowExecution.StepExecution> history = new ArrayList<>();
            history.add(step1);

            WorkflowExecution execution = WorkflowExecution.builder()
                    .executionId("exec-1")
                    .stepHistory(history)
                    .build();

            Instant originalCompletedAt = step1.getCompletedAt();
            execution.markPreviousStepsAsPassed(2, Instant.now());

            WorkflowExecution.StepExecution updatedStep = execution.getStepHistory().get(0);
            assertThat(updatedStep.getStatus()).isEqualTo(WorkflowStatus.COMPLETED);
            assertThat(updatedStep.getCompletedAt()).isEqualTo(originalCompletedAt);
        }

        @Test
        @DisplayName("Should not mark step with same ID as current")
        void shouldNotMarkStepWithSameIdAsCurrent() {
            WorkflowExecution.StepExecution step2 = WorkflowExecution.StepExecution.builder()
                    .stepId(2)
                    .status(WorkflowStatus.IN_PROGRESS)
                    .build();

            ArrayList<WorkflowExecution.StepExecution> history = new ArrayList<>();
            history.add(step2);

            WorkflowExecution execution = WorkflowExecution.builder()
                    .executionId("exec-1")
                    .stepHistory(history)
                    .build();

            execution.markPreviousStepsAsPassed(2, Instant.now());

            // Step 2 should not be marked because currentStepId is 2
            WorkflowExecution.StepExecution updatedStep = execution.getStepHistory().get(0);
            assertThat(updatedStep.getStatus()).isEqualTo(WorkflowStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("Should not mark step with higher ID than current")
        void shouldNotMarkStepWithHigherIdThanCurrent() {
            WorkflowExecution.StepExecution step3 = WorkflowExecution.StepExecution.builder()
                    .stepId(3)
                    .status(WorkflowStatus.IN_PROGRESS)
                    .build();

            ArrayList<WorkflowExecution.StepExecution> history = new ArrayList<>();
            history.add(step3);

            WorkflowExecution execution = WorkflowExecution.builder()
                    .executionId("exec-1")
                    .stepHistory(history)
                    .build();

            execution.markPreviousStepsAsPassed(2, Instant.now());

            // Step 3 should not be marked because 3 > 2
            WorkflowExecution.StepExecution updatedStep = execution.getStepHistory().get(0);
            assertThat(updatedStep.getStatus()).isEqualTo(WorkflowStatus.IN_PROGRESS);
        }
    }

    @Nested
    @DisplayName("getExecutionAttempts()")
    class GetExecutionAttemptsTests {

        @Test
        @DisplayName("Should return empty list when executionAttempts is null")
        void shouldReturnEmptyListWhenExecutionAttemptsIsNull() {
            WorkflowExecution execution = WorkflowExecution.builder()
                    .executionId("exec-1")
                    .build();

            List<WorkflowExecution.ExecutionAttempt> result = execution.getExecutionAttempts();

            assertThat(result).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("Should return defensive copy when executionAttempts has elements")
        void shouldReturnDefensiveCopyWhenExecutionAttemptsHasElements() {
            WorkflowExecution.ExecutionAttempt attempt = WorkflowExecution.ExecutionAttempt.builder()
                    .attemptNumber(1)
                    .startedAt(Instant.now())
                    .build();

            ArrayList<WorkflowExecution.ExecutionAttempt> attempts = new ArrayList<>();
            attempts.add(attempt);

            WorkflowExecution execution = WorkflowExecution.builder()
                    .executionId("exec-1")
                    .executionAttempts(attempts)
                    .build();

            List<WorkflowExecution.ExecutionAttempt> result = execution.getExecutionAttempts();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getAttemptNumber()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("setExecutionAttempts()")
    class SetExecutionAttemptsTests {

        @Test
        @DisplayName("Should set to null when input is null")
        void shouldSetToNullWhenInputIsNull() {
            WorkflowExecution.ExecutionAttempt attempt = WorkflowExecution.ExecutionAttempt.builder()
                    .attemptNumber(1)
                    .build();

            ArrayList<WorkflowExecution.ExecutionAttempt> attempts = new ArrayList<>();
            attempts.add(attempt);

            WorkflowExecution execution = WorkflowExecution.builder()
                    .executionId("exec-1")
                    .executionAttempts(attempts)
                    .build();

            execution.setExecutionAttempts(null);

            // getExecutionAttempts returns empty list when internal is null
            assertThat(execution.getExecutionAttempts()).isEmpty();
        }

        @Test
        @DisplayName("Should set defensive copy when input is not null")
        void shouldSetDefensiveCopyWhenInputIsNotNull() {
            WorkflowExecution execution = WorkflowExecution.builder()
                    .executionId("exec-1")
                    .build();

            WorkflowExecution.ExecutionAttempt attempt = WorkflowExecution.ExecutionAttempt.builder()
                    .attemptNumber(2)
                    .build();

            ArrayList<WorkflowExecution.ExecutionAttempt> attempts = new ArrayList<>();
            attempts.add(attempt);

            execution.setExecutionAttempts(attempts);

            assertThat(execution.getExecutionAttempts()).hasSize(1);
            assertThat(execution.getExecutionAttempts().get(0).getAttemptNumber()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("getNextAttemptNumber()")
    class GetNextAttemptNumberTests {

        @Test
        @DisplayName("Should return 1 when executionAttempts is null")
        void shouldReturn1WhenExecutionAttemptsIsNull() {
            WorkflowExecution execution = WorkflowExecution.builder()
                    .executionId("exec-1")
                    .build();

            int result = execution.getNextAttemptNumber();

            assertThat(result).isEqualTo(1);
        }

        @Test
        @DisplayName("Should return size + 1 when executionAttempts has elements")
        void shouldReturnSizePlus1WhenExecutionAttemptsHasElements() {
            WorkflowExecution.ExecutionAttempt attempt1 = WorkflowExecution.ExecutionAttempt.builder()
                    .attemptNumber(1)
                    .build();
            WorkflowExecution.ExecutionAttempt attempt2 = WorkflowExecution.ExecutionAttempt.builder()
                    .attemptNumber(2)
                    .build();

            ArrayList<WorkflowExecution.ExecutionAttempt> attempts = new ArrayList<>();
            attempts.add(attempt1);
            attempts.add(attempt2);

            WorkflowExecution execution = WorkflowExecution.builder()
                    .executionId("exec-1")
                    .executionAttempts(attempts)
                    .build();

            int result = execution.getNextAttemptNumber();

            assertThat(result).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("addExecutionAttempt()")
    class AddExecutionAttemptTests {

        @Test
        @DisplayName("Should initialize list when null and add attempt")
        void shouldInitializeListWhenNullAndAddAttempt() {
            WorkflowExecution execution = WorkflowExecution.builder()
                    .executionId("exec-1")
                    .build();

            WorkflowExecution.ExecutionAttempt attempt = WorkflowExecution.ExecutionAttempt.builder()
                    .attemptNumber(1)
                    .startedAt(Instant.now())
                    .build();

            execution.addExecutionAttempt(attempt);

            assertThat(execution.getExecutionAttempts()).hasSize(1);
            assertThat(execution.getExecutionAttempts().get(0).getAttemptNumber()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should append to existing list when not null")
        void shouldAppendToExistingListWhenNotNull() {
            WorkflowExecution.ExecutionAttempt existing = WorkflowExecution.ExecutionAttempt.builder()
                    .attemptNumber(1)
                    .build();

            ArrayList<WorkflowExecution.ExecutionAttempt> attempts = new ArrayList<>();
            attempts.add(existing);

            WorkflowExecution execution = WorkflowExecution.builder()
                    .executionId("exec-1")
                    .executionAttempts(attempts)
                    .build();

            WorkflowExecution.ExecutionAttempt newAttempt = WorkflowExecution.ExecutionAttempt.builder()
                    .attemptNumber(2)
                    .build();

            execution.addExecutionAttempt(newAttempt);

            assertThat(execution.getExecutionAttempts()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("getCurrentAttempt()")
    class GetCurrentAttemptTests {

        @Test
        @DisplayName("Should return empty when executionAttempts is null")
        void shouldReturnEmptyWhenExecutionAttemptsIsNull() {
            WorkflowExecution execution = WorkflowExecution.builder()
                    .executionId("exec-1")
                    .build();

            Optional<WorkflowExecution.ExecutionAttempt> result = execution.getCurrentAttempt();

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should return empty when executionAttempts is empty")
        void shouldReturnEmptyWhenExecutionAttemptsIsEmpty() {
            WorkflowExecution execution = WorkflowExecution.builder()
                    .executionId("exec-1")
                    .executionAttempts(new ArrayList<>())
                    .build();

            Optional<WorkflowExecution.ExecutionAttempt> result = execution.getCurrentAttempt();

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should return last attempt when list has elements")
        void shouldReturnLastAttemptWhenListHasElements() {
            WorkflowExecution.ExecutionAttempt attempt1 = WorkflowExecution.ExecutionAttempt.builder()
                    .attemptNumber(1)
                    .build();
            WorkflowExecution.ExecutionAttempt attempt2 = WorkflowExecution.ExecutionAttempt.builder()
                    .attemptNumber(2)
                    .build();

            ArrayList<WorkflowExecution.ExecutionAttempt> attempts = new ArrayList<>();
            attempts.add(attempt1);
            attempts.add(attempt2);

            WorkflowExecution execution = WorkflowExecution.builder()
                    .executionId("exec-1")
                    .executionAttempts(attempts)
                    .build();

            Optional<WorkflowExecution.ExecutionAttempt> result = execution.getCurrentAttempt();

            assertThat(result).isPresent();
            assertThat(result.get().getAttemptNumber()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("StepExecution.markAsPassed()")
    class StepExecutionMarkAsPassedTests {

        @Test
        @DisplayName("Should calculate durationMs when startedAt is set")
        void shouldCalculateDurationMsWhenStartedAtIsSet() {
            Instant startTime = Instant.now().minusSeconds(5);
            WorkflowExecution.StepExecution step = WorkflowExecution.StepExecution.builder()
                    .stepId(1)
                    .startedAt(startTime)
                    .status(WorkflowStatus.IN_PROGRESS)
                    .build();

            Instant completionTime = Instant.now();
            step.markAsPassed(completionTime);

            assertThat(step.getStatus()).isEqualTo(WorkflowStatus.PASSED);
            assertThat(step.getCompletedAt()).isEqualTo(completionTime);
            assertThat(step.getDurationMs()).isNotNull();
            // Duration should be positive (completedAt - startedAt)
            long expectedDuration = completionTime.toEpochMilli() - startTime.toEpochMilli();
            assertThat(step.getDurationMs()).isEqualTo(expectedDuration);
        }

        @Test
        @DisplayName("Should not calculate durationMs when startedAt is null")
        void shouldNotCalculateDurationMsWhenStartedAtIsNull() {
            WorkflowExecution.StepExecution step = WorkflowExecution.StepExecution.builder()
                    .stepId(1)
                    .status(WorkflowStatus.IN_PROGRESS)
                    .build();

            Instant completionTime = Instant.now();
            step.markAsPassed(completionTime);

            assertThat(step.getStatus()).isEqualTo(WorkflowStatus.PASSED);
            assertThat(step.getCompletedAt()).isEqualTo(completionTime);
            assertThat(step.getDurationMs()).isNull();
        }
    }

    @Nested
    @DisplayName("ExecutionAttempt.getPayloadChanges()")
    class ExecutionAttemptGetPayloadChangesTests {

        @Test
        @DisplayName("Should return empty list when payloadChanges is null")
        void shouldReturnEmptyListWhenPayloadChangesIsNull() {
            WorkflowExecution.ExecutionAttempt attempt = WorkflowExecution.ExecutionAttempt.builder()
                    .attemptNumber(1)
                    .build();

            List<WorkflowExecution.PayloadChange> result = attempt.getPayloadChanges();

            assertThat(result).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("Should return defensive copy when payloadChanges has elements")
        void shouldReturnDefensiveCopyWhenPayloadChangesHasElements() {
            WorkflowExecution.PayloadChange change = WorkflowExecution.PayloadChange.builder()
                    .fieldPath("field1")
                    .oldValue("old")
                    .newValue("new")
                    .build();

            ArrayList<WorkflowExecution.PayloadChange> changes = new ArrayList<>();
            changes.add(change);

            WorkflowExecution.ExecutionAttempt attempt = WorkflowExecution.ExecutionAttempt.builder()
                    .attemptNumber(1)
                    .payloadChanges(changes)
                    .build();

            List<WorkflowExecution.PayloadChange> result = attempt.getPayloadChanges();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getFieldPath()).isEqualTo("field1");
        }
    }
}