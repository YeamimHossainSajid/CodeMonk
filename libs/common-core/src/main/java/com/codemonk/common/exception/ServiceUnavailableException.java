package com.codemonk.common.exception;


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
