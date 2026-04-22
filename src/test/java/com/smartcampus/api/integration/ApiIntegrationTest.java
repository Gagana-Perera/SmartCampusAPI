package com.smartcampus.api.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcampus.api.Main;
import com.smartcampus.api.repository.DataStore;
import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiIntegrationTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static HttpServer server;
    private static String baseUrl;

    @BeforeAll
    static void startServer() {
        server = Main.startServer(URI.create("http://localhost:0/api/v1/"), true);
        baseUrl = resolveBaseUrl(server);
    }

    @AfterAll
    static void stopServer() {
        if (server != null) {
            server.shutdownNow();
        }
    }

    @BeforeEach
    void resetData() {
        DataStore.resetToDefaults();
    }

    @Test
    void discoveryEndpointReturnsExpectedMetadata() throws Exception {
        HttpResponse<String> response = send("GET", "", null, null);

        assertEquals(200, response.statusCode());

        JsonNode json = json(response);
        assertEquals("Smart Campus API", json.path("name").asText());
        assertEquals("v1", json.path("version").asText());
        assertEquals("admin@smartcampus.com", json.path("contact").asText());
        assertEquals("/api/v1/rooms", json.path("resources").path("rooms").asText());
        assertEquals("/api/v1/sensors", json.path("resources").path("sensors").asText());
    }

    @Test
    void roomEndpointsSupportCrudAndIdempotentDelete() throws Exception {
        HttpResponse<String> createResponse = send(
                "POST",
                "/rooms",
                "application/json",
                "{\"id\":\"SCI-900\",\"name\":\"Testing Room\",\"capacity\":25}"
        );

        assertEquals(201, createResponse.statusCode());
        assertTrue(createResponse.headers().firstValue("Location").orElse("").endsWith("/rooms/SCI-900"));

        JsonNode createdRoom = json(createResponse);
        assertEquals("SCI-900", createdRoom.path("id").asText());
        assertEquals("Testing Room", createdRoom.path("name").asText());
        assertEquals(25, createdRoom.path("capacity").asInt());

        HttpResponse<String> getResponse = send("GET", "/rooms/SCI-900", null, null);
        assertEquals(200, getResponse.statusCode());
        assertEquals("SCI-900", json(getResponse).path("id").asText());

        HttpResponse<String> deleteResponse = send("DELETE", "/rooms/SCI-900", null, null);
        assertEquals(204, deleteResponse.statusCode());

        HttpResponse<String> repeatDeleteResponse = send("DELETE", "/rooms/SCI-900", null, null);
        assertEquals(204, repeatDeleteResponse.statusCode());

        HttpResponse<String> missingResponse = send("GET", "/rooms/SCI-900", null, null);
        assertEquals(404, missingResponse.statusCode());
        assertEquals("Room with ID SCI-900 not found", json(missingResponse).path("message").asText());
    }

    @Test
    void deletingRoomWithSensorsReturnsConflictJson() throws Exception {
        HttpResponse<String> response = send("DELETE", "/rooms/LIB-301", null, null);

        assertEquals(409, response.statusCode());

        JsonNode json = json(response);
        assertEquals("Conflict", json.path("error").asText());
        assertEquals(
                "Room LIB-301 cannot be deleted because it still has assigned sensors.",
                json.path("message").asText()
        );
        assertEquals("/api/v1/rooms/LIB-301", json.path("path").asText());
    }

    @Test
    void sensorEndpointsValidateLinkedRoomsAndSupportFiltering() throws Exception {
        HttpResponse<String> createResponse = send(
                "POST",
                "/sensors",
                "application/json",
                "{\"id\":\"CO2-200\",\"type\":\"CO2\",\"status\":\"ACTIVE\",\"roomId\":\"ENG-101\"}"
        );

        assertEquals(201, createResponse.statusCode());
        assertTrue(createResponse.headers().firstValue("Location").orElse("").endsWith("/sensors/CO2-200"));

        HttpResponse<String> getResponse = send("GET", "/sensors/CO2-200", null, null);
        assertEquals(200, getResponse.statusCode());
        assertEquals("ENG-101", json(getResponse).path("roomId").asText());

        HttpResponse<String> filterResponse = send("GET", "/sensors?type=co2", null, null);
        assertEquals(200, filterResponse.statusCode());
        JsonNode filteredSensors = json(filterResponse);
        assertEquals(1, filteredSensors.size());
        assertEquals("CO2-200", filteredSensors.get(0).path("id").asText());

        HttpResponse<String> duplicateResponse = send(
                "POST",
                "/sensors",
                "application/json",
                "{\"id\":\"CO2-200\",\"type\":\"CO2\",\"status\":\"ACTIVE\",\"roomId\":\"ENG-101\"}"
        );
        assertEquals(409, duplicateResponse.statusCode());
        assertEquals("Conflict", json(duplicateResponse).path("error").asText());

        HttpResponse<String> missingRoomResponse = send(
                "POST",
                "/sensors",
                "application/json",
                "{\"id\":\"CO2-404\",\"type\":\"CO2\",\"status\":\"ACTIVE\",\"roomId\":\"ROOM-404\"}"
        );
        assertEquals(422, missingRoomResponse.statusCode());
        assertEquals("Unprocessable Entity", json(missingRoomResponse).path("error").asText());
    }

    @Test
    void readingsSubResourceTracksHistoryAndUpdatesParentSensor() throws Exception {
        HttpResponse<String> historyBefore = send("GET", "/sensors/TEMP-001/readings", null, null);
        assertEquals(200, historyBefore.statusCode());
        assertEquals(0, json(historyBefore).size());

        HttpResponse<String> createReadingResponse = send(
                "POST",
                "/sensors/TEMP-001/readings",
                "application/json",
                "{\"value\":23.7}"
        );
        assertEquals(201, createReadingResponse.statusCode());
        assertEquals(23.7, json(createReadingResponse).path("value").asDouble());

        HttpResponse<String> historyAfter = send("GET", "/sensors/TEMP-001/readings", null, null);
        assertEquals(200, historyAfter.statusCode());
        JsonNode readings = json(historyAfter);
        assertEquals(1, readings.size());
        assertEquals(23.7, readings.get(0).path("value").asDouble());

        HttpResponse<String> sensorResponse = send("GET", "/sensors/TEMP-001", null, null);
        assertEquals(200, sensorResponse.statusCode());
        assertEquals(23.7, json(sensorResponse).path("currentValue").asDouble());
    }

    @Test
    void maintenanceSensorsRejectNewReadings() throws Exception {
        HttpResponse<String> createSensorResponse = send(
                "POST",
                "/sensors",
                "application/json",
                "{\"id\":\"MAINT-001\",\"type\":\"Humidity\",\"status\":\"MAINTENANCE\",\"roomId\":\"ENG-101\"}"
        );
        assertEquals(201, createSensorResponse.statusCode());

        HttpResponse<String> readingResponse = send(
                "POST",
                "/sensors/MAINT-001/readings",
                "application/json",
                "{\"value\":45.5}"
        );

        assertEquals(403, readingResponse.statusCode());
        assertEquals("Forbidden", json(readingResponse).path("error").asText());
        assertEquals("/api/v1/sensors/MAINT-001/readings", json(readingResponse).path("path").asText());
    }

    @Test
    void invalidRequestsReturnBadRequestJson() throws Exception {
        HttpResponse<String> invalidRoomResponse = send(
                "POST",
                "/rooms",
                "application/json",
                "{\"id\":\"   \",\"name\":\"\",\"capacity\":0}"
        );
        assertEquals(400, invalidRoomResponse.statusCode());
        assertEquals("Bad Request", json(invalidRoomResponse).path("error").asText());

        HttpResponse<String> invalidReadingResponse = send(
                "POST",
                "/sensors/TEMP-001/readings",
                "application/json",
                "{\"timestamp\":-1,\"value\":9.5}"
        );
        assertEquals(400, invalidReadingResponse.statusCode());
        assertEquals("Reading timestamp must not be negative.", json(invalidReadingResponse).path("message").asText());
    }

    @Test
    void webApplicationExceptionsReturnJsonBodies() throws Exception {
        HttpResponse<String> unsupportedMediaTypeResponse = send(
                "POST",
                "/rooms",
                "text/plain",
                "hello"
        );
        assertEquals(415, unsupportedMediaTypeResponse.statusCode());
        assertEquals("Unsupported Media Type", json(unsupportedMediaTypeResponse).path("error").asText());

        HttpResponse<String> methodNotAllowedResponse = send(
                "PUT",
                "/rooms/ENG-101",
                "application/json",
                "{\"name\":\"Updated\"}"
        );
        assertEquals(405, methodNotAllowedResponse.statusCode());
        assertEquals("Method Not Allowed", json(methodNotAllowedResponse).path("error").asText());
    }

    @Test
    void demoOnlyEndpointReturnsClean500JsonWhenEnabled() throws Exception {
        HttpResponse<String> response = send("GET", "/debug/force-500", null, null);

        assertEquals(500, response.statusCode());

        JsonNode json = json(response);
        assertEquals("Internal Server Error", json.path("error").asText());
        assertEquals("An unexpected error occurred while processing the request.", json.path("message").asText());
        assertEquals("/api/v1/debug/force-500", json.path("path").asText());

        String body = response.body();
        assertFalse(body.contains("RuntimeException"));
        assertFalse(body.contains("Intentional demo-only failure"));
        assertFalse(body.contains("DebugResource"));
    }

    @Test
    void demoOnlyEndpointIsHiddenWhenDisabled() throws Exception {
        HttpServer disabledServer = Main.startServer(URI.create("http://localhost:0/api/v1/"), false);

        try {
            HttpResponse<String> response = send(
                    resolveBaseUrl(disabledServer),
                    "GET",
                    "/debug/force-500",
                    null,
                    null
            );

            assertEquals(404, response.statusCode());
            assertEquals("Not Found", json(response).path("error").asText());
        } finally {
            disabledServer.shutdownNow();
        }
    }

    private static String resolveBaseUrl(HttpServer httpServer) {
        int port = httpServer.getListeners().iterator().next().getPort();
        return "http://localhost:" + port + "/api/v1";
    }

    private static HttpResponse<String> send(String method, String path, String contentType, String body)
            throws IOException, InterruptedException {
        return send(baseUrl, method, path, contentType, body);
    }

    private static HttpResponse<String> send(
            String requestBaseUrl,
            String method,
            String path,
            String contentType,
            String body
    ) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(requestBaseUrl + path))
                .header("Accept", "application/json");

        if (contentType != null) {
            builder.header("Content-Type", contentType);
        }

        if (body == null) {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            builder.method(method, HttpRequest.BodyPublishers.ofString(body));
        }

        return HttpClient.newHttpClient().send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static JsonNode json(HttpResponse<String> response) throws IOException {
        return OBJECT_MAPPER.readTree(response.body());
    }
}
