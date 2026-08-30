package com.codemonk.common.exception;

import com.codemonk.common.constant.TracingConstants;
import com.codemonk.common.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/repositories");
    }

    @Test
    @DisplayName("Should handle ResourceNotFoundException and return 404")
    void shouldHandleResourceNotFoundException() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Repository", "repo-404");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleResourceNotFoundException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(404);
        assertThat(response.getBody().error()).isEqualTo("Not Found");
        assertThat(response.getBody().message()).contains("repo-404");
        assertThat(response.getBody().path()).isEqualTo("/api/v1/repositories");
        assertThat(response.getBody().traceId()).isNotBlank();
    }

    @Test
    @DisplayName("Should handle DuplicateResourceException and return 409")
    void shouldHandleDuplicateResourceException() {
        DuplicateResourceException ex = new DuplicateResourceException("Repository", "url", "https://github.com/org/repo");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleDuplicateResourceException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(409);
        assertThat(response.getBody().error()).isEqualTo("Conflict");
        assertThat(response.getBody().message()).contains("already exists");
    }

    @Test
    @DisplayName("Should handle InvalidRequestException and return 400")
    void shouldHandleInvalidRequestException() {
        InvalidRequestException ex = new InvalidRequestException("Invalid commit SHA format");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleInvalidRequestException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().error()).isEqualTo("Bad Request");
        assertThat(response.getBody().message()).isEqualTo("Invalid commit SHA format");
    }

    @Test
    @DisplayName("Should handle MethodArgumentNotValidException with field errors and return 400")
    void shouldHandleMethodArgumentNotValidException() {
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("registerRepoRequest", "url", "invalid-url", false, null, null, "Must be a valid GitHub URL");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        MethodParameter parameter = mock(MethodParameter.class);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleMethodArgumentNotValidException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().validationErrors()).hasSize(1);
        assertThat(response.getBody().validationErrors().get(0).field()).isEqualTo("url");
        assertThat(response.getBody().validationErrors().get(0).rejectedValue()).isEqualTo("invalid-url");
        assertThat(response.getBody().validationErrors().get(0).message()).isEqualTo("Must be a valid GitHub URL");
    }

    @Test
    @DisplayName("Should handle ConstraintViolationException and return 400")
    void shouldHandleConstraintViolationException() {
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("repositoryUrl");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getInvalidValue()).thenReturn("bad-value");
        when(violation.getMessage()).thenReturn("Invalid format");

        ConstraintViolationException ex = new ConstraintViolationException(Set.of(violation));

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleConstraintViolationException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().validationErrors()).hasSize(1);
        assertThat(response.getBody().validationErrors().get(0).field()).isEqualTo("repositoryUrl");
    }

    @Test
    @DisplayName("Should handle MissingServletRequestParameterException and return 400")
    void shouldHandleMissingServletRequestParameterException() {
        MissingServletRequestParameterException ex = new MissingServletRequestParameterException("branch", "String");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleMissingServletRequestParameterException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).contains("branch");
    }

    @Test
    @DisplayName("Should handle HttpRequestMethodNotSupportedException and return 405")
    void shouldHandleHttpRequestMethodNotSupportedException() {
        HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("DELETE");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleHttpRequestMethodNotSupportedException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(405);
    }

    @Test
    @DisplayName("Should handle ServiceUnavailableException and return 503")
    void shouldHandleServiceUnavailableException() {
        ServiceUnavailableException ex = new ServiceUnavailableException("kafka-broker", "Broker leader unreachable");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleServiceUnavailableException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(503);
        assertThat(response.getBody().message()).contains("kafka-broker");
    }

    @Test
    @DisplayName("Should handle unexpected Exception and return sanitized 500")
    void shouldHandleUnexpectedException() {
        Exception ex = new RuntimeException("Database connection timeout at secret_db_host:5432");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleUnhandledException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(500);
        assertThat(response.getBody().message()).doesNotContain("secret_db_host");
        assertThat(response.getBody().message()).contains("An unexpected internal error occurred");
        assertThat(response.getBody().traceId()).isNotBlank();
    }

    @Test
    @DisplayName("Should extract trace ID from X-Trace-Id request header when present")
    void shouldExtractTraceIdFromHeader() {
        when(request.getHeader(TracingConstants.HEADER_TRACE_ID)).thenReturn("custom-trace-abc-123");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleResourceNotFoundException(
                new ResourceNotFoundException("Test", "1"),
                request
        );

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().traceId()).isEqualTo("custom-trace-abc-123");
    }

    @Test
    @DisplayName("Should extract correlation ID from X-Correlation-Id when X-Trace-Id is absent")
    void shouldExtractCorrelationIdWhenTraceIdAbsent() {
        when(request.getHeader(TracingConstants.HEADER_TRACE_ID)).thenReturn(null);
        when(request.getHeader(TracingConstants.HEADER_CORRELATION_ID)).thenReturn("corr-id-xyz-789");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleResourceNotFoundException(
                new ResourceNotFoundException("Test", "1"),
                request
        );

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().traceId()).isEqualTo("corr-id-xyz-789");
    }

    @Test
    @DisplayName("Should extract trace ID from MDC when headers are absent")
    void shouldExtractTraceIdFromMdcWhenHeadersAbsent() {
        when(request.getHeader(TracingConstants.HEADER_TRACE_ID)).thenReturn(null);
        when(request.getHeader(TracingConstants.HEADER_CORRELATION_ID)).thenReturn(null);

        try {
            MDC.put(TracingConstants.MDC_TRACE_ID, "mdc-trace-999");

            ResponseEntity<ErrorResponse> response = exceptionHandler.handleResourceNotFoundException(
                    new ResourceNotFoundException("Test", "1"),
                    request
            );

            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().traceId()).isEqualTo("mdc-trace-999");
        } finally {
            MDC.clear();
        }
    }
}
