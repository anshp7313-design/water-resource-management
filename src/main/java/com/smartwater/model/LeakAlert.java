package com.smartwater.model;

import java.time.LocalDate;

/**
 * Result of leak/spike analysis.
 */
public class LeakAlert {
    public enum Severity {
        NONE,
        WATCH,
        WARNING,
        CRITICAL
    }

    private final Severity severity;
    private final LocalDate date;
    private final String message;
    private final double observedLiters;
    private final double baselineLiters;

    public LeakAlert(Severity severity, LocalDate date, String message, double observedLiters, double baselineLiters) {
        this.severity = severity;
        this.date = date;
        this.message = message;
        this.observedLiters = observedLiters;
        this.baselineLiters = baselineLiters;
    }

    public static LeakAlert none() {
        return new LeakAlert(Severity.NONE, null, "No leak pattern detected.", 0, 0);
    }

    public Severity getSeverity() {
        return severity;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getMessage() {
        return message;
    }

    public double getObservedLiters() {
        return observedLiters;
    }

    public double getBaselineLiters() {
        return baselineLiters;
    }
}
