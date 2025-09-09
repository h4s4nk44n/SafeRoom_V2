package com.saferoom.p2p;

import com.saferoom.client.ClientMenu;
import com.saferoom.natghost.KeepAliveManager;
import com.saferoom.server.P2PSignalingServer;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Doğru P2P Hole Puncher - Signaling server kullanarak peer bilgilerini alır
 * sonra direkt peer'lar arasında UDP hole punching yapar
 */
public class P2PHolePuncher {
    
    private static final int MIN_CHANNELS = 4;
    private static final long SIGNALING_TIMEOUT_MS = 10_000;
    private static final long PUNCH_TIMEOUT_MS = 15_000;
    private static final long RESEND_INTERVAL_MS = 500;
    private static final long SELECT_BLOCK_MS = 50;
    
    // P2P Protocol signals (peer-to-peer direkt)
    private static final byte SIG_P2P_HELLO = 0x20;
    private static final byte SIG_P2P_HELLO_ACK = 0x21;
    private static final byte SIG_P2P_ESTABLISHED = 0x22;
    
    /**
     * P2P bağlantı kurar - Doğru NAT hole punching protokolü
     */
    public static P2PConnection establishConnection(String targetUsername, InetSocketAddress serverAddr) {
        try {
            System.out.println("🚀 Starting P2P connection to: " + targetUsername);
            
            // 1) SIGNALING PHASE: Server'dan target peer bilgilerini al
            PeerConnectionInfo targetInfo = requestPeerInfo(targetUsername, serverAddr);
            if (targetInfo == null) {
                System.err.println("❌ Could not get peer info for: " + targetUsername);
                return null;
            }
            
            System.out.printf("📡 Target peer info: %s @ %s (ports: %s)%n", 
                targetUsername, targetInfo.address.getHostAddress(), targetInfo.ports);
            
            // 2) P2P PHASE: Direkt peer ile hole punching yap
            return performHolePunching(targetUsername, targetInfo);
            
        } catch (Exception e) {
            System.err.println("❌ P2P connection error: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Signaling server'dan peer bilgilerini iste
     */
    private static PeerConnectionInfo requestPeerInfo(String targetUsername, InetSocketAddress serverAddr) {
        try (DatagramChannel channel = DatagramChannel.open()) {
            
            channel.configureBlocking(false);
            channel.bind(new InetSocketAddress(0));
            
            // Request peer packet oluştur ve gönder
            ByteBuffer request = P2PSignalingServer.createRequestPeerPacket(targetUsername);
            channel.send(request, serverAddr);
            
            System.out.println("📤 Requesting peer info for: " + targetUsername);
            
            Selector selector = Selector.open();
            channel.register(selector, SelectionKey.OP_READ);
            
            long start = System.currentTimeMillis();
            
            while (System.currentTimeMillis() - start < SIGNALING_TIMEOUT_MS) {
                selector.select(SELECT_BLOCK_MS);
                
                Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                while (it.hasNext()) {
                    SelectionKey key = it.next(); 
                    it.remove();
                    
                    if (!key.isReadable()) continue;
                    
                    ByteBuffer buf = ByteBuffer.allocate(1024);
                    SocketAddress from = channel.receive(buf);
                    if (from == null) continue;
                    
                    buf.flip();
                    if (buf.remaining() < 1) continue;
                    
                    byte signal = buf.get(0);
                    
                    if (signal == 0x12) { // SIG_PEER_INFO
                        return parsePeerInfo(buf);
                    } else if (signal == 0x13) { // SIG_PEER_NOT_FOUND
                        System.err.println("❌ Peer not found: " + targetUsername);
                        return null;
                    } else if (signal == 0x14) { // SIG_PEER_PENDING
                        System.out.println("⏰ Peer " + targetUsername + " is pending, continuing to wait...");
                        // Continue waiting - don't return, keep looping
                    }
                }
            }
            
            System.err.println("⏰ Signaling timeout for: " + targetUsername);
            return null;
            
        } catch (Exception e) {
            System.err.println("❌ Signaling error: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Peer info response'unu parse et
     */
    private static PeerConnectionInfo parsePeerInfo(ByteBuffer buf) {
        try {
            buf.position(1); // Skip signal
            
            // Username
            short usernameLen = buf.getShort();
            byte[] usernameBytes = new byte[usernameLen];
            buf.get(usernameBytes);
            String username = new String(usernameBytes);
            
            // IP address
            byte ipLen = buf.get();
            byte[] ipBytes = new byte[ipLen];
            buf.get(ipBytes);
            InetAddress address = InetAddress.getByAddress(ipBytes);
            
            // Ports
            short portCount = buf.getShort();
            List<Integer> ports = new ArrayList<>();
            for (int i = 0; i < portCount; i++) {
                ports.add(buf.getInt());
            }
            
            return new PeerConnectionInfo(username, address, ports);
            
        } catch (Exception e) {
            System.err.println("❌ Error parsing peer info: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Direkt peer ile hole punching gerçekleştir
     */
    private static P2PConnection performHolePunching(String targetUsername, PeerConnectionInfo targetInfo) {
        try {
            System.out.println("🔫 Starting UDP hole punching...");
            
            List<DatagramChannel> channels = new ArrayList<>();
            Selector selector = Selector.open();
            KeepAliveManager KAM = new KeepAliveManager(2_000);
            
            // Her target port için bir channel oluştur
            for (int i = 0; i < targetInfo.ports.size(); i++) {
                DatagramChannel dc = DatagramChannel.open();
                dc.configureBlocking(false);
                dc.bind(new InetSocketAddress(0));
                dc.register(selector, SelectionKey.OP_READ);
                channels.add(dc);
            }
            
            long start = System.currentTimeMillis();
            long lastSend = start;
            boolean connectionEstablished = false;
            DatagramChannel successfulChannel = null;
            InetSocketAddress successfulTarget = null;
            
            System.out.println("📡 Sending P2P HELLO packets...");
            
            while (System.currentTimeMillis() - start < PUNCH_TIMEOUT_MS && !connectionEstablished) {
                
                // Her 500ms'de HELLO paketleri gönder
                if (System.currentTimeMillis() - lastSend > RESEND_INTERVAL_MS) {
                    for (int i = 0; i < channels.size() && i < targetInfo.ports.size(); i++) {
                        DatagramChannel dc = channels.get(i);
                        int targetPort = targetInfo.ports.get(i);
                        InetSocketAddress targetAddr = new InetSocketAddress(targetInfo.address, targetPort);
                        
                        ByteBuffer hello = createP2PHelloPacket(ClientMenu.myUsername, targetUsername);
                        dc.send(hello, targetAddr);
                    }
                    lastSend = System.currentTimeMillis();
                }
                
                selector.select(SELECT_BLOCK_MS);
                
                Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                while (it.hasNext()) {
                    SelectionKey key = it.next(); 
                    it.remove();
                    
                    if (!key.isReadable()) continue;
                    
                    DatagramChannel dc = (DatagramChannel) key.channel();
                    ByteBuffer buf = ByteBuffer.allocate(512);
                    SocketAddress from = dc.receive(buf);
                    if (from == null) continue;
                    
                    buf.flip();
                    if (buf.remaining() < 1) continue;
                    
                    byte signal = buf.get(0);
                    
                    if (signal == SIG_P2P_HELLO) {
                        // HELLO ACK gönder
                        ByteBuffer ack = createP2PHelloAckPacket(ClientMenu.myUsername, targetUsername);
                        dc.send(ack, from);
                        
                        System.out.printf("✅ P2P HELLO received from %s, sending ACK%n", from);
                        
                    } else if (signal == SIG_P2P_HELLO_ACK) {
                        // Bağlantı kuruldu!
                        ByteBuffer established = createP2PEstablishedPacket(ClientMenu.myUsername, targetUsername);
                        dc.send(established, from);
                        
                        connectionEstablished = true;
                        successfulChannel = dc;
                        successfulTarget = (InetSocketAddress) from;
                        
                        System.out.printf("🎉 P2P connection established with %s via %s%n", targetUsername, from);
                        break;
                        
                    } else if (signal == SIG_P2P_ESTABLISHED) {
                        connectionEstablished = true;
                        successfulChannel = dc;
                        successfulTarget = (InetSocketAddress) from;
                        
                        System.out.printf("🎉 P2P connection confirmed with %s via %s%n", targetUsername, from);
                        break;
                    }
                }
            }
            
            // Sonuç
            if (connectionEstablished && successfulChannel != null) {
                // Diğer kanalları kapat
                for (DatagramChannel dc : channels) {
                    if (dc != successfulChannel) {
                        try { dc.close(); } catch (Exception e) {}
                    }
                }
                
                // P2P connection oluştur
                P2PConnection connection = new P2PConnection(targetUsername, successfulTarget, successfulChannel);
                connection.setKeepAliveManager(KAM);
                
                System.out.println("✅ P2P hole punching successful!");
                return connection;
                
            } else {
                System.err.println("❌ P2P hole punching failed - timeout");
                KAM.close();
                for (DatagramChannel dc : channels) {
                    try { dc.close(); } catch (Exception e) {}
                }
                return null;
            }
            
        } catch (Exception e) {
            System.err.println("❌ Hole punching error: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * P2P protokol paketleri
     */
    private static ByteBuffer createP2PHelloPacket(String sender, String target) {
        return createP2PPacket(SIG_P2P_HELLO, sender, target);
    }
    
    private static ByteBuffer createP2PHelloAckPacket(String sender, String target) {
        return createP2PPacket(SIG_P2P_HELLO_ACK, sender, target);
    }
    
    private static ByteBuffer createP2PEstablishedPacket(String sender, String target) {
        return createP2PPacket(SIG_P2P_ESTABLISHED, sender, target);
    }
    
    private static ByteBuffer createP2PPacket(byte signal, String sender, String target) {
        byte[] senderBytes = sender.getBytes();
        byte[] targetBytes = target.getBytes();
        
        ByteBuffer packet = ByteBuffer.allocate(1 + 2 + senderBytes.length + 2 + targetBytes.length);
        packet.put(signal);
        packet.putShort((short) senderBytes.length);
        packet.put(senderBytes);
        packet.putShort((short) targetBytes.length);
        packet.put(targetBytes);
        
        packet.flip();
        return packet;
    }
    
    /**
     * Peer connection bilgileri
     */
    private static class PeerConnectionInfo {
        final InetAddress address;
        final List<Integer> ports;
        
        PeerConnectionInfo(String username, InetAddress address, List<Integer> ports) {
            this.address = address;
            this.ports = ports;
        }
    }
    
    /**
     * Async P2P bağlantı kurar
     */
    public static CompletableFuture<P2PConnection> establishConnectionAsync(String targetUsername, InetSocketAddress serverAddr) {
        return CompletableFuture.supplyAsync(() -> establishConnection(targetUsername, serverAddr))
                .orTimeout(30, TimeUnit.SECONDS);
    }
    
    /**
     * Kendimizi signaling server'a register et
     */
    public static void registerPeerToServer(String username, InetSocketAddress serverAddr) {
        try (DatagramChannel channel = DatagramChannel.open()) {
            
            channel.configureBlocking(false);
            channel.bind(new InetSocketAddress(0));
            
            // Get our local port
            int localPort = ((InetSocketAddress) channel.getLocalAddress()).getPort();
            
            // Register peer packet oluştur ve gönder
            ByteBuffer registerPacket = P2PSignalingServer.createRegisterPeerPacket(username, localPort);
            channel.send(registerPacket, serverAddr);
            
            System.out.printf("📝 Registered peer: %s with port %d to server%n", username, localPort);
            
        } catch (Exception e) {
            System.err.println("❌ Failed to register peer: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
