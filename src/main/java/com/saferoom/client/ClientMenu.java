package com.saferoom.client;

import com.saferoom.grpc.SafeRoomProto;
import com.saferoom.grpc.UDPHoleGrpc;
import com.saferoom.grpc.SafeRoomProto.Verification;
import com.saferoom.server.SafeRoomServer;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
public class ClientMenu{
	public static String Server = SafeRoomServer.ServerIP;
	public static int Port = SafeRoomServer.grpcPort;
	public static int UDP_Port = SafeRoomServer.udpPort1;
	public static String myUsername = "abkarada";
	public static String target_username = "james";

		public static int Login(String username, String Password)
		{
		ManagedChannel channel = ManagedChannelBuilder.forAddress(Server,Port)
			.usePlaintext()
			.build();

		UDPHoleGrpc.UDPHoleBlockingStub client = UDPHoleGrpc.newBlockingStub(channel);
		SafeRoomProto.Menu main_menu = SafeRoomProto.Menu.newBuilder()
			.setUsername(username)
			.setHashPassword(Password)
			.build();
		SafeRoomProto.Status stats = client.menuAns(main_menu);
		
		String message = stats.getMessage();
		int code = stats.getCode();
		switch(code){
			case 0:
				System.out.println("Success!");
				return 0;
			case 1:
				if(message.equals("N_REGISTER")){
					System.out.println("Not Registered");
					return 1;
				}else{
					System.out.println("Blocked User");
					return 2;
		}
		default:
				System.out.println("Message has broken");
				return 3;					
			}
		}
	public static int register_client(String username, String password, String mail)
	{
		ManagedChannel channel = ManagedChannelBuilder.forAddress(Server, Port)
			.usePlaintext()
			.build();

		UDPHoleGrpc.UDPHoleBlockingStub stub = UDPHoleGrpc.newBlockingStub(channel);
		
		SafeRoomProto.Create_User insert_obj = SafeRoomProto.Create_User.newBuilder()
			.setUsername(username)
			.setEmail(mail)
			.setPassword(password)
			.setIsVerified(false)
			.build();
		SafeRoomProto.Status stat = stub.insertUser(insert_obj);

		int code = stat.getCode();
		String message = stat.getMessage();
		
		switch(code){
			case 0:
				System.out.println("Success!");
				return 0;
			case 2:
				if(message.equals("VUSERNAME")){
					System.out.println("Username already taken");
					return 1;
				}else{
					System.out.println("Invalid E-mail");
					return 2;
		}
		default:
				System.out.println("Message has broken");
				return 3;					
			}
		}
	public static int verify_user(String username, String verify_code) {
		ManagedChannel channel = ManagedChannelBuilder.forAddress(Server, Port)
				.usePlaintext()
				.build();
		
		UDPHoleGrpc.UDPHoleBlockingStub stub = UDPHoleGrpc.newBlockingStub(channel);
		
		Verification verification_info = Verification.newBuilder()
				.setUsername(username)
				.setVerify(verify_code)
				.build();
		
		SafeRoomProto.Status response = stub.verifyUser(verification_info);
		
		int code = response.getCode();
		
		switch(code) {
		case 0:
			System.out.println("Verification Completed");
			return 0;
		case 1:
			System.out.println("Not Matched");
			return 1;
		
		default:
			System.out.println("Connection is not safe");
			return 2;
		}
	}	

	}

