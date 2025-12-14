package io.stepprflow.core.tracing;

import io.micrometer.common.KeyValues;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationConvention;

/**
 * Observation convention for workflow step execution.
 * Defines the naming and tagging conventions for tracing workflow steps.
 */
public class WorkflowTracingObservationConvention
        implements ObservationConvention<WorkflowTracingContext> {

    /**
     * Singleton instance.
     */
    public static final WorkflowTracingObservationConvention INSTANCE =
            new WorkflowTracingObservationConvention();

    @Override
    public String getName() {
        return "stepprflow.workflow.step";
    }

    @Override
    public String getContextualName(final WorkflowTracingContext context) {
        return context.getTopic() + "." + context.getStepLabel();
    }

    @Override
    public KeyValues getLowCardinalityKeyValues(
            final WorkflowTracingContext context) {
        return KeyValues.of(
                "stepprflow.workflow.topic", context.getTopic(),
                "stepprflow.workflow.step.id",
                String.valueOf(context.getStepId()),
                "stepprflow.workflow.step.label", context.getStepLabel(),
                "stepprflow.workflow.status", context.getStatus()
        );
    }

    @Override
    public KeyValues getHighCardinalityKeyValues(
            final WorkflowTracingContext context) {
        return KeyValues.of(
                "stepprflow.workflow.execution.id",
                context.getExecutionId(),
                "stepprflow.workflow.correlation.id",
                context.getCorrelationId() != null
                        ? context.getCorrelationId() : ""
        );
    }

    @Override
    public boolean supportsContext(final Observation.Context context) {
        return context instanceof WorkflowTracingContext;
    }
}
