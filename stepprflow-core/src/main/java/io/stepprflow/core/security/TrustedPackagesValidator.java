package io.stepprflow.core.security;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Validates trusted packages configuration for JSON deserialization security.
 *
 * <p>This validator ensures that:
 * <ul>
 *   <li>Wildcards (*) are not used, preventing deserialization of arbitrary
 *   classes</li>
 *   <li>Package names follow valid Java naming conventions</li>
 *   <li>At least one trusted package is configured</li>
 * </ul>
 *
 * <p>Using wildcards in trusted packages is a security vulnerability
 * that can lead to Remote Code Execution (RCE) attacks through malicious
 * JSON payloads.
 *
 * <p>This validator is broker-agnostic and can be used by Kafka, RabbitMQ,
 * or any other messaging broker that needs to validate trusted packages.
 */
public final class TrustedPackagesValidator {

    private static final Pattern VALID_PACKAGE_PATTERN =
        Pattern.compile("^[a-z][a-z0-9]*(\\.[a-z][a-z0-9]*)*$");

    /**
     * Private constructor to prevent instantiation.
     */
    private TrustedPackagesValidator() {
        // Utility class
    }

    /**
     * Validates the list of trusted packages.
     *
     * @param trustedPackages the list of package names to validate
     * @throws IllegalArgumentException if the list is null, empty,
     *         or contains invalid package names
     * @throws SecurityException if wildcards are detected
     */
    public static void validate(final List<String> trustedPackages) {
        if (trustedPackages == null) {
            throw new IllegalArgumentException(
                    "Trusted packages cannot be null");
        }

        if (trustedPackages.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one trusted package must be configured");
        }

        for (String pkg : trustedPackages) {
            validatePackage(pkg);
        }
    }

    /**
     * Validates a single package name.
     *
     * @param pkg the package name to validate
     * @throws IllegalArgumentException if the package name is invalid
     * @throws SecurityException if wildcards are detected
     */
    private static void validatePackage(final String pkg) {
        if (pkg == null || pkg.isBlank()) {
            throw new IllegalArgumentException(
                    "Package name cannot be null or blank");
        }

        // Check for wildcard
        if ("*".equals(pkg)) {
            throw new SecurityException(
                "Wildcard (*) is not allowed in trusted packages. "
                + "This is a security vulnerability that can lead to "
                + "Remote Code Execution. "
                + "Please specify explicit package names like "
                + "'io.stepprflow.core.model'."
            );
        }

        // Check for wildcard patterns (e.g., "com.example.*")
        if (pkg.contains("*")) {
            throw new SecurityException(
                "Wildcard patterns are not allowed in trusted packages: '"
                + pkg + "'. "
                + "Please specify the complete package name."
            );
        }

        // Validate package name format
        if (!VALID_PACKAGE_PATTERN.matcher(pkg).matches()) {
            throw new IllegalArgumentException(
                "Invalid package name: '" + pkg + "'. "
                + "Package names must follow Java naming conventions "
                + "(e.g., 'com.example.domain')."
            );
        }
    }
}
