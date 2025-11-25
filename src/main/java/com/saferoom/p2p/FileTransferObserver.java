package com.saferoom.p2p;

import java.nio.file.Path;

public interface FileTransferObserver {
    default void onTransferStarted(long fileId, Path path, long totalBytes) {}
    default void onTransferProgress(long fileId, long bytesSent, long totalBytes) {}
    default void onTransferCompleted(long fileId) {}
    default void onTransferFailed(long fileId, Throwable error) {}
    default void onTransportStats(long fileId, long droppedPackets) {}
}

