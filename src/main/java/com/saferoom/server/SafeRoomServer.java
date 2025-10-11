package com.saferoom.server;

public class SafeRoomServer {
        public static String ServerIP = "35.198.64.68";
        public static int grpcPort = 443;
        public static int udpPort1 = 45000;

	
	public static void main(String[] args) throws Exception{
	


		P2PSignalingServer SignalingServer = new P2PSignalingServer();
		
		StreamListener Stream = new StreamListener();
		
		// Datagram.start(); // ❌ DEVRE DIŞI
		SignalingServer.start();
		Stream.start();
		
		System.out.println("🚀 SafeRoom Server started:");
		System.out.println("   📡 gRPC Server: " + ServerIP + ":" + grpcPort);
		System.out.println("   🎯 P2P Signaling: " + P2PSignalingServer.SIGNALING_PORT);
		// System.out.println("   🔗 Legacy UDP: " + udpPort1); // ❌ DEVRE DIŞI
	}
}
