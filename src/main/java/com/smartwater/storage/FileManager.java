package com.smartwater.storage;

import com.smartwater.model.WaterRecord;
import com.smartwater.database.DatabaseConnection;
import com.smartwater.database.DatabaseSchema;
import com.smartwater.validation.ValidationException;
import com.smartwater.validation.ValidationUtil;
import com.smartwater.cache.CacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles water records storage using SQLite database.
 * Provides fallback to CSV for backward compatibility and caching for
 * performance.
 */
public class FileManager {
    private static final Logger logger = LoggerFactory.getLogger(FileManager.class);
    private static final String FILE_PATH = "data/records.csv";
    private static FileManager instance;
    private DatabaseConnection dbConnection;
    private CacheManager cacheManager;

    /**
     * Private constructor for singleton pattern.
     */
    private FileManager() {
        this.dbConnection = DatabaseConnection.getInstance();
        this.cacheManager = CacheManager.getInstance();

        // Initialize database schema if needed
        DatabaseSchema schema = new DatabaseSchema(dbConnection);
        schema.initialize();

        logger.info("FileManager initialized with SQLite database");
    }

    /**
     * Gets the singleton instance of FileManager.
     *
     * @return the FileManager instance
     */
    public static synchronized FileManager getInstance() {
        if (instance == null) {
            instance = new FileManager();
        }
        return instance;
    }

    /**
     * Appends a new water record to the database.
     *
     * @param record the WaterRecord to save
     */
    public void saveRecord(WaterRecord record) {
        saveRecord(record, 0);
    }

    public void saveRecord(WaterRecord record, int userId) {
        try {
            // Validate record before saving
            ValidationUtil.validateWaterRecord(record);

            logger.info("Saving water record for date: {}", record.getDate());

            // Check if record already exists for this date
            String checkSql = "SELECT id FROM water_records WHERE user_id = ? AND record_date = ?";
            try (ResultSet rs = dbConnection.executeQuery(checkSql, userId, record.getDate().toString())) {
                if (rs.next()) {
                    // Update existing record
                    String updateSql = """
                                UPDATE water_records
                                SET liters_used = ?, note = ?, updated_at = CURRENT_TIMESTAMP
                                WHERE user_id = ? AND record_date = ?
                            """;
                    dbConnection.executeUpdate(updateSql,
                            record.getLitersUsed(),
                            record.getNote(),
                            userId,
                            record.getDate().toString());
                    logger.info("Updated water record for date: {}", record.getDate());
                } else {
                    // Insert new record
                    String insertSql = """
                                INSERT INTO water_records (user_id, record_date, liters_used, note)
                                VALUES (?, ?, ?, ?)
                            """;
                    dbConnection.executeUpdate(insertSql,
                            userId,
                            record.getDate().toString(),
                            record.getLitersUsed(),
                            record.getNote());
                    logger.info("Created new water record for date: {}", record.getDate());
                }
            }

            // Invalidate caches
            cacheManager.invalidateRecordCache();
            cacheManager.invalidateAnalyticsCache();

        } catch (ValidationException e) {
            logger.error("Validation error while saving record: {}", e.getMessage());
        } catch (SQLException e) {
            logger.error("Database error while saving record: {}", e.getMessage(), e);
        }
    }

    /**
     * Reads all water records from the database.
     *
     * @return List of WaterRecord objects
     */
    public List<WaterRecord> loadAllRecords() {
        return loadAllRecords(0);
    }

    public List<WaterRecord> loadAllRecords(int userId) {
        List<WaterRecord> records = new ArrayList<>();

        try {
            // Try to load from database first
            String sql = "SELECT record_date, liters_used, note FROM water_records WHERE user_id = ? ORDER BY record_date DESC";
            try (ResultSet rs = dbConnection.executeQuery(sql, userId)) {
                while (rs.next()) {
                    LocalDate date = LocalDate.parse(rs.getString("record_date"));
                    double litersUsed = rs.getDouble("liters_used");
                    String note = rs.getString("note");

                    WaterRecord record = new WaterRecord(date, litersUsed, note);
                    records.add(record);
                }
                logger.info("Loaded {} water records from database", records.size());
            }

            // Only legacy/global mode imports the old CSV. New authenticated users
            // should start with their own empty records instead of inheriting sample data.
            if (userId == 0 && records.isEmpty() && Files.exists(Paths.get(FILE_PATH))) {
                logger.info("No records in database. Attempting to load from legacy CSV file...");
                records = loadRecordsFromLegacyCSV();

                // Migrate records to database
                if (!records.isEmpty()) {
                    for (WaterRecord record : records) {
                        try {
                            saveRecord(record, userId);
                        } catch (Exception e) {
                            logger.warn("Failed to migrate record: {}", record.getDate(), e);
                        }
                    }
                    logger.info("Migrated {} records from CSV to database", records.size());
                }
            }

            return records;

        } catch (SQLException e) {
            logger.error("Database error while loading records: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Gets a water record for a specific date.
     *
     * @param date the date to query
     * @return WaterRecord if found, null otherwise
     */
    public WaterRecord getRecordByDate(LocalDate date) {
        return getRecordByDate(date, 0);
    }

    public WaterRecord getRecordByDate(LocalDate date, int userId) {
        try {
            String sql = "SELECT record_date, liters_used, note FROM water_records WHERE user_id = ? AND record_date = ?";
            try (ResultSet rs = dbConnection.executeQuery(sql, userId, date.toString())) {
                if (rs.next()) {
                    WaterRecord record = new WaterRecord(
                            LocalDate.parse(rs.getString("record_date")),
                            rs.getDouble("liters_used"),
                            rs.getString("note"));
                    logger.debug("Retrieved record for date: {}", date);
                    return record;
                }
            }
        } catch (SQLException e) {
            logger.error("Database error while retrieving record for date {}: {}", date, e.getMessage(), e);
        }

        return null;
    }

    /**
     * Loads records from legacy CSV file for backward compatibility.
     *
     * @return List of WaterRecord objects loaded from CSV
     */
    private List<WaterRecord> loadRecordsFromLegacyCSV() {
        List<WaterRecord> records = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_PATH))) {
            String line;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue; // Skip header row
                }

                String[] parts = line.split(",");

                try {
                    if (parts.length >= 2) {
                        LocalDate date = LocalDate.parse(parts[0].trim());
                        double litersUsed = Double.parseDouble(parts[1].trim());
                        String note = parts.length > 2 ? unescapeCsvField(parts[2]) : "";

                        WaterRecord record = new WaterRecord(date, litersUsed, note);
                        records.add(record);
                    }
                } catch (Exception e) {
                    logger.warn("Error parsing CSV line: {} - {}", line, e.getMessage());
                }
            }

            logger.info("Loaded {} records from legacy CSV file", records.size());

        } catch (IOException e) {
            logger.error("Error reading legacy CSV file: {}", e.getMessage(), e);
        }

        return records;
    }

    /**
     * Escapes CSV field by wrapping in quotes if needed.
     *
     * @param field the field value to escape
     * @return escaped field value
     */
    private static String escapeCsvField(String field) {
        if (field == null) {
            return "";
        }

        // If field contains comma, quote, or newline, wrap in quotes
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return field.replace("\"", "\"\"");
        }

        return field;
    }

    /**
     * Unescapes CSV field by removing doubled quotes.
     *
     * @param field the escaped field value
     * @return unescaped field value
     */
    private static String unescapeCsvField(String field) {
        if (field == null) {
            return "";
        }

        // Replace doubled quotes with single quotes
        return field.replace("\"\"", "\"");
    }
}
