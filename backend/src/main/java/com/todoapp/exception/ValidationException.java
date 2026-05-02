package com.todoapp.exception;

/**
 * Thrown when a business rule validation fails that cannot be expressed
 * as a Bean Validation constraint (e.g. past due date detected at service layer,
 * account locked, email not verified).
 * Maps to HTTP 400 Bad Request.
 */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }
}
