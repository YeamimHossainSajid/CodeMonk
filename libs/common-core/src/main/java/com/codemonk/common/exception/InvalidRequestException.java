package com.codemonk.common.exception;

/**
 * Thrown when a business logic or domain validation constraint is violated (HTTP 400 Bad Request).
 */
public class InvalidRequestException extends DomainException {

    public InvalidRequestException(String message) {
        super(message);
    }

    public InvalidRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
