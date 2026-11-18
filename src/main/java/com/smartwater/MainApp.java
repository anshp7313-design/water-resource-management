package com.smartwater;

import com.smartwater.controller.WaterController;
import com.smartwater.model.User;
import com.smartwater.ui.AuthView;
import com.smartwater.ui.DashboardView;
import com.smartwater.ui.SetupWizard;
import com.smartwater.ui.ThemeManager;
import com.smartwater.utils.LimitChecker;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Main entry point for SmartWaterMonitor application.
 * Uses a single-window architecture for a seamless SaaS feel.
 */
public class MainApp extends Application {
    private WaterController waterController;
    private LimitChecker limitChecker;
    private StackPane rootContainer;
    private Stage primaryStage;
    private User currentUser;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        rootContainer = new StackPane();
        rootContainer.getStyleClass().add("root");

        Scene scene = new Scene(rootContainer, 900, 580);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        primaryStage.setTitle("AquaTrack - Smart Water Monitor");
        primaryStage.setWidth(900);
        primaryStage.setHeight(580);
        primaryStage.setResizable(true);
        primaryStage.setScene(scene);
        ThemeManager.getInstance().setScene(scene);
        showAuthView();
        primaryStage.show();

        System.out.println("Smart Water Monitor application started.");
    }

    public void showAuthView() {
        currentUser = null;
        if (limitChecker != null) {
            limitChecker.stop();
            limitChecker = null;
        }
        if (waterController != null) {
            waterController.shutdown();
            waterController = null;
        }
        primaryStage.setTitle("AquaTrack - Smart Water Monitor");
        AuthView authView = new AuthView(this::onAuthenticated);
        rootContainer.getChildren().setAll(authView.getView());
    }

    private void onAuthenticated(User user) {
        this.currentUser = user;
        if (limitChecker != null) {
            limitChecker.stop();
        }
        if (waterController != null) {
            waterController.shutdown();
        }
        waterController = new WaterController(user);
        limitChecker = new LimitChecker(waterController, alertMessage -> {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Water Limit Alert");
                alert.setHeaderText("Daily Water Limit Exceeded");
                alert.setContentText(alertMessage);
                alert.show();
            });
        });
        limitChecker.start();
        primaryStage.setTitle("AquaTrack - " + user.getFullName());
        if (waterController.isFirstLaunch()) {
            showSetupWizard();
        } else {
            transitionToDashboard();
        }
    }

    public void showSetupWizard() {
        SetupWizard setupWizard = new SetupWizard(waterController, this);
        rootContainer.getChildren().setAll(setupWizard.getView());
    }

    public void transitionToDashboard() {
        DashboardView dashboardView = new DashboardView(waterController, primaryStage, currentUser, this::showAuthView);
        javafx.scene.layout.StackPane dashboardRoot = dashboardView.getView();
        dashboardRoot.setOpacity(0);

        rootContainer.getChildren().add(dashboardRoot);

        FadeTransition ftIn = new FadeTransition(Duration.millis(500), dashboardRoot);
        ftIn.setFromValue(0.0);
        ftIn.setToValue(1.0);

        if (rootContainer.getChildren().size() > 1) {
            javafx.scene.Node oldView = rootContainer.getChildren().get(0);
            FadeTransition ftOut = new FadeTransition(Duration.millis(500), oldView);
            ftOut.setFromValue(1.0);
            ftOut.setToValue(0.0);
            ftOut.setOnFinished(e -> rootContainer.getChildren().remove(oldView));
            ftOut.play();
        }

        ftIn.play();
    }

    public void showDashboard() {
        DashboardView dashboardView = new DashboardView(waterController, primaryStage, currentUser, this::showAuthView);
        rootContainer.getChildren().setAll(dashboardView.getView());
    }

    @Override
    public void stop() {
        if (limitChecker != null) {
            limitChecker.stop();
        }
        if (waterController != null) {
            waterController.shutdown();
        }
        System.out.println("Smart Water Monitor application stopped.");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
