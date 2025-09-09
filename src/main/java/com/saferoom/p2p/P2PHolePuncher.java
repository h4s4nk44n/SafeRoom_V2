package com.saferoom.p2p;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.saferoom.client.ClientMenu;
import com.saferoom.natghost.LLS;
import com.saferoom.natghost.KeepAliveManager;
import com.saferoom.p2p.P2PConnection;

/**
 * P2P NAT Hole Punching using LLS cross-matching protocol with STUN
 */
public class P2PHolePuncher {
    
    // LLS Protocol signals
    private static final byte SIG_HELLO = 0x10;
    private static final byte SIG_FIN = 0x11;  
    private static final byte SIG_PORT = 0x12;
    private static final byte SIG_ALL_DONE = 0x13;
    
    // UFW allowed ports for firewall bypass
    private static final int[] FIREWALL_FRIENDLY_PORTS = {4000, 4001, 4002, 4003, 4004, 4005};
    private static int currentPortIndex = 0;
    
    // Timeouts
    private static final int SIGNALING_TIMEOUT_MS = 10_000;
    private static final int PUNCH_TIMEOUT_MS = 8_000;
    private static final int RESEND_INTERVAL_MS = 200;
    private static final int SELECT_BLOCK_MS = 100;
    private static final int MIN_CHANNELS = 3;
    
    /**
     * PORT_INFO response container
     */
    private static class PeerInfo {
        final String targetIP;
        final List<Integer> targetPorts;
        
        PeerInfo(String targetIP, List<Integer> targetPorts) {
            this.targetIP = targetIP;
            this.targetPorts = targetPorts;
        }
    }
    
    /**
     * Ana P2P connection establish methodu
     */
    public static P2PConnection establishConnection(String targetUsername, InetSocketAddress serverAddr) {
        try {
            System.out.println("🚀 Starting P2P connection to: " + targetUsername);
            
            // 1) SIGNALING PHASE: Server'a HELLO paketleri gönder (STUN ile)
            PeerInfo peerInfo = performSignaling(targetUsername, serverAddr);
            if (peerInfo == null || peerInfo.targetPorts.isEmpty()) {
                System.err.println("❌ P2P signaling failed for: " + targetUsername);
                return null;
            }
            
            System.out.printf("📡 Got target peer info for %s: IP=%s, ports=%s%n", 
                targetUsername, peerInfo.targetIP, peerInfo.targetPorts);
            
            // 2) P2P PHASE: Direct hole punching başlat
            P2PConnection connection = performDirectHolePunching(targetUsername, peerInfo);
            if (connection != null) {
                System.out.println("✅ P2P connection established with: " + targetUsername);
                return connection;
            } else {
                System.err.println("❌ P2P hole punching failed - timeout");
                return null;
            }
            
        } catch (Exception e) {
            System.err.println("❌ P2P connection error: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * LLS HELLO/FIN cross-matching signaling - STUN IP kullanarak
     */
    private static PeerInfo performSignaling(String targetUsername, InetSocketAddress serverAddr) {
        try (DatagramChannel channel = DatagramChannel.open()) {
            
            channel.configureBlocking(false);
            
            // Try to bind to firewall-friendly port
            boolean bound = false;
            for (int port : FIREWALL_FRIENDLY_PORTS) {
                try {
                    channel.bind(new InetSocketAddress(port));
                    System.out.printf("🔓 Bound to firewall-friendly port: %d%n", port);
                    bound = true;
                    break;
                } catch (Exception e) {
                    System.out.printf("⚠️ Port %d busy, trying next...%n", port);
                }
            }
            
            if (!bound) {
                // Fallback to random port
                channel.bind(new InetSocketAddress(0));
                System.out.println("⚠️ Using random port (firewall may block)");
            }
            
            // STUN ile public IP/port al
            System.out.println("🌐 Getting public IP/port via STUN...");
            String[] stunResult = MultiStunClient.StunClient();
            
            boolean useSTUN = "true".equals(stunResult[0]);
            String myPublicIP = useSTUN ? stunResult[1] : null;
            int myPublicPort = useSTUN ? Integer.parseInt(stunResult[2]) : 0;
            
            if (useSTUN) {
                System.out.printf("✅ STUN success: %s:%d%n", myPublicIP, myPublicPort);
            } else {
                System.err.println("❌ STUN failed - using basic packets");
            }
            
            System.out.printf("📤 Sending HELLO+FIN to server: '%s' -> '%s'%n", ClientMenu.myUsername, targetUsername);
            
            Selector selector = Selector.open();
            channel.register(selector, SelectionKey.OP_READ);
            
            long start = System.currentTimeMillis();
            long lastHello = start;
            
            // Initial HELLO+FIN gönder
            sendHelloFin(channel, serverAddr, targetUsername, useSTUN, myPublicIP, myPublicPort);
            
            while (System.currentTimeMillis() - start < SIGNALING_TIMEOUT_MS) {
                
                // Her 1 saniyede HELLO gönder
                if (System.currentTimeMillis() - lastHello > 1000) {
                    sendHelloFin(channel, serverAddr, targetUsername, useSTUN, myPublicIP, myPublicPort);
                    lastHello = System.currentTimeMillis();
                    System.out.println("🔄 Resending HELLO+FIN to server...");
                }
                
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
                    
                    if (signal == SIG_PORT) { // PORT_INFO geldi!
                        System.out.println("📡 Received PORT_INFO from server");
                        PeerInfo peerInfo = parsePortInfo(buf);
                        
                        // FIN gönder (signaling tamamlandı)
                        sendHelloFin(channel, serverAddr, targetUsername, useSTUN, myPublicIP, myPublicPort);
                        System.out.println("📤 Sent FIN to server");
                        
                        return peerInfo;
                        
                    } else if (signal == SIG_ALL_DONE) { // Karşı taraf da hazır
                        System.out.println("✅ ALL_DONE received - both peers ready");
                        PeerInfo peerInfo = parsePortInfo(buf);
                        return peerInfo;
                    }
                }
            }
            
            System.err.println("❌ Signaling timeout - no PORT_INFO received");
            return null;
            
        } catch (Exception e) {
            System.err.println("❌ Signaling error: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * HELLO+FIN paketleri gönder
     */
    private static void sendHelloFin(DatagramChannel channel, InetSocketAddress serverAddr, 
            String targetUsername, boolean useSTUN, String myPublicIP, int myPublicPort) throws IOException {
        
        if (useSTUN && myPublicIP != null) {
            // STUN IP ile LLS paketleri
            ByteBuffer hello = createHelloPacketWithIP(ClientMenu.myUsername, targetUsername, myPublicIP, myPublicPort);
            channel.send(hello, serverAddr);
            ByteBuffer fin = createFinPacketWithIP(ClientMenu.myUsername, targetUsername, myPublicIP, myPublicPort);
            channel.send(fin, serverAddr);
        } else {
            // Basic paketler (IP bilgisi olmadan)
            ByteBuffer hello = createHelloPacket(ClientMenu.myUsername, targetUsername);
            channel.send(hello, serverAddr);
            ByteBuffer fin = createFinPacket(ClientMenu.myUsername, targetUsername);
            channel.send(fin, serverAddr);
        }
    }
    
    /**
     * PORT_INFO paketini parse et - IP ve port bilgisi al
     */
    private static PeerInfo parsePortInfo(ByteBuffer buf) {
        try {
            // LLS packet format: [signal][len][username_20][target_20][ip_4][port_4]
            List<Object> parsed = LLS.parseLLSPacket(buf);
            
            // parsed[0] = signal, parsed[1] = len, parsed[2] = username, 
            // parsed[3] = target, parsed[4] = InetAddress, parsed[5] = port
            InetAddress targetInetAddr = (InetAddress) parsed.get(4);
            String targetIP = targetInetAddr.getHostAddress();
            Integer port = (Integer) parsed.get(5);
            
            System.out.printf("🔍 Parsed PORT_INFO: IP=%s, port=%d%n", targetIP, port);
            
            List<Integer> ports = new ArrayList<>();
            ports.add(port);
            return new PeerInfo(targetIP, ports);
            
        } catch (Exception e) {
            System.err.println("❌ Error parsing PORT_INFO: " + e.getMessage());
            e.printStackTrace();
            return new PeerInfo("192.168.1.100", new ArrayList<>());
        }
    }
    
    /**
     * LLS protokol paketleri oluştur - IP ile (STUN'dan gelen)
     */
    private static ByteBuffer createHelloPacketWithIP(String sender, String target, String publicIP, int publicPort) {
        try {
            return LLS.New_PortInfo_Packet(sender, target, SIG_HELLO, 
                java.net.InetAddress.getByName(publicIP), publicPort);
        } catch (Exception e) {
            System.err.println("❌ Error creating HELLO packet with IP: " + e.getMessage());
            return createHelloPacket(sender, target); // fallback
        }
    }
    
    private static ByteBuffer createFinPacketWithIP(String sender, String target, String publicIP, int publicPort) {
        try {
            return LLS.New_PortInfo_Packet(sender, target, SIG_FIN, 
                java.net.InetAddress.getByName(publicIP), publicPort);
        } catch (Exception e) {
            System.err.println("❌ Error creating FIN packet with IP: " + e.getMessage());
            return createFinPacket(sender, target); // fallback
        }
    }
    
    /**
     * LLS protokol paketleri oluştur - basic (IP olmadan)
     */
    private static ByteBuffer createHelloPacket(String sender, String target) {
        return LLS.New_Multiplex_Packet(SIG_HELLO, sender, target);
    }
    
    private static ByteBuffer createFinPacket(String sender, String target) {
        return LLS.New_Multiplex_Packet(SIG_FIN, sender, target);
    }
    
    /**
     * Direct peer-to-peer hole punching (IP ve portları biliyoruz)
     */
    private static P2PConnection performDirectHolePunching(String targetUsername, PeerInfo peerInfo) {
        try {
            System.out.println("🔫 Starting direct P2P hole punching...");
            System.out.printf("🎯 Target: %s @ %s:%s%n", targetUsername, peerInfo.targetIP, peerInfo.targetPorts);
            
            List<DatagramChannel> channels = new ArrayList<>();
            Selector selector = Selector.open();
            KeepAliveManager KAM = new KeepAliveManager(2_000);
            
            // Her target port için bir channel oluştur  
            for (int i = 0; i < Math.max(peerInfo.targetPorts.size(), MIN_CHANNELS); i++) {
                DatagramChannel dc = DatagramChannel.open();
                dc.configureBlocking(false);
                
                // Try firewall-friendly ports for hole punching too
                boolean bound = false;
                for (int port : FIREWALL_FRIENDLY_PORTS) {
                    try {
                        dc.bind(new InetSocketAddress(port + i)); // Offset for multiple channels
                        System.out.printf("🔓 Hole punch channel %d bound to port: %d%n", i, port + i);
                        bound = true;
                        break;
                    } catch (Exception e) {
                        // Port busy, try next
                    }
                }
                
                if (!bound) {
                    dc.bind(new InetSocketAddress(0)); // Fallback
                    System.out.printf("⚠️ Channel %d using random port%n", i);
                }
                
                dc.register(selector, SelectionKey.OP_READ);
                channels.add(dc);
            }
            
            long start = System.currentTimeMillis();
            long lastSend = start;
            boolean connectionEstablished = false;
            DatagramChannel successfulChannel = null;
            InetSocketAddress successfulTarget = null;
            
            System.out.printf("📡 Target ports: %s%n", peerInfo.targetPorts);
            
            while (System.currentTimeMillis() - start < PUNCH_TIMEOUT_MS && !connectionEstablished) {
                
                // Her 200ms'de P2P HELLO paketleri gönder (AGGRESSIVE BURST MODE)
                if (System.currentTimeMillis() - lastSend > RESEND_INTERVAL_MS) {
                    for (int i = 0; i < channels.size() && i < peerInfo.targetPorts.size(); i++) {
                        DatagramChannel dc = channels.get(i);
                        int targetPort = peerInfo.targetPorts.get(i);
                        
                        // PORT_INFO'dan gelen gerçek IP kullan!
                        InetSocketAddress targetAddr = new InetSocketAddress(peerInfo.targetIP, targetPort);
                        
                        System.out.printf("🎯 Sending to REAL target: %s:%d (from PORT_INFO)%n", 
                            peerInfo.targetIP, targetPort);
                        
                        // AGGRESSIVE BURST: Her port için 5 paket gönder (NAT hole punch için)
                        for (int burst = 0; burst < 5; burst++) {
                            ByteBuffer p2pHello = createP2PHelloPacket(ClientMenu.myUsername, targetUsername);
                            dc.send(p2pHello, targetAddr);
                        }
                    }
                    lastSend = System.currentTimeMillis();
                }
                
                // Response bekle
                selector.select(SELECT_BLOCK_MS);
                Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                
                while (it.hasNext()) {
                    SelectionKey key = it.next();
                    it.remove();
                    
                    if (!key.isReadable()) continue;
                    
                    DatagramChannel dc = (DatagramChannel) key.channel();
                    ByteBuffer response = ByteBuffer.allocate(1024);
                    SocketAddress from = dc.receive(response);
                    
                    if (from != null) {
                        response.flip();
                        if (response.remaining() > 0) {
                            byte signal = response.get(0);
                            
                            if (signal == 0x33) { // P2P_HELLO_ACK
                                System.out.printf("✅ P2P HELLO_ACK received from %s%n", from);
                                
                                // ESTABLISHED gönder
                                ByteBuffer established = createP2PEstablishedPacket(ClientMenu.myUsername, targetUsername);
                                dc.send(established, from);
                                
                                connectionEstablished = true;
                                successfulChannel = dc;
                                successfulTarget = (InetSocketAddress) from;
                                break;
                            } else if (signal == 0x32) { // P2P_HELLO
                                System.out.printf("✅ P2P HELLO received from %s - sending ACK%n", from);
                                
                                // ACK gönder
                                ByteBuffer ack = createP2PHelloAckPacket(ClientMenu.myUsername, targetUsername);
                                dc.send(ack, from);
                            }
                        }
                    }
                }
            }
            
            // Cleanup diğer channels
            for (DatagramChannel dc : channels) {
                if (dc != successfulChannel) {
                    try { dc.close(); } catch (Exception e) {}
                }
            }
            
            if (connectionEstablished) {
                System.out.printf("🎉 P2P connection established via %s%n", successfulTarget);
                P2PConnection conn = new P2PConnection(targetUsername, successfulTarget, successfulChannel);
                conn.setKeepAliveManager(KAM);
                return conn;
            } else {
                System.err.println("❌ P2P hole punching timeout");
                return null;
            }
            
        } catch (Exception e) {
            System.err.println("❌ Direct hole punching error: " + e.getMessage());
            return null;
        }
    }
    
    // P2P paket creation methods
    private static ByteBuffer createP2PHelloPacket(String sender, String target) {
        return createP2PPacket((byte) 0x32, sender, target);
    }
    
    private static ByteBuffer createP2PHelloAckPacket(String sender, String target) {
        return createP2PPacket((byte) 0x33, sender, target);
    }
    
    private static ByteBuffer createP2PEstablishedPacket(String sender, String target) {
        return createP2PPacket((byte) 0x34, sender, target);
    }
    
    private static ByteBuffer createP2PPacket(byte signal, String sender, String target) {
        ByteBuffer packet = ByteBuffer.allocate(1 + 2 + 20 + 20);
        packet.put(signal);
        packet.putShort((short) 43);
        
        // Sender - fixed 20 bytes
        byte[] senderBytes = sender.getBytes();
        if (senderBytes.length > 20) {
            packet.put(senderBytes, 0, 20);
        } else {
            packet.put(senderBytes);
            for (int i = senderBytes.length; i < 20; i++) packet.put((byte) 0);
        }
        
        // Target - fixed 20 bytes
        byte[] targetBytes = target.getBytes();
        if (targetBytes.length > 20) {
            packet.put(targetBytes, 0, 20);
        } else {
            packet.put(targetBytes);
            for (int i = targetBytes.length; i < 20; i++) packet.put((byte) 0);
        }
        
        packet.flip();
        return packet;
    }
    
    /**
     * Async version for integration
     */
    public static CompletableFuture<P2PConnection> establishConnectionAsync(String targetUsername, InetSocketAddress serverAddr) {
        return CompletableFuture.supplyAsync(() -> establishConnection(targetUsername, serverAddr));
    }
}
