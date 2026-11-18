package com.smartwater.settings;

import com.smartwater.database.DatabaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Application settings manager for storing and retrieving configuration.
 */
public class SettingsManager {
    private static final Logger logger = LoggerFactory.getLogger(SettingsManager.class);
    private static SettingsManager instance;
    private DatabaseConnection dbConnection;
    private final Map<String, String> settingsCache = new HashMap<>();

    /**
     * Settings keys.
     */
    public static class SettingKey {
        // Application
        public static final String APP_LANGUAGE = "app.language";
        public static final String APP_THEME = "app.theme";
        public static final String APP_NOTIFICATION_ENABLED = "app.notification.enabled";

        // Backup
        public static final String BACKUP_AUTO_ENABLED = "backup.auto.enabled";
        public static final String BACKUP_AUTO_INTERVAL_MINUTES = "backup.auto.interval.minutes";
        public static final String BACKUP_RETENTION_DAYS = "backup.retention.days";

        // Notifications
        public static final String NOTIFY_THRESHOLD_ALERT = "notify.threshold.alert";
        public static final String NOTIFY_ANOMALY = "notify.anomaly";
        public static final String NOTIFY_BACKUP = "notify.backup";

        // UI Preferences
        public static final String UI_AUTO_REFRESH = "ui.auto.refresh";
        public static final String UI_REFRESH_INTERVAL_SECONDS = "ui.refresh.interval.seconds";

        // Data
        public static final String DATA_EXPORT_DEFAULT_FORMAT = "data.export.default.format";

        // Analytics
        public static final String ANALYTICS_SHOW_ANOMALIES = "analytics.show.anomalies";
        public static final String ANALYTICS_PREDICTION_DAYS = "analytics.prediction.days";
    }

    /**
     * Private constructor for singleton pattern.
     */
    private SettingsManager() {
        this.dbConnection = DatabaseConnection.getInstance();
        loadDefaultSettings();
        logger.info("SettingsManager initialized");
    }

    /**
     * Gets the singleton instance.
     *
     * @return the SettingsManager instance
     */
    public static synchronized SettingsManager getInstance() {
        if (instance == null) {
            instance = new SettingsManager();
        }
        return instance;
    }

    /**
     * Gets a setting value.
     *
     * @param key          the setting key
     * @param defaultValue the default value if not found
     * @return the setting value
     */
    public String getSetting(String key, String defaultValue) {
        // First check cache
        if (settingsCache.containsKey(key)) {
            return settingsCache.get(key);
        }

        // Then check database
        try {
            String sql = "SELECT value FROM settings WHERE key = ?";
            try (ResultSet rs = dbConnection.executeQuery(sql, key)) {
                if (rs.next()) {
                    String value = rs.getString("value");
                    settingsCache.put(key, value);
                    return value;
                }
            }
        } catch (SQLException e) {
            logger.error("Error retrieving setting: {}", e.getMessage(), e);
        }

        // Return default value
        settingsCache.put(key, defaultValue);
        return defaultValue;
    }

    /**
     * Gets a setting as boolean.
     *
     * @param key          the setting key
     * @param defaultValue the default value
     * @return the boolean value
     */
    public boolean getSettingBoolean(String key, boolean defaultValue) {
        String value = getSetting(key, String.valueOf(defaultValue));
        return Boolean.parseBoolean(value);
    }

    /**
     * Gets a setting as integer.
     *
     * @param key          the setting key
     * @param defaultValue the default value
     * @return the integer value
     */
    public int getSettingInt(String key, int defaultValue) {
        try {
            String value = getSetting(key, String.valueOf(defaultValue));
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            logger.warn("Invalid integer setting {}: {}", key, e.getMessage());
            return defaultValue;
        }
    }

    /**
     * Gets a setting as double.
     *
     * @param key          the setting key
     * @param defaultValue the default value
     * @return the double value
     */
    public double getSettingDouble(String key, double defaultValue) {
        try {
            String value = getSetting(key, String.valueOf(defaultValue));
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            logger.warn("Invalid double setting {}: {}", key, e.getMessage());
            return defaultValue;
        }
    }

    /**
     * Sets a setting value.
     *
     * @param key   the setting key
     * @param value the value to set
     */
    public void setSetting(String key, String value) {
        try {
            // Check if setting exists
            String checkSql = "SELECT id FROM settings WHERE key = ?";
            try (ResultSet rs = dbConnection.executeQuery(checkSql, key)) {
                if (rs.next()) {
                    // Update existing setting
                    String updateSql = "UPDATE settings SET value = ?, updated_at = CURRENT_TIMESTAMP WHERE key = ?";
                    dbConnection.executeUpdate(updateSql, value, key);
                } else {
                    // Insert new setting
                    String insertSql = "INSERT INTO settings (key, value) VALUES (?, ?)";
                    dbConnection.executeUpdate(insertSql, key, value);
                }
            }

            // Update cache
            settingsCache.put(key, value);
            logger.info("Setting updated: {} = {}", key, value);

        } catch (SQLException e) {
            logger.error("Error setting value: {}", e.getMessage(), e);
        }
    }

    /**
     * Sets a setting from boolean.
     *
     * @param key   the setting key
     * @param value the boolean value
     */
    public void setSettingBoolean(String key, boolean value) {
        setSetting(key, String.valueOf(value));
    }

    /**
     * Sets a setting from integer.
     *
     * @param key   the setting key
     * @param value the integer value
     */
    public void setSettingInt(String key, int value) {
        setSetting(key, String.valueOf(value));
    }

    /**
     * Resets a setting to default.
     *
     * @param key the setting key
     */
    public void resetSetting(String key) {
        try {
            String sql = "DELETE FROM settings WHERE key = ?";
            dbConnection.executeUpdate(sql, key);
            settingsCache.remove(key);
            logger.info("Setting reset: {}", key);
        } catch (SQLException e) {
            logger.error("Error resetting setting: {}", e.getMessage(), e);
        }
    }

    /**
     * Gets all settings.
     *
     * @return map of all settings
     */
    public Map<String, String> getAllSettings() {
        Map<String, String> allSettings = new HashMap<>();

        try {
            String sql = "SELECT key, value FROM settings ORDER BY key";
            try (ResultSet rs = dbConnection.executeQuery(sql)) {
                while (rs.next()) {
                    allSettings.put(rs.getString("key"), rs.getString("value"));
                }
            }
        } catch (SQLException e) {
            logger.error("Error retrieving all settings: {}", e.getMessage(), e);
        }

        return allSettings;
    }

    /**
     * Resets all settings to defaults.
     */
    public void resetAllSettings() {
        try {
            String sql = "DELETE FROM settings";
            dbConnection.executeUpdate(sql);
            settingsCache.clear();
            loadDefaultSettings();
            logger.info("All settings reset to defaults");
        } catch (SQLException e) {
            logger.error("Error resetting all settings: {}", e.getMessage(), e);
        }
    }

    /**
     * Loads default settings.
     */
    private void loadDefaultSettings() {
        // Application
        setSetting(SettingKey.APP_LANGUAGE, "en");
        setSetting(SettingKey.APP_THEME, "light");
        setSetting(SettingKey.APP_NOTIFICATION_ENABLED, "true");

        // Backup
        setSetting(SettingKey.BACKUP_AUTO_ENABLED, "true");
        setSetting(SettingKey.BACKUP_AUTO_INTERVAL_MINUTES, "1440"); // Daily
        setSetting(SettingKey.BACKUP_RETENTION_DAYS, "30");

        // Notifications
        setSetting(SettingKey.NOTIFY_THRESHOLD_ALERT, "true");
        setSetting(SettingKey.NOTIFY_ANOMALY, "true");
        setSetting(SettingKey.NOTIFY_BACKUP, "true");

        // UI Preferences
        setSetting(SettingKey.UI_AUTO_REFRESH, "true");
        setSetting(SettingKey.UI_REFRESH_INTERVAL_SECONDS, "30");

        // Data
        setSetting(SettingKey.DATA_EXPORT_DEFAULT_FORMAT, "csv");

        // Analytics
        setSetting(SettingKey.ANALYTICS_SHOW_ANOMALIES, "true");
        setSetting(SettingKey.ANALYTICS_PREDICTION_DAYS, "90");
    }
}
