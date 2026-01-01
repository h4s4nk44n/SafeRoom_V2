package com.saferoom.gui.model;

/**
 * Represents the lifecycle state of a live P2P session.
 * Used by SessionManager to track and cleanup sessions.
 */
public enum SessionState {
    /**
     * Session is live and accepting viewer connections.
     * The Eye icon is active and clickable.
     */
    ACTIVE,

    /**
     * Host has temporarily paused the session.
     * Existing viewers remain connected but new connections are blocked.
     */
    PAUSED,

    /**
     * Session has been terminated.
     * Triggers zero-persistence cleanup of all transient signaling data.
     */
    ENDED
}
