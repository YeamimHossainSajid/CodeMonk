package com.codemonk.common.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorResponseTest {

    @Test
    @DisplayName("Should create basic ErrorResponse using of() factory")
    void shouldCreateBasicErrorResponse() {
        ErrorResponse response = ErrorResponse.of(404, "Not Found", "Repository not found", "/api/v1/repositories/123");

        assertThat(response.status()).isEqualTo(404);
        assertThat(response.error()).isEqualTo("Not Found");
        assertThat(response.message()).isEqualTo("Repository not found");
        assertThat(response.path()).isEqualTo("/api/v1/repositories/123");
        assertThat(response.timestamp()).isNotNull();
        assertThat(response.timestamp()).isBeforeOrEqualTo(Instant.now());
        assertThat(response.validationErrors()).isNull();
        assertThat(response.traceId()).isNull();
    }

    @Test
    @DisplayName("Should create ErrorResponse with traceId")
    void shouldCreateErrorResponseWithTraceId() {
        String traceId = "test-trace-1234";
        ErrorResponse response = ErrorResponse.of(500, "Internal Server Error", "Something went wrong", "/api/v1/test", traceId);

        assertThat(response.status()).isEqualTo(500);
        assertThat(response.traceId()).isEqualTo(traceId);
        assertThat(response.validationErrors()).isNull();
    }

    @Test
    @DisplayName("Should create ErrorResponse with validation errors")
    void shouldCreateErrorResponseWithValidationErrors() {
        List<ValidationErrorDetail> validationErrors = List.of(
                ValidationErrorDetail.of("url", "invalid-url", "Must be a valid GitHub URL"),
                ValidationErrorDetail.of("branch", "Branch cannot be empty")
        );

        ErrorResponse response = ErrorResponse.withValidation(
                400,
                "Bad Request",
                "Validation failed for 2 field(s)",
                "/api/v1/repositories",
                validationErrors,
                "trace-5678"
        );

        assertThat(response.status()).isEqualTo(400);
        assertThat(response.error()).isEqualTo("Bad Request");
        assertThat(response.validationErrors()).hasSize(2);
        assertThat(response.validationErrors().get(0).field()).isEqualTo("url");
        assertThat(response.validationErrors().get(0).rejectedValue()).isEqualTo("invalid-url");
        assertThat(response.validationErrors().get(1).rejectedValue()).isNull();
        assertThat(response.traceId()).isEqualTo("trace-5678");
    }
}
