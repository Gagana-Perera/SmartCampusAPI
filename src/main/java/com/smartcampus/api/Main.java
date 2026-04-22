package com.smartcampus.api;

import com.smartcampus.api.config.SmartCampusApplication;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main class to start the Grizzly HTTP server.
 */
public class Main {
    // In the embedded Grizzly setup, Jersey mounts resources directly under the
    // server base URI, so we include the API prefix here to match the documented
    // routes.
    public static final String DEFAULT_BASE_URI = "http://localhost:8080/api/v1/";
    public static final String BASE_URI_PROPERTY = "smartcampus.baseUri";
    public static final String DEMO_MODE_PROPERTY = "smartcampus.demoMode";

    public static HttpServer startServer() {
        return startServer(URI.create(resolveBaseUri()), resolveDemoMode());
    }

    public static HttpServer startServer(URI baseUri, boolean demoMode) {
        // Create a resource config with the requested demo-mode visibility.
        final SmartCampusApplication config = new SmartCampusApplication(demoMode);

        // Create and start a new instance of grizzly http server
        // exposing the Jersey application at the resolved base URI
        return GrizzlyHttpServerFactory.createHttpServer(baseUri, config);
    }

    public static void main(String[] args) {
        try {
            final HttpServer server = startServer();
            System.out.println(String.format("Smart Campus API started at %s\nHit enter to stop it...", resolveBaseUri()));
            System.in.read();
            server.shutdownNow();
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static String resolveBaseUri() {
        return System.getProperty(BASE_URI_PROPERTY, DEFAULT_BASE_URI);
    }

    private static boolean resolveDemoMode() {
        return Boolean.parseBoolean(System.getProperty(DEMO_MODE_PROPERTY, "false"));
    }
}
