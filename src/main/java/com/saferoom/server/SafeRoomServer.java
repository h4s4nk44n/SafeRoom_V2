package com.saferoom.server;

public class SafeRoomServer {
	public static String ServerIP = "192.168.1.38";
	public static int grpcPort = 50051;
	public static int udpPort1 = 45000;

	
	public static void main(String[] args) throws Exception{
	
		PeerListener Datagram = new PeerListener();
		StreamListener Stream = new StreamListener();
		Datagram.start();
		Stream.start();
	}
}
