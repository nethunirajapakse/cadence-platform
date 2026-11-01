package com.cadence.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

// A business-rule failure with an explicit HTTP status. GlobalExceptionHandler
// turns it into a plain-text response body, matching the existing 404/409 style.
@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}
