package com.smartcampus.api.resource;

import com.smartcampus.api.exception.DuplicateResourceException;
import com.smartcampus.api.exception.InvalidRequestException;
import com.smartcampus.api.exception.ResourceNotFoundException;
import com.smartcampus.api.exception.RoomNotEmptyException;
import com.smartcampus.api.model.Room;
import com.smartcampus.api.repository.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;

/**
 * Resource for /api/v1/rooms path.
 */
@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {

    @Context
    private UriInfo uriInfo;

    /**
     * Retrieves all rooms in the campus.
     * 
     * @return 200 OK with list of Room objects.
     */
    @GET
    public List<Room> getAllRooms() {
        return DataStore.getAllRooms();
    }

    /**
     * Creates a new room.
     * 
     * @param room The room object to create.
     * @return 201 Created with the new room and Location header.
     * @throws DuplicateResourceException 409 Conflict if room ID already exists.
     * @throws InvalidRequestException 400 Bad Request if validation fails.
     */
    @POST
    public Response createRoom(Room room) {
        Room normalizedRoom = validateAndNormalize(room);

        if (DataStore.roomExists(normalizedRoom.getId())) {
            throw new DuplicateResourceException("Room with ID " + normalizedRoom.getId() + " already exists.");
        }

        DataStore.addRoom(normalizedRoom);
        URI location = uriInfo.getAbsolutePathBuilder().path(normalizedRoom.getId()).build();
        return Response.created(location).entity(normalizedRoom).build();
    }

    /**
     * Retrieves a specific room by ID.
     * 
     * @param roomId The unique ID of the room.
     * @return 200 OK with the Room object.
     * @throws ResourceNotFoundException 404 Not Found if room doesn't exist.
     */
    @GET
    @Path("/{roomId}")
    public Room getRoom(@PathParam("roomId") String roomId) {
        Room room = DataStore.getRoom(roomId);
        if (room == null) {
            throw new ResourceNotFoundException("Room with ID " + roomId + " not found");
        }
        return room;
    }

    /**
     * Deletes a room by ID.
     * Operation is idempotent; deleting a non-existent room returns success.
     * 
     * @param roomId The unique ID of the room.
     * @return 204 No Content on success.
     * @throws RoomNotEmptyException 409 Conflict if sensors are still assigned to the room.
     */
    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = DataStore.getRoom(roomId);

        // Idempotent: If it doesn't exist, success
        if (room == null) {
            return Response.noContent().build();
        }

        // Business Logic: Block if sensors are assigned
        if (DataStore.roomHasSensors(roomId)) {
            throw new RoomNotEmptyException("Room " + roomId + " cannot be deleted because it still has assigned sensors.");
        }

        DataStore.deleteRoom(roomId);
        return Response.noContent().build();
    }

    private Room validateAndNormalize(Room room) {
        if (room == null) {
            throw new InvalidRequestException("Request body is required.");
        }

        String id = normalize(room.getId());
        String name = normalize(room.getName());

        if (id == null) {
            throw new InvalidRequestException("Room id must not be null or blank.");
        }
        if (name == null) {
            throw new InvalidRequestException("Room name must not be null or blank.");
        }
        if (room.getCapacity() <= 0) {
            throw new InvalidRequestException("Room capacity must be greater than 0.");
        }

        return new Room(id, name, room.getCapacity());
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
