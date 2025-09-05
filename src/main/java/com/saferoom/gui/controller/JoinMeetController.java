package com.saferoom.gui.controller;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXSlider;
import com.jfoenix.controls.JFXToggleButton;
import com.saferoom.gui.MainApp;
import com.saferoom.gui.model.Meeting;
import com.saferoom.gui.model.UserRole;
import com.saferoom.gui.utils.WindowStateManager;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.util.Duration;

import java.io.IOException;
import java.util.Objects;
import java.util.Random;

public class JoinMeetController {

    @FXML private JFXButton backButton;
    @FXML private TextField roomIdField;
    @FXML private Button joinButton;
    @FXML private JFXComboBox<String> audioInputBox;
    @FXML private JFXComboBox<String> audioOutputBox;
    @FXML private JFXComboBox<String> cameraSourceBox;
    @FXML private JFXToggleButton cameraToggle;
    @FXML private JFXToggleButton micToggle;
    @FXML private ProgressBar micTestBar;
    @FXML private JFXSlider inputVolumeSlider;
    @FXML private JFXSlider outputVolumeSlider;

    // Window state manager for dragging functionality
    private WindowStateManager windowStateManager = new WindowStateManager();
    private MainController mainController;

    private Timeline micAnimation;
    private Scene returnScene;

    /**
     * Ana controller referansını ayarlar (geri dönüş için)
     */
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    public void setReturnScene(Scene scene) {
        this.returnScene = scene;
    }

    @FXML
    public void initialize() {
        // Root pane'i bulup sürükleme işlevini etkinleştir
        // Bu view'da herhangi bir pane referansı olmadığı için, scene yüklendikten sonra eklenecek
        
        audioInputBox.getItems().addAll("Default - MacBook Pro Microphone", "External USB Mic");
        audioOutputBox.getItems().addAll("Default - MacBook Pro Speakers", "Bluetooth Headphones");
        cameraSourceBox.getItems().addAll("FaceTime HD Camera", "External Webcam");

        audioInputBox.getSelectionModel().selectFirst();
        audioOutputBox.getSelectionModel().selectFirst();
        cameraSourceBox.getSelectionModel().selectFirst();

        backButton.setOnAction(event -> handleBack());
        joinButton.setOnAction(event -> handleJoin());

        startMicTestAnimation();
        
        // Scene yüklendiğinde root pane'i bul ve sürükleme ekle
        backButton.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null && newScene.getRoot() instanceof javafx.scene.layout.Pane) {
                windowStateManager.setupBasicWindowDrag((javafx.scene.layout.Pane) newScene.getRoot());
            }
        });
    }

    private void handleJoin() {
        if (micAnimation != null) micAnimation.stop();

        // =========================================================================
        // DEGISIKLIK BURADA: Oda ID'sinin bos olup olmadigini kontrol ediyoruz.
        // =========================================================================
        String roomId = roomIdField.getText();
        if (roomId == null || roomId.trim().isEmpty()) {
            // Hata stilini ekle ve metodu sonlandır.
            roomIdField.getStyleClass().remove("error-field"); // Önceki hatayı temizle
            roomIdField.getStyleClass().add("error-field"); // Yeni hatayı ekle
            System.out.println("HATA: Katılmak için Oda ID'si girilmelidir.");
            return; // Katılma işlemini burada durdur.
        }

        // Eğer doğrulama başarılıysa, hata stili (varsa) kaldırılır.
        roomIdField.getStyleClass().remove("error-field");
        // =========================================================================

        // Ana controller üzerinden meeting panel'i yükle (content değiştirme)
        if (mainController != null) {
            try {
                FXMLLoader meetingLoader = new FXMLLoader(MainApp.class.getResource("/view/MeetingPanelView.fxml"));
                Parent meetingRoot = meetingLoader.load();

                MeetingPanelController meetingController = meetingLoader.getController();
                meetingController.setMainController(mainController);

                // roomName için artık 'roomId' değişkenini kullanıyoruz.
                String roomName = roomId.isEmpty() ? "Joined Room" : roomId;

                // Meeting nesnesini 'roomId' ile oluşturuyoruz.
                Meeting meetingToJoin = new Meeting(roomId, roomName);

                meetingController.initData(meetingToJoin, UserRole.USER);

                // Ana controller'ın content area'sına meeting panel'i yükle
                mainController.contentArea.getChildren().setAll(meetingRoot);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void startMicTestAnimation() {
        micAnimation = new Timeline(new KeyFrame(Duration.millis(100), event -> {
            if (micToggle.isSelected()) {
                micTestBar.setProgress(new Random().nextDouble() * 0.7);
            } else {
                micTestBar.setProgress(0);
            }
        }));
        micAnimation.setCycleCount(Animation.INDEFINITE);
        micAnimation.play();
    }

    private void handleBack() {
        if (micAnimation != null) micAnimation.stop();
        
        // Ana controller üzerinden ana görünüme geri dön
        if (mainController != null) {
            mainController.returnToMainView();
        }
    }
}