package com.smartcampus.api.resource;

import com.smartcampus.api.exception.InvalidRequestException;
import com.smartcampus.api.exception.ResourceNotFoundException;
import com.smartcampus.api.exception.SensorUnavailableException;
import com.smartcampus.api.model.Sensor;
import com.smartcampus.api.model.SensorReading;
import com.smartcampus.api.repository.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;

/**
 * Sub-resource for /api/v1/sensors/{sensorId}/readings.
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final String sensorId;

    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    @GET
    public List<SensorReading> getReadingHistory() {
        requireSensor();
        return DataStore.getReadingsForSensor(sensorId);
    }

    @POST
    public Response addReading(SensorReading reading) {
        Sensor sensor = requireSensor();

        // Business Rule: Check status
        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException("Sensor " + sensorId + " is currently in MAINTENANCE mode and cannot accept new readings.");
        }

        SensorReading normalizedReading = normalizeReading(reading);
        DataStore.addReading(sensorId, normalizedReading);

        return Response.status(Response.Status.CREATED).entity(normalizedReading).build();
    }

    private Sensor requireSensor() {
        Sensor sensor = DataStore.getSensor(sensorId);
        if (sensor == null) {
            throw new ResourceNotFoundException("Sensor with ID " + sensorId + " not found");
        }
        return sensor;
    }

    private SensorReading normalizeReading(SensorReading reading) {
        if (reading == null) {
            throw new InvalidRequestException("Request body is required.");
        }
        if (reading.getTimestamp() < 0) {
            throw new InvalidRequestException("Reading timestamp must not be negative.");
        }

        SensorReading normalizedReading = new SensorReading();
        normalizedReading.setId(isBlank(reading.getId()) ? UUID.randomUUID().toString() : reading.getId().trim());
        normalizedReading.setTimestamp(reading.getTimestamp() > 0 ? reading.getTimestamp() : System.currentTimeMillis());
        normalizedReading.setValue(reading.getValue());
        return normalizedReading;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
