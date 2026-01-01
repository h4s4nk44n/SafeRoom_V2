package com.saferoom.gui.model;

/**
 * Defines the types of live P2P sessions in the Contextual Workstation.
 * The UI reacts to "how" content is shared, not "what" is being shared.
 */
public enum SessionType {
    /**
     * Screen share session - viewers can watch the host's screen.
     * Displayed with the Eye (👁️) icon in the chat stream.
     */
    SCREEN_SHARE,

    /**
     * Remote control session - viewers can watch AND send input to host.
     * Displayed with the Eye (👁️) icon, with additional input capability.
     */
    REMOTE_CONTROL,

    /**
     * Terminal session - P2P text stream for command-line interaction.
     * Triggers the SafeTerminal overlay and sidebar glow animation.
     */
    TERMINAL
}
