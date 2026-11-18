package com.smartwater.model;

import java.time.LocalDateTime;

/**
 * A cumulative reading captured from a water meter.
 */
public class MeterReading {
    private final int id;
    private final int meterId;
    private final LocalDateTime readingTime;
    private final double readingLiters;
    private final String note;

    public MeterReading(int id, int meterId, LocalDateTime readingTime, double readingLiters, String note) {
        this.id = id;
        this.meterId = meterId;
        this.readingTime = readingTime;
        this.readingLiters = readingLiters;
        this.note = note;
    }

    public MeterReading(int meterId, LocalDateTime readingTime, double readingLiters, String note) {
        this(0, meterId, readingTime, readingLiters, note);
    }

    public int getId() {
        return id;
    }

    public int getMeterId() {
        return meterId;
    }

    public LocalDateTime getReadingTime() {
        return readingTime;
    }

    public double getReadingLiters() {
        return readingLiters;
    }

    public String getNote() {
        return note;
    }
}
