#!/bin/bash

# Smart Campus API — Full Test Suite
# Base URI: http://localhost:8080/api/v1

BASE_URL="http://localhost:8080/api/v1"

echo "--------------------------------------------------"
echo "STEP 1: Create a new room (201 Created)"
echo "POST"
echo "$BASE_URL/rooms"
echo '{"id": "SCI-101", "name": "Science Lab 1", "capacity": 30}'
curl -s -X POST "$BASE_URL/rooms" -H "Content-Type: application/json" -d '{"id": "SCI-101", "name": "Science Lab 1", "capacity": 30}' | python3 -m json.tool
echo "--------------------------------------------------"

echo "STEP 2: Create a sensor in the new room (201 Created)"
echo "POST"
echo "$BASE_URL/sensors"
echo '{"id": "TEMP-999", "type": "Temperature", "status": "ACTIVE", "roomId": "SCI-101"}'
curl -s -X POST "$BASE_URL/sensors" -H "Content-Type: application/json" -d '{"id": "TEMP-999", "type": "Temperature", "status": "ACTIVE", "roomId": "SCI-101"}' | python3 -m json.tool
echo "--------------------------------------------------"

echo "STEP 3: Try to create a sensor with an invalid room (422 Unprocessable Entity)"
echo "POST"
echo "$BASE_URL/sensors"
echo '{"id": "HUM-001", "type": "Humidity", "status": "ACTIVE", "roomId": "NON-EXISTENT"}'
curl -s -X POST "$BASE_URL/sensors" -H "Content-Type: application/json" -d '{"id": "HUM-001", "type": "Humidity", "status": "ACTIVE", "roomId": "NON-EXISTENT"}' | python3 -m json.tool
echo "--------------------------------------------------"

echo "STEP 4: Try to delete a room that has sensors (409 Conflict)"
echo "DELETE"
echo "$BASE_URL/rooms/SCI-101"
curl -s -X DELETE "$BASE_URL/rooms/SCI-101" | python3 -m json.tool
echo "--------------------------------------------------"

echo "STEP 5: Record a valid reading (201 Created)"
echo "POST"
echo "$BASE_URL/sensors/TEMP-999/readings"
echo '{"value": 24.5}'
curl -s -X POST "$BASE_URL/sensors/TEMP-999/readings" -H "Content-Type: application/json" -d '{"value": 24.5}' | python3 -m json.tool
echo "--------------------------------------------------"

echo "STEP 6: Create a MAINTENANCE sensor"
echo "POST"
echo "$BASE_URL/sensors"
echo '{"id": "MAIN-001", "type": "Maintenance", "status": "MAINTENANCE", "roomId": "SCI-101"}'
curl -s -X POST "$BASE_URL/sensors" -H "Content-Type: application/json" -d '{"id": "MAIN-001", "type": "Maintenance", "status": "MAINTENANCE", "roomId": "SCI-101"}' | python3 -m json.tool
echo "--------------------------------------------------"

echo "STEP 7: Try to post a reading to a MAINTENANCE sensor (403 Forbidden)"
echo "POST"
echo "$BASE_URL/sensors/MAIN-001/readings"
echo '{"value": 10.0}'
curl -s -X POST "$BASE_URL/sensors/MAIN-001/readings" -H "Content-Type: application/json" -d '{"value": 10.0}' | python3 -m json.tool
echo "--------------------------------------------------"

echo "STEP 8: Filter sensors by type (200 OK)"
echo "GET"
echo "$BASE_URL/sensors?type=Temperature"
curl -s "$BASE_URL/sensors?type=Temperature" | python3 -m json.tool
echo "--------------------------------------------------"

echo "STEP 9: Get reading history (200 OK)"
echo "GET"
echo "$BASE_URL/sensors/TEMP-999/readings"
curl -s "$BASE_URL/sensors/TEMP-999/readings" | python3 -m json.tool
echo "--------------------------------------------------"

echo "STEP 10: Delete a room without sensors (204 No Content)"
echo "DELETE"
echo "$BASE_URL/rooms/ENG-101"
curl -i -X DELETE "$BASE_URL/rooms/ENG-101"
echo "--------------------------------------------------"
