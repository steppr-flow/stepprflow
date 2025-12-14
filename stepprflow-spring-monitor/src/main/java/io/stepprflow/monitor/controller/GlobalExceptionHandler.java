package io.stepprflow.monitor.controller;

import io.stepprflow.monitor.exception.ConcurrentModificationException;
import io.stepprflow.monitor.exception.ResourceNotFoundException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler for the monitor API.
 * Provides consistent error responses across all endpoints.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handle IllegalArgumentException - invalid input from client.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Invalid argument: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(buildErrorResponse("INVALID_ARGUMENT", ex.getMessage()));
    }

    /**
     * Handle IllegalStateException - operation not allowed in current state.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex) {
        log.warn("Invalid state: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(buildErrorResponse("INVALID_STATE", ex.getMessage()));
    }

    /**
     * Handle ConcurrentModificationException - optimistic locking failure.
     */
    @ExceptionHandler(ConcurrentModificationException.class)
    public ResponseEntity<Map<String, Object>> handleConcurrentModification(ConcurrentModificationException ex) {
        log.warn("Concurrent modification for execution {}: {}", ex.getExecutionId(), ex.getMessage());
        Map<String, Object> response = buildErrorResponse("CONCURRENT_MODIFICATION", ex.getMessage());
        response.put("executionId", ex.getExecutionId());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(response);
    }

    /**
     * Handle ResourceNotFoundException - requested resource not found.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {} {}", ex.getResourceType(), ex.getResourceId());
        Map<String, Object> response = buildErrorResponse("RESOURCE_NOT_FOUND", ex.getMessage());
        response.put("resourceType", ex.getResourceType());
        response.put("resourceId", ex.getResourceId());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(response);
    }

    /**
     * Handle validation errors from @Valid annotations on request body.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationError(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("Validation error: {}", errors);
        Map<String, Object> response = buildErrorResponse("VALIDATION_ERROR", "Validation failed: " + errors);
        response.put("fieldErrors", ex.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of(
                        "field", error.getField(),
                        "message", error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid value"
                ))
                .collect(Collectors.toList()));
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * Handle constraint violation errors from @Validated on method parameters.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException ex) {
        List<Map<String, String>> violations = ex.getConstraintViolations().stream()
                .map(violation -> {
                    String path = violation.getPropertyPath().toString();
                    // Extract parameter name from path (e.g., "listExecutions.page" -> "page")
                    String field = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
                    return Map.of(
                            "field", field,
                            "message", violation.getMessage()
                    );
                })
                .collect(Collectors.toList());

        String errorMessage = violations.stream()
                .map(v -> v.get("field") + ": " + v.get("message"))
                .collect(Collectors.joining(", "));

        log.warn("Constraint violation: {}", errorMessage);
        Map<String, Object> response = buildErrorResponse("VALIDATION_ERROR", "Validation failed: " + errorMessage);
        response.put("fieldErrors", violations);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * Handle all other unexpected exceptions.
     * Logs the full stack trace but returns a safe message to the client.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildErrorResponse("INTERNAL_ERROR", "An unexpected error occurred. Please try again later."));
    }

    /**
     * Build a standardized error response.
     */
    private Map<String, Object> buildErrorResponse(String code, String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("timestamp", Instant.now().toString());
        response.put("code", code);
        response.put("message", message);
        return response;
    }
}
