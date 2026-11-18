package com.smartwater.ui;

import com.smartwater.settings.SettingsManager;
import com.smartwater.settings.SettingsManager.SettingKey;
import javafx.scene.Scene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Theme manager for handling light/dark mode and styling.
 */
public class ThemeManager {
    private static final Logger logger = LoggerFactory.getLogger(ThemeManager.class);
    private static ThemeManager instance;
    private final SettingsManager settingsManager = SettingsManager.getInstance();
    private final String baseStylesheet = getClass().getResource("/style.css") != null
            ? getClass().getResource("/style.css").toExternalForm()
            : null;
    private String currentTheme = "light";
    private Scene currentScene;

    /**
     * Theme enumeration.
     */
    public enum Theme {
        LIGHT("light"),
        DARK("dark"),
        AUTO("auto");

        public final String value;

        Theme(String value) {
            this.value = value;
        }
    }

    /**
     * Private constructor for singleton pattern.
     */
    private ThemeManager() {
        loadThemePreference();
        logger.info("ThemeManager initialized with theme: {}", currentTheme);
    }

    /**
     * Gets the singleton instance.
     *
     * @return the ThemeManager instance
     */
    public static synchronized ThemeManager getInstance() {
        if (instance == null) {
            instance = new ThemeManager();
        }
        return instance;
    }

    /**
     * Loads the theme preference from settings.
     */
    private void loadThemePreference() {
        currentTheme = settingsManager.getSetting(SettingKey.APP_THEME, "light");
    }

    /**
     * Sets the current scene for theme application.
     *
     * @param scene the JavaFX scene
     */
    public void setScene(Scene scene) {
        this.currentScene = scene;
        applyTheme();
    }

    /**
     * Gets the current theme.
     *
     * @return the current theme
     */
    public String getCurrentTheme() {
        return currentTheme;
    }

    /**
     * Sets the theme and applies it.
     *
     * @param theme the theme to apply
     */
    public void setTheme(Theme theme) {
        setTheme(theme.value);
    }

    /**
     * Sets the theme by name and applies it.
     *
     * @param themeName the theme name
     */
    public void setTheme(String themeName) {
        currentTheme = themeName.toLowerCase();
        settingsManager.setSetting(SettingKey.APP_THEME, currentTheme);
        applyTheme();
        logger.info("Theme changed to: {}", currentTheme);
    }

    /**
     * Applies the current theme to the scene.
     */
    public void applyTheme() {
        if (currentScene == null) {
            logger.warn("Scene not set, cannot apply theme");
            return;
        }

        currentScene.getStylesheets().removeIf(stylesheet -> stylesheet != null
                && (stylesheet.endsWith("light-theme.css") || stylesheet.endsWith("dark-theme.css")));

        if (baseStylesheet != null && !currentScene.getStylesheets().contains(baseStylesheet)) {
            currentScene.getStylesheets().add(0, baseStylesheet);
        }

        String stylesheet = getThemeStylesheet();
        if (stylesheet != null) {
            if (!currentScene.getStylesheets().contains(stylesheet)) {
                currentScene.getStylesheets().add(stylesheet);
            }
            logger.debug("Applied stylesheet: {}", stylesheet);
        }
    }

    /**
     * Gets the stylesheet URL for the current theme.
     *
     * @return stylesheet URL or null if not found
     */
    private String getThemeStylesheet() {
        String theme = currentTheme;

        // If auto, detect system preference (simple implementation)
        if ("auto".equals(theme)) {
            theme = isSystemDarkMode() ? "dark" : "light";
        }

        switch (theme) {
            case "dark":
                return getClass().getResource("/styles/dark-theme.css") != null
                        ? getClass().getResource("/styles/dark-theme.css").toExternalForm()
                        : null;
            case "light":
            default:
                return getClass().getResource("/styles/light-theme.css") != null
                        ? getClass().getResource("/styles/light-theme.css").toExternalForm()
                        : null;
        }
    }

    /**
     * Detects if system prefers dark mode.
     *
     * @return true if dark mode is preferred
     */
    private boolean isSystemDarkMode() {
        try {
            // Simple heuristic: check system property
            String osName = System.getProperty("os.name").toLowerCase();
            if (osName.contains("win")) {
                // On Windows, could check registry, for now default to light
                return false;
            } else if (osName.contains("mac")) {
                // On macOS, could check appearance setting
                return false;
            }
        } catch (Exception e) {
            logger.debug("Could not detect system theme preference: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Gets the primary color for current theme.
     *
     * @return color hex code
     */
    public String getPrimaryColor() {
        if ("dark".equals(currentTheme)) {
            return "#1e1e1e";
        }
        return "#ffffff";
    }

    /**
     * Gets the text color for current theme.
     *
     * @return color hex code
     */
    public String getTextColor() {
        if ("dark".equals(currentTheme)) {
            return "#e0e0e0";
        }
        return "#212121";
    }

    /**
     * Gets the accent color for current theme.
     *
     * @return color hex code
     */
    public String getAccentColor() {
        return "#3498db"; // Blue accent for both themes
    }

    /**
     * Checks if current theme is dark.
     *
     * @return true if dark theme is active
     */
    public boolean isDarkTheme() {
        return "dark".equals(currentTheme);
    }

    /**
     * Gets CSS string for background based on theme.
     *
     * @return CSS style string
     */
    public String getBackgroundStyle() {
        if ("dark".equals(currentTheme)) {
            return "-fx-control-inner-background: #2a2a2a; -fx-text-fill: #e0e0e0;";
        }
        return "-fx-control-inner-background: #ffffff; -fx-text-fill: #000000;";
    }

    /**
     * Gets CSS string for border based on theme.
     *
     * @return CSS style string
     */
    public String getBorderStyle() {
        if ("dark".equals(currentTheme)) {
            return "-fx-border-color: #404040;";
        }
        return "-fx-border-color: #cccccc;";
    }
}
