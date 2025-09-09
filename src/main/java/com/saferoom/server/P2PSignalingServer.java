package com.saferoom.server;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * P2P Signaling Server - Sadece peer bilgilerini eşleştirme yapar
 * HELLO/FIN paketleri peer'lar arasında direkt gönderilir
 */
public class P2PSignalingServer extends Thread {

    static final class PeerInfo {
        final String username;
        final InetAddress publicIP;
        final List<Integer> publicPorts;
        final long registrationTime;
        
        PeerInfo(String username, InetAddress publicIP) {
            this.username = username;
            this.publicIP = publicIP;
            this.publicPorts = Collections.synchronizedList(new ArrayList<>());
            this.registrationTime = System.currentTimeMillis();
        }
        
        void addPort(int port) {
            if (!publicPorts.contains(port)) {
                publicPorts.add(port);
            }
        }
    }

    // Protokol signalleri
    private static final byte SIG_REGISTER_PEER = 0x10;
    private static final byte SIG_REQUEST_PEER = 0x11;
    private static final byte SIG_PEER_INFO = 0x12;
    private static final byte SIG_PEER_NOT_FOUND = 0x13;

    private static final Map<String, PeerInfo> registeredPeers = new ConcurrentHashMap<>();
    public static final int SIGNALING_PORT = 45001; // Server'ın signaling portu
    
    private static final long CLEANUP_INTERVAL_MS = 30_000;
    private static final long PEER_TIMEOUT_MS = 120_000;
    private long lastCleanup = System.currentTimeMillis();

    @Override
    public void run() {
        try (Selector selector = Selector.open();
             DatagramChannel channel = DatagramChannel.open()) {

            channel.configureBlocking(false);
            channel.bind(new InetSocketAddress(SIGNALING_PORT));
            channel.register(selector, SelectionKey.OP_READ);

            System.out.println("🎯 P2P Signaling Server running on port " + SIGNALING_PORT);

            while (true) {
                selector.select(100);

                Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                while (it.hasNext()) {
                    SelectionKey key = it.next(); 
                    it.remove();
                    
                    if (!key.isReadable()) continue;

                    ByteBuffer buf = ByteBuffer.allocate(1024);
                    SocketAddress from = channel.receive(buf);
                    if (from == null) continue;

                    buf.flip();
                    if (buf.remaining() < 3) continue; // Minimum header size

                    InetSocketAddress clientAddr = (InetSocketAddress) from;
                    
                    byte signal = buf.get(0);
                    
                    switch (signal) {
                        case SIG_REGISTER_PEER -> handleRegisterPeer(channel, buf, clientAddr);
                        case SIG_REQUEST_PEER -> handleRequestPeer(channel, buf, clientAddr);
                        default -> System.out.println("⚠️ Unknown signal: " + signal);
                    }
                }

                // Cleanup expired peers
                long now = System.currentTimeMillis();
                if (now - lastCleanup > CLEANUP_INTERVAL_MS) {
                    registeredPeers.entrySet().removeIf(entry -> 
                        (now - entry.getValue().registrationTime) > PEER_TIMEOUT_MS);
                    lastCleanup = now;
                }
            }

        } catch (Exception e) {
            System.err.println("❌ P2P Signaling Server ERROR: " + e);
            e.printStackTrace();
        }
    }

    /**
     * Peer registration: REGISTER_PEER username port
     */
    private void handleRegisterPeer(DatagramChannel channel, ByteBuffer buf, InetSocketAddress clientAddr) {
        try {
            buf.position(1); // Skip signal byte
            short usernameLen = buf.getShort();
            
            byte[] usernameBytes = new byte[usernameLen];
            buf.get(usernameBytes);
            String username = new String(usernameBytes);
            
            int port = buf.getInt();
            
            PeerInfo peer = registeredPeers.computeIfAbsent(username, 
                k -> new PeerInfo(username, clientAddr.getAddress()));
            peer.addPort(port);
            
            System.out.printf("📝 Peer registered: %s @ %s:%d (total ports: %d)%n", 
                username, clientAddr.getAddress().getHostAddress(), port, peer.publicPorts.size());
                
        } catch (Exception e) {
            System.err.println("❌ Error handling REGISTER_PEER: " + e.getMessage());
        }
    }

    /**
     * Peer request: REQUEST_PEER target_username
     */
    private void handleRequestPeer(DatagramChannel channel, ByteBuffer buf, InetSocketAddress clientAddr) {
        try {
            buf.position(1); // Skip signal byte
            short targetLen = buf.getShort();
            
            byte[] targetBytes = new byte[targetLen];
            buf.get(targetBytes);
            String targetUsername = new String(targetBytes);
            
            PeerInfo targetPeer = registeredPeers.get(targetUsername);
            
            if (targetPeer != null && !targetPeer.publicPorts.isEmpty()) {
                // Send peer info
                sendPeerInfo(channel, clientAddr, targetPeer);
                System.out.printf("📤 Sent peer info for %s to %s%n", 
                    targetUsername, clientAddr.getAddress().getHostAddress());
            } else {
                // Send peer not found
                sendPeerNotFound(channel, clientAddr, targetUsername);
                System.out.printf("❌ Peer not found: %s (requested by %s)%n", 
                    targetUsername, clientAddr.getAddress().getHostAddress());
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error handling REQUEST_PEER: " + e.getMessage());
        }
    }

    /**
     * Send peer info response
     */
    private void sendPeerInfo(DatagramChannel channel, InetSocketAddress clientAddr, PeerInfo peer) {
        try {
            ByteBuffer response = ByteBuffer.allocate(1024);
            response.put(SIG_PEER_INFO);
            
            // Username
            byte[] usernameBytes = peer.username.getBytes();
            response.putShort((short) usernameBytes.length);
            response.put(usernameBytes);
            
            // IP address
            byte[] ipBytes = peer.publicIP.getAddress();
            response.put((byte) ipBytes.length);
            response.put(ipBytes);
            
            // Ports
            response.putShort((short) peer.publicPorts.size());
            for (int port : peer.publicPorts) {
                response.putInt(port);
            }
            
            response.flip();
            channel.send(response, clientAddr);
            
        } catch (Exception e) {
            System.err.println("❌ Error sending peer info: " + e.getMessage());
        }
    }

    /**
     * Send peer not found response
     */
    private void sendPeerNotFound(DatagramChannel channel, InetSocketAddress clientAddr, String targetUsername) {
        try {
            ByteBuffer response = ByteBuffer.allocate(256);
            response.put(SIG_PEER_NOT_FOUND);
            
            byte[] usernameBytes = targetUsername.getBytes();
            response.putShort((short) usernameBytes.length);
            response.put(usernameBytes);
            
            response.flip();
            channel.send(response, clientAddr);
            
        } catch (Exception e) {
            System.err.println("❌ Error sending peer not found: " + e.getMessage());
        }
    }

    /**
     * Helper method to create REGISTER_PEER packet
     */
    public static ByteBuffer createRegisterPeerPacket(String username, int port) {
        byte[] usernameBytes = username.getBytes();
        ByteBuffer packet = ByteBuffer.allocate(1 + 2 + usernameBytes.length + 4);
        
        packet.put(SIG_REGISTER_PEER);
        packet.putShort((short) usernameBytes.length);
        packet.put(usernameBytes);
        packet.putInt(port);
        
        packet.flip();
        return packet;
    }

    /**
     * Helper method to create REQUEST_PEER packet
     */
    public static ByteBuffer createRequestPeerPacket(String targetUsername) {
        byte[] targetBytes = targetUsername.getBytes();
        ByteBuffer packet = ByteBuffer.allocate(1 + 2 + targetBytes.length);
        
        packet.put(SIG_REQUEST_PEER);
        packet.putShort((short) targetBytes.length);
        packet.put(targetBytes);
        
        packet.flip();
        return packet;
    }
}
