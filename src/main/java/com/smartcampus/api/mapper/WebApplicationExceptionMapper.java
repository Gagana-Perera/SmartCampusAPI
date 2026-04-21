package com.smartcampus.api.mapper;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Mapper for all JAX-RS WebApplicationException subclasses (400, 404, 405, 409, 415 etc).
 */
@Provider
public class WebApplicationExceptionMapper implements ExceptionMapper<WebApplicationException> {

    private static final Logger LOGGER = Logger.getLogger(WebApplicationExceptionMapper.class.getName());

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(WebApplicationException exception) {
        Response response = exception.getResponse();
        int status = response == null ? Response.Status.INTERNAL_SERVER_ERROR.getStatusCode() : response.getStatus();
        String error = response == null ? "Internal Server Error" : response.getStatusInfo().getReasonPhrase();
        String message = exception.getMessage();

        if (message == null || message.trim().isEmpty()) {
            message = error;
        }

        LOGGER.log(Level.INFO, "Web application exception at {0}: {1}",
                new Object[]{ErrorResponseFactory.resolvePath(uriInfo), message});

        return ErrorResponseFactory.build(status, error, message, uriInfo);
    }
}
