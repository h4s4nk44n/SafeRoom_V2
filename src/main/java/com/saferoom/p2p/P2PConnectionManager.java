package com.saferoom.p2p;

import com.saferoom.client.ClientMenu;
import com.saferoom.server.SafeRoomServer;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * P2P bağlantı yöneticisi - kullanıcılar arası direkt bağlantı kurar
 */
public class P2PConnectionManager {
    
    private static final P2PConnectionManager INSTANCE = new P2PConnectionManager();
    
    private final ConcurrentMap<String, P2PConnection> activeConnections = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, CompletableFuture<P2PConnection>> pendingConnections = new ConcurrentHashMap<>();
    
    public static P2PConnectionManager getInstance() {
        return INSTANCE;
    }
    
    /**
     * Belirtilen kullanıcıyla P2P bağlantı kurar
     */
    public CompletableFuture<P2PConnection> connectToUser(String targetUsername) {
        // Zaten bağlantı varsa onu döndür
        P2PConnection existing = activeConnections.get(targetUsername);
        if (existing != null && existing.isConnected()) {
            System.out.println("🔄 Using existing P2P connection to: " + targetUsername);
            return CompletableFuture.completedFuture(existing);
        }
        
        // Pending bağlantı varsa onu döndür
        CompletableFuture<P2PConnection> pending = pendingConnections.get(targetUsername);
        if (pending != null) {
            System.out.println("🔄 P2P connection already pending to: " + targetUsername);
            return pending;
        }
        
        // Yeni bağlantı başlat
        CompletableFuture<P2PConnection> connectionFuture = CompletableFuture.supplyAsync(() -> {
            try {
                System.out.println("🚀 Starting P2P connection to: " + targetUsername);
                
                // NAT hole punching başlat
                String originalTarget = ClientMenu.target_username;
                ClientMenu.target_username = targetUsername;
                
                InetSocketAddress serverAddr = new InetSocketAddress(
                    SafeRoomServer.ServerIP, 
                    45001  // P2P Signaling Server portu
                );
                
                // P2P hole punching gerçekleştir
                P2PConnection connection = P2PHolePuncher.establishConnection(targetUsername, serverAddr);
                
                // Eski target'ı geri yükle
                ClientMenu.target_username = originalTarget;
                
                if (connection != null) {
                    activeConnections.put(targetUsername, connection);
                    System.out.println("✅ P2P connection established with: " + targetUsername);
                    return connection;
                } else {
                    throw new RuntimeException("Failed to establish P2P connection");
                }
                
            } catch (Exception e) {
                System.err.println("❌ P2P connection failed to " + targetUsername + ": " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        });
        
        pendingConnections.put(targetUsername, connectionFuture);
        
        // Bağlantı tamamlandığında pending'den kaldır
        connectionFuture.whenComplete((conn, ex) -> {
            pendingConnections.remove(targetUsername);
            if (ex != null) {
                System.err.println("❌ P2P connection completed with error: " + ex.getMessage());
            }
        });
        
        return connectionFuture;
    }
    
    /**
     * Aktif bağlantıyı al
     */
    public P2PConnection getConnection(String username) {
        return activeConnections.get(username);
    }
    
    /**
     * Aktif bağlantı var mı kontrol et
     */
    public boolean hasActiveConnection(String username) {
        return activeConnections.containsKey(username);
    }
    
    /**
     * Bekleyen bağlantı var mı kontrol et
     */
    public boolean hasPendingConnection(String username) {
        return pendingConnections.containsKey(username);
    }
    
    /**
     * Bağlantıyı kapat
     */
    public void closeConnection(String username) {
        P2PConnection connection = activeConnections.remove(username);
        if (connection != null) {
            connection.close();
            System.out.println("🔌 P2P connection closed with: " + username);
        }
        
        // Pending bağlantıyı da iptal et
        CompletableFuture<P2PConnection> pending = pendingConnections.remove(username);
        if (pending != null) {
            pending.cancel(true);
        }
    }
    
    /**
     * Tüm bağlantıları kapat
     */
    public void closeAllConnections() {
        System.out.println("🔌 Closing all P2P connections...");
        
        activeConnections.values().forEach(P2PConnection::close);
        activeConnections.clear();
        
        pendingConnections.values().forEach(future -> future.cancel(true));
        pendingConnections.clear();
        
        System.out.println("✅ All P2P connections closed");
    }
    
    /**
     * Aktif bağlantı sayısı
     */
    public int getActiveConnectionCount() {
        return activeConnections.size();
    }
    
    /**
     * Bekleyen bağlantı sayısı
     */
    public int getPendingConnectionCount() {
        return pendingConnections.size();
    }
}
