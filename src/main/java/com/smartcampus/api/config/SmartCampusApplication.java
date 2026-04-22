package com.smartcampus.api.config;

import com.smartcampus.api.filter.LoggingFilter;
import com.smartcampus.api.mapper.DuplicateResourceExceptionMapper;
import com.smartcampus.api.mapper.GlobalExceptionMapper;
import com.smartcampus.api.mapper.InvalidRequestExceptionMapper;
import com.smartcampus.api.mapper.LinkedResourceNotFoundMapper;
import com.smartcampus.api.mapper.ResourceNotFoundMapper;
import com.smartcampus.api.mapper.RoomNotEmptyMapper;
import com.smartcampus.api.mapper.SensorUnavailableMapper;
import com.smartcampus.api.mapper.WebApplicationExceptionMapper;
import com.smartcampus.api.resource.DebugResource;
import com.smartcampus.api.resource.DiscoveryResource;
import com.smartcampus.api.resource.RoomResource;
import com.smartcampus.api.resource.SensorResource;

import javax.ws.rs.ApplicationPath;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;

/**
 * JAX-RS Application configuration.
 * The @ApplicationPath defines the base URI for all resource URIs.
 */
@ApplicationPath("/api/v1")
public class SmartCampusApplication extends ResourceConfig {
    public SmartCampusApplication() {
        this(Boolean.parseBoolean(System.getProperty("smartcampus.demoMode", "false")));
    }

    public SmartCampusApplication(boolean demoMode) {
        register(JacksonFeature.class);

        register(DiscoveryResource.class);
        register(RoomResource.class);
        register(SensorResource.class);

        register(LoggingFilter.class);

        register(DuplicateResourceExceptionMapper.class);
        register(GlobalExceptionMapper.class);
        register(InvalidRequestExceptionMapper.class);
        register(LinkedResourceNotFoundMapper.class);
        register(ResourceNotFoundMapper.class);
        register(RoomNotEmptyMapper.class);
        register(SensorUnavailableMapper.class);
        register(WebApplicationExceptionMapper.class);

        if (demoMode) {
            register(DebugResource.class);
        }
    }
}
