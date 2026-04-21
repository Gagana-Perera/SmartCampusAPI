package com.smartcampus.api.mapper;

import com.smartcampus.api.exception.ResourceNotFoundException;

import javax.ws.rs.core.Context;
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
        LOGGER.log(Level.INFO, "Not found at {0}: {1}",
                new Object[]{ErrorResponseFactory.resolvePath(uriInfo), exception.getMessage()});

        return ErrorResponseFactory.build(Response.Status.NOT_FOUND, exception.getMessage(), uriInfo);
    }
}
