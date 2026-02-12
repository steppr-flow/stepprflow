package io.github.stepprflow.core.registration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RegistrationProperties Tests")
class RegistrationPropertiesTest {

    @Nested
    @DisplayName("Default values")
    class DefaultValuesTests {

        @Test
        @DisplayName("Should have enabled true by default")
        void shouldHaveEnabledTrueByDefault() {
            RegistrationProperties props = new RegistrationProperties();
            assertThat(props.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("Should have heartbeatIntervalSeconds 30 by default")
        void shouldHaveHeartbeatInterval30ByDefault() {
            RegistrationProperties props = new RegistrationProperties();
            assertThat(props.getHeartbeatIntervalSeconds()).isEqualTo(30);
        }
    }

    @Nested
    @DisplayName("Setters")
    class SetterTests {

        @Test
        @DisplayName("Should set and get enabled")
        void shouldSetAndGetEnabled() {
            RegistrationProperties props = new RegistrationProperties();
            props.setEnabled(false);
            assertThat(props.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("Should set and get heartbeatIntervalSeconds")
        void shouldSetAndGetHeartbeatIntervalSeconds() {
            RegistrationProperties props = new RegistrationProperties();
            props.setHeartbeatIntervalSeconds(60);
            assertThat(props.getHeartbeatIntervalSeconds()).isEqualTo(60);
        }
    }
}
