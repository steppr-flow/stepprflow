package io.stepprflow.broker.rabbitmq;

import io.stepprflow.core.StepprFlowProperties;
import io.stepprflow.core.security.TrustedPackagesValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TDD Tests for trusted packages security configuration in RabbitMQ.
 *
 * These tests ensure that JSON deserialization is restricted to trusted packages
 * to prevent remote code execution vulnerabilities.
 */
@DisplayName("RabbitMQ Trusted Packages Security")
class RabbitMQTrustedPackagesTest {

    @Nested
    @DisplayName("When configuring trusted packages")
    class TrustedPackagesConfiguration {

        @Test
        @DisplayName("should include stepprflow core model package by default")
        void shouldIncludeStepprFlowCoreModelByDefault() {
            // Given
            StepprFlowProperties properties = new StepprFlowProperties();

            // When
            List<String> trustedPackages = properties.getRabbitmq().getTrustedPackages();

            // Then
            assertThat(trustedPackages).contains("io.stepprflow.core.model");
        }

        @Test
        @DisplayName("should not include wildcard (*) by default")
        void shouldNotIncludeWildcardByDefault() {
            // Given
            StepprFlowProperties properties = new StepprFlowProperties();

            // When
            List<String> trustedPackages = properties.getRabbitmq().getTrustedPackages();

            // Then
            assertThat(trustedPackages).doesNotContain("*");
        }

        @Test
        @DisplayName("should allow adding custom trusted packages")
        void shouldAllowAddingCustomTrustedPackages() {
            // Given
            StepprFlowProperties properties = new StepprFlowProperties();

            // When
            properties.getRabbitmq().setTrustedPackages(
                List.of("io.stepprflow.core.model", "com.mycompany.domain")
            );

            // Then
            assertThat(properties.getRabbitmq().getTrustedPackages())
                .containsExactly("io.stepprflow.core.model", "com.mycompany.domain");
        }

        @Test
        @DisplayName("should reject wildcard when security validation is enabled")
        void shouldRejectWildcardWithSecurityValidation() {
            // Given
            StepprFlowProperties properties = new StepprFlowProperties();
            properties.getRabbitmq().setTrustedPackages(List.of("*"));

            // When/Then
            assertThatThrownBy(() -> TrustedPackagesValidator.validate(properties.getRabbitmq().getTrustedPackages()))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Wildcard (*) is not allowed in trusted packages");
        }
    }

    @Nested
    @DisplayName("When validating trusted packages for RabbitMQ")
    class TrustedPackagesValidation {

        @Test
        @DisplayName("should accept valid Java package names")
        void shouldAcceptValidPackageNames() {
            // Given
            List<String> packages = List.of(
                "io.stepprflow.core.model",
                "com.mycompany.domain",
                "org.example.workflow"
            );

            // When/Then - should not throw
            TrustedPackagesValidator.validate(packages);
        }

        @Test
        @DisplayName("should reject empty package list")
        void shouldRejectEmptyPackageList() {
            // Given
            List<String> packages = List.of();

            // When/Then
            assertThatThrownBy(() -> TrustedPackagesValidator.validate(packages))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("At least one trusted package must be configured");
        }

        @Test
        @DisplayName("should reject null package list")
        void shouldRejectNullPackageList() {
            // When/Then
            assertThatThrownBy(() -> TrustedPackagesValidator.validate(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Trusted packages cannot be null");
        }

        @Test
        @DisplayName("should reject packages with wildcards anywhere")
        void shouldRejectPackagesWithWildcards() {
            // Given
            List<String> packages = List.of("com.mycompany.*");

            // When/Then
            assertThatThrownBy(() -> TrustedPackagesValidator.validate(packages))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Wildcard patterns are not allowed");
        }

        @Test
        @DisplayName("should reject invalid package names")
        void shouldRejectInvalidPackageNames() {
            // Given
            List<String> packages = List.of("invalid..package");

            // When/Then
            assertThatThrownBy(() -> TrustedPackagesValidator.validate(packages))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid package name");
        }
    }

    @Nested
    @DisplayName("RabbitMQ configuration parity with Kafka")
    class ConfigurationParityTests {

        @Test
        @DisplayName("RabbitMQ and Kafka should have same default trusted packages")
        void shouldHaveSameDefaultTrustedPackages() {
            // Given
            StepprFlowProperties properties = new StepprFlowProperties();

            // When
            List<String> kafkaTrusted = properties.getKafka().getTrustedPackages();
            List<String> rabbitTrusted = properties.getRabbitmq().getTrustedPackages();

            // Then
            assertThat(rabbitTrusted).isEqualTo(kafkaTrusted);
        }

        @Test
        @DisplayName("Both brokers should use shared TrustedPackagesValidator")
        void shouldUseSharedValidator() {
            // Given
            StepprFlowProperties properties = new StepprFlowProperties();
            properties.getKafka().setTrustedPackages(List.of("*"));
            properties.getRabbitmq().setTrustedPackages(List.of("*"));

            // When/Then - both should throw the same exception
            assertThatThrownBy(() -> TrustedPackagesValidator.validate(properties.getKafka().getTrustedPackages()))
                .isInstanceOf(SecurityException.class);
            assertThatThrownBy(() -> TrustedPackagesValidator.validate(properties.getRabbitmq().getTrustedPackages()))
                .isInstanceOf(SecurityException.class);
        }
    }
}
