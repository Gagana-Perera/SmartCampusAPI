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
 * Uses thread-safe collections to handle concurrent API requests.
 */
public class DataStore {

    // Storage maps
    private static final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private static final Map<String, Sensor> sensors = new ConcurrentHashMap<>();
    private static final Map<String, List<SensorReading>> sensorReadings = new ConcurrentHashMap<>();

    // Static initializer for sample data
    static {
        resetToDefaults();
    }

    private DataStore() {
    }

    public static synchronized void resetToDefaults() {
        rooms.clear();
        sensors.clear();
        sensorReadings.clear();

        // Preload sample room
        Room lib301 = new Room("LIB-301", "Library Quiet Study", 50);
        addRoom(lib301);

        Room eng101 = new Room("ENG-101", "Engineering Lab", 30);
        addRoom(eng101);

        // Preload sample sensor
        Sensor temp001 = new Sensor("TEMP-001", "Temperature", "ACTIVE", "LIB-301");
        addSensor(temp001);
    }

    // Room Operations
    public static List<Room> getAllRooms() {
        return rooms.values().stream()
                .sorted(Comparator.comparing(Room::getId))
                .collect(Collectors.toList());
    }

    public static Room getRoom(String id) {
        return rooms.get(id);
    }

    public static void addRoom(Room room) {
        room.setSensorIds(room.getSensorIds());
        rooms.put(room.getId(), room);
    }

    public static void deleteRoom(String id) {
        rooms.remove(id);
    }

    public static boolean roomExists(String id) {
        return rooms.containsKey(id);
    }

    public static boolean roomHasSensors(String id) {
        Room room = rooms.get(id);
        return room != null && room.getSensorIds() != null && !room.getSensorIds().isEmpty();
    }

    // Sensor Operations
    public static List<Sensor> getAllSensors() {
        return sensors.values().stream()
                .sorted(Comparator.comparing(Sensor::getId))
                .collect(Collectors.toList());
    }

    public static List<Sensor> getSensorsByType(String type) {
        return sensors.values().stream()
                .filter(s -> s.getType() != null && s.getType().equalsIgnoreCase(type))
                .sorted(Comparator.comparing(Sensor::getId))
                .collect(Collectors.toList());
    }

    public static Sensor getSensor(String id) {
        return sensors.get(id);
    }

    public static void addSensor(Sensor sensor) {
        sensors.put(sensor.getId(), sensor);

        // Link to room
        Room room = rooms.get(sensor.getRoomId());
        if (room != null && !room.getSensorIds().contains(sensor.getId())) {
            room.getSensorIds().add(sensor.getId());
        }

        // Initialize readings container
        sensorReadings.putIfAbsent(sensor.getId(), Collections.synchronizedList(new ArrayList<>()));
    }

    public static boolean sensorExists(String id) {
        return sensors.containsKey(id);
    }

    // Reading Operations
    public static List<SensorReading> getReadingsForSensor(String sensorId) {
        List<SensorReading> readings = sensorReadings.get(sensorId);
        if (readings == null) {
            return new ArrayList<>();
        }

        synchronized (readings) {
            return new ArrayList<>(readings);
        }
    }

    public static void addReading(String sensorId, SensorReading reading) {
        List<SensorReading> readings = sensorReadings.computeIfAbsent(
                sensorId, key -> Collections.synchronizedList(new ArrayList<>())
        );
        synchronized (readings) {
            readings.add(reading);
        }

        // Update parent sensor's current value
        Sensor sensor = sensors.get(sensorId);
        if (sensor != null) {
            sensor.setCurrentValue(reading.getValue());
        }
    }
}
