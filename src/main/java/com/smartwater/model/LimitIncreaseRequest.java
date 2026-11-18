package com.smartwater.model;

import java.time.LocalDateTime;

/**
 * Household request for a municipality-approved daily limit increase.
 */
public class LimitIncreaseRequest {
    private final int id;
    private final int userId;
    private final double requestedDailyLimit;
    private final String reason;
    private final String status;
    private final LocalDateTime createdAt;

    public LimitIncreaseRequest(int id, int userId, double requestedDailyLimit, String reason, String status,
            LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.requestedDailyLimit = requestedDailyLimit;
        this.reason = reason;
        this.status = status;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public int getUserId() {
        return userId;
    }

    public double getRequestedDailyLimit() {
        return requestedDailyLimit;
    }

    public String getReason() {
        return reason;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
