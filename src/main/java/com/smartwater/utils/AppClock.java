package com.smartwater.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public final class AppClock {
    private static final String TODAY_PROPERTY = "smartwater.today";
    private static final String TODAY_ENV = "SMARTWATER_TODAY";

    private AppClock() {
    }

    public static LocalDate today() {
        String configuredDate = System.getProperty(TODAY_PROPERTY);
        if (configuredDate == null || configuredDate.isBlank()) {
            configuredDate = System.getenv(TODAY_ENV);
        }
        if (configuredDate != null && !configuredDate.isBlank()) {
            return LocalDate.parse(configuredDate.trim());
        }
        return LocalDate.now();
    }

    public static LocalDateTime now() {
        return LocalDateTime.of(today(), LocalTime.now());
    }
}
