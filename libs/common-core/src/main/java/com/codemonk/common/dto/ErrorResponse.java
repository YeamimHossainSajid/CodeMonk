package com.codemonk.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * Standardized API error payload for all CodeMonk microservices.
 *
 * @param status           HTTP status code (e.g. 400, 404, 500)
 * @param error            HTTP status reason phrase (e.g. "Bad Request", "Not Found")
 * @param message          Human-readable description of the error
 * @param path             The requested URI path where the error occurred
 * @param timestamp        UTC timestamp when the error occurred
 * @param validationErrors Optional list of field-level validation errors
 * @param traceId          Distributed trace/correlation identifier for cross-service observability
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        int status,
        String error,
        String message,
        String path,
        Instant timestamp,
        List<ValidationErrorDetail> validationErrors,
        String traceId
) {
    public ErrorResponse {
        if (validationErrors != null) {
            validationErrors = Collections.unmodifiableList(validationErrors);
        }
    }

    /**
     * Creates a simple ErrorResponse without validation errors or traceId.
     */
    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(status, error, message, path, Instant.now(), null, null);
    }

    /**
     * Creates an ErrorResponse with a distributed trace ID.
     */
    public static ErrorResponse of(int status, String error, String message, String path, String traceId) {
        return new ErrorResponse(status, error, message, path, Instant.now(), null, traceId);
    }

    /**
     * Creates an ErrorResponse containing field-level validation failures.
     */
    public static ErrorResponse withValidation(
            int status,
            String error,
            String message,
            String path,
            List<ValidationErrorDetail> validationErrors,
            String traceId
    ) {
        return new ErrorResponse(status, error, message, path, Instant.now(), validationErrors, traceId);
    }
}
