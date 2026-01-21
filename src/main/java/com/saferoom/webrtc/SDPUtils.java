package com.saferoom.webrtc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for optimizing SDP generation with platform-aware codec preference.
 * 
 * Key principle: NEVER strip VP8/VP9/H264 - they are all needed for
 * cross-platform
 * compatibility. Instead, REORDER codecs based on platform preference.
 * 
 * - Windows: Prefers H.264 (hardware encoding via QuickSync/NVENC/VCE)
 * - Linux/Mac: Prefers VP8 (better software/VAAPI support)
 */
public class SDPUtils {

    // Platform detection (same as WebRTCClient)
    private static final String OS_NAME = System.getProperty("os.name").toLowerCase();
    private static final boolean IS_WINDOWS = OS_NAME.contains("win");
    private static final boolean IS_LINUX = OS_NAME.contains("linux");
    private static final boolean IS_MAC = OS_NAME.contains("mac");

    // Codec payload type patterns
    private static final Pattern RTPMAP_PATTERN = Pattern.compile("a=rtpmap:(\\d+)\\s+(\\S+)");
    private static final Pattern MLINE_VIDEO_PATTERN = Pattern.compile("m=video\\s+\\d+\\s+\\S+\\s+(.+)");

    /**
     * Optimize SDP by removing unused audio codecs (Opus is universal).
     * DOES NOT remove video codecs VP8/VP9/H264 - they're essential for
     * cross-platform.
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

        // 4. Reorder codec preference based on platform
        return reorderCodecPreference(sb.toString());
    }

    private static boolean shouldRemoveLine(String line) {
        // Remove known useless or heavy lines
        return false; // Aggressive filtering handled in attributes
    }

    private static boolean shouldRemoveAttribute(String line) {
        // ONLY remove uncommon audio codecs - Opus is universally supported
        // DO NOT remove VP8/VP9/H264 - they're essential for cross-platform video

        if (line.contains("AV1") || // AV1 has limited hardware support
                line.contains("PCMU") || line.contains("PCMA") || // G.711 variants
                line.contains("ISAC") || line.contains("G722")) { // Less common audio
            return true;
        }

        // Keep everything else (including VP8, VP9, H264)
        return false;
    }

    /**
     * Force 'sendrecv' direction on a specific media section.
     * Often needed when creating an Offer before the local track is fully live.
     */
    public static String enforceSendRecv(String sdp, String mediaType) {
        if (sdp == null || !sdp.contains("m=" + mediaType)) {
            return sdp;
        }

        StringBuilder sb = new StringBuilder();
        String[] lines = sdp.split("\r\n");
        boolean inMediaSection = false;

        for (String line : lines) {
            String trimmed = line.trim();

            if (trimmed.startsWith("m=")) {
                inMediaSection = trimmed.startsWith("m=" + mediaType);
            }

            if (inMediaSection) {
                // If we see an existing direction, replace it
                if (trimmed.equals("a=sendonly") || trimmed.equals("a=recvonly") || trimmed.equals("a=inactive")) {
                    sb.append("a=sendrecv").append("\r\n");
                    continue;
                }
            }

            sb.append(line).append("\r\n");
        }

        return sb.toString();
    }

    /**
     * Reorder video codec preference based on platform.
     * 
     * This ensures the PREFERRED codec is listed first in the m=video line,
     * but ALL codecs remain available for negotiation.
     * 
     * Windows: H264 first (hardware encoding support)
     * Linux/Mac: VP8 first (better software/VAAPI support)
     */
    public static String reorderCodecPreference(String sdp) {
        if (sdp == null || !sdp.contains("m=video")) {
            return sdp;
        }

        // Determine preferred codec based on platform
        String preferredCodec = IS_WINDOWS ? "H264" : "VP8";

        System.out.printf("[SDPUtils] Platform: %s → Preferred codec: %s%n",
                IS_WINDOWS ? "Windows" : (IS_LINUX ? "Linux" : (IS_MAC ? "macOS" : "Unknown")),
                preferredCodec);

        // Parse payload types for each codec
        List<String> h264Payloads = new ArrayList<>();
        List<String> vp8Payloads = new ArrayList<>();
        List<String> vp9Payloads = new ArrayList<>();
        List<String> otherPayloads = new ArrayList<>();

        String[] lines = sdp.split("\r\n");

        // First pass: collect payload types for each codec
        for (String line : lines) {
            Matcher m = RTPMAP_PATTERN.matcher(line);
            if (m.find()) {
                String payloadType = m.group(1);
                String codecName = m.group(2).toUpperCase();

                if (codecName.contains("H264")) {
                    h264Payloads.add(payloadType);
                } else if (codecName.contains("VP8")) {
                    vp8Payloads.add(payloadType);
                } else if (codecName.contains("VP9")) {
                    vp9Payloads.add(payloadType);
                }
            }
        }

        // Build ordered payload list based on platform preference
        List<String> orderedPayloads = new ArrayList<>();

        if (IS_WINDOWS) {
            // Windows: H264 → VP8 → VP9
            orderedPayloads.addAll(h264Payloads);
            orderedPayloads.addAll(vp8Payloads);
            orderedPayloads.addAll(vp9Payloads);
        } else {
            // Linux/Mac: VP8 → H264 → VP9
            orderedPayloads.addAll(vp8Payloads);
            orderedPayloads.addAll(h264Payloads);
            orderedPayloads.addAll(vp9Payloads);
        }

        // Second pass: reorder payloads in m=video line
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            if (line.startsWith("m=video")) {
                Matcher m = MLINE_VIDEO_PATTERN.matcher(line);
                if (m.find()) {
                    String originalPayloads = m.group(1);
                    String[] allPayloads = originalPayloads.split("\\s+");

                    // Collect payloads not in our video codec list (RTX, RED, ULPFEC, etc.)
                    for (String pt : allPayloads) {
                        if (!orderedPayloads.contains(pt) && !otherPayloads.contains(pt)) {
                            otherPayloads.add(pt);
                        }
                    }

                    // Build new m= line with reordered payloads
                    StringBuilder newPayloads = new StringBuilder();
                    for (String pt : orderedPayloads) {
                        newPayloads.append(pt).append(" ");
                    }
                    for (String pt : otherPayloads) {
                        newPayloads.append(pt).append(" ");
                    }

                    String newMLine = line.substring(0, line.indexOf(originalPayloads)) +
                            newPayloads.toString().trim();
                    sb.append(newMLine).append("\r\n");

                    System.out.printf("[SDPUtils] Reordered video payloads: %s%n", newPayloads.toString().trim());
                    continue;
                }
            }
            sb.append(line).append("\r\n");
        }

        return sb.toString();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HIGH PROFILE PACKETIZATION FIX: Smart Additive Munging
    // ═══════════════════════════════════════════════════════════════════════════

    // Pattern to detect High Profile (64xxxx) - covers all High Profile variants
    private static final Pattern HIGH_PROFILE_PATTERN = Pattern.compile("profile-level-id=64[0-9a-fA-F]{4}",
            Pattern.CASE_INSENSITIVE);

    // Pattern to detect/replace packetization-mode
    private static final Pattern PACKETIZATION_MODE_PATTERN = Pattern.compile("packetization-mode=(\\d)");

    // Pattern for profile-level-id replacement
    private static final Pattern PROFILE_PATTERN = Pattern.compile("(profile-level-id=)([0-9a-fA-F]{6})",
            Pattern.CASE_INSENSITIVE);

    /**
     * ═══════════════════════════════════════════════════════════════════
     * SMART ADDITIVE MUNGING: Preserve High Profile, Enforce Packetization Mode 1
     * ═══════════════════════════════════════════════════════════════════
     *
     * This method PRESERVES the H.264 High Profile (64xxxx) for better video
     * quality
     * while ENSURING packetization-mode=1 is present for cross-platform
     * compatibility.
     *
     * Unlike enforceBaselineH264Profile() which destructively replaces all
     * profiles,
     * this method only ADDS or FIXES packetization-mode on fmtp lines.
     *
     * Transformations:
     * - High Profile missing mode → append ";packetization-mode=1"
     * - Any profile with mode=0 → change to mode=1
     * - Already has mode=1 → no change
     *
     * @param sdp The SDP to process
     * @return SDP with packetization-mode=1 enforced on all H.264 fmtp lines
     */
    public static String enforceHighProfilePacketization(String sdp) {
        if (sdp == null)
            return null;

        StringBuilder result = new StringBuilder();
        String[] lines = sdp.split("\r\n");
        boolean modified = false;

        for (String line : lines) {
            String processedLine = line;

            // Only process fmtp lines with profile-level-id (H.264 codec parameters)
            if (line.startsWith("a=fmtp:") && line.contains("profile-level-id=")) {

                // Check if packetization-mode exists
                Matcher modeMatcher = PACKETIZATION_MODE_PATTERN.matcher(line);
                if (modeMatcher.find()) {
                    // Mode exists - ensure it's mode=1
                    String currentMode = modeMatcher.group(1);
                    if (!"1".equals(currentMode)) {
                        processedLine = modeMatcher.replaceFirst("packetization-mode=1");
                        System.out.printf("[SDPUtils] PACKETIZATION MODE FIX: %s → 1%n", currentMode);
                        modified = true;
                    }
                } else {
                    // Mode missing - append packetization-mode=1
                    // Find end of fmtp line (before \r\n) and append
                    processedLine = line + ";packetization-mode=1";
                    System.out.println("[SDPUtils] PACKETIZATION MODE ADDED: ;packetization-mode=1");
                    modified = true;
                }
            }

            result.append(processedLine).append("\r\n");
        }

        if (modified) {
            System.out.println("[SDPUtils] Smart High Profile munging applied - quality preserved");
        }

        return result.toString();
    }

    /**
     * ═══════════════════════════════════════════════════════════════════
     * SDP PROFILE MUNGING: The Gatekeeper (STRICT MODE) - Mac Fallback
     * ═══════════════════════════════════════════════════════════════════
     *
     * Forces all H.264 fmtp lines to use STRICTLY Constrained Baseline (42e01f).
     * Also enforces packetization-mode=1 for macOS VideoToolbox compatibility.
     * This prevents the native VideoToolbox encoder from receiving a profile
     * it cannot handle during mid-session renegotiation.
     *
     * CRITICAL: This is the "nuclear option" for Mac safety.
     * Use enforceHighProfilePacketization() first to try preserving High Profile.
     *
     * @param sdp The SDP to process
     * @return SDP with all H.264 profiles normalized to Constrained Baseline
     */
    public static String enforceBaselineH264Profile(String sdp) {
        if (sdp == null)
            return null;

        final String SAFE_PROFILE = "42e01f";

        StringBuilder result = new StringBuilder();
        String[] lines = sdp.split("\r\n");
        boolean profileModified = false;
        boolean packetModeModified = false;

        for (String line : lines) {
            String processedLine = line;

            // Check and fix profile-level-id
            if (processedLine.contains("profile-level-id=")) {
                Matcher m = PROFILE_PATTERN.matcher(processedLine);
                if (m.find()) {
                    String originalProfile = m.group(2).toLowerCase();

                    // Only 42e01f is safe
                    if (!originalProfile.equals(SAFE_PROFILE)) {
                        processedLine = m.replaceFirst("$1" + SAFE_PROFILE);
                        System.out.printf("[SDPUtils] PROFILE MUNGED: %s → %s%n",
                                originalProfile, SAFE_PROFILE);
                        profileModified = true;
                    }
                }
            }

            // Check and fix packetization-mode (Mac requires mode=1)
            if (processedLine.contains("packetization-mode=")) {
                Matcher pm = PACKETIZATION_MODE_PATTERN.matcher(processedLine);
                if (pm.find()) {
                    String originalMode = pm.group(1);
                    if (!"1".equals(originalMode)) {
                        processedLine = pm.replaceFirst("packetization-mode=1");
                        System.out.printf("[SDPUtils] PACKETIZATION MODE MUNGED: %s → 1%n", originalMode);
                        packetModeModified = true;
                    }
                }
            }

            result.append(processedLine).append("\r\n");
        }

        if (profileModified || packetModeModified) {
            System.out.println("[SDPUtils] MAC SAFETY: Enforced 42e01f + packetization-mode=1");
        } else {
            System.out.println("[SDPUtils] SDP already uses safe configuration");
        }

        return result.toString();
    }

    /**
     * Logs a detailed analysis of video codecs present in the SDP.
     * Useful for verifying if dangerous profiles are present.
     */
    public static void logVideoCodecAnalysis(String sdp, String label) {
        if (sdp == null)
            return;

        System.out.printf("%n[SDPUtils] ════ VIDEO CODEC ANALYSIS: %s ════%n", label);

        String[] lines = sdp.split("\r\n");
        Map<String, String> payloadCodecMap = new HashMap<>();

        // First pass: Map payload types to codec names
        for (String line : lines) {
            Matcher m = RTPMAP_PATTERN.matcher(line);
            if (m.find()) {
                payloadCodecMap.put(m.group(1), m.group(2));
            }
        }

        // Second pass: Find m=video line and iterate priorities
        for (String line : lines) {
            if (line.startsWith("m=video")) {
                Matcher m = MLINE_VIDEO_PATTERN.matcher(line);
                if (m.find()) {
                    String[] payloadTypes = m.group(1).split("\\s+");

                    int rank = 1;
                    for (String pt : payloadTypes) {
                        String codec = payloadCodecMap.get(pt);
                        if (codec != null) {
                            // Check for H.264 profile
                            String details = "";
                            if (codec.toUpperCase().contains("H264")) {
                                details = " - " + getH264ProfileDetails(sdp, pt);
                            }

                            System.out.printf("  #%d [%s] %s%s%n", rank++, pt, codec, details);
                        }
                    }
                }
            }
        }
        System.out.println("════════════════════════════════════════════════════════════\n");
    }

    private static String getH264ProfileDetails(String sdp, String payloadType) {
        // Look for fmtp line for this payload
        String[] lines = sdp.split("\r\n");
        for (String line : lines) {
            if (line.startsWith("a=fmtp:" + payloadType)) {
                if (line.contains("profile-level-id=")) {
                    Matcher m = PROFILE_PATTERN.matcher(line);
                    if (m.find()) {
                        String hex = m.group(2);
                        return decodeH264Profile(hex) + " (" + hex + ")";
                    }
                }
            }
        }
        return "Unknown Profile";
    }

    /**
     * Decode H.264 profile byte to human-readable name.
     */
    public static String decodeH264Profile(String profileHex) {
        if (profileHex == null || profileHex.length() < 2)
            return "unknown";

        String profileByte = profileHex.substring(0, 2).toLowerCase();
        return switch (profileByte) {
            case "42" -> "Baseline";
            case "4d" -> "Main";
            case "58" -> "Extended";
            case "64" -> "High";
            case "6e" -> "High 10";
            case "7a" -> "High 4:2:2";
            case "f4" -> "High 4:4:4";
            default -> "unknown";
        };
    }
}
