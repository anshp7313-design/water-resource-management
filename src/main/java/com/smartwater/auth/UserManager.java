package com.smartwater.auth;

import com.smartwater.model.User;
import com.smartwater.database.DatabaseConnection;
import com.smartwater.database.DatabaseSchema;
import com.smartwater.validation.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Manages user accounts for multi-user support.
 * Handles user creation, authentication, and profile management.
 */
public class UserManager {
    private static final Logger logger = LoggerFactory.getLogger(UserManager.class);
    private static UserManager instance;
    private DatabaseConnection dbConnection;

    /**
     * Private constructor for singleton pattern.
     */
    private UserManager() {
        this.dbConnection = DatabaseConnection.getInstance();
        logger.info("UserManager initialized");
    }

    /**
     * Gets the singleton instance of UserManager.
     *
     * @return the UserManager instance
     */
    public static synchronized UserManager getInstance() {
        if (instance == null) {
            instance = new UserManager();
        }
        return instance;
    }

    /**
     * Creates a new user account.
     *
     * @param username the username
     * @param email    the email address
     * @param password the plain text password
     * @param fullName the user's full name
     * @return the created User object
     * @throws ValidationException if validation fails
     */
    public User createUser(String username, String email, String password, String fullName) throws ValidationException {
        validateUserInput(username, email, password, fullName);

        // Check if user already exists
        if (userExists(username)) {
            throw new ValidationException("Username already exists: " + username, "username", username);
        }

        if (emailExists(email)) {
            throw new ValidationException("Email already registered: " + email, "email", email);
        }

        // Hash password
        String passwordHash = AuthenticationService.hashPassword(password);

        try {
            logger.info("Creating new user: {}", username);

            String sql = """
                        INSERT INTO users (username, email, password_hash, full_name, role)
                        VALUES (?, ?, ?, ?, 'user')
                    """;

            long userId = dbConnection.executeInsertReturnKey(sql, username, email, passwordHash, fullName);

            User user = new User((int) userId, username, email, passwordHash, fullName, true,
                    LocalDateTime.now(), null, "user");
            new DatabaseSchema(dbConnection).adoptLegacyDataForUser(user.getId());
            logger.info("User created successfully: {}", username);
            return user;

        } catch (SQLException e) {
            logger.error("Database error while creating user: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create user", e);
        }
    }

    /**
     * Authenticates a user with username and password.
     *
     * @param username the username
     * @param password the plain text password
     * @return Optional containing the User if authentication succeeds, empty
     *         otherwise
     */
    public Optional<User> authenticate(String username, String password) {
        try {
            logger.info("Attempting authentication for user: {}", username);

            String sql = """
                        SELECT id, username, email, password_hash, full_name, active, created_at, last_login, role
                        FROM users WHERE username = ? AND active = 1
                    """;

            try (ResultSet rs = dbConnection.executeQuery(sql, username)) {
                if (rs.next()) {
                    String passwordHash = rs.getString("password_hash");

                    // Verify password
                    if (AuthenticationService.verifyPassword(password, passwordHash)) {
                        User user = extractUserFromResultSet(rs);

                        // Update last login time
                        updateLastLogin(user.getId());

                        logger.info("User authenticated successfully: {}", username);
                        return Optional.of(user);
                    } else {
                        logger.warn("Authentication failed: incorrect password for user {}", username);
                    }
                } else {
                    logger.warn("Authentication failed: user not found or inactive {}", username);
                }
            }

        } catch (SQLException e) {
            logger.error("Database error during authentication: {}", e.getMessage(), e);
        }

        return Optional.empty();
    }

    /**
     * Gets a user by ID.
     *
     * @param userId the user ID
     * @return Optional containing the User if found
     */
    public Optional<User> getUserById(int userId) {
        try {
            String sql = """
                        SELECT id, username, email, password_hash, full_name, active, created_at, last_login, role
                        FROM users WHERE id = ?
                    """;

            try (ResultSet rs = dbConnection.executeQuery(sql, userId)) {
                if (rs.next()) {
                    return Optional.of(extractUserFromResultSet(rs));
                }
            }

        } catch (SQLException e) {
            logger.error("Database error while retrieving user: {}", e.getMessage(), e);
        }

        return Optional.empty();
    }

    /**
     * Gets a user by username.
     *
     * @param username the username
     * @return Optional containing the User if found
     */
    public Optional<User> getUserByUsername(String username) {
        try {
            String sql = """
                        SELECT id, username, email, password_hash, full_name, active, created_at, last_login, role
                        FROM users WHERE username = ?
                    """;

            try (ResultSet rs = dbConnection.executeQuery(sql, username)) {
                if (rs.next()) {
                    return Optional.of(extractUserFromResultSet(rs));
                }
            }

        } catch (SQLException e) {
            logger.error("Database error while retrieving user: {}", e.getMessage(), e);
        }

        return Optional.empty();
    }

    /**
     * Gets all active users.
     *
     * @return list of active users
     */
    public List<User> getAllActiveUsers() {
        List<User> users = new ArrayList<>();

        try {
            String sql = """
                        SELECT id, username, email, password_hash, full_name, active, created_at, last_login, role
                        FROM users WHERE active = 1 ORDER BY created_at DESC
                    """;

            try (ResultSet rs = dbConnection.executeQuery(sql)) {
                while (rs.next()) {
                    users.add(extractUserFromResultSet(rs));
                }
            }

            logger.info("Retrieved {} active users", users.size());

        } catch (SQLException e) {
            logger.error("Database error while retrieving users: {}", e.getMessage(), e);
        }

        return users;
    }

    /**
     * Updates a user profile.
     *
     * @param user the user to update
     * @throws ValidationException if validation fails
     */
    public void updateUser(User user) throws ValidationException {
        if (user == null || user.getId() <= 0) {
            throw new ValidationException("Invalid user");
        }

        try {
            logger.info("Updating user: {}", user.getUsername());

            String sql = """
                        UPDATE users SET email = ?, full_name = ?, active = ?, role = ?
                        WHERE id = ?
                    """;

            dbConnection.executeUpdate(sql, user.getEmail(), user.getFullName(),
                    user.isActive() ? 1 : 0, user.getRole(), user.getId());

            logger.info("User updated successfully: {}", user.getUsername());

        } catch (SQLException e) {
            logger.error("Database error while updating user: {}", e.getMessage(), e);
        }
    }

    /**
     * Changes a user's password.
     *
     * @param userId      the user ID
     * @param newPassword the new plain text password
     * @throws ValidationException if validation fails
     */
    public void changePassword(int userId, String newPassword) throws ValidationException {
        if (newPassword == null || newPassword.isEmpty()) {
            throw new ValidationException("Password cannot be empty", "password", null);
        }

        String passwordHash = AuthenticationService.hashPassword(newPassword);

        try {
            logger.info("Changing password for user ID: {}", userId);

            String sql = "UPDATE users SET password_hash = ? WHERE id = ?";
            dbConnection.executeUpdate(sql, passwordHash, userId);

            logger.info("Password changed successfully for user ID: {}", userId);

        } catch (SQLException e) {
            logger.error("Database error while changing password: {}", e.getMessage(), e);
        }
    }

    /**
     * Deactivates a user account.
     *
     * @param userId the user ID
     */
    public void deactivateUser(int userId) {
        try {
            logger.info("Deactivating user ID: {}", userId);

            String sql = "UPDATE users SET active = 0 WHERE id = ?";
            dbConnection.executeUpdate(sql, userId);

            logger.info("User deactivated successfully: {}", userId);

        } catch (SQLException e) {
            logger.error("Database error while deactivating user: {}", e.getMessage(), e);
        }
    }

    /**
     * Checks if a username exists.
     *
     * @param username the username to check
     * @return true if username exists
     */
    private boolean userExists(String username) {
        try {
            String sql = "SELECT COUNT(*) as count FROM users WHERE username = ?";
            try (ResultSet rs = dbConnection.executeQuery(sql, username)) {
                if (rs.next()) {
                    return rs.getInt("count") > 0;
                }
            }
        } catch (SQLException e) {
            logger.error("Error checking user existence: {}", e.getMessage(), e);
        }
        return false;
    }

    /**
     * Checks if an email exists.
     *
     * @param email the email to check
     * @return true if email exists
     */
    private boolean emailExists(String email) {
        try {
            String sql = "SELECT COUNT(*) as count FROM users WHERE email = ?";
            try (ResultSet rs = dbConnection.executeQuery(sql, email)) {
                if (rs.next()) {
                    return rs.getInt("count") > 0;
                }
            }
        } catch (SQLException e) {
            logger.error("Error checking email existence: {}", e.getMessage(), e);
        }
        return false;
    }

    /**
     * Updates the last login timestamp for a user.
     *
     * @param userId the user ID
     */
    private void updateLastLogin(int userId) {
        try {
            String sql = "UPDATE users SET last_login = CURRENT_TIMESTAMP WHERE id = ?";
            dbConnection.executeUpdate(sql, userId);
        } catch (SQLException e) {
            logger.error("Error updating last login: {}", e.getMessage(), e);
        }
    }

    /**
     * Extracts a User object from a ResultSet.
     *
     * @param rs the ResultSet
     * @return the extracted User
     * @throws SQLException if column access fails
     */
    private User extractUserFromResultSet(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String username = rs.getString("username");
        String email = rs.getString("email");
        String passwordHash = rs.getString("password_hash");
        String fullName = rs.getString("full_name");
        boolean active = rs.getInt("active") == 1;
        LocalDateTime createdAt = rs.getTimestamp("created_at") != null
                ? rs.getTimestamp("created_at").toLocalDateTime()
                : null;
        LocalDateTime lastLogin = rs.getTimestamp("last_login") != null
                ? rs.getTimestamp("last_login").toLocalDateTime()
                : null;
        String role = rs.getString("role");

        return new User(id, username, email, passwordHash, fullName, active, createdAt, lastLogin, role);
    }

    /**
     * Validates user input for account creation.
     *
     * @throws ValidationException if validation fails
     */
    private void validateUserInput(String username, String email, String password, String fullName)
            throws ValidationException {
        List<String> errors = new ArrayList<>();

        if (username == null || username.trim().isEmpty()) {
            errors.add("Username cannot be empty");
        } else if (username.length() < 3) {
            errors.add("Username must be at least 3 characters long");
        } else if (!username.matches("^[a-zA-Z0-9_]+$")) {
            errors.add("Username can only contain letters, numbers, and underscores");
        }

        if (email == null || email.trim().isEmpty()) {
            errors.add("Email cannot be empty");
        } else if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            errors.add("Email format is invalid");
        }

        if (!AuthenticationService.isStrongPassword(password)) {
            errors.add(
                    "Password must be at least 8 characters with uppercase, lowercase, numbers, and special characters");
        }

        if (fullName == null || fullName.trim().isEmpty()) {
            errors.add("Full name cannot be empty");
        }

        if (!errors.isEmpty()) {
            throw new ValidationException("User input validation failed: " + String.join("; ", errors));
        }
    }
}
