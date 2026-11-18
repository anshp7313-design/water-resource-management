package com.smartwater.storage;

import com.smartwater.database.DatabaseConnection;
import com.smartwater.database.DatabaseSchema;
import com.smartwater.model.LimitIncreaseRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Persists household requests for municipal limit increases.
 */
public class LimitRequestManager {
    private static final Logger logger = LoggerFactory.getLogger(LimitRequestManager.class);
    private static LimitRequestManager instance;

    private final DatabaseConnection dbConnection;

    private LimitRequestManager() {
        this.dbConnection = DatabaseConnection.getInstance();
        new DatabaseSchema(dbConnection).initialize();
    }

    public static synchronized LimitRequestManager getInstance() {
        if (instance == null) {
            instance = new LimitRequestManager();
        }
        return instance;
    }

    public boolean createRequest(int userId, double requestedDailyLimit, String reason) {
        try {
            String sql = """
                        INSERT INTO limit_increase_requests (user_id, requested_daily_limit, reason, status)
                        VALUES (?, ?, ?, 'PENDING')
                    """;
            dbConnection.executeUpdate(sql, userId, requestedDailyLimit, reason);
            logger.info("Created limit increase request for user {}", userId);
            return true;
        } catch (SQLException e) {
            logger.error("Failed to create limit increase request", e);
            return false;
        }
    }

    public Optional<LimitIncreaseRequest> getLatestRequest(int userId) {
        try {
            String sql = """
                        SELECT id, user_id, requested_daily_limit, reason, status, created_at
                        FROM limit_increase_requests
                        WHERE user_id = ?
                        ORDER BY created_at DESC
                        LIMIT 1
                    """;
            try (ResultSet rs = dbConnection.executeQuery(sql, userId)) {
                if (rs.next()) {
                    Timestamp createdAt = rs.getTimestamp("created_at");
                    return Optional.of(new LimitIncreaseRequest(
                            rs.getInt("id"),
                            rs.getInt("user_id"),
                            rs.getDouble("requested_daily_limit"),
                            rs.getString("reason"),
                            rs.getString("status"),
                            createdAt == null ? LocalDateTime.now() : createdAt.toLocalDateTime()));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to load latest limit increase request", e);
        }
        return Optional.empty();
    }
}
