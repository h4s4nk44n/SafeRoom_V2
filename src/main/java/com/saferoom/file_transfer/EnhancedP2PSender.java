package com.saferoom.file_transfer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Enhanced P2P File Transfer Sender with QUIC-inspired congestion control
 * Kullanım: java EnhancedP2PSender <bind_port> <target_ip> <target_port> <file_path>
 */
public class EnhancedP2PSender {
    
    public static void main(String[] args) {
        if (args.length < 4) {
            System.out.println("=== Enhanced P2P File Transfer Sender ===");
            System.out.println("Kullanım: java EnhancedP2PSender <bind_port> <target_ip> <target_port> <file_path>");
            System.out.println("");
            System.out.println("Parametreler:");
            System.out.println("  bind_port   : Kendi bilgisayarınızda bind edilecek port");
            System.out.println("  target_ip   : Hedef bilgisayarın IP adresi");
            System.out.println("  target_port : Hedef bilgisayarın port numarası");
            System.out.println("  file_path   : Gönderilecek dosyanın yolu");
            System.out.println("");
            System.out.println("Özellikler:");
            System.out.println("   QUIC-inspired congestion control");
            System.out.println("   Real-time RTT measurement");
            System.out.println("   Adaptive bandwidth estimation");
            System.out.println("   Dynamic window sizing");
            System.out.println("");
            System.out.println("Örnekler:");
            System.out.println("  java EnhancedP2PSender 8888 192.168.1.101 9999 test_file.txt");
            System.out.println("  java EnhancedP2PSender 0 127.0.0.1 9999 large_file.bin");
            return;
        }
        
        int bindPort;
        String targetIp = args[1];
        int targetPort;
        String filePath = args[3];
        
        try {
            bindPort = Integer.parseInt(args[0]);
            targetPort = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            System.err.println(" Hata: Port numaraları geçersiz");
            return;
        }
        
        if (bindPort < 0 || bindPort > 65535 || targetPort < 1 || targetPort > 65535) {
            System.err.println(" Hata: Port numaraları geçersiz (bind_port: 0-65535, target_port: 1-65535)");
            return;
        }
        
        // Dosya kontrolü
        Path file = Paths.get(filePath);
        if (!Files.exists(file)) {
            System.err.println(" Hata: Dosya bulunamadı: " + file.toAbsolutePath());
            return;
        }
        
        if (!Files.isRegularFile(file)) {
            System.err.println(" Hata: Bu bir dosya değil: " + file.toAbsolutePath());
            return;
        }
        
        DatagramChannel senderChannel = null;
        
        try {
            long fileSize = Files.size(file);
            double fileSizeMB = fileSize / (1024.0 * 1024.0);
            
            System.out.println("=== Enhanced P2P File Transfer Sender ===");
            System.out.println(" Enhanced Sender başlatılıyor...");
            System.out.println(" Bind Port: " + (bindPort == 0 ? "otomatik" : bindPort));
            System.out.println(" Target: " + targetIp + ":" + targetPort);
            System.out.println(" File: " + file.toAbsolutePath());
            System.out.println(" File Size: " + fileSize + " bytes (" + String.format("%.2f", fileSizeMB) + " MB)");
            System.out.println("");
            
            // Channel setup with optimized buffers
            senderChannel = DatagramChannel.open();
            
            // MAXIMUM UDP BUFFER'LAR
            senderChannel.setOption(java.net.StandardSocketOptions.SO_SNDBUF, 16 * 1024 * 1024); // 16MB send buffer
            senderChannel.setOption(java.net.StandardSocketOptions.SO_RCVBUF, 16 * 1024 * 1024); // 16MB receive buffer
            
            InetSocketAddress bindAddress = new InetSocketAddress(bindPort);
            senderChannel.bind(bindAddress);
            
            // Actual bind port'u al
            int actualBindPort = ((InetSocketAddress) senderChannel.getLocalAddress()).getPort();
            System.out.println("Socket başarıyla bind edildi - Port: " + actualBindPort);
            
            // Target'a connect
            InetSocketAddress targetAddress = new InetSocketAddress(targetIp, targetPort);
            senderChannel.connect(targetAddress);
            System.out.println("Target'a bağlandı: " + targetAddress);
            
            // Always use WAN mode - LAN mode disabled due to excessive packet loss
            boolean isLocal = false; //LAN mode tamamen kapatıldı
            
            System.out.println(" WAN mode enabled - Optimized for stability and performance");
            System.out.println("");
            
            // Enhanced FileTransferSender kullan
            EnhancedFileTransferSender sender = new EnhancedFileTransferSender(senderChannel);
            long fileId = System.currentTimeMillis(); // Unique file ID
            
            System.out.println(" Enhanced file transfer başlatılıyor...");
            System.out.println(" File ID: " + fileId);
            System.out.println(" QUIC-style handshake ve adaptive transfer başlıyor...");
            System.out.println("");
            
            long startTime = System.currentTimeMillis();
            
            // Enhanced transfer başlat
            sender.sendFile(file, fileId);
            
            long endTime = System.currentTimeMillis();
            double transferTime = (endTime - startTime) / 1000.0;
            double throughputMBps = fileSizeMB / transferTime;
            double throughputMbps = throughputMBps * 8;
            
            System.out.println("");
            System.out.println("=== Enhanced Transfer Tamamlandı ===");
            System.out.println(" Dosya başarıyla gönderildi!");
            System.out.println(" Dosya boyutu: " + fileSize + " bytes (" + String.format("%.2f", fileSizeMB) + " MB)");
            System.out.println(" Transfer süresi: " + String.format("%.2f", transferTime) + " saniye");
            System.out.println(" Transfer hızı: " + String.format("%.2f", throughputMBps) + " MB/s (" + 
                             String.format("%.1f", throughputMbps) + " Mbps)");
            System.out.println(" Congestion control: QUIC-inspired hybrid algorithm");
            
        } catch (IOException e) {
            System.err.println(" IO Hatası: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println(" Beklenmeyen hata: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Cleanup
            if (senderChannel != null && senderChannel.isOpen()) {
                try {
                    senderChannel.close();
                    System.out.println(" Enhanced Sender kapatıldı");
                } catch (IOException e) {
                    System.err.println("  Channel kapatma hatası: " + e.getMessage());
                }
            }
            
            // Thread pool'u kapat
            EnhancedFileTransferSender.shutdownThreadPool();
            System.out.println("Enhanced P2P Sender sona erdi");
        }
    }
}
