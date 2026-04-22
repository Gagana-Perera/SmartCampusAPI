package com.smartcampus.api.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

/**
 * Demo-only resource used to prove that the global exception mapper returns a
 * clean 500 response without leaking stack traces.
 */
@Path("/debug")
public class DebugResource {

    @GET
    @Path("/force-500")
    public String forceInternalServerError() {
        throw new RuntimeException("Intentional demo-only failure");
    }
}
