package com.saferoom.gui.utils;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.SequentialTransition;
import javafx.scene.Node;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Effect;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Utility for the "Glowing_State" soft pulse animation on the terminal sidebar
 * icon.
 * 
 * The glow animation consists of:
 * 1. A pulsing drop shadow effect (soft cyan/green glow)
 * 2. Fading opacity between 0.6 and 1.0
 * 
 * Usage:
 * - Call startPulse(node) when a terminal session becomes active
 * - Call stopPulse(node) when the session ends
 */
public class TerminalGlowAnimation {

    private static final Duration PULSE_DURATION = Duration.millis(1500);
    private static final Color GLOW_COLOR = Color.rgb(0, 255, 200, 0.8); // Cyan-green
    private static final double GLOW_RADIUS = 10.0;
    private static final double GLOW_SPREAD = 0.3;

    // Track active animations per node (WeakHashMap to avoid memory leaks)
    private static final Map<Node, Animation> activeAnimations = new WeakHashMap<>();
    private static final Map<Node, Effect> originalEffects = new WeakHashMap<>();

    /**
     * Starts the soft pulse glow animation on the specified node.
     * 
     * @param node The node to animate (typically the terminal icon)
     */
    public static void startPulse(Node node) {
        if (node == null)
            return;

        // Stop any existing animation
        stopPulse(node);

        // Store original effect
        originalEffects.put(node, node.getEffect());

        // Create glow effect
        DropShadow glow = new DropShadow();
        glow.setColor(GLOW_COLOR);
        glow.setRadius(GLOW_RADIUS);
        glow.setSpread(GLOW_SPREAD);
        node.setEffect(glow);

        // Add glow style class
        if (!node.getStyleClass().contains("terminal-glow")) {
            node.getStyleClass().add("terminal-glow");
        }

        // Create fade transition for pulsing effect
        FadeTransition fadeIn = new FadeTransition(PULSE_DURATION.divide(2), node);
        fadeIn.setFromValue(0.6);
        fadeIn.setToValue(1.0);

        FadeTransition fadeOut = new FadeTransition(PULSE_DURATION.divide(2), node);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.6);

        SequentialTransition pulse = new SequentialTransition(fadeIn, fadeOut);
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.setAutoReverse(false);

        // Store and start animation
        activeAnimations.put(node, pulse);
        pulse.play();
    }

    /**
     * Stops the pulse animation and restores the node's original state.
     * 
     * @param node The node to stop animating
     */
    public static void stopPulse(Node node) {
        if (node == null)
            return;

        // Stop animation
        Animation animation = activeAnimations.remove(node);
        if (animation != null) {
            animation.stop();
        }

        // Restore original effect
        Effect original = originalEffects.remove(node);
        node.setEffect(original);

        // Restore opacity
        node.setOpacity(1.0);

        // Remove glow style class
        node.getStyleClass().remove("terminal-glow");
    }

    /**
     * Checks if a node has an active pulse animation.
     * 
     * @param node The node to check
     * @return true if pulsing, false otherwise
     */
    public static boolean isPulsing(Node node) {
        Animation animation = activeAnimations.get(node);
        return animation != null && animation.getStatus() == Animation.Status.RUNNING;
    }

    /**
     * Creates a one-shot attention pulse (non-looping).
     * Useful for drawing attention to a new session.
     * 
     * @param node The node to pulse
     */
    public static void pulseOnce(Node node) {
        if (node == null)
            return;

        // Save original opacity
        double originalOpacity = node.getOpacity();

        // Create temporary glow
        DropShadow glow = new DropShadow();
        glow.setColor(GLOW_COLOR);
        glow.setRadius(GLOW_RADIUS * 1.5);
        glow.setSpread(GLOW_SPREAD);

        Effect originalEffect = node.getEffect();
        node.setEffect(glow);

        // Single pulse
        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), node);
        fadeIn.setFromValue(originalOpacity);
        fadeIn.setToValue(1.0);

        FadeTransition fadeOut = new FadeTransition(Duration.millis(400), node);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(originalOpacity);

        SequentialTransition pulse = new SequentialTransition(fadeIn, fadeOut);
        pulse.setOnFinished(e -> node.setEffect(originalEffect));
        pulse.play();
    }
}
