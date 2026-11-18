package com.smartwater.model;

import java.time.LocalDateTime;

/**
 * Represents an alert log entry for water monitoring system.
 * Contains timestamp, message, and alert type information.
 */
public class AlertLog {
    private final LocalDateTime timestamp;
    private final String message;
    private final String type;

    /**
     * Constructs a new AlertLog with all specified parameters.
     *
     * @param timestamp: when the alert was created
     * @param message: alert message content
     * @param type: alert type ("WARNING" or "CRITICAL")
     */
    public AlertLog(LocalDateTime timestamp, String message, String type) {
        this.timestamp = timestamp;
        this.message = message;
        this.type = type;
    }

    /**
     * Gets the timestamp when the alert was created.
     *
     * @return: timestamp
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * Gets the alert message content.
     *
     * @return: message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Gets the alert type.
     *
     * @return: type ("WARNING" or "CRITICAL")
     */
    public String getType() {
        return type;
    }

    /**
     * Returns a string representation of the AlertLog.
     *
     * @return: string representation in format "timestamp | type | message"
     */
    @Override
    public String toString() {
        return timestamp + " | " + type + " | " + message;
    }
}
