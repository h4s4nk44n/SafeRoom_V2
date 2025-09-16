package com.saferoom.test;

import com.saferoom.client.UDPSender;
import com.saferoom.grpc.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class SafeRoomTestRunner {

    public static void runP2PTest(String myUsername, String targetUsername) {
        System.out.println("🚀 SafeRoom P2P Test Başlıyor: " + myUsername + " → " + targetUsername);

        // 1️⃣ STUN Tespiti - DISABLED (P2P system removed)
        // String[] info = MultiStunClient.StunClient();
        System.out.println("⚠️ P2P system disabled - test skipped");
        return; // Exit early since P2P is disabled

        /*
        if (!info[0].equals("true")) {
            System.out.println("❌ STUN başarısız, test durduruldu.");
            return;
        }

        String myIp = info[1];
        int myPort = Integer.parseInt(info[2]);
        boolean myState = Boolean.parseBoolean(info[3]);

        System.out.println("🌐 STUN IP: " + myIp + " PORT: " + myPort + " OpenAccess: " + myState);
        */
        
        // All P2P test code disabled - will be reimplemented
        /*
        // 2️⃣ GRPC Kanal Oluştur
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 50051)
                .usePlaintext()
                .build();

        UDPHoleGrpc.UDPHoleBlockingStub client = UDPHoleGrpc.newBlockingStub(channel);

        // 3️⃣ RAM'e Kayıt (registerClient)
        SafeRoomProto.Stun_Info myInfo = SafeRoomProto.Stun_Info.newBuilder()
                .setUsername(myUsername)
                .setIp(myIp)
                .setPort(myPort)
                .setState(myState)
                .build();

        SafeRoomProto.Status reg = client.registerClient(myInfo);
        System.out.println("📥 Register: " + reg.getMessage());

        // 4️⃣ Hedef Peer Bilgisini Al
        SafeRoomProto.Request_Client req = SafeRoomProto.Request_Client.newBuilder()
                .setUsername(targetUsername)
                .build();

        SafeRoomProto.Stun_Info targetInfo = client.getStunInfo(req);
        if (targetInfo.getIp().isEmpty()) {
            System.out.println("❌ Hedef peer RAM'de yok, test durduruldu.");
            return;
        }

        System.out.println("🎯 Hedef Bilgisi: " + targetInfo.getIp() + ":" + targetInfo.getPort());

        // 5️⃣ UDP Delik Açma Mesajı Gönder
        String fullMessage = "HOLE_PUNCH:" + myUsername;
        UDPSender.sendPunch(myUsername, targetUsername, myIp, targetInfo.getIp(), myPort, targetInfo.getPort(), fullMessage);

        // 6️⃣ Handshake
        SafeRoomProto.HandshakeConfirm handshake = SafeRoomProto.HandshakeConfirm.newBuilder()
                .setClientId(myUsername)
                .setTargetId(targetUsername)
                .setTimestamp(System.currentTimeMillis())
                .build();

        SafeRoomProto.Status hs = client.handShake(handshake);
        System.out.println("🤝 Handshake sonucu: " + hs.getMessage());

        // 7️⃣ Kanalı kapat
        channel.shutdown();

        System.out.println("✅ P2P Test Başarıyla Tamamlandı.");
        */
    }
}
