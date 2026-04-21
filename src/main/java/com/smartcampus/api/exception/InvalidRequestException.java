package com.smartcampus.api.exception;

/**
 * Thrown when the request payload is missing or fails validation.
 */
public class InvalidRequestException extends RuntimeException {
    public InvalidRequestException(String message) {
        super(message);
    }
}
