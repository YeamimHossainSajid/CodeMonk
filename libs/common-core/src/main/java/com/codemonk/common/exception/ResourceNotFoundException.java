package com.codemonk.common.exception;

/**
 * Thrown when a requested resource (e.g. Repository, CodeEntity, IndexJob) cannot be found.
 */
public class ResourceNotFoundException extends DomainException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceName, String identifier) {
        super(String.format("%s with identifier '%s' was not found", resourceName, identifier));
    }

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s with %s '%s' was not found", resourceName, fieldName, fieldValue));
    }
}
