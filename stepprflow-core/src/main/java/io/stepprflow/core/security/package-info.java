/**
 * Security utilities for workflow orchestration.
 *
 * <p>This package provides:
 * <ul>
 *   <li>Trusted packages validation for JSON deserialization security</li>
 *   <li>Protection against Remote Code Execution (RCE) attacks</li>
 *   <li>Wildcard detection and prevention</li>
 *   <li>Java package naming convention validation</li>
 * </ul>
 *
 * <p>The trusted packages validator ensures that only explicitly specified
 * packages are allowed for JSON deserialization, preventing malicious
 * payloads from executing arbitrary code.
 *
 * <p><strong>Security Note:</strong> Never use wildcards (*) in trusted
 * packages configuration as this enables RCE attacks.
 */
package io.stepprflow.core.security;
