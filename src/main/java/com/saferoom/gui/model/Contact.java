package com.saferoom.gui.model;

/**
 * Kişi listesindeki her bir elemanın verilerini tutan sınıf (Model).
 */
public class Contact {
    private final String name;
    private final String status;
    private final String lastMessage;
    private final String time;
    private final int unreadCount;

    public Contact(String name, String status, String lastMessage, String time, int unreadCount) {
        this.name = name;
        this.status = status;
        this.lastMessage = lastMessage;
        this.time = time;
        this.unreadCount = unreadCount;
    }

    // FXML ve Controller tarafından erişilecek olan Getter metodları
    public String getName() { return name; }
    public String getStatus() { return status; }
    public String getLastMessage() { return lastMessage; }
    public String getTime() { return time; }
    public int getUnreadCount() { return unreadCount; }
    public String getAvatarChar() { return name.substring(0, 1); }
    public boolean isOnline() { return status.toLowerCase().contains("online"); }
}
