# SmartCampusAPI Demo Checklist

## Start The API

Default demo run on port `8080`:

```bash
./mvnw -Dsmartcampus.demoMode=true exec:java
```

If port `8080` is already in use, start the API on `8081` instead:

```bash
./mvnw -Dsmartcampus.demoMode=true -Dsmartcampus.baseUri=http://localhost:8081/api/v1/ exec:java
```

Default base URL:

```text
http://localhost:8080/api/v1
```

Fallback base URL:

```text
http://localhost:8081/api/v1
```

The demo-only `500` trigger is only available when `smartcampus.demoMode=true`.

## Demo Flow

### 1. Show discovery
```bash
curl -i http://localhost:8080/api/v1
```

What to point out:

- JSON discovery object
- version and contact metadata
- resource map for rooms and sensors

### 2. Show sample in-memory data
```bash
curl -i http://localhost:8080/api/v1/rooms
curl -i http://localhost:8080/api/v1/sensors
```

### 3. Create a room and confirm `201 Created`
```bash
curl -i -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{
    "id": "SCI-201",
    "name": "Science Lab",
    "capacity": 40
  }'
```

What to point out:

- `201 Created`
- `Location` header
- created room JSON body

### 4. Get the new room by ID
```bash
curl -i http://localhost:8080/api/v1/rooms/SCI-201
```

### 5. Delete an empty room successfully
```bash
curl -i -X DELETE http://localhost:8080/api/v1/rooms/SCI-201
```

### 6. Recreate the room for the remaining sensor scenarios
```bash
curl -i -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{
    "id": "SCI-201",
    "name": "Science Lab",
    "capacity": 40
  }'
```

### 7. Create a valid sensor
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

### 8. Show filtered retrieval
```bash
curl -i "http://localhost:8080/api/v1/sensors?type=CO2"
curl -i "http://localhost:8080/api/v1/sensors?type=co2"
```

### 9. Show nested readings and parent update
```bash
curl -i -X POST http://localhost:8080/api/v1/sensors/CO2-002/readings \
  -H "Content-Type: application/json" \
  -d '{
    "value": 23.7
  }'

curl -i http://localhost:8080/api/v1/sensors/CO2-002/readings
curl -i http://localhost:8080/api/v1/sensors/CO2-002
```

What to point out:

- readings are stored historically
- parent sensor `currentValue` changes after posting a reading

### 10. Show `409 Conflict` for deleting a room with sensors
```bash
curl -i -X DELETE http://localhost:8080/api/v1/rooms/SCI-201
```

### 11. Show `422 Unprocessable Entity` for a non-existent linked room
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

### 12. Show `403 Forbidden` for maintenance mode
```bash
curl -i -X POST http://localhost:8080/api/v1/sensors \
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

### 13. Show `415 Unsupported Media Type`
```bash
curl -i -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: text/plain" \
  --data 'hello'
```

### 14. Show a clean `500 Internal Server Error`
```bash
curl -i http://localhost:8080/api/v1/debug/force-500
```

What to point out:

- the response is structured JSON
- the message is generic
- no stack trace is exposed to the client

## Final Checks Before Recording

- Use the same base URL consistently throughout the demo.
- Keep the terminal visible so the request/response logging can be seen.
- If you switch to the `8081` fallback, update every curl command accordingly.
- Stop the server cleanly at the end of the recording.
