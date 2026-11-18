package com.smartwater.ui;

import com.smartwater.controller.WaterController;
import com.smartwater.export.ExportService;
import com.smartwater.model.HouseholdProfile;
import com.smartwater.model.WaterRecord;
import com.smartwater.storage.FileManager;
import com.smartwater.storage.ProfileManager;
import com.smartwater.storage.TariffManager;
import com.smartwater.utils.AppClock;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.LocalDate;
import java.util.List;

/**
 * Export dialog for exporting water records in various formats.
 */
public class ExportDialog extends Dialog<Boolean> {
    private static final Logger logger = LoggerFactory.getLogger(ExportDialog.class);
    private final ExportService exportService = ExportService.getInstance();
    private final ProfileManager profileManager = ProfileManager.getInstance();
    private final FileManager fileManager = FileManager.getInstance();
    private final TariffManager tariffManager = TariffManager.getInstance();
    private final WaterController controller;

    private ComboBox<String> formatCombo;
    private DatePicker fromDatePicker;
    private DatePicker toDatePicker;
    private CheckBox includeNotesCheckBox;
    private Label statusLabel;

    /**
     * Creates an export dialog.
     *
     * @param owner the parent stage
     */
    public ExportDialog(Stage owner) {
        this(owner, null);
    }

    public ExportDialog(Stage owner, WaterController controller) {
        this.controller = controller;
        setTitle("Export Water Records");
        setHeaderText("Export data in your preferred format");

        initializeContent();
        setupButtons(owner);

        setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                return performExport();
            }
            return false;
        });
    }

    /**
     * Initializes the dialog content.
     */
    private void initializeContent() {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        // Export Format Section
        VBox formatSection = createFormatSection();
        content.getChildren().add(formatSection);

        // Date Range Section
        VBox dateSection = createDateRangeSection();
        content.getChildren().add(dateSection);

        // Options Section
        VBox optionsSection = createOptionsSection();
        content.getChildren().add(optionsSection);

        // Status Label
        statusLabel = new Label("Ready to export");
        statusLabel.setStyle("-fx-text-fill: #3498db; -fx-font-size: 13;");
        content.getChildren().add(statusLabel);

        getDialogPane().setContent(content);
    }

    /**
     * Creates the export format selection section.
     */
    private VBox createFormatSection() {
        VBox section = new VBox(10);
        Label title = new Label("Export Format");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 15;");

        HBox formatBox = new HBox(10);
        Label formatLabel = new Label("Format:");
        formatLabel.setMinWidth(80);

        formatCombo = new ComboBox<>();
        formatCombo.getItems().addAll("CSV", "Excel", "JSON", "PDF");
        formatCombo.setValue("CSV");
        formatCombo.setPrefWidth(200);

        Label descriptionLabel = new Label("Select CSV for spreadsheet-compatible export, Excel for formatted tables");
        descriptionLabel.setStyle("-fx-font-size: 13; -fx-text-fill: #7f8c8d;");
        descriptionLabel.setWrapText(true);

        formatBox.getChildren().addAll(formatLabel, formatCombo);
        section.getChildren().addAll(title, formatBox, descriptionLabel);

        return section;
    }

    /**
     * Creates the date range selection section.
     */
    private VBox createDateRangeSection() {
        VBox section = new VBox(10);
        Label title = new Label("Date Range");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 15;");

        HBox dateBox = new HBox(15);

        // From date
        VBox fromBox = new VBox(5);
        Label fromLabel = new Label("From Date:");
        fromLabel.setStyle("-fx-font-size: 13;");
        fromDatePicker = new DatePicker(AppClock.today().minusMonths(1));
        fromDatePicker.setPrefWidth(150);
        fromBox.getChildren().addAll(fromLabel, fromDatePicker);

        // To date
        VBox toBox = new VBox(5);
        Label toLabel = new Label("To Date:");
        toLabel.setStyle("-fx-font-size: 13;");
        toDatePicker = new DatePicker(AppClock.today());
        toDatePicker.setPrefWidth(150);
        toBox.getChildren().addAll(toLabel, toDatePicker);

        dateBox.getChildren().addAll(fromBox, toBox);
        section.getChildren().addAll(title, dateBox);

        return section;
    }

    /**
     * Creates the options section.
     */
    private VBox createOptionsSection() {
        VBox section = new VBox(10);
        Label title = new Label("Options");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 15;");

        includeNotesCheckBox = new CheckBox("Include Notes in Export");
        includeNotesCheckBox.setSelected(true);

        section.getChildren().addAll(title, includeNotesCheckBox);

        return section;
    }

    /**
     * Sets up the dialog buttons.
     */
    private void setupButtons(Stage owner) {
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button okButton = (Button) getDialogPane().lookupButton(ButtonType.OK);
        okButton.setText("Export");
        okButton.setStyle("-fx-padding: 8 20;");

        Button cancelButton = (Button) getDialogPane().lookupButton(ButtonType.CANCEL);
        cancelButton.setStyle("-fx-padding: 8 20;");
    }

    /**
     * Performs the export operation.
     */
    private boolean performExport() {
        try {
            String format = formatCombo.getValue();
            LocalDate fromDate = fromDatePicker.getValue();
            LocalDate toDate = toDatePicker.getValue();

            if (fromDate.isAfter(toDate)) {
                statusLabel.setText("From date cannot be after To date");
                statusLabel.setStyle("-fx-text-fill: #e74c3c;");
                return false;
            }

            // Get records in date range
            List<WaterRecord> allRecords = controller != null ? controller.getAllRecords() : fileManager.loadAllRecords();
            List<WaterRecord> filteredRecords = allRecords.stream()
                    .filter(r -> !r.getDate().isBefore(fromDate) && !r.getDate().isAfter(toDate))
                    .toList();

            HouseholdProfile profile = controller != null ? controller.getProfile() : profileManager.loadProfile();

            if (filteredRecords.isEmpty()) {
                statusLabel.setText("No records found in selected date range");
                statusLabel.setStyle("-fx-text-fill: #f39c12;");
                return false;
            }

            // Choose save location
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Select Export Location");
            directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));

            File selectedDirectory = directoryChooser.showDialog(getOwner());
            if (selectedDirectory == null) {
                return false;
            }

            String timestamp = AppClock.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            String filename = String.format("water_export_%s.%s", timestamp, getFileExtension(format));
            String filePath = selectedDirectory.getAbsolutePath() + File.separator + filename;

            if ("PDF".equalsIgnoreCase(format)) {
                exportService.exportToPDF(filteredRecords, profile,
                        controller != null ? controller.getActiveTariffPlan() : tariffManager.getActiveTariffPlan(),
                        filePath);
                statusLabel.setText("Export completed: " + filename);
                statusLabel.setStyle("-fx-text-fill: #2ecc71;");
                logger.info("Export completed: {} records to {}", filteredRecords.size(), filePath);
                Toast.showSuccess("Data exported successfully to " + filename);
                return true;
            }

            // Perform export based on format
            switch (format.toUpperCase()) {
                case "CSV":
                    exportService.exportToCSV(filteredRecords, filePath);
                    break;
                case "EXCEL":
                    exportService.exportToExcel(filteredRecords, profile, filePath);
                    break;
                case "JSON":
                    exportService.exportToJSON(filteredRecords, profile, filePath);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown format: " + format);
            }

            statusLabel.setText("Export completed: " + filename);
            statusLabel.setStyle("-fx-text-fill: #2ecc71;");
            logger.info("Export completed: {} records to {}", filteredRecords.size(), filePath);
            Toast.showSuccess("Data exported successfully to " + filename);
            return true;

        } catch (Exception e) {
            logger.error("Export failed: {}", e.getMessage(), e);
            statusLabel.setText("Export failed: " + e.getMessage());
            statusLabel.setStyle("-fx-text-fill: #e74c3c;");
            DialogUtil.showError("Export Error", "Export Failed", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Gets the file extension for the format.
     */
    private String getFileExtension(String format) {
        return switch (format.toUpperCase()) {
            case "EXCEL" -> "xlsx";
            case "JSON" -> "json";
            case "PDF" -> "pdf";
            default -> "csv";
        };
    }
}
