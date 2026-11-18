package com.smartwater.ui;

import com.smartwater.MainApp;
import com.smartwater.controller.WaterController;
import com.smartwater.model.HouseholdProfile;
import com.smartwater.policy.MunicipalityPolicy;
import com.smartwater.ui.UiMotion;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

/**
 * Setup wizard for first-time application launch.
 * Now acts as an embedded view returning a VBox to the MainApp.
 */
public class SetupWizard {
    private final WaterController controller;
    private final MainApp mainApp;

    private VBox rootPane;
    private VBox wizardContainer;

    // UI Components
    private TextField householdNameField;
    private ComboBox<Integer> memberCountCombo;
    private TextField cityField;
    private Label sliderValueLabel;
    private VBox step1Pane;
    private HBox navigationBar;

    public SetupWizard(WaterController controller, MainApp mainApp) {
        this.controller = controller;
        this.mainApp = mainApp;
        createWizardContent();
    }

    public VBox getView() {
        return rootPane;
    }

    private void createWizardContent() {
        rootPane = new VBox(24);
        rootPane.setAlignment(Pos.TOP_CENTER);
        rootPane.getStyleClass().add("app-shell");

        // Wrap everything in a card-like center container to look like a premium SaaS
        // modal but embedded
        wizardContainer = new VBox(24);
        wizardContainer.getStyleClass().add("card");
        wizardContainer.setMaxWidth(720);
        wizardContainer.setPadding(new Insets(40));
        StackPane.setAlignment(wizardContainer, Pos.TOP_CENTER);
        StackPane.setMargin(wizardContainer, new Insets(60, 0, 0, 0));

        HBox titleSection = createTitleSection();

        step1Pane = createStep1();
        VBox step1 = step1Pane;

        navigationBar = createNavigationButtons();
        HBox navigation = navigationBar;
        VBox.setMargin(navigation, new Insets(16, 0, 0, 0));

        wizardContainer.getChildren().addAll(titleSection, step1, navigation);

        StackPane shell = new StackPane();
        shell.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        VBox.setVgrow(shell, Priority.ALWAYS);
        shell.getChildren().addAll(UiMotion.createAuroraBackdrop(), wizardContainer);
        UiMotion.slideUp(wizardContainer, 600, 22);
        UiMotion.staggerReveal(wizardContainer.getChildren(), 360, 55);
        rootPane.getChildren().add(shell);
    }

    private HBox createTitleSection() {
        HBox titleBox = new HBox(16);
        titleBox.setAlignment(Pos.CENTER);

        StackPane logoSquare = new StackPane();
        logoSquare.setPrefSize(48, 48);
        logoSquare.setStyle("-fx-background-color: -color-primary; -fx-background-radius: 12;");

        Label logoLetter = new Label("A");
        logoLetter.setTextFill(Color.WHITE);
        logoLetter.setStyle("-fx-font-size: 24; -fx-font-weight: bold;");
        logoSquare.getChildren().add(logoLetter);

        VBox titleText = new VBox(4);
        titleText.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label("Welcome to AquaTrack");
        titleLabel.getStyleClass().add("text-h2");

        Label subtitleLabel = new Label("Let's set up your household profile");
        subtitleLabel.getStyleClass().add("text-body");

        titleText.getChildren().addAll(titleLabel, subtitleLabel);
        titleBox.getChildren().addAll(logoSquare, titleText);

        return titleBox;
    }

    private VBox createStep1() {
        VBox step = new VBox(16);

        Label stepTitle = new Label("Household Setup");
        stepTitle.getStyleClass().add("text-h3");
        step.getChildren().add(stepTitle);

        Label nameLabel = new Label("Household Name");
        nameLabel.getStyleClass().add("text-label");
        householdNameField = new TextField();
        householdNameField.setPromptText("e.g. The Sharma Family");
        householdNameField.getStyleClass().add("input-field");
        VBox nameBox = new VBox(8, nameLabel, householdNameField);

        Label memberLabel = new Label("Number of Members");
        memberLabel.getStyleClass().add("text-label");
        memberCountCombo = new ComboBox<>(FXCollections.observableArrayList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        memberCountCombo.setValue(4);
        memberCountCombo.getStyleClass().add("combo-box");
        memberCountCombo.setMaxWidth(Double.MAX_VALUE);
        VBox memberBox = new VBox(8, memberLabel, memberCountCombo);

        Label cityLabel = new Label("City");
        cityLabel.getStyleClass().add("text-label");
        cityField = new TextField();
        cityField.setPromptText("e.g. Ahmedabad");
        cityField.getStyleClass().add("input-field");
        VBox cityBox = new VBox(8, cityLabel, cityField);

        Label description = new Label(
                "AquaTrack will apply the municipal daily limit automatically from your city and household size.");
        description.getStyleClass().add("text-body");
        description.setWrapText(true);

        sliderValueLabel = new Label(MunicipalityPolicy.describeDailyLimit(memberCountCombo.getValue(),
                cityField.getText()));
        sliderValueLabel.getStyleClass().add("text-h3");
        sliderValueLabel.setStyle("-fx-text-fill: -color-primary;");

        memberCountCombo.valueProperty().addListener((obs, oldVal, newVal) -> refreshMunicipalLimitLabel());
        cityField.textProperty().addListener((obs, oldVal, newVal) -> refreshMunicipalLimitLabel());
        VBox policyBox = new VBox(8, new Label("Municipal Daily Limit"), sliderValueLabel, description);

        step.getChildren().addAll(nameBox, memberBox, cityBox, policyBox);
        return step;
    }

    private void refreshMunicipalLimitLabel() {
        if (sliderValueLabel != null) {
            sliderValueLabel.setText(MunicipalityPolicy.describeDailyLimit(memberCountCombo.getValue(),
                    cityField.getText()));
        }
    }

    private HBox createNavigationButtons() {
        HBox navBox = new HBox(16);
        navBox.setAlignment(Pos.CENTER_RIGHT);

        Button nextButton = new Button("Start Tracking");
        nextButton.getStyleClass().addAll("btn", "btn-primary");
        nextButton.setOnAction(e -> goNext());

        navBox.getChildren().add(nextButton);
        return navBox;
    }

    private void goNext() {
        completeSetup();
    }

    private void updateSummaryPreview() {
        String householdName = householdNameField.getText().trim();
        if (householdName.isEmpty())
            householdName = "My Home";

        double dailyLimit = MunicipalityPolicy.calculateDailyLimit(memberCountCombo.getValue(), cityField.getText());

        VBox summaryCard = new VBox(16);
        summaryCard.getChildren().clear();
        Label summaryTitle = new Label("You're all set! 🎉");
        summaryTitle.getStyleClass().add("text-h2");
        summaryTitle.setStyle("-fx-text-fill: -color-primary;");

        Label detailsLabel = new Label(String.format("Household: %s\nLimit: %.1f L/day", householdName, dailyLimit));
        detailsLabel.getStyleClass().add("text-body");
        detailsLabel.setAlignment(Pos.CENTER);
        detailsLabel.setStyle("-fx-text-alignment: center;");

        summaryCard.getChildren().addAll(summaryTitle, detailsLabel);
    }

    private void completeSetup() {
        String householdName = householdNameField.getText().trim();
        if (householdName.isEmpty())
            householdName = "My Home";

        int memberCount = memberCountCombo.getValue();
        String city = cityField.getText().trim();

        double dailyLimit = MunicipalityPolicy.calculateDailyLimit(memberCount, city);

        HouseholdProfile profile = new HouseholdProfile(householdName, memberCount, dailyLimit,
                city, true, true, false);

        String result = controller.saveProfile(profile);

        if (result.contains("Error:")) {
            DialogUtil.showError("Setup Error", "Could not save household profile", result);
        } else {
            // Signal main application to crossfade into Dashboard
            mainApp.transitionToDashboard();
        }
    }
}
