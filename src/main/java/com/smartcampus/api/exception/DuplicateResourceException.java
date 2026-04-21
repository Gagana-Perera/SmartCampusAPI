package com.smartcampus.api.exception;

/**
 * Thrown when a create request uses an identifier that already exists.
 */
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
