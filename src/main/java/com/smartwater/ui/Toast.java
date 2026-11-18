package com.smartwater.ui;

import javafx.animation.PauseTransition;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Toast notification that appears briefly at the bottom right of the screen.
 */
public class Toast {
    private static final Logger logger = LoggerFactory.getLogger(Toast.class);
    private final Stage stage;
    private final Label messageLabel;
    private final String type;

    /**
     * Toast type enumeration.
     */
    public enum ToastType {
        SUCCESS("#2ecc71"),
        ERROR("#e74c3c"),
        WARNING("#f39c12"),
        INFO("#3498db");

        public final String color;

        ToastType(String color) {
            this.color = color;
        }
    }

    /**
     * Creates a toast notification.
     *
     * @param message the message to display
     * @param type    the toast type
     */
    public Toast(String message, ToastType type) {
        this.type = type.name();
        this.stage = new Stage();
        this.messageLabel = new Label(message);

        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setAlwaysOnTop(true);

        // Create content
        VBox content = new VBox();
        content.setStyle(
                "-fx-background-color: " + type.color + "; " +
                        "-fx-text-fill: white; " +
                        "-fx-padding: 15; " +
                        "-fx-border-radius: 5; " +
                        "-fx-background-radius: 5;");
        content.setAlignment(Pos.CENTER);

        messageLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14; -fx-font-weight: 700;");
        content.getChildren().add(messageLabel);

        StackPane root = new StackPane(content);
        root.setStyle("-fx-background-color: transparent;");

        Scene scene = new Scene(root);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        stage.setScene(scene);

        logger.debug("Toast created: {} - {}", type.name(), message);
    }

    /**
     * Shows the toast notification for the specified duration.
     *
     * @param durationMs the duration in milliseconds
     */
    public void show(long durationMs) {
        // Position at bottom right
        java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        stage.setX(screenSize.width - 320);
        stage.setY(screenSize.height - 100);

        stage.show();
        logger.debug("Toast displayed for {} ms", durationMs);

        // Auto-hide after duration
        PauseTransition delay = new PauseTransition(Duration.millis(durationMs));
        delay.setOnFinished(event -> stage.close());
        delay.play();
    }

    /**
     * Shows the toast notification for 3 seconds (default).
     */
    public void show() {
        show(3000);
    }

    /**
     * Closes the toast.
     */
    public void close() {
        if (stage != null) {
            stage.close();
        }
    }

    /**
     * Static convenience method for success toast.
     *
     * @param message the message
     */
    public static void showSuccess(String message) {
        new Toast(message, ToastType.SUCCESS).show();
    }

    /**
     * Static convenience method for error toast.
     *
     * @param message the message
     */
    public static void showError(String message) {
        new Toast(message, ToastType.ERROR).show(4000);
    }

    /**
     * Static convenience method for warning toast.
     *
     * @param message the message
     */
    public static void showWarning(String message) {
        new Toast(message, ToastType.WARNING).show(3500);
    }

    /**
     * Static convenience method for info toast.
     *
     * @param message the message
     */
    public static void showInfo(String message) {
        new Toast(message, ToastType.INFO).show();
    }
}
