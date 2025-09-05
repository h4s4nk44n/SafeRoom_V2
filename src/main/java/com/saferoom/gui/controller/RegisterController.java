package com.saferoom.gui.controller;

import com.jfoenix.controls.JFXButton;
import com.saferoom.gui.utils.AlertUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

public class RegisterController {

    @FXML private VBox rootPane;
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Button createAccountButton;
    @FXML private JFXButton googleSignUpButton;
    @FXML private JFXButton githubSignUpButton;
    @FXML private Hyperlink signInLink;
    @FXML private Button closeButton;

    private double xOffset = 0;
    private double yOffset = 0;

    @FXML
    public void initialize() {
        createAccountButton.setOnAction(event -> handleCreateAccount());
        googleSignUpButton.setOnAction(event -> handleGoogleSignUp());
        githubSignUpButton.setOnAction(event -> handleGitHubSignUp());
        signInLink.setOnAction(event -> handleSignInLink());
        closeButton.setOnAction(event -> handleClose());
    }

    /**
     * "Create Account" butonuna tıklandığında, kayıt penceresini kapatıp
     * mail doğrulama penceresini açar.
     */
    private void handleCreateAccount() {
        System.out.println("Mail doğrulama ekranına geçiliyor...");
        try {
            // Mevcut kayıt penceresini kapat
            Stage currentStage = (Stage) rootPane.getScene().getWindow();
            currentStage.close();

            // Yeni bir doğrulama penceresi aç
            Stage verifyStage = new Stage();
            verifyStage.initStyle(StageStyle.TRANSPARENT);
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/view/VerifyEmailView.fxml")));

            // Sürükleme özelliği
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

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleGoogleSignUp() {
        showAlert("Google ile Kayıt", "Bu özellik yakında eklenecektir.");
    }

    private void handleGitHubSignUp() {
        showAlert("GitHub ile Kayıt", "Bu özellik yakında eklenecektir.");
    }

    private void handleSignInLink() {
        System.out.println("Giriş ekranına dönülüyor...");
        try {
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

    private void handleClose() {
        Platform.exit();
    }

    private void showAlert(String title, String content) {
        AlertUtils.showInfo(title, content);
    }
}
