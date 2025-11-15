package com.saferoom.gui.components;

import dev.onvoid.webrtc.media.video.*;
import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import java.nio.ByteBuffer;

/**
 * Video rendering panel for WebRTC video tracks.
 * Displays both local and remote video streams on JavaFX Canvas.
 */
public class VideoPanel extends Canvas {
    
    private final GraphicsContext gc;
    private WritableImage videoImage;
    private VideoTrack videoTrack;
    private VideoTrackSink videoSink;
    private boolean isActive = false;
    
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
     */
    private void renderFrame(VideoFrame frame) {
        if (!isActive) return;
        
        try {
            VideoFrameBuffer buffer = frame.buffer;
            int width = buffer.getWidth();
            int height = buffer.getHeight();
            
            // Convert to I420 format (YUV)
            I420Buffer i420 = buffer.toI420();
            
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
                    
                    // Draw image to canvas (scaled to fit)
                    gc.drawImage(videoImage, 0, 0, getWidth(), getHeight());
                    
                } catch (Exception e) {
                    System.err.println("[VideoPanel] Error rendering frame: " + e.getMessage());
                }
            });
            
        } catch (Exception e) {
            System.err.println("[VideoPanel] Error processing frame: " + e.getMessage());
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
        gc.setFill(Color.rgb(30, 30, 30));
        gc.fillRect(0, 0, getWidth(), getHeight());
        
        gc.setFill(Color.rgb(150, 150, 150));
        gc.setFont(javafx.scene.text.Font.font(16));
        
        double textWidth = 80; // Approximate
        double textHeight = 16;
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
