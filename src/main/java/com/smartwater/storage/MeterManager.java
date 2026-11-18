package com.smartwater.storage;

import com.smartwater.database.DatabaseConnection;
import com.smartwater.database.DatabaseSchema;
import com.smartwater.model.Meter;
import com.smartwater.model.MeterReading;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Persists meters and cumulative meter readings.
 */
public class MeterManager {
    private static final Logger logger = LoggerFactory.getLogger(MeterManager.class);
    private static MeterManager instance;

    private final DatabaseConnection dbConnection;

    private MeterManager() {
        this.dbConnection = DatabaseConnection.getInstance();
        new DatabaseSchema(dbConnection).initialize();
    }

    public static synchronized MeterManager getInstance() {
        if (instance == null) {
            instance = new MeterManager();
        }
        return instance;
    }

    public Meter getOrCreateDefaultMeter() {
        return getOrCreateDefaultMeter(0);
    }

    public Meter getOrCreateDefaultMeter(int userId) {
        List<Meter> meters = getActiveMeters(userId);
        if (!meters.isEmpty()) {
            return meters.get(0);
        }
        return createMeter(new Meter("Main Meter", "Household inlet"), userId);
    }

    public Meter createMeter(Meter meter) {
        return createMeter(meter, 0);
    }

    public Meter createMeter(Meter meter, int userId) {
        try {
            String sql = """
                        INSERT INTO meters (user_id, name, location, active)
                        VALUES (?, ?, ?, ?)
                    """;
            long id = dbConnection.executeInsertReturnKey(sql, userId, meter.getName(), meter.getLocation(),
                    meter.isActive() ? 1 : 0);
            logger.info("Created meter: {}", meter.getName());
            return new Meter((int) id, meter.getName(), meter.getLocation(), meter.isActive(), LocalDateTime.now());
        } catch (SQLException e) {
            logger.error("Failed to create meter: {}", e.getMessage(), e);
            Optional<Meter> existing = getMeterByName(meter.getName(), userId);
            if (existing.isPresent()) {
                return existing.get();
            }
            throw new RuntimeException("Failed to create meter", e);
        }
    }

    public List<Meter> getActiveMeters() {
        return getActiveMeters(0);
    }

    public List<Meter> getActiveMeters(int userId) {
        List<Meter> meters = new ArrayList<>();
        try {
            String sql = "SELECT id, name, location, active, created_at FROM meters WHERE user_id = ? AND active = 1 ORDER BY id";
            try (ResultSet rs = dbConnection.executeQuery(sql, userId)) {
                while (rs.next()) {
                    meters.add(extractMeter(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to load meters: {}", e.getMessage(), e);
        }
        return meters;
    }

    public Optional<Meter> getMeterByName(String name) {
        return getMeterByName(name, 0);
    }

    public Optional<Meter> getMeterByName(String name, int userId) {
        try {
            String sql = "SELECT id, name, location, active, created_at FROM meters WHERE user_id = ? AND name = ?";
            try (ResultSet rs = dbConnection.executeQuery(sql, userId, name)) {
                if (rs.next()) {
                    return Optional.of(extractMeter(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to load meter by name: {}", e.getMessage(), e);
        }
        return Optional.empty();
    }

    public Optional<MeterReading> getLatestReading(int meterId) {
        return getLatestReading(meterId, 0);
    }

    public Optional<MeterReading> getLatestReading(int meterId, int userId) {
        try {
            String sql = """
                        SELECT id, meter_id, reading_time, reading_liters, note
                        FROM meter_readings
                        WHERE user_id = ? AND meter_id = ?
                        ORDER BY reading_time DESC
                        LIMIT 1
                    """;
            try (ResultSet rs = dbConnection.executeQuery(sql, userId, meterId)) {
                if (rs.next()) {
                    return Optional.of(extractReading(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to load latest meter reading: {}", e.getMessage(), e);
        }
        return Optional.empty();
    }

    public MeterReading saveReading(MeterReading reading) {
        return saveReading(reading, 0);
    }

    public MeterReading saveReading(MeterReading reading, int userId) {
        try {
            String sql = """
                        INSERT INTO meter_readings (user_id, meter_id, reading_time, reading_liters, note)
                        VALUES (?, ?, ?, ?, ?)
                    """;
            long id = dbConnection.executeInsertReturnKey(sql, userId, reading.getMeterId(),
                    Timestamp.valueOf(reading.getReadingTime()), reading.getReadingLiters(), reading.getNote());
            logger.info("Saved meter reading for meter {} at {}", reading.getMeterId(), reading.getReadingTime());
            return new MeterReading((int) id, reading.getMeterId(), reading.getReadingTime(),
                    reading.getReadingLiters(), reading.getNote());
        } catch (SQLException e) {
            logger.error("Failed to save meter reading: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save meter reading", e);
        }
    }

    public List<MeterReading> getReadings(int meterId) {
        return getReadings(meterId, 0);
    }

    public List<MeterReading> getReadings(int meterId, int userId) {
        List<MeterReading> readings = new ArrayList<>();
        try {
            String sql = """
                        SELECT id, meter_id, reading_time, reading_liters, note
                        FROM meter_readings
                        WHERE user_id = ? AND meter_id = ?
                        ORDER BY reading_time DESC
                    """;
            try (ResultSet rs = dbConnection.executeQuery(sql, userId, meterId)) {
                while (rs.next()) {
                    readings.add(extractReading(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to load meter readings: {}", e.getMessage(), e);
        }
        return readings;
    }

    private Meter extractMeter(ResultSet rs) throws SQLException {
        Timestamp createdAt = rs.getTimestamp("created_at");
        return new Meter(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("location"),
                rs.getInt("active") == 1,
                createdAt == null ? null : createdAt.toLocalDateTime());
    }

    private MeterReading extractReading(ResultSet rs) throws SQLException {
        Timestamp readingTime = rs.getTimestamp("reading_time");
        return new MeterReading(
                rs.getInt("id"),
                rs.getInt("meter_id"),
                readingTime.toLocalDateTime(),
                rs.getDouble("reading_liters"),
                rs.getString("note"));
    }
}
