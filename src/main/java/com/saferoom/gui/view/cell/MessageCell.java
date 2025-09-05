package com.saferoom.gui.view.cell;

import com.saferoom.gui.model.Message;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;

public class MessageCell extends ListCell<Message> {
    private final HBox hbox = new HBox(10);
    private final Label avatar = new Label();
    private final Label messageText = new Label();
    private final Pane spacer = new Pane();
    private final String currentUserId;

    public MessageCell(String currentUserId) {
        super();
        this.currentUserId = currentUserId;

        avatar.getStyleClass().add("message-avatar");
        messageText.getStyleClass().add("message-bubble");
        messageText.setWrapText(true);
        HBox.setHgrow(spacer, Priority.ALWAYS);

        listViewProperty().addListener((obs, oldListView, newListView) -> {
            if (newListView != null) {
                messageText.maxWidthProperty().bind(newListView.widthProperty().multiply(0.70));
            }
        });
    }

    @Override
    protected void updateItem(Message message, boolean empty) {
        super.updateItem(message, empty);
        if (empty || message == null || currentUserId == null) {
            setGraphic(null);
        } else {
            messageText.setText(message.getText());
            avatar.setText(message.getSenderAvatarChar());

            final boolean isSentByMe = message.getSenderId().equals(currentUserId);

            if (isSentByMe) {
                hbox.getChildren().setAll(spacer, messageText, avatar);
                messageText.getStyleClass().setAll("message-bubble", "sent");
            } else {
                hbox.getChildren().setAll(avatar, messageText, spacer);
                messageText.getStyleClass().setAll("message-bubble", "received");
            }
            setGraphic(hbox);
        }
    }
}