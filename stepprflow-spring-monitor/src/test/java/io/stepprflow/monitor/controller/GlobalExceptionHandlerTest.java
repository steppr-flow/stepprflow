package io.stepprflow.monitor.controller;

import io.stepprflow.monitor.exception.ConcurrentModificationException;
import io.stepprflow.monitor.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for GlobalExceptionHandler.
 */
@DisplayName("GlobalExceptionHandler Tests")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Nested
    @DisplayName("IllegalArgumentException handling")
    class IllegalArgumentExceptionTests {

        @Test
        @DisplayName("Should return 400 BAD_REQUEST for IllegalArgumentException")
        void shouldReturn400ForIllegalArgument() {
            IllegalArgumentException ex = new IllegalArgumentException("Invalid input");

            ResponseEntity<?> response = handler.handleIllegalArgument(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Should include error message in response")
        void shouldIncludeErrorMessage() {
            IllegalArgumentException ex = new IllegalArgumentException("Field 'name' is required");

            ResponseEntity<?> response = handler.handleIllegalArgument(ex);

            assertThat(response.getBody()).isNotNull();
            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) response.getBody();
            assertThat(body.get("message")).isEqualTo("Field 'name' is required");
        }

        @Test
        @DisplayName("Should include error code in response")
        void shouldIncludeErrorCode() {
            IllegalArgumentException ex = new IllegalArgumentException("Invalid");

            ResponseEntity<?> response = handler.handleIllegalArgument(ex);

            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) response.getBody();
            assertThat(body.get("code")).isEqualTo("INVALID_ARGUMENT");
        }

        @Test
        @DisplayName("Should include timestamp in response")
        void shouldIncludeTimestamp() {
            IllegalArgumentException ex = new IllegalArgumentException("Invalid");

            ResponseEntity<?> response = handler.handleIllegalArgument(ex);

            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) response.getBody();
            assertThat(body.get("timestamp")).isNotNull();
        }
    }

    @Nested
    @DisplayName("IllegalStateException handling")
    class IllegalStateExceptionTests {

        @Test
        @DisplayName("Should return 409 CONFLICT for IllegalStateException")
        void shouldReturn409ForIllegalState() {
            IllegalStateException ex = new IllegalStateException("Cannot process");

            ResponseEntity<?> response = handler.handleIllegalState(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        }

        @Test
        @DisplayName("Should include error code INVALID_STATE")
        void shouldIncludeErrorCode() {
            IllegalStateException ex = new IllegalStateException("Invalid state");

            ResponseEntity<?> response = handler.handleIllegalState(ex);

            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) response.getBody();
            assertThat(body.get("code")).isEqualTo("INVALID_STATE");
        }
    }

    @Nested
    @DisplayName("ConcurrentModificationException handling")
    class ConcurrentModificationExceptionTests {

        @Test
        @DisplayName("Should return 409 CONFLICT for ConcurrentModificationException")
        void shouldReturn409ForConcurrentModification() {
            ConcurrentModificationException ex = new ConcurrentModificationException("exec-123");

            ResponseEntity<?> response = handler.handleConcurrentModification(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        }

        @Test
        @DisplayName("Should include error code CONCURRENT_MODIFICATION")
        void shouldIncludeErrorCode() {
            ConcurrentModificationException ex = new ConcurrentModificationException("exec-123");

            ResponseEntity<?> response = handler.handleConcurrentModification(ex);

            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) response.getBody();
            assertThat(body.get("code")).isEqualTo("CONCURRENT_MODIFICATION");
        }

        @Test
        @DisplayName("Should include executionId in response")
        void shouldIncludeExecutionId() {
            ConcurrentModificationException ex = new ConcurrentModificationException("exec-123");

            ResponseEntity<?> response = handler.handleConcurrentModification(ex);

            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) response.getBody();
            assertThat(body.get("executionId")).isEqualTo("exec-123");
        }
    }

    @Nested
    @DisplayName("ResourceNotFoundException handling")
    class ResourceNotFoundExceptionTests {

        @Test
        @DisplayName("Should return 404 NOT_FOUND for ResourceNotFoundException")
        void shouldReturn404ForResourceNotFound() {
            ResourceNotFoundException ex = new ResourceNotFoundException("Execution", "exec-999");

            ResponseEntity<?> response = handler.handleResourceNotFound(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("Should include error code RESOURCE_NOT_FOUND")
        void shouldIncludeErrorCode() {
            ResourceNotFoundException ex = new ResourceNotFoundException("Execution", "exec-999");

            ResponseEntity<?> response = handler.handleResourceNotFound(ex);

            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) response.getBody();
            assertThat(body.get("code")).isEqualTo("RESOURCE_NOT_FOUND");
        }
    }

    @Nested
    @DisplayName("Generic Exception handling")
    class GenericExceptionTests {

        @Test
        @DisplayName("Should return 500 INTERNAL_SERVER_ERROR for unexpected exceptions")
        void shouldReturn500ForGenericException() {
            Exception ex = new RuntimeException("Unexpected error");

            ResponseEntity<?> response = handler.handleGenericException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        @Test
        @DisplayName("Should not expose internal error details")
        void shouldNotExposeInternalDetails() {
            Exception ex = new RuntimeException("Database connection failed: password=secret");

            ResponseEntity<?> response = handler.handleGenericException(ex);

            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) response.getBody();
            assertThat(body.get("message").toString()).doesNotContain("password");
            assertThat(body.get("message").toString()).doesNotContain("secret");
        }

        @Test
        @DisplayName("Should include error code INTERNAL_ERROR")
        void shouldIncludeErrorCode() {
            Exception ex = new RuntimeException("Error");

            ResponseEntity<?> response = handler.handleGenericException(ex);

            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) response.getBody();
            assertThat(body.get("code")).isEqualTo("INTERNAL_ERROR");
        }
    }
}
