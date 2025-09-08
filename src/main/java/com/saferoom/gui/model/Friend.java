package com.saferoom.gui.model;

// Arkadaş listesindeki her bir kişinin verilerini tutan sınıf
public class Friend {
    private final String name;
    private final String status;
    private final String activity;
    private final String activityType; // "Playing", "Listening to", "Writing" etc.
    private final int requestId; // For pending friend requests

    public Friend(String name, String status, String activityType, String activity) {
        this.name = name;
        this.status = status;
        this.activity = activity;
        this.activityType = activityType;
        this.requestId = -1; // Default for non-pending friends
    }
    
    // Constructor for pending friend requests
    public Friend(String name, String status, String activityType, String activity, int requestId) {
        this.name = name;
        this.status = status;
        this.activity = activity;
        this.activityType = activityType;
        this.requestId = requestId;
    }

    // Getters
    public String getName() { return name; }
    public String getStatus() { return status; }
    public String getActivity() { return activity; }
    public String getActivityType() { return activityType; }
    public int getRequestId() { return requestId; }
    public String getAvatarChar() { return name.substring(0, 1); }
    public boolean isOnline() { return status.toLowerCase().contains("online"); }
}
