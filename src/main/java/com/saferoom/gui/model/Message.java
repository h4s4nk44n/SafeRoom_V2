package com.saferoom.gui.model;

import java.util.Objects;
import java.util.UUID;

import javafx.beans.property.*;
import javafx.scene.image.Image;

public class Message {
    private final String id = UUID.randomUUID().toString();
    private final String senderId;
    private final String senderAvatarChar;

    private final StringProperty text = new SimpleStringProperty("");
    private final ObjectProperty<MessageType> type = new SimpleObjectProperty<>(MessageType.TEXT);
    private final ObjectProperty<FileAttachment> attachment = new SimpleObjectProperty<>();
    private final DoubleProperty progress = new SimpleDoubleProperty(0);
    private final StringProperty statusText = new SimpleStringProperty("");
    private final BooleanProperty outgoing = new SimpleBooleanProperty(false);
    private final LongProperty transferId = new SimpleLongProperty(-1);

    public Message(String text, String senderId, String senderAvatarChar) {
        this.senderId = senderId;
        this.senderAvatarChar = senderAvatarChar;
        this.text.set(text);
        this.statusText.set("");
    }

    public static Message createFilePlaceholder(String senderId,
                                                String senderAvatarChar,
                                                FileAttachment attachment) {
        Message msg = new Message("", senderId, senderAvatarChar);
        msg.type.set(MessageType.FILE_PLACEHOLDER);
        msg.attachment.set(attachment);
        msg.statusText.set("Sending…");
        msg.progress.set(0);
        return msg;
    }

    public String getId() {
        return id;
    }

    public String getText() {
        return text.get();
    }

    public void setText(String value) {
        text.set(value);
    }

    public StringProperty textProperty() {
        return text;
    }

    public MessageType getType() {
        return type.get();
    }

    public void setType(MessageType newType) {
        type.set(newType);
    }

    public ObjectProperty<MessageType> typeProperty() {
        return type;
    }

    public FileAttachment getAttachment() {
        return attachment.get();
    }

    public void setAttachment(FileAttachment attachment) {
        this.attachment.set(attachment);
    }

    public ObjectProperty<FileAttachment> attachmentProperty() {
        return attachment;
    }

    public double getProgress() {
        return progress.get();
    }

    public void setProgress(double value) {
        progress.set(Math.max(0, Math.min(1, value)));
    }

    public DoubleProperty progressProperty() {
        return progress;
    }

    public String getStatusText() {
        return statusText.get();
    }

    public void setStatusText(String value) {
        statusText.set(value);
    }

    public StringProperty statusTextProperty() {
        return statusText;
    }

    public boolean isOutgoing() {
        return outgoing.get();
    }

    public void setOutgoing(boolean value) {
        outgoing.set(value);
    }

    public BooleanProperty outgoingProperty() {
        return outgoing;
    }

    public long getTransferId() {
        return transferId.get();
    }

    public void setTransferId(long id) {
        transferId.set(id);
    }

    public LongProperty transferIdProperty() {
        return transferId;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getSenderAvatarChar() {
        return senderAvatarChar;
    }

    public void setThumbnail(Image image) {
        FileAttachment current = attachment.get();
        if (current == null) {
            return;
        }
        FileAttachment updated = new FileAttachment(
            current.getTargetType(),
            current.getFileName(),
            current.getFileSizeBytes(),
            current.getLocalPath(),
            image
        );
        attachment.set(updated);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Message other)) return false;
        return id.equals(other.id);
    }
}