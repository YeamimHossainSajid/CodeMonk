package com.codemonk.common.exception;

public class ResourceNotFoundException extends DomainException {

    public ResourceNotFoundException(String resourceName, String identifier) {
        super(String.format("%s with identifier '%s' was not found", resourceName, identifier));
    }
}
