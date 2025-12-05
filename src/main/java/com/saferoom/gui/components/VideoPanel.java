package com.saferoom.gui.components;

import com.saferoom.webrtc.pipeline.FrameProcessor;
import com.saferoom.webrtc.pipeline.FrameRenderResult;
import dev.onvoid.webrtc.media.video.VideoTrack;
import dev.onvoid.webrtc.media.video.VideoTrackSink;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.nio.IntBuffer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Video rendering panel for WebRTC video tracks.
 * Heavy decode/convert work is executed on virtual threads; the FX Application Thread only paints.
 */
public class VideoPanel extends Canvas {
    
    private static final PixelFormat<IntBuffer> ARGB_FORMAT = PixelFormat.getIntArgbPreInstance();
    
    private final GraphicsContext gc;
    private final AtomicReference<FrameRenderResult> latestFrame = new AtomicReference<>();
    private final AnimationTimer animationTimer;
    
    private WritableImage videoImage;
    private VideoTrack videoTrack;
    private VideoTrackSink videoSink;
    private FrameProcessor frameProcessor;
    private boolean isActive = false;
    private boolean animationRunning = false;
    private volatile boolean renderingPaused = false;
    
    /**
     * Constructor
     * @param width Canvas width
     * @param height Canvas height
     */
    // Debug: rendered frame counter
    private volatile long renderedCount = 0;
    private volatile long lastRenderedLog = 0;
    
    public VideoPanel(double width, double height) {
        super(width, height);
        this.gc = getGraphicsContext2D();
        this.animationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (!isActive || renderingPaused) {
                    return;
                }
                FrameRenderResult frame = latestFrame.getAndSet(null);
                if (frame != null) {
                    try {
                        paintFrame(frame);
                        lastFrameTimestamp = System.nanoTime();
                        
                        // Log rendered frames
                        renderedCount++;
                        if (renderedCount - lastRenderedLog >= 100) {
                            System.out.printf("[VideoPanel] ✅ RENDERED %d frames (%dx%d)%n",
                                renderedCount, frame.getWidth(), frame.getHeight());
                            lastRenderedLog = renderedCount;
                        }
                    } finally {
                        frame.release();
                    }
                } else if (System.nanoTime() - lastFrameTimestamp > FREEZE_DETECTION_NANOS) {
                    handleStall();
                }
            }
        };
        
        // Draw placeholder
        drawPlaceholder("No Video");
    }
    
    // Frame counter for debugging
    private volatile long frameCount = 0;
    private volatile long lastFrameCountLog = 0;
    
    /**
     * Attach a video track to this panel for rendering
     */
    public void attachVideoTrack(VideoTrack track) {
        if (track == null) {
            System.err.println("[VideoPanel] Cannot attach null video track");
            return;
        }
        
        System.out.println("[VideoPanel] ═══════════════════════════════════════════");
        System.out.printf("[VideoPanel] Attaching video track: %s (enabled=%b)%n", 
            track.getId(), track.isEnabled());
        
        detachVideoTrack();
        this.renderingPaused = false;
        this.videoTrack = track;
        this.isActive = true;
        this.frameCount = 0;

        frameProcessor = buildFrameProcessor();
        videoSink = frame -> {
            frameCount++;
            // Log every 100 frames to show video is being received
            if (frameCount - lastFrameCountLog >= 100) {
                System.out.printf("[VideoPanel] Received %d frames (size: %dx%d)%n", 
                    frameCount, frame.buffer.getWidth(), frame.buffer.getHeight());
                lastFrameCountLog = frameCount;
            }
            FrameProcessor processor = frameProcessor;
            if (processor != null) {
                processor.submit(frame);
            }
        };

        track.addSink(videoSink);
        startAnimation();
        System.out.printf("[VideoPanel] ✅ Video sink attached, waiting for frames...%n");
        System.out.println("[VideoPanel] ═══════════════════════════════════════════");
    }
    
    /**
     * Detach current video track
     */
    public void detachVideoTrack() {
        stopAnimation();
        renderingPaused = false;
        
        if (videoTrack != null && videoSink != null) {
            try {
                videoTrack.removeSink(videoSink);
                System.out.println("[VideoPanel] Video track detached");
            } catch (Exception e) {
                System.err.println("[VideoPanel] Error detaching video: " + e.getMessage());
            }
        }
        
        closeFrameProcessor();
        
        clearLatestFrame();
        videoTrack = null;
        videoSink = null;
        isActive = false;
        
        Platform.runLater(() -> drawPlaceholder("No Video"));
    }
    
    private void paintFrame(FrameRenderResult frame) {
        int width = frame.getWidth();
        int height = frame.getHeight();
        
        if (videoImage == null || videoImage.getWidth() != width || videoImage.getHeight() != height) {
            videoImage = new WritableImage(width, height);
        }
        
        PixelWriter pixelWriter = videoImage.getPixelWriter();
        pixelWriter.setPixels(0, 0, width, height, ARGB_FORMAT, frame.getArgbPixels(), 0, width);
        
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, getWidth(), getHeight());
        
        double canvasWidth = getWidth();
        double canvasHeight = getHeight();
        double videoAspect = (double) width / height;
        double canvasAspect = canvasWidth / canvasHeight;
        
        double drawWidth;
        double drawHeight;
        double drawX;
        double drawY;
        
        if (videoAspect > canvasAspect) {
            drawWidth = canvasWidth;
            drawHeight = canvasWidth / videoAspect;
            drawX = 0;
            drawY = (canvasHeight - drawHeight) / 2;
        } else {
            drawHeight = canvasHeight;
            drawWidth = canvasHeight * videoAspect;
            drawX = (canvasWidth - drawWidth) / 2;
            drawY = 0;
        }
        
        gc.drawImage(videoImage, drawX, drawY, drawWidth, drawHeight);
    }
    
    /**
     * Draw placeholder text when no video available
     */
    private void drawPlaceholder(String text) {
        // Black background (professional look)
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, getWidth(), getHeight());
        
        // Light gray text
        gc.setFill(Color.rgb(180, 180, 180));
        gc.setFont(javafx.scene.text.Font.font("System", 14));
        
        // Measure text for centering
        javafx.scene.text.Text tempText = new javafx.scene.text.Text(text);
        tempText.setFont(gc.getFont());
        double textWidth = tempText.getLayoutBounds().getWidth();
        double textHeight = tempText.getLayoutBounds().getHeight();
        
        gc.fillText(text, 
            (getWidth() - textWidth) / 2, 
            (getHeight() + textHeight) / 2);
    }
    
    /**
     * Check if video is active
     */
    public boolean isActive() {
        return isActive;
    }
    
    /**
     * Cleanup resources
     */
    public void dispose() {
        detachVideoTrack();
        videoImage = null;
    }

    public void pauseRendering() {
        renderingPaused = true;
        clearLatestFrame();
        if (frameProcessor != null) {
            frameProcessor.pause();
        }
    }

    public void resumeRendering() {
        renderingPaused = false;
        if (frameProcessor != null) {
            frameProcessor.resume();
        }
    }

    private void clearLatestFrame() {
        FrameRenderResult pending = latestFrame.getAndSet(null);
        if (pending != null) {
            pending.release();
        }
    }

    // Debug counter for frames set to latestFrame
    private volatile long framesSetToLatest = 0;
    
    private FrameProcessor buildFrameProcessor() {
        lastFrameTimestamp = System.nanoTime();
        System.out.println("[VideoPanel] Building new FrameProcessor...");
        return new FrameProcessor(result -> {
            framesSetToLatest++;
            // Log every 100 frames set to latestFrame
            if (framesSetToLatest % 100 == 0) {
                System.out.printf("[VideoPanel] 📦 Set %d frames to latestFrame%n", framesSetToLatest);
            }
            latestFrame.set(result);
        });
    }

    private void closeFrameProcessor() {
        if (frameProcessor != null) {
            frameProcessor.close();
            frameProcessor = null;
        }
    }

    private void handleStall() {
        System.err.printf("[VideoPanel] ⚠️ No frame rendered for 2s (received=%d, setToLatest=%d, rendered=%d)%n",
            frameCount, framesSetToLatest, renderedCount);
        System.err.printf("[VideoPanel]   isActive=%b, renderingPaused=%b, animationRunning=%b%n",
            isActive, renderingPaused, animationRunning);
        
        clearLatestFrame();
        closeFrameProcessor();
        frameProcessor = buildFrameProcessor();
        if (videoTrack != null && videoSink != null) {
            System.out.println("[VideoPanel] Re-attaching video sink...");
            videoTrack.removeSink(videoSink);
            videoTrack.addSink(videoSink);
        }
        lastFrameTimestamp = System.nanoTime();
    }

    private static final long FREEZE_DETECTION_NANOS = TimeUnit.MILLISECONDS.toNanos(2000);
    private volatile long lastFrameTimestamp = System.nanoTime();

    private void startAnimation() {
        if (!animationRunning) {
            System.out.println("[VideoPanel] 🎬 Starting AnimationTimer on FX thread");
            animationTimer.start();
            animationRunning = true;
        } else {
            System.out.println("[VideoPanel] AnimationTimer already running");
        }
    }

    private void stopAnimation() {
        if (animationRunning) {
            System.out.println("[VideoPanel] ⏹️ Stopping AnimationTimer");
            animationTimer.stop();
            animationRunning = false;
        }
    }
}
