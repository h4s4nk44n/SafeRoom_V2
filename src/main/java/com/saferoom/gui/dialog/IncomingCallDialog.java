package com.saferoom.gui.dialog;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import java.util.concurrent.CompletableFuture;

/**
 * Dialog for incoming WebRTC calls
 * Shows caller information with Accept/Reject buttons
 */
public class IncomingCallDialog {
    
    private final Stage stage;
    private final CompletableFuture<Boolean> result;
    private final String callerUsername;
    private final String callId;
    private final boolean videoEnabled;
    
    /**
     * Create incoming call dialog
     * 
     * @param callerUsername Username of the caller
     * @param callId Unique call identifier
     * @param videoEnabled Whether this is a video call
     */
    public IncomingCallDialog(String callerUsername, String callId, boolean videoEnabled) {
        this.callerUsername = callerUsername;
        this.callId = callId;
        this.videoEnabled = videoEnabled;
        this.result = new CompletableFuture<>();
        this.stage = createDialog();
    }
    
    private Stage createDialog() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.UTILITY);
        dialog.setTitle("Incoming Call");
        dialog.setResizable(false);
        
        // Main container
        VBox mainContainer = new VBox(20);
        mainContainer.setPadding(new Insets(30));
        mainContainer.setAlignment(Pos.CENTER);
        mainContainer.getStyleClass().add("dialog-container");
        
        // Call icon
        FontIcon callIcon = new FontIcon(videoEnabled ? 
            FontAwesomeSolid.VIDEO : FontAwesomeSolid.PHONE);
        callIcon.setIconSize(64);
        callIcon.getStyleClass().add("call-icon-incoming");
        
        // Caller name
        Label callerLabel = new Label(callerUsername);
        callerLabel.getStyleClass().add("caller-name");
        callerLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        
        // Call type label
        Label typeLabel = new Label(videoEnabled ? 
            "Incoming video call..." : "Incoming audio call...");
        typeLabel.getStyleClass().add("call-type-label");
        typeLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #888;");
        
        // Buttons container
        HBox buttonBox = new HBox(20);
        buttonBox.setAlignment(Pos.CENTER);
        
        // Reject button (red)
        Button rejectButton = new Button();
        FontIcon rejectIcon = new FontIcon(FontAwesomeSolid.PHONE_SLASH);
        rejectIcon.setIconSize(24);
        rejectButton.setGraphic(rejectIcon);
        rejectButton.getStyleClass().addAll("call-button", "reject-button");
        rejectButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; " +
                             "-fx-pref-width: 80px; -fx-pref-height: 80px; -fx-background-radius: 40px;");
        rejectButton.setOnAction(e -> {
            System.out.printf("[IncomingCallDialog] ❌ Reject button clicked for call: %s%n", callId);
            result.complete(false);
            dialog.close();
        });
        
        // Accept button (green)
        Button acceptButton = new Button();
        FontIcon acceptIcon = new FontIcon(FontAwesomeSolid.PHONE);
        acceptIcon.setIconSize(24);
        acceptButton.setGraphic(acceptIcon);
        acceptButton.getStyleClass().addAll("call-button", "accept-button");
        acceptButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; " +
                             "-fx-pref-width: 80px; -fx-pref-height: 80px; -fx-background-radius: 40px;");
        acceptButton.setOnAction(e -> {
            System.out.printf("[IncomingCallDialog] ✅ Accept button clicked for call: %s%n", callId);
            result.complete(true);
            dialog.close();
        });
        
        // Add hover effects
        rejectButton.setOnMouseEntered(e -> 
            rejectButton.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white; " +
                                 "-fx-pref-width: 80px; -fx-pref-height: 80px; -fx-background-radius: 40px;"));
        rejectButton.setOnMouseExited(e -> 
            rejectButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; " +
                                 "-fx-pref-width: 80px; -fx-pref-height: 80px; -fx-background-radius: 40px;"));
        
        acceptButton.setOnMouseEntered(e -> 
            acceptButton.setStyle("-fx-background-color: #229954; -fx-text-fill: white; " +
                                 "-fx-pref-width: 80px; -fx-pref-height: 80px; -fx-background-radius: 40px;"));
        acceptButton.setOnMouseExited(e -> 
            acceptButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; " +
                                 "-fx-pref-width: 80px; -fx-pref-height: 80px; -fx-background-radius: 40px;"));
        
        buttonBox.getChildren().addAll(rejectButton, acceptButton);
        
        // Add all elements
        mainContainer.getChildren().addAll(
            callIcon,
            callerLabel,
            typeLabel,
            buttonBox
        );
        
        // Create scene
        Scene scene = new Scene(mainContainer, 400, 350);
        try {
            scene.getStylesheets().add(
                getClass().getResource("/styles/styles.css").toExternalForm()
            );
        } catch (Exception e) {
            System.err.println("[IncomingCallDialog] Warning: Could not load styles.css");
        }
        
        dialog.setScene(scene);
        
        // Handle window close
        dialog.setOnCloseRequest(e -> {
            if (!result.isDone()) {
                result.complete(false);
            }
        });
        
        return dialog;
    }
    
    /**
     * Show the dialog and wait for user response
     * 
     * @return CompletableFuture<Boolean> - true if accepted, false if rejected
     */
    public CompletableFuture<Boolean> showAndWait() {
        stage.showAndWait();
        return result;
    }
    
    /**
     * Show the dialog without waiting
     * 
     * @return CompletableFuture<Boolean> - true if accepted, false if rejected
     */
    public CompletableFuture<Boolean> show() {
        System.out.printf("[IncomingCallDialog] 📺 Showing dialog for call from %s (callId: %s)%n", 
            callerUsername, callId);
        stage.show();
        System.out.printf("[IncomingCallDialog] 📺 Dialog shown, result.isDone=%b%n", result.isDone());
        return result;
    }
    
    /**
     * Close the dialog
     */
    public void close() {
        if (stage.isShowing()) {
            stage.close();
        }
        if (!result.isDone()) {
            result.complete(false);
        }
    }
    
    /**
     * Get the call ID
     */
    public String getCallId() {
        return callId;
    }
    
    /**
     * Get the caller username
     */
    public String getCallerUsername() {
        return callerUsername;
    }
}
