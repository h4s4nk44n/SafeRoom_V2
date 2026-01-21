package com.saferoom.webrtc;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for SDPUtils High Profile Packetization enforcement.
 * Verifies the Smart Additive Munging logic that preserves High Profile
 * while ensuring packetization-mode=1.
 */
@DisplayName("SDPUtils High Profile Packetization Tests")
public class SDPUtilsTest {

    // Sample SDP with High Profile MISSING packetization-mode
    private static final String SDP_HIGH_PROFILE_NO_MODE = """
            v=0\r
            o=- 123456789 2 IN IP4 127.0.0.1\r
            s=Test SDP\r
            t=0 0\r
            m=video 9 UDP/TLS/RTP/SAVPF 102\r
            a=rtpmap:102 H264/90000\r
            a=fmtp:102 level-asymmetry-allowed=1;profile-level-id=640c1f\r
            a=sendrecv\r
            """;

    // Sample SDP with High Profile and packetization-mode=0
    private static final String SDP_HIGH_PROFILE_MODE_0 = """
            v=0\r
            o=- 123456789 2 IN IP4 127.0.0.1\r
            s=Test SDP\r
            t=0 0\r
            m=video 9 UDP/TLS/RTP/SAVPF 102\r
            a=rtpmap:102 H264/90000\r
            a=fmtp:102 packetization-mode=0;profile-level-id=640c1f\r
            a=sendrecv\r
            """;

    // Sample SDP with High Profile and packetization-mode=1 (already correct)
    private static final String SDP_HIGH_PROFILE_MODE_1 = """
            v=0\r
            o=- 123456789 2 IN IP4 127.0.0.1\r
            s=Test SDP\r
            t=0 0\r
            m=video 9 UDP/TLS/RTP/SAVPF 102\r
            a=rtpmap:102 H264/90000\r
            a=fmtp:102 packetization-mode=1;profile-level-id=640c1f\r
            a=sendrecv\r
            """;

    // Sample SDP with Baseline profile
    private static final String SDP_BASELINE = """
            v=0\r
            o=- 123456789 2 IN IP4 127.0.0.1\r
            s=Test SDP\r
            t=0 0\r
            m=video 9 UDP/TLS/RTP/SAVPF 102\r
            a=rtpmap:102 H264/90000\r
            a=fmtp:102 packetization-mode=1;profile-level-id=42e01f\r
            a=sendrecv\r
            """;

    @Test
    @DisplayName("Should append packetization-mode=1 when missing from High Profile")
    void testAddPacketizationModeWhenMissing() {
        String result = SDPUtils.enforceHighProfilePacketization(SDP_HIGH_PROFILE_NO_MODE);

        // Should have appended packetization-mode=1
        assertTrue(result.contains(";packetization-mode=1"),
                "Should append packetization-mode=1 when missing");
        // Should preserve High Profile
        assertTrue(result.contains("profile-level-id=640c1f"),
                "Should preserve High Profile ID (quality preserved)");
    }

    @Test
    @DisplayName("Should change packetization-mode=0 to mode=1")
    void testChangePacketizationModeFromZeroToOne() {
        String result = SDPUtils.enforceHighProfilePacketization(SDP_HIGH_PROFILE_MODE_0);

        // Should have mode=1, not mode=0
        assertTrue(result.contains("packetization-mode=1"),
                "Should have packetization-mode=1");
        assertFalse(result.contains("packetization-mode=0"),
                "Should NOT have packetization-mode=0");
        // Should preserve High Profile
        assertTrue(result.contains("profile-level-id=640c1f"),
                "Should preserve High Profile ID");
    }

    @Test
    @DisplayName("Should not modify SDP already having packetization-mode=1")
    void testNoChangeWhenAlreadyCorrect() {
        String result = SDPUtils.enforceHighProfilePacketization(SDP_HIGH_PROFILE_MODE_1);

        // Should be unchanged (or at least structurally identical)
        assertTrue(result.contains("packetization-mode=1;profile-level-id=640c1f"),
                "Should maintain existing mode=1");
        // Count occurrences - should not have duplicates
        int modeCount = result.split("packetization-mode=1").length - 1;
        assertEquals(1, modeCount, "Should have exactly one packetization-mode=1");
    }

    @Test
    @DisplayName("Should not corrupt Baseline profile lines")
    void testBaselineProfileUnchanged() {
        String result = SDPUtils.enforceHighProfilePacketization(SDP_BASELINE);

        // Should preserve baseline profile
        assertTrue(result.contains("profile-level-id=42e01f"),
                "Should preserve Baseline profile");
    }

    @Test
    @DisplayName("Should handle null SDP gracefully")
    void testNullSDP() {
        String result = SDPUtils.enforceHighProfilePacketization(null);
        assertNull(result, "Should return null for null input");
    }

    @Test
    @DisplayName("Baseline enforcement should convert High to Baseline")
    void testBaselineEnforcementConvertsHighProfile() {
        String result = SDPUtils.enforceBaselineH264Profile(SDP_HIGH_PROFILE_MODE_1);

        // Should convert to baseline
        assertTrue(result.contains("profile-level-id=42e01f"),
                "Should convert to Constrained Baseline");
        assertFalse(result.contains("profile-level-id=640c1f"),
                "Should remove High Profile");
    }
}
