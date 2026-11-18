package com.smartwater.model;

import java.time.LocalDateTime;

/**
 * Represents a user of the SmartWater application.
 * Supports multi-user functionality with authentication.
 */
public class User {
    private int id;
    private String username;
    private String email;
    private String passwordHash;
    private String fullName;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;
    private String role; // "admin" or "user"

    /**
     * Constructs a User with all required parameters.
     */
    public User(String username, String email, String passwordHash, String fullName) {
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.active = true;
        this.role = "user";
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Constructs a User from database record.
     */
    public User(int id, String username, String email, String passwordHash, String fullName,
            boolean active, LocalDateTime createdAt, LocalDateTime lastLogin, String role) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.active = active;
        this.createdAt = createdAt;
        this.lastLogin = lastLogin;
        this.role = role;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", fullName='" + fullName + '\'' +
                ", active=" + active +
                ", role='" + role + '\'' +
                ", createdAt=" + createdAt +
                ", lastLogin=" + lastLogin +
                '}';
    }
}
