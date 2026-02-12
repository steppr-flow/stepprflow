package io.github.stepprflow.core.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SecurityContextPropagator")
class SecurityContextPropagatorTest {

    @Nested
    @DisplayName("NoOpSecurityContextPropagator")
    class NoOpSecurityContextPropagatorTest {

        private final SecurityContextPropagator propagator = new NoOpSecurityContextPropagator();

        @Test
        @DisplayName("capture() should return null")
        void captureShouldReturnNull() {
            String result = propagator.capture();
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("restore() should not throw with null context")
        void restoreShouldNotThrowWithNullContext() {
            propagator.restore(null);
            // No exception expected
        }

        @Test
        @DisplayName("restore() should not throw with valid context")
        void restoreShouldNotThrowWithValidContext() {
            propagator.restore("some-security-context");
            // No exception expected
        }

        @Test
        @DisplayName("clear() should not throw")
        void clearShouldNotThrow() {
            propagator.clear();
            // No exception expected
        }

        @Test
        @DisplayName("isEnabled() should return false")
        void isEnabledShouldReturnFalse() {
            assertThat(propagator.isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("Contract verification")
    class ContractTest {

        @Test
        @DisplayName("capture-restore-clear cycle should work")
        void captureRestoreClearCycleShouldWork() {
            SecurityContextPropagator propagator = new NoOpSecurityContextPropagator();

            // Capture
            String captured = propagator.capture();

            // Restore
            propagator.restore(captured);

            // Clear
            propagator.clear();

            // No exceptions should occur
        }
    }
}