package com.saferoom.gui.components;

import com.saferoom.gui.model.AccessRequest;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;

import org.kordamp.ikonli.javafx.FontIcon;

import java.util.function.Consumer;

/**
 * Non-intrusive popup for Host to approve/deny viewer access requests.
 * 
 * Displays: "[User_Name] wants to watch. [Allow] / [Ignore]"
 * 
 * Features:
 * - Appears near the Eye icon on the session message
 * - Auto-dismiss timer (30 seconds)
 * - Fade-in/fade-out animations
 * - Callback for response handling
 */
public class AccessRequestPopup extends Popup {

    private static final Duration AUTO_DISMISS_DELAY = Duration.seconds(30);
    private static final Duration FADE_DURATION = Duration.millis(200);

    private final VBox container;
    private final Label messageLabel;
    private final Button allowButton;
    private final Button ignoreButton;
    private final PauseTransition autoDismissTimer;
    private final Consumer<Boolean> responseCallback;

    /**
     * Creates an access request popup.
     * 
     * @param request  The access request to display
     * @param callback Called with true for Allow, false for Ignore
     */
    public AccessRequestPopup(AccessRequest request, Consumer<Boolean> callback) {
        super();
        this.responseCallback = callback;

        setAutoHide(true);
        setHideOnEscape(true);

        // Icon
        FontIcon icon = new FontIcon("fas-eye");
        icon.getStyleClass().add("access-request-icon");

        // Message
        messageLabel = new Label(request.getRequesterName() + " wants to watch.");
        messageLabel.getStyleClass().add("access-request-message");

        // Allow button
        allowButton = new Button("Allow");
        allowButton.getStyleClass().addAll("access-request-button", "allow-button");
        allowButton.setGraphic(new FontIcon("fas-check"));
        allowButton.setOnAction(e -> handleResponse(true));

        // Ignore button
        ignoreButton = new Button("Ignore");
        ignoreButton.getStyleClass().addAll("access-request-button", "ignore-button");
        ignoreButton.setOnAction(e -> handleResponse(false));

        // Button row
        HBox buttonRow = new HBox(10);
        buttonRow.setAlignment(Pos.CENTER);
        buttonRow.getChildren().addAll(allowButton, ignoreButton);

        // Content layout
        HBox contentRow = new HBox(10);
        contentRow.setAlignment(Pos.CENTER_LEFT);
        contentRow.getChildren().addAll(icon, messageLabel);

        // Container
        container = new VBox(10);
        container.getStyleClass().add("access-request-popup");
        container.setPadding(new Insets(12, 16, 12, 16));
        container.getChildren().addAll(contentRow, buttonRow);

        getContent().add(container);

        // Auto-dismiss timer
        autoDismissTimer = new PauseTransition(AUTO_DISMISS_DELAY);
        autoDismissTimer.setOnFinished(e -> handleResponse(false)); // Auto-ignore
    }

    /**
     * Shows the popup anchored to a window at the specified position.
     * 
     * @param owner The owner window
     * @param x     Screen X position
     * @param y     Screen Y position
     */
    public void showAt(Window owner, double x, double y) {
        // Fade in
        container.setOpacity(0);
        show(owner, x, y);

        FadeTransition fadeIn = new FadeTransition(FADE_DURATION, container);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();

        // Start auto-dismiss timer
        autoDismissTimer.playFromStart();
    }

    /**
     * Shows the popup centered below the specified anchor node.
     * 
     * @param owner      The owner window
     * @param anchorNode The node to anchor below
     */
    public void showBelowNode(Window owner, javafx.scene.Node anchorNode) {
        if (anchorNode == null) {
            return;
        }

        // Calculate position
        javafx.geometry.Bounds bounds = anchorNode.localToScreen(anchorNode.getBoundsInLocal());
        if (bounds == null) {
            return;
        }

        double x = bounds.getMinX();
        double y = bounds.getMaxY() + 5;

        showAt(owner, x, y);
    }

    private void handleResponse(boolean approved) {
        autoDismissTimer.stop();

        // Fade out
        FadeTransition fadeOut = new FadeTransition(FADE_DURATION, container);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> {
            hide();
            if (responseCallback != null) {
                responseCallback.accept(approved);
            }
        });
        fadeOut.play();
    }

    /**
     * Updates the request message (for multiple queued requests).
     */
    public void updateRequest(AccessRequest request) {
        messageLabel.setText(request.getRequesterName() + " wants to watch.");
        autoDismissTimer.playFromStart();
    }
}
