package com.saferoom.webrtc;

/**
 * Simple heuristic that lowers capture resolution/FPS and bitrate as more peers join
 * a mesh call. This keeps CPU/GPU load manageable on macOS.
 */
final class AdaptiveCapturePolicy {

    CameraCaptureService.CaptureProfile selectProfile(int participantCount) {
        if (participantCount >= 4) {
            return new CameraCaptureService.CaptureProfile(480, 270, 20);
        }
        if (participantCount == 3) {
            return new CameraCaptureService.CaptureProfile(640, 360, 24);
        }
        return new CameraCaptureService.CaptureProfile(960, 540, 30);
    }

    int targetBitrateKbps(int participantCount) {
        if (participantCount >= 4) {
            return 700;
        }
        if (participantCount == 3) {
            return 900;
        }
        return 1500;
    }
}

