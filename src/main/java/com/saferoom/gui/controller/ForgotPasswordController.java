package com.saferoom.gui.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import com.saferoom.gui.utils.AlertUtils;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.regex.Pattern;

public class ForgotPasswordController {

    @FXML private VBox rootPane;
    @FXML private TextField emailField;
    @FXML private Button sendCodeButton;
    @FXML private Hyperlink backToLoginLink;

    // Email validation pattern
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );

    // Window drag variables
    private double xOffset = 0;
    private double yOffset = 0;

    @FXML
    public void initialize() {
        sendCodeButton.setOnAction(event -> handleSendCode());
        backToLoginLink.setOnAction(event -> handleBackToLogin());
        
        // Enter key support for email field
        emailField.setOnAction(event -> handleSendCode());
        
        // Real-time email validation
        emailField.textProperty().addListener((observable, oldValue, newValue) -> {
            validateEmailAndUpdateButton();
        });
    }

    private void validateEmailAndUpdateButton() {
        String email = emailField.getText().trim();
        boolean isValidEmail = !email.isEmpty() && EMAIL_PATTERN.matcher(email).matches();
        
        sendCodeButton.setDisable(!isValidEmail);
        
        // Visual feedback for email field
        if (email.isEmpty()) {
            emailField.setStyle(""); // Default style
        } else if (isValidEmail) {
            emailField.setStyle("-fx-border-color: #22d3ee; -fx-border-width: 1px;");
        } else {
            emailField.setStyle("-fx-border-color: #ef4444; -fx-border-width: 1px;");
        }
    }

    private void handleSendCode() {
        String email = emailField.getText().trim();
        
        if (email.isEmpty()) {
            showError("Please enter your email address.");
            return;
        }
        
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            showError("Please enter a valid email address.");
            return;
        }

        // Simulate sending verification code
        System.out.println("Verification code sent to: " + email);
        
        // TODO: Backend integration - send verification code via email
        // For now, simulate success and navigate to verification screen
        
        try {
            navigateToVerifyResetCode(email);
        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to navigate to verification screen.");
        }
    }

    private void navigateToVerifyResetCode(String email) throws IOException {
        Stage currentStage = (Stage) rootPane.getScene().getWindow();
        currentStage.close();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/VerifyResetCodeView.fxml"));
        Parent root = loader.load();
        
        // Pass email to the next controller
        VerifyResetCodeController controller = loader.getController();
        controller.setEmail(email);

        Stage verifyStage = new Stage();
        verifyStage.initStyle(StageStyle.UNDECORATED);
        
        // Setup window dragging
        root.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });
        root.setOnMouseDragged(event -> {
            verifyStage.setX(event.getScreenX() - xOffset);
            verifyStage.setY(event.getScreenY() - yOffset);
        });

        Scene scene = new Scene(root);
        String cssPath = "/styles/styles.css";
        URL cssUrl = getClass().getResource(cssPath);
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }

        verifyStage.setScene(scene);
        verifyStage.setResizable(false);
        verifyStage.show();
    }

    private void handleBackToLogin() {
        System.out.println("Returning to login screen...");
        try {
            Stage currentStage = (Stage) rootPane.getScene().getWindow();
            currentStage.close();

            Stage loginStage = new Stage();
            loginStage.initStyle(StageStyle.TRANSPARENT);
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/view/LoginView.fxml")));

            // Setup window dragging
            root.setOnMousePressed(event -> {
                xOffset = event.getSceneX();
                yOffset = event.getSceneY();
            });
            root.setOnMouseDragged(event -> {
                loginStage.setX(event.getScreenX() - xOffset);
                loginStage.setY(event.getScreenY() - yOffset);
            });

            Scene scene = new Scene(root);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            String cssPath = "/styles/styles.css";
            URL cssUrl = getClass().getResource(cssPath);
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }

            loginStage.setScene(scene);
            loginStage.setResizable(false);
            loginStage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to return to login screen.");
        }
    }

    private void showError(String message) {
        AlertUtils.showError("Error", message);
    }
}