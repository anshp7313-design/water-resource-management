package com.smartwater.ui;

import com.smartwater.auth.UserManager;
import com.smartwater.model.User;
import com.smartwater.validation.InputValidator;
import com.smartwater.validation.ValidationException;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import java.util.Optional;
import java.util.function.Consumer;

public class AuthView {
    private final UserManager um = UserManager.getInstance();
    private final Consumer<User> onAuth;
    private final StackPane root;
    private StackPane card;
    private HBox content;
    private VBox formBox;
    private boolean reg, loading;
    private TextField fnField, emField, unField;
    private PasswordField pwField, cfField;
    private Label msgLabel, modeTitle, modeSubtitle, modeLinkLabel;
    private Hyperlink modeToggle;

    public AuthView(Consumer<User> onAuth) {
        this.onAuth = onAuth;
        this.reg = um.getAllActiveUsers().isEmpty();
        this.loading = false;
        this.root = new StackPane();
        build();
    }

    public StackPane getView() {
        return root;
    }

    private void build() {
        root.setMinSize(0, 0);
        root.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        root.getStyleClass().add("app-shell");
        root.setStyle(
                "-fx-background-color:linear-gradient(from 0% 0% to 100% 100%,#eef2f7 0%,#e8eef6 100%);");

        Pane backdrop = UiMotion.createLightAuroraBackdrop();
        backdrop.prefWidthProperty().bind(root.widthProperty());
        backdrop.prefHeightProperty().bind(root.heightProperty());
        backdrop.setMinSize(0, 0);
        backdrop.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        StackPane wrap = new StackPane();
        wrap.setAlignment(Pos.CENTER);
        wrap.setPadding(new Insets(34));
        wrap.prefWidthProperty().bind(root.widthProperty());
        wrap.prefHeightProperty().bind(root.heightProperty());
        wrap.setMinSize(0, 0);
        wrap.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        card = makeCard();
        applyModeLayout();

        wrap.getChildren().add(card);
        root.getChildren().addAll(backdrop, wrap);

        // --- NEW: Motion Physics Integration ---
        // 1. Parallax effect (Backdrop moves slow, Card moves slightly faster in opposite direction)
        UiMotion.applyParallax(root, backdrop, 0.015);
        UiMotion.applyParallax(root, card, -0.010);
    }

    private StackPane makeCard() {
        StackPane card = new StackPane();

        // Refraction Highlight (follows mouse)
        Circle refraction = new Circle(120, Color.web("#ffffff", 0.08));
        refraction.setMouseTransparent(true);
        refraction.setTranslateX(0);
        refraction.setTranslateY(0);

        Rectangle body = new Rectangle();
        body.widthProperty().bind(card.widthProperty());
        body.heightProperty().bind(card.heightProperty());
        body.setArcWidth(32);
        body.setArcHeight(32);
        body.setFill(Color.web("#ffffff", 0.28));
        body.setStroke(Color.web("#ffffff", 0.76));
        body.setStrokeWidth(1.4);

        content = new HBox(28);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(58));

        VBox left = makeLeft();
        left.setPrefWidth(365);
        HBox.setHgrow(left, Priority.ALWAYS);

        VBox right = makeRight();
        right.setPrefWidth(365);
        HBox.setHgrow(right, Priority.ALWAYS);

        content.getChildren().addAll(left, right);
        card.getChildren().addAll(body, refraction, content);

        // 2. Liquid Refraction logic
        UiMotion.applyLiquidRefraction(card, refraction);

        return card;
    }

    private VBox makeLeft() {
        VBox panel = new VBox(16);
        panel.setAlignment(Pos.TOP_LEFT);

        HBox logo = new HBox(12);
        logo.setAlignment(Pos.CENTER_LEFT);

        StackPane badge = new StackPane();
        Rectangle badgeShape = new Rectangle(44, 44);
        badgeShape.setArcWidth(14);
        badgeShape.setArcHeight(14);
        badgeShape.setFill(Color.web("#0ea5e9"));
        Label logoLetter = new Label("A");
        logoLetter.setStyle("-fx-font-size:22;-fx-font-weight:800;-fx-text-fill:white;");
        badge.getChildren().addAll(badgeShape, logoLetter);

        VBox brandText = new VBox(2);
        Label brand = new Label("AquaTrack");
        brand.setStyle("-fx-font-size:22;-fx-font-weight:800;-fx-text-fill:#0f172a;");
        Label subBrand = new Label("Municipal Water Portal");
        subBrand.setStyle("-fx-font-size:13;-fx-font-weight:700;-fx-text-fill:#64748b;");
        brandText.getChildren().addAll(brand, subBrand);
        logo.getChildren().addAll(badge, brandText);

        Label headline = new Label("Track water usage with\nclarity.");
        headline.setStyle("-fx-font-size:30;-fx-font-weight:800;-fx-text-fill:#0f172a;-fx-line-spacing:2;");

        Label description = new Label(
                "A focused dashboard for daily consumption, municipal\nallowance, bill estimates, and service requests.");
        description.setStyle("-fx-font-size:13;-fx-font-weight:600;-fx-text-fill:#475569;-fx-line-spacing:3;");

        HBox pills = new HBox(10);
        pills.setAlignment(Pos.CENTER_LEFT);
        pills.getChildren().addAll(lPill("Quota", "25 L/person"), lPill("Bills", "Estimated"),
                lPill("Requests", "Tracked"));

        HBox status = new HBox(8);
        status.setAlignment(Pos.CENTER_LEFT);
        status.setMinHeight(38);
        status.setStyle(
                "-fx-background-color:rgba(255,255,255,0.24);-fx-background-radius:12;-fx-border-color:rgba(255,255,255,0.70);-fx-border-radius:12;-fx-border-width:1;-fx-padding:8 14;");
        Label active = new Label("ACTIVE");
        active.setStyle("-fx-text-fill:#059669;-fx-font-size:11;-fx-font-weight:900;");
        Label statusText = new Label("Local secure account");
        statusText.setStyle("-fx-font-size:13;-fx-font-weight:700;-fx-text-fill:#475569;");
        status.getChildren().addAll(active, statusText);
        VBox.setMargin(status, new Insets(0));

        panel.getChildren().addAll(logo, headline, description, pills, status);
        return panel;
    }

    private VBox lPill(String title, String value) {
        VBox pill = new VBox(3);
        pill.setAlignment(Pos.CENTER_LEFT);
        pill.setMinSize(86, 46);
        pill.setStyle(
                "-fx-background-color:rgba(255,255,255,0.24);-fx-background-radius:12;-fx-border-color:rgba(255,255,255,0.66);-fx-border-radius:12;-fx-border-width:1;-fx-padding:8 12;");
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size:11;-fx-font-weight:800;-fx-text-fill:#64748b;");
        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size:12;-fx-font-weight:900;-fx-text-fill:#0f172a;");
        pill.getChildren().addAll(titleLabel, valueLabel);
        return pill;
    }

    private VBox makeRight() {
        VBox panel = new VBox(12);
        panel.setAlignment(Pos.TOP_LEFT);

        Label section = new Label("AQUATRACK ACCESS");
        section.setStyle("-fx-font-size:11;-fx-font-weight:900;-fx-text-fill:#0891b2;");
        modeTitle = new Label();
        modeTitle.setStyle("-fx-font-size:31;-fx-font-weight:900;-fx-text-fill:#0f172a;");
        modeSubtitle = new Label();
        modeSubtitle.setStyle("-fx-font-size:13;-fx-font-weight:600;-fx-text-fill:#64748b;");

        formBox = new VBox(12);
        formBox.setAlignment(Pos.TOP_LEFT);
        rebuildForm();

        Label footer = new Label("Passwords are stored as secure hashes on this device.");
        footer.setStyle("-fx-font-size:11;-fx-font-weight:600;-fx-text-fill:#94a3b8;");
        footer.setWrapText(true);

        HBox linkRow = new HBox();
        linkRow.setAlignment(Pos.CENTER);
        VBox.setMargin(linkRow, new Insets(2, 0, 0, 0));
        modeLinkLabel = new Label();
        modeLinkLabel.setStyle("-fx-font-size:12;-fx-font-weight:600;-fx-text-fill:#64748b;");
        modeToggle = new Hyperlink();
        modeToggle.setStyle("-fx-text-fill:#0891b2;-fx-font-size:12;-fx-font-weight:800;-fx-padding:0;");
        modeToggle.setOnAction(e -> {
            reg = !reg;
            updateModeText();
            applyModeLayout();
            animateSwitch();
        });
        linkRow.getChildren().addAll(modeLinkLabel, modeToggle);

        updateModeText();
        panel.getChildren().addAll(section, modeTitle, modeSubtitle, formBox, footer, linkRow);
        return panel;
    }

    private void updateModeText() {
        if (modeTitle == null || modeSubtitle == null || modeLinkLabel == null || modeToggle == null) {
            return;
        }

        modeTitle.setText(reg ? "Create account" : "Welcome back");
        modeSubtitle.setText(reg ? "Set up your household access"
                : "Continue to your usage, quota, billing, and requests.");
        modeLinkLabel.setText(reg ? "Already have an account? " : "New household? ");
        modeToggle.setText(reg ? "Sign in" : "Register access");
    }

    private void applyModeLayout() {
        if (card == null || content == null) {
            return;
        }

        if (reg) {
            card.setMinSize(900, 620);
            card.setPrefSize(900, 620);
            card.setMaxSize(900, 620);
            content.setPadding(new Insets(30, 58, 30, 58));
        } else {
            card.setMinSize(860, 480);
            card.setPrefSize(860, 480);
            card.setMaxSize(860, 480);
            content.setPadding(new Insets(58));
        }
    }

    private void rebuildForm() {
        formBox.getChildren().clear();
        formBox.setSpacing(reg ? 9 : 12);
        fnField = reg ? liField("Full name") : null;
        emField = reg ? liField("Email address") : null;
        unField = liField("Username");
        pwField = liPass("Password");
        cfField = reg ? liPass("Confirm password") : null;
        msgLabel = new Label();
        msgLabel.setWrapText(true);
        msgLabel.setVisible(false);
        msgLabel.setManaged(false);

        Button button = new Button(reg ? "Create Account" : "Sign In");
        button.setStyle(
                "-fx-background-color:linear-gradient(to right,#06b6d4,#3b82f6);-fx-text-fill:white;-fx-font-weight:900;-fx-font-size:"
                        + (reg ? "14" : "15") + ";-fx-background-radius:14;-fx-padding:"
                        + (reg ? "12 20" : "14 20")
                        + ";-fx-cursor:hand;-fx-effect:dropshadow(three-pass-box,rgba(6,182,212,0.35),14,0,0,6);");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(e -> handleSubmit(button));

        if (reg) {
            formBox.getChildren().addAll(fg("FULL NAME", fnField), fg("EMAIL", emField),
                    fg("USERNAME", unField), fg("PASSWORD", pwField), fg("CONFIRM PASSWORD", cfField), msgLabel,
                    button);
            fnField.setOnAction(e -> emField.requestFocus());
            emField.setOnAction(e -> unField.requestFocus());
            unField.setOnAction(e -> pwField.requestFocus());
            pwField.setOnAction(e -> cfField.requestFocus());
            cfField.setOnAction(e -> button.fire());
        } else {
            formBox.getChildren().addAll(fg("USERNAME", unField), fg("PASSWORD", pwField), msgLabel, button);
            unField.setOnAction(e -> pwField.requestFocus());
            pwField.setOnAction(e -> button.fire());
        }
    }

    private VBox fg(String label, Control field) {
        VBox group = new VBox(6);
        Label title = new Label(label);
        title.setStyle("-fx-font-size:11;-fx-font-weight:900;-fx-text-fill:#0891b2;");
        group.getChildren().addAll(title, field);
        return group;
    }

    private TextField liField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setStyle(
                "-fx-background-color:rgba(255,255,255,0.22);-fx-background-radius:12;-fx-border-color:rgba(14,165,233,0.72);-fx-border-radius:12;-fx-border-width:1.5;-fx-padding:"
                        + (reg ? "10 16" : "12 16") + ";-fx-font-size:" + (reg ? "14" : "15")
                        + ";-fx-font-weight:600;-fx-text-fill:#0f172a;-fx-prompt-text-fill:#94a3b8;");
        field.setMaxWidth(Double.MAX_VALUE);
        return field;
    }

    private PasswordField liPass(String prompt) {
        PasswordField field = new PasswordField();
        field.setPromptText(prompt);
        field.setStyle(
                "-fx-background-color:rgba(255,255,255,0.22);-fx-background-radius:12;-fx-border-color:rgba(255,255,255,0.82);-fx-border-radius:12;-fx-border-width:1.5;-fx-padding:"
                        + (reg ? "10 16" : "12 16") + ";-fx-font-size:" + (reg ? "14" : "15")
                        + ";-fx-font-weight:600;-fx-text-fill:#0f172a;-fx-prompt-text-fill:#94a3b8;");
        field.setMaxWidth(Double.MAX_VALUE);
        return field;
    }

    private void animateSwitch() {
        FadeTransition out = new FadeTransition(Duration.millis(150), formBox);
        out.setFromValue(1.0);
        out.setToValue(0.0);
        out.setOnFinished(e -> {
            rebuildForm();
            updateModeText();
            formBox.setOpacity(0);
            formBox.setTranslateY(24); // Start lower for spring bounce
            
            FadeTransition in = new FadeTransition(Duration.millis(300), formBox);
            in.setFromValue(0.0);
            in.setToValue(1.0);
            in.play();
            
            // 3. Spring Physics bounce (stiffness 0.1, damping 0.85)
            UiMotion.applySpringPhysics(formBox, 0, 0.12, 0.82);
        });
        out.play();
    }

    private void handleSubmit(Button button) {
        if (loading) {
            return;
        }
        if (reg) {
            handleRegister(button);
        } else {
            handleLogin(button);
        }
    }

    private void handleLogin(Button button) {
        String username = unField.getText().trim();
        String password = pwField.getText();
        if (username.isEmpty()) {
            showError("Username is required");
            return;
        }
        if (password.isEmpty()) {
            showError("Password is required");
            return;
        }
        setLoad(button, true);
        new Thread(() -> {
            Optional<User> result = um.authenticate(username, password);
            Platform.runLater(() -> {
                setLoad(button, false);
                if (result.isPresent()) {
                    onAuth.accept(result.get());
                } else {
                    showError("Invalid username or password");
                }
            });
        }).start();
    }

    private void handleRegister(Button button) {
        String fullName = fnField.getText().trim();
        String email = emField.getText().trim();
        String username = unField.getText().trim();
        String password = pwField.getText();
        String confirm = cfField.getText();

        var fullNameResult = InputValidator.validateFullName(fullName);
        if (!fullNameResult.isValid()) {
            showError(fullNameResult.getMessage());
            return;
        }
        var emailResult = InputValidator.validateEmail(email);
        if (!emailResult.isValid()) {
            showError(emailResult.getMessage());
            return;
        }
        var usernameResult = InputValidator.validateUsername(username);
        if (!usernameResult.isValid()) {
            showError(usernameResult.getMessage());
            return;
        }
        var passwordResult = InputValidator.validatePassword(password);
        if (!passwordResult.isValid()) {
            showError(passwordResult.getMessage());
            return;
        }
        var confirmResult = InputValidator.validatePasswordConfirmation(password, confirm);
        if (!confirmResult.isValid()) {
            showError(confirmResult.getMessage());
            return;
        }

        setLoad(button, true);
        new Thread(() -> {
            try {
                User newUser = um.createUser(username, email, password, fullName);
                Platform.runLater(() -> onAuth.accept(newUser));
            } catch (ValidationException | RuntimeException ex) {
                Platform.runLater(() -> {
                    setLoad(button, false);
                    showError(ex.getMessage());
                });
            }
        }).start();
    }

    private void setLoad(Button button, boolean value) {
        loading = value;
        button.setDisable(value);
        button.setText(value ? "Please wait..." : (reg ? "Create Account" : "Sign In"));
    }

    private void showError(String message) {
        msgLabel.setText(message);
        msgLabel.setStyle("-fx-text-fill:#f43f5e;-fx-font-size:12;-fx-font-weight:800;");
        msgLabel.setVisible(true);
        msgLabel.setManaged(true);
    }
}
