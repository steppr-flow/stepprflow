package io.stepprflow.core.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("StackTraceUtils Tests")
class StackTraceUtilsTest {

    private static final int DEFAULT_MAX_LENGTH = 2000;

    @Nested
    @DisplayName("truncate()")
    class TruncateTests {

        @Test
        @DisplayName("Should return stack trace for simple exception")
        void shouldReturnStackTraceForSimpleException() {
            Exception exception = new RuntimeException("Test error");

            String result = StackTraceUtils.truncate(exception, DEFAULT_MAX_LENGTH);

            assertThat(result).isNotNull();
            assertThat(result).contains("RuntimeException");
            assertThat(result).contains("Test error");
        }

        @Test
        @DisplayName("Should truncate long stack trace and append ellipsis")
        void shouldTruncateLongStackTraceAndAppendEllipsis() {
            Exception exception = createDeepException(100);

            String result = StackTraceUtils.truncate(exception, DEFAULT_MAX_LENGTH);

            assertThat(result).hasSize(DEFAULT_MAX_LENGTH + 3); // +3 for "..."
            assertThat(result).endsWith("...");
        }

        @Test
        @DisplayName("Should not truncate when stack trace is shorter than max length")
        void shouldNotTruncateWhenShorterThanMaxLength() {
            Exception exception = new RuntimeException("Short");

            // Use a very large max length to ensure no truncation
            String result = StackTraceUtils.truncate(exception, 10000);

            assertThat(result).doesNotEndWith("...");
            assertThat(result).contains("RuntimeException");
            assertThat(result).contains("Short");
        }

        @Test
        @DisplayName("Should handle null exception")
        void shouldHandleNullException() {
            String result = StackTraceUtils.truncate(null, DEFAULT_MAX_LENGTH);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should handle exception with null message")
        void shouldHandleExceptionWithNullMessage() {
            Exception exception = new RuntimeException((String) null);

            String result = StackTraceUtils.truncate(exception, DEFAULT_MAX_LENGTH);

            assertThat(result).isNotNull();
            assertThat(result).contains("RuntimeException");
        }

        @Test
        @DisplayName("Should respect custom max length")
        void shouldRespectCustomMaxLength() {
            Exception exception = createDeepException(50);
            int customMaxLength = 500;

            String result = StackTraceUtils.truncate(exception, customMaxLength);

            assertThat(result.length()).isLessThanOrEqualTo(customMaxLength + 3);
        }

        @Test
        @DisplayName("Should throw exception for negative max length")
        void shouldThrowExceptionForNegativeMaxLength() {
            Exception exception = new RuntimeException("Test");

            assertThatThrownBy(() -> StackTraceUtils.truncate(exception, -1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("maxLength");
        }

        @Test
        @DisplayName("Should handle zero max length")
        void shouldHandleZeroMaxLength() {
            Exception exception = new RuntimeException("Test");

            String result = StackTraceUtils.truncate(exception, 0);

            assertThat(result).isEqualTo("...");
        }

        @Test
        @DisplayName("Should include caused by chain")
        void shouldIncludeCausedByChain() {
            Exception cause = new IllegalStateException("Root cause");
            Exception exception = new RuntimeException("Wrapper", cause);

            // Use large max length to capture full stack trace with cause
            String result = StackTraceUtils.truncate(exception, 10000);

            assertThat(result).contains("RuntimeException");
            assertThat(result).contains("Caused by");
            assertThat(result).contains("IllegalStateException");
            assertThat(result).contains("Root cause");
        }
    }

    @Nested
    @DisplayName("truncate() with default max length")
    class TruncateDefaultTests {

        @Test
        @DisplayName("Should use default max length of 2000")
        void shouldUseDefaultMaxLength() {
            Exception exception = createDeepException(100);

            String result = StackTraceUtils.truncate(exception);

            assertThat(result).hasSize(DEFAULT_MAX_LENGTH + 3);
            assertThat(result).endsWith("...");
        }
    }

    private Exception createDeepException(int depth) {
        try {
            throwDeep(depth);
        } catch (Exception e) {
            return e;
        }
        return null;
    }

    private void throwDeep(int depth) {
        if (depth <= 0) {
            throw new RuntimeException("Deep exception with long message: " + "A".repeat(500));
        }
        throwDeep(depth - 1);
    }
}