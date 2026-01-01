package com.saferoom.gui.model;

import javafx.beans.property.IntegerProperty;

/**
 * Extended Message for live session context in the Contextual Workstation.
 * 
 * This message type is injected into the chat stream when a Host starts a
 * Temporary Session. It renders as a custom message block with:
 * - Session metadata (title, host)
 * - Eye icon (👁️) with real-time viewer count
 * - Permission-aware click handling
 */
public class LiveSessionMessage extends Message {

    private final LiveSession session;

    /**
     * Creates a live session message block.
     * 
     * @param session The associated live session
     */
    public LiveSessionMessage(LiveSession session) {
        super("", session.getHostId(), getAvatarChar(session.getHostName()));
        this.session = session;

        // Set appropriate message type based on session type
        setType(session.getType() == SessionType.TERMINAL
                ? MessageType.TERMINAL_SESSION
                : MessageType.LIVE_SESSION);
    }

    private static String getAvatarChar(String name) {
        return name != null && !name.isEmpty()
                ? String.valueOf(name.charAt(0)).toUpperCase()
                : "?";
    }

    /**
     * Gets the associated live session.
     */
    public LiveSession getSession() {
        return session;
    }

    /**
     * Gets the session ID for signaling purposes.
     */
    public String getSessionId() {
        return session.getSessionId();
    }

    /**
     * Gets the session title for display.
     */
    public String getSessionTitle() {
        return session.getTitle();
    }

    /**
     * Gets the host display name.
     */
    public String getHostName() {
        return session.getHostName();
    }

    /**
     * Gets the session type.
     */
    public SessionType getSessionType() {
        return session.getType();
    }

    /**
     * Observable property for real-time viewer count binding.
     */
    public IntegerProperty viewerCountProperty() {
        return session.viewerCountProperty();
    }

    /**
     * Gets current viewer count.
     */
    public int getViewerCount() {
        return session.getViewerCount();
    }

    /**
     * Checks if a user can join immediately (whitelisted).
     */
    public boolean canJoinImmediately(String userId) {
        return session.isWhitelisted(userId);
    }

    /**
     * Checks if the session is still active.
     */
    public boolean isSessionActive() {
        return session.isActive();
    }

    /**
     * Checks if this is a terminal session (triggers sidebar glow).
     */
    public boolean isTerminalSession() {
        return session.getType() == SessionType.TERMINAL;
    }
}
