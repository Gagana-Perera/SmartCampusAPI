package com.smartcampus.api.exception;

/**
 * Thrown when attempting to delete a room that still has sensors.
 */
public class RoomNotEmptyException extends RuntimeException {
    public RoomNotEmptyException(String message) {
        super(message);
    }
}
