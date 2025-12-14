/**
 * Utility classes for workflow orchestration.
 *
 * <p>This package provides:
 * <ul>
 *   <li>Nested path resolver for accessing values in complex Map
 *   structures</li>
 *   <li>Support for dot-notation paths with array index notation</li>
 *   <li>Helper methods for common workflow operations</li>
 * </ul>
 *
 * <p>The nested path resolver supports paths like:
 * <ul>
 *   <li>{@code "name"} - simple top-level key</li>
 *   <li>{@code "address.city"} - nested key</li>
 *   <li>{@code "items[0]"} - array element by index</li>
 *   <li>{@code "items[0].name"} - nested key inside array element</li>
 * </ul>
 */
package io.stepprflow.core.util;
