package com.saferoom.gui.controller;

import com.saferoom.gui.model.Message;
import com.saferoom.gui.model.User;
import com.saferoom.gui.service.ChatService;
import com.saferoom.gui.view.cell.MessageCell;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class ChatViewController {

    @FXML private BorderPane chatPane;
    @FXML private Label chatPartnerAvatar;
    @FXML private Label chatPartnerName;
    @FXML private Label chatPartnerStatus;
    @FXML private ListView<Message> messageListView;
    @FXML private TextField messageInputField;
    @FXML private Button sendButton;
    @FXML private Button phoneButton;
    @FXML private Button videoButton;
    @FXML private HBox chatHeader;
    @FXML private VBox emptyChatPlaceholder;

    private User currentUser;
    private String currentChannelId;
    private ChatService chatService;
    private ObservableList<Message> messages;

    @FXML
    public void initialize() {
        this.currentUser = new User("currentUser123", "You");
        this.chatService = ChatService.getInstance();

        chatService.newMessageProperty().addListener((obs, oldMsg, newMsg) -> {
            if (newMsg != null && messages != null && messages.contains(newMsg)) {
                if (newMsg.getSenderId().equals(currentUser.getId())) {
                    messageListView.scrollTo(messages.size() - 1);
                }
            }
        });
        
        // Add Enter key handler for message input
        if (messageInputField != null) {
            messageInputField.setOnKeyPressed(this::handleKeyPressed);
        }
    }

    public void initChannel(String channelId) {
        this.currentChannelId = channelId;
        this.messages = chatService.getMessagesForChannel(channelId);

        messageListView.setItems(messages);
        messageListView.setCellFactory(param -> new MessageCell(currentUser.getId()));

        updatePlaceholderVisibility();

        messages.addListener((ListChangeListener<Message>) c -> {
            while (c.next()) {
                updatePlaceholderVisibility();
            }
        });

        if (!messages.isEmpty()) {
            messageListView.scrollTo(messages.size() - 1);
        }
        
        // Ensure Enter key handler is set (in case initialize didn't work)
        if (messageInputField != null) {
            messageInputField.setOnKeyPressed(this::handleKeyPressed);
            System.out.println("[ChatView] ⌨️ Enter key handler set for message input");
        }
    }

    public void setWidthConstraint(double width) {
        if (chatPane != null) {
            chatPane.setMaxWidth(width);
            chatPane.setPrefWidth(width);
        }
    }

    public void setHeaderVisible(boolean visible) {
        if (chatHeader != null) {
            chatHeader.setVisible(visible);
            chatHeader.setManaged(visible);
        }
    }

    public void setHeader(String name, String status, String avatarChar, boolean isGroupChat) {
        chatPartnerName.setText(name);
        chatPartnerStatus.setText(status);
        chatPartnerAvatar.setText(avatarChar);

        // Remove all existing status classes
        chatPartnerStatus.getStyleClass().removeAll("status-online", "status-offline", "status-idle", "status-dnd", "status-busy");

        // Apply appropriate status class based on status text
        String statusLower = status.toLowerCase();
        if (statusLower.contains("online")) {
            chatPartnerStatus.getStyleClass().add("status-online");
        } else if (statusLower.contains("idle") || statusLower.contains("away")) {
            chatPartnerStatus.getStyleClass().add("status-idle");
        } else if (statusLower.contains("busy") || statusLower.contains("dnd") || statusLower.contains("do not disturb")) {
            chatPartnerStatus.getStyleClass().add("status-dnd");
        } else {
            chatPartnerStatus.getStyleClass().add("status-offline");
        }

        phoneButton.setVisible(!isGroupChat);
        videoButton.setVisible(!isGroupChat);
    }

    @FXML
    private void handleSendMessage() {
        String text = messageInputField.getText();
        if (text == null || text.trim().isEmpty()) return;

        chatService.sendMessage(currentChannelId, text, currentUser);

        messageInputField.clear();
    }
    
    /**
     * Handle key presses in message input field
     */
    private void handleKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            // Check if Shift+Enter (for new line) or just Enter (for send)
            if (!event.isShiftDown()) {
                handleSendMessage();
                event.consume(); // Prevent default behavior
            }
            // If Shift+Enter, allow default behavior (new line)
        }
    }

    private void updatePlaceholderVisibility() {
        boolean isListEmpty = (messages == null || messages.isEmpty());
        if (emptyChatPlaceholder != null) {
            emptyChatPlaceholder.setVisible(isListEmpty);
            emptyChatPlaceholder.setManaged(isListEmpty);
        }
        messageListView.setVisible(!isListEmpty);
    }
}