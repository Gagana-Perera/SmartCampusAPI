package com.smartcampus.api.mapper;

import com.smartcampus.api.model.ErrorMessage;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
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
        int status = response.getStatus();
        
        LOGGER.log(Level.INFO, "API Client Error [{0}]: {1} at {2}", 
            new Object[]{status, exception.getMessage(), uriInfo.getPath()});

        ErrorMessage error = new ErrorMessage(
                status,
                response.getStatusInfo().getReasonPhrase(),
                exception.getMessage(),
                uriInfo.getPath()
        );

        return Response.fromResponse(response)
                .entity(error)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
