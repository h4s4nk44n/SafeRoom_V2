package com.saferoom.file_transfer;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;
import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.nio.MappedByteBuffer;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class FileTransferReceiver {	
	public  DatagramChannel channel;
	public long fileId;
	public long file_size;  // Changed to long for large file support
	public int total_seq;
	
	public FileChannel fc;
	public Path filePath;
	public MappedByteBuffer mem_buf;  // Legacy support - will be replaced by ChunkManager
	public ChunkManager chunkManager;  // NEW: Chunk-based I/O for unlimited file size
	public static final long MAX_FILE_SIZE = 256L << 20;
	public static final int SLICE_SIZE = 1450; // Maximum payload without fragmentation
	public static final int HEADER_SIZE = 22;
	public static final int PACKET_SIZE = SLICE_SIZE + HEADER_SIZE;
	
	// Transfer timing
	private long transferStartTime = 0;
	private long transferEndTime = 0;
	
	public  boolean handshake()
	{
		System.out.println("[RECEIVER-HANDSHAKE] ╔════════════════════════════════════════════════");
		System.out.println("[RECEIVER-HANDSHAKE] ║ handshake() ENTERED");
		System.out.printf("[RECEIVER-HANDSHAKE] ║ Thread: %s%n", Thread.currentThread().getName());
		System.out.printf("[RECEIVER-HANDSHAKE] ║ Channel connected: %s%n", 
			channel != null ? channel.isConnected() : "NULL");
		System.out.println("[RECEIVER-HANDSHAKE] ║ Polling for SYN packet...");
		System.out.println("[RECEIVER-HANDSHAKE] ╚════════════════════════════════════════════════");
		
		if(channel == null){
			throw new IllegalStateException("Datagram Channel is null you must bind and connect first");
		}
		ByteBuffer rcv_syn = ByteBuffer.allocateDirect(HandShake_Packet.HEADER_SIZE)
			.order(ByteOrder.BIG_ENDIAN);

		rcv_syn.clear();
		int r;

		// Handshake timeout ekle (30 saniye)
		long handshakeDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
		
		SocketAddress senderAddress = null;
		r = 0; // Initialize r
		int receiveAttempts = 0; // DEBUG: Count attempts
		try{
			do{
				if(System.nanoTime() > handshakeDeadline) {
					System.err.println("Handshake timeout after 30 seconds (attempts: " + receiveAttempts + ")");
					return false;
				}
				
				// CRITICAL: Clear buffer before each receive attempt!
				rcv_syn.clear();
				
				senderAddress = channel.receive(rcv_syn);
				receiveAttempts++;
				
				// DEBUG: Log every 100 attempts
				if(receiveAttempts % 100 == 0) {
					System.out.printf("[RECEIVER-HANDSHAKE] Still waiting for SYN... (attempt %d)%n", 
						receiveAttempts);
				}
				
				if(senderAddress == null) {
					LockSupport.parkNanos(1_000_000); // 1ms bekleme
					continue;
				}
				
				System.out.printf("[RECEIVER-HANDSHAKE] Packet received from: %s (size: %d bytes)%n", 
					senderAddress, rcv_syn.position());
				
				r = rcv_syn.position();
				if( r == 0 || r != HandShake_Packet.HEADER_SIZE || HandShake_Packet.get_signal(rcv_syn) != HandShake_Packet.SYN) {
					LockSupport.parkNanos(1_000_000); // 1ms bekleme
				}
			}while( r == 0 || r != HandShake_Packet.HEADER_SIZE || HandShake_Packet.get_signal(rcv_syn) != HandShake_Packet.SYN);
		}catch(IOException e ){
			System.err.println("IO Error during handshake: " + e);
			return false;
		}
		rcv_syn.flip();
		fileId = HandShake_Packet.get_file_Id(rcv_syn);
		file_size = HandShake_Packet.get_file_size(rcv_syn);
		total_seq = HandShake_Packet.get_total_seq(rcv_syn);
		
		System.out.printf("[RECEIVER-HANDSHAKE] SYN received: fileId=%d, size=%d, chunks=%d%n", 
			fileId, file_size, total_seq);
		
		if(fileId != 0 && file_size != 0 && total_seq != 0)
		 {
			 // Sender'a bağlan (eğer henüz bağlı değilse)
			 try {
				 if(!channel.isConnected()) {
					 channel.connect(senderAddress);
					 System.out.println("Sender'a bağlandı: " + senderAddress);
				 } else {
					 System.out.println("Already connected to: " + senderAddress);
				 }
			 } catch(IOException e) {
				 System.err.println("Sender'a bağlanma hatası: " + e);
				 return false;
			 }
			 
			 HandShake_Packet ack_pkt = new HandShake_Packet();
			ack_pkt.make_ACK(fileId, file_size, total_seq);
			
			System.out.printf("[RECEIVER-HANDSHAKE] Sending ACK for fileId=%d%n", fileId);
			
			try{
			int bytesSent = 0;
			while((bytesSent = channel.write(ack_pkt.get_header().duplicate())) == 0)
			{
				ack_pkt.resetForRetransmitter();
				LockSupport.parkNanos(200_000);
			}
			System.out.printf("[RECEIVER-HANDSHAKE] ACK sent: %d bytes%n", bytesSent);
			}catch(IOException e){
				System.err.println("[RECEIVER-HANDSHAKE] ACK send error: " + e);
			}

			int t;
			
			System.out.println("[RECEIVER-HANDSHAKE] 🔄 Waiting for SYN_ACK (0x11)...");
			long synAckDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
			int synAckAttempts = 0;
			
			try{
				do{
					// CRITICAL: Clear buffer before each read attempt!
					rcv_syn.clear();
					t = channel.read(rcv_syn);
					
					if(t > 0) {
						byte signal = rcv_syn.get(0);
						System.out.printf("[RECEIVER-HANDSHAKE] Read %d bytes, signal: 0x%02X%n", t, signal);
						
						// Check if it's SYN_ACK (0x11) with correct fileId
						if(signal == 0x11) {
							long receivedFileId = HandShake_Packet.get_file_Id(rcv_syn);
							System.out.printf("[RECEIVER-HANDSHAKE] ✅ SYN_ACK received! fileId=%d (expected=%d)%n", 
								receivedFileId, fileId);
							
							if(receivedFileId == fileId) {
								System.out.println("[RECEIVER-HANDSHAKE] ✅ Handshake COMPLETE! Returning true...");
								return true;
							} else {
								System.err.printf("[RECEIVER-HANDSHAKE] ❌ FileId mismatch: got=%d, expected=%d%n",
									receivedFileId, fileId);
							}
						}
					}
					
					synAckAttempts++;
					if(synAckAttempts % 100 == 0) {
						System.out.printf("[RECEIVER-HANDSHAKE] Still waiting for SYN_ACK... (attempt %d)%n", synAckAttempts);
					}
					
					if(System.nanoTime() > synAckDeadline) {
						System.err.println("[RECEIVER-HANDSHAKE] ❌ SYN_ACK timeout after 30 seconds");
						return false;
					}
					
					LockSupport.parkNanos(1_000_000); // 1ms
				}while(true);
			}catch(IOException e){
				System.err.println("[RECEIVER-HANDSHAKE] SYN_ACK read error: " + e);
				e.printStackTrace();
				return false;
			}

		 }

		return false;
	}
	
	public boolean initialize()
	{
		try{
			if(handshake()){
				fc = FileChannel.open(filePath, StandardOpenOption.CREATE 
						, StandardOpenOption.READ
						, StandardOpenOption.WRITE 
						,StandardOpenOption.SYNC);

				fc.truncate(file_size);
				
				// Initialize ChunkManager for unlimited file size support
				// Use existing FileChannel (READ_WRITE mode)
				this.chunkManager = new ChunkManager(fc, file_size, SLICE_SIZE);
				
				// Legacy: Keep mem_buf for backward compatibility (will map first chunk)
				if (file_size <= MAX_FILE_SIZE) {
					mem_buf = fc.map(FileChannel.MapMode.READ_WRITE, 0, file_size);
				} else {
					System.out.println("Large file detected (" + (file_size >> 20) + " MB) - using chunked I/O");
					mem_buf = null; // Signal to use ChunkManager
				}

				 return true;

			}
		}catch(IOException e){
			System.err.println("Initialize State Error: " + e);
		}

		return false;
	
	}

	public void ReceiveData(){
	
	if(initialize()){
	
	// Data transfer başlıyor - timing başlat
	transferStartTime = System.currentTimeMillis();
	System.out.println("Data transfer başladı - timing başlatıldı");
	
	// Enhanced NackSender with congestion control - RTT measurement aktif!
	HybridCongestionController receiverCongestionControl = new HybridCongestionController();
	NackSender sender;
	
	// Use appropriate constructor based on file size
	if (mem_buf != null) {
		// Small file: use legacy MappedByteBuffer mode
		sender = new NackSender(channel, fileId, file_size, total_seq, mem_buf, receiverCongestionControl);
	} else {
		// Large file: use ChunkManager mode
		sender = new NackSender(channel, fileId, file_size, total_seq, chunkManager, receiverCongestionControl);
	}
	
	// Transfer completion için CountDownLatch kullan
	CountDownLatch transferLatch = new CountDownLatch(1);
	
	// Completion callback ayarla
	sender.onTransferComplete = () -> {
		System.out.println("All packets received successfully!");
		transferLatch.countDown();
	};
	
	Thread t = new Thread(sender, "nack-sender");
	t.start();

		// Transfer tamamlanana kadar bekle - timeout yok, gerçek completion
		try {
			// Maksimum 5 dakika bekle (sadece çok büyük dosyalar için güvenlik)
			boolean completed = transferLatch.await(300, TimeUnit.SECONDS);
			
			if(!completed) {
				System.err.println("Transfer timeout - very large file or network issue");
			}
			
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			System.err.println("Transfer interrupted");
		}
		
		t.interrupt();
		
		// Transfer timing'i sonlandır
		transferEndTime = System.currentTimeMillis();
		
		// Transfer tamamlandı - sender'a completion signal gönder
		try {
			ByteBuffer completionFrame = ByteBuffer.allocate(8);
			completionFrame.putInt(0xDEADBEEF); // Magic number for completion
			completionFrame.putInt((int)fileId);
			completionFrame.flip();
			
			channel.write(completionFrame);
			System.out.println("Transfer completion signal sent to sender");
			
			// Signal'ın gönderilmesi için kısa bir bekleme
			Thread.sleep(100);
			
		} catch(Exception e) {
			System.err.println("Failed to send completion signal: " + e);
		}
		
		System.out.println("File transfer completed successfully!");
		

	}else{
		System.out.println("Initialization Error ");
		}
	}
	
	public double getTransferTimeSeconds() {
		if(transferStartTime == 0 || transferEndTime == 0) {
			return 0.0;
		}
		return (transferEndTime - transferStartTime) / 1000.0;
	}
}
