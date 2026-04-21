package com.smartcampus.api.mapper;

import com.smartcampus.api.model.ErrorResponse;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * Utility methods to build consistent JSON error responses.
 */
final class ErrorResponseFactory {

    private ErrorResponseFactory() {
    }

    static Response build(Response.StatusType status, String message, UriInfo uriInfo) {
        return build(status.getStatusCode(), status.getReasonPhrase(), message, uriInfo);
    }

    static Response build(int status, String error, String message, UriInfo uriInfo) {
        ErrorResponse errorResponse = new ErrorResponse(status, error, message, resolvePath(uriInfo));
        return Response.status(status)
                .entity(errorResponse)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    static String resolvePath(UriInfo uriInfo) {
        return uriInfo == null ? null : uriInfo.getRequestUri().getPath();
    }
}
