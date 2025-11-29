package com.saferoom.gui.controller;

import com.saferoom.gui.dialog.ActiveCallDialog;
import com.saferoom.gui.dialog.IncomingCallDialog;
import com.saferoom.gui.dialog.OutgoingCallDialog;
import com.saferoom.gui.model.User;
import com.saferoom.gui.service.ChatService;
import com.saferoom.webrtc.CallManager;
import dev.onvoid.webrtc.media.video.VideoTrack;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
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
    
    // WebRTC Call Integration
    private ChatService chatService;
    private String currentChannelId; // Channel ID for the conversation with this user
    private OutgoingCallDialog currentOutgoingDialog;
    private IncomingCallDialog currentIncomingDialog;
    private ActiveCallDialog currentActiveCallDialog;
    private boolean callbacksSetup = false;
    private boolean currentCallVideoEnabled = false;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.chatService = ChatService.getInstance();
        setupActionHandlers();
    }

    public void setUserInfo(User user) {
        this.currentUser = user;
        // Set channelId as username (same as ChatViewController does with currentChannelId)
        this.currentChannelId = user.getUsername();
        populateUserInfo();
    }
    
    /**
     * Set the channel ID for the conversation with this user
     * @param channelId The channel/conversation ID
     */
    public void setChannelId(String channelId) {
        this.currentChannelId = channelId;
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
            startCall(false); // false = audio only
        }
    }

    private void handleVideoCall() {
        if (currentUser != null && currentUser.isOnline()) {
            System.out.println("Starting video call with: " + currentUser.getUsername());
            startCall(true); // true = video enabled
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
    
    // ===============================
    // WebRTC Call Integration Methods
    // ===============================
    
    /**
     * Start a call (audio or video) with the current user
     * @param videoEnabled true for video call, false for audio only
     */
    private void startCall(boolean videoEnabled) {
        if (currentChannelId == null || currentChannelId.isEmpty()) {
            showAlert("Error", "Cannot start call: user information not available.", Alert.AlertType.ERROR);
            return;
        }
        
        try {
            String targetUsername = currentChannelId;
            String myUsername = chatService.getCurrentUsername();
            
            if (myUsername == null) {
                showAlert("Error", "Cannot start call: current user not logged in.", Alert.AlertType.ERROR);
                return;
            }
            
            CallManager callManager = CallManager.getInstance();
            
            // Initialize CallManager if needed
            if (!callManager.isInitialized()) {
                callManager.initialize(myUsername);
            }
            
            // Setup callbacks
            setupCallManagerCallbacks(callManager);
            
            // Check if already in a call
            if (callManager.isInCall()) {
                showAlert("Call In Progress", "You are already in a call.", Alert.AlertType.WARNING);
                return;
            }
            
            // Start the call
            callManager.startCall(targetUsername, true, videoEnabled)
                    .thenAccept(callId -> {
                        Platform.runLater(() -> {
                            currentCallVideoEnabled = videoEnabled;
                            String callMsg = videoEnabled ? "📹 Starting video call..." : "📞 Starting audio call...";
                            chatService.sendMessage(currentChannelId, callMsg, new com.saferoom.gui.model.User(myUsername, myUsername));
                            
                            // Show outgoing call dialog
                            currentOutgoingDialog = new OutgoingCallDialog(targetUsername, callId, videoEnabled);
                            currentOutgoingDialog.show();
                            
                            // Close the user info popup after starting the call
                            closePopup();
                        });
                    })
                    .exceptionally(e -> {
                        Platform.runLater(() -> {
                            showAlert("Call Failed", "Failed to start call: " + e.getMessage(), Alert.AlertType.ERROR);
                        });
                        return null;
                    });
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Error starting call: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    
    /**
     * Setup CallManager callbacks for handling call events
     */
    private void setupCallManagerCallbacks(CallManager callManager) {
        if (callbacksSetup) {
            return; // Already setup
        }
        callbacksSetup = true;
        
        String myUsername = chatService.getCurrentUsername();
        com.saferoom.gui.model.User currentUserModel = new com.saferoom.gui.model.User(myUsername, myUsername);
        
        // Incoming call callback
        callManager.setOnIncomingCallCallback(info -> {
            Platform.runLater(() -> {
                currentCallVideoEnabled = info.videoEnabled;
                IncomingCallDialog incomingDialog = new IncomingCallDialog(
                    info.callerUsername, 
                    info.callId, 
                    info.videoEnabled
                );
                
                this.currentIncomingDialog = incomingDialog;
                
                incomingDialog.show().thenAccept(accepted -> {
                    if (accepted) {
                        callManager.acceptCall(info.callId);
                    } else {
                        callManager.rejectCall(info.callId);
                    }
                    this.currentIncomingDialog = null;
                });
            });
        });
        
        // Call accepted callback
        callManager.setOnCallAcceptedCallback(callId -> {
            Platform.runLater(() -> {
                if (currentChannelId != null) {
                    chatService.sendMessage(currentChannelId, "✅ Call accepted - connecting...", currentUserModel);
                }
                
                if (currentOutgoingDialog != null) {
                    currentOutgoingDialog.close();
                    currentOutgoingDialog = null;
                }
                
                if (currentActiveCallDialog == null) {
                    currentActiveCallDialog = new ActiveCallDialog(
                        currentChannelId, 
                        callId, 
                        currentCallVideoEnabled, 
                        callManager
                    );
                    currentActiveCallDialog.show();
                    
                    if (currentCallVideoEnabled) {
                        VideoTrack localVideo = callManager.getLocalVideoTrack();
                        if (localVideo != null) {
                            currentActiveCallDialog.attachLocalVideo(localVideo);
                        }
                    }
                }
            });
        });
        
        // Call rejected callback
        callManager.setOnCallRejectedCallback(callId -> {
            Platform.runLater(() -> {
                if (currentChannelId != null) {
                    chatService.sendMessage(currentChannelId, "❌ Call was rejected", currentUserModel);
                }
            });
        });
        
        // Call connected callback
        callManager.setOnCallConnectedCallback(() -> {
            Platform.runLater(() -> {
                if (currentChannelId != null) {
                    chatService.sendMessage(currentChannelId, "🔗 Call connected!", currentUserModel);
                }
            });
        });
        
        // Call ended callback
        callManager.setOnCallEndedCallback(callId -> {
            Platform.runLater(() -> {
                if (currentChannelId != null) {
                    chatService.sendMessage(currentChannelId, "📴 Call ended", currentUserModel);
                }
                
                if (currentOutgoingDialog != null) {
                    currentOutgoingDialog.close();
                    currentOutgoingDialog = null;
                }
                if (currentIncomingDialog != null) {
                    currentIncomingDialog.close();
                    currentIncomingDialog = null;
                }
                if (currentActiveCallDialog != null) {
                    currentActiveCallDialog.close();
                    currentActiveCallDialog = null;
                }
                currentCallVideoEnabled = false;
            });
        });
        
        // Remote video track callback
        callManager.setOnRemoteTrackCallback(track -> {
            Platform.runLater(() -> {
                if (track instanceof VideoTrack && currentActiveCallDialog != null) {
                    currentActiveCallDialog.attachRemoteVideo((VideoTrack) track);
                }
            });
        });
        
        // Remote screen share stopped callback
        callManager.setOnRemoteScreenShareStoppedCallback(() -> {
            Platform.runLater(() -> {
                if (currentActiveCallDialog != null) {
                    currentActiveCallDialog.onRemoteScreenShareStopped();
                }
            });
        });
    }
    
    /**
     * Show an alert dialog
     */
    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        
        try {
            alert.getDialogPane().getStylesheets().add(
                getClass().getResource("/styles/styles.css").toExternalForm()
            );
        } catch (Exception e) {
            // Ignore if stylesheet loading fails
        }
        
        alert.show();
    }
}
