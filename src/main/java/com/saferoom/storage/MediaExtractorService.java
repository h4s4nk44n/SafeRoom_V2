package com.saferoom.storage;

import javafx.scene.image.Image;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Media Extractor Service
 * 
 * Generates thumbnails for:
 * - PDF documents (first page)
 * - Video files (first frame via FFmpeg)
 * - Images (scaled down version)
 * 
 * All operations are async to avoid blocking UI thread
 */
public class MediaExtractorService {
    
    private static final Logger LOGGER = Logger.getLogger(MediaExtractorService.class.getName());
    private static final int THUMBNAIL_SIZE = 300;
    private static MediaExtractorService instance;
    
    private final ExecutorService executor;
    
    private MediaExtractorService() {
        this.executor = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "MediaExtractor-Thread");
            t.setDaemon(true);
            return t;
        });
    }
    
    public static synchronized MediaExtractorService getInstance() {
        if (instance == null) {
            instance = new MediaExtractorService();
        }
        return instance;
    }
    
    /**
     * PDF thumbnail extraction has been disabled to reduce dependencies.
     * Returns null - PDFs will show a generic document icon instead.
     * 
     * @param pdfPath Path to PDF file
     * @return CompletableFuture<Image> Always returns null
     */
    public CompletableFuture<Image> extractPdfThumbnailAsync(Path pdfPath) {
        // PDF thumbnail via PDFBox has been removed to reduce dependencies.
        // Return null to use default document icon.
        return CompletableFuture.completedFuture(null);
    }
    
    /**
     * Extract thumbnail from video file using FFmpeg
     * 
     * @param videoPath Path to video file
     * @return CompletableFuture<Image> First frame as thumbnail
     */
    public CompletableFuture<Image> extractVideoThumbnailAsync(Path videoPath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Create temp file for thumbnail
                File tempThumb = File.createTempFile("video_thumb_", ".jpg");
                tempThumb.deleteOnExit();
                
                // FFmpeg command to extract first frame
                ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg",
                    "-i", videoPath.toString(),
                    "-vf", "select=eq(n\\,0)",
                    "-vsync", "vfr",
                    "-vframes", "1",
                    "-q:v", "2",
                    tempThumb.getAbsolutePath()
                );
                
                pb.redirectErrorStream(true);
                Process process = pb.start();
                
                int exitCode = process.waitFor();
                
                if (exitCode == 0 && tempThumb.exists()) {
                    BufferedImage buffered = ImageIO.read(tempThumb);
                    if (buffered != null) {
                        BufferedImage scaled = scaleImage(buffered, THUMBNAIL_SIZE);
                        Image fxImage = javafx.embed.swing.SwingFXUtils.toFXImage(scaled, null);
                        
                        LOGGER.fine("Video thumbnail extracted: " + videoPath.getFileName());
                        return fxImage;
                    }
                }
                
                LOGGER.warning("FFmpeg failed to extract thumbnail: exit code " + exitCode);
                return null;
                
            } catch (IOException | InterruptedException e) {
                LOGGER.log(Level.WARNING, "Failed to extract video thumbnail: " + videoPath, e);
                return null;
            }
        }, executor);
    }
    
    /**
     * Extract thumbnail from image file (scaled down)
     * 
     * @param imagePath Path to image file
     * @return CompletableFuture<Image> Scaled thumbnail
     */
    public CompletableFuture<Image> extractImageThumbnailAsync(Path imagePath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                BufferedImage original = ImageIO.read(imagePath.toFile());
                if (original == null) {
                    LOGGER.warning("Failed to read image: " + imagePath);
                    return null;
                }
                
                BufferedImage scaled = scaleImage(original, THUMBNAIL_SIZE);
                Image fxImage = javafx.embed.swing.SwingFXUtils.toFXImage(scaled, null);
                
                LOGGER.fine("Image thumbnail extracted: " + imagePath.getFileName());
                return fxImage;
                
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to extract image thumbnail: " + imagePath, e);
                return null;
            }
        }, executor);
    }
    
    /**
     * Auto-detect file type and extract appropriate thumbnail
     * 
     * @param filePath Path to file
     * @return CompletableFuture<Image> Thumbnail or null
     */
    public CompletableFuture<Image> extractThumbnailAuto(Path filePath) {
        String fileName = filePath.getFileName().toString().toLowerCase();
        
        if (fileName.endsWith(".pdf")) {
            return extractPdfThumbnailAsync(filePath);
        } else if (fileName.endsWith(".mp4") || fileName.endsWith(".mov") || 
                   fileName.endsWith(".avi") || fileName.endsWith(".mkv")) {
            return extractVideoThumbnailAsync(filePath);
        } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || 
                   fileName.endsWith(".png") || fileName.endsWith(".gif")) {
            return extractImageThumbnailAsync(filePath);
        }
        
        LOGGER.fine("No thumbnail extractor for file type: " + fileName);
        return CompletableFuture.completedFuture(null);
    }
    
    /**
     * Scale image to fit within max size while preserving aspect ratio
     */
    private BufferedImage scaleImage(BufferedImage original, int maxSize) {
        int width = original.getWidth();
        int height = original.getHeight();
        
        // Already small enough?
        if (width <= maxSize && height <= maxSize) {
            return original;
        }
        
        // Calculate new dimensions
        double scale;
        if (width > height) {
            scale = (double) maxSize / width;
        } else {
            scale = (double) maxSize / height;
        }
        
        int newWidth = (int) (width * scale);
        int newHeight = (int) (height * scale);
        
        // Scale using high-quality algorithm
        BufferedImage scaled = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g2d = scaled.createGraphics();
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, 
                            java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(original, 0, 0, newWidth, newHeight, null);
        g2d.dispose();
        
        return scaled;
    }
    
    /**
     * Check if FFmpeg is available on system
     */
    public boolean isFFmpegAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-version");
            Process process = pb.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }
    
    /**
     * Shutdown executor
     */
    public void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
            LOGGER.info("MediaExtractor executor shut down");
        }
    }
}

