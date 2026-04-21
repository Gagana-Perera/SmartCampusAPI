package com.smartcampus.api.mapper;

import com.smartcampus.api.exception.DuplicateResourceException;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Mapper for duplicate resource creation attempts.
 */
@Provider
public class DuplicateResourceExceptionMapper implements ExceptionMapper<DuplicateResourceException> {

    private static final Logger LOGGER = Logger.getLogger(DuplicateResourceExceptionMapper.class.getName());

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(DuplicateResourceException exception) {
        LOGGER.log(Level.WARNING, "Duplicate resource at {0}: {1}",
                new Object[]{ErrorResponseFactory.resolvePath(uriInfo), exception.getMessage()});

        return ErrorResponseFactory.build(Response.Status.CONFLICT, exception.getMessage(), uriInfo);
    }
}
