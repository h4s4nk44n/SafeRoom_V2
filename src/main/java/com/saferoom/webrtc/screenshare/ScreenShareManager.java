package com.saferoom.webrtc.screenshare;

import com.saferoom.system.PlatformDetector;
import dev.onvoid.webrtc.CreateSessionDescriptionObserver;
import dev.onvoid.webrtc.PeerConnectionFactory;
import dev.onvoid.webrtc.RTCOfferOptions;
import dev.onvoid.webrtc.RTCRtpSender;
import dev.onvoid.webrtc.RTCSessionDescription;
import dev.onvoid.webrtc.SetSessionDescriptionObserver;
import dev.onvoid.webrtc.media.video.VideoDesktopSource;
import dev.onvoid.webrtc.media.video.VideoTrack;
import dev.onvoid.webrtc.media.video.desktop.DesktopSource;
import dev.onvoid.webrtc.media.video.desktop.ScreenCapturer;
import dev.onvoid.webrtc.media.video.desktop.WindowCapturer;
import dev.onvoid.webrtc.RTCPeerConnection;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Platform-aware screen share orchestrator. Uses native dev.onvoid WebRTC 
 * VideoDesktopSource for Windows/macOS. Linux screen share is currently disabled
 * pending migration to a native capture solution.
 * 
 * NOTE: Linux screen share via FFmpeg has been removed to reduce dependency bloat.
 */
public final class ScreenShareManager implements AutoCloseable {

    private static final Logger LOGGER = Logger.getLogger(ScreenShareManager.class.getName());
    private static final List<String> SCREEN_SHARE_STREAM_IDS =
        Collections.singletonList("screen-share-stream");

    private final PeerConnectionFactory factory;
    private final RTCPeerConnection peerConnection;
    private final RenegotiationHandler renegotiationHandler;
    private final ExecutorService asyncExecutor;
    private final Object stateLock = new Object();

    private volatile boolean sharing;

    private VideoDesktopSource desktopSource;
    private VideoTrack screenTrack;
    private RTCRtpSender screenSender;
    private RTCRtpSender cameraSender;
    private VideoTrack cameraTrack;
    private boolean usingCameraSender;

    public ScreenShareManager(PeerConnectionFactory factory,
                              RTCPeerConnection peerConnection,
                              RenegotiationHandler renegotiationHandler) {
        this.factory = Objects.requireNonNull(factory, "factory");
        this.peerConnection = Objects.requireNonNull(peerConnection, "peerConnection");
        this.renegotiationHandler = Objects.requireNonNull(renegotiationHandler, "renegotiationHandler");
        this.asyncExecutor = Executors.newThreadPerTaskExecutor(
            Thread.ofVirtual().name("screen-share-task-", 0).factory());
    }

    /**
     * Starts screen sharing for the current platform.
     * NOTE: Linux screen share is currently disabled. Only Windows/macOS are supported.
     */
    public CompletableFuture<Void> startScreenShare() {
        return startScreenShare(ScreenSourceOption.auto());
    }

    /**
     * Checks if screen sharing is supported on the current platform.
     * @return true if supported (Windows/macOS), false if not (Linux)
     */
    public static boolean isScreenShareSupported() {
        return !PlatformDetector.isLinux();
    }

    public CompletableFuture<Void> startScreenShare(ScreenSourceOption sourceOption) {
        // Linux screen share is disabled - reject early with clear message
        if (PlatformDetector.isLinux()) {
            LOGGER.warning("[ScreenShareManager] Screen sharing is temporarily disabled on Linux");
            return CompletableFuture.failedFuture(
                new UnsupportedOperationException("Screen sharing is temporarily disabled on Linux. " +
                    "This feature requires native capture support which is not yet available."));
        }

        ScreenSourceOption effectiveOption = sourceOption != null ? sourceOption : ScreenSourceOption.auto();
        return CompletableFuture.supplyAsync(() -> {
            synchronized (stateLock) {
                if (sharing) {
                    throw new IllegalStateException("Screen sharing already active");
                }
                sharing = true;
            }
            try {
                startNativeCapture(effectiveOption);
            } catch (Exception ex) {
                synchronized (stateLock) {
                    sharing = false;
                }
                cleanupCapture();
                throw new CompletionException(ex);
            }
            return null;
        }, asyncExecutor).thenCompose(ignored -> renegotiate(ScreenShareEvent.STARTED));
    }

    /**
     * Stops screen sharing, removes the track from the peer connection and renegotiates.
     */
    public CompletableFuture<Void> stopScreenShare() {
        boolean shouldStop;
        synchronized (stateLock) {
            shouldStop = sharing;
            sharing = false;
        }
        if (!shouldStop) {
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.runAsync(() -> {
            detachTrack();
            cleanupCapture();
        }, asyncExecutor).thenCompose(ignored -> renegotiate(ScreenShareEvent.STOPPED));
    }

    public boolean isScreenShareActive() {
        return sharing;
    }

    private void startNativeCapture(ScreenSourceOption option) throws Exception {
        LOGGER.info("[ScreenShareManager] Starting native desktop capture");
        DesktopSourceSelection selection = resolveDesktopSource(option);

        desktopSource = new VideoDesktopSource();
        desktopSource.setFrameRate(30);
        desktopSource.setMaxFrameSize(1920, 1080);
        desktopSource.setSourceId(selection.source().id, selection.window());
        desktopSource.start();

        String trackId = "screen-share-native-" + System.currentTimeMillis();
        screenTrack = factory.createVideoTrack(trackId, desktopSource);
        screenTrack.setEnabled(true);

        publishTrack(screenTrack);
        LOGGER.info("[ScreenShareManager] Native screen share track attached");
    }

    private CompletableFuture<Void> renegotiate(ScreenShareEvent event) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        RTCOfferOptions options = new RTCOfferOptions();

        peerConnection.createOffer(options, new CreateSessionDescriptionObserver() {
            @Override
            public void onSuccess(RTCSessionDescription description) {
                peerConnection.setLocalDescription(description, new SetSessionDescriptionObserver() {
                    @Override
                    public void onSuccess() {
                        try {
                            if (event == ScreenShareEvent.STARTED) {
                                renegotiationHandler.onScreenShareOffer(description.sdp);
                            } else {
                                renegotiationHandler.onScreenShareStopped(description.sdp);
                            }
                            future.complete(null);
                        } catch (Exception ex) {
                            future.completeExceptionally(ex);
                        }
                    }

                    @Override
                    public void onFailure(String error) {
                        future.completeExceptionally(new IllegalStateException(error));
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                future.completeExceptionally(new IllegalStateException(error));
            }
        });

        return future;
    }

    public void registerCameraSource(RTCRtpSender sender, VideoTrack track) {
        this.cameraSender = sender;
        this.cameraTrack = track;
    }

    private void publishTrack(VideoTrack track) throws Exception {
        if (cameraSender != null && cameraTrack != null) {
            LOGGER.info("[ScreenShareManager] Replacing camera track with screen share");
            cameraSender.replaceTrack(track);
            usingCameraSender = true;
            screenSender = cameraSender;
            return;
        }

        screenSender = peerConnection.addTrack(track, SCREEN_SHARE_STREAM_IDS);
        usingCameraSender = false;
        if (screenSender == null) {
            throw new IllegalStateException("PeerConnection rejected screen share track");
        }
    }

    private void detachTrack() {
        if (usingCameraSender && cameraSender != null && cameraTrack != null) {
            try {
                cameraSender.replaceTrack(cameraTrack);
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "[ScreenShareManager] Failed to restore camera track", ex);
            } finally {
                usingCameraSender = false;
                screenSender = null;
            }
            return;
        }

        if (screenSender != null) {
            try {
                peerConnection.removeTrack(screenSender);
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "[ScreenShareManager] Failed to remove screen track", ex);
            } finally {
                screenSender = null;
            }
        }
    }

    private void cleanupCapture() {
        if (desktopSource != null) {
            try {
                desktopSource.stop();
            } catch (Exception ex) {
                LOGGER.log(Level.FINE, "[ScreenShareManager] Error stopping desktop source", ex);
            }
            try {
                desktopSource.dispose();
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "[ScreenShareManager] Failed to dispose desktop source", ex);
            } finally {
                desktopSource = null;
            }
        }

        if (screenTrack != null) {
            try {
                screenTrack.setEnabled(false);
                screenTrack.dispose();
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "[ScreenShareManager] Failed to dispose screen track", ex);
            } finally {
                screenTrack = null;
            }
        }
    }

    private DesktopSourceSelection resolveDesktopSource(ScreenSourceOption option) throws Exception {
        if (option.kind() == ScreenSourceOption.Kind.AUTO) {
            return resolveDefaultDesktopSource();
        }
        DesktopSource desktopSource = option.desktopSource();
        if (desktopSource == null) {
            throw new IllegalArgumentException("Desktop source cannot be null for native capture");
        }
        return new DesktopSourceSelection(desktopSource, option.isWindow());
    }

    private DesktopSourceSelection resolveDefaultDesktopSource() throws Exception {
        DesktopSource screen = tryEnumerateScreens();
        if (screen != null) {
            return new DesktopSourceSelection(screen, false);
        }
        DesktopSource window = tryEnumerateWindows();
        if (window != null) {
            return new DesktopSourceSelection(window, true);
        }
        throw new IllegalStateException("No desktop sources are available for capture");
    }

    private DesktopSource tryEnumerateScreens() {
        ScreenCapturer capturer = null;
        try {
            capturer = new ScreenCapturer();
            List<DesktopSource> sources = capturer.getDesktopSources();
            return (sources != null && !sources.isEmpty()) ? sources.get(0) : null;
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING, "[ScreenShareManager] Failed to enumerate screens", t);
            return null;
        } finally {
            if (capturer != null) {
                try {
                    capturer.dispose();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    private DesktopSource tryEnumerateWindows() {
        WindowCapturer capturer = null;
        try {
            capturer = new WindowCapturer();
            List<DesktopSource> sources = capturer.getDesktopSources();
            return (sources != null && !sources.isEmpty()) ? sources.get(0) : null;
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING, "[ScreenShareManager] Failed to enumerate windows", t);
            return null;
        } finally {
            if (capturer != null) {
                try {
                    capturer.dispose();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    @Override
    public void close() {
        stopScreenShare().exceptionally(ex -> {
            LOGGER.log(Level.WARNING, "[ScreenShareManager] Error while stopping during close", ex);
            return null;
        }).join();
        asyncExecutor.shutdown();
    }

    private enum ScreenShareEvent {
        STARTED,
        STOPPED
    }

    private record DesktopSourceSelection(DesktopSource source, boolean window) {
    }

    /**
     * Callback interface so callers can forward the renegotiated SDP to their signaling layer.
     */
    public interface RenegotiationHandler {
        void onScreenShareOffer(String sdp);

        void onScreenShareStopped(String sdp);
    }
}

