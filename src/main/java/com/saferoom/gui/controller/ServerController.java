package com.saferoom.gui.controller;

import com.saferoom.gui.model.User;
import com.saferoom.gui.model.Channel;
import com.saferoom.gui.utils.UserSession;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Popup;
import com.saferoom.gui.controller.EditChannelPopupController;
import com.saferoom.gui.controller.EditChannelPopupController.Result;

public class ServerController implements Initializable {

    // Main Pane
    @FXML
    private BorderPane serverPane;

    private Popup currentEditPopup;
    private Node currentEditTarget;

    // Left Sidebar - Channels
    @FXML
    private VBox channelsSidebar;
    @FXML
    private VBox serverHeader;
    @FXML
    private FontIcon serverIcon;
    @FXML
    private Label serverNameLabel;
    @FXML
    private Label serverMembersLabel;
    @FXML
    private Label serverPrivacyBadge; // Added
    @FXML
    private Button serverSettingsBtn;
    @FXML
    private VBox channelsContainer;
    @FXML
    private VBox voiceChannelsSection;
    @FXML
    private VBox textChannelsSection;
    @FXML
    private VBox voiceChannelsList;
    @FXML
    private VBox textChannelsList;
    @FXML
    private VBox fileChannelsSection;
    @FXML
    private VBox fileChannelsList;
    @FXML
    private HBox userInfoBottom;
    // @FXML private Label currentUserAvatar; // Removed as per UI change
    @FXML
    private Label currentUserName;
    @FXML
    private Label currentUserStatus;
    @FXML
    private Button muteBtn;
    @FXML
    private Button deafenBtn;
    @FXML
    private Button settingsBtn;

    // Voice Channel Items
    @FXML
    private HBox voiceGeneral;
    @FXML
    private HBox voiceMeeting;
    @FXML
    private HBox voicePrivate;
    @FXML
    private VBox voiceGeneralUsers;
    @FXML
    private VBox voiceMeetingUsers;

    // Text Channel Items
    @FXML
    private HBox textGeneral;
    @FXML
    private HBox textAnnouncements;
    @FXML
    private HBox textTeam;
    @FXML
    private HBox textPrivate;
    @FXML
    private HBox fileResources;
    @FXML
    private HBox fileProject;
    @FXML
    private VBox textGeneralNotification;
    @FXML
    private VBox textTeamNotification;

    // Main Content Area
    @FXML
    private StackPane mainContentArea;
    @FXML
    private VBox defaultChannelView;
    @FXML
    private BorderPane textChatView;
    @FXML
    private VBox voiceChatView;

    // Text Chat Components
    @FXML
    private HBox chatHeader;
    @FXML
    private FontIcon currentChannelIcon;
    @FXML
    private Label currentChannelName;
    @FXML
    private Label currentChannelTopic;
    @FXML
    private ListView<String> messagesListView;
    @FXML
    private HBox messageInputArea;
    @FXML
    private TextField messageTextField;
    @FXML
    private Button sendMessageBtn;

    // Voice Chat Components
    @FXML
    private Label voiceChannelName;
    @FXML
    private Label voiceChannelInfo;
    @FXML
    private VBox voiceUsersContainer;
    @FXML
    private HBox voiceControls;
    @FXML
    private Button toggleMicBtn;
    @FXML
    private Button toggleDeafenBtn;
    @FXML
    private Button shareScreenBtn;
    @FXML
    private Button leaveVoiceBtn;

    // Helper method for channel creation dialog
    private void showCreateChannelDialog(VBox sectionNode, VBox listNode, boolean initialVoice) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/CreateChannelDialog.fxml"));
            Parent root = loader.load();

            CreateChannelDialogController controller = loader.getController();
            controller.setInitialCategory(false); // Default to channel creation
            controller.setInitialVoice(initialVoice);

            // Create a transparent overlay stage
            Stage ownerStage = (Stage) serverPane.getScene().getWindow();
            Stage dialogStage = new Stage();
            dialogStage.initOwner(ownerStage);
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initStyle(StageStyle.TRANSPARENT);

            // Container for the dim effect and centering
            StackPane overlay = new StackPane();
            overlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.5);");

            // Limit dialog size to preferred size so it doesn't stretch
            if (root instanceof javafx.scene.layout.Region) {
                ((javafx.scene.layout.Region) root).setMaxSize(javafx.scene.layout.Region.USE_PREF_SIZE,
                        javafx.scene.layout.Region.USE_PREF_SIZE);
            }

            overlay.getChildren().add(root);

            // Close if clicked outside (on the overlay background)
            overlay.setOnMouseClicked(event -> {
                // Check if the click target is the overlay itself, not the dialog content
                // Also ensure it's a primary button click and it's not part of a drag operation
                if (event.getTarget() == overlay &&
                        event.getButton() == javafx.scene.input.MouseButton.PRIMARY &&
                        event.isStillSincePress()) {
                    dialogStage.close();
                }
            });

            // Ensure dialog itself consumes clicks so they don't propagate to overlay
            root.setOnMouseClicked(event -> event.consume());

            // Set scene with transparent fill
            Scene scene = new Scene(overlay);
            scene.setFill(Color.TRANSPARENT);
            dialogStage.setScene(scene);

            // Bind size to owner size to cover the whole window
            dialogStage.setX(ownerStage.getX());
            dialogStage.setY(ownerStage.getY());
            dialogStage.setWidth(ownerStage.getWidth());
            dialogStage.setHeight(ownerStage.getHeight());

            // Keep it resized with owner
            ownerStage.xProperty().addListener((obs, oldVal, newVal) -> dialogStage.setX(newVal.doubleValue()));
            ownerStage.yProperty().addListener((obs, oldVal, newVal) -> dialogStage.setY(newVal.doubleValue()));
            ownerStage.widthProperty().addListener((obs, oldVal, newVal) -> dialogStage.setWidth(newVal.doubleValue()));
            ownerStage.heightProperty()
                    .addListener((obs, oldVal, newVal) -> dialogStage.setHeight(newVal.doubleValue()));

            dialogStage.showAndWait();

            if (controller.isConfirmed()) {
                if (controller.isCategory()) {
                    createCategory(controller.getResultName(), sectionNode);
                } else {
                    if (controller.isVoice()) {
                        createVoiceChannel(controller.getResultName(), listNode, controller.getUserLimit());
                    } else {
                        createTextChannel(controller.getResultName(), listNode);
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAddVoiceSection(ActionEvent event) {
        showCreateChannelDialog(voiceChannelsSection, voiceChannelsList, true);
    }

    @FXML
    private void handleAddTextSection(ActionEvent event) {
        showCreateChannelDialog(textChannelsSection, textChannelsList, false);
    }

    @FXML
    private void handleAddFileSection(ActionEvent event) {
        showCreateChannelDialog(fileChannelsSection, fileChannelsList, false);
    }

    private void createCategory(String name, VBox siblingSection) {
        VBox newSection = new VBox(3.0);

        // Header
        HBox header = new HBox(5.0);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("channel-category-header");

        FontIcon arrow = new FontIcon("fas-chevron-down");
        arrow.getStyleClass().add("category-arrow");

        FontIcon icon = new FontIcon("fas-folder"); // Default icon
        icon.getStyleClass().add("category-icon");

        Label label = new Label(name.toUpperCase());
        label.getStyleClass().add("category-label");

        javafx.scene.layout.Pane spacer = new javafx.scene.layout.Pane();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Button addBtn = new Button();
        addBtn.getStyleClass().add("icon-button-tiny");
        FontIcon plusIcon = new FontIcon("fas-plus");
        plusIcon.getStyleClass().add("tiny-icon");
        addBtn.setGraphic(plusIcon);

        header.getChildren().addAll(arrow, icon, label, spacer, addBtn);

        // List container
        VBox listContainer = new VBox(2.0);

        newSection.getChildren().addAll(header, listContainer);

        // Add functionality
        // Toggle collapse
        header.setOnMouseClicked(e -> {
            if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                boolean isVisible = listContainer.isVisible();
                listContainer.setVisible(!isVisible);
                listContainer.setManaged(!isVisible);
                arrow.setIconLiteral(!isVisible ? "fas-chevron-down" : "fas-chevron-right");
            }
        });

        // Add button handler
        addBtn.setOnAction(e -> showCreateChannelDialog(newSection, listContainer, false));

        // Insert into sidebar
        int index = channelsContainer.getChildren().indexOf(siblingSection);
        if (index != -1) {
            channelsContainer.getChildren().add(index + 1, newSection);
        } else {
            channelsContainer.getChildren().add(newSection);
        }

        // Make reorderable
        setupReordering(channelsContainer);

        // Add Context Menu
        setupCategoryContextMenu(header, newSection);
    }

    private void setupCategoryContextMenu(Node target, VBox section) {
        target.setOnContextMenuRequested(e -> {
            showEditPopup(target, true, false, section);
            e.consume();
        });
    }

    private void handleDeleteCategory(VBox section) {
        // Check if it's the last category
        long categoryCount = channelsContainer.getChildren().stream()
                .filter(node -> node instanceof VBox)
                .count();

        if (categoryCount <= 1) {
            Alert alert = new Alert(AlertType.WARNING);
            alert.setTitle("Cannot Delete");
            alert.setHeaderText(null);
            alert.setContentText("You cannot delete the last category.");
            alert.showAndWait();
            return;
        }

        channelsContainer.getChildren().remove(section);
    }

    private void showEditPopup(Node target, boolean isCategory, boolean isVoice, VBox categorySection) {
        try {
            // Close existing popup if open
            if (currentEditPopup != null) {
                if (currentEditTarget == target && currentEditPopup.isShowing()) {
                    return; // Do nothing if clicking same target
                }
                currentEditPopup.hide();
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/EditChannelPopup.fxml"));
            Parent root = loader.load();
            EditChannelPopupController controller = loader.getController();

            // Extract current data
            String currentName = "";
            int currentLimit = 0;
            Label nameLabel = null;
            VBox limitIndicator = null;

            if (target instanceof HBox) {
                HBox row = (HBox) target;
                for (Node child : row.getChildren()) {
                    if (child instanceof Label && (child.getStyleClass().contains("category-label")
                            || child.getStyleClass().contains("channel-name"))) {
                        nameLabel = (Label) child;
                        currentName = nameLabel.getText();
                    }
                    if (child instanceof VBox) {
                        VBox vBox = (VBox) child;
                        if (!vBox.getChildren().isEmpty() && vBox.getChildren().get(0) instanceof Label) {
                            Label label = (Label) vBox.getChildren().get(0);
                            if (label.getStyleClass().contains("voice-user-count")) {
                                limitIndicator = vBox;
                                String text = label.getText(); // "0/12"
                                if (text.contains("/")) {
                                    try {
                                        currentLimit = Integer.parseInt(text.split("/")[1]);
                                    } catch (Exception e) {
                                    }
                                }
                            }
                        }
                    }
                }
            }

            controller.setData(currentName, isCategory, isVoice, currentLimit);

            // Disable delete if it's the last category of its type
            if (isCategory && categorySection != null && categorySection.getParent() instanceof VBox) {
                VBox parent = (VBox) categorySection.getParent();
                long categoryCount = parent.getChildren().stream()
                        .filter(node -> node instanceof VBox && !((VBox) node).getChildren().isEmpty()
                                && ((VBox) node).getChildren().get(0).getStyleClass()
                                        .contains("channel-category-header"))
                        .count();

                // Also check if we are deleting the specific section passed
                if (categoryCount <= 1) {
                    controller.setDeleteDisabled(true);
                }
            }

            Popup popup = new Popup();
            popup.getContent().add(root);
            popup.setAutoHide(true);
            popup.setOnHidden(e -> {
                currentEditPopup = null;
                currentEditTarget = null;
            });
            currentEditPopup = popup;
            currentEditTarget = target;

            // Calculate position
            javafx.geometry.Bounds bounds = target.localToScreen(target.getBoundsInLocal());
            popup.show(target, bounds.getMaxX() + 5, bounds.getMinY() - 2);

            // Callbacks
            Label finalNameLabel = nameLabel;
            VBox finalLimitIndicator = limitIndicator;

            controller.setOnSave(result -> {
                if (finalNameLabel != null) {
                    finalNameLabel.setText(result.name);
                    if (isCategory) {
                        finalNameLabel.setText(result.name.toUpperCase());
                    } else if (!isVoice) {
                        finalNameLabel.setText(result.name.toLowerCase().replace(" ", "-"));
                    }
                }

                if (isVoice && finalLimitIndicator != null) {
                    if (!finalLimitIndicator.getChildren().isEmpty()
                            && finalLimitIndicator.getChildren().get(0) instanceof Label) {
                        ((Label) finalLimitIndicator.getChildren().get(0)).setText("0/" + result.userLimit);
                    }
                    finalLimitIndicator.setVisible(result.userLimit > 0);
                }
                popup.hide();
            });

            controller.setOnDelete(() -> {
                if (isCategory) {
                    handleDeleteCategory(categorySection);
                } else {
                    if (target.getParent() instanceof VBox) {
                        ((VBox) target.getParent()).getChildren().remove(target);
                    }
                }
                popup.hide();
            });

            controller.setOnCancel(popup::hide);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createTextChannel(String name, VBox container) {
        HBox item = new HBox(8.0);
        item.setAlignment(Pos.CENTER_LEFT);
        item.getStyleClass().add("text-channel-item");

        FontIcon icon = new FontIcon("fas-comment");
        icon.getStyleClass().add("channel-icon");

        Label label = new Label(name.toLowerCase().replace(" ", "-"));
        label.getStyleClass().add("channel-name");

        javafx.scene.layout.Pane spacer = new javafx.scene.layout.Pane();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        item.getChildren().addAll(icon, label, spacer);

        item.setOnMouseClicked(e -> openTextChannel(label.getText(), "fas-comment", "Topic for " + label.getText()));

        container.getChildren().add(item);

        // Make reorderable
        setupReordering(container);

        // Add Context Menu
        setupChannelContextMenu(item, false);
    }

    private void setupChannelContextMenu(Node target, boolean isVoice) {
        target.setOnContextMenuRequested(e -> {
            showEditPopup(target, false, isVoice, null);
            e.consume();
        });
    }

    private void createVoiceChannel(String name, VBox container, int userLimit) {
        HBox item = new HBox(8.0);
        item.setAlignment(Pos.CENTER_LEFT);
        item.getStyleClass().add("voice-channel-item");

        FontIcon icon = new FontIcon("fas-volume-up");
        icon.getStyleClass().add("channel-icon");

        Label label = new Label(name);
        label.getStyleClass().add("channel-name");

        javafx.scene.layout.Pane spacer = new javafx.scene.layout.Pane();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        VBox limitIndicator = new VBox(1.0);
        limitIndicator.getStyleClass().add("voice-users-indicator");
        // Always visible if created with limit? Current UI hides it if 0?
        // Current UI: visible="false" initially.
        // Let's make it visible to show limit: "0/12"
        Label limitLabel = new Label("0/" + userLimit);
        limitLabel.getStyleClass().add("voice-user-count");
        limitIndicator.getChildren().add(limitLabel);
        limitIndicator.setVisible(true); // Show it

        item.getChildren().addAll(icon, label, spacer, limitIndicator);

        item.setOnMouseClicked(e -> joinVoiceChannel(name, "fas-volume-up"));

        container.getChildren().add(item);

        // Make reorderable
        setupReordering(container);

        // Add Context Menu
        setupChannelContextMenu(item, true);
    }

    // Right Sidebar - Users
    @FXML
    private VBox usersSidebar;
    @FXML
    private Label totalUsersCount;
    @FXML
    private Label onlineUsersCount;
    @FXML
    private Label offlineUsersCount;
    @FXML
    private VBox onlineUsersList;
    @FXML
    private VBox offlineUsersList;
    @FXML
    private VBox offlineUsersSection;

    // Data
    private String currentServerName = "";
    private Channel currentChannel = null;
    private List<User> serverUsers = new ArrayList<>();
    private List<Channel> voiceChannels = new ArrayList<>();
    private List<Channel> textChannels = new ArrayList<>();
    private boolean isUserMuted = false;
    private boolean isUserDeafened = false;
    private boolean offlineUsersExpanded = false;
    private boolean voiceChannelsExpanded = true;
    private boolean textChannelsExpanded = true;
    private boolean fileChannelsExpanded = true;
    private MainController mainController;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupChannelHandlers();
        setupChannelCategoryToggles();
        setupUserControls();

        // Setup Drag and Drop Reordering
        setupReordering(channelsContainer);
        setupReordering(voiceChannelsList);
        setupReordering(textChannelsList);
        setupReordering(fileChannelsList);

        setupReordering(fileChannelsList);

        // initializeMockData(); // Removed to prevent confusion with FXML data
        setupContextMenusForExistingNodes();
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

        // Update privacy badge (Mock logic for demonstration)
        if (serverPrivacyBadge != null) {
            boolean isPrivate = serverName.toLowerCase().contains("private")
                    || serverName.toLowerCase().contains("secure");
            serverPrivacyBadge.setText(isPrivate ? "PRIVATE" : "PUBLIC");
            serverPrivacyBadge.getStyleClass().removeAll("private", "public");
            serverPrivacyBadge.getStyleClass().add(isPrivate ? "private" : "public");
        }
    }

    private void setupChannelHandlers() {
        // Voice Channel Handlers
        voiceGeneral.setOnMouseClicked(event -> joinVoiceChannel("General", "fas-volume-up"));
        voiceMeeting.setOnMouseClicked(event -> joinVoiceChannel("Meeting Room", "fas-volume-up"));
        voicePrivate.setOnMouseClicked(event -> joinVoiceChannel("Private Discussion", "fas-lock"));

        // Text Channel Handlers
        textGeneral.setOnMouseClicked(
                event -> openTextChannel("general", "fas-hashtag", "General discussion for the team"));
        textAnnouncements.setOnMouseClicked(
                event -> openTextChannel("announcements", "fas-bullhorn", "Important announcements and updates"));
        textTeam.setOnMouseClicked(
                event -> openTextChannel("team-chat", "fas-hashtag", "Team coordination and planning"));
        textPrivate.setOnMouseClicked(event -> openTextChannel("private", "fas-lock", "Private team discussions"));

        // File Channel Handlers
        fileResources.setOnMouseClicked(event -> openTextChannel("resources", "fas-file-alt", "Shared resources"));
        fileProject.setOnMouseClicked(
                event -> openTextChannel("project-files", "fas-file-code", "Project files repository"));

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
            if (event.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                offlineUsersExpanded = !offlineUsersExpanded;
                offlineUsersList.setVisible(offlineUsersExpanded);
                offlineUsersList.setManaged(offlineUsersExpanded);

                // Update arrow icon
                HBox headerBox = (HBox) offlineHeader;
                FontIcon arrow = (FontIcon) headerBox.getChildren().get(0);
                arrow.setIconLiteral(offlineUsersExpanded ? "fas-chevron-down" : "fas-chevron-right");
            }
        });
    }

    private void setupChannelCategoryToggles() {
        // Voice Channels Toggle
        if (voiceChannelsSection != null && !voiceChannelsSection.getChildren().isEmpty()) {
            Node header = voiceChannelsSection.getChildren().get(0);
            header.setOnMouseClicked(event -> {
                if (event.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                    voiceChannelsExpanded = !voiceChannelsExpanded;
                    voiceChannelsList.setVisible(voiceChannelsExpanded);
                    voiceChannelsList.setManaged(voiceChannelsExpanded);

                    if (header instanceof HBox) {
                        HBox headerBox = (HBox) header;
                        if (!headerBox.getChildren().isEmpty() && headerBox.getChildren().get(0) instanceof FontIcon) {
                            FontIcon arrow = (FontIcon) headerBox.getChildren().get(0);
                            arrow.setIconLiteral(voiceChannelsExpanded ? "fas-chevron-down" : "fas-chevron-right");
                        }
                    }
                }
            });
        }

        // Text Channels Toggle
        if (textChannelsSection != null && !textChannelsSection.getChildren().isEmpty()) {
            Node header = textChannelsSection.getChildren().get(0);
            header.setOnMouseClicked(event -> {
                if (event.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                    textChannelsExpanded = !textChannelsExpanded;
                    textChannelsList.setVisible(textChannelsExpanded);
                    textChannelsList.setManaged(textChannelsExpanded);

                    if (header instanceof HBox) {
                        HBox headerBox = (HBox) header;
                        if (!headerBox.getChildren().isEmpty() && headerBox.getChildren().get(0) instanceof FontIcon) {
                            FontIcon arrow = (FontIcon) headerBox.getChildren().get(0);
                            arrow.setIconLiteral(textChannelsExpanded ? "fas-chevron-down" : "fas-chevron-right");
                        }
                    }
                }
            });
        }

        // File Channels Toggle
        if (fileChannelsSection != null && !fileChannelsSection.getChildren().isEmpty()) {
            Node header = fileChannelsSection.getChildren().get(0);
            header.setOnMouseClicked(event -> {
                if (event.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                    fileChannelsExpanded = !fileChannelsExpanded;
                    fileChannelsList.setVisible(fileChannelsExpanded);
                    fileChannelsList.setManaged(fileChannelsExpanded);

                    if (header instanceof HBox) {
                        HBox headerBox = (HBox) header;
                        if (!headerBox.getChildren().isEmpty() && headerBox.getChildren().get(0) instanceof FontIcon) {
                            FontIcon arrow = (FontIcon) headerBox.getChildren().get(0);
                            arrow.setIconLiteral(fileChannelsExpanded ? "fas-chevron-down" : "fas-chevron-right");
                        }
                    }
                }
            });
        }
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
            serverUsers.add(
                    new User(String.valueOf(i), "User" + i, "Offline", "Member", "", false, "Last seen 2 hours ago"));
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
            case "online":
                return "status-online";
            case "idle":
                return "status-idle";
            case "do not disturb":
                return "status-dnd";
            case "offline":
                return "status-offline";
            default:
                return "status-offline";
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
                "David Wilson: Great to have a secure space for our discussions");

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
        fileResources.getStyleClass().remove("channel-active");
        fileProject.getStyleClass().remove("channel-active");
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

        // Get StackPane from buttons
        StackPane headphonesStack = (StackPane) deafenBtn.getGraphic();
        StackPane voiceHeadphonesStack = (StackPane) toggleDeafenBtn.getGraphic();

        // Get slash icons (Shadow is index 1, Red Slash is index 2)
        Node slashShadow = headphonesStack.getChildren().get(1);
        Node slashIcon = headphonesStack.getChildren().get(2);

        Node voiceSlashShadow = voiceHeadphonesStack.getChildren().get(1);
        Node voiceSlashIcon = voiceHeadphonesStack.getChildren().get(2);

        if (isUserDeafened) {
            slashShadow.setVisible(true);
            slashIcon.setVisible(true);
            voiceSlashShadow.setVisible(true);
            voiceSlashIcon.setVisible(true);

            deafenBtn.getStyleClass().add("deafened");
            toggleDeafenBtn.getStyleClass().add("deafened");

            // If deafened, also mute
            if (!isUserMuted) {
                toggleMute();
            }
        } else {
            slashShadow.setVisible(false);
            slashIcon.setVisible(false);
            voiceSlashShadow.setVisible(false);
            voiceSlashIcon.setVisible(false);

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
    public void enterServer(String serverName, String serverIconLiteral, boolean isPrivate, boolean isCustom) {
        setServerInfo(serverName, serverIconLiteral);

        if (serverPrivacyBadge != null) {
            serverPrivacyBadge.setText(isPrivate ? "PRIVATE" : "PUBLIC");
            serverPrivacyBadge.getStyleClass().removeAll("private", "public");
            serverPrivacyBadge.getStyleClass().add(isPrivate ? "private" : "public");
        }

        if (isCustom) {
            // Clear all existing channels and sections
            channelsContainer.getChildren().clear();

            // Create one default category
            createCategory("General", null);
        }
    }

    private void setupContextMenusForExistingNodes() {
        // Setup for existing voice channels
        if (voiceChannelsList != null) {
            for (Node node : voiceChannelsList.getChildren()) {
                setupChannelContextMenu(node, true);
            }
        }
        // Setup for existing text channels
        if (textChannelsList != null) {
            for (Node node : textChannelsList.getChildren()) {
                setupChannelContextMenu(node, false);
            }
        }
        // Setup for existing file channels
        if (fileChannelsList != null) {
            for (Node node : fileChannelsList.getChildren()) {
                setupChannelContextMenu(node, false);
            }
        }

        // Setup for existing categories (headers)
        if (voiceChannelsSection != null && !voiceChannelsSection.getChildren().isEmpty()) {
            setupCategoryContextMenu(voiceChannelsSection.getChildren().get(0), voiceChannelsSection);
        }
        if (textChannelsSection != null && !textChannelsSection.getChildren().isEmpty()) {
            setupCategoryContextMenu(textChannelsSection.getChildren().get(0), textChannelsSection);
        }
        if (fileChannelsSection != null && !fileChannelsSection.getChildren().isEmpty()) {
            setupCategoryContextMenu(fileChannelsSection.getChildren().get(0), fileChannelsSection);
        }
    }

    // Method to set MainController reference
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
        if (mainController != null) {
            mainController.addStatusListener(this::onStatusChanged);
            updateCurrentUserDisplay();
        }
    }

    private void onStatusChanged(MainController.UserStatus status) {
        if (currentUserStatus != null) {
            // Map UserStatus enum to string text and style class
            String statusText = "Online";
            String styleClass = "status-online";

            switch (status) {
                case ONLINE:
                    statusText = "Online";
                    styleClass = "status-online";
                    break;
                case IDLE:
                    statusText = "Idle";
                    styleClass = "status-idle";
                    break;
                case DND:
                    statusText = "Do Not Disturb";
                    styleClass = "status-dnd";
                    break;
                case OFFLINE:
                    statusText = "Offline";
                    styleClass = "status-offline";
                    break;
            }

            String finalStatusText = statusText;
            String finalStyleClass = styleClass;

            // Ensure UI update happens on JavaFX Application Thread
            if (javafx.application.Platform.isFxApplicationThread()) {
                currentUserStatus.setText(finalStatusText);
                currentUserStatus.getStyleClass().removeAll("status-online", "status-idle", "status-dnd",
                        "status-offline");
                if (!currentUserStatus.getStyleClass().contains(finalStyleClass)) {
                    currentUserStatus.getStyleClass().add(finalStyleClass);
                }
            } else {
                javafx.application.Platform.runLater(() -> {
                    currentUserStatus.setText(finalStatusText);
                    currentUserStatus.getStyleClass().removeAll("status-online", "status-idle", "status-dnd",
                            "status-offline");
                    if (!currentUserStatus.getStyleClass().contains(finalStyleClass)) {
                        currentUserStatus.getStyleClass().add(finalStyleClass);
                    }
                });
            }
        }
    }

    private void updateCurrentUserDisplay() {
        if (mainController != null) {
            // Get data from UserSession which MainController also uses
            String username = UserSession.getInstance().getDisplayName();
            if (currentUserName != null) {
                currentUserName.setText(username);
            }

            // Get status from MainController
            MainController.UserStatus status = mainController.getUserStatus();
            onStatusChanged(status);
        }
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

    private void setupReordering(VBox container) {
        if (container == null)
            return;
        for (Node child : container.getChildren()) {
            makeDraggable(child, container);
        }

        // Listen for new items if list changes
        container.getChildren().addListener((javafx.collections.ListChangeListener<Node>) c -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    for (Node node : c.getAddedSubList()) {
                        makeDraggable(node, container);
                    }
                }
            }
        });
    }

    private void makeDraggable(Node node, VBox container) {
        node.setOnDragDetected(event -> {
            // Collapse if it's a section header container
            if (container == channelsContainer) {
                // Logic to force collapse will be handled by UI state listeners if needed,
                // but simpler approach: temporarily hide content if we were dragging headers
                // specifically.
                // However, `node` here IS the VBox section (e.g. voiceChannelsSection).
                // We can access its children to find the list and hide it.
                if (node instanceof VBox) {
                    VBox section = (VBox) node;
                    // Assuming standard structure: Header HBox is index 0, List VBox is index 1
                    if (section.getChildren().size() > 1 && section.getChildren().get(1) instanceof VBox) {
                        Node list = section.getChildren().get(1);
                        list.setVisible(false);
                        list.setManaged(false);
                    }
                }
            }

            Dragboard db = node.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString(String.valueOf(container.getChildren().indexOf(node)));
            db.setContent(content);

            SnapshotParameters params = new SnapshotParameters();
            params.setFill(Color.TRANSPARENT);
            db.setDragView(node.snapshot(params, null));

            event.consume();
        });

        node.setOnDragOver(event -> {
            if (event.getGestureSource() != node &&
                    event.getDragboard().hasString() &&
                    container.getChildren().contains(event.getGestureSource())) {

                event.acceptTransferModes(TransferMode.MOVE);

                double y = event.getY();
                double height = node.getLayoutBounds().getHeight();

                // Clear previous styles first
                node.getStyleClass().removeAll("drag-over-top", "drag-over-bottom");

                if (y < height / 2) {
                    if (!node.getStyleClass().contains("drag-over-top")) {
                        node.getStyleClass().add("drag-over-top");
                    }
                } else {
                    if (!node.getStyleClass().contains("drag-over-bottom")) {
                        node.getStyleClass().add("drag-over-bottom");
                    }
                }
            }
            event.consume();
        });

        node.setOnDragExited(event -> {
            node.getStyleClass().removeAll("drag-over-top", "drag-over-bottom");
            event.consume();
        });

        node.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasString()) {
                Node source = (Node) event.getGestureSource();
                // Ensure we are moving within the same container
                if (container.getChildren().contains(source)) {
                    double y = event.getY();
                    double height = node.getLayoutBounds().getHeight();

                    container.getChildren().remove(source);
                    // Find target index AFTER removing source (indices shift)
                    int targetIndex = container.getChildren().indexOf(node);

                    if (y < height / 2) {
                        // Insert before
                        container.getChildren().add(targetIndex, source);
                    } else {
                        // Insert after
                        container.getChildren().add(targetIndex + 1, source);
                    }
                    success = true;
                }
            }
            event.setDropCompleted(success);
            node.getStyleClass().removeAll("drag-over-top", "drag-over-bottom");
            event.consume();
        });

        node.setOnDragDone(event -> {
            // Restore visibility if it was a section
            if (container == channelsContainer) {
                Node source = (Node) event.getSource();
                if (source instanceof VBox) {
                    VBox section = (VBox) source;
                    if (section.getChildren().size() > 1 && section.getChildren().get(1) instanceof VBox) {
                        Node list = section.getChildren().get(1);

                        // Determine visibility based on the arrow icon in the header
                        boolean shouldBeVisible = false;
                        if (!section.getChildren().isEmpty() && section.getChildren().get(0) instanceof HBox) {
                            HBox header = (HBox) section.getChildren().get(0);
                            // Look for the arrow icon which is typically the first child
                            if (!header.getChildren().isEmpty() && header.getChildren().get(0) instanceof FontIcon) {
                                FontIcon arrow = (FontIcon) header.getChildren().get(0);
                                // If arrow is down, it should be visible
                                shouldBeVisible = "fas-chevron-down".equals(arrow.getIconLiteral());
                            }
                        }

                        list.setVisible(shouldBeVisible);
                        list.setManaged(shouldBeVisible);
                    }
                }
            }
            event.consume();
        });
    }
}
