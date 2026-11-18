package com.smartwater.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.*;

/**
 * Initializes and manages the database schema for SmartWater.
 * Creates all necessary tables on first run.
 */
public class DatabaseSchema {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseSchema.class);
    private final DatabaseConnection dbConnection;

    /**
     * Constructs a DatabaseSchema instance.
     *
     * @param dbConnection the database connection to use
     */
    public DatabaseSchema(DatabaseConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    /**
     * Initializes the database schema by creating all required tables.
     */
    public void initialize() {
        try {
            createUsersTable();
            createHouseholdProfileTable();
            createWaterRecordsTable();
            createMetersTable();
            createMeterReadingsTable();
            createTariffPlansTable();
            createAlertLogsTable();
            createLimitIncreaseRequestsTable();
            createSettingsTable();
            migrateUserScopedTables();
            logger.info("Database schema initialized successfully");
        } catch (SQLException e) {
            logger.error("Failed to initialize database schema", e);
            throw new RuntimeException("Failed to initialize database schema", e);
        }
    }

    /**
     * Creates the users table for multi-user authentication.
     */
    private void createUsersTable() throws SQLException {
        String sql = """
                    CREATE TABLE IF NOT EXISTS users (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        username TEXT NOT NULL UNIQUE,
                        email TEXT NOT NULL UNIQUE,
                        password_hash TEXT NOT NULL,
                        full_name TEXT NOT NULL,
                        active BOOLEAN DEFAULT 1,
                        role TEXT DEFAULT 'user' CHECK(role IN ('admin', 'user')),
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        last_login TIMESTAMP,
                        UNIQUE(username)
                    )
                """;

        try (Statement stmt = dbConnection.getConnection().createStatement()) {
            stmt.execute(sql);
            logger.debug("Created/verified users table");
        }
    }

    /**
     * Creates the household_profiles table.
     */
    private void createHouseholdProfileTable() throws SQLException {
        String sql = """
                    CREATE TABLE IF NOT EXISTS household_profiles (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        user_id INTEGER NOT NULL DEFAULT 0,
                        household_name TEXT NOT NULL,
                        member_count INTEGER NOT NULL CHECK(member_count > 0 AND member_count <= 20),
                        daily_limit REAL NOT NULL CHECK(daily_limit > 0),
                        city TEXT,
                        alert_at_70 BOOLEAN DEFAULT 1,
                        alert_at_90 BOOLEAN DEFAULT 1,
                        dark_mode BOOLEAN DEFAULT 0,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        UNIQUE(user_id)
                    )
                """;

        try (Statement stmt = dbConnection.getConnection().createStatement()) {
            stmt.execute(sql);
            logger.debug("Created/verified household_profiles table");
        }
    }

    /**
     * Creates the water_records table.
     */
    private void createWaterRecordsTable() throws SQLException {
        String sql = """
                    CREATE TABLE IF NOT EXISTS water_records (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        user_id INTEGER NOT NULL DEFAULT 0,
                        record_date DATE NOT NULL,
                        liters_used REAL NOT NULL CHECK(liters_used > 0),
                        note TEXT,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        UNIQUE(user_id, record_date)
                    )
                """;

        try (Statement stmt = dbConnection.getConnection().createStatement()) {
            stmt.execute(sql);
            logger.debug("Created/verified water_records table");
        }
    }

    /**
     * Creates the meters table for physical/virtual meter tracking.
     */
    private void createMetersTable() throws SQLException {
        String sql = """
                    CREATE TABLE IF NOT EXISTS meters (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        user_id INTEGER NOT NULL DEFAULT 0,
                        name TEXT NOT NULL,
                        location TEXT,
                        active BOOLEAN DEFAULT 1,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        UNIQUE(user_id, name)
                    )
                """;

        try (Statement stmt = dbConnection.getConnection().createStatement()) {
            stmt.execute(sql);
            logger.debug("Created/verified meters table");
        }
    }

    /**
     * Creates the meter_readings table. Readings are cumulative meter values.
     */
    private void createMeterReadingsTable() throws SQLException {
        String sql = """
                    CREATE TABLE IF NOT EXISTS meter_readings (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        user_id INTEGER NOT NULL DEFAULT 0,
                        meter_id INTEGER NOT NULL,
                        reading_time TIMESTAMP NOT NULL,
                        reading_liters REAL NOT NULL CHECK(reading_liters >= 0),
                        note TEXT,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        UNIQUE(user_id, meter_id, reading_time),
                        FOREIGN KEY(meter_id) REFERENCES meters(id)
                    )
                """;

        try (Statement stmt = dbConnection.getConnection().createStatement()) {
            stmt.execute(sql);
            logger.debug("Created/verified meter_readings table");
        }
    }

    /**
     * Creates the tariff_plans table for bill estimation.
     */
    private void createTariffPlansTable() throws SQLException {
        String sql = """
                    CREATE TABLE IF NOT EXISTS tariff_plans (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        user_id INTEGER NOT NULL DEFAULT 0,
                        name TEXT NOT NULL,
                        fixed_monthly_charge REAL NOT NULL DEFAULT 0,
                        rate_per_liter REAL NOT NULL CHECK(rate_per_liter >= 0),
                        free_allowance_liters REAL NOT NULL DEFAULT 0,
                        active BOOLEAN DEFAULT 1,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        UNIQUE(user_id, name)
                    )
                """;

        try (Statement stmt = dbConnection.getConnection().createStatement()) {
            stmt.execute(sql);
            logger.debug("Created/verified tariff_plans table");
        }
    }

    /**
     * Creates the alert_logs table.
     */
    private void createAlertLogsTable() throws SQLException {
        String sql = """
                    CREATE TABLE IF NOT EXISTS alert_logs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        user_id INTEGER NOT NULL DEFAULT 0,
                        alert_timestamp TIMESTAMP NOT NULL,
                        message TEXT NOT NULL,
                        alert_type TEXT NOT NULL CHECK(alert_type IN ('WARNING', 'CRITICAL')),
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                """;

        try (Statement stmt = dbConnection.getConnection().createStatement()) {
            stmt.execute(sql);
            logger.debug("Created/verified alert_logs table");
        }
    }

    /**
     * Creates the settings table.
     */
    private void createSettingsTable() throws SQLException {
        String sql = """
                    CREATE TABLE IF NOT EXISTS settings (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        key TEXT NOT NULL UNIQUE,
                        value TEXT NOT NULL,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                """;

        try (Statement stmt = dbConnection.getConnection().createStatement()) {
            stmt.execute(sql);
            logger.debug("Created/verified settings table");
        }
    }

    /**
     * Creates limit increase requests for municipality approval workflows.
     */
    private void createLimitIncreaseRequestsTable() throws SQLException {
        String sql = """
                    CREATE TABLE IF NOT EXISTS limit_increase_requests (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        user_id INTEGER NOT NULL,
                        requested_daily_limit REAL NOT NULL CHECK(requested_daily_limit > 0),
                        reason TEXT NOT NULL,
                        status TEXT NOT NULL DEFAULT 'PENDING'
                            CHECK(status IN ('PENDING', 'APPROVED', 'REJECTED')),
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        reviewed_at TIMESTAMP
                    )
                """;

        try (Statement stmt = dbConnection.getConnection().createStatement()) {
            stmt.execute(sql);
            logger.debug("Created/verified limit_increase_requests table");
        }
    }

    /**
     * Converts older single-household tables into user-scoped tables.
     */
    private void migrateUserScopedTables() throws SQLException {
        migrateHouseholdProfiles();
        migrateWaterRecords();
        migrateMeters();
        migrateMeterReadings();
        migrateTariffPlans();
        migrateAlertLogs();
    }

    private void migrateHouseholdProfiles() throws SQLException {
        if (hasColumn("household_profiles", "user_id")) {
            return;
        }

        runTableMigration("household_profiles", """
                    CREATE TABLE household_profiles (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        user_id INTEGER NOT NULL DEFAULT 0,
                        household_name TEXT NOT NULL,
                        member_count INTEGER NOT NULL CHECK(member_count > 0 AND member_count <= 20),
                        daily_limit REAL NOT NULL CHECK(daily_limit > 0),
                        city TEXT,
                        alert_at_70 BOOLEAN DEFAULT 1,
                        alert_at_90 BOOLEAN DEFAULT 1,
                        dark_mode BOOLEAN DEFAULT 0,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        UNIQUE(user_id)
                    )
                """, """
                    INSERT INTO household_profiles
                        (id, user_id, household_name, member_count, daily_limit, city, alert_at_70,
                         alert_at_90, dark_mode, created_at, updated_at)
                    SELECT id, legacyOwner(), household_name, member_count, daily_limit, city, alert_at_70,
                           alert_at_90, dark_mode, created_at, updated_at
                    FROM household_profiles_old
                """);
    }

    private void migrateWaterRecords() throws SQLException {
        if (hasColumn("water_records", "user_id")) {
            return;
        }

        runTableMigration("water_records", """
                    CREATE TABLE water_records (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        user_id INTEGER NOT NULL DEFAULT 0,
                        record_date DATE NOT NULL,
                        liters_used REAL NOT NULL CHECK(liters_used > 0),
                        note TEXT,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        UNIQUE(user_id, record_date)
                    )
                """, """
                    INSERT INTO water_records
                        (id, user_id, record_date, liters_used, note, created_at, updated_at)
                    SELECT id, legacyOwner(), record_date, liters_used, note, created_at, updated_at
                    FROM water_records_old
                """);
    }

    private void migrateMeters() throws SQLException {
        if (hasColumn("meters", "user_id")) {
            return;
        }

        runTableMigration("meters", """
                    CREATE TABLE meters (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        user_id INTEGER NOT NULL DEFAULT 0,
                        name TEXT NOT NULL,
                        location TEXT,
                        active BOOLEAN DEFAULT 1,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        UNIQUE(user_id, name)
                    )
                """, """
                    INSERT INTO meters (id, user_id, name, location, active, created_at)
                    SELECT id, legacyOwner(), name, location, active, created_at
                    FROM meters_old
                """);
    }

    private void migrateMeterReadings() throws SQLException {
        if (hasColumn("meter_readings", "user_id")) {
            return;
        }

        runTableMigration("meter_readings", """
                    CREATE TABLE meter_readings (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        user_id INTEGER NOT NULL DEFAULT 0,
                        meter_id INTEGER NOT NULL,
                        reading_time TIMESTAMP NOT NULL,
                        reading_liters REAL NOT NULL CHECK(reading_liters >= 0),
                        note TEXT,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        UNIQUE(user_id, meter_id, reading_time),
                        FOREIGN KEY(meter_id) REFERENCES meters(id)
                    )
                """, """
                    INSERT INTO meter_readings
                        (id, user_id, meter_id, reading_time, reading_liters, note, created_at)
                    SELECT id, legacyOwner(), meter_id, reading_time, reading_liters, note, created_at
                    FROM meter_readings_old
                """);
    }

    private void migrateTariffPlans() throws SQLException {
        if (hasColumn("tariff_plans", "user_id")) {
            return;
        }

        runTableMigration("tariff_plans", """
                    CREATE TABLE tariff_plans (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        user_id INTEGER NOT NULL DEFAULT 0,
                        name TEXT NOT NULL,
                        fixed_monthly_charge REAL NOT NULL DEFAULT 0,
                        rate_per_liter REAL NOT NULL CHECK(rate_per_liter >= 0),
                        free_allowance_liters REAL NOT NULL DEFAULT 0,
                        active BOOLEAN DEFAULT 1,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        UNIQUE(user_id, name)
                    )
                """, """
                    INSERT INTO tariff_plans
                        (id, user_id, name, fixed_monthly_charge, rate_per_liter, free_allowance_liters, active, created_at)
                    SELECT id, legacyOwner(), name, fixed_monthly_charge, rate_per_liter,
                           free_allowance_liters, active, created_at
                    FROM tariff_plans_old
                """);
    }

    private void migrateAlertLogs() throws SQLException {
        if (hasColumn("alert_logs", "user_id")) {
            return;
        }

        runTableMigration("alert_logs", """
                    CREATE TABLE alert_logs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        user_id INTEGER NOT NULL DEFAULT 0,
                        alert_timestamp TIMESTAMP NOT NULL,
                        message TEXT NOT NULL,
                        alert_type TEXT NOT NULL CHECK(alert_type IN ('WARNING', 'CRITICAL')),
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                """, """
                    INSERT INTO alert_logs (id, user_id, alert_timestamp, message, alert_type, created_at)
                    SELECT id, legacyOwner(), alert_timestamp, message, alert_type, created_at
                    FROM alert_logs_old
                """);
    }

    private void runTableMigration(String table, String createSql, String copySql) throws SQLException {
        logger.info("Migrating {} to user-scoped schema", table);
        try (Statement stmt = dbConnection.getConnection().createStatement()) {
            stmt.execute("PRAGMA foreign_keys = OFF");
            stmt.execute("ALTER TABLE " + table + " RENAME TO " + table + "_old");
            stmt.execute(createSql);
            stmt.execute(copySql.replace("legacyOwner()", legacyOwnerSql()));
            stmt.execute("DROP TABLE " + table + "_old");
            stmt.execute("PRAGMA foreign_keys = ON");
        }
    }

    private String legacyOwnerSql() {
        return "COALESCE((SELECT id FROM users WHERE active = 1 ORDER BY id LIMIT 1), 0)";
    }

    private boolean hasColumn(String table, String column) throws SQLException {
        try (Statement stmt = dbConnection.getConnection().createStatement();
                ResultSet rs = stmt.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rs.next()) {
                if (column.equalsIgnoreCase(rs.getString("name"))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Assigns legacy ownerless rows to a real user after first account creation.
     */
    public void adoptLegacyDataForUser(int userId) {
        try {
            dbConnection.executeUpdate("UPDATE household_profiles SET user_id = ? WHERE user_id = 0", userId);
            dbConnection.executeUpdate("UPDATE water_records SET user_id = ? WHERE user_id = 0", userId);
            dbConnection.executeUpdate("UPDATE meters SET user_id = ? WHERE user_id = 0", userId);
            dbConnection.executeUpdate("UPDATE meter_readings SET user_id = ? WHERE user_id = 0", userId);
            dbConnection.executeUpdate("UPDATE tariff_plans SET user_id = ? WHERE user_id = 0", userId);
            dbConnection.executeUpdate("UPDATE alert_logs SET user_id = ? WHERE user_id = 0", userId);
            logger.info("Adopted legacy data for user {}", userId);
        } catch (SQLException e) {
            logger.error("Failed to adopt legacy data for user {}", userId, e);
        }
    }

    /**
     * Drops all tables (use for testing/reset only).
     */
    public void dropAllTables() {
        try {
            String[] tables = { "settings", "limit_increase_requests", "alert_logs", "meter_readings", "meters", "tariff_plans",
                    "water_records", "household_profiles", "users" };
            for (String table : tables) {
                try (Statement stmt = dbConnection.getConnection().createStatement()) {
                    stmt.execute("DROP TABLE IF EXISTS " + table);
                    logger.warn("Dropped table: {}", table);
                }
            }
            logger.warn("All tables dropped");
        } catch (SQLException e) {
            logger.error("Error dropping tables", e);
        }
    }

    /**
     * Checks if a specific table exists in the database.
     *
     * @param tableName the name of the table
     * @return true if table exists, false otherwise
     */
    public boolean tableExists(String tableName) {
        try {
            DatabaseMetaData meta = dbConnection.getConnection().getMetaData();
            try (ResultSet rs = meta.getTables(null, null, tableName, new String[] { "TABLE" })) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.error("Error checking if table exists: {}", tableName, e);
            return false;
        }
    }
}
