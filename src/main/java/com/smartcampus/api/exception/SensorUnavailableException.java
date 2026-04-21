package com.smartcampus.api.exception;

/**
 * Thrown when a sensor is in MAINTENANCE and cannot accept readings.
 */
public class SensorUnavailableException extends RuntimeException {
    public SensorUnavailableException(String message) {
        super(message);
    }
}
