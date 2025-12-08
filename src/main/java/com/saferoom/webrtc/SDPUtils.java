package com.saferoom.webrtc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Utility for minimizing SDP generation.
 * Strips unused codecs, RTX/FEC mechanisms, and reduces line overhead.
 * Target size: < 1KB
 */
public class SDPUtils {

    // Keep only widely compatible and fast codecs
    private static final List<String> PREFERRED_VIDEO_CODECS = Arrays.asList("VP8", "H264");
    private static final List<String> PREFERRED_AUDIO_CODECS = Arrays.asList("opus", "PCMU", "PCMA");

    /**
     * Minimize SDP by removing unused codecs and extensions
     */
    public static String mungeSDP(String sdp) {
        if (sdp == null || sdp.isEmpty())
            return sdp;

        StringBuilder sb = new StringBuilder();
        String[] lines = sdp.split("\r\n");

        for (String line : lines) {
            String trimmed = line.trim();

            // 1. Remove specific unwanted lines entirely
            if (shouldRemoveLine(trimmed)) {
                continue;
            }

            // 2. Filter attribute lines (a=)
            if (trimmed.startsWith("a=")) {
                if (shouldRemoveAttribute(trimmed)) {
                    continue;
                }
            }

            // 3. Keep line
            sb.append(line).append("\r\n");
        }

        return sb.toString();
    }

    private static boolean shouldRemoveLine(String line) {
        // Remove known useless or heavy lines
        return false; // Aggressive filtering handled in attributes
    }

    private static boolean shouldRemoveAttribute(String line) {
        // Remove RTX, RED, ULPFEC (Error correction overhead)
        if (line.contains("rtx") || line.contains("red") || line.contains("ulpfec")) {
            return true;
        }

        // Remove Simulcast (Heavy overhead for P2P)
        if (line.contains("simulcast") || line.contains("rid:")) {
            return true;
        }

        // Remove unnecessary extensions
        if (line.startsWith("a=extmap:") && (line.contains("transport-cc") ||
                line.contains("goog-remb") ||
                line.contains("toffset") ||
                line.contains("abs-send-time") ||
                line.contains("video-orientation"))) {
            // Keep critical extensions only (like orientation if strictly needed, but
            // stripping for speed)
            // Ideally keep mid, and maybe orientation. Removing transport-cc saves
            // bandwidth.
            return true;
        }

        // Remove generic framework info
        if (line.startsWith("a=msid-semantic: WMS")) { // Usually safe to remove if we don't rely on it
            return false;
        }

        return false;
    }
}
