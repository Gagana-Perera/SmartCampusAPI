package com.smartcampus.api.repository;

import com.smartcampus.api.model.Room;
import com.smartcampus.api.model.Sensor;
import com.smartcampus.api.model.SensorReading;

import java.util.*;
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
        // Preload sample room
        Room lib301 = new Room("LIB-301", "Library Quiet Study", 50);
        rooms.put(lib301.getId(), lib301);

        Room eng101 = new Room("ENG-101", "Engineering Lab", 30);
        rooms.put(eng101.getId(), eng101);

        // Preload sample sensor
        Sensor temp001 = new Sensor("TEMP-001", "Temperature", "ACTIVE", "LIB-301");
        sensors.put(temp001.getId(), temp001);
        lib301.getSensorIds().add(temp001.getId());
        
        // Initialize readings list for the sensor
        sensorReadings.put(temp001.getId(), Collections.synchronizedList(new ArrayList<>()));
    }

    // Room Operations
    public static List<Room> getAllRooms() {
        return new ArrayList<>(rooms.values());
    }

    public static Room getRoom(String id) {
        return rooms.get(id);
    }

    public static void addRoom(Room room) {
        rooms.put(room.getId(), room);
    }

    public static void deleteRoom(String id) {
        rooms.remove(id);
    }

    public static boolean roomExists(String id) {
        return rooms.containsKey(id);
    }

    // Sensor Operations
    public static List<Sensor> getAllSensors() {
        return new ArrayList<>(sensors.values());
    }

    public static List<Sensor> getSensorsByType(String type) {
        return sensors.values().stream()
                .filter(s -> s.getType().equalsIgnoreCase(type))
                .collect(Collectors.toList());
    }

    public static Sensor getSensor(String id) {
        return sensors.get(id);
    }

    public static void addSensor(Sensor sensor) {
        sensors.put(sensor.getId(), sensor);
        // Link to room
        Room room = rooms.get(sensor.getRoomId());
        if (room != null) {
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
        return sensorReadings.getOrDefault(sensorId, new ArrayList<>());
    }

    public static void addReading(String sensorId, SensorReading reading) {
        List<SensorReading> readings = sensorReadings.computeIfAbsent(sensorId, 
            k -> Collections.synchronizedList(new ArrayList<>()));
        readings.add(reading);
        
        // Update parent sensor's current value
        Sensor sensor = sensors.get(sensorId);
        if (sensor != null) {
            sensor.setCurrentValue(reading.getValue());
        }
    }
}
