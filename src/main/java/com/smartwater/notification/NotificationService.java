package com.smartwater.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Notification service for real-time alerts and updates.
 * Uses observer pattern to notify listeners of events.
 */
public class NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    private static NotificationService instance;
    private final List<NotificationListener> listeners = new CopyOnWriteArrayList<>();
    private final Deque<Notification> notificationHistory = new LinkedList<>();
    private static final int MAX_HISTORY = 100;

    /**
     * Notification types.
     */
    public enum NotificationType {
        ALERT_THRESHOLD("Water limit threshold reached"),
        ANOMALY_DETECTED("Unusual consumption detected"),
        BACKUP_CREATED("Database backup completed"),
        USER_LOGIN("User logged in"),
        RECORD_ADDED("Water record added"),
        PROFILE_UPDATED("Profile updated"),
        EXPORT_COMPLETED("Data export completed"),
        SYSTEM_ERROR("System error occurred"),
        INFO("Information message");

        public final String description;

        NotificationType(String description) {
            this.description = description;
        }
    }

    /**
     * Notification class.
     */
    public static class Notification {
        public final String id;
        public final NotificationType type;
        public final String title;
        public final String message;
        public final LocalDateTime timestamp;
        public final Map<String, Object> metadata;

        public Notification(NotificationType type, String title, String message) {
            this.id = UUID.randomUUID().toString();
            this.type = type;
            this.title = title;
            this.message = message;
            this.timestamp = LocalDateTime.now();
            this.metadata = new HashMap<>();
        }

        public void addMetadata(String key, Object value) {
            metadata.put(key, value);
        }

        @Override
        public String toString() {
            return String.format("[%s] %s: %s - %s", type, title, message, timestamp);
        }
    }

    /**
     * Listener interface for notifications.
     */
    @FunctionalInterface
    public interface NotificationListener {
        /**
         * Called when a notification is triggered.
         *
         * @param notification the notification
         */
        void onNotification(Notification notification);
    }

    /**
     * Private constructor for singleton pattern.
     */
    private NotificationService() {
        logger.info("NotificationService initialized");
    }

    /**
     * Gets the singleton instance.
     *
     * @return the NotificationService instance
     */
    public static synchronized NotificationService getInstance() {
        if (instance == null) {
            instance = new NotificationService();
        }
        return instance;
    }

    /**
     * Registers a listener for notifications.
     *
     * @param listener the listener to register
     */
    public void subscribe(NotificationListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
            logger.debug("Listener registered. Total listeners: {}", listeners.size());
        }
    }

    /**
     * Unregisters a listener.
     *
     * @param listener the listener to unregister
     */
    public void unsubscribe(NotificationListener listener) {
        if (listeners.remove(listener)) {
            logger.debug("Listener unregistered. Total listeners: {}", listeners.size());
        }
    }

    /**
     * Sends a notification to all registered listeners.
     *
     * @param notification the notification to send
     */
    public void notify(Notification notification) {
        if (notification == null) {
            logger.warn("Attempted to send null notification");
            return;
        }

        // Add to history
        addToHistory(notification);

        logger.info("Sending notification: {}", notification);

        // Notify all listeners
        for (NotificationListener listener : listeners) {
            try {
                listener.onNotification(notification);
            } catch (Exception e) {
                logger.error("Error notifying listener: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * Creates and sends a threshold alert notification.
     *
     * @param date  the date of the alert
     * @param usage the water usage
     * @param limit the daily limit
     */
    public void notifyThresholdAlert(String date, double usage, double limit) {
        String title = "Water Limit Alert";
        double percentage = (usage / limit) * 100;
        String message = String.format("Usage: %.2f L of %.2f L (%.1f%%) on %s",
                usage, limit, percentage, date);

        Notification notification = new Notification(
                NotificationType.ALERT_THRESHOLD,
                title,
                message);
        notification.addMetadata("date", date);
        notification.addMetadata("usage", usage);
        notification.addMetadata("limit", limit);
        notification.addMetadata("percentage", percentage);

        notify(notification);
    }

    /**
     * Creates and sends an anomaly detection notification.
     *
     * @param date  the date of the anomaly
     * @param usage the unusual usage value
     * @param type  anomaly type (HIGH/LOW)
     */
    public void notifyAnomaly(String date, double usage, String type) {
        String title = "Anomaly Detected";
        String message = String.format("%s consumption detected on %s: %.2f L", type, date, usage);

        Notification notification = new Notification(
                NotificationType.ANOMALY_DETECTED,
                title,
                message);
        notification.addMetadata("date", date);
        notification.addMetadata("usage", usage);
        notification.addMetadata("anomalyType", type);

        notify(notification);
    }

    /**
     * Creates and sends a backup completed notification.
     *
     * @param backupFile the backup file path
     * @param size       the backup size in bytes
     */
    public void notifyBackupCompleted(String backupFile, long size) {
        String title = "Backup Completed";
        String message = String.format("Database backup created: %s (%d bytes)", backupFile, size);

        Notification notification = new Notification(
                NotificationType.BACKUP_CREATED,
                title,
                message);
        notification.addMetadata("backupFile", backupFile);
        notification.addMetadata("size", size);

        notify(notification);
    }

    /**
     * Creates and sends a user login notification.
     *
     * @param username the username
     */
    public void notifyUserLogin(String username) {
        String title = "User Login";
        String message = String.format("User '%s' logged in", username);

        Notification notification = new Notification(
                NotificationType.USER_LOGIN,
                title,
                message);
        notification.addMetadata("username", username);

        notify(notification);
    }

    /**
     * Creates and sends a record added notification.
     *
     * @param date  the record date
     * @param usage the water usage
     */
    public void notifyRecordAdded(String date, double usage) {
        String title = "Record Added";
        String message = String.format("Water record added for %s: %.2f L", date, usage);

        Notification notification = new Notification(
                NotificationType.RECORD_ADDED,
                title,
                message);
        notification.addMetadata("date", date);
        notification.addMetadata("usage", usage);

        notify(notification);
    }

    /**
     * Creates and sends a profile updated notification.
     *
     * @param householdName the household name
     */
    public void notifyProfileUpdated(String householdName) {
        String title = "Profile Updated";
        String message = String.format("Profile for '%s' has been updated", householdName);

        Notification notification = new Notification(
                NotificationType.PROFILE_UPDATED,
                title,
                message);
        notification.addMetadata("householdName", householdName);

        notify(notification);
    }

    /**
     * Creates and sends an export completed notification.
     *
     * @param format   the export format (CSV, EXCEL, JSON)
     * @param filePath the exported file path
     */
    public void notifyExportCompleted(String format, String filePath) {
        String title = "Export Completed";
        String message = String.format("Data exported to %s: %s", format.toUpperCase(), filePath);

        Notification notification = new Notification(
                NotificationType.EXPORT_COMPLETED,
                title,
                message);
        notification.addMetadata("format", format);
        notification.addMetadata("filePath", filePath);

        notify(notification);
    }

    /**
     * Creates and sends a system error notification.
     *
     * @param errorMessage the error message
     */
    public void notifySystemError(String errorMessage) {
        String title = "System Error";
        String message = "An error occurred: " + errorMessage;

        Notification notification = new Notification(
                NotificationType.SYSTEM_ERROR,
                title,
                message);
        notification.addMetadata("errorMessage", errorMessage);

        notify(notification);
    }

    /**
     * Gets recent notifications.
     *
     * @param count the number of notifications to retrieve
     * @return list of recent notifications
     */
    public List<Notification> getRecentNotifications(int count) {
        List<Notification> recent = new ArrayList<>();
        int retrieved = 0;

        for (Notification n : notificationHistory) {
            if (retrieved >= count)
                break;
            recent.add(n);
            retrieved++;
        }

        return recent;
    }

    /**
     * Gets all notifications.
     *
     * @return list of all notifications in history
     */
    public List<Notification> getAllNotifications() {
        return new ArrayList<>(notificationHistory);
    }

    /**
     * Clears notification history.
     */
    public void clearHistory() {
        notificationHistory.clear();
        logger.info("Notification history cleared");
    }

    /**
     * Gets number of registered listeners.
     *
     * @return listener count
     */
    public int getListenerCount() {
        return listeners.size();
    }

    /**
     * Adds notification to history (keeps last 100).
     *
     * @param notification the notification to add
     */
    private void addToHistory(Notification notification) {
        notificationHistory.addFirst(notification);
        if (notificationHistory.size() > MAX_HISTORY) {
            notificationHistory.removeLast();
        }
    }
}
