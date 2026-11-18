package com.smartwater.controller;

import com.smartwater.model.WaterRecord;
import com.smartwater.model.HouseholdProfile;
import com.smartwater.model.AlertLog;
import com.smartwater.model.BillEstimate;
import com.smartwater.model.LeakAlert;
import com.smartwater.model.LimitIncreaseRequest;
import com.smartwater.model.Meter;
import com.smartwater.model.MeterReading;
import com.smartwater.model.TariffPlan;
import com.smartwater.model.User;
import com.smartwater.policy.MunicipalityPolicy;
import com.smartwater.analytics.BillingService;
import com.smartwater.analytics.LeakDetectionService;
import com.smartwater.storage.FileManager;
import com.smartwater.storage.ProfileManager;
import com.smartwater.storage.AlertManager;
import com.smartwater.storage.MeterManager;
import com.smartwater.storage.TariffManager;
import com.smartwater.storage.LimitRequestManager;
import com.smartwater.utils.AppClock;
import com.smartwater.validation.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Central controller for water management operations.
 * Now features Reactive Data Binding, Async Tasks, and SQLite database.
 */
public class WaterController {
    private static final Logger logger = LoggerFactory.getLogger(WaterController.class);
    private final FileManager fileManager;
    private final ProfileManager profileManager;
    private final AlertManager alertManager;
    private final MeterManager meterManager;
    private final TariffManager tariffManager;
    private final BillingService billingService;
    private final LeakDetectionService leakDetectionService;
    private final LimitRequestManager limitRequestManager;
    private final ExecutorService backgroundExecutor;
    private final int userId;

    private HouseholdProfile profile;
    private final ObservableList<WaterRecord> recordsList;

    public WaterController() {
        this(null);
    }

    public WaterController(User user) {
        this.userId = user == null ? 0 : user.getId();
        this.fileManager = FileManager.getInstance();
        this.profileManager = ProfileManager.getInstance();
        this.alertManager = AlertManager.getInstance();
        this.meterManager = MeterManager.getInstance();
        this.tariffManager = TariffManager.getInstance();
        this.billingService = BillingService.getInstance();
        this.leakDetectionService = LeakDetectionService.getInstance();
        this.limitRequestManager = LimitRequestManager.getInstance();
        this.backgroundExecutor = Executors.newFixedThreadPool(3, runnable -> {
            Thread thread = new Thread(runnable, "WaterControllerWorker");
            thread.setDaemon(true);
            return thread;
        });

        logger.info("WaterController initialized");

        this.profile = profileManager.loadProfile(userId);
        this.recordsList = FXCollections.observableArrayList(fileManager.loadAllRecords(userId));
    }

    // ==================== PROFILE MANAGEMENT ====================

    public HouseholdProfile getProfile() {
        return profile;
    }

    public String saveProfile(HouseholdProfile updatedProfile) {
        try {
            updatedProfile.setDailyLimit(MunicipalityPolicy.calculateDailyLimit(
                    updatedProfile.getMemberCount(), updatedProfile.getCity()));
            // Validate profile before saving
            com.smartwater.validation.ValidationUtil.validateHouseholdProfile(updatedProfile);

            // Run in background task to avoid UI freeze
            Task<Boolean> saveTask = new Task<>() {
                @Override
                protected Boolean call() {
                    return profileManager.saveProfile(updatedProfile, userId);
                }
            };

            saveTask.setOnSucceeded(e -> {
                if (saveTask.getValue()) {
                    this.profile = updatedProfile;
                    logger.info("Profile updated successfully");
                }
            });

            saveTask.setOnFailed(e -> {
                logger.error("Failed to save profile", saveTask.getException());
            });

            backgroundExecutor.submit(saveTask);
            return "Saving profile in background...";

        } catch (ValidationException e) {
            logger.error("Profile validation failed: {}", e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    public boolean isFirstLaunch() {
        return profileManager.isFirstLaunch(userId);
    }

    public double getDailyLimit() {
        return profile.getDailyLimit();
    }

    // ==================== RECORD MANAGEMENT ====================

    public ObservableList<WaterRecord> getRecordsList() {
        return recordsList;
    }

    public String addRecord(LocalDate date, double litersUsed, String note) {
        try {
            if (litersUsed <= 0)
                return "Error: Liters used must be greater than 0";

            Optional<WaterRecord> existingRecord = getUsageForDate(date);
            if (existingRecord.isPresent())
                return "Error: A record already exists for date: " + date;

            WaterRecord record = new WaterRecord(date, litersUsed, note);

            // Validate record before saving
            com.smartwater.validation.ValidationUtil.validateWaterRecord(record);

            recordsList.add(record); // Updates UI automatically

            // Async save
            Task<Void> saveTask = new Task<>() {
                @Override
                protected Void call() {
                    fileManager.saveRecord(record, userId);
                    return null;
                }
            };

            saveTask.setOnFailed(e -> {
                logger.error("Failed to save record", saveTask.getException());
                recordsList.remove(record);
            });

            backgroundExecutor.submit(saveTask);

            checkAndLogAlerts(date, litersUsed);
            logger.info("Record added for date: {}", date);
            return "Record saved successfully!";

        } catch (ValidationException e) {
            logger.error("Record validation failed: {}", e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    public List<WaterRecord> getAllRecords() {
        return recordsList;
    }

    public Optional<WaterRecord> getUsageForDate(LocalDate date) {
        return recordsList.stream().filter(r -> r.getDate().equals(date)).findFirst();
    }

    // ==================== REAL-WORLD METER TRACKING ====================

    public List<Meter> getMeters() {
        return meterManager.getActiveMeters(userId);
    }

    public Meter getDefaultMeter() {
        return meterManager.getOrCreateDefaultMeter(userId);
    }

    public List<MeterReading> getReadingsForDefaultMeter() {
        return meterManager.getReadings(getDefaultMeter().getId(), userId);
    }

    public Optional<MeterReading> getLatestMeterReading() {
        return meterManager.getLatestReading(getDefaultMeter().getId(), userId);
    }

    public String addMeterReading(double cumulativeLiters, String note) {
        return addMeterReading(AppClock.now(), cumulativeLiters, note);
    }

    public String addMeterReading(LocalDateTime readingTime, double cumulativeLiters, String note) {
        if (readingTime == null) {
            return "Error: Reading time is required";
        }
        if (readingTime.isAfter(AppClock.now())) {
            return "Error: Reading time cannot be in the future";
        }
        if (cumulativeLiters < 0) {
            return "Error: Meter reading cannot be negative";
        }

        Meter meter = getDefaultMeter();
        Optional<MeterReading> previous = meterManager.getLatestReading(meter.getId(), userId);
        if (previous.isPresent() && cumulativeLiters < previous.get().getReadingLiters()) {
            return "Error: New meter reading cannot be lower than the previous reading ("
                    + String.format("%.1f", previous.get().getReadingLiters()) + " L)";
        }

        try {
            meterManager.saveReading(new MeterReading(meter.getId(), readingTime, cumulativeLiters, note), userId);
            if (previous.isPresent()) {
                double usageDelta = cumulativeLiters - previous.get().getReadingLiters();
                if (usageDelta > 0) {
                    upsertDailyUsageFromMeter(readingTime.toLocalDate(), usageDelta, note);
                }
            }
            return previous.isPresent()
                    ? "Meter reading saved and daily usage updated."
                    : "First meter reading saved. Add the next reading to calculate usage.";
        } catch (RuntimeException e) {
            logger.error("Failed to add meter reading", e);
            return "Error: Failed to save meter reading";
        }
    }

    private void upsertDailyUsageFromMeter(LocalDate date, double usageDelta, String note) {
        Optional<WaterRecord> existing = getUsageForDate(date);
        String meterNote = "Meter reading" + (note == null || note.isBlank() ? "" : ": " + note);

        if (existing.isPresent()) {
            WaterRecord record = existing.get();
            record.setLitersUsed(record.getLitersUsed() + usageDelta);
            if (record.getNote() == null || record.getNote().isBlank()) {
                record.setNote(meterNote);
            }
            backgroundExecutor.submit(() -> fileManager.saveRecord(record, userId));
            checkAndLogAlerts(date, record.getLitersUsed());
        } else {
            WaterRecord record = new WaterRecord(date, usageDelta, meterNote);
            recordsList.add(record);
            backgroundExecutor.submit(() -> fileManager.saveRecord(record, userId));
            checkAndLogAlerts(date, usageDelta);
        }
    }

    // ==================== ANALYTICS METHODS ====================

    public List<WaterRecord> getWeeklyData() {
        LocalDate sevenDaysAgo = AppClock.today().minusDays(7);
        return recordsList.stream()
                .filter(record -> record.getDate().isAfter(sevenDaysAgo))
                .sorted((a, b) -> a.getDate().compareTo(b.getDate()))
                .collect(Collectors.toList());
    }

    public List<WaterRecord> getMonthlyData() {
        LocalDate today = AppClock.today();
        return recordsList.stream()
                .filter(record -> record.getDate().getYear() == today.getYear() &&
                        record.getDate().getMonthValue() == today.getMonthValue())
                .collect(Collectors.toList());
    }

    public double getMonthlyAverage() {
        List<WaterRecord> monthlyRecords = getMonthlyData();
        if (monthlyRecords.isEmpty())
            return 0.0;
        double totalLiters = monthlyRecords.stream().mapToDouble(WaterRecord::getLitersUsed).sum();
        return totalLiters / monthlyRecords.size();
    }

    public Optional<WaterRecord> getTodayRecord() {
        return getUsageForDate(AppClock.today());
    }

    public int getCurrentStreak() {
        int streak = 0;
        LocalDate currentDate = AppClock.today();
        for (int i = 0; i < 365; i++) {
            LocalDate checkDate = currentDate.minusDays(i);
            Optional<WaterRecord> record = getUsageForDate(checkDate);
            if (record.isPresent() && record.get().getLitersUsed() <= getDailyLimit())
                streak++;
            else if (record.isPresent())
                break;
        }
        return streak;
    }

    public Optional<WaterRecord> getBestDay() {
        return recordsList.stream().min((a, b) -> Double.compare(a.getLitersUsed(), b.getLitersUsed()));
    }

    public Optional<WaterRecord> getWorstDay() {
        return recordsList.stream().max((a, b) -> Double.compare(a.getLitersUsed(), b.getLitersUsed()));
    }

    public double getMonthlyTotal() {
        return getMonthlyData().stream().mapToDouble(WaterRecord::getLitersUsed).sum();
    }

    public double getMonthlyQuotaUsed() {
        double monthlyTotal = getMonthlyTotal();
        double monthlyQuota = getDailyLimit() * AppClock.today().lengthOfMonth();
        return (monthlyQuota > 0) ? (monthlyTotal / monthlyQuota) : 0.0;
    }

    // ==================== ALERT MANAGEMENT ====================

    private void checkAndLogAlerts(LocalDate date, double litersUsed) {
        double dailyLimit = getDailyLimit();
        double percentage = (dailyLimit > 0) ? (litersUsed / dailyLimit) : 0.0;
        if (percentage >= 1.0 && profile.isAlertAt90())
            logAlert("CRITICAL", "Daily limit exceeded! Used " + litersUsed + "L of " + dailyLimit + "L");
        else if (percentage >= 0.7 && profile.isAlertAt70())
            logAlert("WARNING", "Approaching limit! Used " + litersUsed + "L of " + dailyLimit + "L");
    }

    private void logAlert(String type, String message) {
        AlertLog alert = new AlertLog(AppClock.now(), message, type);
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                alertManager.saveAlert(alert, userId);
                return null;
            }
        };
        backgroundExecutor.submit(task);
    }

    public List<AlertLog> getRecentAlerts(int count) {
        return alertManager.getRecentAlerts(count, userId);
    }

    public String clearAlerts() {
        if (alertManager.clearAlerts(userId))
            return "All alerts cleared successfully!";
        return "Error clearing alerts!";
    }

    public String getSmartTip() {
        Optional<WaterRecord> todayRecord = getTodayRecord();
        List<WaterRecord> recentRecords = getWeeklyData();

        if (todayRecord.isPresent() && todayRecord.get().getLitersUsed() > getDailyLimit())
            return "💧 You exceeded today's limit! Try spreading usage across the day tomorrow.";

        long exceededDays = recentRecords.stream().filter(r -> r.getLitersUsed() > getDailyLimit()).count();
        if (exceededDays >= 3)
            return "You've exceeded your limit 3 days in a row. Consider reducing laundry or shower duration.";

        if (todayRecord.isPresent() && (todayRecord.get().getLitersUsed() / getDailyLimit()) < 0.5)
            return "🌟 Great job! You're well within your limit today. Keep it up!";

        if (getMonthlyAverage() > getDailyLimit())
            return "📊 Your monthly average exceeds your daily limit. Try setting a stricter goal.";

        return "💡 WHO recommends " + profile.getMemberCount() * 50 + "L/day for your household of "
                + profile.getMemberCount() + " people.";
    }

    public double estimateMonthlyBill(double ratePerLiter) {
        return getMonthlyTotal() * ratePerLiter;
    }

    public TariffPlan getActiveTariffPlan() {
        return tariffManager.getActiveTariffPlan(userId, MunicipalityPolicy.defaultTariffPlan(profile.getCity()));
    }

    public String saveTariffPlan(String name, double fixedCharge, double ratePerLiter, double freeAllowanceLiters) {
        if (name == null || name.isBlank()) {
            return "Error: Tariff name is required";
        }
        if (fixedCharge < 0 || ratePerLiter < 0 || freeAllowanceLiters < 0) {
            return "Error: Tariff values cannot be negative";
        }

        boolean saved = tariffManager.saveTariffPlan(
                new TariffPlan(name, fixedCharge, ratePerLiter, freeAllowanceLiters), userId);
        return saved ? "Tariff saved successfully!" : "Error: Failed to save tariff";
    }

    public BillEstimate getCurrentMonthBillEstimate() {
        return billingService.estimateCurrentMonthBill(getAllRecords(), getActiveTariffPlan(), AppClock.today());
    }

    public LeakAlert getLeakAlert() {
        return leakDetectionService.analyzeRecentUsage(getAllRecords(), getDailyLimit());
    }

    public Optional<LimitIncreaseRequest> getLatestLimitIncreaseRequest() {
        return limitRequestManager.getLatestRequest(userId);
    }

    public String requestLimitIncrease(double requestedDailyLimit, String reason) {
        if (requestedDailyLimit <= getDailyLimit()) {
            return "Error: Requested limit must be higher than your current municipal limit.";
        }
        if (reason == null || reason.trim().length() < 10) {
            return "Error: Please provide a reason of at least 10 characters.";
        }

        boolean saved = limitRequestManager.createRequest(userId, requestedDailyLimit, reason.trim());
        return saved ? "Request submitted for municipality review." : "Error: Could not submit request.";
    }

    public void shutdown() {
        backgroundExecutor.shutdownNow();
    }
}
