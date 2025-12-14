package io.stepprflow.core.service;

import io.stepprflow.core.model.*;
import io.stepprflow.core.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CoreMessageFactory Tests")
class CoreMessageFactoryTest {

    private CoreMessageFactory factory;
    private WorkflowMessage originalMessage;
    private StepDefinition testStep;

    @BeforeEach
    void setUp() {
        factory = new CoreMessageFactory();

        originalMessage = WorkflowMessage.builder()
                .executionId("exec-123")
                .correlationId("corr-456")
                .topic("test-topic")
                .currentStep(2)
                .totalSteps(5)
                .status(WorkflowStatus.IN_PROGRESS)
                .payload(Map.of("orderId", "ORD-001"))
                .payloadType("java.util.Map")
                .securityContext("auth-token")
                .metadata(Map.of("key", "value"))
                .createdAt(Instant.now().minusSeconds(3600))
                .build();

        testStep = StepDefinition.builder()
                .id(2)
                .label("processOrder")
                .description("Process the order")
                .build();
    }

    @Nested
    @DisplayName("createRetryMessage()")
    class CreateRetryMessageTests {

        @Test
        @DisplayName("Should create message with RETRY_PENDING status")
        void shouldCreateMessageWithRetryPendingStatus() {
            RetryInfo retryInfo = RetryInfo.builder()
                    .attempt(2)
                    .maxAttempts(3)
                    .nextRetryAt(Instant.now().plusSeconds(60))
                    .build();

            WorkflowMessage retryMessage = factory.createRetryMessage(originalMessage, retryInfo);

            assertThat(retryMessage.getStatus()).isEqualTo(WorkflowStatus.RETRY_PENDING);
        }

        @Test
        @DisplayName("Should preserve original message fields")
        void shouldPreserveOriginalMessageFields() {
            RetryInfo retryInfo = RetryInfo.builder()
                    .attempt(2)
                    .maxAttempts(3)
                    .build();

            WorkflowMessage retryMessage = factory.createRetryMessage(originalMessage, retryInfo);

            assertThat(retryMessage.getExecutionId()).isEqualTo("exec-123");
            assertThat(retryMessage.getCorrelationId()).isEqualTo("corr-456");
            assertThat(retryMessage.getTopic()).isEqualTo("test-topic");
            assertThat(retryMessage.getCurrentStep()).isEqualTo(2);
            assertThat(retryMessage.getTotalSteps()).isEqualTo(5);
            assertThat(retryMessage.getPayload()).isEqualTo(Map.of("orderId", "ORD-001"));
            assertThat(retryMessage.getPayloadType()).isEqualTo("java.util.Map");
            assertThat(retryMessage.getSecurityContext()).isEqualTo("auth-token");
            assertThat(retryMessage.getMetadata()).isEqualTo(Map.of("key", "value"));
            assertThat(retryMessage.getCreatedAt()).isEqualTo(originalMessage.getCreatedAt());
        }

        @Test
        @DisplayName("Should set provided retry info")
        void shouldSetProvidedRetryInfo() {
            Instant nextRetry = Instant.now().plusSeconds(120);
            RetryInfo retryInfo = RetryInfo.builder()
                    .attempt(2)
                    .maxAttempts(5)
                    .nextRetryAt(nextRetry)
                    .lastError("Previous error")
                    .build();

            WorkflowMessage retryMessage = factory.createRetryMessage(originalMessage, retryInfo);

            assertThat(retryMessage.getRetryInfo()).isNotNull();
            assertThat(retryMessage.getRetryInfo().getAttempt()).isEqualTo(2);
            assertThat(retryMessage.getRetryInfo().getMaxAttempts()).isEqualTo(5);
            assertThat(retryMessage.getRetryInfo().getNextRetryAt()).isEqualTo(nextRetry);
            assertThat(retryMessage.getRetryInfo().getLastError()).isEqualTo("Previous error");
        }

        @Test
        @DisplayName("Should set updatedAt to current time")
        void shouldSetUpdatedAtToCurrentTime() {
            RetryInfo retryInfo = RetryInfo.builder()
                    .attempt(1)
                    .maxAttempts(3)
                    .build();

            Instant before = Instant.now();
            WorkflowMessage retryMessage = factory.createRetryMessage(originalMessage, retryInfo);
            Instant after = Instant.now();

            assertThat(retryMessage.getUpdatedAt()).isAfterOrEqualTo(before);
            assertThat(retryMessage.getUpdatedAt()).isBeforeOrEqualTo(after);
        }

        @Test
        @DisplayName("Should not modify original message")
        void shouldNotModifyOriginalMessage() {
            RetryInfo retryInfo = RetryInfo.builder()
                    .attempt(2)
                    .maxAttempts(3)
                    .build();

            factory.createRetryMessage(originalMessage, retryInfo);

            assertThat(originalMessage.getStatus()).isEqualTo(WorkflowStatus.IN_PROGRESS);
            assertThat(originalMessage.getRetryInfo()).isNull();
        }
    }

    @Nested
    @DisplayName("createDlqMessage()")
    class CreateDlqMessageTests {

        @Test
        @DisplayName("Should create message with FAILED status")
        void shouldCreateMessageWithFailedStatus() {
            ErrorInfo errorInfo = ErrorInfo.builder()
                    .code("STEP_EXECUTION_FAILED")
                    .message("Test error")
                    .build();

            WorkflowMessage dlqMessage = factory.createDlqMessage(originalMessage, errorInfo);

            assertThat(dlqMessage.getStatus()).isEqualTo(WorkflowStatus.FAILED);
        }

        @Test
        @DisplayName("Should preserve original message fields")
        void shouldPreserveOriginalMessageFields() {
            ErrorInfo errorInfo = ErrorInfo.builder()
                    .code("STEP_EXECUTION_FAILED")
                    .message("Test error")
                    .build();

            WorkflowMessage dlqMessage = factory.createDlqMessage(originalMessage, errorInfo);

            assertThat(dlqMessage.getExecutionId()).isEqualTo("exec-123");
            assertThat(dlqMessage.getCorrelationId()).isEqualTo("corr-456");
            assertThat(dlqMessage.getTopic()).isEqualTo("test-topic");
            assertThat(dlqMessage.getCurrentStep()).isEqualTo(2);
            assertThat(dlqMessage.getTotalSteps()).isEqualTo(5);
            assertThat(dlqMessage.getPayload()).isEqualTo(Map.of("orderId", "ORD-001"));
            assertThat(dlqMessage.getPayloadType()).isEqualTo("java.util.Map");
            assertThat(dlqMessage.getSecurityContext()).isEqualTo("auth-token");
            assertThat(dlqMessage.getMetadata()).isEqualTo(Map.of("key", "value"));
            assertThat(dlqMessage.getCreatedAt()).isEqualTo(originalMessage.getCreatedAt());
        }

        @Test
        @DisplayName("Should set provided error info")
        void shouldSetProvidedErrorInfo() {
            ErrorInfo errorInfo = ErrorInfo.builder()
                    .code("STEP_EXECUTION_FAILED")
                    .message("Test error message")
                    .exceptionType("java.lang.RuntimeException")
                    .stackTrace("at com.example.Test.method(Test.java:42)")
                    .stepId(2)
                    .stepLabel("processOrder")
                    .build();

            WorkflowMessage dlqMessage = factory.createDlqMessage(originalMessage, errorInfo);

            assertThat(dlqMessage.getErrorInfo()).isNotNull();
            assertThat(dlqMessage.getErrorInfo().getCode()).isEqualTo("STEP_EXECUTION_FAILED");
            assertThat(dlqMessage.getErrorInfo().getMessage()).isEqualTo("Test error message");
            assertThat(dlqMessage.getErrorInfo().getExceptionType()).isEqualTo("java.lang.RuntimeException");
            assertThat(dlqMessage.getErrorInfo().getStackTrace()).contains("Test.java:42");
            assertThat(dlqMessage.getErrorInfo().getStepId()).isEqualTo(2);
            assertThat(dlqMessage.getErrorInfo().getStepLabel()).isEqualTo("processOrder");
        }

        @Test
        @DisplayName("Should preserve existing retry info")
        void shouldPreserveExistingRetryInfo() {
            RetryInfo retryInfo = RetryInfo.builder()
                    .attempt(3)
                    .maxAttempts(3)
                    .lastError("Previous retry error")
                    .build();
            originalMessage = originalMessage.toBuilder()
                    .retryInfo(retryInfo)
                    .build();

            ErrorInfo errorInfo = ErrorInfo.builder()
                    .code("STEP_EXECUTION_FAILED")
                    .message("Final error")
                    .build();

            WorkflowMessage dlqMessage = factory.createDlqMessage(originalMessage, errorInfo);

            assertThat(dlqMessage.getRetryInfo()).isNotNull();
            assertThat(dlqMessage.getRetryInfo().getAttempt()).isEqualTo(3);
            assertThat(dlqMessage.getRetryInfo().getMaxAttempts()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should set updatedAt to current time")
        void shouldSetUpdatedAtToCurrentTime() {
            ErrorInfo errorInfo = ErrorInfo.builder()
                    .code("STEP_EXECUTION_FAILED")
                    .message("Test error")
                    .build();

            Instant before = Instant.now();
            WorkflowMessage dlqMessage = factory.createDlqMessage(originalMessage, errorInfo);
            Instant after = Instant.now();

            assertThat(dlqMessage.getUpdatedAt()).isAfterOrEqualTo(before);
            assertThat(dlqMessage.getUpdatedAt()).isBeforeOrEqualTo(after);
        }

        @Test
        @DisplayName("Should not modify original message")
        void shouldNotModifyOriginalMessage() {
            ErrorInfo errorInfo = ErrorInfo.builder()
                    .code("STEP_EXECUTION_FAILED")
                    .message("Test error")
                    .build();

            factory.createDlqMessage(originalMessage, errorInfo);

            assertThat(originalMessage.getStatus()).isEqualTo(WorkflowStatus.IN_PROGRESS);
            assertThat(originalMessage.getErrorInfo()).isNull();
        }
    }

    @Nested
    @DisplayName("createErrorInfo()")
    class CreateErrorInfoTests {

        @Test
        @DisplayName("Should create error info from exception and step")
        void shouldCreateErrorInfoFromExceptionAndStep() {
            RuntimeException error = new RuntimeException("Test error message");

            ErrorInfo errorInfo = factory.createErrorInfo(error, testStep);

            assertThat(errorInfo.getCode()).isEqualTo("STEP_EXECUTION_FAILED");
            assertThat(errorInfo.getMessage()).isEqualTo("Test error message");
            assertThat(errorInfo.getExceptionType()).isEqualTo("java.lang.RuntimeException");
            assertThat(errorInfo.getStepId()).isEqualTo(2);
            assertThat(errorInfo.getStepLabel()).isEqualTo("processOrder");
        }

        @Test
        @DisplayName("Should include stack trace")
        void shouldIncludeStackTrace() {
            RuntimeException error = new RuntimeException("Test error");

            ErrorInfo errorInfo = factory.createErrorInfo(error, testStep);

            assertThat(errorInfo.getStackTrace()).isNotNull();
            assertThat(errorInfo.getStackTrace()).contains("CoreMessageFactoryTest");
        }

        @Test
        @DisplayName("Should truncate long stack traces")
        void shouldTruncateLongStackTraces() {
            // Create an exception with a very long stack trace by adding many "cause" layers
            RuntimeException error = createDeepException(50);

            ErrorInfo errorInfo = factory.createErrorInfo(error, testStep);

            assertThat(errorInfo.getStackTrace().length()).isLessThanOrEqualTo(2003); // 2000 + "..."
        }

        @Test
        @DisplayName("Should handle null error message")
        void shouldHandleNullErrorMessage() {
            RuntimeException error = new RuntimeException((String) null);

            ErrorInfo errorInfo = factory.createErrorInfo(error, testStep);

            assertThat(errorInfo.getMessage()).isNull();
        }

        private RuntimeException createDeepException(int depth) {
            RuntimeException current = new RuntimeException("Base exception with long message to ensure stack trace is long enough for truncation test");
            for (int i = 1; i <= depth; i++) {
                current = new RuntimeException("Level " + i + " with additional text to make the stack trace longer", current);
            }
            return current;
        }
    }
}
