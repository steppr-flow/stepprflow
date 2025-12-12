package io.stepprflow.agent;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for AgentProperties.
 */
@DisplayName("AgentProperties Tests")
class AgentPropertiesTest {

    @Nested
    @DisplayName("Default values")
    class DefaultValuesTests {

        @Test
        @DisplayName("should have correct default values")
        void shouldHaveCorrectDefaultValues() {
            AgentProperties properties = new AgentProperties();

            assertThat(properties)
                    .extracting(
                            AgentProperties::getServerUrl,
                            AgentProperties::isAutoRegister,
                            AgentProperties::getHeartbeatIntervalSeconds,
                            AgentProperties::getConnectTimeoutMs,
                            AgentProperties::getReadTimeoutMs)
                    .containsExactly(null, true, 30, 5000, 10000);
        }
    }

    @Nested
    @DisplayName("Setters and getters")
    class SettersAndGettersTests {

        @Test
        @DisplayName("should set and get all properties")
        void shouldSetAndGetAllProperties() {
            AgentProperties properties = new AgentProperties();
            properties.setServerUrl("http://localhost:8090");
            properties.setAutoRegister(false);
            properties.setHeartbeatIntervalSeconds(60);
            properties.setConnectTimeoutMs(3000);
            properties.setReadTimeoutMs(15000);

            assertThat(properties)
                    .extracting(
                            AgentProperties::getServerUrl,
                            AgentProperties::isAutoRegister,
                            AgentProperties::getHeartbeatIntervalSeconds,
                            AgentProperties::getConnectTimeoutMs,
                            AgentProperties::getReadTimeoutMs)
                    .containsExactly("http://localhost:8090", false, 60, 3000, 15000);
        }
    }
}
