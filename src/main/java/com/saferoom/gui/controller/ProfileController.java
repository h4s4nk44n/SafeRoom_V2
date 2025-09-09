package com.saferoom.gui.controller;

import com.jfoenix.controls.JFXButton;
import com.saferoom.client.ClientMenu;
import com.saferoom.grpc.SafeRoomProto;
import com.saferoom.gui.utils.UserSession;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

public class ProfileController {

    // Profile Header Elements
    @FXML private JFXButton backButton;
    @FXML private Label profileAvatar;
    @FXML private Label usernameLabel;
    @FXML private Label statusDot;
    @FXML private Label statusLabel;
    @FXML private Label emailLabel;
    @FXML private Label joinDateLabel;
    @FXML private Label lastSeenLabel;
    
    // Action Buttons
    @FXML private JFXButton friendActionButton;
    @FXML private JFXButton messageButton;
    @FXML private JFXButton blockButton;
    
    // Stats Elements
    @FXML private Label roomsCreatedLabel;
    @FXML private Label roomsJoinedLabel;
    @FXML private Label messagesSentLabel;
    @FXML private Label filesSharedLabel;
    @FXML private Label securityScoreLabel;
    @FXML private ProgressBar securityProgressBar;
    
    // Content Containers
    @FXML private TabPane profileTabs;
    @FXML private VBox activitiesContainer;
    @FXML private VBox roomsContainer;
    @FXML private VBox allActivitiesContainer;
    
    // Profile Data
    private String targetUsername;
    private String currentUser;
    private SafeRoomProto.UserProfile userProfile;
    private String friendStatus = "none"; // none, pending, friends, blocked

    @FXML
    public void initialize() {
        // Setup back button
        backButton.setOnAction(e -> handleBack());
        
        // Initialize default values
        setupDefaultContent();
    }
    
    public void setProfileData(String username) {
        this.targetUsername = username;
        this.currentUser = UserSession.getInstance().getDisplayName();
        
        // Load profile data from server
        loadProfileData();
    }
    
    private void setupDefaultContent() {
        // Setup loading state
        usernameLabel.setText("Loading...");
        emailLabel.setText("Loading...");
        joinDateLabel.setText("Loading...");
        lastSeenLabel.setText("Loading...");
        
        // Setup activity containers with loading states
        addLoadingState(activitiesContainer, "Loading recent activities...");
        addLoadingState(allActivitiesContainer, "Loading all activities...");
        
        // Clear rooms container
        roomsContainer.getChildren().clear();
        addEmptyState(roomsContainer, "No rooms to display");
    }
    
    private void loadProfileData() {
        System.out.println("🔄 Loading profile for user: " + targetUsername);
        
        CompletableFuture.supplyAsync(() -> {
            try {
                return ClientMenu.getProfile(targetUsername, currentUser);
            } catch (Exception e) {
                System.err.println("❌ Error loading profile: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        }).thenAcceptAsync(response -> {
            Platform.runLater(() -> {
                if (response != null && response.getSuccess()) {
                    this.userProfile = response.getProfile();
                    this.friendStatus = userProfile.getFriendStatus();
                    updateUI();
                    System.out.println("✅ Profile loaded successfully");
                } else {
                    showErrorState();
                    System.err.println("❌ Failed to load profile");
                }
            });
        });
    }
    
    private void updateUI() {
        // Update basic info
        usernameLabel.setText(userProfile.getUsername());
        emailLabel.setText(userProfile.getEmail());
        joinDateLabel.setText("Joined " + formatDate(userProfile.getJoinDate()));
        
        // Update avatar
        String firstLetter = userProfile.getUsername().substring(0, 1).toUpperCase();
        profileAvatar.setText(firstLetter);
        
        // Update online status
        if (userProfile.getIsOnline()) {
            statusLabel.setText("Online");
            statusDot.getStyleClass().removeAll("status-offline", "status-away");
            statusDot.getStyleClass().add("status-online");
        } else {
            statusLabel.setText("Last seen " + formatLastSeen(userProfile.getLastSeen()));
            statusDot.getStyleClass().removeAll("status-online", "status-away");
            statusDot.getStyleClass().add("status-offline");
        }
        
        // Update stats
        if (userProfile.hasStats()) {
            SafeRoomProto.UserStats stats = userProfile.getStats();
            roomsCreatedLabel.setText(String.valueOf(stats.getRoomsCreated()));
            roomsJoinedLabel.setText(String.valueOf(stats.getRoomsJoined()));
            messagesSentLabel.setText(String.valueOf(stats.getMessagesSent()));
            filesSharedLabel.setText(String.valueOf(stats.getFilesShared()));
            
            double activityScore = stats.getSecurityScore(); // Proto'da hala securityScore ama activity olarak kullanıyoruz
            securityScoreLabel.setText(String.format("%.1f", activityScore));
            securityProgressBar.setProgress(activityScore / 10.0); // 10 üzerinden score
        }
        
        // Update friend action button
        updateFriendActionButton();
        
        // Load activities
        loadActivities();
    }
    
    private void updateFriendActionButton() {
        switch (friendStatus.toLowerCase()) {
            case "none":
                friendActionButton.setText("Add Friend");
                friendActionButton.getStyleClass().removeAll("pending-button", "friends-button");
                friendActionButton.getStyleClass().add("add-friend-button");
                friendActionButton.setDisable(false);
                break;
            case "pending":
                friendActionButton.setText("Pending");
                friendActionButton.getStyleClass().removeAll("add-friend-button", "friends-button");
                friendActionButton.getStyleClass().add("pending-button");
                friendActionButton.setDisable(true);
                break;
            case "friends":
                friendActionButton.setText("Friends");
                friendActionButton.getStyleClass().removeAll("add-friend-button", "pending-button");
                friendActionButton.getStyleClass().add("friends-button");
                friendActionButton.setDisable(true);
                break;
            case "blocked":
                friendActionButton.setText("Blocked");
                friendActionButton.setDisable(true);
                break;
        }
        
        // Enable/disable other buttons based on relationship
        boolean canInteract = !friendStatus.equals("blocked");
        messageButton.setDisable(!canInteract);
        blockButton.setText(friendStatus.equals("blocked") ? "Unblock" : "Block");
    }
    
    private void loadActivities() {
        // Clear loading states
        activitiesContainer.getChildren().clear();
        allActivitiesContainer.getChildren().clear();
        
        if (userProfile.getRecentActivitiesCount() > 0) {
            // Load recent activities (first 5)
            int count = 0;
            for (SafeRoomProto.UserActivity activity : userProfile.getRecentActivitiesList()) {
                if (count >= 5) break;
                
                VBox activityCard = createActivityCard(activity);
                activitiesContainer.getChildren().add(activityCard);
                count++;
            }
            
            // Load all activities
            for (SafeRoomProto.UserActivity activity : userProfile.getRecentActivitiesList()) {
                VBox activityCard = createActivityCard(activity);
                allActivitiesContainer.getChildren().add(activityCard);
            }
        } else {
            // No activities
            addEmptyState(activitiesContainer, "No recent activity");
            addEmptyState(allActivitiesContainer, "No activity to display");
        }
    }
    
    private VBox createActivityCard(SafeRoomProto.UserActivity activity) {
        VBox card = new VBox(8);
        card.getStyleClass().add("activity-card");
        
        // Activity header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        
        // Activity icon
        FontIcon icon = getActivityIcon(activity.getActivityType());
        icon.getStyleClass().add("activity-icon");
        
        // Activity description
        Label description = new Label(activity.getDescription());
        description.getStyleClass().add("activity-description");
        description.setWrapText(true);
        
        header.getChildren().addAll(icon, description);
        
        // Activity time
        Label timestamp = new Label(formatTimestamp(activity.getTimestamp()));
        timestamp.getStyleClass().add("activity-timestamp");
        
        card.getChildren().addAll(header, timestamp);
        return card;
    }
    
    private FontIcon getActivityIcon(String activityType) {
        switch (activityType.toLowerCase()) {
            case "room_created":
                return new FontIcon("fas-plus-circle");
            case "room_joined":
                return new FontIcon("fas-sign-in-alt");
            case "message_sent":
                return new FontIcon("fas-comment");
            case "file_shared":
                return new FontIcon("fas-file-export");
            case "login":
                return new FontIcon("fas-sign-in-alt");
            case "logout":
                return new FontIcon("fas-sign-out-alt");
            default:
                return new FontIcon("fas-circle");
        }
    }
    
    private void addLoadingState(VBox container, String message) {
        container.getChildren().clear();
        Label loadingLabel = new Label(message);
        loadingLabel.getStyleClass().add("loading-state");
        container.getChildren().add(loadingLabel);
    }
    
    private void addEmptyState(VBox container, String message) {
        container.getChildren().clear();
        Label emptyLabel = new Label(message);
        emptyLabel.getStyleClass().add("empty-state");
        container.getChildren().add(emptyLabel);
    }
    
    private void showErrorState() {
        usernameLabel.setText("Error loading profile");
        emailLabel.setText("Could not load user data");
        joinDateLabel.setText("Error");
        lastSeenLabel.setText("Error");
        
        addEmptyState(activitiesContainer, "Error loading activities");
        addEmptyState(allActivitiesContainer, "Error loading activities");
    }
    
    @FXML
    private void handleBack() {
        // Navigate back to friends view
        try {
            MainController mainController = MainController.getInstance();
            if (mainController != null) {
                mainController.handleFriends();
            }
        } catch (Exception e) {
            System.err.println("Error navigating back: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleFriendAction() {
        if (friendStatus.equals("none")) {
            sendFriendRequest();
        }
    }
    
    private void sendFriendRequest() {
        System.out.println("🤝 Sending friend request to: " + targetUsername);
        
        friendActionButton.setDisable(true);
        friendActionButton.setText("Sending...");
        
        CompletableFuture.supplyAsync(() -> {
            try {
                return ClientMenu.sendFriendRequest(currentUser, targetUsername);
            } catch (Exception e) {
                System.err.println("❌ Error sending friend request: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        }).thenAcceptAsync(response -> {
            Platform.runLater(() -> {
                if (response != null && response.getSuccess()) {
                    friendStatus = "pending";
                    updateFriendActionButton();
                    System.out.println("✅ Friend request sent successfully");
                    
                    // Show success notification
                    showNotification("Friend request sent!", "success");
                } else {
                    friendActionButton.setDisable(false);
                    friendActionButton.setText("Add Friend");
                    System.err.println("❌ Failed to send friend request");
                    
                    // Show error notification
                    String errorMessage = response != null ? response.getMessage() : "Unknown error";
                    showNotification("Failed to send friend request: " + errorMessage, "error");
                }
            });
        });
    }
    
    @FXML
    private void handleMessage() {
        System.out.println("💬 Opening message with: " + targetUsername);
        
        try {
            MainController mainController = MainController.getInstance();
            if (mainController != null) {
                // Messages sekmesine geç
                mainController.handleMessages();
                
                // TODO: MessagesController'da belirli kullanıcıyla sohbet başlatma işlevselliği
                // Bu kısım daha sonra Messages controller'da implement edilecek
                System.out.println("📱 Switched to Messages tab for user: " + targetUsername);
                
                // Notification göster
                showNotification("Switched to Messages - Chat with " + targetUsername + " will be implemented soon!", "info");
            }
        } catch (Exception e) {
            System.err.println("Error opening messages: " + e.getMessage());
            showNotification("Error opening messages", "error");
        }
    }
    
    @FXML
    private void handleBlock() {
        System.out.println("🚫 Block/Unblock user: " + targetUsername);
        // TODO: Implement block/unblock functionality
        showNotification("Block feature coming soon!", "info");
    }
    
    private void showNotification(String message, String type) {
        // Simple notification system - could be improved with proper toast notifications
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Notification");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    // Utility methods for date formatting
    private String formatDate(String dateStr) {
        try {
            // Try different date formats
            if (dateStr == null || dateStr.isEmpty()) {
                return "Unknown";
            }
            
            // Remove any trailing timezone info and parse
            String cleanDateStr = dateStr.replace("T", " ").split("\\.")[0];
            
            // Parse MySQL timestamp format
            LocalDateTime date = LocalDateTime.parse(cleanDateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            return date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
        } catch (Exception e) {
            // Fallback: try to extract just the date part
            try {
                if (dateStr.contains(" ")) {
                    String datePart = dateStr.split(" ")[0];
                    LocalDate date = LocalDate.parse(datePart);
                    return date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
                }
            } catch (Exception e2) {
                // If all parsing fails, return a cleaned version
                return dateStr.split("T")[0]; // Just return YYYY-MM-DD
            }
            return "Unknown";
        }
    }
    
    private String formatLastSeen(String lastSeenStr) {
        try {
            LocalDateTime lastSeen = LocalDateTime.parse(lastSeenStr);
            LocalDateTime now = LocalDateTime.now();
            
            long minutes = java.time.Duration.between(lastSeen, now).toMinutes();
            
            if (minutes < 1) {
                return "just now";
            } else if (minutes < 60) {
                return minutes + " minutes ago";
            } else if (minutes < 1440) { // Less than 24 hours
                long hours = minutes / 60;
                return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
            } else {
                return lastSeen.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
            }
        } catch (Exception e) {
            return lastSeenStr; // Return as-is if parsing fails
        }
    }
    
    private String formatTimestamp(String timestampStr) {
        try {
            LocalDateTime timestamp = LocalDateTime.parse(timestampStr);
            LocalDateTime now = LocalDateTime.now();
            
            long minutes = java.time.Duration.between(timestamp, now).toMinutes();
            
            if (minutes < 1) {
                return "just now";
            } else if (minutes < 60) {
                return minutes + "m ago";
            } else if (minutes < 1440) {
                long hours = minutes / 60;
                return hours + "h ago";
            } else {
                long days = minutes / 1440;
                return days + "d ago";
            }
        } catch (Exception e) {
            return timestampStr;
        }
    }
}
