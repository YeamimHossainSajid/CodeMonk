package com.codemonk.common.exception;

import com.codemonk.common.dto.ErrorResponse;
import com.codemonk.common.dto.ValidationErrorDetail;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.UUID;

@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String HEADER_TRACE_ID = "X-Trace-Id";
    private static final String HEADER_CORRELATION_ID = "X-Correlation-Id";
    private static final String MDC_TRACE_ID = "traceId";

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex, HttpServletRequest request) {
        String traceId = resolveTraceId(request);
        log.warn("[TraceId: {}] Resource not found on path {}: {}", traceId, request.getRequestURI(), ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.of(HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND.getReasonPhrase(),
                ex.getMessage(), request.getRequestURI(), traceId);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResourceException(DuplicateResourceException ex, HttpServletRequest request) {
        String traceId = resolveTraceId(request);
        log.warn("[TraceId: {}] Duplicate resource conflict on path {}: {}", traceId, request.getRequestURI(), ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.of(HttpStatus.CONFLICT.value(), HttpStatus.CONFLICT.getReasonPhrase(),
                ex.getMessage(), request.getRequestURI(), traceId);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler({InvalidRequestException.class, DomainException.class, IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ErrorResponse> handleInvalidRequestException(Exception ex, HttpServletRequest request) {
        String traceId = resolveTraceId(request);
        log.warn("[TraceId: {}] Invalid request on path {}: {}", traceId, request.getRequestURI(), ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ex.getMessage(), request.getRequestURI(), traceId);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String traceId = resolveTraceId(request);
        log.warn("[TraceId: {}] Validation failed for request body on path {}", traceId, request.getRequestURI());

        List<ValidationErrorDetail> validationErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> ValidationErrorDetail.of(fe.getField(), fe.getRejectedValue(),
                        fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value")).toList();

        String message = String.format("Validation failed for %d field(s)", validationErrors.size());

        ErrorResponse errorResponse = ErrorResponse.withValidation(HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(), message, request.getRequestURI(), validationErrors, traceId);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException ex, HttpServletRequest request) {
        String traceId = resolveTraceId(request);
        log.warn("[TraceId: {}] Constraint violation on path {}", traceId, request.getRequestURI());

        List<ValidationErrorDetail> validationErrors = ex.getConstraintViolations().stream()
                .map(cv -> ValidationErrorDetail.of(cv.getPropertyPath() != null ?
                        cv.getPropertyPath().toString() : "unknown", cv.getInvalidValue(), cv.getMessage())).toList();

        String message = String.format("Constraint validation failed for %d field(s)", validationErrors.size());

        ErrorResponse errorResponse = ErrorResponse.withValidation(HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(), message, request.getRequestURI(), validationErrors, traceId);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestParameterException(MissingServletRequestParameterException ex,
                                                                                       HttpServletRequest request) {
        String traceId = resolveTraceId(request);
        log.warn("[TraceId: {}] Missing required request parameter on path {}: {}", traceId, request.getRequestURI(), ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.of(HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(), String.format("Required request parameter '%s' is missing",
                        ex.getParameterName()), request.getRequestURI(), traceId);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException ex,
                                                                                      HttpServletRequest request) {
        String traceId = resolveTraceId(request);
        log.warn("[TraceId: {}] HTTP method {} not supported for path {}", traceId, ex.getMethod(), request.getRequestURI());

        ErrorResponse errorResponse = ErrorResponse.of(HttpStatus.METHOD_NOT_ALLOWED.value(),
                HttpStatus.METHOD_NOT_ALLOWED.getReasonPhrase(), ex.getMessage(), request.getRequestURI(), traceId);
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(errorResponse);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException ex,
                                                                                  HttpServletRequest request) {
        String traceId = resolveTraceId(request);
        log.warn("[TraceId: {}] Content type not supported on path {}: {}", traceId, request.getRequestURI(), ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.of(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(),
                HttpStatus.UNSUPPORTED_MEDIA_TYPE.getReasonPhrase(), ex.getMessage(), request.getRequestURI(), traceId);
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(errorResponse);
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleServiceUnavailableException(ServiceUnavailableException ex, HttpServletRequest request) {
        String traceId = resolveTraceId(request);
        log.error("[TraceId: {}] Downstream service unavailable on path {}: {}", traceId, request.getRequestURI(), ex.getMessage(), ex);

        ErrorResponse errorResponse = ErrorResponse.of(HttpStatus.SERVICE_UNAVAILABLE.value(),
                HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase(), ex.getMessage(), request.getRequestURI(), traceId);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnhandledException(Exception ex, HttpServletRequest request) {
        String traceId = resolveTraceId(request);
        log.error("[TraceId: {}] Unhandled internal server error on path {}: {}", traceId, request.getRequestURI(), ex.getMessage(), ex);

        ErrorResponse errorResponse = ErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "An unexpected internal error occurred. Please contact support with the provided trace ID.", request.getRequestURI(), traceId);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Resolves the trace ID from the request headers, MDC context, or generates a fallback UUID.
     */
    protected String resolveTraceId(HttpServletRequest request) {
        if (request != null) {
            String traceId = request.getHeader(HEADER_TRACE_ID);
            if (traceId != null && !traceId.isBlank()) {
                return traceId;
            }
            String correlationId = request.getHeader(HEADER_CORRELATION_ID);
            if (correlationId != null && !correlationId.isBlank()) {
                return correlationId;
            }
        }

        String mdcTraceId = MDC.get(MDC_TRACE_ID);
        if (mdcTraceId != null && !mdcTraceId.isBlank()) {
            return mdcTraceId;
        }

        return UUID.randomUUID().toString();
    }
}
