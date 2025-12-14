/**
 * Spring application events for workflow orchestration.
 *
 * <p>This package provides:
 * <ul>
 *   <li>Workflow message events for decoupled monitoring</li>
 *   <li>Event-driven integration with other modules</li>
 *   <li>Support for custom event listeners</li>
 * </ul>
 *
 * <p>Events are published when workflow messages are received, allowing
 * other modules (like stepprflow-monitor) to react to workflow message
 * processing without tight coupling to the core workflow engine.
 */
package io.stepprflow.core.event;
