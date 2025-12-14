/**
 * Metrics collection and monitoring for workflow orchestration.
 *
 * <p>This package provides:
 * <ul>
 *   <li>Micrometer-based metrics collection for workflows and steps</li>
 *   <li>Event listeners for capturing workflow lifecycle events</li>
 *   <li>Metrics summary APIs for monitoring dashboards</li>
 *   <li>Auto-configuration for metrics when Micrometer is available</li>
 * </ul>
 *
 * <p>Key metrics include:
 * <ul>
 *   <li>Workflow started/completed/failed/cancelled counters</li>
 *   <li>Active workflow gauges</li>
 *   <li>Workflow and step duration timers</li>
 *   <li>Retry and DLQ counters</li>
 * </ul>
 */
package io.stepprflow.core.metrics;
