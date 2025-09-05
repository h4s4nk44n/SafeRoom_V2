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
            
            // Navigate to ServerView
            if (MainController.getInstance() != null) {
                MainController.getInstance().loadServerView(roomName, getRoomIcon(roomCard));
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
} 