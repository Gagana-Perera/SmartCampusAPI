package com.smartcampus.api.mapper;

import com.smartcampus.api.exception.SensorUnavailableException;
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
 * Mapper for 403 Forbidden.
 */
@Provider
public class SensorUnavailableMapper implements ExceptionMapper<SensorUnavailableException> {

    private static final Logger LOGGER = Logger.getLogger(SensorUnavailableMapper.class.getName());

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(SensorUnavailableException exception) {
        LOGGER.log(Level.WARNING, "Forbidden: {0} at {1}", 
            new Object[]{exception.getMessage(), uriInfo.getPath()});

        ErrorMessage error = new ErrorMessage(
                Response.Status.FORBIDDEN.getStatusCode(),
                "Forbidden",
                exception.getMessage(),
                uriInfo.getPath()
        );

        return Response.status(Response.Status.FORBIDDEN)
                .entity(error)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
