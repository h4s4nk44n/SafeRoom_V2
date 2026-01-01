package com.saferoom.gui.model;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;

/**
 * Represents a live P2P session in the Contextual Workstation.
 * 
 * A session is created when a Host starts a Temporary Session (Screen Share,
 * Remote Control, or Terminal) and is injected into the chat stream as a
 * custom message block.
 * 
 * Key features:
 * - Permission-based access (whitelist for immediate join, request for others)
 * - Real-time viewer count tracking via observable properties
 * - Zero-persistence: session data is purged when ENDED
 */
public class LiveSession {

    private final String sessionId;
    private final String hostId;
    private final String hostName;
    private final String roomId;
    private final SessionType type;
    private final String title;
    private final LocalDateTime startTime;

    // Whitelist of user IDs who can join immediately without host approval
    private final Set<String> whitelist;

    // Currently connected viewer IDs
    private final Set<String> activeViewers;

    // Observable properties for UI binding
    private final ObjectProperty<SessionState> state;
    private final IntegerProperty viewerCount;

    /**
     * Creates a new live session.
     * 
     * @param hostId   User ID of the session host
     * @param hostName Display name of the host
     * @param roomId   Room/Sector where the session is active
     * @param type     Type of session (SCREEN_SHARE, REMOTE_CONTROL, TERMINAL)
     * @param title    Human-readable title (e.g., "UE5 Viewport")
     */
    public LiveSession(String hostId, String hostName, String roomId,
            SessionType type, String title) {
        this.sessionId = UUID.randomUUID().toString();
        this.hostId = Objects.requireNonNull(hostId, "hostId cannot be null");
        this.hostName = Objects.requireNonNull(hostName, "hostName cannot be null");
        this.roomId = Objects.requireNonNull(roomId, "roomId cannot be null");
        this.type = Objects.requireNonNull(type, "type cannot be null");
        this.title = title != null ? title : getDefaultTitle(type);
        this.startTime = LocalDateTime.now();
        this.whitelist = new HashSet<>();
        this.activeViewers = new HashSet<>();
        this.state = new SimpleObjectProperty<>(SessionState.ACTIVE);
        this.viewerCount = new SimpleIntegerProperty(0);
    }

    private String getDefaultTitle(SessionType type) {
        return switch (type) {
            case SCREEN_SHARE -> "Screen Share";
            case REMOTE_CONTROL -> "Remote Control";
            case TERMINAL -> "Terminal Session";
        };
    }

    // ==================== Getters ====================

    public String getSessionId() {
        return sessionId;
    }

    public String getHostId() {
        return hostId;
    }

    public String getHostName() {
        return hostName;
    }

    public String getRoomId() {
        return roomId;
    }

    public SessionType getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public Set<String> getWhitelist() {
        return Collections.unmodifiableSet(whitelist);
    }

    public Set<String> getActiveViewers() {
        return Collections.unmodifiableSet(activeViewers);
    }

    // ==================== State Property ====================

    public SessionState getState() {
        return state.get();
    }

    public void setState(SessionState newState) {
        state.set(newState);
    }

    public ObjectProperty<SessionState> stateProperty() {
        return state;
    }

    // ==================== Viewer Count Property ====================

    public int getViewerCount() {
        return viewerCount.get();
    }

    public IntegerProperty viewerCountProperty() {
        return viewerCount;
    }

    // ==================== Permission Methods ====================

    /**
     * Checks if a user is whitelisted for immediate access.
     * 
     * @param userId User ID to check
     * @return true if whitelisted, false if access request is required
     */
    public boolean isWhitelisted(String userId) {
        return whitelist.contains(userId);
    }

    /**
     * Adds a user to the whitelist for immediate access.
     * 
     * @param userId User ID to whitelist
     */
    public void addToWhitelist(String userId) {
        if (userId != null && !userId.equals(hostId)) {
            whitelist.add(userId);
        }
    }

    /**
     * Removes a user from the whitelist.
     * 
     * @param userId User ID to remove
     */
    public void removeFromWhitelist(String userId) {
        whitelist.remove(userId);
    }

    // ==================== Viewer Management ====================

    /**
     * Adds a viewer to the active viewers list and updates the count.
     * 
     * @param viewerId User ID of the viewer
     * @return true if added, false if already present
     */
    public boolean addViewer(String viewerId) {
        if (viewerId != null && !viewerId.equals(hostId) && activeViewers.add(viewerId)) {
            viewerCount.set(activeViewers.size());
            return true;
        }
        return false;
    }

    /**
     * Removes a viewer from the active viewers list and updates the count.
     * 
     * @param viewerId User ID of the viewer
     * @return true if removed, false if not present
     */
    public boolean removeViewer(String viewerId) {
        if (activeViewers.remove(viewerId)) {
            viewerCount.set(activeViewers.size());
            return true;
        }
        return false;
    }

    /**
     * Clears all viewers (called during session cleanup).
     */
    public void clearViewers() {
        activeViewers.clear();
        viewerCount.set(0);
    }

    // ==================== Session Lifecycle ====================

    /**
     * Checks if the session is currently active and accepting connections.
     */
    public boolean isActive() {
        return state.get() == SessionState.ACTIVE;
    }

    /**
     * Checks if the session has ended.
     */
    public boolean isEnded() {
        return state.get() == SessionState.ENDED;
    }

    /**
     * Ends the session and triggers cleanup.
     */
    public void endSession() {
        state.set(SessionState.ENDED);
        clearViewers();
        whitelist.clear();
    }

    // ==================== Object Methods ====================

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof LiveSession other))
            return false;
        return sessionId.equals(other.sessionId);
    }

    @Override
    public int hashCode() {
        return sessionId.hashCode();
    }

    @Override
    public String toString() {
        return "LiveSession{" +
                "sessionId='" + sessionId + '\'' +
                ", hostName='" + hostName + '\'' +
                ", type=" + type +
                ", title='" + title + '\'' +
                ", state=" + state.get() +
                ", viewers=" + viewerCount.get() +
                '}';
    }
}
