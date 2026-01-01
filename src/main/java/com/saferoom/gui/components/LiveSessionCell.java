package com.saferoom.gui.components;

import com.saferoom.gui.model.LiveSession;
import com.saferoom.gui.model.LiveSessionMessage;
import com.saferoom.gui.model.SessionState;

import com.saferoom.session.SessionManager;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import org.kordamp.ikonli.javafx.FontIcon;

import java.util.function.BiConsumer;

/**
 * Custom ListCell for rendering live session message blocks in the chat stream.
 * 
 * Features:
 * - Eye icon (👁️) with real-time viewer count badge
 * - Session metadata (title, host, type)
 * - Permission-aware click handling:
 * - Whitelisted viewers: Direct P2P stream connection
 * - Non-whitelisted viewers: Sends Access_Request signal to Host
 */
public class LiveSessionCell extends ListCell<LiveSessionMessage> {

    private final String currentUserId;
    private final SessionManager sessionManager;

    // UI Components
    private final HBox container;
    private final VBox sessionCard;
    private final HBox headerRow;
    private final Label titleLabel;
    private final Label hostLabel;
    private final Label typeLabel;
    private final StackPane eyeIconContainer;
    private final FontIcon eyeIcon;
    private final Label viewerCountBadge;
    private final Label statusLabel;

    // Click handler callback: (sessionId, isHost) -> action
    private BiConsumer<String, Boolean> sessionClickHandler;

    public LiveSessionCell(String currentUserId, SessionManager sessionManager) {
        super();
        this.currentUserId = currentUserId;
        this.sessionManager = sessionManager;

        // Initialize components
        getStyleClass().add("live-session-cell");

        // Session type icon
        typeLabel = new Label();
        typeLabel.getStyleClass().add("session-type-label");

        // Title
        titleLabel = new Label();
        titleLabel.getStyleClass().add("session-title");

        // Host info
        hostLabel = new Label();
        hostLabel.getStyleClass().add("session-host");

        // Header row
        headerRow = new HBox(8);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        headerRow.getChildren().addAll(typeLabel, titleLabel);

        // Eye icon with viewer count
        eyeIcon = new FontIcon("fas-eye");
        eyeIcon.getStyleClass().add("eye-icon");

        viewerCountBadge = new Label("0");
        viewerCountBadge.getStyleClass().add("viewer-count-badge");

        eyeIconContainer = new StackPane(eyeIcon, viewerCountBadge);
        eyeIconContainer.getStyleClass().add("eye-icon-container");
        StackPane.setAlignment(viewerCountBadge, Pos.TOP_RIGHT);

        // Status label
        statusLabel = new Label("Live");
        statusLabel.getStyleClass().add("session-status-live");

        // Session card layout
        VBox infoBox = new VBox(4);
        infoBox.getChildren().addAll(headerRow, hostLabel);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        HBox contentRow = new HBox(12);
        contentRow.setAlignment(Pos.CENTER_LEFT);
        contentRow.getChildren().addAll(infoBox, statusLabel, eyeIconContainer);

        sessionCard = new VBox(8);
        sessionCard.getStyleClass().add("live-session-card");
        sessionCard.setPadding(new Insets(12));
        sessionCard.getChildren().add(contentRow);

        // Container for alignment
        container = new HBox();
        container.getChildren().add(sessionCard);

        // Click handler for Eye icon
        eyeIconContainer.setOnMouseClicked(event -> {
            event.consume();
            handleEyeClick();
        });

        // Tooltip
        Tooltip.install(eyeIconContainer, new Tooltip("Click to join session"));
    }

    @Override
    protected void updateItem(LiveSessionMessage message, boolean empty) {
        super.updateItem(message, empty);

        if (empty || message == null) {
            setGraphic(null);
        } else {
            LiveSession session = message.getSession();
            boolean isHost = session.getHostId().equals(currentUserId);

            // Update type icon
            String iconLiteral = switch (session.getType()) {
                case SCREEN_SHARE -> "fas-desktop";
                case REMOTE_CONTROL -> "fas-gamepad";
                case TERMINAL -> "fas-terminal";
            };
            typeLabel.setGraphic(new FontIcon(iconLiteral));

            // Update labels
            titleLabel.setText(session.getTitle());
            hostLabel.setText(isHost ? "You are hosting" : "Hosted by " + session.getHostName());

            // Update status
            updateSessionStatus(session);

            // Bind viewer count
            viewerCountBadge.textProperty().unbind();
            viewerCountBadge.textProperty().bind(
                    Bindings.createStringBinding(
                            () -> String.valueOf(session.getViewerCount()),
                            session.viewerCountProperty()));

            // Update eye icon style based on permission
            updateEyeIconStyle(session, isHost);

            // Alignment based on host
            container.getChildren().clear();
            if (isHost) {
                container.setAlignment(Pos.CENTER_RIGHT);
                sessionCard.getStyleClass().remove("session-card-received");
                if (!sessionCard.getStyleClass().contains("session-card-sent")) {
                    sessionCard.getStyleClass().add("session-card-sent");
                }
            } else {
                container.setAlignment(Pos.CENTER_LEFT);
                sessionCard.getStyleClass().remove("session-card-sent");
                if (!sessionCard.getStyleClass().contains("session-card-received")) {
                    sessionCard.getStyleClass().add("session-card-received");
                }
            }
            container.getChildren().add(sessionCard);

            // Store reference for click handler
            setUserData(message);

            setGraphic(container);
        }
    }

    private void updateSessionStatus(LiveSession session) {
        SessionState state = session.getState();
        statusLabel.getStyleClass().removeAll(
                "session-status-live",
                "session-status-paused",
                "session-status-ended");

        switch (state) {
            case ACTIVE -> {
                statusLabel.setText("Live");
                statusLabel.getStyleClass().add("session-status-live");
            }
            case PAUSED -> {
                statusLabel.setText("Paused");
                statusLabel.getStyleClass().add("session-status-paused");
            }
            case ENDED -> {
                statusLabel.setText("Ended");
                statusLabel.getStyleClass().add("session-status-ended");
            }
        }
    }

    private void updateEyeIconStyle(LiveSession session, boolean isHost) {
        eyeIcon.getStyleClass().removeAll(
                "eye-icon-host",
                "eye-icon-whitelisted",
                "eye-icon-restricted",
                "eye-icon-inactive");

        eyeIconContainer.setDisable(false);

        if (!session.isActive()) {
            eyeIcon.getStyleClass().add("eye-icon-inactive");
            eyeIconContainer.setDisable(true);
            Tooltip.install(eyeIconContainer, new Tooltip("Session ended"));
        } else if (isHost) {
            eyeIcon.getStyleClass().add("eye-icon-host");
            Tooltip.install(eyeIconContainer, new Tooltip("You are the host"));
        } else if (session.isWhitelisted(currentUserId)) {
            eyeIcon.getStyleClass().add("eye-icon-whitelisted");
            Tooltip.install(eyeIconContainer, new Tooltip("Click to join (whitelisted)"));
        } else {
            eyeIcon.getStyleClass().add("eye-icon-restricted");
            Tooltip.install(eyeIconContainer, new Tooltip("Click to request access"));
        }
    }

    /**
     * Handles click on the Eye icon.
     * - Host: Shows viewer list/controls
     * - Whitelisted viewer: Direct P2P connection
     * - Non-whitelisted viewer: Sends access request
     */
    private void handleEyeClick() {
        LiveSessionMessage message = (LiveSessionMessage) getUserData();
        if (message == null)
            return;

        LiveSession session = message.getSession();
        if (!session.isActive())
            return;

        String sessionId = session.getSessionId();
        boolean isHost = session.getHostId().equals(currentUserId);

        if (sessionClickHandler != null) {
            sessionClickHandler.accept(sessionId, isHost);
        }

        if (!isHost) {
            // Request access (SessionManager handles whitelist check)
            Platform.runLater(() -> {
                String userName = currentUserId; // TODO: Get display name from UserSession
                sessionManager.requestAccess(sessionId, currentUserId, userName);
            });
        }
    }

    /**
     * Sets callback for session click events.
     * 
     * @param handler BiConsumer receiving (sessionId, isHost)
     */
    public void setSessionClickHandler(BiConsumer<String, Boolean> handler) {
        this.sessionClickHandler = handler;
    }
}
