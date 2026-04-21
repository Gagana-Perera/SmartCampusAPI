package com.smartcampus.api.resource;

import com.smartcampus.api.exception.LinkedResourceNotFoundException;
import com.smartcampus.api.exception.ResourceNotFoundException;
import com.smartcampus.api.model.Sensor;
import com.smartcampus.api.repository.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * Resource for /api/v1/sensors path.
 */
@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    @GET
    public List<Sensor> getSensors(@QueryParam("type") String type) {
        if (type != null && !type.isEmpty()) {
            return DataStore.getSensorsByType(type);
        }
        return DataStore.getAllSensors();
    }

    @POST
    public Response createSensor(Sensor sensor) {
        if (sensor.getId() == null || sensor.getId().trim().isEmpty()) {
            throw new BadRequestException("Sensor ID is required");
        }
        if (DataStore.sensorExists(sensor.getId())) {
            throw new ClientErrorException("Sensor with ID " + sensor.getId() + " already exists", Response.Status.CONFLICT);
        }
        
        // Business Rule: Verify Room existence
        if (!DataStore.roomExists(sensor.getRoomId())) {
            throw new LinkedResourceNotFoundException("Cannot create sensor. Room with ID " + sensor.getRoomId() + " does not exist.");
        }

        DataStore.addSensor(sensor);
        return Response.status(Response.Status.CREATED).entity(sensor).build();
    }

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
     */
    @Path("/{sensorId}/readings")
    public SensorReadingResource getReadingsResource(@PathParam("sensorId") String sensorId) {
        Sensor sensor = DataStore.getSensor(sensorId);
        if (sensor == null) {
            throw new ResourceNotFoundException("Sensor with ID " + sensorId + " not found");
        }
        return new SensorReadingResource(sensorId);
    }
}
