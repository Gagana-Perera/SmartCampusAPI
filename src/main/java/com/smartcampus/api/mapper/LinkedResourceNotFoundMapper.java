package com.smartcampus.api.mapper;

import com.smartcampus.api.exception.LinkedResourceNotFoundException;
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
 * Mapper for 422 Unprocessable Entity.
 */
@Provider
public class LinkedResourceNotFoundMapper implements ExceptionMapper<LinkedResourceNotFoundException> {

    private static final Logger LOGGER = Logger.getLogger(LinkedResourceNotFoundMapper.class.getName());

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(LinkedResourceNotFoundException exception) {
        LOGGER.log(Level.WARNING, "Unprocessable Entity: {0} at {1}", 
            new Object[]{exception.getMessage(), uriInfo.getPath()});

        ErrorMessage error = new ErrorMessage(
                422, // Unprocessable Entity
                "Unprocessable Entity",
                exception.getMessage(),
                uriInfo.getPath()
        );

        return Response.status(422) // Semantically more accurate than 404
                .entity(error)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
