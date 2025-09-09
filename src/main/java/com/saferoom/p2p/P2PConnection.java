package com.saferoom.p2p;

import com.saferoom.natghost.KeepAliveManager;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * P2P bağlantı wrapper sınıfı - bir kullanıcıyla direkt bağlantı temsil eder
 */
public class P2PConnection {
    
    private final String remoteUsername;
    private final InetSocketAddress remoteAddress;
    private final DatagramChannel channel;
    private final AtomicBoolean connected = new AtomicBoolean(true);
    private final BlockingQueue<byte[]> messageQueue = new LinkedBlockingQueue<>();
    private KeepAliveManager keepAliveManager; // P2P keep-alive için
    
    // Keep-alive mekanizması için
    private long lastHeartbeat = System.currentTimeMillis();
    private static final long HEARTBEAT_TIMEOUT = 30000; // 30 saniye
    
    public P2PConnection(String remoteUsername, InetSocketAddress remoteAddress, DatagramChannel channel) {
        this.remoteUsername = remoteUsername;
        this.remoteAddress = remoteAddress;
        this.channel = channel;
    }
    
    /**
     * KeepAliveManager'ı ayarla
     */
    public void setKeepAliveManager(KeepAliveManager kam) {
        this.keepAliveManager = kam;
    }
    
    /**
     * Mesaj gönder
     */
    public boolean sendMessage(byte[] data) {
        if (!isConnected()) {
            System.err.println("❌ Cannot send message - connection closed to: " + remoteUsername);
            return false;
        }
        
        try {
            ByteBuffer buffer = ByteBuffer.wrap(data);
            int sent = channel.send(buffer, remoteAddress);
            
            if (sent > 0) {
                System.out.println("📤 Sent " + sent + " bytes to " + remoteUsername);
                return true;
            } else {
                System.err.println("⚠️ Failed to send data to " + remoteUsername);
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error sending message to " + remoteUsername + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Mesaj kuyruğuna mesaj ekle (alınan mesajlar için)
     */
    public void queueReceivedMessage(byte[] data) {
        try {
            messageQueue.offer(data);
            updateHeartbeat();
        } catch (Exception e) {
            System.err.println("❌ Error queuing message from " + remoteUsername + ": " + e.getMessage());
        }
    }
    
    /**
     * Bekleyen mesaj al
     */
    public byte[] receiveMessage() {
        try {
            return messageQueue.poll();
        } catch (Exception e) {
            System.err.println("❌ Error receiving message from " + remoteUsername + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Heartbeat güncelle
     */
    public void updateHeartbeat() {
        lastHeartbeat = System.currentTimeMillis();
    }
    
    /**
     * Bağlantı aktif mi kontrol et
     */
    public boolean isConnected() {
        if (!connected.get()) {
            return false;
        }
        
        // Heartbeat timeout kontrolü
        long timeSinceLastHeartbeat = System.currentTimeMillis() - lastHeartbeat;
        if (timeSinceLastHeartbeat > HEARTBEAT_TIMEOUT) {
            System.out.println("💔 P2P connection timed out with: " + remoteUsername);
            connected.set(false);
            return false;
        }
        
        return true;
    }
    
    /**
     * Bağlantıyı kapat
     */
    public void close() {
        connected.set(false);
        
        // KeepAliveManager'ı kapat
        if (keepAliveManager != null) {
            try {
                keepAliveManager.close();
            } catch (Exception e) {
                System.err.println("❌ Error closing KeepAliveManager: " + e.getMessage());
            }
        }
        
        try {
            if (channel != null && channel.isOpen()) {
                channel.close();
            }
        } catch (Exception e) {
            System.err.println("❌ Error closing P2P connection to " + remoteUsername + ": " + e.getMessage());
        }
        
        // Mesaj kuyruğunu temizle
        messageQueue.clear();
        
        System.out.println("🔌 P2P connection closed with: " + remoteUsername);
    }
    
    // Getters
    public String getRemoteUsername() {
        return remoteUsername;
    }
    
    public InetSocketAddress getRemoteAddress() {
        return remoteAddress;
    }
    
    public DatagramChannel getChannel() {
        return channel;
    }
    
    public long getLastHeartbeat() {
        return lastHeartbeat;
    }
    
    public int getQueuedMessageCount() {
        return messageQueue.size();
    }
    
    @Override
    public String toString() {
        return String.format("P2PConnection{user='%s', address=%s, connected=%s, queuedMessages=%d}", 
                           remoteUsername, remoteAddress, isConnected(), getQueuedMessageCount());
    }
}
