package com.saferoom.gui.components;

import dev.onvoid.webrtc.media.video.*;
import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Video rendering panel for WebRTC video tracks.
 * Displays both local and remote video streams on JavaFX Canvas.
 * 
 * THREAD-SAFE: Uses atomic flags to prevent UI thread flooding.
 */
public class VideoPanel extends Canvas {
    
    private final GraphicsContext gc;
    private WritableImage videoImage;
    private VideoTrack videoTrack;
    private VideoTrackSink videoSink;
    private boolean isActive = false;
    
    // ===== THREADING FIX: Prevent UI thread flooding =====
    private final AtomicBoolean isRendering = new AtomicBoolean(false);
    private volatile VideoFrame pendingFrame = null;
    
    /**
     * Constructor
     * @param width Canvas width
     * @param height Canvas height
     */
    public VideoPanel(double width, double height) {
        super(width, height);
        this.gc = getGraphicsContext2D();
        
        // Draw placeholder
        drawPlaceholder("No Video");
    }
    
    /**
     * Attach a video track to this panel for rendering
     */
    public void attachVideoTrack(VideoTrack track) {
        if (track == null) {
            System.err.println("[VideoPanel] Cannot attach null video track");
            return;
        }
        
        System.out.println("[VideoPanel] Attaching video track: " + track.getId());
        
        // Remove previous sink if exists
        detachVideoTrack();
        
        this.videoTrack = track;
        this.isActive = true;
        
        // Create video sink to receive frames
        this.videoSink = new VideoTrackSink() {
            @Override
            public void onVideoFrame(VideoFrame frame) {
                renderFrame(frame);
            }
        };
        
        // Add sink to track
        track.addSink(videoSink);
        
        System.out.println("[VideoPanel] Video track attached successfully");
    }
    
    /**
     * Detach current video track
     */
    public void detachVideoTrack() {
        if (videoTrack != null && videoSink != null) {
            try {
                videoTrack.removeSink(videoSink);
                System.out.println("[VideoPanel] Video track detached");
            } catch (Exception e) {
                System.err.println("[VideoPanel] Error detaching video: " + e.getMessage());
            }
        }
        
        videoTrack = null;
        videoSink = null;
        isActive = false;
        
        Platform.runLater(() -> drawPlaceholder("No Video"));
    }
    
    /**
     * Render a video frame to the canvas
     * THREAD-SAFE: Queues frames and renders on JavaFX thread without blocking
     */
    private void renderFrame(VideoFrame frame) {
        if (!isActive) return;
        
        // ===== THREADING FIX =====
        // If already rendering, store this frame as pending (drop old one)
        if (isRendering.get()) {
            // Release old pending frame if exists
            if (pendingFrame != null) {
                pendingFrame.release();
            }
            pendingFrame = frame;
            pendingFrame.retain(); // Keep reference
            return; // Don't queue another Platform.runLater
        }
        
        // Mark as rendering
        isRendering.set(true);
        
        try {
            VideoFrameBuffer buffer = frame.buffer;
            int width = buffer.getWidth();
            int height = buffer.getHeight();
            
            // Convert to I420 format (YUV)
            I420Buffer i420 = buffer.toI420();
            
            // Queue rendering on JavaFX thread
            Platform.runLater(() -> {
                try {
                    // Create or resize image if needed
                    if (videoImage == null || 
                        videoImage.getWidth() != width || 
                        videoImage.getHeight() != height) {
                        videoImage = new WritableImage(width, height);
                    }
                    
                    // Convert YUV to RGB
                    convertYUVtoRGB(i420, videoImage);
                    
                    // Clear canvas with black background
                    gc.setFill(Color.BLACK);
                    gc.fillRect(0, 0, getWidth(), getHeight());
                    
                    // Calculate aspect ratio preserving dimensions (letterbox/pillarbox)
                    double canvasWidth = getWidth();
                    double canvasHeight = getHeight();
                    double videoWidth = videoImage.getWidth();
                    double videoHeight = videoImage.getHeight();
                    
                    double canvasAspect = canvasWidth / canvasHeight;
                    double videoAspect = videoWidth / videoHeight;
                    
                    double drawWidth, drawHeight, drawX, drawY;
                    
                    if (videoAspect > canvasAspect) {
                        // Video is wider - fit to width, add letterbox top/bottom
                        drawWidth = canvasWidth;
                        drawHeight = canvasWidth / videoAspect;
                        drawX = 0;
                        drawY = (canvasHeight - drawHeight) / 2;
                    } else {
                        // Video is taller - fit to height, add pillarbox left/right
                        drawHeight = canvasHeight;
                        drawWidth = canvasHeight * videoAspect;
                        drawX = (canvasWidth - drawWidth) / 2;
                        drawY = 0;
                    }
                    
                    // Draw image with preserved aspect ratio
                    gc.drawImage(videoImage, drawX, drawY, drawWidth, drawHeight);
                    
                } catch (Exception e) {
                    System.err.println("[VideoPanel] Error rendering frame: " + e.getMessage());
                } finally {
                    // Mark rendering complete
                    isRendering.set(false);
                    
                    // If there's a pending frame, render it now
                    VideoFrame pending = pendingFrame;
                    if (pending != null) {
                        pendingFrame = null;
                        renderFrame(pending);
                        pending.release();
                    }
                }
            });
            
        } catch (Exception e) {
            System.err.println("[VideoPanel] Error processing frame: " + e.getMessage());
            isRendering.set(false);
        }
    }
    
    /**
     * Convert YUV (I420) format to RGB and write to JavaFX WritableImage
     */
    private void convertYUVtoRGB(I420Buffer yuv, WritableImage image) {
        int width = yuv.getWidth();
        int height = yuv.getHeight();
        
        ByteBuffer yPlane = yuv.getDataY();
        ByteBuffer uPlane = yuv.getDataU();
        ByteBuffer vPlane = yuv.getDataV();
        
        int yStride = yuv.getStrideY();
        int uStride = yuv.getStrideU();
        int vStride = yuv.getStrideV();
        
        PixelWriter pixelWriter = image.getPixelWriter();
        
        // YUV to RGB conversion (ITU-R BT.601)
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // Get Y, U, V values
                int yIndex = y * yStride + x;
                int uvIndex = (y / 2) * uStride + (x / 2);
                
                int Y = (yPlane.get(yIndex) & 0xFF) - 16;
                int U = (uPlane.get(uvIndex) & 0xFF) - 128;
                int V = (vPlane.get(uvIndex) & 0xFF) - 128;
                
                // YUV to RGB conversion formula
                int R = (298 * Y + 409 * V + 128) >> 8;
                int G = (298 * Y - 100 * U - 208 * V + 128) >> 8;
                int B = (298 * Y + 516 * U + 128) >> 8;
                
                // Clamp to 0-255
                R = Math.max(0, Math.min(255, R));
                G = Math.max(0, Math.min(255, G));
                B = Math.max(0, Math.min(255, B));
                
                // Write pixel (ARGB format)
                int argb = 0xFF000000 | (R << 16) | (G << 8) | B;
                pixelWriter.setArgb(x, y, argb);
            }
        }
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
}
