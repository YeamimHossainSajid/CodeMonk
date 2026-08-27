package com.codemonk.common.exception;

/**
 * Thrown when a required external dependency or downstream microservice is unreachable or times out (HTTP 503).
 */
public class ServiceUnavailableException extends DomainException {

    public ServiceUnavailableException(String message) {
        super(message);
    }

    public ServiceUnavailableException(String serviceName, String reason) {
        super(String.format("Service '%s' is currently unavailable: %s", serviceName, reason));
    }

    public ServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
