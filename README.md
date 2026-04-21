# SmartCampusAPI - 5COSC022W Client-Server Architectures

## Overview
SmartCampusAPI is a RESTful web service built for the university's "Smart Campus" initiative. The system provides a seamless interface for campus facilities managers and automated building systems to manage Rooms, Sensors, and Sensor Readings. 

This project is built strictly using **JAX-RS (Jersey implementation)** and focuses on RESTful architectural patterns, deep resource nesting, and resilient error-handling.

## Features
- **API Discovery**: Root endpoint providing metadata and resource links.
- **Room Management**: Full CRUD operations with safety constraints.
- **Sensor Operations**: Linking sensors to rooms with validation.
- **Deep Nesting**: Hierarchical access to sensor reading history using Sub-Resource Locators.
- **Adaptive Filtering**: Query-parameter based sensor filtering by type.
- **Robust Error Handling**: Custom exception mapping (409, 422, 403) with consistent JSON error responses.
- **Operational Visibility**: Contextual logging of all requests, responses, and errors.
- **In-Memory Persistence**: Thread-safe storage that persists throughout the server's lifecycle.

## Technologies Used
- **Language**: Java 11
- **Framework**: JAX-RS (Jakarta RESTful Web Services)
- **Implementation**: Jersey 2.35
- **Server**: Grizzly 2 (Embedded)
- **Build Tool**: Maven
- **JSON Serialization**: Jackson

## Project Structure
```text
com.smartcampus.api
├── config          # JAX-RS Application configuration
├── filter          # Logging filters (Request/Response)
├── exception       # Custom business logic exceptions
├── mapper          # Exception mappers for HTTP status codes
├── model           # POJO data models
├── repository      # In-memory thread-safe DataStore
├── resource        # REST resource endpoints
└── Main.java       # Server entry point
```

## How to Build and Run

### Prerequisites
- Java 11 or higher
- Apache Maven

### Steps
1. **Clone the repository**:
   ```bash
   git clone git@github.com:Gagana-Perera/Smart-Campus-API.git
   cd SmartCampusAPI
   ```

2. **Build the project**:
   ```bash
   mvn clean install
   ```

3. **Run the server**:
   ```bash
   mvn exec:java
   ```
   The API will be available at: `http://localhost:8080/api/v1`

---

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1` | Discovery endpoint |
| GET | `/api/v1/rooms` | List all rooms |
| POST | `/api/v1/rooms` | Create a new room |
| DELETE | `/api/v1/rooms/{id}` | Delete a room (must be empty) |
| GET | `/api/v1/sensors` | List all sensors (optional `?type=`) |
| POST | `/api/v1/sensors` | Create and link a sensor |
| GET | `/api/v1/sensors/{id}/readings` | Get reading history for a sensor |
| POST | `/api/v1/sensors/{id}/readings` | Add a new sensor reading |

---

## Sample interactons (Curl Commands)

### 1. API Discovery
```bash
curl -X GET http://localhost:8080/api/v1
```

### 2. Create a New Room
```bash
curl -X POST http://localhost:8080/api/v1/rooms \
     -H "Content-Type: application/json" \
     -d '{"id":"LIB-301", "name":"Library Quiet Study", "capacity":50}'
```

### 3. Create a Sensor (Linked to Room)
```bash
curl -X POST http://localhost:8080/api/v1/sensors \
     -H "Content-Type: application/json" \
     -d '{"id":"TEMP-001", "type":"Temperature", "status":"ACTIVE", "roomId":"LIB-301"}'
```

### 4. Filter Sensors by Type
```bash
curl -X GET "http://localhost:8080/api/v1/sensors?type=Temperature"
```

### 5. Post a Sensor Reading
```bash
curl -X POST http://localhost:8080/api/v1/sensors/TEMP-001/readings \
     -H "Content-Type: application/json" \
     -d '{"value": 22.5}'
```

### 6. Get Reading History
```bash
curl -X GET http://localhost:8080/api/v1/sensors/TEMP-001/readings
```

### 7. Trigger Conflict (Delete Room with Sensors)
```bash
curl -X DELETE http://localhost:8080/api/v1/rooms/LIB-301
```

---

## Report Answers

### Part 1: Lifecycle & Persistence
**Question**: Explain the default lifecycle of a JAX-RS Resource class and how it impacts in-memory data.
**Answer**: By default, JAX-RS resources follow a **per-request** lifecycle. This means a new instance of the resource class is created for every incoming HTTP request and destroyed once the response is sent. If I were to store data in ordinary instance variables within the Resource classes, all data would be lost between requests. To avoid this and ensure persistence while the server is running, I used a dedicated `DataStore` class with **static collections** (like `ConcurrentHashMap`). This ensures the data resides in the application's heap memory rather than a short-lived resource instance, allowing it to persist across multiple requests.

**Question**: Why is Hypermedia (HATEOAS) beneficial?
**Answer**: Providing links within API responses (Hypermedia) is beneficial because it makes the API **self-descriptive**. Instead of relying purely on static documentation, a client can "discover" available actions and resources directly from the response. This reduces coupling between the client and the server, as the server can change URI structures without breaking clients as long as the relationship names remain stable.

### Part 2: Collection Design & Idempotency
**Question**: What are the implications of returning full objects vs. just IDs in collections?
**Answer**: Returning only IDs reduces **network bandwidth** and minimizes payload size, which is great for mobile devices or very large datasets. However, it requires the client to make multiple follow-up requests ("N+1 problem") to get details. Returning full objects is more "chunky" and efficient for clients that need to display data immediately, but it increases server load and response time for large collections.

**Question**: Is DELETE idempotent in your implementation?
**Answer**: Yes. My `DELETE /rooms/{roomId}` is idempotent because calling it multiple times results in the same server state. The first call deletes the room (if allowed), and subsequent calls for the same ID simply return a `204 No Content` because the room is already gone. The "effect" on the server doesn't change after the first successful deletion.

### Part 3: Filtering & Content Types
**Question**: Why are query parameters better than path parameters for filtering?
**Answer**: Path parameters are generally used to identify a **specific resource** (like a unique ID), whereas query parameters are used to **modify the view** of a collection (filtering, sorting, searching). Using query params (e.g., `?type=CO2`) is more flexible because it allows for optional combinations of filters without creating complex, rigid URL patterns for every possible filter combination.

**Question**: What happens if a client sends the wrong content type to an endpoint?
**Answer**: Since our endpoints use `@Consumes(MediaType.APPLICATION_JSON)`, if a client sends `text/plain` or `application/xml`, Jersey will automatically intercept the request and return a **415 Unsupported Media Type** error before the business logic is even reached.

### Part 4: Sub-Resource Locators
**Question**: What are the benefits of the Sub-Resource Locator pattern?
**Answer**: Sub-resource locators (like `/{sensorId}/readings`) allow us to delegate logic to dedicated classes, which improves **code modularity**. Instead of having one massive `SensorResource` handling dozens of endpoints, we separate the "readings" logic into its own class. This makes the code cleaner, easier to test, and more scalable as the API grows.

### Part 5: Error Handling & Observability
**Question**: Why is 422 more accurate than 404 for missing linked resources?
**Answer**: While 404 indicates a URI itself doesn't exist, **422 Unprocessable Entity** indicates that the request body is syntactically correct (valid JSON) but semantically invalid (the referenced Room ID doesn't exist in our system). This distinction helps developers debug whether they hit the wrong endpoint or simply provided invalid data.

**Question**: What are the risks of exposing raw stack traces?
**Answer**: From a security standpoint, stack traces reveal **internal implementation details**, such as library versions, package structures, and file paths. An attacker can use this information to find specific vulnerabilities or map out the server's architecture. My `GlobalExceptionMapper` prevents this by catching all `Throwable` errors and returning a generic JSON message.

**Question**: Why use filters for logging?
**Answer**: Filters allow us to implement **cross-cutting concerns** (logic that applies to all endpoints) in a single place. Instead of manually adding `Logger.info()` to 20 different methods, a filter intercepts every request and response automatically. This ensures consistency, reduces code duplication, and makes the core business logic much easier to read.
