package com.saferoom.oauth;

public class UserInfo {
    private String email;
    private String name;
    private String provider; // "Google" or "GitHub"
    private String avatarUrl;
    private String id;

    // Constructors
    public UserInfo() {}

    public UserInfo(String email, String name, String provider) {
        this.email = email;
        this.name = name;
        this.provider = provider;
    }

    // Getters and Setters
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "UserInfo{" +
                "email='" + email + '\'' +
                ", name='" + name + '\'' +
                ", provider='" + provider + '\'' +
                ", avatarUrl='" + avatarUrl + '\'' +
                ", id='" + id + '\'' +
                '}';
    }
}
