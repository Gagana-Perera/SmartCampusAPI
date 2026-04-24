# SmartCampusAPI

## Overview

SmartCampusAPI is a RESTful web service for managing rooms, sensors, and nested sensor readings in a smart campus environment. The project is implemented using JAX-RS with Jersey and Grizzly, keeps all data in memory, and exposes a versioned API under `/api/v1`.

The API supports discovery, room management, sensor management, sensor filtering, nested reading history, custom exception mapping, request/response logging, and a gated demo-only `500` trigger for coursework demonstrations.

## Technologies Used

- Java 11
- JAX-RS (Jersey 2.35)
- Grizzly HTTP Server
- Jackson JSON support
- Maven
- In-memory collections such as `ConcurrentHashMap`, `ArrayList`, and synchronized lists

## Prerequisites

- Java 11 or newer
- Maven 3.8 or newer

```bash
./mvnw clean package
```

This will generate `target/smart-campus-api.war` for Tomcat and a runnable JAR for Grizzly.

### Option A: Embedded Grizzly (Standalone)

```bash
./mvnw exec:java
```

The server starts on `http://localhost:8080/api/v1`.

To enable the demo-only `500` endpoint during a recording:

```bash
./mvnw -Dsmartcampus.demoMode=true exec:java
```

### Option B: Deploy to Tomcat (WAR)

1. Build the project: `./mvnw clean package`
2. Copy `target/smart-campus-api.war` to your Tomcat `webapps/` directory.
3. Access the API at `http://localhost:8080/smart-campus-api/api/v1/`

If port `8080` is unavailable, you can override the base URI for Grizzly:

```bash
./mvnw -Dsmartcampus.baseUri=http://localhost:8081/api/v1/ exec:java
```

## Base URL

`http://localhost:8080/api/v1`

## Core Endpoints

| Method | Endpoint                              | Description                        |
| ------ | ------------------------------------- | ---------------------------------- |
| GET    | `/api/v1`                             | Discovery endpoint                 |
| GET    | `/api/v1/rooms`                       | Get all rooms                      |
| POST   | `/api/v1/rooms`                       | Create a new room                  |
| GET    | `/api/v1/rooms/{roomId}`              | Get a room by ID                   |
| DELETE | `/api/v1/rooms/{roomId}`              | Delete a room if it has no sensors |
| GET    | `/api/v1/sensors`                     | Get all sensors                    |
| GET    | `/api/v1/sensors?type=CO2`            | Filter sensors by type             |
| POST   | `/api/v1/sensors`                     | Create a new sensor                |
| GET    | `/api/v1/sensors/{sensorId}`          | Get a sensor by ID                 |
| GET    | `/api/v1/sensors/{sensorId}/readings` | Get all readings for one sensor    |
| POST   | `/api/v1/sensors/{sensorId}/readings` | Add a reading for one sensor       |

## Quick Checks

Show discovery:

```bash
curl -i http://localhost:8080/api/v1
```

List seeded rooms:

```bash
curl -i http://localhost:8080/api/v1/rooms
```

Create a room:

```bash
curl -i -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{
    "id": "SCI-201",
    "name": "Science Lab",
    "capacity": 40
  }'
```

Create a sensor:

```bash
curl -i -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{
    "id": "CO2-002",
    "type": "CO2",
    "status": "ACTIVE",
    "roomId": "SCI-201"
  }'
```

Post a reading:

```bash
curl -i -X POST http://localhost:8080/api/v1/sensors/CO2-002/readings \
  -H "Content-Type: application/json" \
  -d '{
    "value": 23.7
  }'
```

Filter sensors by type:

```bash
curl -i "http://localhost:8080/api/v1/sensors?type=CO2"
```

Show conflict when deleting a room that still has sensors:

```bash
curl -i -X DELETE http://localhost:8080/api/v1/rooms/SCI-201
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

## Coursework Report Answers

1. JAX-RS resource classes are normally created per request, so shared in-memory data should not be stored in ordinary resource instance fields. This project stores shared state in the static `DataStore` for the lifetime of the running application.
2. Hypermedia is useful because the discovery endpoint tells clients where the main resources are without relying only on external documentation.
3. Returning full objects makes the API easier to test and use because clients get the useful fields immediately. Returning only IDs would reduce response size but require extra follow-up requests.
4. `DELETE /api/v1/rooms/{roomId}` is idempotent because deleting an already deleted room still returns `204 No Content`.
5. Query parameters are suitable for filtering because `GET /api/v1/sensors?type=CO2` is a filtered view of the sensors collection, while path parameters identify one specific resource.
6. If a POST request uses the wrong content type, Jersey returns `415 Unsupported Media Type`, and the API formats that as JSON through the web exception mapper.
7. The readings endpoints use a sub-resource because readings belong to a specific sensor and the separate resource keeps the code easier to maintain.
8. `422 Unprocessable Entity` is used for a sensor with an invalid `roomId` because the JSON request is understandable, but the linked room is not valid.
9. Stack traces should not be returned to clients because they can expose implementation details. The global mapper returns a clean generic `500` response instead.
10. Request and response filters are used for logging so every endpoint logs the method, URI, and status in one central place.

Recommended Postman order:

1. `GET /api/v1` to show discovery.
2. `GET /api/v1/rooms` and `GET /api/v1/sensors` to show seeded in-memory data.
3. `POST /api/v1/rooms`, then `GET /api/v1/rooms/{roomId}`, then `DELETE /api/v1/rooms/{roomId}` to show room management.
4. Recreate the room, then `POST /api/v1/sensors` to show linked sensor creation.
5. `GET /api/v1/sensors?type=CO2` to show query-parameter filtering.
6. `POST /api/v1/sensors/{sensorId}/readings`, then `GET /api/v1/sensors/{sensorId}/readings`, then `GET /api/v1/sensors/{sensorId}` to show the nested sub-resource and updated `currentValue`.
7. Demonstrate error handling with `409` for deleting a room with sensors, `422` for creating a sensor with a missing room, `403` for posting a reading to a maintenance sensor, and the demo-only `500` endpoint.
