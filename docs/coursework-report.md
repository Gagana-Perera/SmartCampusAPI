# SmartCampusAPI Coursework Report

## Setup And Discovery

### Architecture And Configuration
SmartCampusAPI is a Java 11 REST API built with JAX-RS using Jersey and the embedded Grizzly HTTP server. The application is configured through [`SmartCampusApplication`](../src/main/java/com/smartcampus/api/config/SmartCampusApplication.java), which uses `@ApplicationPath("/api/v1")` and registers the API resources, logging filter, JSON support, and exception mappers.

The project keeps its state in an in-memory [`DataStore`](../src/main/java/com/smartcampus/api/repository/DataStore.java). The store uses `ConcurrentHashMap` plus synchronized reading lists so that multiple requests can safely access rooms, sensors, and reading history while the process is running.

### Discovery Endpoint
`GET /api/v1` returns a JSON discovery document containing:

- API name
- version
- contact information
- resource map for `/rooms` and `/sensors`

This gives clients a simple entry point for the API and satisfies the discovery requirement in the coursework brief.

## Room Management

### Room Implementation
The room resource supports:

- `GET /api/v1/rooms`
- `POST /api/v1/rooms`
- `GET /api/v1/rooms/{roomId}`

Validation ensures that room IDs and names are not blank and that capacity is greater than zero. Successful room creation returns `201 Created`, includes a `Location` header, and returns the created room as JSON.

### Deletion And Business Logic
`DELETE /api/v1/rooms/{roomId}` is implemented as an idempotent operation:

- If the room does not exist, the API returns `204 No Content`.
- If the room exists but still has assigned sensors, the API returns `409 Conflict`.
- If the room exists and is empty, it is deleted and the API returns `204 No Content`.

This prevents orphaned sensor records while also preserving idempotent delete semantics.

## Sensors And Filtering

### Sensor Integrity
The sensor resource supports:

- `GET /api/v1/sensors`
- `POST /api/v1/sensors`
- `GET /api/v1/sensors/{sensorId}`

When a sensor is created, the API validates that the linked `roomId` exists. If the request is syntactically valid JSON but references a non-existent room, the API returns `422 Unprocessable Entity` using a dedicated exception mapper.

### Filtered Retrieval
`GET /api/v1/sensors?type=CO2` filters the sensors collection by type. The comparison is case-insensitive, so clients can request `CO2`, `co2`, or similar variations and still receive the expected result.

This uses a query parameter because the client is asking for a filtered view of the collection rather than identifying a different resource.

## Sub-Resources

### Sub-Resource Locator
The API uses a sub-resource locator for sensor readings:

- `GET /api/v1/sensors/{sensorId}/readings`
- `POST /api/v1/sensors/{sensorId}/readings`

The parent `SensorResource` delegates reading-related work to a separate `SensorReadingResource`, which keeps the code modular and follows the natural URI hierarchy.

### Historical Management
Posting a reading stores the historical event and also updates the parent sensor’s `currentValue`. This keeps the sensor summary consistent with the latest recorded reading while still preserving full reading history.

If a sensor is in `MAINTENANCE`, posting a reading returns `403 Forbidden`.

## Error Handling

### Specific Exceptions
The API includes dedicated exception mappers for the main coursework scenarios:

- `409 Conflict` for duplicate resources and non-empty room deletion
- `422 Unprocessable Entity` for missing linked rooms during sensor creation
- `403 Forbidden` for posting readings to maintenance sensors

Each mapper returns a consistent JSON error body with timestamp, status, error, message, and path.

### Global Safety Net
Unexpected exceptions are handled by a catch-all `ExceptionMapper<Throwable>`, which returns a clean `500 Internal Server Error` JSON response instead of exposing a stack trace to the client.

For video demonstration purposes, the project also includes a demo-only `/api/v1/debug/force-500` endpoint. It is only registered when `smartcampus.demoMode=true`, so it is not part of the normal API surface.

## Written Answers

### 1. Explain the default lifecycle of a JAX-RS resource and how it impacts in-memory data
JAX-RS resource classes are typically created per request. A new resource object is created for an incoming HTTP request and discarded once the response is sent. Because of that lifecycle, storing shared application state in ordinary resource instance fields would not work: the data would not survive across requests. In this project, persistent in-memory state is therefore kept in the static `DataStore`, which remains alive for the lifetime of the application process. Because multiple requests may access that shared state concurrently, thread-safe collections such as `ConcurrentHashMap` and synchronized lists are used.

### 2. Why is hypermedia useful in REST APIs
Hypermedia makes an API easier to discover because clients can learn where to go next from the API itself instead of depending completely on external documentation. In this project, `GET /api/v1` advertises the main resources and gives clients a lightweight self-documenting entry point to `/rooms` and `/sensors`.

### 3. What are the trade-offs of returning full objects versus IDs in collection responses
Returning full objects is convenient because clients receive useful information in a single request and do not need extra follow-up calls just to resolve IDs. The drawback is that payloads are larger. Returning only IDs reduces payload size and keeps collection responses lighter, but it shifts more work onto the client because each ID often requires an extra lookup. For this coursework, returning full room and sensor objects keeps the API easier to use and easier to test.

### 4. Is DELETE idempotent in this API
Yes. `DELETE /api/v1/rooms/{roomId}` is idempotent because repeating the same request does not keep changing server state after the resource is gone. If the room does not exist, the API still returns `204 No Content`, so repeated delete requests have the same observable result after the first successful deletion.

### 5. Why use query parameters for filtering instead of path parameters
Query parameters are the better fit for optional filtering because they refine the representation of a collection rather than identify a different resource. `/api/v1/sensors?type=CO2` clearly means “show the sensors collection filtered by type.” A path parameter is more appropriate when identifying one concrete resource, such as `/api/v1/sensors/TEMP-001`.

### 6. What happens if the wrong content type is sent to a POST endpoint
The POST endpoints are annotated with `@Consumes(MediaType.APPLICATION_JSON)`. If a client sends a body using a different media type, such as `text/plain`, Jersey rejects the request before the resource method executes and returns `415 Unsupported Media Type`. In this project, the generic web exception mapper formats that failure as the same structured JSON error body used elsewhere.

### 7. Why use the sub-resource locator pattern for readings
The sub-resource locator keeps the reading-history logic separate from the main sensor resource. That improves maintainability because `SensorResource` stays focused on sensor-level operations, while `SensorReadingResource` handles the nested reading collection. It also maps well to the URI design because readings naturally belong to a specific sensor.

### 8. Why is HTTP 422 suitable for a missing linked room during sensor creation
`422 Unprocessable Entity` is appropriate because the request body can still be valid JSON and the endpoint itself exists. The problem is semantic: the payload refers to a room that is not valid in the current application state. Using `404 Not Found` would be misleading because the client did reach a valid endpoint; it is the linked entity inside the payload that is invalid.

### 9. Why should raw stack traces not be exposed to API clients
Raw stack traces can reveal internal class names, package structure, library behavior, and sometimes filesystem paths or implementation details that attackers could use for reconnaissance. They also make error responses noisy and harder for legitimate clients to consume. Returning a clean generic `500` response protects internal details while still signaling that the server failed unexpectedly.

### 10. Why use request and response filters for logging
Filters centralize cross-cutting concerns. By handling logging in a request/response filter, the API records method, URI, and response status for every endpoint in one place instead of duplicating logging code in every resource method. This keeps the resource code cleaner and makes logging behavior consistent across the whole service.
