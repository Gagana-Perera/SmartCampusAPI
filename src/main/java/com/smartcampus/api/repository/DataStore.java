package com.smartcampus.api.repository;

import com.smartcampus.api.model.Room;
import com.smartcampus.api.model.Sensor;
import com.smartcampus.api.model.SensorReading;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory repository for storing campus data.
 * This class acts as the single source of truth for the application.
 * 
 * Thread-safety is achieved using:
 * 1. ConcurrentHashMap for high-concurrency access to rooms and sensors.
 * 2. Collections.synchronizedList for sensor reading histories.
 * 3. Atomic/Synchronized access patterns where multiple operations must be atomic.
 */
public class DataStore {

    // Storage maps using ConcurrentHashMap for thread-safe access
    private static final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private static final Map<String, Sensor> sensors = new ConcurrentHashMap<>();
    
    // Each sensor ID maps to a synchronized list of readings to maintain history order safely
    private static final Map<String, List<SensorReading>> sensorReadings = new ConcurrentHashMap<>();

    // Static initializer to ensure predictable state on startup
    static {
        resetToDefaults();
    }

    private DataStore() {
        // Private constructor to prevent instantiation of utility class
    }

    /**
     * Resets the data store to a known baseline state.
     * Useful for testing and ensuring the API has data for demonstration.
     */
    public static synchronized void resetToDefaults() {
        rooms.clear();
        sensors.clear();
        sensorReadings.clear();

        // Seed initial data for coursework demonstration
        Room lib301 = new Room("LIB-301", "Library Quiet Study", 50);
        addRoom(lib301);

        Room eng101 = new Room("ENG-101", "Engineering Lab", 30);
        addRoom(eng101);

        Sensor temp001 = new Sensor("TEMP-001", "Temperature", "ACTIVE", "LIB-301");
        addSensor(temp001);
    }

    // --- Room Operations ---

    /**
     * Retrieves all rooms sorted by their ID.
     * @return List of all rooms.
     */
    public static List<Room> getAllRooms() {
        return rooms.values().stream()
                .sorted(Comparator.comparing(Room::getId))
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a specific room by ID.
     * @param id The room ID.
     * @return The Room object or null if not found.
     */
    public static Room getRoom(String id) {
        return rooms.get(id);
    }

    /**
     * Adds or updates a room in the store.
     * @param room The room to add.
     */
    public static void addRoom(Room room) {
        // Ensure the list is properly initialized as a synchronized list
        room.setSensorIds(room.getSensorIds());
        rooms.put(room.getId(), room);
    }

    /**
     * Deletes a room from the store.
     * @param id The room ID.
     */
    public static void deleteRoom(String id) {
        rooms.remove(id);
    }

    /**
     * Checks if a room exists.
     * @param id The room ID.
     * @return true if exists, false otherwise.
     */
    public static boolean roomExists(String id) {
        return rooms.containsKey(id);
    }

    /**
     * Business Logic Check: Verifies if a room still has sensors assigned.
     * Used for 409 Conflict validation during deletion.
     * @param id The room ID.
     * @return true if sensors are assigned, false otherwise.
     */
    public static boolean roomHasSensors(String id) {
        Room room = rooms.get(id);
        return room != null && room.getSensorIds() != null && !room.getSensorIds().isEmpty();
    }

    // --- Sensor Operations ---

    /**
     * Retrieves all sensors across the campus.
     * @return List of all sensors.
     */
    public static List<Sensor> getAllSensors() {
        return sensors.values().stream()
                .sorted(Comparator.comparing(Sensor::getId))
                .collect(Collectors.toList());
    }

    /**
     * Filters sensors by their type (case-insensitive).
     * @param type The type to filter by (e.g., "Temperature").
     * @return List of matching sensors.
     */
    public static List<Sensor> getSensorsByType(String type) {
        return sensors.values().stream()
                .filter(s -> s.getType() != null && s.getType().equalsIgnoreCase(type))
                .sorted(Comparator.comparing(Sensor::getId))
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a specific sensor by ID.
     * @param id The sensor ID.
     * @return The Sensor object or null if not found.
     */
    public static Sensor getSensor(String id) {
        return sensors.get(id);
    }

    /**
     * Adds a sensor and establishes the bidirectional link with the room.
     * @param sensor The sensor to add.
     */
    public static void addSensor(Sensor sensor) {
        sensors.put(sensor.getId(), sensor);

        // Link the sensor ID to the Room's internal list
        Room room = rooms.get(sensor.getRoomId());
        if (room != null && !room.getSensorIds().contains(sensor.getId())) {
            room.getSensorIds().add(sensor.getId());
        }

        // Initialize empty readings history for the new sensor
        sensorReadings.putIfAbsent(sensor.getId(), Collections.synchronizedList(new ArrayList<>()));
    }

    /**
     * Checks if a sensor exists.
     * @param id The sensor ID.
     * @return true if exists, false otherwise.
     */
    public static boolean sensorExists(String id) {
        return sensors.containsKey(id);
    }

    // --- Reading Operations ---

    /**
     * Retrieves the history of readings for a specific sensor.
     * @param sensorId The sensor ID.
     * @return List of readings (defensive copy for thread-safety).
     */
    public static List<SensorReading> getReadingsForSensor(String sensorId) {
        List<SensorReading> readings = sensorReadings.get(sensorId);
        if (readings == null) {
            return new ArrayList<>();
        }

        // Return a copy to prevent ConcurrentModificationException during iteration
        synchronized (readings) {
            return new ArrayList<>(readings);
        }
    }

    /**
     * Records a new reading and updates the current value of the sensor.
     * @param sensorId The sensor ID.
     * @param reading The new reading.
     */
    public static void addReading(String sensorId, SensorReading reading) {
        List<SensorReading> readings = sensorReadings.computeIfAbsent(
                sensorId, key -> Collections.synchronizedList(new ArrayList<>())
        );
        synchronized (readings) {
            readings.add(reading);
        }

        // Update the 'currentValue' cache on the sensor object for quick retrieval
        Sensor sensor = sensors.get(sensorId);
        if (sensor != null) {
            sensor.setCurrentValue(reading.getValue());
        }
    }
}
