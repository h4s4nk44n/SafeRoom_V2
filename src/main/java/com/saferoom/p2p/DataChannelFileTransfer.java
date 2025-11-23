package com.saferoom.p2p;

import com.saferoom.file_transfer.*;
import dev.onvoid.webrtc.RTCDataChannel;
import dev.onvoid.webrtc.RTCDataChannelBuffer;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.*;

/**
 * File Transfer Manager for WebRTC DataChannel
 * 
 * ROLE: Coordinate sender/receiver roles ONLY
 * 
 * Architecture:
 * 1. DataChannelWrapper: Wraps DataChannel as DatagramChannel
 * 2. EnhancedFileTransferSender: Original sender (QUIC, congestion control)
 * 3. FileTransferReceiver: Original receiver (ChunkManager, unlimited size)
 */
public class DataChannelFileTransfer {
    
    private final String username;
    private final DataChannelWrapper channelWrapper;
    
    private final EnhancedFileTransferSender sender;
    private final FileTransferReceiver receiver;
    
    private final ExecutorService executor;
    
    private final ConcurrentMap<Long, Path> pendingDownloadPaths = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, CompletableFuture<Void>> receiverReadySignals = new ConcurrentHashMap<>();
    
    private FileTransferCallback transferCallback;
    private volatile boolean receiverStarted = false;  // Track if receiver is running
    
    @FunctionalInterface
    public interface FileTransferCallback {
        void onFileReceived(String sender, long fileId, Path filePath, long fileSize);
    }
    
    public DataChannelFileTransfer(String username, RTCDataChannel dataChannel, String remoteUsername) {
        this.username = username;
        
        // CRITICAL: Use SINGLE wrapper for BOTH sender and receiver!
        // This ensures ACK packets reach the sender's queue
        this.channelWrapper = new DataChannelWrapper(dataChannel, username, remoteUsername);
        
        this.executor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "FileTransfer-" + username);
            t.setDaemon(true);
            return t;
        });
        
        try {
            // Both sender and receiver use THE SAME wrapper!
            this.sender = new EnhancedFileTransferSender(channelWrapper);
            this.receiver = new FileTransferReceiver();
            this.receiver.channel = channelWrapper;  // SAME wrapper as sender!
            
            System.out.printf("[DCFileTransfer] Initialized for %s (SHARED wrapper)%n", username);
        } catch (Exception e) {
            System.err.printf("[DCFileTransfer] Init error: %s%n", e.getMessage());
            throw new RuntimeException("Failed to initialize", e);
        }
    }
    
    public void setTransferCallback(FileTransferCallback callback) {
        this.transferCallback = callback;
    }
    
    public CompletableFuture<Boolean> sendFile(Path filePath, long fileId) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        System.out.println("[DCFileTransfer] ╔════════════════════════════════════════════════");
        System.out.printf("[DCFileTransfer] ║ sendFile() CALLED%n");
        System.out.printf("[DCFileTransfer] ║ filePath: %s%n", filePath);
        System.out.printf("[DCFileTransfer] ║ fileId: %d%n", fileId);
        System.out.printf("[DCFileTransfer] ║ sender: %s%n", sender != null ? "INITIALIZED" : "NULL");
        System.out.printf("[DCFileTransfer] ║ thread: %s%n", Thread.currentThread().getName());
        System.out.println("[DCFileTransfer] ╚════════════════════════════════════════════════");
        
        executor.execute(() -> {
            try {
                System.out.printf("[DCFileTransfer] 📤 Executor thread started: %s%n", Thread.currentThread().getName());
                System.out.printf("[DCFileTransfer] 🚀 Calling sender.sendFile()...%n");
                
                sender.sendFile(filePath, fileId);
                
                System.out.printf("[DCFileTransfer] ✅ sender.sendFile() returned successfully%n");
                System.out.printf("[DCFileTransfer] Sent: %s%n", filePath.getFileName());
                future.complete(true);
            } catch (Exception e) {
                System.err.println("[DCFileTransfer] ❌❌❌ EXCEPTION CAUGHT ❌❌❌");
                System.err.printf("[DCFileTransfer] Exception type: %s%n", e.getClass().getName());
                System.err.printf("[DCFileTransfer] Exception message: %s%n", e.getMessage());
                System.err.println("[DCFileTransfer] Stack trace:");
                e.printStackTrace();
                future.complete(false);
            }
        });
        
        return future;
    }
    
    public void startReceiver(Path downloadPath) {
        if (receiverStarted) {
            System.out.printf("[DCFileTransfer] Receiver already started for %s%n", username);
            return;
        }
        
        receiverStarted = true;
        
        System.out.println("[FT-RECV] ═══════════════════════════════════════════════════");
        System.out.printf("[FT-RECV] startReceiver() called at %d%n", System.currentTimeMillis());
        System.out.printf("[FT-RECV] Thread: %s%n", Thread.currentThread().getName());
        System.out.printf("[FT-RECV] Download path: %s%n", downloadPath);
        System.out.println("[FT-RECV] ═══════════════════════════════════════════════════");
        
        executor.execute(() -> {
            try {
                downloadPath.getParent().toFile().mkdirs();
                receiver.filePath = downloadPath;
                
                System.out.printf("[FT-RECV] Receiver thread started, calling ReceiveData()...%n");
                receiver.ReceiveData();
                
                System.out.printf("[DCFileTransfer] Received: %s%n", downloadPath);
                
                if (transferCallback != null) {
                    transferCallback.onFileReceived(username, receiver.fileId, downloadPath, receiver.file_size);
                }
            } catch (Exception e) {
                System.err.printf("[DCFileTransfer] Receive error: %s%n", e.getMessage());
                e.printStackTrace();
            } finally {
                receiverStarted = false;
            }
        });
    }
    
    public void handleIncomingMessage(RTCDataChannelBuffer buffer) {
        java.nio.ByteBuffer data = buffer.data.duplicate();
        if (data.remaining() > 0) {
            byte signal = data.get(0);
            if (signal == 0x01 && !receiverStarted) {
                long fileId = System.currentTimeMillis();
                if (data.remaining() >= HandShake_Packet.HEADER_SIZE) {
                    java.nio.ByteBuffer dup = data.duplicate();
                    dup.position(1); // skip signal byte
                    if (dup.remaining() >= Long.BYTES) {
                        fileId = dup.getLong();
                    }
                }
                System.out.printf("[DCFileTransfer] SYN received without prepared receiver - starting fallback receiver (fileId=%d)%n",
                    fileId);
                startPreparedReceiver(fileId);
            }
        }
        
        channelWrapper.onDataChannelMessage(buffer);
    }
    
    public DataChannelWrapper getWrapper() {
        return channelWrapper;
    }
    
    public void shutdown() {
        executor.shutdownNow();
    }
    
    public Path prepareIncomingFile(long fileId, String originalName) {
        Path downloadsDir = Paths.get("downloads");
        downloadsDir.toFile().mkdirs();
        String sanitized = sanitizeFileName(Optional.ofNullable(originalName).orElse("file.bin"));
        Path target = downloadsDir.resolve(fileId + "_" + sanitized);
        pendingDownloadPaths.put(fileId, target);
        System.out.printf("[DCFileTransfer] Incoming file registered: %s%n", target);
        return target;
    }
    
    public void startPreparedReceiver(long fileId) {
        Path target = pendingDownloadPaths.remove(fileId);
        if (target == null) {
            target = createDefaultDownloadPath(fileId);
        }
        
        System.out.println("[FT-RECV] ╔════════════════════════════════════════════════");
        System.out.printf("[FT-RECV] ║ startPreparedReceiver() called%n");
        System.out.printf("[FT-RECV] ║ fileId: %d%n", fileId);
        System.out.printf("[FT-RECV] ║ target: %s%n", target);
        System.out.printf("[FT-RECV] ║ thread: %s%n", Thread.currentThread().getName());
        System.out.printf("[FT-RECV] ║ receiverStarted: %s%n", receiverStarted);
        System.out.println("[FT-RECV] ╚════════════════════════════════════════════════");
        
        startReceiver(target);
    }
    
    public CompletableFuture<Void> awaitReceiverReady(long fileId) {
        return receiverReadySignals.computeIfAbsent(fileId, id -> new CompletableFuture<>());
    }
    
    public void markReceiverReady(long fileId) {
        CompletableFuture<Void> future = receiverReadySignals.computeIfAbsent(fileId, id -> new CompletableFuture<>());
        future.complete(null);
        receiverReadySignals.remove(fileId);
    }
    
    private Path createDefaultDownloadPath(long fileId) {
        return Paths.get("downloads", "received_" + fileId + ".bin");
    }
    
    private static String sanitizeFileName(String name) {
        return name.replaceAll("[\\\\/:]", "_");
    }
}
