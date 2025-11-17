package com.saferoom.gui.controller;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXSlider;
import com.jfoenix.controls.JFXToggleButton;
import com.saferoom.gui.MainApp;
import com.saferoom.gui.components.VideoPanel;
import com.saferoom.gui.model.Meeting;
import com.saferoom.gui.model.UserRole;
import com.saferoom.gui.utils.WindowStateManager;
import com.saferoom.webrtc.WebRTCClient;
import dev.onvoid.webrtc.media.video.*;
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
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class SecureRoomController {

    @FXML private JFXButton backButton;
    @FXML private Button initiateButton;
    @FXML private JFXComboBox<String> audioInputBox;
    @FXML private JFXComboBox<String> audioOutputBox;
    @FXML private JFXComboBox<String> cameraSourceBox;
    @FXML private JFXToggleButton cameraToggle;
    @FXML private JFXToggleButton micToggle;
    @FXML private ProgressBar micTestBar;
    @FXML private JFXSlider inputVolumeSlider;
    @FXML private JFXSlider outputVolumeSlider;
    @FXML private JFXCheckBox autoDestroyCheck;
    @FXML private JFXCheckBox noLogsCheck;
    @FXML private TextField roomIdField;
    @FXML private StackPane cameraView; // Sol taraftaki kamera preview alanı

    // Window state manager for dragging functionality
    private WindowStateManager windowStateManager = new WindowStateManager();
    private MainController mainController;

    private Timeline micAnimation;
    private Scene returnScene;
    
    // Camera preview
    private VideoPanel cameraPreview;
    private VideoDeviceSource videoSource;
    private VideoTrack videoTrack;

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
        initiateButton.setOnAction(event -> handleInitiate());

        startMicTestAnimation();
        
        // Scene yüklendiğinde root pane'i bul ve sürükleme ekle
        backButton.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null && newScene.getRoot() instanceof javafx.scene.layout.Pane) {
                windowStateManager.setupBasicWindowDrag((javafx.scene.layout.Pane) newScene.getRoot());
            }
        });
    }

    private void handleInitiate() {
        if (micAnimation != null) micAnimation.stop();

        // Ana controller üzerinden meeting panel'i yükle (content değiştirme)
        if (mainController != null) {
            try {
                FXMLLoader meetingLoader = new FXMLLoader(MainApp.class.getResource("/view/MeetingPanelView.fxml"));
                Parent meetingRoot = meetingLoader.load();

                MeetingPanelController meetingController = meetingLoader.getController();
                meetingController.setMainController(mainController);

                // =========================================================================
                // DEGISIKLIK BURADA: Oda ID'si bos ise rastgele bir ID olusturuyoruz.
                // =========================================================================
                String roomId = roomIdField.getText();
                if (roomId == null || roomId.trim().isEmpty()) {
                    // Basit bir rastgele ID oluşturma (daha karmaşık bir yapı da kullanılabilir)
                    roomId = "SecureRoom-" + (new Random().nextInt(90000) + 10000);
                }
                // =========================================================================

                // Meeting oluştur
                Meeting secureMeeting = new Meeting(roomId, "Secure Room");
                
                // Kamera ve mikrofon ayarlarını al
                boolean withCamera = cameraToggle.isSelected();
                boolean withMic = micToggle.isSelected();

                // Meeting controller'ı camera/mic durumlarıyla başlat
                meetingController.initData(secureMeeting, UserRole.ADMIN, withCamera, withMic);

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