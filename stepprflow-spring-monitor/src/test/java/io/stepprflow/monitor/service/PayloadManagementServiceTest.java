package io.stepprflow.monitor.service;

import io.stepprflow.core.model.WorkflowStatus;
import io.stepprflow.core.util.NestedPathResolver;
import io.stepprflow.monitor.model.WorkflowExecution;
import io.stepprflow.monitor.repository.WorkflowExecutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.stepprflow.monitor.exception.ConcurrentModificationException;
import org.springframework.dao.OptimisticLockingFailureException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PayloadManagementService.
 * This service handles payload field updates and restoration.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PayloadManagementService Tests")
class PayloadManagementServiceTest {

    @Mock
    private WorkflowExecutionRepository repository;

    @Mock
    private NestedPathResolver pathResolver;

    @InjectMocks
    private PayloadManagementService payloadService;

    private WorkflowExecution testExecution;

    @BeforeEach
    void setUp() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderId", "ORD-001");
        payload.put("quantity", 5);
        payload.put("customer", new LinkedHashMap<>(Map.of("name", "John", "email", "john@example.com")));

        testExecution = WorkflowExecution.builder()
                .executionId("exec-123")
                .correlationId("corr-456")
                .topic("test-topic")
                .status(WorkflowStatus.FAILED)
                .currentStep(2)
                .totalSteps(5)
                .payload(payload)
                .payloadType("java.util.Map")
                .createdAt(Instant.now().minusSeconds(3600))
                .build();
    }

    @Nested
    @DisplayName("updatePayloadField() method")
    class UpdatePayloadFieldTests {

        @Test
        @DisplayName("Should throw exception when execution not found")
        void shouldThrowExceptionWhenNotFound() {
            when(repository.findById("unknown")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> payloadService.updatePayloadField("unknown", "orderId", "NEW-001", "user", "Fix typo"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Execution not found");
        }

        @Test
        @DisplayName("Should throw exception when status is not editable")
        void shouldThrowExceptionWhenStatusNotEditable() {
            testExecution.setStatus(WorkflowStatus.COMPLETED);
            when(repository.findById("exec-123")).thenReturn(Optional.of(testExecution));

            assertThatThrownBy(() -> payloadService.updatePayloadField("exec-123", "orderId", "NEW-001", "user", "Fix"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot update payload for execution with status");
        }

        @Test
        @DisplayName("Should throw exception when status is IN_PROGRESS")
        void shouldThrowExceptionWhenInProgress() {
            testExecution.setStatus(WorkflowStatus.IN_PROGRESS);
            when(repository.findById("exec-123")).thenReturn(Optional.of(testExecution));

            assertThatThrownBy(() -> payloadService.updatePayloadField("exec-123", "orderId", "NEW-001", "user", "Fix"))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("Should update simple field in payload")
        void shouldUpdateSimpleField() {
            testExecution.setStatus(WorkflowStatus.FAILED);
            when(repository.findById("exec-123")).thenReturn(Optional.of(testExecution));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(pathResolver.getValue(any(), eq("orderId"))).thenReturn("ORD-001");
            doAnswer(inv -> {
                Map<String, Object> map = inv.getArgument(0);
                map.put("orderId", inv.getArgument(2));
                return null;
            }).when(pathResolver).setValue(any(), eq("orderId"), eq("NEW-001"));

            WorkflowExecution result = payloadService.updatePayloadField("exec-123", "orderId", "NEW-001", "UI User", "Fix order ID");

            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) result.getPayload();
            assertThat(payload.get("orderId")).isEqualTo("NEW-001");
        }

        @Test
        @DisplayName("Should update nested field in payload")
        void shouldUpdateNestedField() {
            testExecution.setStatus(WorkflowStatus.FAILED);
            when(repository.findById("exec-123")).thenReturn(Optional.of(testExecution));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(pathResolver.getValue(any(), eq("customer.email"))).thenReturn("john@example.com");
            doAnswer(inv -> {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = inv.getArgument(0);
                @SuppressWarnings("unchecked")
                Map<String, Object> customer = (Map<String, Object>) map.get("customer");
                customer.put("email", inv.getArgument(2));
                return null;
            }).when(pathResolver).setValue(any(), eq("customer.email"), eq("new@example.com"));

            WorkflowExecution result = payloadService.updatePayloadField("exec-123", "customer.email", "new@example.com", "UI User", "Fix email");

            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) result.getPayload();
            @SuppressWarnings("unchecked")
            Map<String, Object> customer = (Map<String, Object>) payload.get("customer");
            assertThat(customer.get("email")).isEqualTo("new@example.com");
        }

        @Test
        @DisplayName("Should track change in payload history")
        void shouldTrackChangeInHistory() {
            testExecution.setStatus(WorkflowStatus.FAILED);
            when(repository.findById("exec-123")).thenReturn(Optional.of(testExecution));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(pathResolver.getValue(any(), eq("quantity"))).thenReturn(5);

            WorkflowExecution result = payloadService.updatePayloadField("exec-123", "quantity", 10, "UI User", "Increase quantity");

            assertThat(result.getPayloadHistory()).hasSize(1);
            WorkflowExecution.PayloadChange change = result.getPayloadHistory().get(0);
            assertThat(change.getFieldPath()).isEqualTo("quantity");
            assertThat(change.getOldValue()).isEqualTo(5);
            assertThat(change.getNewValue()).isEqualTo(10);
            assertThat(change.getChangedBy()).isEqualTo("UI User");
            assertThat(change.getReason()).isEqualTo("Increase quantity");
            assertThat(change.getChangedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should allow update for PAUSED status")
        void shouldAllowUpdateForPausedStatus() {
            testExecution.setStatus(WorkflowStatus.PAUSED);
            when(repository.findById("exec-123")).thenReturn(Optional.of(testExecution));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(pathResolver.getValue(any(), eq("orderId"))).thenReturn("ORD-001");

            WorkflowExecution result = payloadService.updatePayloadField("exec-123", "orderId", "NEW-001", "user", "Fix");

            assertThat(result).isNotNull();
            verify(repository).save(any());
        }

        @Test
        @DisplayName("Should allow update for RETRY_PENDING status")
        void shouldAllowUpdateForRetryPendingStatus() {
            testExecution.setStatus(WorkflowStatus.RETRY_PENDING);
            when(repository.findById("exec-123")).thenReturn(Optional.of(testExecution));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(pathResolver.getValue(any(), eq("orderId"))).thenReturn("ORD-001");

            WorkflowExecution result = payloadService.updatePayloadField("exec-123", "orderId", "NEW-001", "user", "Fix");

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Should preserve key order when updating payload")
        void shouldPreserveKeyOrder() {
            testExecution.setStatus(WorkflowStatus.FAILED);
            when(repository.findById("exec-123")).thenReturn(Optional.of(testExecution));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(pathResolver.getValue(any(), eq("quantity"))).thenReturn(5);

            WorkflowExecution result = payloadService.updatePayloadField("exec-123", "quantity", 20, "user", "Fix");

            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) result.getPayload();
            List<String> keys = new ArrayList<>(payload.keySet());
            assertThat(keys).containsExactly("orderId", "quantity", "customer");
        }

        @Test
        @DisplayName("Should accumulate multiple changes in history")
        void shouldAccumulateChangesInHistory() {
            testExecution.setStatus(WorkflowStatus.FAILED);
            testExecution.setPayloadHistory(new ArrayList<>());
            when(repository.findById("exec-123")).thenReturn(Optional.of(testExecution));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(pathResolver.getValue(any(), eq("orderId"))).thenReturn("ORD-001");
            when(pathResolver.getValue(any(), eq("quantity"))).thenReturn(5);

            payloadService.updatePayloadField("exec-123", "orderId", "NEW-001", "user1", "First fix");
            payloadService.updatePayloadField("exec-123", "quantity", 10, "user2", "Second fix");

            assertThat(testExecution.getPayloadHistory()).hasSize(2);
        }

        @Test
        @DisplayName("Should throw exception when payload is not a Map")
        void shouldThrowExceptionWhenPayloadNotMap() {
            testExecution.setStatus(WorkflowStatus.FAILED);
            testExecution.setPayload("not a map");
            when(repository.findById("exec-123")).thenReturn(Optional.of(testExecution));

            assertThatThrownBy(() -> payloadService.updatePayloadField("exec-123", "field", "value", "user", "reason"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Payload must be a Map");
        }

        @Test
        @DisplayName("Should throw ConcurrentModificationException on optimistic locking failure")
        void shouldThrowConcurrentModificationExceptionOnLockingFailure() {
            testExecution.setStatus(WorkflowStatus.FAILED);
            when(repository.findById("exec-123")).thenReturn(Optional.of(testExecution));
            when(pathResolver.getValue(any(), eq("orderId"))).thenReturn("ORD-001");
            when(repository.save(any())).thenThrow(new OptimisticLockingFailureException("Version conflict"));

            assertThatThrownBy(() -> payloadService.updatePayloadField("exec-123", "orderId", "NEW-001", "user", "Fix"))
                    .isInstanceOf(ConcurrentModificationException.class)
                    .hasMessageContaining("exec-123")
                    .hasMessageContaining("modified by another process");
        }
    }

    @Nested
    @DisplayName("restorePayload() method")
    class RestorePayloadTests {

        @BeforeEach
        void setUpWithChanges() {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("orderId", "MODIFIED-001");
            payload.put("quantity", 20);
            testExecution.setPayload(payload);

            List<WorkflowExecution.PayloadChange> history = new ArrayList<>();
            history.add(WorkflowExecution.PayloadChange.builder()
                    .fieldPath("orderId")
                    .oldValue("ORD-001")
                    .newValue("MODIFIED-001")
                    .changedAt(Instant.now())
                    .changedBy("user1")
                    .reason("Change 1")
                    .build());
            history.add(WorkflowExecution.PayloadChange.builder()
                    .fieldPath("quantity")
                    .oldValue(5)
                    .newValue(20)
                    .changedAt(Instant.now())
                    .changedBy("user2")
                    .reason("Change 2")
                    .build());
            testExecution.setPayloadHistory(history);
        }

        @Test
        @DisplayName("Should throw exception when execution not found")
        void shouldThrowExceptionWhenNotFound() {
            when(repository.findById("unknown")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> payloadService.restorePayload("unknown"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Execution not found");
        }

        @Test
        @DisplayName("Should throw exception when status is not editable")
        void shouldThrowExceptionWhenStatusNotEditable() {
            testExecution.setStatus(WorkflowStatus.COMPLETED);
            when(repository.findById("exec-123")).thenReturn(Optional.of(testExecution));

            assertThatThrownBy(() -> payloadService.restorePayload("exec-123"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot restore payload for execution with status");
        }

        @Test
        @DisplayName("Should revert all changes in reverse order")
        void shouldRevertAllChangesInReverseOrder() {
            testExecution.setStatus(WorkflowStatus.FAILED);
            when(repository.findById("exec-123")).thenReturn(Optional.of(testExecution));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            doAnswer(inv -> {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = inv.getArgument(0);
                String fieldPath = inv.getArgument(1);
                Object value = inv.getArgument(2);
                map.put(fieldPath, value);
                return null;
            }).when(pathResolver).setValue(any(), anyString(), any());

            WorkflowExecution result = payloadService.restorePayload("exec-123");

            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) result.getPayload();
            assertThat(payload.get("orderId")).isEqualTo("ORD-001");
            assertThat(payload.get("quantity")).isEqualTo(5);
        }

        @Test
        @DisplayName("Should clear payload history after restore")
        void shouldClearPayloadHistoryAfterRestore() {
            testExecution.setStatus(WorkflowStatus.FAILED);
            when(repository.findById("exec-123")).thenReturn(Optional.of(testExecution));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            WorkflowExecution result = payloadService.restorePayload("exec-123");

            assertThat(result.getPayloadHistory()).isEmpty();
        }

        @Test
        @DisplayName("Should return unchanged execution when no pending changes")
        void shouldReturnUnchangedWhenNoPendingChanges() {
            testExecution.setStatus(WorkflowStatus.FAILED);
            testExecution.setPayloadHistory(new ArrayList<>());
            when(repository.findById("exec-123")).thenReturn(Optional.of(testExecution));

            WorkflowExecution result = payloadService.restorePayload("exec-123");

            assertThat(result).isEqualTo(testExecution);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Should return unchanged execution when payload history is null")
        void shouldReturnUnchangedWhenHistoryIsNull() {
            testExecution.setStatus(WorkflowStatus.FAILED);
            testExecution.setPayloadHistory(null);
            when(repository.findById("exec-123")).thenReturn(Optional.of(testExecution));

            WorkflowExecution result = payloadService.restorePayload("exec-123");

            assertThat(result).isEqualTo(testExecution);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Should update updatedAt timestamp")
        void shouldUpdateTimestamp() {
            testExecution.setStatus(WorkflowStatus.FAILED);
            Instant before = Instant.now();
            when(repository.findById("exec-123")).thenReturn(Optional.of(testExecution));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            WorkflowExecution result = payloadService.restorePayload("exec-123");

            assertThat(result.getUpdatedAt()).isAfterOrEqualTo(before);
        }

        @Test
        @DisplayName("Should throw ConcurrentModificationException on optimistic locking failure")
        void shouldThrowConcurrentModificationExceptionOnLockingFailure() {
            testExecution.setStatus(WorkflowStatus.FAILED);
            when(repository.findById("exec-123")).thenReturn(Optional.of(testExecution));
            when(repository.save(any())).thenThrow(new OptimisticLockingFailureException("Version conflict"));

            assertThatThrownBy(() -> payloadService.restorePayload("exec-123"))
                    .isInstanceOf(ConcurrentModificationException.class)
                    .hasMessageContaining("exec-123");
        }
    }
}
