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

    public static HttpServer startServer() {
        // Create a resource config that scans for JAX-RS resources and providers
        final SmartCampusApplication config = new SmartCampusApplication();

        // Create and start a new instance of grizzly http server
        // exposing the Jersey application at the resolved base URI
        return GrizzlyHttpServerFactory.createHttpServer(URI.create(resolveBaseUri()), config);
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
        return System.getProperty("smartcampus.baseUri", DEFAULT_BASE_URI);
    }
}
