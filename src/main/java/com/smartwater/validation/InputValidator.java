package com.smartwater.validation;

import java.util.regex.Pattern;

/**
 * Utility class for validating user input in the authentication UI.
 * Provides real-time validation feedback for form fields.
 */
public class InputValidator {

    // Validation patterns
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final Pattern USERNAME_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_-]{3,20}$");

    private static final Pattern FULLNAME_PATTERN = Pattern.compile(
            "^[a-zA-Z\\s'-]{2,50}$");

    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 128;

    /**
     * Validates email format.
     */
    public static ValidationResult validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return new ValidationResult(false, "Email is required");
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return new ValidationResult(false, "Please enter a valid email address");
        }
        return new ValidationResult(true, "");
    }

    /**
     * Validates username format and length.
     */
    public static ValidationResult validateUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return new ValidationResult(false, "Username is required");
        }
        if (username.length() < 3) {
            return new ValidationResult(false, "Username must be at least 3 characters");
        }
        if (username.length() > 20) {
            return new ValidationResult(false, "Username must not exceed 20 characters");
        }
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            return new ValidationResult(false, "Username can only contain letters, numbers, hyphens, and underscores");
        }
        return new ValidationResult(true, "");
    }

    /**
     * Validates full name format.
     */
    public static ValidationResult validateFullName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return new ValidationResult(false, "Full name is required");
        }
        if (fullName.length() < 2) {
            return new ValidationResult(false, "Full name must be at least 2 characters");
        }
        if (fullName.length() > 50) {
            return new ValidationResult(false, "Full name must not exceed 50 characters");
        }
        if (!FULLNAME_PATTERN.matcher(fullName).matches()) {
            return new ValidationResult(false, "Full name can only contain letters, spaces, hyphens, and apostrophes");
        }
        return new ValidationResult(true, "");
    }

    /**
     * Validates password strength and requirements.
     */
    public static ValidationResult validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            return new ValidationResult(false, "Password is required");
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            return new ValidationResult(false, "Password must be at least 8 characters");
        }
        if (password.length() > MAX_PASSWORD_LENGTH) {
            return new ValidationResult(false, "Password is too long");
        }
        if (!password.matches(".*[a-z].*")) {
            return new ValidationResult(false, "Password must contain lowercase letters");
        }
        if (!password.matches(".*[A-Z].*")) {
            return new ValidationResult(false, "Password must contain uppercase letters");
        }
        if (!password.matches(".*\\d.*")) {
            return new ValidationResult(false, "Password must contain numbers");
        }
        return new ValidationResult(true, "");
    }

    /**
     * Validates password confirmation.
     */
    public static ValidationResult validatePasswordConfirmation(String password, String confirmation) {
        if (confirmation == null || confirmation.isEmpty()) {
            return new ValidationResult(false, "Please confirm your password");
        }
        if (!password.equals(confirmation)) {
            return new ValidationResult(false, "Passwords do not match");
        }
        return new ValidationResult(true, "");
    }

    /**
     * Calculates password strength (0-3).
     * 0: Weak (length only)
     * 1: Fair (lowercase + uppercase)
     * 2: Good (lowercase + uppercase + numbers)
     * 3: Strong (lowercase + uppercase + numbers + special chars)
     */
    public static int calculatePasswordStrength(String password) {
        if (password == null || password.length() < 8) {
            return 0;
        }

        boolean hasLower = password.matches(".*[a-z].*");
        boolean hasUpper = password.matches(".*[A-Z].*");
        boolean hasDigit = password.matches(".*\\d.*");
        boolean hasSpecial = password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*");

        int strength = 0;
        if (hasLower && hasUpper)
            strength = 1;
        if (hasLower && hasUpper && hasDigit)
            strength = 2;
        if (hasLower && hasUpper && hasDigit && hasSpecial)
            strength = 3;

        return strength;
    }

    /**
     * Result object for validation operations.
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String message;

        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }
    }
}
