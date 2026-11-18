package com.smartwater.ui;

import com.smartwater.controller.WaterController;
import com.smartwater.backup.BackupService;
import com.smartwater.model.BillEstimate;
import com.smartwater.model.LeakAlert;
import com.smartwater.model.LimitIncreaseRequest;
import com.smartwater.model.User;
import com.smartwater.model.WaterRecord;
import com.smartwater.policy.MunicipalityPolicy;
import com.smartwater.model.HouseholdProfile;
import com.smartwater.analytics.AnalyticsService;
import com.smartwater.ui.tabs.DashboardTab;
import com.smartwater.ui.AnimationUtil;
import com.smartwater.ui.UiMotion;
import com.smartwater.utils.AppClock;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.List;
import java.io.IOException;

/**
 * Premium Playful Dashboard with Left Sidebar, Reactive Binding, and Full Flow.
 */
public class DashboardView {
    private final WaterController controller;
    private final Stage primaryStage;
    private final User currentUser;
    private final Runnable onLogout;

    private int currentTab = 1;
    private StackPane rootStack; // Master root for overlays
    private BorderPane mainLayout;
    private VBox sidebar;
    private StackPane contentStack;
    private VBox[] tabContents;
    private Button addRecordButton;
    private Pane liquidBackdrop;

    // Externalized Tab
    private DashboardTab dashboardTab;

    // Modal Overlay
    private StackPane modalOverlay;

    public DashboardView(WaterController controller, Stage primaryStage) {
        this(controller, primaryStage, null, null);
    }

    public DashboardView(WaterController controller, Stage primaryStage, User currentUser, Runnable onLogout) {
        this.controller = controller;
        this.primaryStage = primaryStage;
        this.currentUser = currentUser;
        this.onLogout = onLogout;
        setupLayout();
        setupAddRecordModal();
        bindData();
    }

    private void setupLayout() {
        rootStack = new StackPane();
        rootStack.getStyleClass().add("app-shell");
        mainLayout = new BorderPane();
        mainLayout.getStyleClass().add("dashboard-shell");

        liquidBackdrop = UiMotion.createLightAuroraBackdrop();
        liquidBackdrop.prefWidthProperty().bind(rootStack.widthProperty());
        liquidBackdrop.prefHeightProperty().bind(rootStack.heightProperty());

        setupSidebar();
        mainLayout.setLeft(sidebar);

        VBox mainArea = new VBox();
        HBox.setHgrow(mainArea, Priority.ALWAYS);

        HBox topHeader = setupTopHeader();
        contentStack = new StackPane();
        contentStack.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(contentStack, Priority.ALWAYS);

        dashboardTab = new DashboardTab(controller);

        tabContents = new VBox[6];
        tabContents[0] = dashboardTab.getView();
        tabContents[1] = createCalendarContent();
        tabContents[2] = createAnalyticsContent();
        tabContents[3] = createReadableInsightsContent();
        tabContents[4] = createRecordsContent();
        tabContents[5] = createRequestContent();

        for (int i = 0; i < 6; i++) {
            if (tabContents[i] != null) {
                tabContents[i].setOpacity(1.0);
                installLiquidGlassTree(tabContents[i]);
            }
        }
        contentStack.getChildren().setAll(tabContents[0]);
        UiMotion.slideUp(contentStack, 600, 20);
        UiMotion.reveal(tabContents[0], 520, 90);

        mainArea.getChildren().addAll(topHeader, contentStack);
        mainLayout.setCenter(mainArea);

        rootStack.getChildren().addAll(liquidBackdrop, mainLayout);
        // Parallax disabled — background stays fixed
        // PauseTransition parallaxDelay = new PauseTransition(Duration.millis(700));
        // parallaxDelay.setOnFinished(e -> UiMotion.installLiquidScene(rootStack, liquidBackdrop, mainLayout, contentStack));
        // parallaxDelay.play();
        installLiquidGlassTree(mainLayout);

        UiMotion.slideUp(mainLayout, 520, 18);
    }

    private void installLiquidGlassTree(Node node) {
        if (liquidBackdrop == null || node == null) {
            return;
        }
        if (node instanceof Region region
                && region.getStyleClass().contains("card")
                && (region.getStyle() == null || region.getStyle().isBlank())) {
            UiMotion.installLiquidGlass(region, liquidBackdrop);
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                installLiquidGlassTree(child);
            }
        }
    }

    private void setupSidebar() {
        sidebar = new VBox(24);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(240);
        sidebar.setPadding(new Insets(32, 0, 32, 0));

        HBox logoBox = new HBox(12);
        logoBox.setAlignment(Pos.CENTER_LEFT);
        logoBox.setPadding(new Insets(0, 24, 0, 24));

        StackPane logoSquare = new StackPane();
        logoSquare.setPrefSize(40, 40);
        logoSquare.setStyle(
                "-fx-background-color: linear-gradient(to right, #06b6d4, #3b82f6); -fx-background-radius: 12;");
        Label logoLetter = new Label("A");
        logoLetter.setStyle("-fx-text-fill: white; -fx-font-size: 20; -fx-font-weight: 800;");
        logoSquare.getChildren().add(logoLetter);
        UiMotion.pulse(logoSquare, 2.8, 1.06);

        Label appName = new Label("AquaTrack");
        appName.getStyleClass().add("text-h3");
        logoBox.getChildren().addAll(logoSquare, appName);

        VBox navMenu = new VBox(8);
        navMenu.setPadding(new Insets(16, 16, 0, 0));

        String[] tabNames = { "Dashboard", "Calendar", "Analytics", "Insights", "Records", "Request" };
        for (int i = 0; i < tabNames.length; i++) {
            final int tabIndex = i + 1;
            Button tabButton = new Button(tabNames[i]);
            tabButton.setMaxWidth(Double.MAX_VALUE);
            tabButton.getStyleClass().add("nav-tab");
            if (tabIndex == 1)
                tabButton.getStyleClass().add("nav-tab-active");

            tabButton.setOnAction(e -> {
                addSquishAnimation(tabButton);
                switchToTab(tabIndex);
            });
            UiMotion.installHoverLift(tabButton, -2, 1.01);
            navMenu.getChildren().add(tabButton);
        }
        sidebar.getChildren().addAll(logoBox, navMenu);
        UiMotion.staggerReveal(navMenu.getChildren(), 420, 55, 14, 0.98);
        UiMotion.slideUp(sidebar, 520, 14);
    }

    private HBox setupTopHeader() {
        HBox topHeader = new HBox(16);
        topHeader.getStyleClass().add("header");
        topHeader.setPrefHeight(80);
        topHeader.setAlignment(Pos.CENTER_LEFT);
        topHeader.setPadding(new Insets(0, 32, 0, 32));

        addRecordButton = new Button("+ Add Record");
        addRecordButton.getStyleClass().addAll("btn", "btn-primary");
        addRecordButton.setOnAction(e -> {
            addSquishAnimation(addRecordButton);
            showAddRecordModal();
        });
        UiMotion.installHoverLift(addRecordButton, -3, 1.025);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        String displayName = currentUser == null ? "Local User" : currentUser.getFullName();
        HBox accountChip = createAccountChip(displayName);
        UiMotion.installHoverLift(accountChip, -2, 1.01);

        Button logoutBtn = new Button("Sign Out");
        logoutBtn.getStyleClass().addAll("btn", "btn-subtle");
        logoutBtn.setOnAction(e -> {
            if (onLogout != null) {
                onLogout.run();
            }
        });
        UiMotion.installHoverLift(logoutBtn, -2, 1.015);

        topHeader.getChildren().addAll(addRecordButton, spacer, accountChip, logoutBtn);
        UiMotion.slideUp(topHeader, 460, 10);
        return topHeader;
    }

    private HBox createAccountChip(String displayName) {
        HBox chip = new HBox(10);
        chip.getStyleClass().add("account-chip");
        chip.setAlignment(Pos.CENTER_LEFT);
        chip.setMinSize(188, 50);
        chip.setPrefSize(188, 50);
        chip.setMaxSize(188, 50);

        StackPane avatarBadge = new StackPane();
        avatarBadge.getStyleClass().add("avatar-badge");
        avatarBadge.setMinSize(34, 34);
        avatarBadge.setPrefSize(34, 34);
        avatarBadge.setMaxSize(34, 34);

        Label initial = new Label(getInitial(displayName));
        initial.getStyleClass().add("avatar-initial");
        avatarBadge.getChildren().add(initial);

        VBox accountText = new VBox(1);
        accountText.setAlignment(Pos.CENTER_LEFT);
        accountText.setMinWidth(120);
        accountText.setPrefWidth(120);
        accountText.setMaxWidth(120);
        Label nameLabel = new Label(displayName);
        nameLabel.getStyleClass().add("account-name");
        nameLabel.setMaxWidth(120);
        nameLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
        Label metaLabel = new Label("Household account");
        metaLabel.getStyleClass().add("account-meta");
        metaLabel.setMaxWidth(120);
        metaLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
        accountText.getChildren().addAll(nameLabel, metaLabel);

        chip.getChildren().addAll(avatarBadge, accountText);
        return chip;
    }

    private String getInitial(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            return "U";
        }
        return displayName.trim().substring(0, 1).toUpperCase();
    }

    // ==================== ADD RECORD MODAL ====================
    private void setupAddRecordModal() {
        modalOverlay = new StackPane();
        modalOverlay.setStyle("-fx-background-color: rgba(15, 23, 42, 0.6);");
        modalOverlay.setVisible(false);

        VBox modalBox = new VBox(24);
        modalBox.getStyleClass().add("card");
        modalBox.setMaxWidth(400);
        modalBox.setMaxHeight(Region.USE_PREF_SIZE);

        Label title = new Label("Add Water Usage");
        title.getStyleClass().add("text-h2");

        VBox form = new VBox(16);

        Label dateLabel = new Label("Date");
        dateLabel.getStyleClass().add("text-label");
        DatePicker datePicker = new DatePicker(AppClock.today());
        datePicker.getStyleClass().add("input-field");
        datePicker.setMaxWidth(Double.MAX_VALUE);
        VBox dateBox = new VBox(8, dateLabel, datePicker);

        Label litersLabel = new Label("Liters Used");
        litersLabel.getStyleClass().add("text-label");
        TextField litersField = new TextField();
        litersField.setPromptText("e.g. 50");
        litersField.getStyleClass().add("input-field");
        VBox litersBox = new VBox(8, litersLabel, litersField);

        Label noteLabel = new Label("Note (Optional)");
        noteLabel.getStyleClass().add("text-label");
        TextField noteField = new TextField();
        noteField.setPromptText("e.g. Extra shower today");
        noteField.getStyleClass().add("input-field");
        VBox noteBox = new VBox(8, noteLabel, noteField);

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: -color-danger; -fx-font-weight: 600;");
        errorLabel.setVisible(false);

        HBox actions = new HBox(16);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().addAll("btn", "btn-outline");
        cancelBtn.setOnAction(e -> hideAddRecordModal());
        UiMotion.installHoverLift(cancelBtn, -2, 1.015);

        Button saveBtn = new Button("Save Record");
        saveBtn.getStyleClass().addAll("btn", "btn-primary");
        UiMotion.installHoverLift(saveBtn, -2, 1.015);
        saveBtn.setOnAction(e -> {
            try {
                double liters = Double.parseDouble(litersField.getText().trim());
                String result = controller.addRecord(datePicker.getValue(), liters, noteField.getText().trim());
                if (result.startsWith("Error:")) {
                    errorLabel.setText(result);
                    errorLabel.setVisible(true);
                    shakeNode(modalBox);
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Validation Error");
                    alert.setHeaderText(null);
                    alert.setContentText(result.replaceFirst("Error:\\s*", ""));
                    alert.showAndWait();
                } else {
                    hideAddRecordModal();
                    litersField.clear();
                    noteField.clear();
                    datePicker.setValue(AppClock.today());
                    errorLabel.setVisible(false);
                    javafx.application.Platform.runLater(() -> Toast.showSuccess("Record saved"));
                }
            } catch (NumberFormatException ex) {
                errorLabel.setText("Please enter a valid number for liters.");
                errorLabel.setVisible(true);
                shakeNode(modalBox);
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Validation Error");
                alert.setHeaderText(null);
                alert.setContentText("Please enter a valid number for liters.");
                alert.showAndWait();
            }
        });

        actions.getChildren().addAll(cancelBtn, saveBtn);
        form.getChildren().addAll(dateBox, litersBox, noteBox, errorLabel);
        modalBox.getChildren().addAll(title, form, actions);

        modalOverlay.getChildren().add(modalBox);
        rootStack.getChildren().add(modalOverlay);
        installLiquidGlassTree(modalBox);
    }

    private void showAddRecordModal() {
        modalOverlay.setOpacity(0);
        modalOverlay.setVisible(true);
        FadeTransition ft = new FadeTransition(Duration.millis(200), modalOverlay);
        ft.setToValue(1.0);
        ft.play();

        Node modalBox = modalOverlay.getChildren().get(0);
        modalBox.setScaleX(0.9);
        modalBox.setScaleY(0.9);
        ScaleTransition st = new ScaleTransition(Duration.millis(300), modalBox);
        st.setToX(1.0);
        st.setToY(1.0);
        st.setInterpolator(AnimationUtil.safeSpline(0.25, 0.1, 0.25, 1.0));
        st.play();
    }

    private void hideAddRecordModal() {
        FadeTransition ft = new FadeTransition(Duration.millis(150), modalOverlay);
        ft.setToValue(0.0);
        ft.setOnFinished(e -> modalOverlay.setVisible(false));
        ft.play();
    }

    private void shakeNode(Node node) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(50), node);
        tt.setByX(10);
        tt.setAutoReverse(true);
        tt.setCycleCount(6);
        tt.play();
    }

    private void switchToTab(int tabIndex) {
        if (currentTab == tabIndex)
            return;
        currentTab = tabIndex;
        updateHeaderActions();

        VBox navMenu = (VBox) sidebar.getChildren().get(1);
        for (int i = 0; i < navMenu.getChildren().size(); i++) {
            Button btn = (Button) navMenu.getChildren().get(i);
            if (i == tabIndex - 1) {
                if (!btn.getStyleClass().contains("nav-tab-active"))
                    btn.getStyleClass().add("nav-tab-active");
            } else {
                btn.getStyleClass().remove("nav-tab-active");
            }
        }

        VBox enteringTab = tabContents[currentTab - 1];
        Node exitingTab = contentStack.getChildren().isEmpty() ? null : contentStack.getChildren().get(0);

        enteringTab.setVisible(true);
        if (exitingTab == null || exitingTab == enteringTab) {
            contentStack.getChildren().setAll(enteringTab);
            animateTabEntry(enteringTab);
        } else {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(150), exitingTab);
            fadeOut.setToValue(0.0);
            fadeOut.setInterpolator(Interpolator.EASE_OUT);

            TranslateTransition slideOut = new TranslateTransition(Duration.millis(150), exitingTab);
            slideOut.setToY(-10);
            slideOut.setInterpolator(Interpolator.EASE_OUT);

            ParallelTransition exit = new ParallelTransition(fadeOut, slideOut);
            exit.setOnFinished(e -> {
                exitingTab.setOpacity(1.0);
                exitingTab.setTranslateY(0);
                contentStack.getChildren().setAll(enteringTab);
                animateTabEntry(enteringTab);
            });
            exit.play();
        }

        if (currentTab == 3) {
            Platform.runLater(() -> refreshAnalyticsCharts(true));
        }
    }

    private void animateTabEntry(Node tab) {
        tab.setOpacity(0.0);
        tab.setTranslateY(24);
        tab.setScaleX(0.985);
        tab.setScaleY(0.985);
        UiMotion.reveal(tab, 460, 20, 24, 0.985);
    }

    private void updateHeaderActions() {
        if (addRecordButton != null) {
            boolean dashboard = currentTab == 1;
            addRecordButton.setVisible(dashboard);
            addRecordButton.setManaged(true);
        }
    }

    private void addSquishAnimation(Node node) {
        ScaleTransition st = new ScaleTransition(Duration.millis(150), node);
        st.setByX(-0.1);
        st.setByY(-0.1);
        st.setAutoReverse(true);
        st.setCycleCount(2);
        st.play();
    }

    private VBox createTabPage() {
        VBox content = new VBox(24);
        content.setPadding(new Insets(32));
        content.setMaxWidth(Double.MAX_VALUE);
        return content;
    }

    // ==================== CALENDAR TAB ====================
    private Label currentMonthLabel;
    private HBox calendarSummaryRow;
    private GridPane calendarGrid;
    private YearMonth currentCalendarMonth = YearMonth.from(AppClock.today());

    private VBox createCalendarContent() {
        VBox content = createTabPage();

        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);
        currentMonthLabel = new Label();
        currentMonthLabel.getStyleClass().add("text-h2");
        Button prev = new Button("<");
        prev.getStyleClass().add("btn-icon");
        prev.setOnAction(e -> {
            addSquishAnimation(prev);
            currentCalendarMonth = currentCalendarMonth.minusMonths(1);
            buildCalendarGrid(currentCalendarMonth);
        });
        UiMotion.installHoverLift(prev, -2, 1.04);
        Button next = new Button(">");
        next.getStyleClass().add("btn-icon");
        next.setOnAction(e -> {
            addSquishAnimation(next);
            currentCalendarMonth = currentCalendarMonth.plusMonths(1);
            buildCalendarGrid(currentCalendarMonth);
        });
        UiMotion.installHoverLift(next, -2, 1.04);
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        header.getChildren().addAll(currentMonthLabel, sp, prev, next);

        calendarSummaryRow = new HBox(24);
        calendarSummaryRow.setAlignment(Pos.CENTER_LEFT);

        calendarGrid = new GridPane();
        calendarGrid.getStyleClass().add("card");
        calendarGrid.setHgap(10);
        calendarGrid.setVgap(10);
        calendarGrid.setMaxWidth(Double.MAX_VALUE);
        buildCalendarGrid(currentCalendarMonth);

        content.getChildren().addAll(header, calendarSummaryRow, calendarGrid);
        UiMotion.staggerReveal(content.getChildren(), 380, 70);
        return content;
    }

    private void buildCalendarGrid(YearMonth yearMonth) {
        calendarGrid.getChildren().clear();
        calendarGrid.getColumnConstraints().clear();
        for (int i = 0; i < 7; i++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(100.0 / 7.0);
            column.setHgrow(Priority.ALWAYS);
            calendarGrid.getColumnConstraints().add(column);
        }
        if (calendarSummaryRow != null) {
            buildCalendarSummary(yearMonth);
        }
        currentMonthLabel.setText(yearMonth.getMonth().name() + " " + yearMonth.getYear());
        String[] daysOfWeek = { "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat" };
        for (int i = 0; i < 7; i++) {
            Label dayLabel = new Label(daysOfWeek[i]);
            dayLabel.getStyleClass().add("text-label");
            calendarGrid.add(dayLabel, i, 0);
        }

        int daysInMonth = yearMonth.lengthOfMonth();
        LocalDate today = AppClock.today();
        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate d = yearMonth.atDay(day);
            VBox cell = new VBox(8);
            double usageRatio = 0;
            cell.setMaxWidth(Double.MAX_VALUE);
            cell.setMinHeight(56);
            cell.setStyle(
                    "-fx-background-color: white; -fx-border-color: -color-border; -fx-border-width: 2; -fx-background-radius: 12; -fx-border-radius: 12; -fx-padding: 6; -fx-min-height: 56;");
            cell.setAlignment(Pos.CENTER);
            Label dayLabel = new Label(String.valueOf(d.getDayOfMonth()));
            dayLabel.getStyleClass().add("text-body");
            dayLabel.setStyle("-fx-font-size: 13; -fx-font-weight: 800;");

            Optional<WaterRecord> record = controller.getUsageForDate(d);
            if (record.isPresent()) {
                double liters = record.get().getLitersUsed();
                usageRatio = controller.getDailyLimit() > 0 ? liters / controller.getDailyLimit() : 0;
                boolean safe = liters <= controller.getDailyLimit();
                Label usageLabel = new Label(String.format("%.1fL", liters));
                usageLabel.setStyle("-fx-font-size: 12; -fx-font-weight: 800; -fx-text-fill: "
                        + (safe ? "-color-success" : "-color-danger") + ";");
                cell.getChildren().addAll(dayLabel, usageLabel);
            } else {
                cell.getChildren().add(dayLabel);
            }
            if (record.isPresent() && usageRatio > 1.0) {
                cell.setStyle(calendarCellStyle("#fff1f2", "-color-danger"));
            } else if (record.isPresent() && usageRatio >= 0.8) {
                cell.setStyle(calendarCellStyle("#fffbeb", "-color-warning"));
            } else if (record.isPresent()) {
                cell.setStyle(calendarCellStyle("#ecfdf5", "-color-success"));
            }
            if (d.equals(today))
                cell.setStyle(
                        "-fx-background-color: #ecfeff; -fx-border-color: -color-primary; -fx-border-width: 2; -fx-background-radius: 12; -fx-border-radius: 12; -fx-padding: 6; -fx-min-height: 56;");
            UiMotion.installHoverLift(cell, -3, 1.03);
            calendarGrid.add(cell, (d.getDayOfWeek().getValue() % 7),
                    1 + (day - 1 + yearMonth.atDay(1).getDayOfWeek().getValue() % 7) / 7);
        }
        UiMotion.staggerReveal(calendarGrid.getChildren(), 260, 8, 8, 0.96);
        installLiquidGlassTree(calendarGrid);
    }

    private String calendarCellStyle(String background, String border) {
        return "-fx-background-color: " + background + "; -fx-border-color: " + border
                + "; -fx-border-width: 2; -fx-background-radius: 12; -fx-border-radius: 12; -fx-padding: 6; -fx-min-height: 56;";
    }

    private void buildCalendarSummary(YearMonth yearMonth) {
        List<WaterRecord> monthRecords = controller.getAllRecords().stream()
                .filter(r -> YearMonth.from(r.getDate()).equals(yearMonth))
                .toList();
        double total = monthRecords.stream().mapToDouble(WaterRecord::getLitersUsed).sum();
        double quota = controller.getDailyLimit() * yearMonth.lengthOfMonth();
        long exceededDays = monthRecords.stream().filter(r -> r.getLitersUsed() > controller.getDailyLimit()).count();
        double avg = monthRecords.isEmpty() ? 0 : total / monthRecords.size();

        VBox totalCard = createMetricCard("MONTH TOTAL", String.format("%.1f L", total),
                quota > 0 ? String.format("%.0f%% quota", (total / quota) * 100) : "no quota");
        VBox avgCard = createMetricCard("DAILY AVERAGE", String.format("%.1f L", avg),
                String.format("limit %.1f L", controller.getDailyLimit()));
        VBox exceededCard = createMetricCard("OVER-LIMIT DAYS", String.valueOf(exceededDays),
                monthRecords.size() + " recorded days");
        calendarSummaryRow.getChildren().setAll(totalCard, avgCard, exceededCard);
        installLiquidGlassTree(calendarSummaryRow);
    }

    // ==================== ANALYTICS TAB ====================
    private AreaChart<String, Number> weeklyChart;
    private BarChart<String, Number> monthlyBarChart;
    private Timeline analyticsLiveTicker;

    private VBox createAnalyticsContent() {
        VBox content = new VBox(16);
        content.setPadding(new Insets(24));
        content.setMaxWidth(Double.MAX_VALUE);
        Label titleLabel = new Label("Usage Analytics");
        titleLabel.getStyleClass().add("text-h2");

        AnalyticsService analyticsService = AnalyticsService.getInstance();
        AnalyticsService.TrendAnalysis trend = analyticsService.analyzeTrend(controller.getAllRecords(), 30);
        AnalyticsService.Prediction prediction = analyticsService.predictNextMonth(controller.getAllRecords());
        AnalyticsService.SavingsPotential savings = analyticsService.calculateSavingsPotential(
                controller.getAllRecords(), controller.getDailyLimit());
        LeakAlert leakAlert = controller.getLeakAlert();
        BillEstimate billEstimate = controller.getCurrentMonthBillEstimate();

        // Key Metrics Row with Status Colors
        HBox metricsRow = new HBox(12);
        metricsRow.setAlignment(Pos.CENTER_LEFT);
        metricsRow.setFillHeight(true);

        // Determine color for trend
        String trendColor = trend.trend.contains("DECREASING") ? "-color-success" : "-color-danger";

        metricsRow.getChildren().addAll(
                createColoredMetricCard("30-DAY TREND", trend.trend,
                        String.format("%.1f%%", trend.getPercentageChange()), trendColor),
                createColoredMetricCard("FORECAST", String.format("%.1f L/day", prediction.predictedValue),
                        String.format("%.0f%% confident", prediction.confidence), "-color-primary"),
                createColoredMetricCard("SAVE POTENTIAL", String.format("%.1f L", savings.savingsAmount),
                        String.format("%.0f%% possible", savings.savingsPercentage), "-color-success"),
                createColoredMetricCard("BILL FORECAST", formatCurrency(billEstimate.getProjectedTotal()),
                        formatCurrency(billEstimate.getTotal()) + " so far", "-color-warning"));

        // Charts Section
        VBox chartSection = new VBox(8);
        chartSection.getStyleClass().add("card");
        chartSection.setPrefHeight(125);
        chartSection.setMaxHeight(125);
        Label weeklyTitle = new Label("7-Day Usage Trend");
        weeklyTitle.setStyle("-fx-font-size: 14; -fx-font-weight: 800;");
        weeklyChart = new AreaChart<>(new CategoryAxis(), new NumberAxis());
        weeklyChart.setLegendVisible(false);
        weeklyChart.setAnimated(false);
        weeklyChart.setCreateSymbols(true);
        weeklyChart.setPrefHeight(100);
        chartSection.getChildren().addAll(weeklyTitle, weeklyChart);

        VBox monthlySection = new VBox(8);
        monthlySection.getStyleClass().add("card");
        monthlySection.setPrefHeight(125);
        monthlySection.setMaxHeight(125);
        Label monthlyTitle = new Label("This Month vs Daily Limit");
        monthlyTitle.setStyle("-fx-font-size: 14; -fx-font-weight: 800;");
        monthlyBarChart = new BarChart<>(new CategoryAxis(), new NumberAxis());
        monthlyBarChart.setLegendVisible(true);
        monthlyBarChart.setAnimated(false);
        monthlyBarChart.setBarGap(8);
        monthlyBarChart.setCategoryGap(18);
        monthlyBarChart.setPrefHeight(100);
        monthlySection.getChildren().addAll(monthlyTitle, monthlyBarChart);

        // Leak Alert Status with Icon
        HBox leakStatusBox = new HBox(8);
        leakStatusBox.setAlignment(Pos.CENTER_LEFT);
        String alertColor = leakAlert.getSeverity() == LeakAlert.Severity.NONE ? "-color-success"
                : (leakAlert.getSeverity() == LeakAlert.Severity.WARNING ? "-color-warning" : "-color-danger");
        Label alertIcon = new Label("●");
        alertIcon.setStyle("-fx-font-size: 15; -fx-text-fill: " + alertColor + ";");
        Label leakMessage = new Label(leakAlert.getMessage());
        leakMessage.setStyle("-fx-font-size: 13; -fx-text-fill: -color-text-secondary;");
        leakStatusBox.getChildren().addAll(alertIcon, leakMessage);

        content.getChildren().addAll(titleLabel, metricsRow, chartSection, monthlySection, leakStatusBox);
        UiMotion.staggerReveal(content.getChildren(), 320, 60);
        UiMotion.installHoverLift(chartSection, -3, 1.006);
        UiMotion.installHoverLift(monthlySection, -3, 1.006);
        return content;
    }

    private VBox createMetricCard(String title, String value, String subtitle) {
        VBox card = new VBox(8);
        card.getStyleClass().add("card");
        card.setMinWidth(180);
        card.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(card, Priority.ALWAYS);
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 12; -fx-font-weight: 900; -fx-text-fill: -color-text-secondary;");
        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 24; -fx-font-weight: 900; -fx-text-fill: -color-primary;");
        valueLabel.setWrapText(true);
        Label subLabel = new Label(subtitle);
        subLabel.setStyle("-fx-font-size: 13; -fx-font-weight: 700; -fx-text-fill: -color-text-secondary;");
        subLabel.setWrapText(true);
        card.getChildren().addAll(titleLabel, valueLabel, subLabel);
        UiMotion.installHoverLift(card);
        return card;
    }

    private void animateAnalyticsChart(Node chart) {
        if (chart == null) {
            return;
        }

        chart.setOpacity(0.72);
        chart.setTranslateY(4);

        FadeTransition fade = new FadeTransition(Duration.millis(220), chart);
        fade.setToValue(1.0);
        fade.setInterpolator(Interpolator.EASE_OUT);

        TranslateTransition slide = new TranslateTransition(Duration.millis(220), chart);
        slide.setToY(0);
        slide.setInterpolator(AnimationUtil.safeSpline(0.25, 0.1, 0.25, 1.0));

        new ParallelTransition(fade, slide).play();
    }

    private void animateWeeklyChartSymbols() {
        if (weeklyChart == null) {
            return;
        }

        runAfterChartLayout(() -> {
            for (XYChart.Series<String, Number> series : weeklyChart.getData()) {
                for (XYChart.Data<String, Number> data : series.getData()) {
                    Node node = data.getNode();
                    if (node == null) {
                        continue;
                    }
                    node.setScaleX(0.35);
                    node.setScaleY(0.35);
                    node.setOpacity(0.0);

                    FadeTransition fade = new FadeTransition(Duration.millis(260), node);
                    fade.setToValue(1.0);

                    ScaleTransition scale = new ScaleTransition(Duration.millis(320), node);
                    scale.setToX(1.0);
                    scale.setToY(1.0);
                    scale.setInterpolator(AnimationUtil.safeSpline(0.25, 0.1, 0.25, 1.0));

                    new ParallelTransition(fade, scale).play();
                }
            }
        });
    }

    private void animateMonthlyBars() {
        if (monthlyBarChart == null) {
            return;
        }

        runAfterChartLayout(() -> {
            for (XYChart.Series<String, Number> series : monthlyBarChart.getData()) {
                for (XYChart.Data<String, Number> data : series.getData()) {
                    Node node = data.getNode();
                    if (node == null) {
                        continue;
                    }
                    node.setScaleY(0.15);
                    node.setOpacity(0.0);

                    FadeTransition fade = new FadeTransition(Duration.millis(300), node);
                    fade.setToValue(1.0);

                    ScaleTransition grow = new ScaleTransition(Duration.millis(360), node);
                    grow.setToY(1.0);
                    grow.setInterpolator(AnimationUtil.safeSpline(0.25, 0.1, 0.25, 1.0));

                    new ParallelTransition(fade, grow).play();
                }
            }
        });
    }

    private void runAfterChartLayout(Runnable animation) {
        Platform.runLater(() -> {
            PauseTransition delay = new PauseTransition(Duration.millis(90));
            delay.setOnFinished(e -> animation.run());
            delay.play();
        });
    }

    private VBox createReadableInsightsContent() {
        VBox content = createTabPage();

        Label titleLabel = new Label("Smart Insights");
        titleLabel.getStyleClass().add("text-h1");

        LeakAlert leakAlert = controller.getLeakAlert();
        BillEstimate billEstimate = controller.getCurrentMonthBillEstimate();
        double monthlyAvg = controller.getMonthlyAverage();
        double dailyLimit = controller.getDailyLimit();
        double quotaPercent = controller.getMonthlyQuotaUsed() * 100;
        double dailyDifference = monthlyAvg - dailyLimit;

        HBox topRow = new HBox(24);
        topRow.setAlignment(Pos.CENTER_LEFT);
        topRow.getChildren().addAll(
                createReadableStatusCard("Leak Status",
                        leakAlert.getSeverity() == LeakAlert.Severity.NONE ? "Normal" : leakAlert.getSeverity().toString(),
                        leakAlert.getMessage(),
                        leakAlert.getSeverity() == LeakAlert.Severity.NONE ? "-color-success" : "-color-danger"),
                createReadableStatusCard("Monthly Quota",
                        String.format("%.0f%%", quotaPercent),
                        quotaPercent > 100 ? "Your household is over quota." : "Your household is within quota.",
                        quotaPercent > 100 ? "-color-danger" : "-color-success"),
                createReadableStatusCard("Bill Forecast",
                        formatCurrency(billEstimate.getProjectedTotal()),
                        formatCurrency(billEstimate.getTotal()) + " so far from "
                                + String.format("%.1f L", billEstimate.getUsageLiters()),
                        "-color-primary"));

        GridPane insightGrid = new GridPane();
        insightGrid.setHgap(24);
        insightGrid.setVgap(24);
        ColumnConstraints left = new ColumnConstraints();
        left.setPercentWidth(50);
        left.setHgrow(Priority.ALWAYS);
        ColumnConstraints right = new ColumnConstraints();
        right.setPercentWidth(50);
        right.setHgrow(Priority.ALWAYS);
        insightGrid.getColumnConstraints().addAll(left, right);

        insightGrid.add(createReadableInsightCard("Usage Summary",
                new String[] {
                        "Average daily use: " + String.format("%.1f L", monthlyAvg),
                        "Municipal daily limit: " + String.format("%.1f L", dailyLimit),
                        dailyDifference > 0
                                ? "You are above the daily limit by " + String.format("%.1f L", dailyDifference)
                                : "You are below the daily limit by " + String.format("%.1f L", Math.abs(dailyDifference))
                }), 0, 0);

        insightGrid.add(createReadableInsightCard("Recommended Actions",
                new String[] {
                        "Check taps and toilets if usage jumps suddenly.",
                        "Use full loads for washing to reduce repeat cycles.",
                        "Keep high-usage activities on fewer planned days.",
                        "Request a higher allowance only when the need is genuine."
                }), 1, 0);

        insightGrid.add(createReadableInsightCard("Municipal Billing",
                new String[] {
                        "Free allowance: " + String.format("%.0f L", billEstimate.getFreeAllowanceLiters()),
                        "Remaining allowance: " + String.format("%.1f L", billEstimate.getRemainingAllowanceLiters()),
                        "Billable usage: " + String.format("%.1f L", billEstimate.getBillableLiters()),
                        "Fixed charge: " + formatCurrency(billEstimate.getFixedCharge()),
                        "Usage charge: " + formatCurrency(billEstimate.getVariableCharge())
                }), 0, 1);

        insightGrid.add(createReadableInsightCard("Today",
                new String[] {
                        controller.getSmartTip(),
                        "Use Dashboard for daily entry.",
                        "Use Household for allowance increase requests."
                }), 1, 1);

        content.getChildren().addAll(titleLabel, topRow, insightGrid);
        UiMotion.staggerReveal(content.getChildren(), 320, 60);
        return content;
    }

    private VBox createReadableStatusCard(String title, String value, String body, String colorVar) {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(card, Priority.ALWAYS);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("text-label");
        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 24; -fx-font-weight: 800; -fx-text-fill: " + colorVar + ";");
        valueLabel.setWrapText(true);
        Label bodyLabel = new Label(body);
        bodyLabel.setStyle("-fx-font-size: 14; -fx-text-fill: -color-text-secondary; -fx-line-spacing: 4;");
        bodyLabel.setWrapText(true);

        card.getChildren().addAll(titleLabel, valueLabel, bodyLabel);
        UiMotion.installHoverLift(card);
        return card;
    }

    private VBox createReadableInsightCard(String title, String[] lines) {
        VBox card = new VBox(14);
        card.getStyleClass().add("card");
        card.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(card, Priority.ALWAYS);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("text-h4");

        VBox body = new VBox(10);
        for (String line : lines) {
            Label label = new Label("- " + line);
            label.setStyle("-fx-font-size: 14; -fx-text-fill: -color-text-secondary; -fx-line-spacing: 4;");
            label.setWrapText(true);
            body.getChildren().add(label);
        }

        card.getChildren().addAll(titleLabel, body);
        UiMotion.installHoverLift(card);
        return card;
    }

    private VBox createCompactMetricCard(String title, String value, String subtitle) {
        VBox card = new VBox(4);
        card.getStyleClass().add("card");
        card.setMinWidth(100);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setPadding(new Insets(12));
        HBox.setHgrow(card, Priority.ALWAYS);
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 12; -fx-font-weight: 700; -fx-text-fill: -color-text-secondary;");
        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 16; -fx-font-weight: 800; -fx-text-fill: -color-primary;");
        valueLabel.setWrapText(true);
        Label subLabel = new Label(subtitle);
        subLabel.setStyle("-fx-font-size: 12; -fx-text-fill: -color-text-secondary;");
        subLabel.setWrapText(true);
        card.getChildren().addAll(titleLabel, valueLabel, subLabel);
        UiMotion.installHoverLift(card);
        return card;
    }

    private VBox createColoredMetricCard(String title, String value, String subtitle, String colorVar) {
        VBox contentBox = new VBox(5);
        contentBox.setAlignment(Pos.CENTER_LEFT);
        contentBox.setMinWidth(100);
        contentBox.setMinHeight(104);
        contentBox.setMaxWidth(Double.MAX_VALUE);
        contentBox.setPadding(new Insets(18, 22, 18, 24));
        contentBox.setStyle("-fx-background-color: rgba(255,255,255,0.84);"
                + "-fx-background-radius: 18;"
                + "-fx-border-color: " + colorVar + " rgba(255,255,255,0.68) rgba(255,255,255,0.68) " + colorVar + ";"
                + "-fx-border-width: 0 1.5 1.5 5;"
                + "-fx-border-radius: 18;"
                + "-fx-effect: dropshadow(three-pass-box, rgba(15,23,42,0.07), 18, 0, 0, 8);");
        HBox.setHgrow(contentBox, Priority.ALWAYS);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 12; -fx-font-weight: 900; -fx-text-fill: -color-text-secondary;");
        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 18; -fx-font-weight: 900; -fx-text-fill: " + colorVar + ";");
        valueLabel.setWrapText(true);
        Label subLabel = new Label(subtitle);
        subLabel.setStyle("-fx-font-size: 12; -fx-font-weight: 700; -fx-text-fill: -color-text-secondary;");
        subLabel.setWrapText(true);

        contentBox.getChildren().addAll(titleLabel, valueLabel, subLabel);

        VBox card = new VBox();
        card.getChildren().add(contentBox);
        HBox.setHgrow(card, Priority.ALWAYS);
        UiMotion.installHoverLift(card);
        return card;
    }

    // ==================== INSIGHTS TAB ====================
    private VBox createInsightsContent() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(24));
        content.setMaxWidth(Double.MAX_VALUE);

        Label titleLabel = new Label("Smart Insights");
        titleLabel.getStyleClass().add("text-h2");

        LeakAlert leakAlert = controller.getLeakAlert();
        BillEstimate billEstimate = controller.getCurrentMonthBillEstimate();
        double monthlyAvg = controller.getMonthlyAverage();
        double dailyLimit = controller.getDailyLimit();
        double monthlyQuota = controller.getMonthlyQuotaUsed() * 100;

        // ===== Quick Status Section =====
        Label statusSectionLabel = new Label("Current Status");
        statusSectionLabel.setStyle("-fx-font-size: 12; -fx-font-weight: 800; -fx-text-fill: -color-text-secondary;");

        HBox statusRow = new HBox(12);
        statusRow.setAlignment(Pos.CENTER_LEFT);

        String leakStatus = leakAlert.getSeverity() == LeakAlert.Severity.NONE ? "Normal"
                : leakAlert.getSeverity().toString();
        String leakColor = leakAlert.getSeverity() == LeakAlert.Severity.NONE ? "-color-success"
                : (leakAlert.getSeverity() == LeakAlert.Severity.WARNING ? "-color-warning" : "-color-danger");

        statusRow.getChildren().addAll(
                createStatusCard("LEAK STATUS", leakStatus, leakAlert.getMessage(), leakColor),
                createStatusCard("MONTHLY QUOTA", String.format("%.0f%%", monthlyQuota),
                        monthlyQuota > 100 ? "Over quota" : "On track",
                        monthlyQuota > 100 ? "-color-danger" : "-color-success"),
                createStatusCard("AVG DAILY USAGE", String.format("%.1f L", monthlyAvg),
                        String.format("Limit: %.1f L", dailyLimit), "-color-primary"));

        VBox statusSection = new VBox(10);
        statusSection.getChildren().addAll(statusSectionLabel, statusRow);

        // ===== Key Insights Section =====
        Label insightsSectionLabel = new Label("Key Insights");
        insightsSectionLabel.setStyle("-fx-font-size: 12; -fx-font-weight: 800; -fx-text-fill: -color-text-secondary;");

        VBox recommendationsCard = createInsightCard(
                "💡 Smart Recommendations",
                new String[] {
                        "• Install low-flow faucets (15-20% reduction)",
                        "• Fix leaky taps within 24 hours (save 30L/day)",
                        "• Optimize shower time (5 min = 60L saved)",
                        "• Use full loads for washing (20% more efficient)"
                });

        VBox usageCard = createInsightCard(
                "📊 Usage Analytics",
                new String[] {
                        "Daily Average: " + String.format("%.1f L", monthlyAvg),
                        "Daily Limit: " + String.format("%.1f L", dailyLimit),
                        "Difference: " + String.format("%.1f L", Math.abs(dailyLimit - monthlyAvg)),
                        (monthlyAvg > dailyLimit ? "⚠️ Exceeding limit" : "✓ Within limit")
                });

        VBox billCard = createInsightCard(
                "💰 Bill Forecast",
                new String[] {
                        "Projected: " + formatCurrency(billEstimate.getProjectedTotal()),
                        "So far: " + formatCurrency(billEstimate.getTotal()),
                        "Usage: " + String.format("%.1f L", billEstimate.getUsageLiters()),
                        "Rate: " + formatCurrency(billEstimate.getRatePerLiter()) + "/L",
                        "Billable: " + String.format("%.1f L", billEstimate.getBillableLiters())
                });

        HBox row1 = new HBox(12);
        row1.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(recommendationsCard, Priority.ALWAYS);
        HBox.setHgrow(usageCard, Priority.ALWAYS);
        HBox.setHgrow(billCard, Priority.ALWAYS);
        row1.getChildren().addAll(recommendationsCard, usageCard, billCard);

        VBox insightsSection = new VBox(10);
        insightsSection.getChildren().addAll(insightsSectionLabel, row1);

        // ===== Actions Section =====
        Label actionsSectionLabel = new Label("Personal Goals");
        actionsSectionLabel.setStyle("-fx-font-size: 12; -fx-font-weight: 800; -fx-text-fill: -color-text-secondary;");

        VBox smartTipCard = createBorderedCard(12, "-color-primary",
                new Label("🎯 Today's Smart Tip"),
                controller.getSmartTip());

        VBox conservationCard = createBorderedCard(12, "-color-success",
                new Label("♻️ Conservation Goal"),
                "You could save " + String.format("%.1f L/day", Math.max(0, dailyLimit - monthlyAvg)) +
                        " by maintaining your average usage.\n" +
                        "That's " + String.format("%.0f L/month", Math.max(0, dailyLimit - monthlyAvg) * 30) + " or "
                                + formatCurrency(Math.max(0, dailyLimit - monthlyAvg) * 30
                                        * billEstimate.getRatePerLiter()));

        HBox tipsRow = new HBox(12);
        HBox.setHgrow(smartTipCard, Priority.ALWAYS);
        HBox.setHgrow(conservationCard, Priority.ALWAYS);
        tipsRow.getChildren().addAll(smartTipCard, conservationCard);

        VBox actionsSection = new VBox(10);
        actionsSection.getChildren().addAll(actionsSectionLabel, tipsRow);

        content.getChildren().addAll(titleLabel, statusSection, insightsSection, actionsSection);
        UiMotion.staggerReveal(content.getChildren(), 320, 60);
        return content;
    }

    private VBox createBorderedCard(int spacing, String colorVar, Label titleLabel, String contentText) {
        HBox borderBox = new HBox();
        borderBox.setAlignment(Pos.TOP_LEFT);

        Pane borderLeft = new Pane();
        borderLeft.setPrefWidth(4);
        borderLeft.setMinHeight(1);
        borderLeft.setStyle("-fx-background-color: " + colorVar + ";");

        VBox contentBox = new VBox(spacing);
        contentBox.getStyleClass().add("card");
        contentBox.setPadding(new Insets(18, 18, 18, 14));
        contentBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(contentBox, Priority.ALWAYS);

        titleLabel.setStyle("-fx-font-size: 15; -fx-font-weight: 800;");

        Label contentLabel = new Label(contentText);
        contentLabel.setStyle("-fx-font-size: 14; -fx-text-fill: -color-text-secondary;");
        contentLabel.setWrapText(true);

        contentBox.getChildren().addAll(titleLabel, contentLabel);
        borderBox.getChildren().addAll(borderLeft, contentBox);

        VBox card = new VBox();
        card.getChildren().add(borderBox);
        UiMotion.installHoverLift(card);
        return card;
    }

    private VBox createStatusCard(String title, String value, String subtitle, String colorVar) {
        HBox borderBox = new HBox();
        borderBox.setAlignment(Pos.TOP_LEFT);

        Pane borderLeft = new Pane();
        borderLeft.setPrefWidth(3);
        borderLeft.setMinHeight(1);
        borderLeft.setStyle("-fx-background-color: " + colorVar + ";");

        VBox contentBox = new VBox(4);
        contentBox.getStyleClass().add("card");
        contentBox.setPadding(new Insets(14, 14, 14, 10));
        contentBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(contentBox, Priority.ALWAYS);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 12; -fx-font-weight: 800; -fx-text-fill: -color-text-secondary;");

        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 20; -fx-font-weight: 800; -fx-text-fill: " + colorVar + ";");
        valueLabel.setWrapText(true);

        Label subLabel = new Label(subtitle);
        subLabel.setStyle("-fx-font-size: 12; -fx-text-fill: -color-text-secondary;");
        subLabel.setWrapText(true);

        contentBox.getChildren().addAll(titleLabel, valueLabel, subLabel);
        borderBox.getChildren().addAll(borderLeft, contentBox);

        VBox card = new VBox();
        card.getChildren().add(borderBox);
        UiMotion.installHoverLift(card);
        return card;
    }

    private VBox createInsightCard(String title, String[] bulletPoints) {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(16));
        card.setMaxWidth(Double.MAX_VALUE);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 15; -fx-font-weight: 800;");

        VBox pointsBox = new VBox(8);
        for (String point : bulletPoints) {
            Label pointLabel = new Label(point);
            pointLabel.setStyle("-fx-font-size: 14; -fx-text-fill: -color-text-secondary;");
            pointLabel.setWrapText(true);
            pointsBox.getChildren().add(pointLabel);
        }

        card.getChildren().addAll(titleLabel, pointsBox);
        UiMotion.installHoverLift(card);
        return card;
    }

    // ==================== RECORDS TAB ====================
    private TableView<WaterRecord> fullRecordsTable;

    private VBox createRecordsContent() {
        VBox content = createTabPage();
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label("Full Records History");
        titleLabel.getStyleClass().add("text-h1");

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        Button exportBtn = new Button("Export Data");
        exportBtn.getStyleClass().addAll("btn", "btn-primary");
        exportBtn.setOnAction(e -> {
            addSquishAnimation(exportBtn);
            new ExportDialog(primaryStage, controller).showAndWait();
        });
        UiMotion.installHoverLift(exportBtn, -2, 1.015);

        header.getChildren().addAll(titleLabel, headerSpacer, exportBtn);

        fullRecordsTable = new TableView<>();
        fullRecordsTable.getStyleClass().add("card");
        VBox.setVgrow(fullRecordsTable, Priority.ALWAYS);

        TableColumn<WaterRecord, String> dateCol = new TableColumn<>("DATE");
        dateCol.setCellValueFactory(d -> Bindings.createStringBinding(
                () -> d.getValue().getDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                d.getValue().dateProperty()));

        TableColumn<WaterRecord, String> litersCol = new TableColumn<>("LITERS USED");
        litersCol.setCellValueFactory(d -> Bindings.createStringBinding(
                () -> String.format("%.1f", d.getValue().getLitersUsed()), d.getValue().litersUsedProperty()));

        TableColumn<WaterRecord, String> noteCol = new TableColumn<>("NOTE");
        noteCol.setCellValueFactory(d -> d.getValue().noteProperty());

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

        fullRecordsTable.getColumns().addAll(dateCol, litersCol, noteCol, statusCol);
        fullRecordsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        content.getChildren().addAll(header, fullRecordsTable);
        UiMotion.staggerReveal(content.getChildren(), 380, 80);
        return content;
    }

    // ==================== REQUEST TAB ====================
    private VBox createRequestContent() {
        VBox content = createTabPage();
        Label titleLabel = new Label("Request Limit Increase");
        titleLabel.getStyleClass().add("text-h1");

        VBox requestSection = new VBox(16);
        requestSection.getStyleClass().add("card");

        Optional<LimitIncreaseRequest> latestRequest = controller.getLatestLimitIncreaseRequest();
        Label requestStatus = new Label(latestRequest
                .map(req -> String.format("Latest request: %.1f L/day - %s", req.getRequestedDailyLimit(),
                        req.getStatus()))
                .orElse("No active request submitted."));
        requestStatus.getStyleClass().add("text-body");
        requestStatus.setWrapText(true);

        TextField requestedLimitField = new TextField();
        requestedLimitField.setPromptText("Requested daily limit in liters");
        requestedLimitField.getStyleClass().add("input-field");

        TextArea reasonArea = new TextArea();
        reasonArea.setPromptText("Reason for increase, e.g. medical need, guests, home business");
        reasonArea.setWrapText(true);
        reasonArea.setPrefRowCount(8);
        reasonArea.getStyleClass().add("input-field");

        Button submitRequestBtn = new Button("Submit Request");
        submitRequestBtn.getStyleClass().addAll("btn", "btn-primary");
        UiMotion.installHoverLift(submitRequestBtn, -2, 1.015);
        submitRequestBtn.setOnAction(e -> {
            addSquishAnimation(submitRequestBtn);
            try {
                double requestedLimit = Double.parseDouble(requestedLimitField.getText().trim());
                String result = controller.requestLimitIncrease(requestedLimit, reasonArea.getText());
                boolean error = result.startsWith("Error:");
                requestStatus.setText(result);
                requestStatus.setStyle("-fx-text-fill: " + (error ? "-color-danger" : "-color-success")
                        + "; -fx-font-weight: 800;");
                if (!error) {
                    requestedLimitField.clear();
                    reasonArea.clear();
                }
            } catch (NumberFormatException ex) {
                requestStatus.setText("Enter a valid requested limit.");
                requestStatus.setStyle("-fx-text-fill: -color-danger; -fx-font-weight: 800;");
            }
        });

        requestSection.getChildren().addAll(
                new Label("Current Status"), requestStatus,
                new Label("Requested Daily Limit (L)"), requestedLimitField,
                new Label("Reason for Increase"), reasonArea,
                submitRequestBtn);

        content.getChildren().addAll(titleLabel, requestSection);
        UiMotion.staggerReveal(content.getChildren(), 420, 80);
        return content;
    }

    private void applyThemeSelection(String selectedTheme) {
        if (primaryStage.getScene() != null) {
            ThemeManager.getInstance().setScene(primaryStage.getScene());
        }
        ThemeManager.getInstance().setTheme(selectedTheme);
        Toast.showInfo("Theme switched to " + selectedTheme.toLowerCase());
    }

    private String formatCurrency(double amount) {
        return "Rs. " + String.format("%.2f", amount);
    }

    private String capitalizeTheme(String theme) {
        if (theme == null || theme.isBlank()) {
            return "Light";
        }
        return Character.toUpperCase(theme.charAt(0)) + theme.substring(1).toLowerCase();
    }

    // ==================== DATA REFRESH ====================
    private void bindData() {
        controller.getRecordsList()
                .addListener((ListChangeListener<WaterRecord>) c -> Platform.runLater(this::refreshData));
        refreshData();
        startAnalyticsLiveTicker();
    }

    private void refreshData() {
        buildCalendarGrid(currentCalendarMonth);
        refreshAnalyticsCharts(currentTab == 3);

        fullRecordsTable.setItems(controller.getRecordsList());

        // Ensure Insights are updated if we reconstruct the tab contents dynamically,
        // but for now, they are statically created on launch. A full reactive model
        // would re-generate insights.
    }

    private void refreshAnalyticsCharts(boolean animate) {
        if (weeklyChart == null) {
            return;
        }
        XYChart.Series<String, Number> s = new XYChart.Series<>();
        for (WaterRecord w : controller.getWeeklyData()) {
            s.getData().add(
                    new XYChart.Data<>(w.getDate().format(DateTimeFormatter.ofPattern("MMM dd")), w.getLitersUsed()));
        }
        weeklyChart.getData().clear();
        weeklyChart.getData().add(s);
        if (animate) {
            animateAnalyticsChart(weeklyChart);
            animateWeeklyChartSymbols();
        }

        if (monthlyBarChart != null) {
            XYChart.Series<String, Number> usageSeries = new XYChart.Series<>();
            usageSeries.setName("Usage");
            XYChart.Series<String, Number> limitSeries = new XYChart.Series<>();
            limitSeries.setName("Daily limit");
            controller.getMonthlyData().stream()
                    .sorted((a, b) -> a.getDate().compareTo(b.getDate()))
                    .forEach(w -> {
                        String day = w.getDate().format(DateTimeFormatter.ofPattern("dd"));
                        usageSeries.getData().add(new XYChart.Data<>(day, w.getLitersUsed()));
                        limitSeries.getData().add(new XYChart.Data<>(day, controller.getDailyLimit()));
                    });
            monthlyBarChart.getData().setAll(usageSeries, limitSeries);
            if (animate) {
                animateAnalyticsChart(monthlyBarChart);
                animateMonthlyBars();
            }
        }
    }

    private void startAnalyticsLiveTicker() {
        analyticsLiveTicker = new Timeline(new KeyFrame(Duration.seconds(15), e -> {
            if (currentTab == 3) {
                refreshAnalyticsCharts(true);
            }
        }));
        analyticsLiveTicker.setCycleCount(Animation.INDEFINITE);
        analyticsLiveTicker.play();
    }

    public StackPane getView() {
        return rootStack;
    }
}
