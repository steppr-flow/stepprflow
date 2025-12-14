/**
 * Annotations for defining workflow steps and callbacks.
 *
 * <p>This package provides the core annotations used to define workflows:
 * <ul>
 *   <li>{@link io.stepprflow.core.annotation.Topic} - Marks a class as a workflow handler</li>
 *   <li>{@link io.stepprflow.core.annotation.Step} - Marks a method as a workflow step</li>
 *   <li>{@link io.stepprflow.core.annotation.Timeout} - Sets timeout for a step or workflow</li>
 *   <li>{@link io.stepprflow.core.annotation.OnSuccess} - Callback on workflow success</li>
 *   <li>{@link io.stepprflow.core.annotation.OnFailure} - Callback on workflow failure</li>
 * </ul>
 */
package io.stepprflow.core.annotation;
