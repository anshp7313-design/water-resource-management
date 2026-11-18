package com.smartwater.storage;

import com.smartwater.model.HouseholdProfile;
import com.smartwater.database.DatabaseConnection;
import com.smartwater.database.DatabaseSchema;
import com.smartwater.validation.ValidationException;
import com.smartwater.validation.ValidationUtil;
import com.smartwater.cache.CacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Manages reading and writing HouseholdProfile using SQLite database.
 * Includes caching for improved performance.
 */
public class ProfileManager {
    private static final Logger logger = LoggerFactory.getLogger(ProfileManager.class);
    private static final String FILE_PATH = "data/profile.json";
    private static ProfileManager instance;
    private DatabaseConnection dbConnection;
    private CacheManager cacheManager;

    /**
     * Private constructor for singleton pattern.
     */
    private ProfileManager() {
        this.dbConnection = DatabaseConnection.getInstance();
        this.cacheManager = CacheManager.getInstance();

        // Initialize database schema if needed
        DatabaseSchema schema = new DatabaseSchema(dbConnection);
        schema.initialize();

        logger.info("ProfileManager initialized with SQLite database");
    }

    /**
     * Gets the singleton instance of ProfileManager.
     *
     * @return the ProfileManager instance
     */
    public static synchronized ProfileManager getInstance() {
        if (instance == null) {
            instance = new ProfileManager();
        }
        return instance;
    }

    /**
     * Saves HouseholdProfile to SQLite database.
     *
     * @param profile the profile to save
     * @return true if successful, false if error occurred
     */
    public boolean saveProfile(HouseholdProfile profile) {
        return saveProfile(profile, 0);
    }

    public boolean saveProfile(HouseholdProfile profile, int userId) {
        try {
            // Validate profile before saving
            ValidationUtil.validateHouseholdProfile(profile);

            logger.info("Saving household profile: {}", profile.getHouseholdName());

            // Check if profile exists for this user
            String checkSql = "SELECT id FROM household_profiles WHERE user_id = ?";
            try (ResultSet rs = dbConnection.executeQuery(checkSql, userId)) {
                if (rs.next()) {
                    // Update existing profile
                    String updateSql = """
                                UPDATE household_profiles
                                SET household_name = ?, member_count = ?, daily_limit = ?, city = ?,
                                    alert_at_70 = ?, alert_at_90 = ?, dark_mode = ?, updated_at = CURRENT_TIMESTAMP
                                WHERE user_id = ?
                            """;
                    dbConnection.executeUpdate(updateSql,
                            profile.getHouseholdName(),
                            profile.getMemberCount(),
                            profile.getDailyLimit(),
                            profile.getCity(),
                            profile.isAlertAt70() ? 1 : 0,
                            profile.isAlertAt90() ? 1 : 0,
                            profile.isDarkMode() ? 1 : 0,
                            userId);
                    logger.info("Updated household profile: {}", profile.getHouseholdName());
                } else {
                    // Insert new profile
                    String insertSql = """
                                INSERT INTO household_profiles (user_id, household_name, member_count, daily_limit, city, alert_at_70, alert_at_90, dark_mode)
                                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                            """;
                    dbConnection.executeUpdate(insertSql,
                            userId,
                            profile.getHouseholdName(),
                            profile.getMemberCount(),
                            profile.getDailyLimit(),
                            profile.getCity(),
                            profile.isAlertAt70() ? 1 : 0,
                            profile.isAlertAt90() ? 1 : 0,
                            profile.isDarkMode() ? 1 : 0);
                    logger.info("Created new household profile: {}", profile.getHouseholdName());
                }
            }

            // Invalidate cache
            cacheManager.invalidateProfileCache();
            return true;

        } catch (ValidationException e) {
            logger.error("Validation error while saving profile: {}", e.getMessage());
            return false;
        } catch (SQLException e) {
            logger.error("Database error while saving profile: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Loads HouseholdProfile from SQLite database.
     *
     * @return loaded profile, or default profile if none exists
     */
    public HouseholdProfile loadProfile() {
        return loadProfile(0);
    }

    public HouseholdProfile loadProfile(int userId) {
        try {
            // Try to load from database first
            String sql = "SELECT * FROM household_profiles WHERE user_id = ? LIMIT 1";
            try (ResultSet rs = dbConnection.executeQuery(sql, userId)) {
                if (rs.next()) {
                    HouseholdProfile profile = new HouseholdProfile(
                            rs.getString("household_name"),
                            rs.getInt("member_count"),
                            rs.getDouble("daily_limit"),
                            rs.getString("city"),
                            rs.getInt("alert_at_70") == 1,
                            rs.getInt("alert_at_90") == 1,
                            rs.getInt("dark_mode") == 1);

                    // Cache the profile
                    cacheManager.putProfileCache(profile.getHouseholdName(), profile);
                    logger.info("Loaded household profile from database: {}", profile.getHouseholdName());
                    return profile;
                }
            }

            // Only legacy/global mode imports the old JSON profile. Authenticated
            // users should complete setup with their own household profile.
            if (userId == 0 && Files.exists(Paths.get(FILE_PATH))) {
                logger.info("No profile in database. Attempting to load from legacy JSON file...");
                HouseholdProfile legacyProfile = loadProfileFromLegacyJson();
                if (legacyProfile != null) {
                    // Save the legacy profile to database
                    saveProfile(legacyProfile, userId);
                    logger.info("Migrated legacy profile to database");
                    return legacyProfile;
                }
            }

            // Return default profile (first launch)
            HouseholdProfile defaultProfile = new HouseholdProfile("My Home", 4, 100.0, "", true, true, false);
            logger.info("No profile found. Using default profile for first launch");
            return defaultProfile;

        } catch (SQLException e) {
            logger.error("Database error while loading profile: {}", e.getMessage(), e);
            return new HouseholdProfile("My Home", 4, 100.0, "", true, true, false);
        }
    }

    /**
     * Checks if this is the first application launch.
     *
     * @return true if no profile exists in database
     */
    public boolean isFirstLaunch() {
        return isFirstLaunch(0);
    }

    public boolean isFirstLaunch(int userId) {
        try {
            String sql = "SELECT COUNT(*) as count FROM household_profiles WHERE user_id = ?";
            try (ResultSet rs = dbConnection.executeQuery(sql, userId)) {
                if (rs.next()) {
                    boolean isFirst = rs.getInt("count") == 0;
                    logger.debug("First launch check: {}", isFirst);
                    return isFirst;
                }
            }
        } catch (SQLException e) {
            logger.error("Error checking first launch status: {}", e.getMessage(), e);
        }
        return true;
    }

    /**
     * Loads profile from legacy JSON file (for backward compatibility).
     *
     * @return HouseholdProfile loaded from JSON, or null if error
     */
    private HouseholdProfile loadProfileFromLegacyJson() {
        try {
            String content = new String(Files.readAllBytes(Paths.get(FILE_PATH)));
            HouseholdProfile profile = new HouseholdProfile("My Home", 4, 100.0, "", true, true, false);

            profile.setHouseholdName(extractFieldValue(content, "householdName"));
            profile.setMemberCount(parseInt(extractFieldValue(content, "memberCount")));
            profile.setDailyLimit(parseDouble(extractFieldValue(content, "dailyLimit")));
            profile.setCity(extractFieldValue(content, "city"));
            profile.setAlertAt70(parseBoolean(extractFieldValue(content, "alertAt70")));
            profile.setAlertAt90(parseBoolean(extractFieldValue(content, "alertAt90")));
            profile.setDarkMode(parseBoolean(extractFieldValue(content, "darkMode")));

            return profile;
        } catch (Exception e) {
            logger.error("Error loading legacy JSON profile: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Extracts field value from JSON content.
     * Looks for pattern: "key": "value"
     *
     * @param content: JSON content as string
     * @param key:     field name to extract
     * @return: extracted value or empty string if not found
     */
    private static String extractFieldValue(String content, String key) {
        String searchPattern = "\"" + key + "\":";
        int startIndex = content.indexOf(searchPattern);

        if (startIndex == -1) {
            return "";
        }

        startIndex += searchPattern.length();

        // Skip potential whitespace after colon
        while (startIndex < content.length() && Character.isWhitespace(content.charAt(startIndex))) {
            startIndex++;
        }

        // Handle quoted strings
        if (startIndex < content.length() && content.charAt(startIndex) == '"') {
            startIndex++; // Skip opening quote
            int endIndex = content.indexOf("\"", startIndex);
            if (endIndex == -1) {
                return "";
            }
            return content.substring(startIndex, endIndex);
        }

        // Handle unquoted values
        int endIndex = startIndex;
        while (endIndex < content.length() &&
                content.charAt(endIndex) != ',' &&
                content.charAt(endIndex) != '}' &&
                content.charAt(endIndex) != '\n') {
            endIndex++;
        }

        return content.substring(startIndex, endIndex).trim();
    }

    /**
     * Parses integer value from string.
     *
     * @param value: string to parse
     * @return: parsed integer or 0 if parsing fails
     */
    private static int parseInt(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Parses double value from string.
     *
     * @param value: string to parse
     * @return: parsed double or 0.0 if parsing fails
     */
    private static double parseDouble(String value) {
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    /**
     * Parses boolean value from string.
     *
     * @param value: string to parse
     * @return: parsed boolean or false if parsing fails
     */
    private static boolean parseBoolean(String value) {
        return "true".equals(value.trim());
    }
}
