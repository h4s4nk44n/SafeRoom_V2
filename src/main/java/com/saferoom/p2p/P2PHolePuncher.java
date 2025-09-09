package com.saferoom.p2p;

import com.saferoom.client.ClientMenu;
import com.saferoom.natghost.KeepAliveManager;
import com.saferoom.natghost.LLS;
import com.saferoom.natghost.NatAnalyzer;

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
 * P2P Hole Puncher - NatAnalyzer'dan uyarlanan NAT hole punching implementasyonu
 */
public class P2PHolePuncher {
    
    private static final int MIN_CHANNELS = 4;
    private static final long MATCH_TIMEOUT_MS = 20_000;
    private static final long RESEND_INTERVAL_MS = 1_000;
    private static final long SELECT_BLOCK_MS = 50;
    
    /**
     * P2P bağlantı kurar - NatAnalyzer multiplexer metodundan uyarlanmıştır
     */
    public static P2PConnection establishConnection(String targetUsername, InetSocketAddress serverAddr) {
        try {
            System.out.println("🚀 Starting P2P hole punching to: " + targetUsername);
            
            // NAT analiz yap (basitleştirilmiş)
            analyzeNAT();
            
            int holeCount = Math.max(NatAnalyzer.Public_PortList.size(), MIN_CHANNELS);
            
            Selector selector = Selector.open();
            List<DatagramChannel> channels = new ArrayList<>(holeCount);
            
            // KeepAliveManager kuruluyor
            KeepAliveManager KAM = new KeepAliveManager(2_000);
            
            ByteBuffer hello = LLS.New_Hello_Packet(ClientMenu.myUsername, targetUsername, LLS.SIG_HELLO);
            
            // 1) HELLO paketleri gönder
            for (int i = 0; i < holeCount; i++) {
                DatagramChannel dc = DatagramChannel.open();
                dc.configureBlocking(false);
                dc.bind(new InetSocketAddress(0));
                dc.send(hello.duplicate(), serverAddr);
                dc.register(selector, SelectionKey.OP_READ);
                channels.add(dc);
                
                InetSocketAddress local = (InetSocketAddress) dc.getLocalAddress();
                System.out.println("[P2P] HELLO sent from local port: " + local.getPort());
            }
            
            // 2) FIN paketi gönder
            channels.get(0).send(
                LLS.New_Fin_Packet(ClientMenu.myUsername, targetUsername).duplicate(),
                serverAddr
            );
            
            long start = System.currentTimeMillis();
            long lastSend = start;
            boolean allDone = false;
            
            Set<Integer> remotePorts = new LinkedHashSet<>();
            InetSocketAddress remoteAddress = null;
            
            // Round-robin index for channel assignment
            int rrIdx = 0;
            
            System.out.println("🔍 Waiting for remote peer ports...");
            
            // 3) Server'dan karşı tarafın port bilgilerini al
            while (System.currentTimeMillis() - start < MATCH_TIMEOUT_MS && !allDone) {
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
                    
                    if (!LLS.hasWholeFrame(buf)) continue;
                    byte type = LLS.peekType(buf);
                    
                    if (type == LLS.SIG_PORT) {
                        List<Object> info = LLS.parsePortInfo(buf.duplicate());
                        InetSocketAddress peerAddr = new InetSocketAddress(
                            (java.net.InetAddress) info.get(0), 
                            (Integer) info.get(1)
                        );
                        
                        if (remoteAddress == null) {
                            remoteAddress = peerAddr;
                        }
                        
                        if (remotePorts.add(peerAddr.getPort())) {
                            System.out.printf("[P2P] <<< PORT %s:%d\\n", 
                                            peerAddr.getAddress().getHostAddress(), peerAddr.getPort());
                            
                            // Channel seç ve keep-alive kaydet
                            DatagramChannel chosen = channels.get(rrIdx % channels.size());
                            rrIdx++;
                            
                            KAM.register(chosen, peerAddr);
                        }
                        
                    } else if (type == LLS.SIG_ALL_DONE) {
                        List<Object> info = LLS.parseAllDone(buf.duplicate());
                        String who = (String) info.get(0);
                        System.out.println("[P2P] <<< ALL_DONE from " + who);
                        allDone = true;
                    }
                }
                
                // Cevap yoksa resend
                if (!allDone && remotePorts.isEmpty() &&
                    (System.currentTimeMillis() - lastSend) > RESEND_INTERVAL_MS) {
                    
                    for (DatagramChannel dc : channels) {
                        dc.send(hello.duplicate(), serverAddr);
                    }
                    channels.get(0).send(
                        LLS.New_Fin_Packet(ClientMenu.myUsername, targetUsername).duplicate(),
                        serverAddr
                    );
                    lastSend = System.currentTimeMillis();
                }
            }
            
            // 4) Sonuç kontrolü
            if (!allDone || remoteAddress == null) {
                System.err.println("❌ P2P hole punching failed - timeout or no remote address");
                KAM.close();
                closeChannels(channels);
                return null;
            }
            
            System.out.println("✅ P2P hole punching successful!");
            System.out.println("📡 Remote address: " + remoteAddress);
            System.out.println("🔌 Remote ports: " + remotePorts);
            
            // 5) P2PConnection oluştur (ilk kanal kullan)
            DatagramChannel primaryChannel = channels.get(0);
            P2PConnection connection = new P2PConnection(targetUsername, remoteAddress, primaryChannel);
            
            // KeepAliveManager'ı connection'a ata
            connection.setKeepAliveManager(KAM);
            
            // Diğer kanalları kapat
            for (int i = 1; i < channels.size(); i++) {
                try {
                    channels.get(i).close();
                } catch (Exception e) {
                    System.err.println("Warning: Failed to close channel " + i);
                }
            }
            
            System.out.println("🎉 P2P connection established with: " + targetUsername);
            return connection;
            
        } catch (Exception e) {
            System.err.println("❌ P2P hole punching error: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Kanalları temizle
     */
    private static void closeChannels(List<DatagramChannel> channels) {
        for (DatagramChannel dc : channels) {
            try {
                if (dc.isOpen()) {
                    dc.close();
                }
            } catch (Exception e) {
                System.err.println("Warning: Failed to close channel: " + e.getMessage());
            }
        }
    }
    
    /**
     * Basitleştirilmiş NAT analizi - gerçek implementasyonda NatAnalyzer.analyzer() kullanılabilir
     */
    private static byte analyzeNAT() {
        // NatAnalyzer.analyzer(stunServers) çağrısının basitleştirilmiş versiyonu
        // Gerçek implementasyonda bu metot kullanılabilir
        return (byte) 0x01; // Örnek signal
    }
    
    /**
     * Async P2P bağlantı kurar
     */
    public static CompletableFuture<P2PConnection> establishConnectionAsync(String targetUsername, InetSocketAddress serverAddr) {
        return CompletableFuture.supplyAsync(() -> establishConnection(targetUsername, serverAddr))
                .orTimeout(30, TimeUnit.SECONDS);
    }
}
