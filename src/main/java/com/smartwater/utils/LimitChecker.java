package com.smartwater.utils;

import com.smartwater.controller.WaterController;
import com.smartwater.model.WaterRecord;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Utility class for checking water consumption limits.
 * Runs a background thread that continuously monitors water usage and alerts if
 * daily limit is exceeded.
 */
public class LimitChecker implements Runnable {
    private final WaterController controller;
    private volatile boolean running;
    private final LimitAlertCallback callback;
    private LocalDate lastAlertDate = null; // Track when alert was last shown

    /**
     * Functional interface for handling limit exceeded alerts.
     */
    @FunctionalInterface
    public interface LimitAlertCallback {
        /**
         * Called when the daily water limit is exceeded.
         *
         * @param message the alert message
         */
        void onLimitExceeded(String message);
    }

    /**
     * Constructs a new LimitChecker with the specified controller and callback.
     *
     * @param controller the WaterController for accessing water data
     * @param callback   the callback to handle limit exceeded alerts
     */
    public LimitChecker(WaterController controller, LimitAlertCallback callback) {
        this.controller = controller;
        this.callback = callback;
        this.running = false;
    }

    /**
     * The main monitoring loop that runs in the background thread.
     * Checks every 30 seconds if today's water limit is exceeded.
     * Shows alert only once per day.
     */
    @Override
    public void run() {
        System.out.println("LimitChecker thread started. Monitoring water usage...");

        while (running) {
            try {
                // Check if today's limit is exceeded
                LocalDate today = AppClock.today();
                Optional<WaterRecord> todayRecord = controller.getUsageForDate(today);

                // Only show alert once per day
                if (todayRecord.isPresent() && todayRecord.get().getLitersUsed() > controller.getDailyLimit()) {
                    if (lastAlertDate == null || !lastAlertDate.equals(today)) {
                        String alertMessage = "Warning: Daily water limit exceeded for " + today;
                        callback.onLimitExceeded(alertMessage);
                        lastAlertDate = today;
                    }
                }

                // Wait for 30 seconds before next check
                Thread.sleep(30000);

            } catch (InterruptedException e) {
                System.out.println("LimitChecker thread interrupted. Stopping monitoring...");
                Thread.currentThread().interrupt(); // Restore interrupt status
                break;
            } catch (Exception e) {
                System.err.println("Error in LimitChecker monitoring: " + e.getMessage());
            }
        }

        System.out.println("LimitChecker thread stopped.");
    }

    /**
     * Starts the limit checking in a background daemon thread.
     */
    public void start() {
        if (running) {
            System.out.println("LimitChecker is already running.");
            return;
        }

        running = true;
        Thread monitorThread = new Thread(this, "LimitCheckerThread");
        monitorThread.setDaemon(true); // Set as daemon thread so it doesn't prevent JVM shutdown
        monitorThread.start();
        System.out.println("LimitChecker started in background thread.");
    }

    /**
     * Safely stops the limit checking thread.
     */
    public void stop() {
        if (!running) {
            System.out.println("LimitChecker is not running.");
            return;
        }

        running = false;
        System.out.println("LimitChecker stop requested. Thread will terminate on next check.");
    }
}
