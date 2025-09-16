package com.saferoom.client;

import com.saferoom.crypto.CryptoUtils;
import com.saferoom.crypto.KeyExchange;
import com.saferoom.grpc.*;
import com.saferoom.grpc.SafeRoomProto.PublicKeyMessage;
import com.saferoom.grpc.SafeRoomProto.Request_Client;
import com.saferoom.grpc.SafeRoomProto.SendPublicKeyRequest;
import com.saferoom.grpc.SafeRoomProto.Status;
import com.saferoom.client.EncryptedMessageHandler;
import com.saferoom.client.DecryptedMessageHandler;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

// import com.saferoom.p2p.*; // P2P system removed
import com.saferoom.sessions.SessionInfo;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;

import java.security.PrivateKey;
import java.security.PublicKey;

import java.security.KeyFactory;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class SafeRoomClient {
	
	public static String[] info;
	public static SafeRoomProto.Stun_Info myInfo;
	public static SafeRoomProto.Status reg;
	public static SafeRoomProto.Request_Client request;
	public static  SafeRoomProto.Stun_Info targetInfo; 
	public static SafeRoomProto.HandshakeConfirm hs;
	public static SafeRoomProto.Status hsResult;
	public static Status rsaResult;
	public static Object lock = new Object();
	
	public static ExecutorService threadPool = Executors.newFixedThreadPool(5);
	
	 public static void run(String myUsername, String targetUsername) throws Exception{
	        // P2P system removed - method disabled
	        System.out.println("⚠️ P2P system disabled - using server relay only");
	        return; // Temporarily disabled
	        
	        /*
	        info = MultiStunClient.StunClient();

	        if (!info[0].equals("true")) {
	            System.out.println("❌ STUN başarısız. Test durduruldu.");
	            return;
	        }
	        

	        String myIp = info[1];
	        int myPort = Integer.parseInt(info[2]);
	        boolean myState = Boolean.parseBoolean(info[3]);

	        System.out.println("🌐 STUN IP: " + myIp + ", Port: " + myPort + ", OpenAccess: " + myState);
	        */
	        
	        // All remaining P2P code disabled - will be reimplemented
	        /*
	        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 50051)//server
	                .usePlaintext()
	                .build();
	        

	        UDPHoleGrpc.UDPHoleBlockingStub client = UDPHoleGrpc.newBlockingStub(channel);
	        
	        myInfo = SafeRoomProto.Stun_Info.newBuilder()
	                .setUsername(myUsername)
	                .setIp(myIp)
	                .setPort(myPort)
	                .setState(myState)
	                .build();

	        reg = client.registerClient(myInfo);
	        System.out.println("📥 Register sonucu: " + reg.getMessage());

	        request = SafeRoomProto.Request_Client.newBuilder()
	                .setUsername(targetUsername)
	                .build();

	        targetInfo = client.getStunInfo(request);
	        if (targetInfo.getIp().isEmpty()) {
	            System.out.println("❌ Hedef kullanıcı RAM'de yok. Test iptal.");
	            return;
	        }

	        System.out.println("🎯 Hedef IP: " + targetInfo.getIp() + ":" + targetInfo.getPort());

	        String punchMessage = "HOLE_PUNCH:" + myUsername;
	        UDPSender.sendPunch(myUsername, targetUsername, myIp, targetInfo.getIp(), myPort, targetInfo.getPort(), punchMessage);
	        
	        // Remaining P2P handshake code also disabled
	        hs = SafeRoomProto.HandshakeConfirm.newBuilder()
	                .setClientId(myUsername)
	                .setTargetId(targetUsername)
	                .setTimestamp(System.currentTimeMillis())
	                .build();

	        hsResult = client.handShake(hs);
	        System.out.println("🤝 Handshake sonucu: " + hsResult.getMessage());

	        channel.shutdown();

	        System.out.println("✅ SafeRoomClient işlemi tamamlandı.");
	        */
	    }
	
	private ThreadPoolExecutor newFixedPool(int i) {
		// TODO Auto-generated method stub
		return null;
	}

	public static void Join(String me ,String wtoJoin) throws Exception {
			try {
				run("localhost", wtoJoin);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
		synchronized(lock) {
			KeyExchange.create_AES();
			lock.notifyAll();
		}

		String client_ID =wtoJoin+me;

		SecretKey SecretKey_AES = KeyExchange.AES_Key;
		
		String wtoJoinAddress =targetInfo.getIp()+":"+targetInfo.getPort();
		
		ManagedChannel channel = ManagedChannelBuilder.forTarget(wtoJoinAddress)
				.usePlaintext()
				.build();
		
		UDPHoleGrpc.UDPHoleBlockingStub stub = UDPHoleGrpc.newBlockingStub(channel);
		
		SafeRoomProto.RequestByClient_ID req = SafeRoomProto.RequestByClient_ID.newBuilder()
															.setClientId(client_ID)
															.build();
		
		SafeRoomProto.PublicKeyMessage public_Key = stub.getPublicKey(req);
		byte[] RSAPublicByte = Base64.getDecoder().decode(public_Key.getBase64Key());
		X509EncodedKeySpec keySpec = new X509EncodedKeySpec(RSAPublicByte);
		KeyFactory kf = KeyFactory.getInstance("RSA");
		PublicKey ReturnedPublicKey = kf.generatePublic(keySpec);
		
		String EncryptedAESKey = CryptoUtils.encrypt_AESkey(SecretKey_AES, ReturnedPublicKey);
		SafeRoomProto.EncryptedAESKeyMessage EncryptedProtoBuf = SafeRoomProto.EncryptedAESKeyMessage.newBuilder()
																					.setClientId(client_ID)
																					.setEncryptedKey(EncryptedAESKey)
																					.build();
		SafeRoomProto.Status stats = stub.sendEncryptedAESKey(EncryptedProtoBuf);
			if(stats != null && stats.equals(0)) {
				System.out.println("Encrypted Key Successfully been sended");
				SafeRoomProto.HandshakeConfirm HandShake = SafeRoomProto.HandshakeConfirm.newBuilder()
																			.setClientId(me)
																			.setTargetId(wtoJoinAddress)
																			.setTimestamp(0)
																			.build();
				
			}
			else {
				System.err.println("There has been a empty routing \n Handshake Failed");
	
			}
			
		threadPool.execute(new EncryptedMessageHandler());
		threadPool.execute(new DecryptedMessageHandler());
		

		
	}
	
	public static void NewMeeting(String me, String wtoWait) throws Exception {
		synchronized(lock) {
		KeyExchange.init();
		lock.notifyAll();
		}

		String client_ID = me+wtoWait;

		PublicKey Public_Key = KeyExchange.publicKey;
		PrivateKey Private_Key = KeyExchange.privateKey;

		
		String wtoClientAddress =targetInfo.getIp()+":"+targetInfo.getPort();

		
		ManagedChannel channel = ManagedChannelBuilder.forTarget(wtoClientAddress)
				.usePlaintext()
				.build();
		
		UDPHoleGrpc.UDPHoleBlockingStub stub = UDPHoleGrpc.newBlockingStub(channel);
		
		String EncodedPublicKey = Base64.getEncoder().encodeToString(Public_Key.getEncoded());

		SendPublicKeyRequest request = SendPublicKeyRequest.newBuilder()
													.setBase64Pubkey(EncodedPublicKey)
													.setSessionID(client_ID)
													.build();

		Status response = stub.sendPublicKey(request);

			if(response.getCode() == 0){
				System.out.println("Public Key was sent");
			}
			else{
				System.err.println("Error has been accour[ERROR]");
			}
			
			SafeRoomProto.RequestByClient_ID pullbyID = SafeRoomProto.RequestByClient_ID.newBuilder()
																						.setClientId(client_ID)
																						.build();
			
			
			SafeRoomProto.EncryptedAESKeyMessage returnedAESKey = stub.getEncryptedAESKey(pullbyID);
			
			if(returnedAESKey != null && !returnedAESKey.getEncryptedKey().isEmpty()) {
				System.out.println("AES Key Has Been Taken");
			}
			else {
				System.err.println("AES Exchange could not done");
			}
			String aes_key = returnedAESKey.getEncryptedKey();
			
			SecretKey DecryptedAESKey = CryptoUtils.decrypt_AESkey(aes_key, Private_Key);
			
			if(DecryptedAESKey != null) {
				SafeRoomProto.HandshakeConfirm HandShake = SafeRoomProto.HandshakeConfirm.newBuilder()
																		.setClientId(me)
																		.setTargetId(wtoWait)
																		.setTimestamp(0)
																		.build();
				SafeRoomProto.Status stats = stub.handShake(HandShake);
				if(stats != null && stats.getCode() == 0) {
					System.out.println("HandShake is done Communication can start righ now");
				}
				else { 
					System.err.println("HandShake Failure");
				}
				
				
			}
			threadPool.execute(new EncryptedMessageHandler());
			threadPool.execute(new DecryptedMessageHandler());
			

		
	}

   
    public static void main(String[] args){
        try {
			run("root","target");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
}
