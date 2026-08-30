package com.codemonk.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Represents a specific field-level validation failure in a REST request.
 *
 * @param field         the name of the invalid request property/field
 * @param rejectedValue the invalid value provided by the client (sanitized)
 * @param message       the reason why validation failed
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ValidationErrorDetail(
        String field,
        Object rejectedValue,
        String message
) {
    public static ValidationErrorDetail of(String field, Object rejectedValue, String message) {
        return new ValidationErrorDetail(field, rejectedValue, message);
    }

    public static ValidationErrorDetail of(String field, String message) {
        return new ValidationErrorDetail(field, null, message);
    }
}
