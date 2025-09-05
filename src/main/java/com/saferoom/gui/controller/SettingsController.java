package com.saferoom.gui.controller;

import com.jfoenix.controls.JFXSlider;
import com.jfoenix.controls.JFXToggleButton;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

public class SettingsController {

    // FXML'deki bileşenlere karşılık gelen değişkenler
    @FXML private JFXToggleButton twoFaToggle;
    @FXML private JFXToggleButton e2eToggle;
    @FXML private JFXToggleButton onlineStatusToggle;
    @FXML private JFXToggleButton desktopNotificationToggle;
    @FXML private JFXToggleButton soundNotificationToggle;
    @FXML private JFXSlider volumeSlider;
    @FXML private Button clearDataButton;
    @FXML private Button deleteAccountButton;

    @FXML
    public void initialize() {
        // Ayar bileşenlerinin olay dinleyicileri buraya eklenebilir.
        // Örnek:
        twoFaToggle.selectedProperty().addListener((obs, oldVal, newVal) -> {
            System.out.println("İki Faktörlü Doğrulama: " + (newVal ? "Açık" : "Kapalı"));
        });

        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            System.out.println("Ses Seviyesi: " + String.format("%.0f%%", newVal));
        });
    }
}
