package com.smartwater.model;

import java.time.LocalDateTime;

/**
 * Represents a physical or virtual water meter.
 */
public class Meter {
    private final int id;
    private final String name;
    private final String location;
    private final boolean active;
    private final LocalDateTime createdAt;

    public Meter(int id, String name, String location, boolean active, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.active = active;
        this.createdAt = createdAt;
    }

    public Meter(String name, String location) {
        this(0, name, location, true, LocalDateTime.now());
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getLocation() {
        return location;
    }

    public boolean isActive() {
        return active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return name + (location == null || location.isBlank() ? "" : " (" + location + ")");
    }
}
