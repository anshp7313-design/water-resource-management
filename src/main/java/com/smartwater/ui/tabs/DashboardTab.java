package com.smartwater.ui.tabs;

import com.smartwater.controller.WaterController;
import com.smartwater.model.WaterRecord;
import com.smartwater.ui.UiMotion;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class DashboardTab {
    private final WaterController controller;
    private final VBox view;

    private Label todayUsageLabel;
    private Label todayUsageSubLabel;
    private Label monthlyAverageLabel;
    private Label currentStreakLabel;
    private StackPane progressTrack;
    private Pane progressFill;
    private Label progressPercentageLabel;
    private TableView<WaterRecord> recordsTable;
    private static final String STAT_VALUE_STYLE = "-fx-font-size: 38; -fx-font-weight: 800; -fx-text-fill: ";

    public DashboardTab(WaterController controller) {
        this.controller = controller;
        this.view = new VBox(24);
        this.view.setPadding(new Insets(32));
        setupUI();
        bindData();
    }

    public VBox getView() {
        return view;
    }

    private void setupUI() {
        Label titleLabel = new Label("Dashboard Overview");
        titleLabel.getStyleClass().add("text-h1");

        HBox statsRow = new HBox(24);
        todayUsageLabel = new Label("-");
        todayUsageSubLabel = new Label("No data");
        monthlyAverageLabel = new Label("-");
        currentStreakLabel = new Label("-");

        VBox todayCard = createStatCard("LITERS TODAY", todayUsageLabel, todayUsageSubLabel, "-color-primary");
        VBox avgCard = createStatCard("AVG THIS MONTH", monthlyAverageLabel, new Label("liters / day"),
                "-color-success");
        VBox streakCard = createStatCard("CURRENT STREAK", currentStreakLabel, new Label("days under limit"),
                "-color-warning");
        HBox.setHgrow(todayCard, Priority.ALWAYS);
        HBox.setHgrow(avgCard, Priority.ALWAYS);
        HBox.setHgrow(streakCard, Priority.ALWAYS);
        statsRow.getChildren().addAll(todayCard, avgCard, streakCard);

        VBox progressSection = createProgressSection();
        VBox tableSection = createRecentRecordsTable();
        VBox.setVgrow(tableSection, Priority.ALWAYS);

        view.getChildren().addAll(titleLabel, statsRow, progressSection, tableSection);
        UiMotion.staggerReveal(view.getChildren(), 420, 80, 20, 0.985);
    }

    private VBox createStatCard(String title, Label valueLabel, Label subLabel, String colorVar) {
        VBox card = new VBox(8);
        card.getStyleClass().add("card");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setMinHeight(128);
        card.setPrefHeight(128);
        card.setMaxHeight(128);
        card.setMaxWidth(Double.MAX_VALUE);
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("text-label");
        valueLabel.setMinHeight(44);
        valueLabel.setStyle(STAT_VALUE_STYLE + colorVar + ";");
        subLabel.getStyleClass().add("text-small");
        subLabel.setMinHeight(22);
        card.getChildren().addAll(titleLabel, valueLabel, subLabel);

        UiMotion.installHoverLift(card, -5, 1.035);
        return card;
    }

    private VBox createProgressSection() {
        VBox section = new VBox(16);
        section.getStyleClass().add("card");
        HBox header = new HBox();
        Label titleLabel = new Label("Today's Usage vs Limit");
        titleLabel.getStyleClass().add("text-h4");
        progressPercentageLabel = new Label("0%");
        progressPercentageLabel.setStyle("-fx-font-size: 20; -fx-font-weight: 800; -fx-text-fill: -color-primary;");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        header.getChildren().addAll(titleLabel, sp, progressPercentageLabel);

        progressTrack = new StackPane();
        progressTrack.setStyle("-fx-background-color: #f1f5f9; -fx-background-radius: 999; -fx-pref-height: 16;");
        progressTrack.setAlignment(Pos.CENTER_LEFT);

        progressFill = new Pane();
        progressFill.setStyle(
                "-fx-background-color: linear-gradient(to right, #06b6d4, #3b82f6); -fx-background-radius: 999; -fx-pref-height: 16;");
        progressTrack.getChildren().add(progressFill);

        progressTrack.widthProperty().addListener((obs, oldVal, newVal) -> updateProgressFill());

        section.getChildren().addAll(header, progressTrack);
        UiMotion.installHoverLift(section, -3, 1.01);
        return section;
    }

    private VBox createRecentRecordsTable() {
        VBox section = new VBox(0);
        section.getStyleClass().add("card");
        Label title = new Label("Recent Records");
        title.getStyleClass().add("text-h4");
        title.setPadding(new Insets(0, 0, 16, 0));

        recordsTable = new TableView<>();
        TableColumn<WaterRecord, String> dateCol = new TableColumn<>("DATE");
        dateCol.setCellValueFactory(d -> Bindings.createStringBinding(
                () -> d.getValue().getDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                d.getValue().dateProperty()));

        TableColumn<WaterRecord, String> litersCol = new TableColumn<>("LITERS USED");
        litersCol.setCellValueFactory(d -> Bindings.createStringBinding(
                () -> String.format("%.1f", d.getValue().getLitersUsed()), d.getValue().litersUsedProperty()));

        TableColumn<WaterRecord, String> statusCol = new TableColumn<>("STATUS");
        statusCol.setCellValueFactory(d -> Bindings.createStringBinding(
                () -> d.getValue().getLitersUsed() > controller.getDailyLimit() ? "Exceeded" : "Safe",
                d.getValue().litersUsedProperty()));
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null)
                    setGraphic(null);
                else {
                    Label badge = new Label(item);
                    if (item.equals("Safe")) {
                        badge.getStyleClass().add("badge-success");
                        badge.setStyle("-fx-text-fill: -color-success;");
                    } else {
                        badge.getStyleClass().add("badge-danger");
                        badge.setStyle("-fx-text-fill: -color-danger;");
                    }
                    setGraphic(badge);
                }
            }
        });

        recordsTable.getColumns().addAll(dateCol, litersCol, statusCol);
        recordsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        recordsTable.setFixedCellSize(48);

        // Reactive binding to table height
        recordsTable.prefHeightProperty()
                .bind(recordsTable.fixedCellSizeProperty().multiply(Bindings.size(recordsTable.getItems())).add(50));

        section.getChildren().addAll(title, recordsTable);
        UiMotion.installHoverLift(section, -3, 1.006);
        return section;
    }

    private void bindData() {
        // Initial load
        refreshUI();

        // Listen to observable list changes
        controller.getRecordsList()
                .addListener((ListChangeListener<WaterRecord>) c -> Platform.runLater(this::refreshUI));

        // Listen to daily limit changes
        controller.getProfile().dailyLimitProperty()
                .addListener((obs, oldVal, newVal) -> Platform.runLater(this::refreshUI));
    }

    private void refreshUI() {
        Optional<WaterRecord> today = controller.getTodayRecord();
        if (today.isPresent()) {
            double l = today.get().getLitersUsed();
            boolean exc = l > controller.getDailyLimit();
            todayUsageLabel.setText(String.format("%.1f", l));
            todayUsageLabel.setStyle(STAT_VALUE_STYLE + (exc ? "-color-danger" : "-color-primary") + ";");
            todayUsageSubLabel.setText(exc ? "limit exceeded" : "under limit");
            todayUsageSubLabel.setStyle("-fx-font-size: 13; -fx-font-weight: 600; -fx-text-fill: "
                    + (exc ? "-color-danger" : "-color-text-secondary") + ";");
        } else {
            todayUsageLabel.setText("0.0");
            todayUsageSubLabel.setText("no usage today");
            todayUsageLabel.setStyle(STAT_VALUE_STYLE + "-color-primary;");
            todayUsageSubLabel.setStyle("-fx-font-size: 13; -fx-font-weight: 600; -fx-text-fill: -color-text-secondary;");
        }

        monthlyAverageLabel.setText(String.format("%.1f", controller.getMonthlyAverage()));
        currentStreakLabel.setText(String.valueOf(controller.getCurrentStreak()));
        updateProgressFill();

        // Update table with max 5 recent items
        java.util.List<WaterRecord> r = controller.getAllRecords().stream()
                .sorted((a, b) -> b.getDate().compareTo(a.getDate())).limit(5).toList();
        recordsTable.getItems().setAll(r);
    }

    private void updateProgressFill() {
        double progress = 0;
        Optional<WaterRecord> today = controller.getTodayRecord();
        if (today.isPresent())
            progress = Math.min(today.get().getLitersUsed() / controller.getDailyLimit(), 1.0);

        progressPercentageLabel.setText(String.format("%.0f%%", progress * 100));
        progressFill.setStyle("-fx-background-color: "
                + (progress > 0.9 ? "-color-danger" : "linear-gradient(to right, #06b6d4, #3b82f6)")
                + "; -fx-background-radius: 999;");

        if (progressTrack.getWidth() > 0) {
            progressFill.setPrefWidth(progressTrack.getWidth() * progress);
        }
    }
}
