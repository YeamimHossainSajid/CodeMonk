package com.codemonk.common.exception;

/**
 * Thrown when attempting to create a resource that already exists (HTTP 409 Conflict).
 */
public class DuplicateResourceException extends DomainException {

    public DuplicateResourceException(String message) {
        super(message);
    }

    public DuplicateResourceException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s with %s '%s' already exists", resourceName, fieldName, fieldValue));
    }
}
