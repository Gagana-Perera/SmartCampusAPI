package com.smartcampus.api.exception;

/**
 * Thrown when a foreign key link fails (e.g. roomId doesn't exist when creating a sensor).
 */
public class LinkedResourceNotFoundException extends RuntimeException {
    public LinkedResourceNotFoundException(String message) {
        super(message);
    }
}
