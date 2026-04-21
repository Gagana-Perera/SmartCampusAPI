# SmartCampusAPI

## Project Title
SmartCampusAPI for 5COSC022W Client-Server Architectures coursework

## Overview
SmartCampusAPI is a RESTful web service for managing rooms, sensors, and nested sensor readings in a smart campus environment. The project is implemented using JAX-RS with Jersey and Grizzly, keeps all data in memory, and exposes a versioned API under `/api/v1`.

The API supports discovery, room management, sensor management, sensor filtering, sub-resource based reading history, custom exception mapping, and request/response logging.

## Technologies Used
- Java 11
- JAX-RS (Jersey 2.35)
- Grizzly HTTP Server
- Jackson JSON support
- Maven
- In-memory collections such as `ConcurrentHashMap`, `ArrayList`, and synchronized lists

## Project Structure
```text
src/main/java/com/smartcampus/api
├── config
│   └── SmartCampusApplication.java
├── exception
│   ├── DuplicateResourceException.java
│   ├── InvalidRequestException.java
│   ├── LinkedResourceNotFoundException.java
│   ├── ResourceNotFoundException.java
│   ├── RoomNotEmptyException.java
│   └── SensorUnavailableException.java
├── filter
│   └── LoggingFilter.java
├── mapper
│   ├── DuplicateResourceExceptionMapper.java
│   ├── ErrorResponseFactory.java
│   ├── GlobalExceptionMapper.java
│   ├── InvalidRequestExceptionMapper.java
│   ├── LinkedResourceNotFoundMapper.java
│   ├── ResourceNotFoundMapper.java
│   ├── RoomNotEmptyMapper.java
│   ├── SensorUnavailableMapper.java
│   └── WebApplicationExceptionMapper.java
├── model
│   ├── ErrorResponse.java
│   ├── Room.java
│   ├── Sensor.java
│   └── SensorReading.java
├── repository
│   └── DataStore.java
├── resource
│   ├── DiscoveryResource.java
│   ├── RoomResource.java
│   ├── SensorReadingResource.java
│   └── SensorResource.java
└── Main.java
```

## How to Build and Run

### Prerequisites
- Java 11 or newer
- Maven 3.8 or newer

### Build
```bash
mvn clean package
```

### Run
```bash
mvn exec:java
```

The server starts on `http://localhost:8080`.

## Base URL
`http://localhost:8080/api/v1`

## Endpoint List

| Method | Endpoint | Description |
| --- | --- | --- |
| GET | `/api/v1` | Discovery endpoint |
| GET | `/api/v1/rooms` | Get all rooms |
| POST | `/api/v1/rooms` | Create a new room |
| GET | `/api/v1/rooms/{roomId}` | Get a room by ID |
| DELETE | `/api/v1/rooms/{roomId}` | Delete a room if it has no sensors |
| GET | `/api/v1/sensors` | Get all sensors |
| GET | `/api/v1/sensors?type=CO2` | Filter sensors by type |
| POST | `/api/v1/sensors` | Create a new sensor |
| GET | `/api/v1/sensors/{sensorId}` | Get a sensor by ID |
| GET | `/api/v1/sensors/{sensorId}/readings` | Get all readings for one sensor |
| POST | `/api/v1/sensors/{sensorId}/readings` | Add a reading for one sensor |

## Sample curl Commands

### 1. Discovery endpoint
```bash
curl http://localhost:8080/api/v1
```

### 2. Create a room
```bash
curl -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{
    "id": "SCI-201",
    "name": "Science Lab",
    "capacity": 40
  }'
```

### 3. Get all rooms
```bash
curl http://localhost:8080/api/v1/rooms
```

### 4. Get one room
```bash
curl http://localhost:8080/api/v1/rooms/SCI-201
```

### 5. Create a sensor
```bash
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{
    "id": "CO2-002",
    "type": "CO2",
    "status": "ACTIVE",
    "currentValue": 0.0,
    "roomId": "SCI-201"
  }'
```

### 6. Filter sensors by type
```bash
curl "http://localhost:8080/api/v1/sensors?type=CO2"
```

### 7. Post a reading
```bash
curl -X POST http://localhost:8080/api/v1/sensors/CO2-002/readings \
  -H "Content-Type: application/json" \
  -d '{
    "value": 23.7
  }'
```

### 8. Get readings for a sensor
```bash
curl http://localhost:8080/api/v1/sensors/CO2-002/readings
```

### 9. Trigger a 409 conflict by deleting a non-empty room
```bash
curl -i -X DELETE http://localhost:8080/api/v1/rooms/SCI-201
```

### 10. Trigger a 422 linked resource error
```bash
curl -i -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{
    "id": "TMP-404",
    "type": "Temperature",
    "status": "ACTIVE",
    "roomId": "ROOM-DOES-NOT-EXIST"
  }'
```

### 11. Trigger a 403 sensor unavailable error
```bash
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{
    "id": "MAINT-001",
    "type": "Humidity",
    "status": "MAINTENANCE",
    "roomId": "SCI-201"
  }'

curl -i -X POST http://localhost:8080/api/v1/sensors/MAINT-001/readings \
  -H "Content-Type: application/json" \
  -d '{
    "value": 45.5
  }'
```

## Error Response Format
All handled errors return a consistent JSON body:

```json
{
  "timestamp": 1710000000000,
  "status": 409,
  "error": "Conflict",
  "message": "Room SCI-201 cannot be deleted because it still has assigned sensors.",
  "path": "/api/v1/rooms/SCI-201"
}
```

## Report Answers

### 1. Explain the default lifecycle of a JAX-RS resource and how it impacts in-memory data
JAX-RS resource classes are typically created per request. That means a new resource object is created for each incoming HTTP request and discarded after the response is sent. If application state were stored in normal instance fields inside a resource class, that state would not survive between requests. To keep data available while the server is running, this project stores rooms, sensors, and readings inside a dedicated static `DataStore` that lives for the lifetime of the application process.

### 2. Why is hypermedia useful in REST APIs
Hypermedia makes the API easier to discover because the client can learn the main entry points from the response instead of relying only on external documentation. In this project the discovery endpoint advertises the top-level resources so a client can find `/rooms` and `/sensors` from `/api/v1`.

### 3. What are the trade-offs of returning full objects versus IDs in collection responses
Returning full objects is convenient because the client gets the data it needs in one request. The downside is larger payloads. Returning only IDs reduces payload size and coupling, but it often forces the client to perform extra follow-up requests. For this coursework, returning full room and sensor objects keeps the API simpler and easier to test.

### 4. Is DELETE idempotent in this API
Yes. `DELETE /api/v1/rooms/{roomId}` is implemented in an idempotent way. If the room does not exist, the API still returns `204 No Content`, so repeating the same delete request does not keep changing server state after the first successful deletion.

### 5. Why use query parameters for filtering instead of path parameters
Query parameters are more appropriate for optional filtering because they refine the view of a collection without changing the identity of the resource itself. `/api/v1/sensors?type=CO2` clearly means “the sensors collection filtered by type,” while a path parameter is better suited for identifying one specific resource such as `/api/v1/sensors/TEMP-001`.

### 6. What happens if the wrong content type is sent to a POST endpoint
Endpoints that accept request bodies use `@Consumes(MediaType.APPLICATION_JSON)`. If a client sends a different media type such as `text/plain`, Jersey rejects the request before the resource method runs and returns `415 Unsupported Media Type`. The generic web exception mapper then formats that error as JSON.

### 7. Why use the sub-resource locator pattern for readings
The sub-resource locator keeps the sensor reading logic in its own class instead of overloading `SensorResource` with unrelated behavior. This improves separation of concerns and matches the natural URI hierarchy because readings belong to a specific sensor.

### 8. Why is HTTP 422 suitable for a missing linked room during sensor creation
The request body can still be syntactically valid JSON even when the `roomId` points to a room that does not exist. That makes it a semantic validation problem rather than a missing endpoint. Returning `422 Unprocessable Entity` communicates that the server understood the request format but could not accept it because the linked resource was invalid.

### 9. Why should raw stack traces not be exposed to API clients
Raw stack traces can leak implementation details such as internal class names, file paths, and framework behavior. That makes debugging easier for attackers and produces noisy responses for clients. This project catches unexpected exceptions and returns a generic `500 Internal Server Error` JSON body instead.

### 10. Why use request and response filters for logging
Filters handle cross-cutting concerns in one place. By using a request/response filter, the API logs the HTTP method, requested URI, and final response status for every endpoint without duplicating logging code inside each resource method.

## Suggested Video Demonstration Order
1. Start the application and show the console logs.
2. Call `GET /api/v1` to show discovery.
3. Call `GET /api/v1/rooms` and `GET /api/v1/sensors` to show sample in-memory data.
4. Create a new room with `POST /api/v1/rooms`.
5. Retrieve it with `GET /api/v1/rooms/{roomId}`.
6. Create a valid sensor linked to that room.
7. Filter sensors with `GET /api/v1/sensors?type=CO2`.
8. Post a reading to the new sensor and then fetch reading history.
9. Attempt to delete the non-empty room and show the `409 Conflict` response.
10. Attempt to create a sensor with an invalid `roomId` and show the `422` response.
11. Create a maintenance sensor, try posting a reading, and show the `403` response.
12. Stop the server and conclude with the README/report highlights.
