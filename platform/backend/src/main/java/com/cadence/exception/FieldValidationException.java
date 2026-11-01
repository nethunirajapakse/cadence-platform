package com.cadence.exception;

import lombok.Getter;

// Same response shape as bean-validation failures ({ field: message }), so the
// frontend can show it inline next to the field with no special handling.
@Getter
public class FieldValidationException extends RuntimeException {

    private final String field;

    public FieldValidationException(String field, String message) {
        super(message);
        this.field = field;
    }
}
