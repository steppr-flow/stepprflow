package io.stepprflow.agent;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.annotation.AnnotationUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for AgentAutoConfiguration.
 */
@DisplayName("AgentAutoConfiguration Tests")
class AgentAutoConfigurationTest {

    @Nested
    @DisplayName("Annotation verification")
    class AnnotationVerificationTests {

        @Test
        @DisplayName("should have ConditionalOnProperty annotation with correct values")
        void shouldHaveConditionalOnPropertyAnnotation() {
            ConditionalOnProperty annotation = AnnotationUtils.findAnnotation(
                    AgentAutoConfiguration.class, ConditionalOnProperty.class);

            assertThat(annotation).isNotNull();
            assertThat(annotation)
                    .extracting(
                            ConditionalOnProperty::prefix,
                            ConditionalOnProperty::havingValue,
                            ConditionalOnProperty::matchIfMissing)
                    .containsExactly("stepprflow", "true", true);
            assertThat(annotation.name()).containsExactly("enabled");
        }

        @Test
        @DisplayName("should have EnableConfigurationProperties annotation for AgentProperties")
        void shouldHaveEnableConfigurationPropertiesAnnotation() {
            EnableConfigurationProperties annotation = AnnotationUtils.findAnnotation(
                    AgentAutoConfiguration.class, EnableConfigurationProperties.class);

            assertThat(annotation).isNotNull();
            assertThat(annotation.value()).contains(AgentProperties.class);
        }
    }

    @Nested
    @DisplayName("Configuration class structure")
    class ConfigurationClassStructureTests {

        @Test
        @DisplayName("should be a valid configuration class")
        void shouldBeValidConfigurationClass() {
            assertThat(AgentAutoConfiguration.class).isPublic();
            assertThat(AgentAutoConfiguration.class.getConstructors()).hasSize(1);
        }

        @Test
        @DisplayName("should be instantiable")
        void shouldBeInstantiable() throws Exception {
            AgentAutoConfiguration config = AgentAutoConfiguration.class
                    .getDeclaredConstructor()
                    .newInstance();
            assertThat(config).isNotNull();
        }
    }
}