package com.saferoom.gui.controller;

import com.jfoenix.controls.JFXButton;
import com.saferoom.client.ClientMenu;
import com.saferoom.gui.model.Friend;
import com.saferoom.gui.utils.UserSession;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import javafx.scene.layout.StackPane;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class FriendsController {

    @FXML private VBox friendsContainer;
    @FXML private HBox filterBar;
    @FXML private TextField searchField;
    @FXML private ListView<Map<String, Object>> searchResultsList;
    @FXML private JFXButton onlineFilterButton;
    @FXML private JFXButton allFilterButton;
    @FXML private JFXButton pendingFilterButton;
    @FXML private JFXButton blockedFilterButton;

    // Stat labels
    @FXML private Label onlineCountLabel;
    @FXML private Label totalCountLabel;
    @FXML private Label pendingCountLabel;

    private final ObservableList<Friend> allFriends = FXCollections.observableArrayList();
    private final ObservableList<Friend> pendingFriends = FXCollections.observableArrayList();
    private Timeline searchDebouncer;
    private Timeline friendsRefresher;
    private ListView<Friend> friendsListViewInstance;
    private static FriendsController currentInstance;

    public FriendsController() {
        currentInstance = this;
    }

    @FXML
    public void initialize() {
        setupSearchFunctionality();
        loadFriendsData();
        setupFilterButtons();
        setupAutoRefresh();

        // Set "All Friends" as default active
        loadAllFriends();
    }

    private void loadFriendsData() {
        CompletableFuture.supplyAsync(() -> {
            try {
                String currentUser = UserSession.getInstance().getDisplayName();

                com.saferoom.grpc.SafeRoomProto.FriendsListResponse friendsResponse =
                        ClientMenu.getFriendsList(currentUser);

                com.saferoom.grpc.SafeRoomProto.PendingRequestsResponse pendingResponse =
                        ClientMenu.getPendingFriendRequests(currentUser);

                if (friendsResponse.getSuccess()) {
                    allFriends.clear();
                    for (com.saferoom.grpc.SafeRoomProto.FriendInfo friendInfo : friendsResponse.getFriendsList()) {
                        String status = friendInfo.getIsOnline() ? "Online" : "Offline";
                        String lastSeen = friendInfo.getLastSeen().isEmpty() ? "Never" : friendInfo.getLastSeen();
                        String activity = friendInfo.getIsOnline() ? "Active now" : "Last seen " + lastSeen;

                        Friend friend = new Friend(
                                friendInfo.getUsername(),
                                status,
                                friendInfo.getIsOnline() ? "🟢" : "🔴",
                                activity
                        );
                        allFriends.add(friend);
                    }
                }

                if (pendingResponse.getSuccess()) {
                    pendingFriends.clear();
                    for (com.saferoom.grpc.SafeRoomProto.FriendRequestInfo requestInfo : pendingResponse.getRequestsList()) {
                        Friend pendingFriend = new Friend(
                                requestInfo.getSender(),
                                "Friend Request",
                                "Pending",
                                "Sent: " + requestInfo.getSentAt(),
                                requestInfo.getRequestId()
                        );
                        pendingFriends.add(pendingFriend);
                    }
                }

                return true;
            } catch (Exception e) {
                System.err.println("❌ Error loading friends data: " + e.getMessage());
                return false;
            }
        }).thenAcceptAsync(success -> {
            Platform.runLater(() -> {
                if (success) {
                    updateStatsLabels();
                    System.out.println("✅ Friends data loaded successfully");
                }
            });
        });
    }

    private void updateStatsLabels() {
        long onlineCount = allFriends.stream().filter(Friend::isOnline).count();

        if (onlineCountLabel != null) onlineCountLabel.setText(String.valueOf(onlineCount));
        if (totalCountLabel != null) totalCountLabel.setText(String.valueOf(allFriends.size()));
        if (pendingCountLabel != null) pendingCountLabel.setText(String.valueOf(pendingFriends.size()));
    }

    private void setupFilterButtons() {
        // Remove active class from all
        onlineFilterButton.setOnAction(e -> {
            setActiveFilter(onlineFilterButton);
            loadOnlineFriends();
        });

        allFilterButton.setOnAction(e -> {
            setActiveFilter(allFilterButton);
            loadAllFriends();
        });

        pendingFilterButton.setOnAction(e -> {
            setActiveFilter(pendingFilterButton);
            loadPendingRequests();
        });

        blockedFilterButton.setOnAction(e -> {
            setActiveFilter(blockedFilterButton);
            loadBlockedUsers();
        });
    }

    private void setActiveFilter(JFXButton activeButton) {
        onlineFilterButton.getStyleClass().remove("active-filter-pill");
        allFilterButton.getStyleClass().remove("active-filter-pill");
        pendingFilterButton.getStyleClass().remove("active-filter-pill");
        blockedFilterButton.getStyleClass().remove("active-filter-pill");

        activeButton.getStyleClass().add("active-filter-pill");
    }

    private void loadAllFriends() {
        updateFriendsList(allFriends, "ALL FRIENDS — " + allFriends.size(), false);
    }

    private void loadOnlineFriends() {
        ObservableList<Friend> onlineFriends = allFriends.stream()
                .filter(Friend::isOnline)
                .collect(Collectors.toCollection(FXCollections::observableArrayList));
        updateFriendsList(onlineFriends, "ONLINE — " + onlineFriends.size(), false);
    }

    private void loadPendingRequests() {
        updateFriendsList(pendingFriends, "PENDING REQUESTS — " + pendingFriends.size(), true);
    }

    private void loadBlockedUsers() {
        friendsContainer.getChildren().clear();

        Label headerLabel = new Label("BLOCKED USERS — 0");
        headerLabel.getStyleClass().add("friends-list-header-modern");

        VBox emptyState = createEmptyState("No blocked users", "You haven't blocked anyone yet");

        friendsContainer.getChildren().addAll(headerLabel, emptyState);
    }

    private void updateFriendsList(ObservableList<Friend> friends, String header, boolean isPending) {
        friendsContainer.getChildren().clear();

        // Add header
        Label headerLabel = new Label(header);
        headerLabel.getStyleClass().add("friends-list-header-modern");
        friendsContainer.getChildren().add(headerLabel);

        if (friends.isEmpty()) {
            VBox emptyState = createEmptyState(
                    "No friends yet",
                    "Add friends to start connecting"
            );
            friendsContainer.getChildren().add(emptyState);
            return;
        }

        // Create or reuse ListView
        if (this.friendsListViewInstance == null) {
            this.friendsListViewInstance = new ListView<>(friends);
            this.friendsListViewInstance.getStyleClass().add("friends-list-view");
            VBox.setVgrow(this.friendsListViewInstance, Priority.ALWAYS);
        } else {
            this.friendsListViewInstance.setItems(friends);
        }

        // Set cell factory
        this.friendsListViewInstance.setCellFactory(param ->
                isPending ? new PendingRequestCellModern() : new FriendCellModern()
        );

        friendsContainer.getChildren().add(this.friendsListViewInstance);
    }

    private VBox createEmptyState(String title, String subtitle) {
        VBox emptyState = new VBox(16);
        emptyState.getStyleClass().add("friends-empty-state");
        emptyState.setAlignment(Pos.CENTER);
        VBox.setVgrow(emptyState, Priority.ALWAYS);

        StackPane iconContainer = new StackPane();
        iconContainer.setAlignment(Pos.CENTER);

        FontIcon backgroundIcon = new FontIcon("fas-user-friends"); // Aynı ikon
        backgroundIcon.getStyleClass().add("empty-icon-background"); // Yeni stil sınıfı

        FontIcon foregroundIcon = new FontIcon("fas-user-friends");
        foregroundIcon.getStyleClass().add("empty-icon-foreground"); // Yeni stil sınıfı

        iconContainer.getChildren().addAll(backgroundIcon, foregroundIcon);


        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("empty-title-modern");

        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.getStyleClass().add("empty-subtitle-modern");

        // DÜZELTME: İkon yerine StackPane'i ekliyoruz
        emptyState.getChildren().addAll(iconContainer, titleLabel, subtitleLabel);

        return emptyState;
    }

    // ========== CELL FACTORIES ==========

    static class FriendCellModern extends ListCell<Friend> {
        private final HBox card = new HBox(14);
        private final StackPane avatarStack = new StackPane();
        private final Label avatar = new Label();
        private final Circle onlineDot = new Circle(6);
        private final VBox infoBox = new VBox(4);
        private final Label nameLabel = new Label();
        private final Label statusLabel = new Label();
        private final Pane spacer = new Pane();
        private final HBox actionButtons = new HBox(8);

        public FriendCellModern() {
            super();

            // Avatar setup
            avatar.getStyleClass().add("friend-avatar-modern");
            onlineDot.getStyleClass().add("online-dot-modern");
            StackPane.setAlignment(onlineDot, Pos.BOTTOM_RIGHT);
            StackPane.setMargin(onlineDot, new Insets(0, -2, -2, 0));
            avatarStack.getChildren().addAll(avatar, onlineDot);

            // Info setup
            nameLabel.getStyleClass().add("friend-name-modern");
            statusLabel.getStyleClass().add("friend-status-modern");
            infoBox.getChildren().addAll(nameLabel, statusLabel);

            // Action buttons
            FontIcon messageIcon = new FontIcon("fas-comment-dots");
            messageIcon.getStyleClass().add("action-icon-modern");
            HBox messageBtn = new HBox(messageIcon);
            messageBtn.getStyleClass().add("friend-action-modern");
            messageBtn.setAlignment(Pos.CENTER);

            FontIcon callIcon = new FontIcon("fas-phone");
            callIcon.getStyleClass().add("action-icon-modern");
            HBox callBtn = new HBox(callIcon);
            callBtn.getStyleClass().add("friend-action-modern");
            callBtn.setAlignment(Pos.CENTER);

            messageBtn.setOnMouseClicked(e -> {
                String friendName = nameLabel.getText();
                if (friendName != null && !friendName.isEmpty()) {
                    openMessagesWithUser(friendName);
                }
            });

            actionButtons.getChildren().addAll(messageBtn, callBtn);
            actionButtons.setAlignment(Pos.CENTER);

            HBox.setHgrow(spacer, Priority.ALWAYS);
            card.getChildren().addAll(avatarStack, infoBox, spacer, actionButtons);
            card.setAlignment(Pos.CENTER_LEFT);
            card.getStyleClass().add("friend-card-modern");

            // Card margin for spacing between items
            HBox.setMargin(card, new Insets(0, 0, 8, 0));
        }

        @Override
        protected void updateItem(Friend friend, boolean empty) {
            super.updateItem(friend, empty);

            if (empty || friend == null) {
                setGraphic(null);
                setText(null);
                setStyle("");
            } else {
                avatar.setText(friend.getAvatarChar());
                nameLabel.setText(friend.getName());
                statusLabel.setText(friend.getActivity());

                // Update online indicator
                if (friend.isOnline()) {
                    onlineDot.getStyleClass().setAll("online-dot-modern");
                } else {
                    onlineDot.getStyleClass().setAll("offline-dot-modern");
                }

                setGraphic(card);
            }
        }
    }

    static class PendingRequestCellModern extends ListCell<Friend> {
        private final HBox card = new HBox(14);
        private final Label avatar = new Label();
        private final VBox infoBox = new VBox(4);
        private final Label nameLabel = new Label();
        private final Label statusLabel = new Label();
        private final Pane spacer = new Pane();
        private final HBox actionButtons = new HBox(8);
        private final JFXButton acceptButton = new JFXButton("Accept");
        private final JFXButton rejectButton = new JFXButton("Reject");

        public PendingRequestCellModern() {
            super();

            avatar.getStyleClass().add("friend-avatar-modern");
            nameLabel.getStyleClass().add("friend-name-modern");
            statusLabel.getStyleClass().add("friend-status-modern");

            acceptButton.getStyleClass().add("pending-accept-btn-modern");
            rejectButton.getStyleClass().add("pending-reject-btn-modern");

            infoBox.getChildren().addAll(nameLabel, statusLabel);
            actionButtons.getChildren().addAll(acceptButton, rejectButton);
            actionButtons.setAlignment(Pos.CENTER);

            HBox.setHgrow(spacer, Priority.ALWAYS);
            card.getChildren().addAll(avatar, infoBox, spacer, actionButtons);
            card.setAlignment(Pos.CENTER_LEFT);
            card.getStyleClass().add("friend-card-modern");

            HBox.setMargin(card, new Insets(0, 0, 8, 0));
        }

        @Override
        protected void updateItem(Friend friend, boolean empty) {
            super.updateItem(friend, empty);

            if (empty || friend == null) {
                setGraphic(null);
                setText(null);
            } else {
                avatar.setText(friend.getAvatarChar());
                nameLabel.setText(friend.getName());
                statusLabel.setText(friend.getActivity());

                int requestId = friend.getRequestId();
                acceptButton.setOnAction(e -> {
                    acceptButton.setDisable(true);
                    rejectButton.setDisable(true);
                    acceptFriendRequest(requestId, friend.getName());
                });

                rejectButton.setOnAction(e -> {
                    acceptButton.setDisable(true);
                    rejectButton.setDisable(true);
                    rejectFriendRequest(requestId, friend.getName());
                });

                setGraphic(card);
            }
        }
    }

    // ========== SEARCH FUNCTIONALITY ==========

    private void setupSearchFunctionality() {
        searchDebouncer = new Timeline(new KeyFrame(Duration.millis(500), e -> performSearch()));

        searchField.textProperty().addListener((obs, oldText, newText) -> {
            searchDebouncer.stop();
            if (newText != null && newText.trim().length() >= 2) {
                searchDebouncer.play();
                searchResultsList.setVisible(true);
            } else {
                searchResultsList.getItems().clear();
                searchResultsList.setVisible(false);
            }
        });

        searchResultsList.setCellFactory(listView -> new SearchResultCell());

        searchField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) {
                Timeline hideDelay = new Timeline(new KeyFrame(Duration.millis(100), e -> {
                    if (!searchResultsList.isFocused()) {
                        searchResultsList.setVisible(false);
                    }
                }));
                hideDelay.play();
            }
        });
    }

    private void performSearch() {
        String searchTerm = searchField.getText().trim();
        if (searchTerm.length() < 2) return;

        CompletableFuture.supplyAsync(() -> {
            try {
                String currentUser = UserSession.getInstance().getDisplayName();
                return ClientMenu.searchUsers(searchTerm, currentUser);
            } catch (Exception e) {
                System.err.println("❌ Search error: " + e.getMessage());
                return Collections.<Map<String, Object>>emptyList();
            }
        }).thenAcceptAsync(results -> {
            Platform.runLater(() -> {
                searchResultsList.getItems().clear();
                searchResultsList.getItems().addAll(results);
            });
        });
    }

    static class SearchResultCell extends ListCell<Map<String, Object>> {
        private final HBox hbox = new HBox(12);
        private final Label avatar = new Label();
        private final VBox userInfo = new VBox(2);
        private final Label nameLabel = new Label();
        private final Label emailLabel = new Label();
        private final Pane spacer = new Pane();
        private final JFXButton addButton = new JFXButton("Add");

        public SearchResultCell() {
            super();

            avatar.getStyleClass().add("friend-avatar-modern");
            avatar.setMinSize(32, 32);
            avatar.setMaxSize(32, 32);
            avatar.setAlignment(Pos.CENTER);

            nameLabel.getStyleClass().add("search-result-name");
            emailLabel.getStyleClass().add("search-result-email");

            addButton.getStyleClass().add("search-add-button");

            userInfo.getChildren().addAll(nameLabel, emailLabel);
            HBox.setHgrow(spacer, Priority.ALWAYS);
            hbox.getChildren().addAll(avatar, userInfo, spacer, addButton);
            hbox.setAlignment(Pos.CENTER_LEFT);
            hbox.setPadding(new Insets(8));
        }

        @Override
        protected void updateItem(Map<String, Object> user, boolean empty) {
            super.updateItem(user, empty);

            if (empty || user == null) {
                setGraphic(null);
                setText(null);
            } else {
                String username = (String) user.get("username");
                String email = (String) user.get("email");
                Boolean isFriend = (Boolean) user.get("is_friend");
                Boolean hasPending = (Boolean) user.get("has_pending_request");

                nameLabel.setText(username);
                emailLabel.setText(email);
                avatar.setText(username.substring(0, 1).toUpperCase());

                if (isFriend != null && isFriend) {
                    addButton.setText("Friends");
                    addButton.setDisable(true);
                } else if (hasPending != null && hasPending) {
                    addButton.setText("Pending");
                    addButton.setDisable(true);
                } else {
                    addButton.setText("Add");
                    addButton.setDisable(false);
                    addButton.setOnAction(e -> sendFriendRequest(username));
                }

                setGraphic(hbox);
            }
        }
    }

    // ========== BACKEND ACTIONS ==========

    private static void sendFriendRequest(String username) {
        CompletableFuture.supplyAsync(() -> {
            try {
                String currentUser = UserSession.getInstance().getDisplayName();
                return ClientMenu.sendFriendRequest(currentUser, username);
            } catch (Exception e) {
                System.err.println("❌ Error sending friend request: " + e.getMessage());
                return null;
            }
        }).thenAcceptAsync(response -> {
            Platform.runLater(() -> {
                if (response != null && response.getSuccess()) {
                    System.out.println("✅ Friend request sent!");
                }
            });
        });
    }

    private static void acceptFriendRequest(int requestId, String senderUsername) {
        CompletableFuture.supplyAsync(() -> {
            try {
                String currentUser = UserSession.getInstance().getDisplayName();
                return ClientMenu.acceptFriendRequest(requestId, currentUser);
            } catch (Exception e) {
                System.err.println("❌ Error accepting friend request: " + e.getMessage());
                return null;
            }
        }).thenAcceptAsync(response -> {
            Platform.runLater(() -> {
                if (response != null && response.getCode() == 0) {
                    System.out.println("✅ Friend request accepted!");
                    FriendsController instance = getCurrentInstance();
                    if (instance != null) {
                        instance.loadFriendsData();
                    }
                }
            });
        });
    }

    private static void rejectFriendRequest(int requestId, String senderUsername) {
        CompletableFuture.supplyAsync(() -> {
            try {
                String currentUser = UserSession.getInstance().getDisplayName();
                return ClientMenu.rejectFriendRequest(requestId, currentUser);
            } catch (Exception e) {
                System.err.println("❌ Error rejecting friend request: " + e.getMessage());
                return null;
            }
        }).thenAcceptAsync(response -> {
            Platform.runLater(() -> {
                if (response != null && response.getCode() == 0) {
                    System.out.println("✅ Friend request rejected!");
                    FriendsController instance = getCurrentInstance();
                    if (instance != null) {
                        instance.loadFriendsData();
                    }
                }
            });
        });
    }

    private static void openMessagesWithUser(String username) {
        try {
            MainController mainController = MainController.getInstance();
            if (mainController != null) {
                mainController.handleMessages();
                MessagesController.openChatWithUser(username);
            }
        } catch (Exception e) {
            System.err.println("Error opening messages: " + e.getMessage());
        }
    }

    private void setupAutoRefresh() {
        friendsRefresher = new Timeline(new KeyFrame(Duration.seconds(30), e -> {
            System.out.println("🔄 Auto-refreshing friends list...");
            loadFriendsData();
        }));
        friendsRefresher.setCycleCount(Timeline.INDEFINITE);
        friendsRefresher.play();
    }

    private static FriendsController getCurrentInstance() {
        return currentInstance;
    }
}