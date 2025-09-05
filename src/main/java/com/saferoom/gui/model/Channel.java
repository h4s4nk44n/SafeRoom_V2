package com.saferoom.gui.model;

public class Channel {
    private String id;
    private String name;
    private String type; // "voice" or "text"
    private boolean isPrivate;
    private String topic;
    private int userCount;
    private boolean hasNotifications;
    private String iconLiteral;

    public Channel(String id, String name, String type, boolean isPrivate) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.isPrivate = isPrivate;
        this.topic = "";
        this.userCount = 0;
        this.hasNotifications = false;
        this.iconLiteral = type.equals("voice") ? "fas-volume-up" : "fas-hashtag";
    }

    public Channel(String id, String name, String type, boolean isPrivate, String topic) {
        this(id, name, type, isPrivate);
        this.topic = topic;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
        // Update icon based on type
        if (type.equals("voice")) {
            this.iconLiteral = isPrivate ? "fas-lock" : "fas-volume-up";
        } else {
            this.iconLiteral = isPrivate ? "fas-lock" : "fas-hashtag";
        }
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public void setPrivate(boolean isPrivate) {
        this.isPrivate = isPrivate;
        // Update icon based on privacy
        if (type.equals("voice")) {
            this.iconLiteral = isPrivate ? "fas-lock" : "fas-volume-up";
        } else {
            this.iconLiteral = isPrivate ? "fas-lock" : "fas-hashtag";
        }
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public int getUserCount() {
        return userCount;
    }

    public void setUserCount(int userCount) {
        this.userCount = userCount;
    }

    public boolean hasNotifications() {
        return hasNotifications;
    }

    public void setHasNotifications(boolean hasNotifications) {
        this.hasNotifications = hasNotifications;
    }

    public String getIconLiteral() {
        return iconLiteral;
    }

    public void setIconLiteral(String iconLiteral) {
        this.iconLiteral = iconLiteral;
    }

    public boolean isVoiceChannel() {
        return "voice".equals(type);
    }

    public boolean isTextChannel() {
        return "text".equals(type);
    }

    @Override
    public String toString() {
        return "Channel{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", isPrivate=" + isPrivate +
                ", topic='" + topic + '\'' +
                ", userCount=" + userCount +
                ", hasNotifications=" + hasNotifications +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Channel channel = (Channel) obj;
        return id != null ? id.equals(channel.id) : channel.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
