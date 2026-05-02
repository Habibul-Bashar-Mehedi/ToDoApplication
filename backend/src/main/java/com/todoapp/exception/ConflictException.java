package com.todoapp.exception;

/**
 * Thrown when a create or update operation would violate a uniqueness constraint,
 * such as a duplicate email address or duplicate module name per owner.
 * Maps to HTTP 409 Conflict.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
