package com.smartwater.validation;

import com.smartwater.model.HouseholdProfile;
import com.smartwater.model.WaterRecord;
import com.smartwater.utils.AppClock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for validating application data models.
 * Provides comprehensive validation for all entities with detailed error
 * messages.
 */
public class ValidationUtil {
    private static final double MIN_DAILY_LIMIT = 1.0;
    private static final double MAX_DAILY_LIMIT = 10000.0;
    private static final double MIN_LITERS_USED = 0.001;
    private static final double MAX_LITERS_USED = 100000.0;
    private static final int MIN_MEMBER_COUNT = 1;
    private static final int MAX_MEMBER_COUNT = 20;
    private static final int MAX_STRING_LENGTH = 255;

    /**
     * Validates a WaterRecord for data integrity.
     *
     * @param record the WaterRecord to validate
     * @throws ValidationException if validation fails
     */
    public static void validateWaterRecord(WaterRecord record) throws ValidationException {
        List<String> errors = new ArrayList<>();

        if (record == null) {
            throw new ValidationException("WaterRecord cannot be null");
        }

        // Validate date
        if (record.getDate() == null) {
            errors.add("Date cannot be null");
        } else if (record.getDate().isAfter(AppClock.today())) {
            errors.add("Date cannot be in the future");
        }

        // Validate liters used
        if (record.getLitersUsed() < MIN_LITERS_USED) {
            errors.add("Liters used must be at least " + MIN_LITERS_USED);
        } else if (record.getLitersUsed() > MAX_LITERS_USED) {
            errors.add("Liters used exceeds maximum limit of " + MAX_LITERS_USED);
        }

        // Validate note
        if (record.getNote() != null && record.getNote().length() > MAX_STRING_LENGTH) {
            errors.add("Note exceeds maximum length of " + MAX_STRING_LENGTH + " characters");
        }

        if (!errors.isEmpty()) {
            throw new ValidationException("WaterRecord validation failed: " + String.join("; ", errors));
        }
    }

    /**
     * Validates a HouseholdProfile for data integrity.
     *
     * @param profile the HouseholdProfile to validate
     * @throws ValidationException if validation fails
     */
    public static void validateHouseholdProfile(HouseholdProfile profile) throws ValidationException {
        List<String> errors = new ArrayList<>();

        if (profile == null) {
            throw new ValidationException("HouseholdProfile cannot be null");
        }

        // Validate household name
        if (profile.getHouseholdName() == null || profile.getHouseholdName().trim().isEmpty()) {
            errors.add("Household name cannot be empty");
        } else if (profile.getHouseholdName().length() > MAX_STRING_LENGTH) {
            errors.add("Household name exceeds maximum length of " + MAX_STRING_LENGTH + " characters");
        }

        // Validate member count
        if (profile.getMemberCount() < MIN_MEMBER_COUNT) {
            errors.add("Member count must be at least " + MIN_MEMBER_COUNT);
        } else if (profile.getMemberCount() > MAX_MEMBER_COUNT) {
            errors.add("Member count cannot exceed " + MAX_MEMBER_COUNT);
        }

        // Validate daily limit
        if (profile.getDailyLimit() < MIN_DAILY_LIMIT) {
            errors.add("Daily limit must be at least " + MIN_DAILY_LIMIT + " liters");
        } else if (profile.getDailyLimit() > MAX_DAILY_LIMIT) {
            errors.add("Daily limit cannot exceed " + MAX_DAILY_LIMIT + " liters");
        }

        // Validate city
        if (profile.getCity() != null && profile.getCity().length() > MAX_STRING_LENGTH) {
            errors.add("City name exceeds maximum length of " + MAX_STRING_LENGTH + " characters");
        }

        if (!errors.isEmpty()) {
            throw new ValidationException("HouseholdProfile validation failed: " + String.join("; ", errors));
        }
    }

    /**
     * Validates that a string is not null or empty.
     *
     * @param value     the string to validate
     * @param fieldName the name of the field being validated
     * @throws ValidationException if validation fails
     */
    public static void validateNotEmpty(String value, String fieldName) throws ValidationException {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(fieldName + " cannot be empty", fieldName, value);
        }
    }

    /**
     * Validates that a value is within a numeric range.
     *
     * @param value     the value to validate
     * @param min       the minimum allowed value (inclusive)
     * @param max       the maximum allowed value (inclusive)
     * @param fieldName the name of the field being validated
     * @throws ValidationException if validation fails
     */
    public static void validateRange(double value, double min, double max, String fieldName)
            throws ValidationException {
        if (value < min || value > max) {
            throw new ValidationException(
                    String.format("%s must be between %.1f and %.1f", fieldName, min, max),
                    fieldName,
                    value);
        }
    }

    /**
     * Validates that a date is not in the future.
     *
     * @param date      the date to validate
     * @param fieldName the name of the field being validated
     * @throws ValidationException if validation fails
     */
    public static void validateNotFuture(LocalDate date, String fieldName) throws ValidationException {
        if (date != null && date.isAfter(AppClock.today())) {
            throw new ValidationException(fieldName + " cannot be in the future", fieldName, date);
        }
    }

    /**
     * Validates that a string length is within acceptable limits.
     *
     * @param value     the string to validate
     * @param maxLength the maximum allowed length
     * @param fieldName the name of the field being validated
     * @throws ValidationException if validation fails
     */
    public static void validateStringLength(String value, int maxLength, String fieldName) throws ValidationException {
        if (value != null && value.length() > maxLength) {
            throw new ValidationException(
                    String.format("%s exceeds maximum length of %d characters", fieldName, maxLength),
                    fieldName,
                    value);
        }
    }
}
