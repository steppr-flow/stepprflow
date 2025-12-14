package io.stepprflow.monitor.util;

import io.stepprflow.core.model.RetryInfo;
import io.stepprflow.core.model.WorkflowMessage;
import io.stepprflow.core.model.WorkflowStatus;
import io.stepprflow.monitor.model.WorkflowExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("WorkflowMessageFactory Tests")
class WorkflowMessageFactoryTest {

    private WorkflowMessageFactory factory;
    private WorkflowExecution execution;

    @BeforeEach
    void setUp() {
        factory = new WorkflowMessageFactory();
        execution = WorkflowExecution.builder()
                .executionId("exec-123")
                .correlationId("corr-456")
                .topic("test-topic")
                .currentStep(2)
                .totalSteps(5)
                .status(WorkflowStatus.FAILED)
                .payload(Map.of("orderId", "ORD-001"))
                .payloadType("java.util.Map")
                .securityContext("auth-token")
                .metadata(Map.of("key", "value"))
                .createdAt(Instant.now().minusSeconds(3600))
                .build();
    }

    @Nested
    @DisplayName("createResumeMessage()")
    class CreateResumeMessageTests {

        @Test
        @DisplayName("Should create message with all execution data")
        void shouldCreateMessageWithAllExecutionData() {
            WorkflowMessage message = factory.createResumeMessage(execution, 2);

            assertThat(message.getExecutionId()).isEqualTo("exec-123");
            assertThat(message.getCorrelationId()).isEqualTo("corr-456");
            assertThat(message.getTopic()).isEqualTo("test-topic");
            assertThat(message.getTotalSteps()).isEqualTo(5);
            assertThat(message.getPayload()).isEqualTo(Map.of("orderId", "ORD-001"));
            assertThat(message.getPayloadType()).isEqualTo("java.util.Map");
            assertThat(message.getSecurityContext()).isEqualTo("auth-token");
            assertThat(message.getMetadata()).isEqualTo(Map.of("key", "value"));
            assertThat(message.getCreatedAt()).isEqualTo(execution.getCreatedAt());
        }

        @Test
        @DisplayName("Should set status to IN_PROGRESS")
        void shouldSetStatusToInProgress() {
            WorkflowMessage message = factory.createResumeMessage(execution, 2);

            assertThat(message.getStatus()).isEqualTo(WorkflowStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("Should use provided step number")
        void shouldUseProvidedStepNumber() {
            WorkflowMessage message = factory.createResumeMessage(execution, 3);

            assertThat(message.getCurrentStep()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should set updatedAt to current time")
        void shouldSetUpdatedAtToCurrentTime() {
            Instant before = Instant.now();
            WorkflowMessage message = factory.createResumeMessage(execution, 2);
            Instant after = Instant.now();

            assertThat(message.getUpdatedAt()).isAfterOrEqualTo(before);
            assertThat(message.getUpdatedAt()).isBeforeOrEqualTo(after);
        }

        @Test
        @DisplayName("Should throw exception for null execution")
        void shouldThrowExceptionForNullExecution() {
            assertThatThrownBy(() -> factory.createResumeMessage(null, 1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Execution cannot be null");
        }
    }

    @Nested
    @DisplayName("createRetryMessage()")
    class CreateRetryMessageTests {

        @BeforeEach
        void setUp() {
            execution.setRetryInfo(RetryInfo.builder()
                    .maxAttempts(3)
                    .attempt(1)
                    .build());
        }

        @Test
        @DisplayName("Should create message with retry info")
        void shouldCreateMessageWithRetryInfo() {
            WorkflowMessage message = factory.createRetryMessage(execution);

            assertThat(message.getRetryInfo()).isNotNull();
            assertThat(message.getRetryInfo().getMaxAttempts()).isEqualTo(3);
            assertThat(message.getRetryInfo().getAttempt()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should use current step from execution")
        void shouldUseCurrentStepFromExecution() {
            WorkflowMessage message = factory.createRetryMessage(execution);

            assertThat(message.getCurrentStep()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should preserve all execution data")
        void shouldPreserveAllExecutionData() {
            WorkflowMessage message = factory.createRetryMessage(execution);

            assertThat(message.getExecutionId()).isEqualTo("exec-123");
            assertThat(message.getCorrelationId()).isEqualTo("corr-456");
            assertThat(message.getTopic()).isEqualTo("test-topic");
            assertThat(message.getTotalSteps()).isEqualTo(5);
            assertThat(message.getPayload()).isEqualTo(Map.of("orderId", "ORD-001"));
            assertThat(message.getPayloadType()).isEqualTo("java.util.Map");
            assertThat(message.getSecurityContext()).isEqualTo("auth-token");
            assertThat(message.getMetadata()).isEqualTo(Map.of("key", "value"));
        }

        @Test
        @DisplayName("Should set status to IN_PROGRESS")
        void shouldSetStatusToInProgress() {
            WorkflowMessage message = factory.createRetryMessage(execution);

            assertThat(message.getStatus()).isEqualTo(WorkflowStatus.IN_PROGRESS);
        }
    }

    @Nested
    @DisplayName("Handle null fields gracefully")
    class NullFieldHandlingTests {

        @Test
        @DisplayName("Should handle null security context")
        void shouldHandleNullSecurityContext() {
            execution.setSecurityContext(null);

            WorkflowMessage message = factory.createResumeMessage(execution, 2);

            assertThat(message.getSecurityContext()).isNull();
        }

        @Test
        @DisplayName("Should handle null metadata")
        void shouldHandleNullMetadata() {
            execution.setMetadata(null);

            WorkflowMessage message = factory.createResumeMessage(execution, 2);

            assertThat(message.getMetadata()).isNull();
        }

        @Test
        @DisplayName("Should handle null retry info")
        void shouldHandleNullRetryInfo() {
            execution.setRetryInfo(null);

            WorkflowMessage message = factory.createRetryMessage(execution);

            assertThat(message.getRetryInfo()).isNull();
        }
    }
}
