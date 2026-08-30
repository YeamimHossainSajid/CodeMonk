package com.codemonk.common.exception;

public class ForbiddenException extends DomainException{

    public ForbiddenException(String message) {
        super(message);
    }

    public ForbiddenException(String message, Throwable cause) {
        super(message, cause);
    }
}
