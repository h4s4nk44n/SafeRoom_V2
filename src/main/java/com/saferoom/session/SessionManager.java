package com.saferoom.session;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.saferoom.gui.model.LiveSession;
import com.saferoom.gui.model.SessionType;

/**
 * SessionManager logic: Central controller for Saferoom Live Sessions.
 * Manages active sessions, notifies UI components, and handles P2P signalling
 * bridges.
 */
public class SessionManager {

    private static SessionManager instance;

    // Active Sessions: roomId -> LiveSession
    private final Map<String, LiveSession> activeSessions = new HashMap<>();

    // UI Properties: roomId -> BooleanProperty (is live?)
    private final Map<String, BooleanProperty> roomLiveIndicators = new HashMap<>();

    private final List<Consumer<LiveSession>> sessionStartListeners = new ArrayList<>();
    private final List<Consumer<LiveSession>> sessionEndListeners = new ArrayList<>();

    private SessionSignalingHandler signalingHandler;

    private SessionManager() {
        // Private constructor for Singleton
    }

    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    // --- Session Lifecycle ---

    public void startSession(LiveSession session) {
        if (session == null || session.getRoomId() == null)
            return;

        // Store session
        activeSessions.put(session.getRoomId(), session);

        // Update UI Property
        roomLiveIndicatorProperty(session.getRoomId()).set(true);

        // Notify Listeners
        notifySessionStart(session);

        System.out.println(
                "SessionManager: Session STARTED [" + session.getSessionId() + "] in Room: " + session.getRoomId());
    }

    public void endSession(String roomId) {
        LiveSession session = activeSessions.remove(roomId);
        if (session != null) {
            // Update UI Property
            if (roomLiveIndicators.containsKey(roomId)) {
                roomLiveIndicators.get(roomId).set(false);
            }

            // Notify Listeners
            notifySessionEnd(session);

            System.out.println("SessionManager: Session ENDED [" + session.getSessionId() + "] in Room: " + roomId);
        }
    }

    public LiveSession getSession(String roomId) {
        return activeSessions.get(roomId);
    }

    public boolean hasActiveSession(String roomId) {
        return activeSessions.containsKey(roomId);
    }

    public boolean hasActiveTerminalSession(String roomId) {
        LiveSession session = activeSessions.get(roomId);
        return session != null && session.getType() == SessionType.TERMINAL;
    }

    // --- UI Bindings ---

    public BooleanProperty roomLiveIndicatorProperty(String roomId) {
        return roomLiveIndicators.computeIfAbsent(roomId, k -> new SimpleBooleanProperty(false));
    }

    public Map<String, LiveSession> getActiveSessions() {
        return activeSessions;
    }

    // --- Listeners ---

    public void addSessionStartListener(Consumer<LiveSession> listener) {
        sessionStartListeners.add(listener);
    }

    public void addSessionEndListener(Consumer<LiveSession> listener) {
        sessionEndListeners.add(listener);
    }

    private void notifySessionStart(LiveSession session) {
        for (Consumer<LiveSession> listener : sessionStartListeners) {
            try {
                listener.accept(session);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void notifySessionEnd(LiveSession session) {
        for (Consumer<LiveSession> listener : sessionEndListeners) {
            try {
                listener.accept(session);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // --- P2P & Signaling Hooks ---

    public void setSignalingHandler(SessionSignalingHandler handler) {
        this.signalingHandler = handler;
    }

    public void requestAccess(String roomId, String sessionId, String requestorId) {
        // Logic to handle access request (e.g., from LiveSessionCell)
        System.out.println("SessionManager: Access requested by " + requestorId + " for session " + sessionId);
        // Default: Auto-approve for now or forward to owner
        if (signalingHandler != null) {
            // Forward to signaling handler if needed
        }
    }

    public void addViewer(String roomId, String viewerId) {
        LiveSession session = activeSessions.get(roomId);
        if (session != null) {
            session.addViewer(viewerId); // Assuming LiveSession has this method
            System.out.println("SessionManager: Added viewer " + viewerId + " to session " + session.getSessionId());
        }
    }

    public void onP2PConnectionEstablished(String roomId) {
        System.out.println("SessionManager: P2P Connection established for room " + roomId);
        // Can trigger specific UI updates here if needed
    }

    public void onSignalingMessageReceived(String senderId, String message) {
        // Handle incoming invisible signaling message
        System.out.println("SessionManager: Invisible signaling received from " + senderId + ": " + message);
        if (signalingHandler != null) {
            signalingHandler.handleSignalingMessage(senderId, message);
        }
    }
}
