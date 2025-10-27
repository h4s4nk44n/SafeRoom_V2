package com.saferoom.p2p;

import com.saferoom.file_transfer.*;
import dev.onvoid.webrtc.RTCDataChannel;
import dev.onvoid.webrtc.RTCDataChannelBuffer;

import java.nio.file.Path;
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
    
    private FileTransferCallback transferCallback;
    
    @FunctionalInterface
    public interface FileTransferCallback {
        void onFileReceived(String sender, long fileId, Path filePath, long fileSize);
    }
    
    public DataChannelFileTransfer(String username, RTCDataChannel dataChannel, String remoteUsername) {
        this.username = username;
        
        // ⚠️ CRITICAL: Use SINGLE wrapper for BOTH sender and receiver!
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
            
            System.out.printf("[DCFileTransfer] ✅ Initialized for %s (SHARED wrapper)%n", username);
        } catch (Exception e) {
            System.err.printf("[DCFileTransfer] ❌ Init error: %s%n", e.getMessage());
            throw new RuntimeException("Failed to initialize", e);
        }
    }
    
    public void setTransferCallback(FileTransferCallback callback) {
        this.transferCallback = callback;
    }
    
    public CompletableFuture<Boolean> sendFile(String receiver, Path filePath) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        executor.execute(() -> {
            try {
                long fileId = System.currentTimeMillis();
                System.out.printf("[DCFileTransfer] 📤 Sending: %s to %s%n", filePath.getFileName(), receiver);
                
                sender.sendFile(filePath, fileId);
                
                System.out.printf("[DCFileTransfer] ✅ Sent: %s%n", filePath.getFileName());
                future.complete(true);
            } catch (Exception e) {
                System.err.printf("[DCFileTransfer] ❌ Send error: %s%n", e.getMessage());
                future.complete(false);
            }
        });
        
        return future;
    }
    
    public void startReceiver(Path downloadPath) {
        executor.execute(() -> {
            try {
                downloadPath.getParent().toFile().mkdirs();
                receiver.filePath = downloadPath;
                
                System.out.printf("[DCFileTransfer] 📥 Receiver started: %s%n", downloadPath);
                receiver.ReceiveData();
                
                System.out.printf("[DCFileTransfer] ✅ Received: %s%n", downloadPath);
                
                if (transferCallback != null) {
                    transferCallback.onFileReceived(username, receiver.fileId, downloadPath, receiver.file_size);
                }
            } catch (Exception e) {
                System.err.printf("[DCFileTransfer] ❌ Receive error: %s%n", e.getMessage());
            }
        });
    }
    
    public void handleIncomingMessage(RTCDataChannelBuffer buffer) {
        channelWrapper.onDataChannelMessage(buffer);
    }
    
    public DataChannelWrapper getWrapper() {
        return channelWrapper;
    }
    
    public void shutdown() {
        executor.shutdownNow();
    }
}
