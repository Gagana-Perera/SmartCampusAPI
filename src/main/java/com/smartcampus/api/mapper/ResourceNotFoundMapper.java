package com.smartcampus.api.mapper;

import com.smartcampus.api.exception.ResourceNotFoundException;
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
 * Mapper for 404 Not Found.
 */
@Provider
public class ResourceNotFoundMapper implements ExceptionMapper<ResourceNotFoundException> {

    private static final Logger LOGGER = Logger.getLogger(ResourceNotFoundMapper.class.getName());

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(ResourceNotFoundException exception) {
        LOGGER.log(Level.INFO, "Not Found: {0} at {1}", 
            new Object[]{exception.getMessage(), uriInfo.getPath()});

        ErrorMessage error = new ErrorMessage(
                Response.Status.NOT_FOUND.getStatusCode(),
                "Not Found",
                exception.getMessage(),
                uriInfo.getPath()
        );

        return Response.status(Response.Status.NOT_FOUND)
                .entity(error)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
