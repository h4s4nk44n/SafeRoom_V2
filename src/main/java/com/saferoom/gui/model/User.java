package com.saferoom.gui.model;

public class User {
    private final String id;
    private final String name;
    private String username;
    private String status;
    private String role;
    private String roleIcon;
    private boolean isOnline;
    private String activity;

    // Original constructor for backward compatibility
    public User(String id, String name) {
        this.id = id;
        this.name = name;
        this.username = name;
        this.status = "Online";
        this.role = "Member";
        this.roleIcon = "";
        this.isOnline = true;
        this.activity = "";
    }

    // Extended constructor for server/chat functionality
    public User(String id, String username, String status, String role, String roleIcon, boolean isOnline, String activity) {
        this.id = id;
        this.name = username; // Keep name for backward compatibility
        this.username = username;
        this.status = status;
        this.role = role;
        this.roleIcon = roleIcon;
        this.isOnline = isOnline;
        this.activity = activity;
    }

    // Original getters for backward compatibility
    public String getId() { 
        return id; 
    }
    
    public String getName() { 
        return name; 
    }

    // Extended getters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getRoleIcon() {
        return roleIcon;
    }

    public void setRoleIcon(String roleIcon) {
        this.roleIcon = roleIcon;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        isOnline = online;
    }

    public String getActivity() {
        return activity;
    }

    public void setActivity(String activity) {
        this.activity = activity;
    }

    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", username='" + username + '\'' +
                ", status='" + status + '\'' +
                ", role='" + role + '\'' +
                ", isOnline=" + isOnline +
                ", activity='" + activity + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        User user = (User) obj;
        return id != null ? id.equals(user.id) : user.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}