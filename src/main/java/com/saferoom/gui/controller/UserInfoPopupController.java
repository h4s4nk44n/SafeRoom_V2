package com.saferoom.gui.controller;

import com.saferoom.gui.model.User;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class UserInfoPopupController implements Initializable {

    // User Avatar and Basic Info
    @FXML private StackPane userAvatarContainer;
    @FXML private StackPane userAvatar;
    @FXML private Label userAvatarText;
    @FXML private StackPane statusIndicator;
    @FXML private Label usernameLabel;
    @FXML private FontIcon roleIcon;
    @FXML private Label statusLabel;

    // User Details
    @FXML private FontIcon roleDisplayIcon;
    @FXML private Label roleLabel;
    @FXML private Label statusDisplayLabel;
    @FXML private VBox activitySection;
    @FXML private Label activityLabel;
    @FXML private Label memberSinceLabel;
    @FXML private VBox userIdSection;
    @FXML private Label userIdLabel;

    // Action Buttons
    @FXML private Button sendMessageBtn;
    @FXML private Button callBtn;
    @FXML private Button videoCallBtn;
    @FXML private VBox adminActionsSection;
    @FXML private Button muteBtn;
    @FXML private Button kickBtn;
    @FXML private Button banBtn;
    @FXML private Button closeBtn;

    private User currentUser;
    private boolean isCurrentUserAdmin = false; // This would be determined by current user's permissions

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupActionHandlers();
    }

    public void setUserInfo(User user) {
        this.currentUser = user;
        populateUserInfo();
    }

    public void setCurrentUserAdmin(boolean isAdmin) {
        this.isCurrentUserAdmin = isAdmin;
        adminActionsSection.setVisible(isAdmin);
        adminActionsSection.setManaged(isAdmin);
    }

    private void populateUserInfo() {
        if (currentUser == null) return;

        // Set avatar text (first letter of username)
        userAvatarText.setText(currentUser.getUsername().substring(0, 1).toUpperCase());

        // Set username
        usernameLabel.setText(currentUser.getUsername());

        // Set role icon if exists
        if (!currentUser.getRoleIcon().isEmpty()) {
            roleIcon.setIconLiteral(currentUser.getRoleIcon());
            roleIcon.setVisible(true);
            
            roleDisplayIcon.setIconLiteral(currentUser.getRoleIcon());
            roleDisplayIcon.setVisible(true);
        } else {
            roleIcon.setVisible(false);
            roleDisplayIcon.setVisible(false);
        }

        // Set status
        statusLabel.setText(currentUser.getStatus());
        statusDisplayLabel.setText(currentUser.getStatus());

        // Set status indicator styling
        statusIndicator.getStyleClass().removeAll("status-online", "status-idle", "status-dnd", "status-offline");
        statusIndicator.getStyleClass().add(getStatusStyleClass(currentUser.getStatus()));

        // Set role
        roleLabel.setText(currentUser.getRole());

        // Set activity
        if (!currentUser.getActivity().isEmpty()) {
            activityLabel.setText(currentUser.getActivity());
            activitySection.setVisible(true);
            activitySection.setManaged(true);
        } else {
            activitySection.setVisible(false);
            activitySection.setManaged(false);
        }

        // Set member since (mock data for now)
        memberSinceLabel.setText(generateMockMemberSince());

        // Set user ID (for debugging - only show for admins)
        userIdLabel.setText(currentUser.getId());
        userIdSection.setVisible(isCurrentUserAdmin);
        userIdSection.setManaged(isCurrentUserAdmin);

        // Disable actions for offline users
        boolean isUserOnline = currentUser.isOnline();
        callBtn.setDisable(!isUserOnline);
        videoCallBtn.setDisable(!isUserOnline);
    }

    private String getStatusStyleClass(String status) {
        switch (status.toLowerCase()) {
            case "online": return "status-online";
            case "idle": return "status-idle";
            case "do not disturb": return "status-dnd";
            case "offline": return "status-offline";
            default: return "status-offline";
        }
    }

    private String generateMockMemberSince() {
        // Generate a mock date based on user ID for consistency
        int daysAgo = Math.abs(currentUser.getId().hashCode()) % 365 + 30; // 30-395 days ago
        LocalDate memberSince = LocalDate.now().minusDays(daysAgo);
        return memberSince.format(DateTimeFormatter.ofPattern("MMMM yyyy"));
    }

    private void setupActionHandlers() {
        sendMessageBtn.setOnAction(event -> handleSendMessage());
        callBtn.setOnAction(event -> handleCall());
        videoCallBtn.setOnAction(event -> handleVideoCall());
        
        // Admin actions
        muteBtn.setOnAction(event -> handleMute());
        kickBtn.setOnAction(event -> handleKick());
        banBtn.setOnAction(event -> handleBan());
    }

    private void handleSendMessage() {
        if (currentUser != null) {
            System.out.println("Opening direct message with: " + currentUser.getUsername());
            // Here you would integrate with the chat system
            // For example: ChatController.openDirectMessage(currentUser);
            closePopup();
        }
    }

    private void handleCall() {
        if (currentUser != null && currentUser.isOnline()) {
            System.out.println("Starting voice call with: " + currentUser.getUsername());
            // Here you would integrate with the voice call system
            closePopup();
        }
    }

    private void handleVideoCall() {
        if (currentUser != null && currentUser.isOnline()) {
            System.out.println("Starting video call with: " + currentUser.getUsername());
            // Here you would integrate with the video call system
            closePopup();
        }
    }

    private void handleMute() {
        if (currentUser != null) {
            System.out.println("Muting user: " + currentUser.getUsername());
            // Here you would implement server muting functionality
            closePopup();
        }
    }

    private void handleKick() {
        if (currentUser != null) {
            System.out.println("Kicking user: " + currentUser.getUsername());
            // Here you would implement user kicking functionality
            closePopup();
        }
    }

    private void handleBan() {
        if (currentUser != null) {
            System.out.println("Banning user: " + currentUser.getUsername());
            // Here you would implement user banning functionality
            closePopup();
        }
    }

    @FXML
    private void closePopup() {
        Stage stage = (Stage) closeBtn.getScene().getWindow();
        stage.close();
    }

    // Method to set user-specific permissions
    public void setPermissions(boolean canMute, boolean canKick, boolean canBan) {
        if (adminActionsSection.isVisible()) {
            muteBtn.setVisible(canMute);
            muteBtn.setManaged(canMute);
            kickBtn.setVisible(canKick);
            kickBtn.setManaged(canKick);
            banBtn.setVisible(canBan);
            banBtn.setManaged(canBan);
        }
    }
}
