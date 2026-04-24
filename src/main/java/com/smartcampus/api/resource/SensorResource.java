package com.smartcampus.api.resource;

import com.smartcampus.api.exception.DuplicateResourceException;
import com.smartcampus.api.exception.InvalidRequestException;
import com.smartcampus.api.exception.LinkedResourceNotFoundException;
import com.smartcampus.api.exception.ResourceNotFoundException;
import com.smartcampus.api.model.Sensor;
import com.smartcampus.api.repository.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;

/**
 * Resource for /api/v1/sensors path.
 */
@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    @Context
    private UriInfo uriInfo;

    /**
     * Retrieves sensors, optionally filtered by type.
     * 
     * @param type Optional query parameter to filter by sensor type (e.g., "Temperature").
     * @return 200 OK with list of matching Sensor objects.
     */
    @GET
    public List<Sensor> getSensors(@QueryParam("type") String type) {
        String normalizedType = normalize(type);
        if (normalizedType != null) {
            return DataStore.getSensorsByType(normalizedType);
        }
        return DataStore.getAllSensors();
    }

    /**
     * Creates a new sensor and links it to a room.
     * 
     * @param sensor The sensor object to create.
     * @return 201 Created with the new sensor.
     * @throws DuplicateResourceException 409 Conflict if sensor ID already exists.
     * @throws LinkedResourceNotFoundException 422 Unprocessable Entity if the specified roomId doesn't exist.
     * @throws InvalidRequestException 400 Bad Request if validation fails.
     */
    @POST
    public Response createSensor(Sensor sensor) {
        Sensor normalizedSensor = validateAndNormalize(sensor);

        if (DataStore.sensorExists(normalizedSensor.getId())) {
            throw new DuplicateResourceException("Sensor with ID " + normalizedSensor.getId() + " already exists.");
        }

        // Business Rule: Verify Room existence
        if (!DataStore.roomExists(normalizedSensor.getRoomId())) {
            throw new LinkedResourceNotFoundException(
                    "Cannot create sensor. Room with ID " + normalizedSensor.getRoomId() + " does not exist."
            );
        }

        DataStore.addSensor(normalizedSensor);
        URI location = uriInfo.getAbsolutePathBuilder().path(normalizedSensor.getId()).build();
        return Response.created(location).entity(normalizedSensor).build();
    }

    /**
     * Retrieves a specific sensor by ID.
     * 
     * @param sensorId The unique ID of the sensor.
     * @return 200 OK with the Sensor object.
     * @throws ResourceNotFoundException 404 Not Found if sensor doesn't exist.
     */
    @GET
    @Path("/{sensorId}")
    public Sensor getSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = DataStore.getSensor(sensorId);
        if (sensor == null) {
            throw new ResourceNotFoundException("Sensor with ID " + sensorId + " not found");
        }
        return sensor;
    }

    /**
     * Sub-resource locator for readings history.
     * This delegates requests for nested reading resources to SensorReadingResource.
     * 
     * @param sensorId The parent sensor ID.
     * @return A new instance of SensorReadingResource.
     * @throws ResourceNotFoundException 404 Not Found if parent sensor doesn't exist.
     */
    @Path("/{sensorId}/readings")
    public SensorReadingResource getReadingsResource(@PathParam("sensorId") String sensorId) {
        Sensor sensor = DataStore.getSensor(sensorId);
        if (sensor == null) {
            throw new ResourceNotFoundException("Sensor with ID " + sensorId + " not found");
        }
        return new SensorReadingResource(sensorId);
    }

    private Sensor validateAndNormalize(Sensor sensor) {
        if (sensor == null) {
            throw new InvalidRequestException("Request body is required.");
        }

        String id = normalize(sensor.getId());
        String type = normalize(sensor.getType());
        String status = normalize(sensor.getStatus());
        String roomId = normalize(sensor.getRoomId());

        if (id == null) {
            throw new InvalidRequestException("Sensor id must not be blank.");
        }
        if (type == null) {
            throw new InvalidRequestException("Sensor type must not be blank.");
        }
        if (status == null) {
            throw new InvalidRequestException("Sensor status must not be blank.");
        }
        if (roomId == null) {
            throw new InvalidRequestException("Sensor roomId must not be blank.");
        }

        Sensor normalizedSensor = new Sensor(id, type, status, roomId);
        normalizedSensor.setCurrentValue(sensor.getCurrentValue());
        return normalizedSensor;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
