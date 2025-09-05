// ✅ FriendsController.java
package com.saferoom.gui.controller;

import com.jfoenix.controls.JFXButton;
import com.saferoom.gui.model.Friend;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.stream.Collectors;

public class FriendsController {

    @FXML private VBox friendsContainer;
    @FXML private HBox filterBar;
    @FXML private Label totalFriendsLabel;
    @FXML private Label onlineFriendsLabel;
    @FXML private Label pendingFriendsLabel;

    private final ObservableList<Friend> allFriends = FXCollections.observableArrayList();
    private final ObservableList<Friend> pendingFriends = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
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
}