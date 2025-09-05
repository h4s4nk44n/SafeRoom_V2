package com.saferoom.gui.model;

public class Message {
    private final String text;
    private final String senderId; // 'sentByMe' yerine gönderenin kimliği
    private final String senderAvatarChar; // Gönderenin avatarı

    public Message(String text, String senderId, String senderAvatarChar) {
        this.text = text;
        this.senderId = senderId;
        this.senderAvatarChar = senderAvatarChar;
    }

    public String getText() { return text; }
    public String getSenderId() { return senderId; }
    public String getSenderAvatarChar() { return senderAvatarChar; }
}