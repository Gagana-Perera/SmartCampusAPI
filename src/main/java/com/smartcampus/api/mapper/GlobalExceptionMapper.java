package com.smartcampus.api.mapper;

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
 * Catch-all ExceptionMapper to prevent leaking stack traces.
 */
@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOGGER = Logger.getLogger(GlobalExceptionMapper.class.getName());

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(Throwable exception) {
        // Log the full exception with context for internal debugging
        LOGGER.log(Level.SEVERE, "Unexpected error at {0}: {1}", 
            new Object[]{uriInfo.getPath(), exception.getMessage()});
        exception.printStackTrace(); // Optional: log to server console

        ErrorMessage error = new ErrorMessage(
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                "Internal Server Error",
                "An unexpected error occurred. Please contact the administrator.",
                uriInfo.getPath()
        );

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(error)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
