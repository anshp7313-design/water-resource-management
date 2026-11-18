package com.smartwater.ui;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static javafx.scene.control.ButtonType.*;

/**
 * Utility class for showing enhanced error, warning, and info dialogs.
 */
public class DialogUtil {
    private static final Logger logger = LoggerFactory.getLogger(DialogUtil.class);

    /**
     * Shows an error dialog with detailed information.
     *
     * @param title   the dialog title
     * @param header  the header text
     * @param message the error message
     */
    public static void showError(String title, String header, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
        logger.error("Error dialog shown: {} - {}", title, message);
    }

    /**
     * Shows an error dialog with exception details.
     *
     * @param title     the dialog title
     * @param header    the header text
     * @param message   the error message
     * @param exception the exception to display details for
     */
    public static void showError(String title, String header, String message, Exception exception) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(message);

        // Create expandable details section
        StringBuffer sb = new StringBuffer();
        sb.append(exception.getMessage()).append("\n");
        for (StackTraceElement element : exception.getStackTrace()) {
            sb.append("\t").append(element).append("\n");
        }

        TextArea textArea = new TextArea(sb.toString());
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefRowCount(10);

        VBox expContent = new VBox(10);
        expContent.setPrefWidth(400);
        expContent.getChildren().add(textArea);

        alert.getDialogPane().setExpandableContent(expContent);
        alert.getDialogPane().setExpanded(false);
        alert.showAndWait();

        logger.error("Error dialog shown with exception details: {}", title, exception);
    }

    /**
     * Shows a warning dialog.
     *
     * @param title   the dialog title
     * @param header  the header text
     * @param message the warning message
     */
    public static void showWarning(String title, String header, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
        logger.warn("Warning dialog shown: {} - {}", title, message);
    }

    /**
     * Shows an info dialog.
     *
     * @param title   the dialog title
     * @param header  the header text
     * @param message the info message
     */
    public static void showInfo(String title, String header, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
        logger.info("Info dialog shown: {} - {}", title, message);
    }

    /**
     * Shows a confirmation dialog with Yes/No buttons.
     *
     * @param title   the dialog title
     * @param header  the header text
     * @param message the confirmation message
     * @return true if user clicked OK, false otherwise
     */
    public static boolean showConfirmation(String title, String header, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(message);

        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    /**
     * Shows a confirmation dialog with custom buttons.
     *
     * @param title   the dialog title
     * @param header  the header text
     * @param message the confirmation message
     * @param buttons the button types to show
     * @return the selected button type
     */
    public static ButtonType showConfirmationWithOptions(String title, String header, String message,
            ButtonType... buttons) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.getButtonTypes().setAll(buttons);

        return alert.showAndWait().orElse(ButtonType.CANCEL);
    }

    /**
     * Shows a text input dialog.
     *
     * @param title        the dialog title
     * @param header       the header text
     * @param prompt       the input prompt text
     * @param defaultValue the default value
     * @return the entered text or null if cancelled
     */
    public static String showTextInput(String title, String header, String prompt, String defaultValue) {
        TextInputDialog dialog = new TextInputDialog(defaultValue);
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        dialog.setContentText(prompt);

        return dialog.showAndWait().orElse(null);
    }

    /**
     * Shows a progress dialog that auto-closes.
     *
     * @param title      the dialog title
     * @param message    the progress message
     * @param durationMs the duration in milliseconds before auto-close
     */
    public static void showProgress(String title, String message, long durationMs) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        // Create progress bar
        ProgressBar progressBar = new ProgressBar();
        progressBar.setStyle("-fx-padding: 10;");
        VBox content = new VBox();
        content.setPadding(new Insets(10));
        content.getChildren().add(progressBar);
        alert.getDialogPane().setContent(content);

        // Show and auto-close after duration
        new Thread(() -> {
            try {
                Thread.sleep(durationMs);
                javafx.application.Platform.runLater(alert::close);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();

        alert.show();
    }

    /**
     * Shows a choice dialog.
     *
     * @param title         the dialog title
     * @param header        the header text
     * @param message       the choice message
     * @param choices       the available choices
     * @param defaultChoice the default choice
     * @return the selected choice or null if cancelled
     */
    public static <T> T showChoice(String title, String header, String message,
            java.util.List<T> choices, T defaultChoice) {
        ChoiceDialog<T> dialog = new ChoiceDialog<>(defaultChoice, choices);
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        dialog.setContentText(message);

        return dialog.showAndWait().orElse(null);
    }

    /**
     * Shows a multi-line text input dialog.
     *
     * @param title       the dialog title
     * @param header      the header text
     * @param defaultText the default text
     * @return the entered text or null if cancelled
     */
    public static String showMultilineTextInput(String title, String header, String defaultText) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(header);

        TextArea textArea = new TextArea(defaultText != null ? defaultText : "");
        textArea.setWrapText(true);
        textArea.setPrefRowCount(10);
        textArea.setPrefColumnCount(40);

        VBox content = new VBox();
        content.setPadding(new Insets(10));
        content.getChildren().add(textArea);
        VBox.setVgrow(textArea, Priority.ALWAYS);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                return textArea.getText();
            }
            return null;
        });

        return dialog.showAndWait().orElse(null);
    }
}
