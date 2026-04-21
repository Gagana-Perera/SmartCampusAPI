package com.smartcampus.api.resource;

import com.smartcampus.api.exception.ResourceNotFoundException;
import com.smartcampus.api.exception.RoomNotEmptyException;
import com.smartcampus.api.model.Room;
import com.smartcampus.api.repository.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * Resource for /api/v1/rooms path.
 */
@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {

    @GET
    public List<Room> getAllRooms() {
        return DataStore.getAllRooms();
    }

    @POST
    public Response createRoom(Room room) {
        if (room.getId() == null || room.getId().trim().isEmpty()) {
            throw new BadRequestException("Room ID is required");
        }
        if (DataStore.roomExists(room.getId())) {
            throw new ClientErrorException("Room with ID " + room.getId() + " already exists", Response.Status.CONFLICT);
        }
        if (room.getCapacity() <= 0) {
            throw new BadRequestException("Capacity must be positive");
        }

        DataStore.addRoom(room);
        return Response.status(Response.Status.CREATED).entity(room).build();
    }

    @GET
    @Path("/{roomId}")
    public Room getRoom(@PathParam("roomId") String roomId) {
        Room room = DataStore.getRoom(roomId);
        if (room == null) {
            throw new ResourceNotFoundException("Room with ID " + roomId + " not found");
        }
        return room;
    }

    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = DataStore.getRoom(roomId);
        
        // Idempotent: If it doesn't exist, success
        if (room == null) {
            return Response.noContent().build();
        }

        // Business Logic: Block if sensors are assigned
        if (!room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException("Room " + roomId + " cannot be deleted because it still has assigned sensors.");
        }

        DataStore.deleteRoom(roomId);
        return Response.noContent().build();
    }
}
