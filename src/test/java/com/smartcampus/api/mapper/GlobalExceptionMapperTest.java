package com.smartcampus.api.mapper;

import com.smartcampus.api.model.ErrorResponse;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionMapperTest {

    @Test
    void mapsUnexpectedExceptionsToInternalServerError() {
        GlobalExceptionMapper mapper = new GlobalExceptionMapper();

        Response response = mapper.toResponse(new RuntimeException("boom"));

        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        assertNotNull(response.getEntity());

        ErrorResponse errorResponse = (ErrorResponse) response.getEntity();
        assertEquals("Internal Server Error", errorResponse.getError());
        assertEquals("An unexpected error occurred while processing the request.", errorResponse.getMessage());
    }
}
