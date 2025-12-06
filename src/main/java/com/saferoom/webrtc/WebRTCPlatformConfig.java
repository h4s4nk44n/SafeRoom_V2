package com.saferoom.webrtc;

import dev.onvoid.webrtc.PeerConnectionFactory;
import dev.onvoid.webrtc.RTCPeerConnection;
import dev.onvoid.webrtc.RTCRtpCapabilities;
import dev.onvoid.webrtc.RTCRtpCodecCapability;
import dev.onvoid.webrtc.RTCRtpSender;
import dev.onvoid.webrtc.RTCRtpTransceiver;
import dev.onvoid.webrtc.media.MediaStreamTrack;
import dev.onvoid.webrtc.media.MediaType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Platform-specific WebRTC tuning.
 * 
 * - macOS: Prioritize H264 for VideoToolbox hardware acceleration
 * - Windows: Prioritize H264 for Intel QuickSync / NVIDIA NVENC / AMD VCE
 * - Linux: Use VP8/VP9 (better software encoding support)
 */
final class WebRTCPlatformConfig {

    private static final WebRTCPlatformConfig EMPTY =
        new WebRTCPlatformConfig(false, false, List.of(), "Unknown");

    private final boolean preferH264;
    private final boolean isWindows;
    private final List<RTCRtpCodecCapability> videoCodecPreferences;
    private final String platformName;

    private WebRTCPlatformConfig(boolean preferH264, boolean isWindows,
                                 List<RTCRtpCodecCapability> videoCodecPreferences,
                                 String platformName) {
        this.preferH264 = preferH264;
        this.isWindows = isWindows;
        this.videoCodecPreferences = videoCodecPreferences;
        this.platformName = platformName;
    }

    static WebRTCPlatformConfig detect(PeerConnectionFactory factory) {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        boolean mac = os.contains("mac");
        boolean windows = os.contains("win");
        boolean linux = os.contains("linux");
        
        String platformName = mac ? "macOS" : windows ? "Windows" : linux ? "Linux" : "Unknown";
        System.out.printf("[WebRTC] Platform detected: %s%n", platformName);
        
        if (factory == null) {
            System.out.println("[WebRTC] Factory null - using default codec order");
            return new WebRTCPlatformConfig(false, windows, List.of(), platformName);
        }

        RTCRtpCapabilities senderCaps = factory.getRtpSenderCapabilities(MediaType.VIDEO);
        
        // ═══════════════════════════════════════════════════════════════
        // CROSS-PLATFORM FIX: Force VP8 for Linux <-> Windows compatibility
        // 
        // H.264 has format issues between platforms:
        // - Windows (Media Foundation): Uses AVCC format (length prefix)
        // - Linux (FFmpeg): Expects Annex B format (start codes)
        // - SPS/PPS may be sent out-of-band on one platform, in-band on other
        //
        // VP8 is a pure software codec with NO format ambiguity
        // It works identically on all platforms
        // ═══════════════════════════════════════════════════════════════
        
        List<RTCRtpCodecCapability> sorted = reorderCodecs(senderCaps, false); // VP8 first!
        
        if (!sorted.isEmpty()) {
            System.out.printf("[WebRTC] %s → VP8 preferred (cross-platform compatibility)%n", platformName);
            System.out.println("[WebRTC] Codec priority (VP8 first to avoid H.264 format issues):");
            for (int i = 0; i < Math.min(5, sorted.size()); i++) {
                RTCRtpCodecCapability codec = sorted.get(i);
                if (codec != null && codec.getName() != null) {
                    System.out.printf("  [%d] %s%n", i + 1, codec.getName());
                }
            }
        } else {
            System.out.printf("[WebRTC] %s detected but codec capabilities unavailable%n", platformName);
        }
        
        return new WebRTCPlatformConfig(false, windows, sorted, platformName);
    }

    static WebRTCPlatformConfig empty() {
        return EMPTY;
    }
    
    private static void printAvailableCodecs(List<RTCRtpCodecCapability> codecs) {
        if (codecs.isEmpty()) return;
        System.out.println("[WebRTC] Available video codecs (priority order):");
        for (int i = 0; i < Math.min(5, codecs.size()); i++) {
            RTCRtpCodecCapability codec = codecs.get(i);
            System.out.printf("  [%d] %s%n", i + 1, codec.getName());
        }
    }

    void applyVideoCodecPreferences(RTCPeerConnection peerConnection) {
        if (peerConnection == null || videoCodecPreferences.isEmpty()) {
            System.out.println("[WebRTC] Skipping codec preferences (empty or no connection)");
            return;
        }
        
        RTCRtpTransceiver[] transceivers = peerConnection.getTransceivers();
        if (transceivers == null || transceivers.length == 0) {
            System.out.println("[WebRTC] No transceivers found for codec configuration");
            return;
        }
        
        int applied = 0;
        for (RTCRtpTransceiver transceiver : transceivers) {
            if (transceiver == null) continue;
            
            RTCRtpSender sender = transceiver.getSender();
            if (sender == null) continue;
            
            MediaStreamTrack track = sender.getTrack();
            if (track == null || !"video".equalsIgnoreCase(track.getKind())) continue;
            
            try {
                transceiver.setCodecPreferences(videoCodecPreferences);
                applied++;
                System.out.printf("[WebRTC] Codec preferences applied to transceiver (track: %s)%n", 
                    track.getId());
            } catch (Exception ex) {
                System.err.printf("[WebRTC] Failed to set codec preferences: %s%n", ex.getMessage());
            }
        }
        
        if (applied > 0) {
            System.out.printf("[WebRTC] Codec preferences applied to %d transceiver(s) on %s%n", 
                applied, platformName);
        }
    }

    /**
     * Reorder codecs based on platform preference.
     * @param capabilities RTP capabilities from factory
     * @param preferH264 true to prioritize H264 (macOS/Windows), false for VP8/VP9 (Linux)
     */
    private static List<RTCRtpCodecCapability> reorderCodecs(RTCRtpCapabilities capabilities, 
                                                              boolean preferH264) {
        if (capabilities == null || capabilities.getCodecs() == null) {
            return List.of();
        }
        
        List<RTCRtpCodecCapability> codecs = new ArrayList<>(
            capabilities.getCodecs().stream()
                .filter(Objects::nonNull)
                .toList()
        );
        
        if (codecs.isEmpty()) {
            return List.of();
        }

        Predicate<RTCRtpCodecCapability> isH264 = codec -> {
            String name = codec.getName();
            return name != null && name.toUpperCase(Locale.ROOT).contains("H264");
        };
        
        Predicate<RTCRtpCodecCapability> isVP8 = codec -> {
            String name = codec.getName();
            return name != null && name.toUpperCase(Locale.ROOT).contains("VP8");
        };
        
        Predicate<RTCRtpCodecCapability> isVP9 = codec -> {
            String name = codec.getName();
            return name != null && name.toUpperCase(Locale.ROOT).contains("VP9");
        };

        if (preferH264) {
            // H264 first, then VP8, then VP9, then others
            codecs.sort((a, b) -> {
                int scoreA = isH264.test(a) ? 3 : isVP8.test(a) ? 2 : isVP9.test(a) ? 1 : 0;
                int scoreB = isH264.test(b) ? 3 : isVP8.test(b) ? 2 : isVP9.test(b) ? 1 : 0;
                return Integer.compare(scoreB, scoreA);
            });
        } else {
            // VP8 first, then VP9, then H264, then others
            codecs.sort((a, b) -> {
                int scoreA = isVP8.test(a) ? 3 : isVP9.test(a) ? 2 : isH264.test(a) ? 1 : 0;
                int scoreB = isVP8.test(b) ? 3 : isVP9.test(b) ? 2 : isH264.test(b) ? 1 : 0;
                return Integer.compare(scoreB, scoreA);
            });
        }
        
        return Collections.unmodifiableList(codecs);
    }
    
    @Override
    public String toString() {
        return String.format("WebRTCPlatformConfig[platform=%s, preferH264=%b, codecs=%d]",
            platformName, preferH264, videoCodecPreferences.size());
    }
}

