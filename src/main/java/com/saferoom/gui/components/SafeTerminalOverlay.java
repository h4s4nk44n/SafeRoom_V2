package com.saferoom.gui.components;

import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import org.kordamp.ikonli.javafx.FontIcon;

import java.util.function.Consumer;

/**
 * SafeTerminal overlay component for P2P terminal sessions.
 * 
 * Features:
 * - 25% height bottom bar overlaying the Command Center
 * - Slide-up/slide-down animations
 * - Live P2P text stream output
 * - Command input field
 * - Scroll-to-bottom on new output
 * 
 * Usage:
 * 1. Add as child to the main StackPane (over the chat area)
 * 2. Call slideUp() when terminal session starts or icon clicked
 * 3. Call slideDown() to dismiss
 */
public class SafeTerminalOverlay extends VBox {

    private static final Duration ANIMATION_DURATION = Duration.millis(300);
    private static final double HEIGHT_PERCENTAGE = 0.25; // 25% of parent

    private final String sessionId;
    private final TextArea terminalOutput;
    private final TextField terminalInput;
    private final Label sessionLabel;
    private final Button closeButton;
    private final ScrollPane outputScrollPane;

    private Consumer<String> inputHandler;
    private Runnable closeHandler;
    private boolean isVisible = false;

    /**
     * Creates a SafeTerminal overlay for a session.
     * 
     * @param sessionId    The terminal session ID
     * @param sessionTitle The session title for display
     */
    public SafeTerminalOverlay(String sessionId, String sessionTitle) {
        super();
        this.sessionId = sessionId;

        getStyleClass().add("terminal-overlay");
        setAlignment(Pos.BOTTOM_CENTER);

        // Initially hidden (translated down)
        setVisible(false);
        setManaged(false);

        // Header bar
        FontIcon terminalIcon = new FontIcon("fas-terminal");
        terminalIcon.getStyleClass().add("terminal-header-icon");

        sessionLabel = new Label(sessionTitle);
        sessionLabel.getStyleClass().add("terminal-session-label");

        HBox headerLeft = new HBox(8);
        headerLeft.setAlignment(Pos.CENTER_LEFT);
        headerLeft.getChildren().addAll(terminalIcon, sessionLabel);
        HBox.setHgrow(headerLeft, Priority.ALWAYS);

        closeButton = new Button();
        closeButton.setGraphic(new FontIcon("fas-times"));
        closeButton.getStyleClass().add("terminal-close-button");
        closeButton.setOnAction(e -> {
            if (closeHandler != null) {
                closeHandler.run();
            }
            slideDown();
        });

        HBox headerBar = new HBox(10);
        headerBar.getStyleClass().add("terminal-header");
        headerBar.setPadding(new Insets(8, 12, 8, 12));
        headerBar.setAlignment(Pos.CENTER);
        headerBar.getChildren().addAll(headerLeft, closeButton);

        // Terminal output area
        terminalOutput = new TextArea();
        terminalOutput.getStyleClass().add("terminal-output");
        terminalOutput.setEditable(false);
        terminalOutput.setWrapText(true);
        terminalOutput.setFocusTraversable(false);

        outputScrollPane = new ScrollPane(terminalOutput);
        outputScrollPane.setFitToWidth(true);
        outputScrollPane.setFitToHeight(true);
        outputScrollPane.getStyleClass().add("terminal-output-scroll");
        VBox.setVgrow(outputScrollPane, Priority.ALWAYS);

        // Terminal input area
        terminalInput = new TextField();
        terminalInput.getStyleClass().add("terminal-input");
        terminalInput.setPromptText("Enter command...");
        terminalInput.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                sendInput();
            }
        });

        Button sendButton = new Button();
        sendButton.setGraphic(new FontIcon("fas-paper-plane"));
        sendButton.getStyleClass().add("terminal-send-button");
        sendButton.setOnAction(e -> sendInput());

        HBox inputBar = new HBox(8);
        inputBar.getStyleClass().add("terminal-input-bar");
        inputBar.setPadding(new Insets(8, 12, 8, 12));
        inputBar.setAlignment(Pos.CENTER);
        HBox.setHgrow(terminalInput, Priority.ALWAYS);
        inputBar.getChildren().addAll(terminalInput, sendButton);

        // Assemble layout
        getChildren().addAll(headerBar, outputScrollPane, inputBar);
    }

    /**
     * Animates the terminal overlay sliding up from the bottom.
     */
    public void slideUp() {
        if (isVisible)
            return;

        setVisible(true);
        setManaged(true);
        isVisible = true;

        // Start from below the visible area
        setTranslateY(getHeight() > 0 ? getHeight() : 300);

        TranslateTransition transition = new TranslateTransition(ANIMATION_DURATION, this);
        transition.setFromY(getHeight() > 0 ? getHeight() : 300);
        transition.setToY(0);
        transition.play();

        // Focus input after animation
        transition.setOnFinished(e -> terminalInput.requestFocus());
    }

    /**
     * Animates the terminal overlay sliding down and hides it.
     */
    public void slideDown() {
        if (!isVisible)
            return;

        TranslateTransition transition = new TranslateTransition(ANIMATION_DURATION, this);
        transition.setFromY(0);
        transition.setToY(getHeight() > 0 ? getHeight() : 300);
        transition.setOnFinished(e -> {
            setVisible(false);
            setManaged(false);
            isVisible = false;
        });
        transition.play();
    }

    /**
     * Toggles the terminal visibility.
     */
    public void toggle() {
        if (isVisible) {
            slideDown();
        } else {
            slideUp();
        }
    }

    /**
     * Appends output text to the terminal.
     * Automatically scrolls to bottom.
     * 
     * @param text The text to append
     */
    public void appendOutput(String text) {
        if (text == null || text.isEmpty())
            return;

        terminalOutput.appendText(text);

        // Auto-scroll to bottom
        terminalOutput.setScrollTop(Double.MAX_VALUE);
    }

    /**
     * Appends a line of output with newline.
     */
    public void appendLine(String line) {
        appendOutput(line + "\n");
    }

    /**
     * Clears the terminal output.
     */
    public void clearOutput() {
        terminalOutput.clear();
    }

    /**
     * Sets the handler for user input commands.
     * 
     * @param handler Consumer receiving the input string
     */
    public void setInputHandler(Consumer<String> handler) {
        this.inputHandler = handler;
    }

    /**
     * Sets the handler for close button clicks.
     */
    public void setCloseHandler(Runnable handler) {
        this.closeHandler = handler;
    }

    private void sendInput() {
        String input = terminalInput.getText().trim();
        if (input.isEmpty())
            return;

        // Echo input to output
        appendLine("> " + input);

        // Clear input field
        terminalInput.clear();

        // Send to handler
        if (inputHandler != null) {
            inputHandler.accept(input);
        }
    }

    /**
     * Updates the session label.
     */
    public void setSessionTitle(String title) {
        sessionLabel.setText(title);
    }

    /**
     * Gets the session ID this overlay is associated with.
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Checks if the overlay is currently visible.
     */
    public boolean isOverlayVisible() {
        return isVisible;
    }

    /**
     * Configures the overlay to fill 25% of parent height.
     * Call this after adding to the parent.
     * 
     * @param parentHeight The parent container height
     */
    public void configureHeight(double parentHeight) {
        double targetHeight = parentHeight * HEIGHT_PERCENTAGE;
        setMinHeight(targetHeight);
        setMaxHeight(targetHeight);
        setPrefHeight(targetHeight);
    }
}
