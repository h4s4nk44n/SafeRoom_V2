package com.saferoom.gui.controller;

import com.jfoenix.controls.JFXSlider;
import com.jfoenix.controls.JFXToggleButton;
import com.saferoom.gui.utils.UserSession;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class SettingsController {

    // FXML'deki bileşenlere karşılık gelen değişkenler
    @FXML
    private JFXToggleButton twoFaToggle;
    @FXML
    private JFXToggleButton e2eToggle;
    @FXML
    private JFXToggleButton onlineStatusToggle;
    @FXML
    private JFXToggleButton desktopNotificationToggle;
    @FXML
    private JFXToggleButton soundNotificationToggle;
    @FXML
    private JFXSlider volumeSlider;
    @FXML
    private Button clearDataButton;
    @FXML
    private Button deleteAccountButton;
    @FXML
    private Button backButton;
    @FXML
    private Label userAvatarLabel;
    @FXML
    private Label userNameLabel;
    @FXML
    private Label userEmailLabel;

    @FXML
    public void initialize() {
        // Update user profile information
        updateUserProfile();

        if (backButton != null) {
            backButton.setOnAction(e -> MainController.getInstance().returnFromSettings());
        }

        // Ayar bileşenlerinin olay dinleyicileri buraya eklenebilir.
        // Örnek:
        twoFaToggle.selectedProperty().addListener((obs, oldVal, newVal) -> {
            System.out.println("İki Faktörlü Doğrulama: " + (newVal ? "Açık" : "Kapalı"));
        });

        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            System.out.println("Ses Seviyesi: " + String.format("%.0f%%", newVal));
        });
    }

    private void updateUserProfile() {
        UserSession session = UserSession.getInstance();

        if (userAvatarLabel != null) {
            userAvatarLabel.setText(session.getUserInitials());
        }

        if (userNameLabel != null) {
            userNameLabel.setText(session.getDisplayName());
        }

        if (userEmailLabel != null) {
            String email = session.getCurrentUser() != null ? session.getCurrentUser().getEmail()
                    : "kullanici@saferoom.com";
            userEmailLabel.setText(email);
        }
    }
}
