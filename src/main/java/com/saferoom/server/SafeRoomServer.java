package com.saferoom.server;

public class SafeRoomServer {
	public static String ServerIP = "192.168.1.38";
	public static int grpcPort = 50051;
	public static int udpPort1 = 45000;

	
	public static void main(String[] args) throws Exception{
	
		// Eski PeerListener (HELLO/FIN sonsuz loop sorunu var)
		PeerListener Datagram = new PeerListener();
		
		// Yeni P2P Signaling Server (sadece peer bilgilerini eşleştirme)
		P2PSignalingServer SignalingServer = new P2PSignalingServer();
		
		StreamListener Stream = new StreamListener();
		
		Datagram.start();
		SignalingServer.start();
		Stream.start();
		
		System.out.println("🚀 SafeRoom Server started:");
		System.out.println("   📡 gRPC Server: " + ServerIP + ":" + grpcPort);
		System.out.println("   🔗 Legacy UDP: " + udpPort1);
		System.out.println("   🎯 P2P Signaling: " + P2PSignalingServer.SIGNALING_PORT);
	}
}
