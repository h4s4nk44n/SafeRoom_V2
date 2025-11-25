package com.saferoom.gui.model;

import java.nio.file.Path;

import javafx.scene.image.Image;

/**
 * Metadata for file or media messages. Keeps enough information for
 * placeholder bubbles to render size, name, and thumbnails.
 */
public final class FileAttachment {
    private final MessageType targetType;
    private final String fileName;
    private final long fileSizeBytes;
    private final Path localPath;
    private final Image thumbnail;

    public FileAttachment(MessageType targetType,
                          String fileName,
                          long fileSizeBytes,
                          Path localPath,
                          Image thumbnail) {
        this.targetType = targetType;
        this.fileName = fileName;
        this.fileSizeBytes = fileSizeBytes;
        this.localPath = localPath;
        this.thumbnail = thumbnail;
    }

    public MessageType getTargetType() {
        return targetType;
    }

    public String getFileName() {
        return fileName;
    }

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public Path getLocalPath() {
        return localPath;
    }

    public Image getThumbnail() {
        return thumbnail;
    }

    public String getFormattedSize() {
        double bytes = fileSizeBytes;
        if (bytes < 1024) {
            return ((int) bytes) + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        }
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
}

