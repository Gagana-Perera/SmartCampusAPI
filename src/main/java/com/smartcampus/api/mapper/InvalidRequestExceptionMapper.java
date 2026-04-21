package com.smartcampus.api.mapper;

import com.smartcampus.api.exception.InvalidRequestException;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Mapper for request validation failures.
 */
@Provider
public class InvalidRequestExceptionMapper implements ExceptionMapper<InvalidRequestException> {

    private static final Logger LOGGER = Logger.getLogger(InvalidRequestExceptionMapper.class.getName());

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(InvalidRequestException exception) {
        LOGGER.log(Level.INFO, "Invalid request at {0}: {1}",
                new Object[]{ErrorResponseFactory.resolvePath(uriInfo), exception.getMessage()});

        return ErrorResponseFactory.build(Response.Status.BAD_REQUEST, exception.getMessage(), uriInfo);
    }
}
