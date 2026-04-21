package com.smartcampus.api.mapper;

import com.smartcampus.api.exception.RoomNotEmptyException;
import com.smartcampus.api.model.ErrorMessage;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Mapper for 409 Conflict.
 */
@Provider
public class RoomNotEmptyMapper implements ExceptionMapper<RoomNotEmptyException> {
    
    private static final Logger LOGGER = Logger.getLogger(RoomNotEmptyMapper.class.getName());

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(RoomNotEmptyException exception) {
        LOGGER.log(Level.WARNING, "Conflict: {0} at {1}", 
            new Object[]{exception.getMessage(), uriInfo.getPath()});

        ErrorMessage error = new ErrorMessage(
                Response.Status.CONFLICT.getStatusCode(),
                "Conflict",
                exception.getMessage(),
                uriInfo.getPath()
        );

        return Response.status(Response.Status.CONFLICT)
                .entity(error)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
