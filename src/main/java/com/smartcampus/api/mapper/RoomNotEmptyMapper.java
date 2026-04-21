package com.smartcampus.api.mapper;

import com.smartcampus.api.exception.RoomNotEmptyException;

import javax.ws.rs.core.Context;
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
        LOGGER.log(Level.WARNING, "Conflict at {0}: {1}",
                new Object[]{ErrorResponseFactory.resolvePath(uriInfo), exception.getMessage()});

        return ErrorResponseFactory.build(Response.Status.CONFLICT, exception.getMessage(), uriInfo);
    }
}
