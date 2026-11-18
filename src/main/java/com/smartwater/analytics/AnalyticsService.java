package com.smartwater.analytics;

import com.smartwater.model.WaterRecord;
import com.smartwater.utils.AppClock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Advanced analytics service for water consumption analysis.
 * Provides trends, anomaly detection, predictions, and insights.
 */
public class AnalyticsService {
    private static final Logger logger = LoggerFactory.getLogger(AnalyticsService.class);
    private static AnalyticsService instance;

    /**
     * Private constructor for singleton pattern.
     */
    private AnalyticsService() {
        logger.info("AnalyticsService initialized");
    }

    /**
     * Gets the singleton instance.
     *
     * @return the AnalyticsService instance
     */
    public static synchronized AnalyticsService getInstance() {
        if (instance == null) {
            instance = new AnalyticsService();
        }
        return instance;
    }

    /**
     * Analyzes consumption trend over time.
     *
     * @param records the water records to analyze
     * @param days    number of days to analyze
     * @return TrendAnalysis with trend direction and percentage change
     */
    public TrendAnalysis analyzeTrend(List<WaterRecord> records, int days) {
        if (records.isEmpty()) {
            logger.warn("No records available for trend analysis");
            return new TrendAnalysis(0, 0, "No data");
        }

        LocalDate cutoffDate = AppClock.today().minusDays(days);
        List<WaterRecord> periodRecords = records.stream()
                .filter(r -> r.getDate().isAfter(cutoffDate))
                .sorted(Comparator.comparing(WaterRecord::getDate))
                .collect(Collectors.toList());

        if (periodRecords.size() < 2) {
            logger.warn("Insufficient data for trend analysis");
            return new TrendAnalysis(0, 0, "Insufficient data");
        }

        // Split into first and second half
        int midpoint = periodRecords.size() / 2;
        double firstHalf = periodRecords.subList(0, midpoint).stream()
                .mapToDouble(WaterRecord::getLitersUsed).average().orElse(0);
        double secondHalf = periodRecords.subList(midpoint, periodRecords.size()).stream()
                .mapToDouble(WaterRecord::getLitersUsed).average().orElse(0);

        double percentageChange = ((secondHalf - firstHalf) / firstHalf) * 100;
        String trend = percentageChange > 5 ? "INCREASING" : percentageChange < -5 ? "DECREASING" : "STABLE";

        logger.info("Trend analysis: {} ({:.2f}%)", trend, percentageChange);
        return new TrendAnalysis(firstHalf, secondHalf, trend);
    }

    /**
     * Detects anomalies in water consumption (outliers).
     *
     * @param records the water records to analyze
     * @return list of AnomalyReport for detected anomalies
     */
    public List<AnomalyReport> detectAnomalies(List<WaterRecord> records) {
        List<AnomalyReport> anomalies = new ArrayList<>();

        if (records.isEmpty()) {
            return anomalies;
        }

        double[] values = records.stream()
                .sorted(Comparator.comparing(WaterRecord::getDate))
                .mapToDouble(WaterRecord::getLitersUsed)
                .toArray();

        if (values.length < 3) {
            return anomalies;
        }

        // Calculate mean and standard deviation
        double mean = Arrays.stream(values).average().orElse(0);
        double stdDev = calculateStandardDeviation(values, mean);
        double threshold = 2.0; // 2 standard deviations

        List<WaterRecord> sorted = records.stream()
                .sorted(Comparator.comparing(WaterRecord::getDate))
                .collect(Collectors.toList());

        for (int i = 0; i < sorted.size(); i++) {
            WaterRecord record = sorted.get(i);
            double zScore = Math.abs((record.getLitersUsed() - mean) / stdDev);

            if (zScore > threshold) {
                String type = record.getLitersUsed() > mean ? "HIGH" : "LOW";
                anomalies.add(new AnomalyReport(record.getDate(), record.getLitersUsed(),
                        type, zScore));
                logger.info("Anomaly detected on {}: {} L (z-score: {:.2f})",
                        record.getDate(), record.getLitersUsed(), zScore);
            }
        }

        return anomalies;
    }

    /**
     * Predicts next month's consumption based on historical data.
     *
     * @param records the historical water records
     * @return Prediction with predicted value and confidence
     */
    public Prediction predictNextMonth(List<WaterRecord> records) {
        if (records.isEmpty()) {
            logger.warn("No data for prediction");
            return new Prediction(0, 0, "No historical data");
        }

        // Get last 90 days of data
        LocalDate ninetyDaysAgo = AppClock.today().minusDays(90);
        List<WaterRecord> recentRecords = records.stream()
                .filter(r -> r.getDate().isAfter(ninetyDaysAgo))
                .sorted(Comparator.comparing(WaterRecord::getDate))
                .collect(Collectors.toList());

        if (recentRecords.isEmpty()) {
            logger.warn("Insufficient recent data for prediction");
            return new Prediction(0, 0, "Insufficient data");
        }

        // Simple moving average prediction
        double average = recentRecords.stream()
                .mapToDouble(WaterRecord::getLitersUsed)
                .average()
                .orElse(0);

        // Calculate confidence based on data consistency
        double stdDev = calculateStandardDeviation(
                recentRecords.stream().mapToDouble(WaterRecord::getLitersUsed).toArray(),
                average);
        double coefficientOfVariation = (stdDev / average) * 100;
        double confidence = Math.max(0, 100 - coefficientOfVariation);

        logger.info("Next month prediction: {:.2f} L (confidence: {:.2f}%)", average, confidence);
        return new Prediction(average, confidence, "Based on recent trends");
    }

    /**
     * Calculates savings potential based on consumption data.
     *
     * @param records    the water records
     * @param dailyLimit the daily limit in liters
     * @return SavingsPotential with estimated savings
     */
    public SavingsPotential calculateSavingsPotential(List<WaterRecord> records, double dailyLimit) {
        if (records.isEmpty()) {
            return new SavingsPotential(0, 0, "No data");
        }

        // Get last 30 days
        LocalDate thirtyDaysAgo = AppClock.today().minusDays(30);
        List<WaterRecord> monthRecords = records.stream()
                .filter(r -> r.getDate().isAfter(thirtyDaysAgo))
                .collect(Collectors.toList());

        double currentUsage = monthRecords.stream()
                .mapToDouble(WaterRecord::getLitersUsed)
                .sum();

        double targetUsage = dailyLimit * monthRecords.size();
        double potentialSavings = Math.max(0, currentUsage - targetUsage);
        double savingsPercentage = (potentialSavings / currentUsage) * 100;

        logger.info("Savings potential: {:.2f} L per month ({:.2f}%)", potentialSavings, savingsPercentage);
        return new SavingsPotential(potentialSavings, savingsPercentage, "Based on daily limit");
    }

    /**
     * Gets daily average consumption.
     *
     * @param records the water records
     * @return average daily consumption
     */
    public double getDailyAverage(List<WaterRecord> records) {
        if (records.isEmpty()) {
            return 0;
        }
        return records.stream()
                .mapToDouble(WaterRecord::getLitersUsed)
                .average()
                .orElse(0);
    }

    /**
     * Gets peak consumption day.
     *
     * @param records the water records
     * @return Optional with highest consumption day
     */
    public Optional<WaterRecord> getPeakDay(List<WaterRecord> records) {
        return records.stream()
                .max(Comparator.comparingDouble(WaterRecord::getLitersUsed));
    }

    /**
     * Gets lowest consumption day.
     *
     * @param records the water records
     * @return Optional with lowest consumption day
     */
    public Optional<WaterRecord> getLowestDay(List<WaterRecord> records) {
        return records.stream()
                .min(Comparator.comparingDouble(WaterRecord::getLitersUsed));
    }

    /**
     * Calculates standard deviation.
     *
     * @param values the array of values
     * @param mean   the mean value
     * @return standard deviation
     */
    private double calculateStandardDeviation(double[] values, double mean) {
        double variance = Arrays.stream(values)
                .map(x -> Math.pow(x - mean, 2))
                .average()
                .orElse(0);
        return Math.sqrt(variance);
    }

    /**
     * Inner class for trend analysis results.
     */
    public static class TrendAnalysis {
        public final double firstHalf;
        public final double secondHalf;
        public final String trend;

        public TrendAnalysis(double firstHalf, double secondHalf, String trend) {
            this.firstHalf = firstHalf;
            this.secondHalf = secondHalf;
            this.trend = trend;
        }

        public double getPercentageChange() {
            if (firstHalf == 0)
                return 0;
            return ((secondHalf - firstHalf) / firstHalf) * 100;
        }

        @Override
        public String toString() {
            return String.format("Trend: %s (%.2f%% change)", trend, getPercentageChange());
        }
    }

    /**
     * Inner class for anomaly detection results.
     */
    public static class AnomalyReport {
        public final LocalDate date;
        public final double value;
        public final String type;
        public final double zScore;

        public AnomalyReport(LocalDate date, double value, String type, double zScore) {
            this.date = date;
            this.value = value;
            this.type = type;
            this.zScore = zScore;
        }

        @Override
        public String toString() {
            return String.format("Anomaly on %s: %.2f L (%s, z-score: %.2f)", date, value, type, zScore);
        }
    }

    /**
     * Inner class for prediction results.
     */
    public static class Prediction {
        public final double predictedValue;
        public final double confidence;
        public final String basis;

        public Prediction(double predictedValue, double confidence, String basis) {
            this.predictedValue = predictedValue;
            this.confidence = confidence;
            this.basis = basis;
        }

        @Override
        public String toString() {
            return String.format("Prediction: %.2f L (confidence: %.2f%%) - %s", predictedValue, confidence, basis);
        }
    }

    /**
     * Inner class for savings potential.
     */
    public static class SavingsPotential {
        public final double savingsAmount;
        public final double savingsPercentage;
        public final String basis;

        public SavingsPotential(double savingsAmount, double savingsPercentage, String basis) {
            this.savingsAmount = savingsAmount;
            this.savingsPercentage = savingsPercentage;
            this.basis = basis;
        }

        @Override
        public String toString() {
            return String.format("Potential savings: %.2f L (%.2f%%) - %s", savingsAmount, savingsPercentage, basis);
        }
    }
}
