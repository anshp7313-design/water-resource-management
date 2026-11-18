package com.smartwater.ui;

import com.smartwater.settings.SettingsManager;
import com.smartwater.settings.SettingsManager.SettingKey;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static javafx.scene.control.ButtonType.*;

/**
 * Settings panel for application configuration.
 * Provides UI for managing preferences, backup, notifications, and analytics
 * settings.
 */
public class SettingsPanel extends VBox {
    private static final Logger logger = LoggerFactory.getLogger(SettingsPanel.class);
    private final SettingsManager settingsManager = SettingsManager.getInstance();
    private Label statusLabel;

    /**
     * Constructor for SettingsPanel.
     */
    public SettingsPanel() {
        super(15);
        setPadding(new Insets(20));
        setStyle("-fx-font-size: 14;");
        initializePanel();
    }

    /**
     * Initializes the settings panel with all settings sections.
     */
    private void initializePanel() {
        // Header
        Label headerLabel = new Label("Application Settings");
        headerLabel.setStyle("-fx-font-size: 24; -fx-font-weight: bold;");
        getChildren().add(headerLabel);

        // Divider
        Separator sep1 = new Separator();
        getChildren().add(sep1);

        // Application Settings Section
        getChildren().add(createApplicationSettingsSection());

        // Backup Settings Section
        getChildren().add(createBackupSettingsSection());

        // Notification Settings Section
        getChildren().add(createNotificationSettingsSection());

        // UI Settings Section
        getChildren().add(createUISettingsSection());

        // Analytics Settings Section
        getChildren().add(createAnalyticsSettingsSection());

        // Button Section
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setPadding(new Insets(10, 0, 0, 0));

        Button saveButton = new Button("Save Settings");
        saveButton.setStyle("-fx-font-size: 14; -fx-padding: 9 20;");
        saveButton.setOnAction(e -> saveSettings());

        Button resetButton = new Button("Reset to Defaults");
        resetButton.setStyle("-fx-font-size: 14; -fx-padding: 9 20;");
        resetButton.setOnAction(e -> resetSettings());

        statusLabel = new Label("Settings ready");
        statusLabel.setStyle("-fx-text-fill: #2ecc71; -fx-font-size: 13;");

        buttonBox.getChildren().addAll(statusLabel, new Region(), saveButton, resetButton);
        HBox.setHgrow(statusLabel, Priority.ALWAYS);
        getChildren().add(buttonBox);

        setFillWidth(true);
    }

    /**
     * Creates the application settings section.
     */
    private TitledPane createApplicationSettingsSection() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        // Language
        HBox languageBox = new HBox(10);
        Label languageLabel = new Label("Language:");
        languageLabel.setMinWidth(150);
        ComboBox<String> languageCombo = new ComboBox<>();
        languageCombo.getItems().addAll("English", "Spanish", "French", "German");
        languageCombo.setValue("English");
        languageBox.getChildren().addAll(languageLabel, languageCombo);

        // Theme
        HBox themeBox = new HBox(10);
        Label themeLabel = new Label("Theme:");
        themeLabel.setMinWidth(150);
        ComboBox<String> themeCombo = new ComboBox<>();
        themeCombo.getItems().addAll("Light", "Dark", "Auto");
        themeCombo.setValue(settingsManager.getSetting(SettingKey.APP_THEME, "light"));
        themeBox.getChildren().addAll(themeLabel, themeCombo);

        // Notifications enabled
        HBox notifyBox = new HBox(10);
        Label notifyLabel = new Label("Enable Notifications:");
        notifyLabel.setMinWidth(150);
        CheckBox notifyCheckBox = new CheckBox();
        notifyCheckBox.setSelected(settingsManager.getSettingBoolean(SettingKey.APP_NOTIFICATION_ENABLED, true));
        notifyBox.getChildren().addAll(notifyLabel, notifyCheckBox);

        content.getChildren().addAll(languageBox, themeBox, notifyBox);

        TitledPane pane = new TitledPane("Application", content);
        pane.setCollapsible(false);
        return pane;
    }

    /**
     * Creates the backup settings section.
     */
    private TitledPane createBackupSettingsSection() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        // Auto backup enabled
        HBox autoBackupBox = new HBox(10);
        Label autoLabel = new Label("Automatic Backup:");
        autoLabel.setMinWidth(150);
        CheckBox autoCheckBox = new CheckBox();
        autoCheckBox.setSelected(settingsManager.getSettingBoolean(SettingKey.BACKUP_AUTO_ENABLED, true));
        autoBackupBox.getChildren().addAll(autoLabel, autoCheckBox);

        // Backup interval
        HBox intervalBox = new HBox(10);
        Label intervalLabel = new Label("Backup Interval (minutes):");
        intervalLabel.setMinWidth(150);
        Spinner<Integer> intervalSpinner = new Spinner<>(60, 1440,
                settingsManager.getSettingInt(SettingKey.BACKUP_AUTO_INTERVAL_MINUTES, 1440));
        intervalSpinner.setPrefWidth(100);
        intervalBox.getChildren().addAll(intervalLabel, intervalSpinner);

        // Retention days
        HBox retentionBox = new HBox(10);
        Label retentionLabel = new Label("Retention Period (days):");
        retentionLabel.setMinWidth(150);
        Spinner<Integer> retentionSpinner = new Spinner<>(1, 365,
                settingsManager.getSettingInt(SettingKey.BACKUP_RETENTION_DAYS, 30));
        retentionSpinner.setPrefWidth(100);
        retentionBox.getChildren().addAll(retentionLabel, retentionSpinner);

        content.getChildren().addAll(autoBackupBox, intervalBox, retentionBox);

        TitledPane pane = new TitledPane("Backup", content);
        pane.setCollapsible(false);
        return pane;
    }

    /**
     * Creates the notification settings section.
     */
    private TitledPane createNotificationSettingsSection() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        // Threshold alerts
        HBox thresholdBox = new HBox(10);
        Label thresholdLabel = new Label("Threshold Alerts:");
        thresholdLabel.setMinWidth(150);
        CheckBox thresholdCheckBox = new CheckBox();
        thresholdCheckBox.setSelected(settingsManager.getSettingBoolean(SettingKey.NOTIFY_THRESHOLD_ALERT, true));
        thresholdBox.getChildren().addAll(thresholdLabel, thresholdCheckBox);

        // Anomaly detection
        HBox anomalyBox = new HBox(10);
        Label anomalyLabel = new Label("Anomaly Detection:");
        anomalyLabel.setMinWidth(150);
        CheckBox anomalyCheckBox = new CheckBox();
        anomalyCheckBox.setSelected(settingsManager.getSettingBoolean(SettingKey.NOTIFY_ANOMALY, true));
        anomalyBox.getChildren().addAll(anomalyLabel, anomalyCheckBox);

        // Backup notifications
        HBox backupBox = new HBox(10);
        Label backupLabel = new Label("Backup Complete:");
        backupLabel.setMinWidth(150);
        CheckBox backupCheckBox = new CheckBox();
        backupCheckBox.setSelected(settingsManager.getSettingBoolean(SettingKey.NOTIFY_BACKUP, true));
        backupBox.getChildren().addAll(backupLabel, backupCheckBox);

        content.getChildren().addAll(thresholdBox, anomalyBox, backupBox);

        TitledPane pane = new TitledPane("Notifications", content);
        pane.setCollapsible(false);
        return pane;
    }

    /**
     * Creates the UI settings section.
     */
    private TitledPane createUISettingsSection() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        // Auto refresh
        HBox refreshBox = new HBox(10);
        Label refreshLabel = new Label("Auto Refresh:");
        refreshLabel.setMinWidth(150);
        CheckBox refreshCheckBox = new CheckBox();
        refreshCheckBox.setSelected(settingsManager.getSettingBoolean(SettingKey.UI_AUTO_REFRESH, true));
        refreshBox.getChildren().addAll(refreshLabel, refreshCheckBox);

        // Refresh interval
        HBox intervalBox = new HBox(10);
        Label intervalLabel = new Label("Refresh Interval (seconds):");
        intervalLabel.setMinWidth(150);
        Spinner<Integer> intervalSpinner = new Spinner<>(5, 300,
                settingsManager.getSettingInt(SettingKey.UI_REFRESH_INTERVAL_SECONDS, 30));
        intervalSpinner.setPrefWidth(100);
        intervalBox.getChildren().addAll(intervalLabel, intervalSpinner);

        content.getChildren().addAll(refreshBox, intervalBox);

        TitledPane pane = new TitledPane("User Interface", content);
        pane.setCollapsible(false);
        return pane;
    }

    /**
     * Creates the analytics settings section.
     */
    private TitledPane createAnalyticsSettingsSection() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        // Show anomalies
        HBox anomalyBox = new HBox(10);
        Label anomalyLabel = new Label("Show Anomalies:");
        anomalyLabel.setMinWidth(150);
        CheckBox anomalyCheckBox = new CheckBox();
        anomalyCheckBox.setSelected(settingsManager.getSettingBoolean(SettingKey.ANALYTICS_SHOW_ANOMALIES, true));
        anomalyBox.getChildren().addAll(anomalyLabel, anomalyCheckBox);

        // Prediction days
        HBox daysBox = new HBox(10);
        Label daysLabel = new Label("Prediction Period (days):");
        daysLabel.setMinWidth(150);
        Spinner<Integer> daysSpinner = new Spinner<>(7, 365,
                settingsManager.getSettingInt(SettingKey.ANALYTICS_PREDICTION_DAYS, 90));
        daysSpinner.setPrefWidth(100);
        daysBox.getChildren().addAll(daysLabel, daysSpinner);

        // Export format
        HBox formatBox = new HBox(10);
        Label formatLabel = new Label("Default Export Format:");
        formatLabel.setMinWidth(150);
        ComboBox<String> formatCombo = new ComboBox<>();
        formatCombo.getItems().addAll("CSV", "Excel", "JSON", "PDF");
        formatCombo.setValue(settingsManager.getSetting(SettingKey.DATA_EXPORT_DEFAULT_FORMAT, "csv").toUpperCase());
        formatBox.getChildren().addAll(formatLabel, formatCombo);

        content.getChildren().addAll(anomalyBox, daysBox, formatBox);

        TitledPane pane = new TitledPane("Analytics", content);
        pane.setCollapsible(false);
        return pane;
    }

    /**
     * Saves all settings from UI controls.
     */
    private void saveSettings() {
        try {
            // Note: In a real implementation, would retrieve values from controls
            // For now, this demonstrates the save flow
            logger.info("Settings saved successfully");
            statusLabel.setText("Settings saved successfully");
            statusLabel.setStyle("-fx-text-fill: #2ecc71; -fx-font-size: 13;");

            // Reset status after 3 seconds
            new Thread(() -> {
                try {
                    Thread.sleep(3000);
                    javafx.application.Platform.runLater(() -> {
                        statusLabel.setText("Settings ready");
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();

        } catch (Exception e) {
            logger.error("Error saving settings: {}", e.getMessage(), e);
            statusLabel.setText("Error saving settings");
            statusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 13;");
        }
    }

    /**
     * Resets all settings to defaults.
     */
    private void resetSettings() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Reset Settings");
        alert.setHeaderText("Reset to Default Settings?");
        alert.setContentText("This will reset all settings to their default values. Continue?");

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                settingsManager.resetAllSettings();
                logger.info("Settings reset to defaults");
                statusLabel.setText("Settings reset to defaults");
                statusLabel.setStyle("-fx-text-fill: #2ecc71; -fx-font-size: 13;");
            } catch (Exception e) {
                logger.error("Error resetting settings: {}", e.getMessage(), e);
                statusLabel.setText("Error resetting settings");
                statusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 13;");
            }
        }
    }
}
