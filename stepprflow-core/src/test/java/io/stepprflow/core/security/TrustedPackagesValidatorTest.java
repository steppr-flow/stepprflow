package io.stepprflow.core.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for TrustedPackagesValidator - validates trusted packages configuration
 * for JSON deserialization security across all brokers (Kafka, RabbitMQ).
 */
@DisplayName("TrustedPackagesValidator")
class TrustedPackagesValidatorTest {

    @Nested
    @DisplayName("validate()")
    class ValidateTests {

        @Nested
        @DisplayName("When packages list is invalid")
        class InvalidListTests {

            @Test
            @DisplayName("Should reject null list")
            void shouldRejectNullList() {
                assertThatThrownBy(() -> TrustedPackagesValidator.validate(null))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("cannot be null");
            }

            @Test
            @DisplayName("Should reject empty list")
            void shouldRejectEmptyList() {
                assertThatThrownBy(() -> TrustedPackagesValidator.validate(List.of()))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("At least one trusted package");
            }
        }

        @Nested
        @DisplayName("When package name is invalid")
        class InvalidPackageNameTests {

            @Test
            @DisplayName("Should reject null package name in list")
            void shouldRejectNullPackageName() {
                List<String> packages = new java.util.ArrayList<>();
                packages.add(null);

                assertThatThrownBy(() -> TrustedPackagesValidator.validate(packages))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("cannot be null or blank");
            }

            @ParameterizedTest
            @NullAndEmptySource
            @ValueSource(strings = {"   ", "\t", "\n"})
            @DisplayName("Should reject blank package names")
            void shouldRejectBlankPackageNames(String pkg) {
                List<String> packages = new java.util.ArrayList<>();
                packages.add(pkg);

                assertThatThrownBy(() -> TrustedPackagesValidator.validate(packages))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("cannot be null or blank");
            }

            @ParameterizedTest
            @ValueSource(strings = {
                    "Com.example",      // Uppercase at start
                    "com.Example",      // Uppercase in segment
                    "123.example",      // Starts with number
                    "com.123example",   // Segment starts with number
                    "com..example",     // Empty segment
                    ".com.example",     // Leading dot
                    "com.example.",     // Trailing dot
                    "com-example",      // Hyphen
                    "com_example",      // Underscore
                    "com example"       // Space
            })
            @DisplayName("Should reject invalid package name format")
            void shouldRejectInvalidPackageFormat(String pkg) {
                assertThatThrownBy(() -> TrustedPackagesValidator.validate(List.of(pkg)))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("Invalid package name");
            }
        }

        @Nested
        @DisplayName("When wildcard is used (security vulnerability)")
        class WildcardSecurityTests {

            @Test
            @DisplayName("Should reject standalone wildcard (*) with SecurityException")
            void shouldRejectStandaloneWildcard() {
                assertThatThrownBy(() -> TrustedPackagesValidator.validate(List.of("*")))
                        .isInstanceOf(SecurityException.class)
                        .hasMessageContaining("Wildcard (*) is not allowed")
                        .hasMessageContaining("Remote Code Execution");
            }

            @ParameterizedTest
            @ValueSource(strings = {
                    "com.example.*",
                    "*.example",
                    "com.*.example",
                    "io.stepprflow.*"
            })
            @DisplayName("Should reject wildcard patterns with SecurityException")
            void shouldRejectWildcardPatterns(String pkg) {
                assertThatThrownBy(() -> TrustedPackagesValidator.validate(List.of(pkg)))
                        .isInstanceOf(SecurityException.class)
                        .hasMessageContaining("Wildcard patterns are not allowed")
                        .hasMessageContaining(pkg);
            }

            @Test
            @DisplayName("Should fail even if one package in list is wildcard")
            void shouldFailIfAnyPackageIsWildcard() {
                List<String> packages = List.of(
                        "io.stepprflow.core.model",
                        "*",
                        "com.example.domain"
                );

                assertThatThrownBy(() -> TrustedPackagesValidator.validate(packages))
                        .isInstanceOf(SecurityException.class);
            }
        }

        @Nested
        @DisplayName("When packages are valid")
        class ValidPackagesTests {

            @Test
            @DisplayName("Should accept single valid package")
            void shouldAcceptSingleValidPackage() {
                assertThatCode(() -> TrustedPackagesValidator.validate(List.of("io.stepprflow.core.model")))
                        .doesNotThrowAnyException();
            }

            @Test
            @DisplayName("Should accept multiple valid packages")
            void shouldAcceptMultipleValidPackages() {
                List<String> packages = List.of(
                        "io.stepprflow.core.model",
                        "com.example.domain",
                        "org.acme.workflow"
                );

                assertThatCode(() -> TrustedPackagesValidator.validate(packages))
                        .doesNotThrowAnyException();
            }

            @ParameterizedTest
            @ValueSource(strings = {
                    "com",
                    "io",
                    "org.example",
                    "com.example.domain",
                    "io.stepprflow.core.model",
                    "a.b.c.d.e.f.g.h.i.j"
            })
            @DisplayName("Should accept various valid package formats")
            void shouldAcceptVariousValidFormats(String pkg) {
                assertThatCode(() -> TrustedPackagesValidator.validate(List.of(pkg)))
                        .doesNotThrowAnyException();
            }
        }
    }
}
