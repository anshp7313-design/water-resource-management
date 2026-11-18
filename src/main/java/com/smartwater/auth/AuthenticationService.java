package com.smartwater.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Authentication service using PBKDF2 for secure password hashing and
 * verification.
 * PBKDF2 is a standard, built-in Java algorithm that doesn't require external
 * libraries.
 */
public class AuthenticationService {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_LENGTH = 32;

    /**
     * Hashes a plain text password using PBKDF2.
     *
     * @param plainPassword the plain text password
     * @return the hashed password with salt
     */
    public static String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }

        try {
            byte[] salt = new byte[SALT_LENGTH];
            new SecureRandom().nextBytes(salt);

            PBEKeySpec spec = new PBEKeySpec(
                    plainPassword.toCharArray(),
                    salt,
                    ITERATIONS,
                    KEY_LENGTH);

            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            byte[] hash = factory.generateSecret(spec).getEncoded();

            // Combine salt and hash for storage
            byte[] combined = new byte[salt.length + hash.length];
            System.arraycopy(salt, 0, combined, 0, salt.length);
            System.arraycopy(hash, 0, combined, salt.length, hash.length);

            String hashed = Base64.getEncoder().encodeToString(combined);
            logger.debug("Password hashed successfully using PBKDF2");
            return hashed;

        } catch (Exception e) {
            logger.error("Error hashing password: {}", e.getMessage(), e);
            throw new RuntimeException("Password hashing failed", e);
        }
    }

    /**
     * Verifies a plain text password against a PBKDF2 hash.
     *
     * @param plainPassword  the plain text password to verify
     * @param hashedPassword the hash to compare against
     * @return true if password matches, false otherwise
     */
    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null) {
            logger.warn("Null password provided for verification");
            return false;
        }

        try {
            // Decode the stored hash to extract salt and hash
            byte[] combined = Base64.getDecoder().decode(hashedPassword);

            // Extract salt and hash
            byte[] salt = new byte[SALT_LENGTH];
            byte[] hash = new byte[combined.length - SALT_LENGTH];
            System.arraycopy(combined, 0, salt, 0, SALT_LENGTH);
            System.arraycopy(combined, SALT_LENGTH, hash, 0, hash.length);

            // Hash the provided password with the extracted salt
            PBEKeySpec spec = new PBEKeySpec(
                    plainPassword.toCharArray(),
                    salt,
                    ITERATIONS,
                    KEY_LENGTH);

            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            byte[] testHash = factory.generateSecret(spec).getEncoded();

            // Compare hashes
            boolean matches = constantTimeEquals(hash, testHash);
            if (matches) {
                logger.debug("Password verification successful");
            } else {
                logger.warn("Password verification failed");
            }
            return matches;

        } catch (IllegalArgumentException e) {
            logger.error("Invalid password hash format: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            logger.error("Error verifying password: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Constant-time comparison to prevent timing attacks.
     */
    private static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a.length != b.length) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }

    /**
     * Checks if a password needs rehashing.
     *
     * @param hashedPassword the hashed password to check
     * @return true if rehashing is recommended
     */
    public static boolean needsRehash(String hashedPassword) {
        if (hashedPassword == null || hashedPassword.isEmpty()) {
            return true;
        }

        // If we ever need to change iterations or algorithm, this would return true
        // For now, always false since PBKDF2 is stable
        return false;
    }

    /**
     * Validates password strength.
     *
     * @param password the password to validate
     * @return true if password meets strength requirements
     */
    public static boolean isStrongPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }

        boolean hasUpperCase = password.matches(".*[A-Z].*");
        boolean hasLowerCase = password.matches(".*[a-z].*");
        boolean hasDigit = password.matches(".*\\d.*");
        boolean hasSpecialChar = password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*");

        return hasUpperCase && hasLowerCase && hasDigit && hasSpecialChar;
    }

    /**
     * Gets password strength feedback.
     *
     * @param password the password to evaluate
     * @return feedback message
     */
    public static String getPasswordStrengthFeedback(String password) {
        if (password == null || password.isEmpty()) {
            return "Password is required";
        }

        if (password.length() < 8) {
            return "Password must be at least 8 characters long";
        }

        StringBuilder feedback = new StringBuilder();

        if (!password.matches(".*[A-Z].*")) {
            feedback.append("Add uppercase letters. ");
        }
        if (!password.matches(".*[a-z].*")) {
            feedback.append("Add lowercase letters. ");
        }
        if (!password.matches(".*\\d.*")) {
            feedback.append("Add digits. ");
        }
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
            feedback.append("Add special characters. ");
        }

        if (feedback.length() == 0) {
            return "Password strength is strong";
        }

        return feedback.toString().trim();
    }
}
