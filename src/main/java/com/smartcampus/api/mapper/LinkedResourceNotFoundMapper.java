package com.smartcampus.api.mapper;

import com.smartcampus.api.exception.LinkedResourceNotFoundException;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Mapper for 422 Unprocessable Entity.
 */
@Provider
public class LinkedResourceNotFoundMapper implements ExceptionMapper<LinkedResourceNotFoundException> {

    private static final Logger LOGGER = Logger.getLogger(LinkedResourceNotFoundMapper.class.getName());

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(LinkedResourceNotFoundException exception) {
        LOGGER.log(Level.WARNING, "Unprocessable entity at {0}: {1}",
                new Object[]{ErrorResponseFactory.resolvePath(uriInfo), exception.getMessage()});

        return ErrorResponseFactory.build(422, "Unprocessable Entity", exception.getMessage(), uriInfo);
    }
}
