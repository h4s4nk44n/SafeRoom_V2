package com.saferoom.session;

import com.saferoom.gui.model.LiveSession;
import com.saferoom.p2p.P2PConnectionManager;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bridge between SessionManager and P2PConnectionManager for WebRTC signaling.
 * 
 * Implements the "Invisible Signaling" pattern:
 * - All WebRTC signals (ICE candidates, offers, answers) are embedded in the
 * chat stream
 * as non-rendering SIGNALING_METADATA message packets
 * - Maintains Zero-Cloud Relay philosophy
 * - Triggers zero-persistence cleanup after P2P connection is established
 * 
 * This handler is registered with SessionManager and coordinates:
 * 1. Viewer connection initiation
 * 2. ICE candidate exchange via chat stream
 * 3. Signaling metadata cleanup after connection
 */
public class SessionSignalingHandler {

    private final SessionManager sessionManager;
    private final P2PConnectionManager p2pManager;

    // ==================== Signaling State ====================

    // Map: SessionId -> Set of pending signaling message IDs (for cleanup)
    private final Map<String, Set<String>> signalingMessages;

    // Map: SessionId -> Map of ViewerId -> ConnectionState
    private final Map<String, Map<String, ConnectionState>> connectionStates;

    // Callback for injecting signaling messages into chat (invisible)
    private SignalingMessageCallback messageCallback;

    public SessionSignalingHandler(SessionManager sessionManager, P2PConnectionManager p2pManager) {
        this.sessionManager = sessionManager;
        this.p2pManager = p2pManager;
        this.signalingMessages = new ConcurrentHashMap<>();
        this.connectionStates = new ConcurrentHashMap<>();

        // Register with SessionManager
        sessionManager.setSignalingHandler(this);
    }

    /**
     * Connection state for tracking P2P setup progress.
     */
    private enum ConnectionState {
        INITIATING, // Offer being created
        OFFER_SENT, // Offer sent, waiting for answer
        ANSWER_RECEIVED, // Answer received, exchanging ICE
        CONNECTED, // P2P stream established
        FAILED // Connection failed
    }

    // ==================== Viewer Connection ====================

    /**
     * Initiates P2P connection for a viewer to watch a session.
     * Called when:
     * 1. Whitelisted viewer clicks the Eye icon
     * 2. Host approves an access request
     * 
     * @param sessionId The session to join
     * @param viewerId  The viewer's user ID
     */
    public void initiateViewerConnection(String sessionId, String viewerId) {
        LiveSession session = sessionManager.getSession(sessionId);
        if (session == null || !session.isActive()) {
            System.err
                    .println("[SessionSignalingHandler] Cannot initiate connection - session not active: " + sessionId);
            return;
        }

        String hostId = session.getHostId();

        // Track connection state
        connectionStates
                .computeIfAbsent(sessionId, k -> new ConcurrentHashMap<>())
                .put(viewerId, ConnectionState.INITIATING);

        // Create session-specific P2P connection
        createSessionConnection(sessionId, hostId, viewerId);

        System.out.println("[SessionSignalingHandler] Initiating P2P connection: " +
                viewerId + " -> " + hostId + " (session: " + sessionId + ")");
    }

    /**
     * Creates the WebRTC peer connection for a session viewer.
     */
    private void createSessionConnection(String sessionId, String hostId, String viewerId) {
        // Use P2PConnectionManager to create the connection
        // The offer will be sent via chat stream as invisible signaling

        // Note: This integrates with existing P2PConnectionManager
        // The connection is tagged with sessionId for routing

        try {
            // Create connection with session context
            // P2PConnectionManager handles ICE gathering and offer creation
            p2pManager.createConnection(hostId);

            // Update state
            Map<String, ConnectionState> states = connectionStates.get(sessionId);
            if (states != null) {
                states.put(viewerId, ConnectionState.OFFER_SENT);
            }

        } catch (Exception e) {
            System.err.println("[SessionSignalingHandler] Failed to create connection: " + e.getMessage());

            Map<String, ConnectionState> states = connectionStates.get(sessionId);
            if (states != null) {
                states.put(viewerId, ConnectionState.FAILED);
            }
        }
    }

    // ==================== Signaling Message Handling ====================

    /**
     * Parses and handles a raw signaling message string.
     * Expected format: $$SIGNAL$$|sessionId|signalType|data
     */
    public void handleSignalingMessage(String senderId, String rawMessage) {
        if (rawMessage == null || !rawMessage.startsWith("$$SIGNAL$$"))
            return;

        try {
            String[] parts = rawMessage.split("\\|", 4);
            if (parts.length < 4) {
                System.err.println("[SessionSignalingHandler] Invalid signaling message format");
                return;
            }

            String sessionId = parts[1];
            String typeStr = parts[2];
            String data = parts[3];

            SignalType type = SignalType.valueOf(typeStr);
            handleIncomingSignal(sessionId, data, type, senderId);

        } catch (Exception e) {
            System.err.println("[SessionSignalingHandler] Error parsing signaling message: " + e.getMessage());
        }
    }

    /**
     * Handles incoming signaling message from chat stream.
     * These are invisible SIGNALING_METADATA messages.
     * 
     * @param sessionId  The session context
     * @param signal     The signaling data (SDP or ICE)
     * @param signalType Type of signal (OFFER, ANSWER, ICE_CANDIDATE)
     * @param fromUserId Sender of the signal
     */
    public void handleIncomingSignal(String sessionId, String signal,
            SignalType signalType, String fromUserId) {

        LiveSession session = sessionManager.getSession(sessionId);
        if (session == null) {
            System.err.println("[SessionSignalingHandler] Unknown session: " + sessionId);
            return;
        }

        switch (signalType) {
            case OFFER -> handleOffer(sessionId, signal, fromUserId);
            case ANSWER -> handleAnswer(sessionId, signal, fromUserId);
            case ICE_CANDIDATE -> handleIceCandidate(sessionId, signal, fromUserId);
        }
    }

    private void handleOffer(String sessionId, String sdp, String fromUserId) {
        System.out.println("[SessionSignalingHandler] Received offer from: " + fromUserId);
        // Process via P2PConnectionManager
        // Response will be sent as invisible signaling message
    }

    private void handleAnswer(String sessionId, String sdp, String fromUserId) {
        System.out.println("[SessionSignalingHandler] Received answer from: " + fromUserId);

        // Update connection state
        Map<String, ConnectionState> states = connectionStates.get(sessionId);
        if (states != null) {
            states.put(fromUserId, ConnectionState.ANSWER_RECEIVED);
        }

        // Process via P2PConnectionManager
    }

    private void handleIceCandidate(String sessionId, String candidate, String fromUserId) {
        // Process ICE candidate
        // Track for cleanup
        trackSignalingMessage(sessionId, "ice_" + System.currentTimeMillis());
    }

    // ==================== P2P Connection Callbacks ====================

    /**
     * Called when P2P connection is successfully established.
     * Triggers zero-persistence cleanup per directive.
     * 
     * @param sessionId The session ID
     * @param viewerId  The connected viewer
     */
    public void onConnectionEstablished(String sessionId, String viewerId) {
        // Update state
        Map<String, ConnectionState> states = connectionStates.get(sessionId);
        if (states != null) {
            states.put(viewerId, ConnectionState.CONNECTED);
        }

        // Add viewer to session
        sessionManager.addViewer(sessionId, viewerId);

        // Trigger zero-persistence cleanup
        sessionManager.onP2PConnectionEstablished(sessionId);

        System.out.println("[SessionSignalingHandler] P2P connection established: " +
                viewerId + " -> session " + sessionId);
    }

    /**
     * Called when P2P connection fails.
     */
    public void onConnectionFailed(String sessionId, String viewerId, String error) {
        Map<String, ConnectionState> states = connectionStates.get(sessionId);
        if (states != null) {
            states.put(viewerId, ConnectionState.FAILED);
        }

        System.err.println("[SessionSignalingHandler] Connection failed for " +
                viewerId + ": " + error);
    }

    // ==================== Zero-Persistence Cleanup ====================

    /**
     * Cleans up signaling metadata for a session.
     * Purges transient ICE candidates and offer/answer data.
     * 
     * @param sessionId The session to clean up
     */
    public void cleanupSignalingMetadata(String sessionId) {
        Set<String> messages = signalingMessages.remove(sessionId);
        if (messages != null && !messages.isEmpty()) {
            // Notify callback to remove invisible messages from chat
            if (messageCallback != null) {
                for (String messageId : messages) {
                    messageCallback.removeSignalingMessage(messageId);
                }
            }
            System.out.println("[SessionSignalingHandler] Cleaned " + messages.size() +
                    " signaling messages for session: " + sessionId);
        }
    }

    /**
     * Cleans up all signaling state for a session (called on session end).
     */
    public void cleanupSessionSignaling(String sessionId) {
        cleanupSignalingMetadata(sessionId);
        connectionStates.remove(sessionId);

        System.out.println("[SessionSignalingHandler] Cleaned up all signaling for session: " + sessionId);
    }

    // ==================== Signaling Message Tracking ====================

    private void trackSignalingMessage(String sessionId, String messageId) {
        signalingMessages
                .computeIfAbsent(sessionId, k -> ConcurrentHashMap.newKeySet())
                .add(messageId);
    }

    /**
     * Sends a signaling message via the chat stream (invisible).
     * 
     * @param sessionId  The session context
     * @param targetUser The recipient
     * @param signalType Type of signal
     * @param data       The signaling data
     */
    public void sendSignalingMessage(String sessionId, String targetUser,
            SignalType signalType, String data) {
        if (messageCallback != null) {
            String messageId = messageCallback.sendSignalingMessage(
                    sessionId, targetUser, signalType, data);
            trackSignalingMessage(sessionId, messageId);
        }
    }

    // ==================== Registration ====================

    /**
     * Sets the callback for sending/removing signaling messages in chat.
     */
    public void setMessageCallback(SignalingMessageCallback callback) {
        this.messageCallback = callback;
    }

    // ==================== Signal Types ====================

    public enum SignalType {
        OFFER,
        ANSWER,
        ICE_CANDIDATE
    }

    // ==================== Callback Interface ====================

    /**
     * Callback interface for chat stream integration.
     * Implemented by ChatViewController to handle invisible signaling messages.
     */
    public interface SignalingMessageCallback {
        /**
         * Sends an invisible signaling message via chat stream.
         * 
         * @return The message ID for tracking
         */
        String sendSignalingMessage(String sessionId, String targetUser,
                SignalType signalType, String data);

        /**
         * Removes a signaling message from local history (zero-persistence).
         */
        void removeSignalingMessage(String messageId);
    }
}
