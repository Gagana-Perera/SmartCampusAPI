package com.smartcampus.api.mapper;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Catch-all ExceptionMapper to prevent leaking stack traces.
 */
@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOGGER = Logger.getLogger(GlobalExceptionMapper.class.getName());

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(Throwable exception) {
        LOGGER.log(Level.SEVERE, "Unexpected error at " + ErrorResponseFactory.resolvePath(uriInfo), exception);

        return ErrorResponseFactory.build(
                Response.Status.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred while processing the request.",
                uriInfo
        );
    }
}
