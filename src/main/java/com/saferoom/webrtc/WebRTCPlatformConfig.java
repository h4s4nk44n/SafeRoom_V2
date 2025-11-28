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
 * Platform-specific WebRTC tuning. On macOS we prioritize H264 so that
 * VideoToolbox is selected automatically by libwebrtc.
 */
final class WebRTCPlatformConfig {

    private static final WebRTCPlatformConfig EMPTY =
        new WebRTCPlatformConfig(false, List.of());

    private final boolean preferH264;
    private final List<RTCRtpCodecCapability> videoCodecPreferences;

    private WebRTCPlatformConfig(boolean preferH264,
                                 List<RTCRtpCodecCapability> videoCodecPreferences) {
        this.preferH264 = preferH264;
        this.videoCodecPreferences = videoCodecPreferences;
    }

    static WebRTCPlatformConfig detect(PeerConnectionFactory factory) {
        boolean mac = isMacOs();
        if (!mac || factory == null) {
            return mac ? new WebRTCPlatformConfig(true, List.of()) : EMPTY;
        }

        RTCRtpCapabilities senderCaps = factory.getRtpSenderCapabilities(MediaType.VIDEO);
        List<RTCRtpCodecCapability> sorted = reorderCodecs(senderCaps);
        if (!sorted.isEmpty()) {
            System.out.printf("[WebRTC] macOS detected → prioritizing %s for hardware accel%n",
                sorted.get(0).getName());
        } else {
            System.out.println("[WebRTC] macOS detected but codec capabilities unavailable");
        }
        return new WebRTCPlatformConfig(true, sorted);
    }

    static WebRTCPlatformConfig empty() {
        return EMPTY;
    }

    void applyVideoCodecPreferences(RTCPeerConnection peerConnection) {
        if (!preferH264 || peerConnection == null || videoCodecPreferences.isEmpty()) {
            return;
        }
        RTCRtpTransceiver[] transceivers = peerConnection.getTransceivers();
        if (transceivers == null || transceivers.length == 0) {
            return;
        }
        for (RTCRtpTransceiver transceiver : transceivers) {
            if (transceiver == null) {
                continue;
            }
            RTCRtpSender sender = transceiver.getSender();
            if (sender == null) {
                continue;
            }
            MediaStreamTrack track = sender.getTrack();
            if (track == null || !"video".equalsIgnoreCase(track.getKind())) {
                continue;
            }
            try {
                transceiver.setCodecPreferences(videoCodecPreferences);
            } catch (Exception ex) {
                System.err.printf("[WebRTC] Failed to set codec preferences: %s%n",
                    ex.getMessage());
            }
        }
    }

    private static List<RTCRtpCodecCapability> reorderCodecs(RTCRtpCapabilities capabilities) {
        if (capabilities == null || capabilities.getCodecs() == null) {
            return List.of();
        }
        List<RTCRtpCodecCapability> codecs =
            new ArrayList<>(capabilities.getCodecs().stream()
                .filter(Objects::nonNull)
                .toList());
        if (codecs.isEmpty()) {
            return List.of();
        }

        Predicate<RTCRtpCodecCapability> isH264 = codec -> {
            String name = codec.getName();
            return name != null && name.toUpperCase(Locale.ROOT).startsWith("H264");
        };

        codecs.sort((a, b) -> {
            boolean aH264 = isH264.test(a);
            boolean bH264 = isH264.test(b);
            if (aH264 == bH264) {
                return 0;
            }
            return aH264 ? -1 : 1;
        });
        return Collections.unmodifiableList(codecs);
    }

    private static boolean isMacOs() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        return os.contains("mac");
    }
}

