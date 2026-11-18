package com.smartwater.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.*;

/**
 * Manages SQLite database connections for the SmartWater application.
 * Provides thread-safe connection pooling and lifecycle management.
 */
public class DatabaseConnection {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnection.class);
    private static final String DATABASE_URL = "jdbc:sqlite:data/smartwater.db";
    private static DatabaseConnection instance;
    private Connection connection;

    /**
     * Private constructor for singleton pattern.
     */
    private DatabaseConnection() {
        try {
            Class.forName("org.sqlite.JDBC");
            this.connection = DriverManager.getConnection(DATABASE_URL);
            // Enable foreign keys
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON");
                logger.info("SQLite connection established and foreign keys enabled");
            }
        } catch (ClassNotFoundException e) {
            logger.error("SQLite JDBC driver not found", e);
            throw new RuntimeException("Failed to load SQLite driver", e);
        } catch (SQLException e) {
            logger.error("Failed to establish database connection", e);
            throw new RuntimeException("Failed to connect to database", e);
        }
    }

    /**
     * Gets the singleton instance of DatabaseConnection.
     *
     * @return the DatabaseConnection instance
     */
    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    /**
     * Gets the active database connection.
     *
     * @return the Connection object
     */
    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                logger.warn("Connection was closed or null, re-establishing...");
                this.connection = DriverManager.getConnection(DATABASE_URL);
            }
        } catch (SQLException e) {
            logger.error("Error retrieving connection", e);
        }
        return connection;
    }

    /**
     * Closes the database connection.
     */
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                logger.info("Database connection closed");
            }
        } catch (SQLException e) {
            logger.error("Error closing database connection", e);
        }
    }

    /**
     * Executes a query and returns the result set.
     *
     * @param query  the SQL query to execute
     * @param params the query parameters
     * @return the ResultSet
     * @throws SQLException if query execution fails
     */
    public ResultSet executeQuery(String query, Object... params) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement(query);
        for (int i = 0; i < params.length; i++) {
            stmt.setObject(i + 1, params[i]);
        }
        logger.debug("Executing query: {}", query);
        return stmt.executeQuery();
    }

    /**
     * Executes an update/insert/delete statement.
     *
     * @param query  the SQL update query
     * @param params the query parameters
     * @return the number of affected rows
     * @throws SQLException if query execution fails
     */
    public int executeUpdate(String query, Object... params) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            logger.debug("Executing update: {}", query);
            return stmt.executeUpdate();
        }
    }

    /**
     * Executes an insert statement and returns the generated key.
     *
     * @param query  the SQL insert query
     * @param params the query parameters
     * @return the generated ID
     * @throws SQLException if query execution fails
     */
    public long executeInsertReturnKey(String query, Object... params) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            logger.debug("Executing insert: {}", query);
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
            return -1;
        }
    }

    /**
     * Closes the application shutdown resources.
     */
    public void shutdown() {
        closeConnection();
        logger.info("Database connection shut down");
    }
}
