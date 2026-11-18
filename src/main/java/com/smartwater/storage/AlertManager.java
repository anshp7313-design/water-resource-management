package com.smartwater.storage;

import com.smartwater.model.AlertLog;
import com.smartwater.database.DatabaseConnection;
import com.smartwater.database.DatabaseSchema;
import com.smartwater.cache.CacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages AlertLog entries using SQLite database.
 * Provides fallback to CSV for backward compatibility and caching for
 * performance.
 */
public class AlertManager {
    private static final Logger logger = LoggerFactory.getLogger(AlertManager.class);
    private static final String FILE_PATH = "data/alerts.csv";
    private static AlertManager instance;
    private DatabaseConnection dbConnection;
    private CacheManager cacheManager;

    /**
     * Private constructor for singleton pattern.
     */
    private AlertManager() {
        this.dbConnection = DatabaseConnection.getInstance();
        this.cacheManager = CacheManager.getInstance();

        // Initialize database schema if needed
        DatabaseSchema schema = new DatabaseSchema(dbConnection);
        schema.initialize();

        logger.info("AlertManager initialized with SQLite database");
    }

    /**
     * Gets the singleton instance of AlertManager.
     *
     * @return the AlertManager instance
     */
    public static synchronized AlertManager getInstance() {
        if (instance == null) {
            instance = new AlertManager();
        }
        return instance;
    }

    /**
     * Saves an alert to the database.
     *
     * @param alert the alert to save
     * @return true if successful, false if error occurred
     */
    public boolean saveAlert(AlertLog alert) {
        return saveAlert(alert, 0);
    }

    public boolean saveAlert(AlertLog alert, int userId) {
        try {
            logger.info("Saving alert: {} - {}", alert.getType(), alert.getMessage());

            String sql = """
                        INSERT INTO alert_logs (user_id, alert_timestamp, message, alert_type)
                        VALUES (?, ?, ?, ?)
                    """;

            dbConnection.executeUpdate(sql,
                    userId,
                    alert.getTimestamp().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    alert.getMessage(),
                    alert.getType());

            logger.info("Alert saved successfully");

            // Invalidate cache
            cacheManager.invalidateAnalyticsCache();
            return true;

        } catch (SQLException e) {
            logger.error("Database error while saving alert: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Loads all alerts from the database.
     *
     * @return list of loaded alerts
     */
    public List<AlertLog> loadAlerts() {
        return loadAlerts(0);
    }

    public List<AlertLog> loadAlerts(int userId) {
        List<AlertLog> alerts = new ArrayList<>();

        try {
            // Try to load from database first
            String sql = "SELECT alert_timestamp, message, alert_type FROM alert_logs WHERE user_id = ? ORDER BY alert_timestamp DESC";
            try (ResultSet rs = dbConnection.executeQuery(sql, userId)) {
                while (rs.next()) {
                    LocalDateTime timestamp = LocalDateTime.parse(
                            rs.getString("alert_timestamp"),
                            DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                    String message = rs.getString("message");
                    String type = rs.getString("alert_type");

                    AlertLog alert = new AlertLog(timestamp, message, type);
                    alerts.add(alert);
                }
                logger.info("Loaded {} alerts from database", alerts.size());
            }

            // If no alerts found in database, try to load from legacy CSV file
            if (alerts.isEmpty() && Files.exists(Paths.get(FILE_PATH))) {
                logger.info("No alerts in database. Attempting to load from legacy CSV file...");
                alerts = loadAlertsFromLegacyCSV();

                // Migrate alerts to database
                if (!alerts.isEmpty()) {
                    for (AlertLog alert : alerts) {
                        try {
                            saveAlert(alert, userId);
                        } catch (Exception e) {
                            logger.warn("Failed to migrate alert: {}", alert.getTimestamp(), e);
                        }
                    }
                    logger.info("Migrated {} alerts from CSV to database", alerts.size());
                }
            }

            return alerts;

        } catch (SQLException e) {
            logger.error("Database error while loading alerts: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Gets the most recent N alerts.
     *
     * @param count number of recent alerts to return
     * @return list of recent alerts
     */
    public List<AlertLog> getRecentAlerts(int count) {
        return getRecentAlerts(count, 0);
    }

    public List<AlertLog> getRecentAlerts(int count, int userId) {
        List<AlertLog> allAlerts = loadAlerts(userId);

        if (allAlerts.size() <= count) {
            return allAlerts;
        }

        return allAlerts.subList(0, count);
    }

    /**
     * Clears all alerts from the database.
     *
     * @return true if successful, false if error occurred
     */
    public boolean clearAlerts() {
        return clearAlerts(0);
    }

    public boolean clearAlerts(int userId) {
        try {
            logger.info("Clearing all alerts");

            String sql = "DELETE FROM alert_logs WHERE user_id = ?";
            dbConnection.executeUpdate(sql, userId);

            logger.info("All alerts cleared successfully");

            // Invalidate cache
            cacheManager.invalidateAnalyticsCache();
            return true;

        } catch (SQLException e) {
            logger.error("Database error while clearing alerts: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Loads alerts from legacy CSV file for backward compatibility.
     *
     * @return list of alerts loaded from CSV
     */
    private List<AlertLog> loadAlertsFromLegacyCSV() {
        List<AlertLog> alerts = new ArrayList<>();

        try {
            String content = new String(Files.readAllBytes(Paths.get(FILE_PATH)));
            String[] lines = content.split("\n");

            // Skip header row
            for (int i = 1; i < lines.length; i++) {
                String line = lines[i].trim();
                if (line.isEmpty()) {
                    continue;
                }

                try {
                    AlertLog alert = parseAlertLine(line);
                    if (alert != null) {
                        alerts.add(alert);
                    }
                } catch (Exception e) {
                    logger.warn("Error parsing alert line: {}", line, e);
                }
            }

            logger.info("Loaded {} alerts from legacy CSV file", alerts.size());

        } catch (IOException e) {
            logger.error("Error reading legacy CSV file: {}", e.getMessage(), e);
        }

        return alerts;
    }

    /**
     * Parses a single CSV line into AlertLog object.
     *
     * @param line the CSV line to parse
     * @return parsed AlertLog, or null if parsing fails
     */
    private AlertLog parseAlertLine(String line) {
        try {
            String[] parts = line.split(",", 3);

            if (parts.length < 3) {
                return null;
            }

            LocalDateTime timestamp = LocalDateTime.parse(parts[0].trim(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            String type = parts[1].trim();
            String message = unescapeCsvField(parts[2]);

            return new AlertLog(timestamp, message, type);

        } catch (Exception e) {
            logger.warn("Error parsing alert line: {}", line, e);
            return null;
        }
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

        return field.replace("\"\"", "\"");
    }
}
