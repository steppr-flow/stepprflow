/**
 * Distributed tracing support for workflow orchestration.
 *
 * <p>This package provides:
 * <ul>
 *   <li>Micrometer Observation API integration for distributed tracing</li>
 *   <li>Workflow step execution tracing with context propagation</li>
 *   <li>Custom observation conventions for workflow-specific tags</li>
 *   <li>Auto-configuration for tracing when Observation API is available</li>
 * </ul>
 *
 * <p>Tracing includes:
 * <ul>
 *   <li>Execution ID and correlation ID propagation</li>
 *   <li>Step-level timing and status tracking</li>
 *   <li>High and low cardinality tags for filtering</li>
 *   <li>Integration with Zipkin, Jaeger, and other tracing systems</li>
 * </ul>
 */
package io.stepprflow.core.tracing;
