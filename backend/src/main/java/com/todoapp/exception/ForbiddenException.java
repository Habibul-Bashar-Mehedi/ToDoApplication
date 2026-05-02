package com.todoapp.exception;

/**
 * Thrown when the authenticated user does not have the required permission
 * to perform the requested operation on a resource.
 * Maps to HTTP 403 Forbidden.
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
