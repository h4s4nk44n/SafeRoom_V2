package com.saferoom.gui.controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import com.saferoom.gui.utils.AlertUtils;
import com.saferoom.client.ClientMenu;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Objects;

public class VerifyEmailController {

    @FXML private VBox rootPane;
    @FXML private TextField digit1, digit2, digit3, digit4, digit5, digit6;
    @FXML private Button verifyButton;
    @FXML private Hyperlink resendLink;
    @FXML private Hyperlink backToLoginLink;

    private List<TextField> digitFields;
    private Timeline timeline;
    private final IntegerProperty countdownSeconds = new SimpleIntegerProperty();
    private static final int COUNTDOWN_START_VALUE = 60;
    
    // Username'i saklamak için
    private String currentUsername;

    // Sürükleme için ofset değişkenleri
    private double xOffset = 0;
    private double yOffset = 0;
    
    /**
     * RegisterController'dan username'i alır
     */
    public void setUsername(String username) {
        this.currentUsername = username;
        System.out.println("VerifyEmailController: Username set to " + username);
    }

    @FXML
    public void initialize() {
        digitFields = List.of(digit1, digit2, digit3, digit4, digit5, digit6);

        setupOtpFields();

        verifyButton.setOnAction(event -> handleVerify());
        backToLoginLink.setOnAction(event -> handleBackToLogin());
        resendLink.setOnAction(event -> handleResend());

        startResendTimer(); // Sayfayı ilk açıldığında sayacı başlat
    }

    private void setupOtpFields() {
        for (int i = 0; i < digitFields.size(); i++) {
            final TextField currentField = digitFields.get(i);
            final int nextIndex = i + 1;
            final int prevIndex = i - 1;

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
            });

            currentField.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.BACK_SPACE && currentField.getText().isEmpty() && prevIndex >= 0) {
                    TextField prevField = digitFields.get(prevIndex);
                    prevField.requestFocus();
                    prevField.end();
                }
            });
        }
    }

    /**
     * Geri sayım sayacını başlatan metod.
     */
    private void startResendTimer() {
        if (timeline != null) {
            timeline.stop();
        }
        countdownSeconds.set(COUNTDOWN_START_VALUE);
        resendLink.setDisable(true); // Linki pasif yap

        // Linkin metnini geri sayım değerine bağla
        resendLink.textProperty().bind(countdownSeconds.asString("Resend in %ds"));

        timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            countdownSeconds.set(countdownSeconds.get() - 1);
            if (countdownSeconds.get() <= 0) {
                timeline.stop();
                resendLink.textProperty().unbind(); // Bağlantıyı kopar
                resendLink.setText("Resend"); // Metni eski haline getir
                resendLink.setDisable(false); // Linki aktif yap
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    /**
     * "Resend" linkine tıklandığında çalışır.
     */
    private void handleResend() {
        if (currentUsername == null || currentUsername.isEmpty()) {
            showAlert("Error", "Username not found. Please restart registration.");
            return;
        }
        
        System.out.println("Yeniden kod gönderme isteği - Username: " + currentUsername);
        // TODO: Backend'e yeniden kod gönderme isteği yollanacak.
        // Şimdilik sadece timer'ı restart ediyoruz
        startResendTimer(); // Sayacı yeniden başlat
        showAlert("Info", "Verification code has been resent to your email.");
    }

    private void handleVerify() {
        StringBuilder code = new StringBuilder();
        digitFields.forEach(field -> code.append(field.getText()));

        System.out.println("Doğrulama denendi. Kod: " + code.toString());
        
        if (code.length() == 6) {
            if (currentUsername == null || currentUsername.isEmpty()) {
                showAlert("Error", "Username not found. Please restart registration.");
                return;
            }
            
            try {
                int verificationResult = ClientMenu.verify_user(currentUsername, code.toString());
                
                switch (verificationResult) {
                    case 0:
                        showAlert("Success", "Email verification completed successfully!");
                        handleBackToLogin();
                        break;
                    case 1:
                        showAlert("Error", "Invalid verification code. Please try again.");
                        clearDigitFields();
                        break;
                    case 2:
                        showAlert("Error", "Connection error. Please try again.");
                        break;
                    default:
                        showAlert("Error", "Unexpected error occurred. Please try again.");
                        break;
                }
            } catch (Exception e) {
                System.err.println("Verification error: " + e.getMessage());
                showAlert("Error", "Failed to verify code. Please check your connection.");
            }
        } else {
            showAlert("Error", "Please enter the complete 6-digit code.");
        }
    }
    
    /**
     * Kod alanlarını temizler
     */
    private void clearDigitFields() {
        digitFields.forEach(field -> field.clear());
        digitFields.get(0).requestFocus();
    }

    private void handleBackToLogin() {
        System.out.println("Giriş ekranına dönülüyor...");
        try {
            if (timeline != null) {
                timeline.stop(); // Sayfadan ayrılırken sayacı durdur
            }
            Stage currentStage = (Stage) rootPane.getScene().getWindow();
            currentStage.close();

            Stage loginStage = new Stage();
            loginStage.initStyle(StageStyle.TRANSPARENT);
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/view/LoginView.fxml")));

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
        }
    }

    private void showAlert(String title, String content) {
        AlertUtils.showInfo(title, content);
    }
}
