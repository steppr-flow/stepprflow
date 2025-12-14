package io.stepprflow.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Error information for failed workflows.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorInfo {

    /**
     * Error code.
     */
    private String code;

    /**
     * Error message.
     */
    private String message;

    /**
     * Exception class name.
     */
    private String exceptionType;

    /**
     * Stack trace (truncated).
     */
    private String stackTrace;

    /**
     * Step ID where error occurred.
     */
    private int stepId;

    /**
     * Step label where error occurred.
     */
    private String stepLabel;

    /**
     * Error timestamp.
     */
    @Builder.Default
    private Instant timestamp = Instant.now();
}
