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
        // ⚠️ DISABLE ALL AGGRESSIVE FILTERING TO RESTORE STABILITY
        // Sending a few extra bytes is better than crashing the call.

        // Remove generic framework info (Safe to remove)
        if (line.startsWith("a=msid-semantic: WMS")) {
            return false;
        }

        return false;
    }

    /**
     * Force the specified media type to have 'sendrecv' direction.
     * Use this to correct Early Offer race conditions where the track
     * might not be fully registered yet, but we INTEND to send.
     */
    public static String enforceSendRecv(String sdp, String mediaType) {
        if (sdp == null || !sdp.contains("m=" + mediaType))
            return sdp;

        StringBuilder sb = new StringBuilder();
        String[] lines = sdp.split("\r\n");
        boolean inMediaSection = false;
        boolean directionSet = false;

        for (String line : lines) {
            if (line.startsWith("m=")) {
                inMediaSection = line.startsWith("m=" + mediaType);
                if (inMediaSection) {
                    // Reset flag for this new section
                    directionSet = false;
                }
            }

            if (inMediaSection) {
                // If we see an existing direction, force it to sendrecv
                if (line.equals("a=recvonly") || line.equals("a=sendonly") || line.equals("a=inactive")) {
                    sb.append("a=sendrecv").append("\r\n");
                    directionSet = true;
                    continue;
                }
                if (line.equals("a=sendrecv")) {
                    directionSet = true;
                }
            }

            sb.append(line).append("\r\n");
        }

        return sb.toString();
    }
}
