package com.smartcampus.api.config;

import javax.ws.rs.ApplicationPath;
import org.glassfish.jersey.server.ResourceConfig;

/**
 * JAX-RS Application configuration.
 * The @ApplicationPath defines the base URI for all resource URIs.
 */
@ApplicationPath("/api/v1")
public class SmartCampusApplication extends ResourceConfig {
    public SmartCampusApplication() {
        // Scan for resources and providers in the specified package
        packages("com.smartcampus.api");
        
        // Register Jackson for JSON support
        register(org.glassfish.jersey.jackson.JacksonFeature.class);
    }
}
