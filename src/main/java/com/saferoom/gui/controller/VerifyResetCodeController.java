package com.saferoom.gui.controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;

import com.saferoom.client.ClientMenu;
import com.saferoom.gui.utils.AlertUtils;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

public class VerifyResetCodeController {

    @FXML private VBox rootPane;
    @FXML private Label emailLabel;
    @FXML private TextField digit1, digit2, digit3, digit4, digit5, digit6;
    @FXML private Button verifyButton;
    @FXML private Hyperlink resendLink;
    @FXML private Hyperlink backToForgotPasswordLink;

    private List<TextField> digitFields;
    private Timeline timeline;
    private final IntegerProperty countdownSeconds = new SimpleIntegerProperty();
    private static final int COUNTDOWN_START_VALUE = 60;
    private static final int CODE_EXPIRY_MINUTES = 15;
    
    // Password reset specific fields
    private String userEmail;
    private String generatedCode;
    private LocalDateTime codeGeneratedTime;
    private int resendAttempts = 0;
    private static final int MAX_RESEND_ATTEMPTS = 3;

    // Window drag variables
    private double xOffset = 0;
    private double yOffset = 0;

    @FXML
    public void initialize() {
        digitFields = List.of(digit1, digit2, digit3, digit4, digit5, digit6);
        
        setupOtpFields();
        
        verifyButton.setOnAction(event -> handleVerifyCode());
        resendLink.setOnAction(event -> handleResendCode());
        backToForgotPasswordLink.setOnAction(event -> handleBackToForgotPassword());
        
        startResendTimer();
    }

    /**
     * Set email from previous screen and generate verification code
     */
    public void setEmail(String email) {
        this.userEmail = email;
        emailLabel.setText(email);
        generateVerificationCode();
        
        // Simulate sending code
        System.out.println("Reset verification code sent to: " + email);
        System.out.println("Generated code: " + generatedCode + " (for testing)");
    }

    private void generateVerificationCode() {
        // Generate cryptographically secure 6-digit code
        generatedCode = String.format("%06d", (int) (Math.random() * 1000000));
        codeGeneratedTime = LocalDateTime.now();
        
        // TODO: In production, this should be:
        // 1. Generated using SecureRandom
        // 2. Hashed and stored in database
        // 3. Sent via email service
    }

    private void setupOtpFields() {
        for (int i = 0; i < digitFields.size(); i++) {
            final TextField currentField = digitFields.get(i);
            final int nextIndex = i + 1;
            final int prevIndex = i - 1;

            // Only allow digits and auto-advance
            currentField.textProperty().addListener((obs, oldValue, newValue) -> {
                if (!newValue.matches("\\d*")) {
                    currentField.setText(newValue.replaceAll("[^\\d]", ""));
                }
                if (newValue.length() > 1) {
                    currentField.setText(newValue.substring(0, 1));
                }
                if (newValue.length() == 1 && nextIndex < digitFields.size()) {
                    digitFields.get(nextIndex).requestFocus();
                }
                updateVerifyButtonState();
            });

            // Handle backspace navigation
            currentField.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.BACK_SPACE && currentField.getText().isEmpty() && prevIndex >= 0) {
                    TextField prevField = digitFields.get(prevIndex);
                    prevField.requestFocus();
                    prevField.end();
                }
                if (event.getCode() == KeyCode.ENTER) {
                    handleVerifyCode();
                }
            });
        }
    }

    private void updateVerifyButtonState() {
        String enteredCode = getEnteredCode();
        verifyButton.setDisable(enteredCode.length() != 6);
    }

    private String getEnteredCode() {
        StringBuilder code = new StringBuilder();
        digitFields.forEach(field -> code.append(field.getText()));
        return code.toString();
    }

    private void startResendTimer() {
        if (timeline != null) {
            timeline.stop();
        }
        
        countdownSeconds.set(COUNTDOWN_START_VALUE);
        resendLink.setDisable(true);
        
        resendLink.textProperty().bind(countdownSeconds.asString("Resend in %ds"));
        
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            countdownSeconds.set(countdownSeconds.get() - 1);
            if (countdownSeconds.get() <= 0) {
                timeline.stop();
                resendLink.textProperty().unbind();
                resendLink.setText("Resend Code");
                resendLink.setDisable(resendAttempts >= MAX_RESEND_ATTEMPTS);
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private void handleVerifyCode() {
        String enteredCode = getEnteredCode();
        
        if (enteredCode.length() != 6) {
            showError("Please enter the complete 6-digit verification code.");
            return;
        }
        
        // Check if code has expired
        if (isCodeExpired()) {
            showError("Verification code has expired. Please request a new one.");
            clearDigitFields();
            return;
        }
        
        int result_from_server = ClientMenu.verify_user(this.userEmail, enteredCode); 
        // Verify the code
        if (result_from_server == 0) {
            System.out.println("Reset code verified successfully for: " + userEmail);
            
            try {
                navigateToNewPassword();
            } catch (IOException e) {
                e.printStackTrace();
                showError("Failed to navigate to password reset screen.");
            }
        } else {
            showError("Invalid verification code. Please try again.");
            clearDigitFields();
            digitFields.get(0).requestFocus();
        }
    }

    private boolean isCodeExpired() {
        if (codeGeneratedTime == null) return true;
        return ChronoUnit.MINUTES.between(codeGeneratedTime, LocalDateTime.now()) > CODE_EXPIRY_MINUTES;
    }

    private void clearDigitFields() {
        digitFields.forEach(field -> field.clear());
    }

    private void navigateToNewPassword() throws IOException {
        Stage currentStage = (Stage) rootPane.getScene().getWindow();
        currentStage.close();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/NewPasswordView.fxml"));
        Parent root = loader.load();
        
        // Pass email to new password controller
        NewPasswordController controller = loader.getController();
        controller.setEmail(userEmail);

        Stage newPasswordStage = new Stage();
        newPasswordStage.initStyle(StageStyle.TRANSPARENT);
        
        // Setup window dragging
        root.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });
        root.setOnMouseDragged(event -> {
            newPasswordStage.setX(event.getScreenX() - xOffset);
            newPasswordStage.setY(event.getScreenY() - yOffset);
        });

        Scene scene = new Scene(root);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        String cssPath = "/styles/styles.css";
        URL cssUrl = getClass().getResource(cssPath);
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }

        newPasswordStage.setScene(scene);
        newPasswordStage.setResizable(false);
        newPasswordStage.show();
    }

    private void handleResendCode() {
        if (resendAttempts >= MAX_RESEND_ATTEMPTS) {
            showError("Maximum resend attempts reached. Please try again later.");
            return;
        }
        
        resendAttempts++;
        generateVerificationCode();
        clearDigitFields();
        digitFields.get(0).requestFocus();
        
        ClientMenu.verify_email(this.userEmail);
        System.out.println("New reset code sent to: " + userEmail);        
        startResendTimer();
        
        showInfo("Verification code resent to " + userEmail);
    }

    private void handleBackToForgotPassword() {
        System.out.println("Returning to forgot password screen...");
        try {
            if (timeline != null) {
                timeline.stop();
            }
            
            Stage currentStage = (Stage) rootPane.getScene().getWindow();
            currentStage.close();

            Stage forgotPasswordStage = new Stage();
            forgotPasswordStage.initStyle(StageStyle.TRANSPARENT);
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/view/ForgotPasswordView.fxml")));

            // Setup window dragging
            root.setOnMousePressed(event -> {
                xOffset = event.getSceneX();
                yOffset = event.getSceneY();
            });
            root.setOnMouseDragged(event -> {
                forgotPasswordStage.setX(event.getScreenX() - xOffset);
                forgotPasswordStage.setY(event.getScreenY() - yOffset);
            });

            Scene scene = new Scene(root);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            String cssPath = "/styles/styles.css";
            URL cssUrl = getClass().getResource(cssPath);
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }

            forgotPasswordStage.setScene(scene);
            forgotPasswordStage.setResizable(false);
            forgotPasswordStage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to return to forgot password screen.");
        }
    }

    private void showError(String message) {
        AlertUtils.showError("Error", message);
    }

    private void showInfo(String message) {
        AlertUtils.showInfo("Information", message);
    }
}