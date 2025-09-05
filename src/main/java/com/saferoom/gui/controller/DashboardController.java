package com.saferoom.gui.controller;

import com.saferoom.gui.controller.cards.ActionCardController;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;

public class DashboardController {

    @FXML private VBox newMeetingCard;
    @FXML private VBox joinRoomCard;
    @FXML private VBox scheduleRoomCard;
    @FXML private VBox encryptedFilesCard;
    @FXML private ActionCardController newMeetingCardController;
    @FXML private ActionCardController joinRoomCardController;
    @FXML private ActionCardController scheduleRoomCardController;
    @FXML private ActionCardController encryptedFilesCardController;
    @FXML private ListView<String> activityListView;

    @FXML
    public void initialize() {
        if (newMeetingCardController != null) {
            newMeetingCardController.setData("fas-plus", "New Meeting", "Instant Secure Room");
        }
        if (joinRoomCardController != null) {
            joinRoomCardController.setData("fas-arrow-right", "Join Meeting", "Connect to Tunnel");
        }
        if (scheduleRoomCardController != null) {
            scheduleRoomCardController.setData("far-clock", "Schedule Room", "Programmatic Sync");
        }
        if (encryptedFilesCardController != null) {
            encryptedFilesCardController.setData("fas-file-archive", "Encrypted Files", "File Vault");
        }

        newMeetingCard.setOnMouseClicked(event -> handleNewMeeting());
        joinRoomCard.setOnMouseClicked(event -> handleJoinMeet());
        encryptedFilesCard.setOnMouseClicked(event -> {
            if (MainController.getInstance() != null) {
                MainController.getInstance().handleFileVault();
            }
        });

        activityListView.getItems().addAll(
                "You started a secure room 'Project Phoenix'",
                "Zeynep Kaya joined 'Project Phoenix'",
                "You shared 'design_final_v3.zip' in File Vault",
                "New message from Ahmet Çelik"
        );
    }

    private void handleNewMeeting() {
        if (MainController.getInstance() != null) {
            MainController.getInstance().loadSecureRoomView();
        } else {
            System.err.println("MainController instance is null. Cannot load SecureRoomView.");
        }
    }

    private void handleJoinMeet() {
        if (MainController.getInstance() != null) {
            MainController.getInstance().loadJoinMeetView();
        } else {
            System.err.println("MainController instance is null. Cannot load JoinMeetView.");
        }
    }
}
