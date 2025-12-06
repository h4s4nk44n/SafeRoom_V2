package com.saferoom.gui.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.util.ResourceBundle;

public class RoomsController implements Initializable {

    private static class RoomData {
        String name;
        boolean isPrivate;
        boolean isCustom;

        public RoomData(String name, boolean isPrivate, boolean isCustom) {
            this.name = name;
            this.isPrivate = isPrivate;
            this.isCustom = isCustom;
        }
    }

    @FXML
    private TextField searchTextField;

    @FXML
    private FlowPane hubListContainer;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Initialize search functionality for rooms
        searchTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterRooms(newValue);
        });

        // Setup room card click handlers
        setupRoomHandlers();
    }

    private void filterRooms(String searchText) {
        String lowerCaseSearchText = searchText.toLowerCase();

        if (hubListContainer != null) {
            for (Node node : hubListContainer.getChildren()) {
                if (node instanceof VBox && node.getStyleClass().contains("compact-room-card")) {
                    VBox roomCard = (VBox) node;
                    Label roomNameLabel = findRoomNameLabel(roomCard);

                    if (roomNameLabel != null) {
                        String roomName = roomNameLabel.getText().toLowerCase();
                        if (roomName.contains(lowerCaseSearchText) || searchText.isEmpty()) {
                            roomCard.setVisible(true);
                            roomCard.setManaged(true);
                        } else {
                            roomCard.setVisible(false);
                            roomCard.setManaged(false);
                        }
                    }
                }
            }
        }
    }

    private Label findRoomNameLabel(VBox roomCard) {
        // Look for the room name label in the compact room card
        for (Node node : roomCard.getChildren()) {
            if (node instanceof VBox && node.getStyleClass().contains("room-info-compact")) {
                VBox roomInfo = (VBox) node;
                for (Node infoChild : roomInfo.getChildren()) {
                    if (infoChild instanceof Label && infoChild.getStyleClass().contains("room-name-compact")) {
                        return (Label) infoChild;
                    }
                }
            }
        }
        return null;
    }

    private void setupRoomHandlers() {
        if (hubListContainer != null) {
            for (Node node : hubListContainer.getChildren()) {
                if (node instanceof VBox && node.getStyleClass().contains("compact-room-card")) {
                    VBox roomCard = (VBox) node;

                    // Setup click handler for room card to navigate to room
                    roomCard.setOnMouseClicked(event -> {
                        // Check if click was on action buttons, if so, don't navigate
                        if (!isActionButtonClick(event.getTarget())) {
                            navigateToRoom(roomCard);
                        }
                    });

                    // Setup handlers for action buttons
                    setupCompactActionButtons(roomCard);
                }
            }
        }
    }

    private boolean isActionButtonClick(Object target) {
        // Check if the click target is an action button or its child elements
        if (target instanceof Node) {
            Node node = (Node) target;
            while (node != null) {
                if (node.getStyleClass().contains("quick-action-btn")) {
                    return true;
                }
                node = node.getParent();
            }
        }
        return false;
    }

    private void navigateToRoom(VBox roomCard) {
        // Get room name for navigation
        Label roomNameLabel = findRoomNameLabel(roomCard);
        if (roomNameLabel != null) {
            String roomName = roomNameLabel.getText();
            System.out.println("Navigating to room: " + roomName);

            // Get room data if available
            boolean isPrivate = false;
            boolean isCustom = false;

            if (roomCard.getUserData() instanceof RoomData) {
                RoomData data = (RoomData) roomCard.getUserData();
                isPrivate = data.isPrivate;
                isCustom = data.isCustom;
            } else {
                // Check for mock data logic or assume default
                // For the hardcoded rooms in FXML, we can check names or just assume
                // public/mock
                if (roomName.contains("Defense") || roomName.contains("Sentry")) {
                    isPrivate = true;
                }
            }

            // Navigate to ServerView
            if (MainController.getInstance() != null) {
                MainController.getInstance().loadServerView(roomName, getRoomIcon(roomCard), isPrivate, isCustom);
            } else {
                System.err.println("MainController instance is null. Cannot load ServerView.");
            }
        }
    }

    private String getRoomIcon(VBox roomCard) {
        // Extract the icon from the room card to pass to the server view
        // Look for FontIcon in the room avatar
        for (Node node : roomCard.getChildren()) {
            if (node instanceof StackPane && node.getStyleClass().contains("room-avatar-large")) {
                StackPane avatar = (StackPane) node;
                for (Node avatarChild : avatar.getChildren()) {
                    if (avatarChild instanceof FontIcon) {
                        FontIcon icon = (FontIcon) avatarChild;
                        return icon.getIconLiteral();
                    }
                }
            }
        }
        return "fas-shield-alt"; // Default icon
    }

    private void setupCompactActionButtons(VBox roomCard) {
        // Find and setup action buttons in the room card
        for (Node node : roomCard.getChildren()) {
            if (node instanceof VBox && node.getStyleClass().contains("room-info-compact")) {
                VBox roomInfo = (VBox) node;
                for (Node infoChild : roomInfo.getChildren()) {
                    if (infoChild instanceof HBox && infoChild.getStyleClass().contains("quick-actions")) {
                        HBox actionsBox = (HBox) infoChild;
                        for (Node actionNode : actionsBox.getChildren()) {
                            if (actionNode instanceof Button) {
                                Button actionBtn = (Button) actionNode;
                                setupActionButtonHandler(actionBtn);
                            }
                        }
                    }
                }
            }
        }
    }

    private void setupActionButtonHandler(Button actionBtn) {
        actionBtn.setOnAction(event -> {
            if (actionBtn.getStyleClass().contains("voice-quick")) {
                System.out.println("Voice action clicked");
                // Add voice connection logic here
            } else if (actionBtn.getStyleClass().contains("chat-quick")) {
                System.out.println("Chat action clicked");
                // Add chat navigation logic here
            }
        });
    }

    @FXML
    private void handleCreateRoom() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/view/CreateRoomDialog.fxml"));
            javafx.scene.Parent root = loader.load();

            CreateRoomDialogController controller = loader.getController();

            // Create a transparent overlay stage
            javafx.stage.Stage ownerStage = (javafx.stage.Stage) searchTextField.getScene().getWindow();
            javafx.stage.Stage dialogStage = new javafx.stage.Stage();
            dialogStage.initOwner(ownerStage);
            dialogStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            dialogStage.initStyle(javafx.stage.StageStyle.TRANSPARENT);

            // Container for the dim effect and centering
            StackPane overlay = new StackPane();
            overlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.5);");

            // Limit dialog size to preferred size so it doesn't stretch
            if (root instanceof javafx.scene.layout.Region) {
                ((javafx.scene.layout.Region) root).setMaxSize(javafx.scene.layout.Region.USE_PREF_SIZE,
                        javafx.scene.layout.Region.USE_PREF_SIZE);
            }

            overlay.getChildren().add(root);

            // Close if clicked outside
            overlay.setOnMouseClicked(event -> {
                if (event.getTarget() == overlay &&
                        event.getButton() == javafx.scene.input.MouseButton.PRIMARY &&
                        event.isStillSincePress()) {
                    dialogStage.close();
                }
            });

            root.setOnMouseClicked(event -> event.consume());

            javafx.scene.Scene scene = new javafx.scene.Scene(overlay);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            dialogStage.setScene(scene);

            // Bind size to owner size
            dialogStage.setX(ownerStage.getX());
            dialogStage.setY(ownerStage.getY());
            dialogStage.setWidth(ownerStage.getWidth());
            dialogStage.setHeight(ownerStage.getHeight());

            ownerStage.xProperty().addListener((obs, oldVal, newVal) -> dialogStage.setX(newVal.doubleValue()));
            ownerStage.yProperty().addListener((obs, oldVal, newVal) -> dialogStage.setY(newVal.doubleValue()));
            ownerStage.widthProperty().addListener((obs, oldVal, newVal) -> dialogStage.setWidth(newVal.doubleValue()));
            ownerStage.heightProperty()
                    .addListener((obs, oldVal, newVal) -> dialogStage.setHeight(newVal.doubleValue()));

            dialogStage.showAndWait();

            if (controller.isConfirmed()) {
                String roomName = controller.getResultName();
                boolean isPrivate = controller.isPrivate();
                javafx.scene.image.Image roomImage = controller.getResultImage();

                System.out.println("Creating new room: " + roomName + ", Private: " + isPrivate);

                // Add the new room card to the UI
                addRoomCard(roomName, isPrivate, roomImage);
            }

        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    private void addRoomCard(String roomName, boolean isPrivate, javafx.scene.image.Image roomImage) {
        if (hubListContainer == null)
            return;

        VBox roomCard = new VBox();
        roomCard.getStyleClass().add("compact-room-card");

        // Avatar
        StackPane avatar = new StackPane();
        avatar.getStyleClass().add("room-avatar-large");

        if (roomImage != null) {
            javafx.scene.shape.Circle imageCircle = new javafx.scene.shape.Circle(24);
            imageCircle.setFill(new javafx.scene.paint.ImagePattern(roomImage));
            avatar.getChildren().add(imageCircle);
        } else {
            FontIcon icon = new FontIcon("fas-rocket");
            icon.getStyleClass().add("room-icon-large");
            avatar.getChildren().add(icon);
        }

        // Room Info
        VBox roomInfo = new VBox(4);
        roomInfo.getStyleClass().add("room-info-compact");
        roomInfo.setAlignment(javafx.geometry.Pos.CENTER);

        Label nameLabel = new Label(roomName);
        nameLabel.getStyleClass().add("room-name-compact");

        // Stats (Mock data for new room)
        HBox onlineStat = new HBox(4);
        onlineStat.setAlignment(javafx.geometry.Pos.CENTER);
        onlineStat.getChildren().addAll(new FontIcon("fas-circle"), new Label("1"));
        ((FontIcon) onlineStat.getChildren().get(0)).getStyleClass().add("online-indicator");
        ((Label) onlineStat.getChildren().get(1)).getStyleClass().add("stat-number");

        HBox totalStat = new HBox(4);
        totalStat.setAlignment(javafx.geometry.Pos.CENTER);
        totalStat.getChildren().addAll(new FontIcon("fas-users"), new Label("1"));
        ((FontIcon) totalStat.getChildren().get(0)).getStyleClass().add("total-indicator");
        ((Label) totalStat.getChildren().get(1)).getStyleClass().add("stat-number");

        HBox stats = new HBox(8);
        stats.getStyleClass().add("room-stats");
        stats.setAlignment(javafx.geometry.Pos.CENTER);
        stats.getChildren().addAll(onlineStat, totalStat);

        // Quick Actions
        HBox actions = new HBox(8);
        actions.getStyleClass().add("quick-actions");
        actions.setAlignment(javafx.geometry.Pos.CENTER);

        Button voiceBtn = new Button();
        voiceBtn.getStyleClass().addAll("quick-action-btn", "voice-quick");
        voiceBtn.setGraphic(new FontIcon("fas-microphone"));
        ((FontIcon) voiceBtn.getGraphic()).getStyleClass().add("quick-action-icon");

        Button chatBtn = new Button();
        chatBtn.getStyleClass().addAll("quick-action-btn", "chat-quick");
        chatBtn.setGraphic(new FontIcon("fas-comments"));
        ((FontIcon) chatBtn.getGraphic()).getStyleClass().add("quick-action-icon");

        actions.getChildren().addAll(voiceBtn, chatBtn);

        // Activity Indicators
        VBox activity = new VBox(2);
        activity.getStyleClass().add("activity-indicators");
        activity.setAlignment(javafx.geometry.Pos.CENTER);

        HBox voiceActivity = new HBox(4);
        voiceActivity.setAlignment(javafx.geometry.Pos.CENTER);
        voiceActivity.getChildren().addAll(new FontIcon("fas-volume-up"), new Label("0 in voice"));
        ((FontIcon) voiceActivity.getChildren().get(0)).getStyleClass().add("activity-icon");
        ((Label) voiceActivity.getChildren().get(1)).getStyleClass().add("activity-text");

        HBox msgActivity = new HBox(4);
        msgActivity.setAlignment(javafx.geometry.Pos.CENTER);
        msgActivity.getChildren().addAll(new FontIcon("fas-envelope"), new Label("No new messages"));
        ((FontIcon) msgActivity.getChildren().get(0)).getStyleClass().add("activity-icon");
        ((Label) msgActivity.getChildren().get(1)).getStyleClass().add("activity-text");

        activity.getChildren().addAll(voiceActivity, msgActivity);

        roomInfo.getChildren().addAll(nameLabel, stats, actions, activity);
        roomCard.getChildren().addAll(avatar, roomInfo);

        // Add click handlers
        roomCard.setOnMouseClicked(event -> {
            if (!isActionButtonClick(event.getTarget())) {
                navigateToRoom(roomCard);
            }
        });
        setupActionButtonHandler(voiceBtn);
        setupActionButtonHandler(chatBtn);

        // Store room data
        roomCard.setUserData(new RoomData(roomName, isPrivate, true));

        hubListContainer.getChildren().add(0, roomCard); // Add to top
    }
}