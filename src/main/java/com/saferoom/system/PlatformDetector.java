package com.saferoom.system;

import java.util.Locale;
import java.util.Objects;

/**
 * Simple centralized utility for OS and compositor detection. Avoids having string-based
 * platform checks scattered throughout the codebase.
 */
public final class PlatformDetector {

    private static final String OS_NAME = System.getProperty("os.name", "").toLowerCase(Locale.ENGLISH);
    private static final String SESSION_TYPE = System.getenv("XDG_SESSION_TYPE");
    private static final String WAYLAND_DISPLAY = System.getenv("WAYLAND_DISPLAY");

    private PlatformDetector() {
    }

    public static boolean isLinux() {
        return OS_NAME.contains("linux");
    }

    public static boolean isWindows() {
        return OS_NAME.contains("win");
    }

    public static boolean isMac() {
        return OS_NAME.contains("mac");
    }

    /**
     * Wayland detection is best-effort: if XDG_SESSION_TYPE or WAYLAND_DISPLAY indicate Wayland,
     * we treat the current session as Wayland.
     */
    public static boolean isWayland() {
        if (!isLinux()) {
            return false;
        }
        if (Objects.equals("wayland", SESSION_TYPE != null ? SESSION_TYPE.toLowerCase(Locale.ENGLISH) : null)) {
            return true;
        }
        return WAYLAND_DISPLAY != null && !WAYLAND_DISPLAY.isBlank();
    }
}

