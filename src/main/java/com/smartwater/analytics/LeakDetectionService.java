package com.smartwater.analytics;

import com.smartwater.model.LeakAlert;
import com.smartwater.model.WaterRecord;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * Detects suspicious spikes that may indicate leaks or unusual usage.
 */
public class LeakDetectionService {
    private static LeakDetectionService instance;

    private LeakDetectionService() {
    }

    public static synchronized LeakDetectionService getInstance() {
        if (instance == null) {
            instance = new LeakDetectionService();
        }
        return instance;
    }

    public LeakAlert analyzeRecentUsage(List<WaterRecord> records, double dailyLimit) {
        List<WaterRecord> sorted = records.stream()
                .sorted(Comparator.comparing(WaterRecord::getDate))
                .toList();

        if (sorted.size() < 7) {
            return new LeakAlert(LeakAlert.Severity.WATCH, null,
                    "Add at least 7 days of records for reliable leak detection.", 0, 0);
        }

        WaterRecord latest = sorted.get(sorted.size() - 1);
        List<WaterRecord> baselineRecords = sorted.subList(Math.max(0, sorted.size() - 8), sorted.size() - 1);
        double baseline = baselineRecords.stream().mapToDouble(WaterRecord::getLitersUsed).average().orElse(0);

        if (baseline <= 0) {
            return LeakAlert.none();
        }

        double latestUsage = latest.getLitersUsed();
        LocalDate latestDate = latest.getDate();
        if (latestUsage >= baseline * 2.0 && latestUsage > dailyLimit) {
            return new LeakAlert(LeakAlert.Severity.CRITICAL, latestDate,
                    "Usage is more than double the recent baseline and above your daily limit. Check for leaks.",
                    latestUsage, baseline);
        }

        if (latestUsage >= baseline * 1.5) {
            return new LeakAlert(LeakAlert.Severity.WARNING, latestDate,
                    "Usage jumped sharply compared with your recent baseline.", latestUsage, baseline);
        }

        long highDays = sorted.stream()
                .skip(Math.max(0, sorted.size() - 3))
                .filter(r -> r.getLitersUsed() > dailyLimit)
                .count();
        if (highDays == 3) {
            return new LeakAlert(LeakAlert.Severity.WARNING, latestDate,
                    "Usage stayed above the daily limit for 3 straight recorded days.", latestUsage, baseline);
        }

        return LeakAlert.none();
    }
}
