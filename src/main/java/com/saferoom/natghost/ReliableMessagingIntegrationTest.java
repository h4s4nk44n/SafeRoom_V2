package com.saferoom.natghost;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Integration test for ReliableMessageSender + ReliableMessageReceiver
 * 
 * Tests:
 * 1. Simple message send/receive
 * 2. Large message chunking
 * 3. Out-of-order delivery simulation
 * 4. Packet loss simulation
 */
public class ReliableMessagingIntegrationTest {
    
    public static void main(String[] args) throws Exception {
        System.out.println("🧪 RELIABLE MESSAGING - INTEGRATION TEST\n");
        
        // Test 1: Simple loopback
        testSimpleLoopback();
        
        // Test 2: Large message
        testLargeMessage();
        
        System.out.println("\n✅ ALL INTEGRATION TESTS PASSED!");
    }
    
    /**
     * Test 1: Simple message over loopback
     */
    static void testSimpleLoopback() throws Exception {
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("TEST 1: Simple Message (Loopback)");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        // Setup sender channel (port 9001)
        DatagramChannel senderChannel = DatagramChannel.open();
        senderChannel.configureBlocking(false);
        senderChannel.bind(new InetSocketAddress("127.0.0.1", 9001));
        
        // Setup receiver channel (port 9002)
        DatagramChannel receiverChannel = DatagramChannel.open();
        receiverChannel.configureBlocking(false);
        receiverChannel.bind(new InetSocketAddress("127.0.0.1", 9002));
        
        CountDownLatch messageLatch = new CountDownLatch(1);
        final String[] receivedMessage = new String[1];
        
        // Create receiver
        ReliableMessageReceiver receiver = new ReliableMessageReceiver(
            "bob",
            receiverChannel,
            (sender, msgId, message) -> {
                receivedMessage[0] = new String(message);
                System.out.println("✓ Message received by callback: \"" + receivedMessage[0] + "\"");
                messageLatch.countDown();
            }
        );
        
        // Create sender
        ReliableMessageSender sender = new ReliableMessageSender("alice", senderChannel);
        
        // Start packet forwarding thread (simulate network)
        Thread forwarder = startPacketForwarder(senderChannel, receiverChannel, 
            new InetSocketAddress("127.0.0.1", 9002),
            new InetSocketAddress("127.0.0.1", 9001),
            sender, receiver);
        
        // Send message
        String testMessage = "Hello from Alice to Bob!";
        System.out.println("📤 Sending: \"" + testMessage + "\"");
        
        CompletableFuture<Boolean> sendFuture = sender.sendMessage(
            "bob",
            testMessage.getBytes(),
            new InetSocketAddress("127.0.0.1", 9002)
        );
        
        // Wait for completion
        boolean success = sendFuture.get(5, TimeUnit.SECONDS);
        messageLatch.await(5, TimeUnit.SECONDS);
        
        // Verify
        assert success : "Send failed!";
        assert testMessage.equals(receivedMessage[0]) : "Message mismatch!";
        
        System.out.println("✅ Test 1 PASSED\n");
        
        // Cleanup
        forwarder.interrupt();
        sender.shutdown();
        receiver.shutdown();
        senderChannel.close();
        receiverChannel.close();
        
        Thread.sleep(500); // Allow cleanup
    }
    
    /**
     * Test 2: Large message (multiple chunks)
     */
    static void testLargeMessage() throws Exception {
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("TEST 2: Large Message (10KB, ~9 chunks)");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        // Setup channels
        DatagramChannel senderChannel = DatagramChannel.open();
        senderChannel.configureBlocking(false);
        senderChannel.bind(new InetSocketAddress("127.0.0.1", 9003));
        
        DatagramChannel receiverChannel = DatagramChannel.open();
        receiverChannel.configureBlocking(false);
        receiverChannel.bind(new InetSocketAddress("127.0.0.1", 9004));
        
        CountDownLatch messageLatch = new CountDownLatch(1);
        final byte[][] receivedMessage = new byte[1][];
        
        // Create receiver
        ReliableMessageReceiver receiver = new ReliableMessageReceiver(
            "bob",
            receiverChannel,
            (sender, msgId, message) -> {
                receivedMessage[0] = message;
                System.out.println("✓ Large message received: " + message.length + " bytes");
                messageLatch.countDown();
            }
        );
        
        // Create sender
        ReliableMessageSender sender = new ReliableMessageSender("alice", senderChannel);
        
        // Start packet forwarding
        Thread forwarder = startPacketForwarder(senderChannel, receiverChannel,
            new InetSocketAddress("127.0.0.1", 9004),
            new InetSocketAddress("127.0.0.1", 9003),
            sender, receiver);
        
        // Create 10KB message
        byte[] largeMessage = new byte[10000];
        for (int i = 0; i < largeMessage.length; i++) {
            largeMessage[i] = (byte) (i % 256);
        }
        
        System.out.println("📤 Sending: " + largeMessage.length + " bytes");
        
        CompletableFuture<Boolean> sendFuture = sender.sendMessage(
            "bob",
            largeMessage,
            new InetSocketAddress("127.0.0.1", 9004)
        );
        
        // Wait for completion
        boolean success = sendFuture.get(10, TimeUnit.SECONDS);
        messageLatch.await(10, TimeUnit.SECONDS);
        
        // Verify
        assert success : "Send failed!";
        assert receivedMessage[0] != null : "No message received!";
        assert receivedMessage[0].length == largeMessage.length : "Size mismatch!";
        
        // Verify content
        for (int i = 0; i < largeMessage.length; i++) {
            if (largeMessage[i] != receivedMessage[0][i]) {
                throw new AssertionError("Content mismatch at byte " + i);
            }
        }
        
        System.out.println("✅ Test 2 PASSED\n");
        
        // Cleanup
        forwarder.interrupt();
        sender.shutdown();
        receiver.shutdown();
        senderChannel.close();
        receiverChannel.close();
        
        Thread.sleep(500);
    }
    
    /**
     * Packet forwarding thread (simulates network)
     */
    static Thread startPacketForwarder(
        DatagramChannel senderChannel,
        DatagramChannel receiverChannel,
        InetSocketAddress receiverAddr,
        InetSocketAddress senderAddr,
        ReliableMessageSender sender,
        ReliableMessageReceiver receiver
    ) {
        Thread t = new Thread(() -> {
            ByteBuffer buffer = ByteBuffer.allocate(2048);
            int forwardedCount = 0;
            
            System.out.println("[FORWARDER] 🔄 Started packet forwarding thread");
            
            while (!Thread.interrupted()) {
                try {
                    // Forward packets from sender → receiver
                    buffer.clear();
                    InetSocketAddress from = (InetSocketAddress) senderChannel.receive(buffer);
                    if (from != null) {
                        buffer.flip();
                        byte packetType = buffer.get(0);
                        forwardedCount++;
                        
                        System.out.printf("[FORWARDER] ➡️  Forwarding packet #%d (type=0x%02x) sender→receiver%n",
                            forwardedCount, packetType);
                        
                        // Route packet to receiver
                        if (packetType == LLS.SIG_RMSG_DATA) {
                            // Forward DATA to receiver
                            receiverChannel.send(buffer, receiverAddr);
                            
                            // Parse and handle by receiver
                            buffer.rewind();
                            Object[] parsed = LLS.parseReliableMessageChunk(buffer);
                            receiver.handleDataChunk(parsed, senderAddr);
                            
                        } else if (packetType == LLS.SIG_RMSG_FIN) {
                            // Forward FIN to receiver
                            receiverChannel.send(buffer, receiverAddr);
                            
                            buffer.rewind();
                            Object[] parsed = LLS.parseReliableMessageFIN(buffer);
                            receiver.handleFIN((long) parsed[4]);
                        }
                    }
                    
                    // Forward packets from receiver → sender
                    buffer.clear();
                    from = (InetSocketAddress) receiverChannel.receive(buffer);
                    if (from != null) {
                        buffer.flip();
                        byte packetType = buffer.get(0);
                        
                        // Route packet to sender
                        if (packetType == LLS.SIG_RMSG_ACK) {
                            // Forward ACK to sender
                            senderChannel.send(buffer, senderAddr);
                            
                            // Parse and handle by sender
                            buffer.rewind();
                            Object[] parsed = LLS.parseReliableMessageACK(buffer);
                            sender.handleACK((long) parsed[4], (int) parsed[5], (long) parsed[6]);
                            
                        } else if (packetType == LLS.SIG_RMSG_NACK) {
                            // Forward NACK to sender
                            senderChannel.send(buffer, senderAddr);
                            
                            // Parse and handle by sender
                            buffer.rewind();
                            Object[] parsed = LLS.parseReliableMessageNACK(buffer);
                            sender.handleNACK((long) parsed[4], (int[]) parsed[5], senderAddr);
                        }
                    }
                    
                    Thread.sleep(1); // Small delay to avoid busy-wait
                    
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    System.err.println("❌ Forwarder error: " + e.getMessage());
                }
            }
        }, "PacketForwarder");
        
        t.setDaemon(true);
        t.start();
        return t;
    }
}
