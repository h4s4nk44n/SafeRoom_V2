// ✅ FriendsController.java
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
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class FriendsController {

    @FXML private VBox friendsContainer;
    @FXML private HBox filterBar;
    @FXML private Label totalFriendsLabel;
    @FXML private Label onlineFriendsLabel;
    @FXML private Label pendingFriendsLabel;
    @FXML private TextField searchField;
    @FXML private ListView<Map<String, Object>> searchResultsList;

    private final ObservableList<Friend> allFriends = FXCollections.observableArrayList();
    private final ObservableList<Friend> pendingFriends = FXCollections.observableArrayList();
    private Timeline searchDebouncer;

    @FXML
    public void initialize() {
        // Search functionality setup
        setupSearchFunctionality();
        
        // Örnek veriler
        allFriends.addAll(
                new Friend("John Doe", "Online - Active 2 mins ago", "Playing", "VALORANT"),
                new Friend("Jane Smith", "Online - Active now", "Writing", "code"),
                new Friend("Mike Johnson", "Online - Active 5 mins ago", "Listening to", "Spotify"),
                new Friend("Emily White", "Offline", "Last online 3 hours ago", "")
        );
        pendingFriends.addAll(
                new Friend("Alex Green", "Pending", "", "")
        );

        ObservableList<Friend> onlineFriends = allFriends.stream()
                .filter(Friend::isOnline)
                .collect(Collectors.toCollection(FXCollections::observableArrayList));

        totalFriendsLabel.setText(String.valueOf(allFriends.size()));
        onlineFriendsLabel.setText(String.valueOf(onlineFriends.size()));
        pendingFriendsLabel.setText(String.valueOf(pendingFriends.size()));

        for (Node node : filterBar.getChildren()) {
            JFXButton button = (JFXButton) node;
            button.setOnAction(event -> {
                filterBar.getChildren().forEach(btn -> btn.getStyleClass().remove("active"));
                button.getStyleClass().add("active");

                switch (button.getText()) {
                    case "Online":
                        updateFriendsList(onlineFriends, "Online");
                        break;
                    case "All":
                        updateFriendsList(allFriends, "All Friends");
                        break;
                    case "Pending":
                        updateFriendsList(pendingFriends, "Pending");
                        break;
                    default:
                        friendsContainer.getChildren().clear();
                        break;
                }
            });
        }

        filterBar.getChildren().get(0).getStyleClass().add("active");
        updateFriendsList(onlineFriends, "Online");
    }

    private void setupSearchFunctionality() {
        // Debouncing - 500ms bekle, sonra ara
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
        
        // Search results cell factory
        searchResultsList.setCellFactory(listView -> new SearchResultCell());
        
        // Başka yere tıklayınca search results'ı gizle
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
        
        // Background thread'de arama yap
        CompletableFuture.supplyAsync(() -> {
            try {
                String currentUser = UserSession.getInstance().getDisplayName();
                return ClientMenu.searchUsers(searchTerm, currentUser);
            } catch (Exception e) {
                System.err.println("Search error: " + e.getMessage());
                return Collections.<Map<String, Object>>emptyList();
            }
        }).thenAcceptAsync(results -> {
            Platform.runLater(() -> {
                searchResultsList.getItems().clear();
                searchResultsList.getItems().addAll(results);
            });
        });
    }

    private void updateFriendsList(ObservableList<Friend> friends, String header) {
        friendsContainer.getChildren().clear();
        if (friends != null && !friends.isEmpty()) {
            ListView<Friend> listView = new ListView<>(friends);
            listView.getStyleClass().add("friends-list-view");
            listView.setCellFactory(param -> new FriendCell());
            VBox.setVgrow(listView, Priority.ALWAYS);
            friendsContainer.getChildren().add(listView);
        }
    }

    // ✅ Boş hücrelerde hover engellendi ve görünüm optimize edildi
    static class FriendCell extends ListCell<Friend> {
        private final HBox hbox = new HBox(15);
        private final Label avatar = new Label();
        private final VBox infoBox = new VBox(2);
        private final Label nameLabel = new Label();
        private final Label statusLabel = new Label();
        private final HBox activityBox = new HBox(5);
        private final Label activityTypeLabel = new Label();
        private final Label activityLabel = new Label();
        private final Pane spacer = new Pane();
        private final HBox actionButtons = new HBox(10);

        public FriendCell() {
            super();
            avatar.getStyleClass().add("friend-avatar");
            nameLabel.getStyleClass().add("friend-name");
            statusLabel.getStyleClass().add("friend-status");
            activityTypeLabel.getStyleClass().add("friend-activity-type");
            activityLabel.getStyleClass().add("friend-activity");

            FontIcon messageIcon = new FontIcon("fas-comment-dots");
            FontIcon callIcon = new FontIcon("fas-phone");
            messageIcon.getStyleClass().add("action-icon");
            callIcon.getStyleClass().add("action-icon");

            actionButtons.getChildren().addAll(messageIcon, callIcon);
            actionButtons.setAlignment(Pos.CENTER);

            activityBox.getChildren().addAll(activityTypeLabel, activityLabel);
            infoBox.getChildren().addAll(nameLabel, statusLabel, activityBox);

            HBox.setHgrow(spacer, Priority.ALWAYS);
            hbox.getChildren().addAll(avatar, infoBox, spacer, actionButtons);
            hbox.setAlignment(Pos.CENTER_LEFT);
        }

        @Override
        protected void updateItem(Friend friend, boolean empty) {
            super.updateItem(friend, empty);
            if (empty || friend == null) {
                setGraphic(null);
                setText(null);
                setStyle("");
                setDisable(true);
                setMouseTransparent(true);
                setOnMouseEntered(null);
                setOnMouseExited(null);
            } else {
                avatar.setText(friend.getAvatarChar());
                nameLabel.setText(friend.getName());
                statusLabel.setText(friend.getStatus());
                activityTypeLabel.setText(friend.getActivityType());
                activityLabel.setText(friend.getActivity());
                activityBox.setVisible(!friend.getActivity().isEmpty());

                setGraphic(hbox);
                setDisable(false);
                setMouseTransparent(false);
            }
        }
    }

    // Search result cell for user search
    static class SearchResultCell extends ListCell<Map<String, Object>> {
        private final HBox hbox = new HBox(10);
        private final Circle avatar = new Circle(15);
        private final VBox userInfo = new VBox(2);
        private final Label nameLabel = new Label();
        private final Label emailLabel = new Label();
        private final Pane spacer = new Pane();
        private final JFXButton addButton = new JFXButton("Add Friend");

        public SearchResultCell() {
            super();
            avatar.setFill(Color.LIGHTBLUE);
            nameLabel.getStyleClass().add("search-result-name");
            emailLabel.getStyleClass().add("search-result-email");
            addButton.getStyleClass().add("search-add-button");
            
            userInfo.getChildren().addAll(nameLabel, emailLabel);
            HBox.setHgrow(spacer, Priority.ALWAYS);
            hbox.getChildren().addAll(avatar, userInfo, spacer, addButton);
            hbox.setAlignment(Pos.CENTER_LEFT);
            hbox.setPadding(new Insets(5));
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
                
                nameLabel.setText(username);
                emailLabel.setText(email);
                
                // Avatar letter
                if (username != null && !username.isEmpty()) {
                    // Create a label for avatar text instead of circle
                    Label avatarText = new Label(username.substring(0, 1).toUpperCase());
                    avatarText.getStyleClass().add("search-avatar");
                    avatarText.setMinSize(30, 30);
                    avatarText.setMaxSize(30, 30);
                    avatarText.setAlignment(Pos.CENTER);
                    
                    // Replace circle with label in hbox
                    hbox.getChildren().set(0, avatarText);
                }
                
                addButton.setOnAction(e -> sendFriendRequest(username));
                setGraphic(hbox);
            }
        }
    }

    private static void sendFriendRequest(String username) {
        System.out.println("Sending friend request to: " + username);
        // TODO: Implement friend request logic
    }
}