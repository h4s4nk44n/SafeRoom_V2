package com.saferoom.gui.controller;

import com.saferoom.gui.model.User;
import com.saferoom.gui.model.Channel;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class ServerController implements Initializable {

    // Main Pane
    @FXML private BorderPane serverPane;
    
    // Left Sidebar - Channels
    @FXML private VBox channelsSidebar;
    @FXML private HBox serverHeader;
    @FXML private FontIcon serverIcon;
    @FXML private Label serverNameLabel;
    @FXML private Label serverMembersLabel;
    @FXML private Button serverSettingsBtn;
    @FXML private VBox channelsContainer;
    @FXML private VBox voiceChannelsList;
    @FXML private VBox textChannelsList;
    @FXML private HBox userInfoBottom;
    @FXML private Label currentUserAvatar;
    @FXML private Label currentUserName;
    @FXML private Label currentUserStatus;
    @FXML private Button muteBtn;
    @FXML private Button deafenBtn;
    @FXML private Button settingsBtn;
    
    // Voice Channel Items
    @FXML private HBox voiceGeneral;
    @FXML private HBox voiceMeeting;
    @FXML private HBox voicePrivate;
    @FXML private VBox voiceGeneralUsers;
    @FXML private VBox voiceMeetingUsers;
    
    // Text Channel Items
    @FXML private HBox textGeneral;
    @FXML private HBox textAnnouncements;
    @FXML private HBox textTeam;
    @FXML private HBox textPrivate;
    @FXML private VBox textGeneralNotification;
    @FXML private VBox textTeamNotification;
    
    // Main Content Area
    @FXML private StackPane mainContentArea;
    @FXML private VBox defaultChannelView;
    @FXML private BorderPane textChatView;
    @FXML private VBox voiceChatView;
    
    // Text Chat Components
    @FXML private HBox chatHeader;
    @FXML private FontIcon currentChannelIcon;
    @FXML private Label currentChannelName;
    @FXML private Label currentChannelTopic;
    @FXML private ListView<String> messagesListView;
    @FXML private HBox messageInputArea;
    @FXML private TextField messageTextField;
    @FXML private Button sendMessageBtn;
    
    // Voice Chat Components
    @FXML private Label voiceChannelName;
    @FXML private Label voiceChannelInfo;
    @FXML private VBox voiceUsersContainer;
    @FXML private HBox voiceControls;
    @FXML private Button toggleMicBtn;
    @FXML private Button toggleDeafenBtn;
    @FXML private Button shareScreenBtn;
    @FXML private Button leaveVoiceBtn;
    
    // Right Sidebar - Users
    @FXML private VBox usersSidebar;
    @FXML private Label totalUsersCount;
    @FXML private Label onlineUsersCount;
    @FXML private Label offlineUsersCount;
    @FXML private VBox onlineUsersList;
    @FXML private VBox offlineUsersList;
    @FXML private VBox offlineUsersSection;
    
    // Data
    private String currentServerName = "";
    private Channel currentChannel = null;
    private List<User> serverUsers = new ArrayList<>();
    private List<Channel> voiceChannels = new ArrayList<>();
    private List<Channel> textChannels = new ArrayList<>();
    private boolean isUserMuted = false;
    private boolean isUserDeafened = false;
    private boolean offlineUsersExpanded = false;
    private MainController mainController;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupChannelHandlers();
        setupUserControls();
        initializeMockData();
        populateUsers();
        
        // Set default view
        showDefaultView();
        
        // Setup message sending
        sendMessageBtn.setOnAction(event -> sendMessage());
        messageTextField.setOnAction(event -> sendMessage());
        
        // Ensure server pane allows window resize by not consuming edge mouse events
        setupResizeEventPassthrough();
    }
    
    public void setServerInfo(String serverName, String serverIconLiteral) {
        this.currentServerName = serverName;
        serverNameLabel.setText(serverName);
        if (serverIcon != null) {
            serverIcon.setIconLiteral(serverIconLiteral);
        }
        updateMemberCount();
    }
    
    private void setupChannelHandlers() {
        // Voice Channel Handlers
        voiceGeneral.setOnMouseClicked(event -> joinVoiceChannel("General", "fas-volume-up"));
        voiceMeeting.setOnMouseClicked(event -> joinVoiceChannel("Meeting Room", "fas-volume-up"));
        voicePrivate.setOnMouseClicked(event -> joinVoiceChannel("Private Discussion", "fas-lock"));
        
        // Text Channel Handlers
        textGeneral.setOnMouseClicked(event -> openTextChannel("general", "fas-hashtag", "General discussion for the team"));
        textAnnouncements.setOnMouseClicked(event -> openTextChannel("announcements", "fas-bullhorn", "Important announcements and updates"));
        textTeam.setOnMouseClicked(event -> openTextChannel("team-chat", "fas-hashtag", "Team coordination and planning"));
        textPrivate.setOnMouseClicked(event -> openTextChannel("private", "fas-lock", "Private team discussions"));
        
        // Voice users indicators
        voiceGeneralUsers.setVisible(true);
        voiceMeetingUsers.setVisible(true);
        
        // Update channel selection styling
        updateChannelSelection();
        
        // Offline users section toggle
        setupOfflineUsersToggle();
    }
    
    private void setupOfflineUsersToggle() {
        Node offlineHeader = offlineUsersSection.getChildren().get(0);
        offlineHeader.setOnMouseClicked(event -> {
            offlineUsersExpanded = !offlineUsersExpanded;
            offlineUsersList.setVisible(offlineUsersExpanded);
            offlineUsersList.setManaged(offlineUsersExpanded);
            
            // Update arrow icon
            HBox headerBox = (HBox) offlineHeader;
            FontIcon arrow = (FontIcon) headerBox.getChildren().get(0);
            arrow.setIconLiteral(offlineUsersExpanded ? "fas-chevron-down" : "fas-chevron-right");
        });
    }
    
    private void setupUserControls() {
        muteBtn.setOnAction(event -> toggleMute());
        deafenBtn.setOnAction(event -> toggleDeafen());
        settingsBtn.setOnAction(event -> openSettings());
        
        // Voice controls
        toggleMicBtn.setOnAction(event -> toggleMute());
        toggleDeafenBtn.setOnAction(event -> toggleDeafen());
        shareScreenBtn.setOnAction(event -> shareScreen());
        leaveVoiceBtn.setOnAction(event -> leaveVoiceChannel());
    }
    
    private void initializeMockData() {
        // Create mock users
        serverUsers.add(new User("1", "Username", "Online", "Owner", "fas-crown", true, "Working on project"));
        serverUsers.add(new User("2", "Alice Cooper", "Online", "Admin", "fas-shield-alt", true, "In meeting"));
        serverUsers.add(new User("3", "Bob Smith", "Online", "Member", "", true, "Available"));
        serverUsers.add(new User("4", "Carol Johnson", "Idle", "Member", "", true, "Away"));
        serverUsers.add(new User("5", "David Wilson", "Do Not Disturb", "Moderator", "fas-hammer", true, "Focusing"));
        serverUsers.add(new User("6", "Emma Davis", "Online", "Member", "", true, "Chatting"));
        
        // Add some offline users
        for (int i = 7; i <= 20; i++) {
            serverUsers.add(new User(String.valueOf(i), "User" + i, "Offline", "Member", "", false, "Last seen 2 hours ago"));
        }
        
        // Create mock channels
        voiceChannels.add(new Channel("voice_general", "General", "voice", false));
        voiceChannels.add(new Channel("voice_meeting", "Meeting Room", "voice", false));
        voiceChannels.add(new Channel("voice_private", "Private Discussion", "voice", true));
        
        textChannels.add(new Channel("text_general", "general", "text", false));
        textChannels.add(new Channel("text_announcements", "announcements", "text", false));
        textChannels.add(new Channel("text_team", "team-chat", "text", false));
        textChannels.add(new Channel("text_private", "private", "text", true));
    }
    
    private void populateUsers() {
        onlineUsersList.getChildren().clear();
        offlineUsersList.getChildren().clear();
        
        int onlineCount = 0;
        int offlineCount = 0;
        
        for (User user : serverUsers) {
            HBox userItem = createUserItem(user);
            
            if (user.isOnline()) {
                onlineUsersList.getChildren().add(userItem);
                onlineCount++;
            } else {
                offlineUsersList.getChildren().add(userItem);
                offlineCount++;
            }
        }
        
        onlineUsersCount.setText(String.valueOf(onlineCount));
        offlineUsersCount.setText(String.valueOf(offlineCount));
        updateMemberCount();
    }
    
    private HBox createUserItem(User user) {
        HBox userItem = new HBox(8.0);
        userItem.setAlignment(Pos.CENTER_LEFT);
        userItem.getStyleClass().add("user-item");
        
        // User avatar
        StackPane avatar = new StackPane();
        avatar.getStyleClass().add("user-avatar-tiny");
        Label avatarText = new Label(user.getUsername().substring(0, 1).toUpperCase());
        avatarText.getStyleClass().add("user-avatar-text-tiny");
        avatar.getChildren().add(avatarText);
        
        // Status indicator
        StackPane statusContainer = new StackPane();
        statusContainer.getStyleClass().add("user-status-container");
        statusContainer.getChildren().add(avatar);
        
        StackPane statusDot = new StackPane();
        statusDot.getStyleClass().addAll("user-status-dot", getStatusStyleClass(user.getStatus()));
        statusContainer.getChildren().add(statusDot);
        
        // Username and role
        VBox userInfo = new VBox(1.0);
        HBox nameAndRole = new HBox(5.0);
        nameAndRole.setAlignment(Pos.CENTER_LEFT);
        
        Label username = new Label(user.getUsername());
        username.getStyleClass().add("user-name-small");
        nameAndRole.getChildren().add(username);
        
        if (!user.getRoleIcon().isEmpty()) {
            FontIcon roleIcon = new FontIcon(user.getRoleIcon());
            roleIcon.getStyleClass().add("user-role-icon");
            nameAndRole.getChildren().add(roleIcon);
        }
        
        userInfo.getChildren().add(nameAndRole);
        
        if (!user.getActivity().isEmpty()) {
            Label activity = new Label(user.getActivity());
            activity.getStyleClass().add("user-activity");
            userInfo.getChildren().add(activity);
        }
        
        userItem.getChildren().addAll(statusContainer, userInfo);
        
        // Click handler to show user info popup
        userItem.setOnMouseClicked(event -> showUserInfoPopup(user));
        
        return userItem;
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
    
    private void showUserInfoPopup(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/UserInfoPopup.fxml"));
            Parent root = loader.load();
            
            UserInfoPopupController controller = loader.getController();
            controller.setUserInfo(user);
            
            Stage popup = new Stage();
            popup.initModality(Modality.APPLICATION_MODAL);
            popup.initStyle(StageStyle.UTILITY);
            popup.setTitle("User Information");
            popup.setScene(new Scene(root));
            popup.setResizable(false);
            popup.show();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Could not load UserInfoPopup.fxml");
        }
    }
    
    private void openTextChannel(String channelName, String iconLiteral, String topic) {
        currentChannelName.setText(channelName);
        currentChannelIcon.setIconLiteral(iconLiteral);
        currentChannelTopic.setText(topic);
        messageTextField.setPromptText("Message #" + channelName);
        
        // Clear and populate with mock messages
        messagesListView.getItems().clear();
        messagesListView.getItems().addAll(
            "Alice Cooper: Welcome to the " + channelName + " channel!",
            "Bob Smith: Thanks for setting this up",
            "Carol Johnson: Looking forward to collaborating here",
            "David Wilson: Great to have a secure space for our discussions"
        );
        
        showTextChatView();
        updateChannelSelection();
        
        // Clear notification badges for this channel
        clearChannelNotifications(channelName);
    }
    
    private void joinVoiceChannel(String channelName, String iconLiteral) {
        voiceChannelName.setText(channelName);
        
        // Populate with mock voice users
        voiceUsersContainer.getChildren().clear();
        
        List<User> voiceUsers = serverUsers.subList(0, Math.min(5, serverUsers.size()));
        for (User user : voiceUsers) {
            if (user.isOnline()) {
                VBox voiceUser = createVoiceUserItem(user);
                voiceUsersContainer.getChildren().add(voiceUser);
            }
        }
        
        showVoiceChatView();
        updateChannelSelection();
    }
    
    private VBox createVoiceUserItem(User user) {
        VBox voiceUser = new VBox(5.0);
        voiceUser.setAlignment(Pos.CENTER);
        voiceUser.getStyleClass().add("voice-user-item");
        
        // User avatar
        StackPane avatar = new StackPane();
        avatar.getStyleClass().add("voice-user-avatar");
        Label avatarText = new Label(user.getUsername().substring(0, 1).toUpperCase());
        avatarText.getStyleClass().add("voice-user-avatar-text");
        avatar.getChildren().add(avatarText);
        
        // Username
        Label username = new Label(user.getUsername());
        username.getStyleClass().add("voice-user-name");
        
        // Mic status
        FontIcon micIcon = new FontIcon("fas-microphone");
        micIcon.getStyleClass().add("voice-user-mic");
        
        voiceUser.getChildren().addAll(avatar, username, micIcon);
        
        return voiceUser;
    }
    
    private void clearChannelNotifications(String channelName) {
        switch (channelName) {
            case "general":
                textGeneralNotification.setVisible(false);
                break;
            case "team-chat":
                textTeamNotification.setVisible(false);
                break;
        }
    }
    
    private void showDefaultView() {
        defaultChannelView.setVisible(true);
        textChatView.setVisible(false);
        voiceChatView.setVisible(false);
    }
    
    private void showTextChatView() {
        defaultChannelView.setVisible(false);
        textChatView.setVisible(true);
        voiceChatView.setVisible(false);
    }
    
    private void showVoiceChatView() {
        defaultChannelView.setVisible(false);
        textChatView.setVisible(false);
        voiceChatView.setVisible(true);
    }
    
    private void updateChannelSelection() {
        // Remove active class from all channels
        clearChannelSelections();
        
        // Add active class to current channel (implementation can be enhanced)
        // This would typically track the current channel and apply styling
    }
    
    private void clearChannelSelections() {
        // Remove active styling from all channel items
        voiceGeneral.getStyleClass().remove("channel-active");
        voiceMeeting.getStyleClass().remove("channel-active");
        voicePrivate.getStyleClass().remove("channel-active");
        textGeneral.getStyleClass().remove("channel-active");
        textAnnouncements.getStyleClass().remove("channel-active");
        textTeam.getStyleClass().remove("channel-active");
        textPrivate.getStyleClass().remove("channel-active");
    }
    
    private void sendMessage() {
        String messageText = messageTextField.getText().trim();
        if (!messageText.isEmpty()) {
            messagesListView.getItems().add("You: " + messageText);
            messageTextField.clear();
            
            // Scroll to bottom
            messagesListView.scrollTo(messagesListView.getItems().size() - 1);
        }
    }
    
    private void toggleMute() {
        isUserMuted = !isUserMuted;
        FontIcon micIcon = (FontIcon) muteBtn.getGraphic();
        FontIcon voiceMicIcon = (FontIcon) toggleMicBtn.getGraphic();
        
        if (isUserMuted) {
            micIcon.setIconLiteral("fas-microphone-slash");
            voiceMicIcon.setIconLiteral("fas-microphone-slash");
            muteBtn.getStyleClass().add("muted");
            toggleMicBtn.getStyleClass().add("muted");
        } else {
            micIcon.setIconLiteral("fas-microphone");
            voiceMicIcon.setIconLiteral("fas-microphone");
            muteBtn.getStyleClass().remove("muted");
            toggleMicBtn.getStyleClass().remove("muted");
        }
    }
    
    private void toggleDeafen() {
        isUserDeafened = !isUserDeafened;
        FontIcon headphonesIcon = (FontIcon) deafenBtn.getGraphic();
        FontIcon voiceHeadphonesIcon = (FontIcon) toggleDeafenBtn.getGraphic();
        
        if (isUserDeafened) {
            headphonesIcon.setIconLiteral("fas-volume-mute");
            voiceHeadphonesIcon.setIconLiteral("fas-volume-mute");
            deafenBtn.getStyleClass().add("deafened");
            toggleDeafenBtn.getStyleClass().add("deafened");
            
            // If deafened, also mute
            if (!isUserMuted) {
                toggleMute();
            }
        } else {
            headphonesIcon.setIconLiteral("fas-headphones");
            voiceHeadphonesIcon.setIconLiteral("fas-headphones");
            deafenBtn.getStyleClass().remove("deafened");
            toggleDeafenBtn.getStyleClass().remove("deafened");
        }
    }
    
    private void shareScreen() {
        System.out.println("Screen sharing feature would be implemented here");
        // Implementation for screen sharing
    }
    
    private void leaveVoiceChannel() {
        showDefaultView();
        System.out.println("Left voice channel");
    }
    
    private void openSettings() {
        if (MainController.getInstance() != null) {
            MainController.getInstance().handleSettings();
        }
    }
    
    private void updateMemberCount() {
        int totalMembers = serverUsers.size();
        serverMembersLabel.setText(totalMembers + " members");
        totalUsersCount.setText(String.valueOf(totalMembers));
    }
    
    // Method to be called from RoomsController
    public void enterServer(String serverName, String serverIconLiteral) {
        setServerInfo(serverName, serverIconLiteral);
    }
    
    // Method to set MainController reference
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }
    
    private void setupResizeEventPassthrough() {
        if (serverPane != null) {
            // Set up mouse event filters to allow window resize functionality
            serverPane.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, event -> {
                // Check if mouse is near window edges
                double x = event.getSceneX();
                double y = event.getSceneY();
                double sceneWidth = serverPane.getScene().getWidth();
                double sceneHeight = serverPane.getScene().getHeight();
                
                final int RESIZE_BORDER = 10;
                
                boolean isNearEdge = x <= RESIZE_BORDER || x >= sceneWidth - RESIZE_BORDER || 
                                   y <= RESIZE_BORDER || y >= sceneHeight - RESIZE_BORDER;
                
                if (isNearEdge) {
                    // Don't consume the event - let it bubble up to the main window
                    // for resize handling
                    return;
                }
            });
            
            serverPane.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_DRAGGED, event -> {
                // Allow drag events near edges to bubble up for window resize
                double x = event.getSceneX();
                double y = event.getSceneY();
                double sceneWidth = serverPane.getScene().getWidth();
                double sceneHeight = serverPane.getScene().getHeight();
                
                final int RESIZE_BORDER = 10;
                
                boolean isNearEdge = x <= RESIZE_BORDER || x >= sceneWidth - RESIZE_BORDER || 
                                   y <= RESIZE_BORDER || y >= sceneHeight - RESIZE_BORDER;
                
                if (isNearEdge) {
                    // Don't consume - let the main window handle resize
                    return;
                }
            });
            
            serverPane.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_MOVED, event -> {
                // Allow mouse moved events near edges to bubble up for cursor changes
                double x = event.getSceneX();
                double y = event.getSceneY();
                double sceneWidth = serverPane.getScene().getWidth();
                double sceneHeight = serverPane.getScene().getHeight();
                
                final int RESIZE_BORDER = 10;
                
                boolean isNearEdge = x <= RESIZE_BORDER || x >= sceneWidth - RESIZE_BORDER || 
                                   y <= RESIZE_BORDER || y >= sceneHeight - RESIZE_BORDER;
                
                if (isNearEdge) {
                    // Don't consume - let main window handle cursor changes
                    return;
                }
            });
        }
    }
}
