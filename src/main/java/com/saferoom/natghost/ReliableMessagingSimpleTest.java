package com.saferoom.natghost;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Simplified integration test - Direct UDP communication
 */
public class ReliableMessagingSimpleTest {
    
    public static void main(String[] args) throws Exception {
        System.out.println("🧪 RELIABLE MESSAGING - SIMPLE TEST\n");
        
        testDirectCommunication();
        testLargeMessage();
        
        System.out.println("\n✅ ALL TESTS PASSED!");
    }
    
    static void testDirectCommunication() throws Exception {
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("TEST: Direct Alice ↔ Bob Communication");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        // Alice (sender)
        DatagramChannel aliceChannel = DatagramChannel.open();
        aliceChannel.configureBlocking(false);
        aliceChannel.bind(new InetSocketAddress("127.0.0.1", 15001));
        InetSocketAddress aliceAddr = (InetSocketAddress) aliceChannel.getLocalAddress();
        
        // Bob (receiver)
        DatagramChannel bobChannel = DatagramChannel.open();
        bobChannel.configureBlocking(false);
        bobChannel.bind(new InetSocketAddress("127.0.0.1", 15002));
        InetSocketAddress bobAddr = (InetSocketAddress) bobChannel.getLocalAddress();
        
        System.out.println("✓ Alice: " + aliceAddr);
        System.out.println("✓ Bob: " + bobAddr);
        
        CountDownLatch messageLatch = new CountDownLatch(1);
        final String[] receivedMessage = new String[1];
        
        // Create Bob's receiver
        ReliableMessageReceiver bobReceiver = new ReliableMessageReceiver(
            "bob",
            bobChannel,
            (sender, msgId, message) -> {
                receivedMessage[0] = new String(message);
                System.out.println("\n🎉 Bob received: \"" + receivedMessage[0] + "\"");
                messageLatch.countDown();
            }
        );
        
        // Create Alice's sender
        ReliableMessageSender aliceSender = new ReliableMessageSender("alice", aliceChannel);
        
        // Start packet listeners
        Selector aliceSelector = Selector.open();
        aliceChannel.register(aliceSelector, SelectionKey.OP_READ);
        
        Selector bobSelector = Selector.open();
        bobChannel.register(bobSelector, SelectionKey.OP_READ);
        
        // Alice's listener (receives ACK/NACK from Bob)
        Thread aliceListener = new Thread(() -> {
            ByteBuffer buffer = ByteBuffer.allocate(2048);
            while (!Thread.interrupted()) {
                try {
                    if (aliceSelector.select(10) > 0) {
                        aliceSelector.selectedKeys().clear();
                        
                        buffer.clear();
                        InetSocketAddress from = (InetSocketAddress) aliceChannel.receive(buffer);
                        if (from != null) {
                            buffer.flip();
                            byte packetType = buffer.get(0);
                            
                            if (packetType == LLS.SIG_RMSG_ACK) {
                                buffer.rewind();
                                Object[] parsed = LLS.parseReliableMessageACK(buffer);
                                aliceSender.handleACK((long) parsed[4], (int) parsed[5], (long) parsed[6]);
                            } else if (packetType == LLS.SIG_RMSG_NACK) {
                                buffer.rewind();
                                Object[] parsed = LLS.parseReliableMessageNACK(buffer);
                                aliceSender.handleNACK((long) parsed[4], (int[]) parsed[5], from);
                            }
                        }
                    }
                } catch (Exception e) {
                    // Suppress errors during shutdown
                    if (!Thread.interrupted() && e.getMessage() != null) {
                        System.err.println("Alice listener error: " + e.getMessage());
                    }
                }
            }
        }, "AliceListener");
        aliceListener.setDaemon(true);
        aliceListener.start();
        
        // Bob's listener (receives DATA/FIN from Alice)
        Thread bobListener = new Thread(() -> {
            ByteBuffer buffer = ByteBuffer.allocate(2048);
            while (!Thread.interrupted()) {
                try {
                    if (bobSelector.select(10) > 0) {
                        bobSelector.selectedKeys().clear();
                        
                        buffer.clear();
                        InetSocketAddress from = (InetSocketAddress) bobChannel.receive(buffer);
                        if (from != null) {
                            buffer.flip();
                            byte packetType = buffer.get(0);
                            
                            if (packetType == LLS.SIG_RMSG_DATA) {
                                buffer.rewind();
                                Object[] parsed = LLS.parseReliableMessageChunk(buffer);
                                bobReceiver.handleDataChunk(parsed, from);
                            } else if (packetType == LLS.SIG_RMSG_FIN) {
                                buffer.rewind();
                                Object[] parsed = LLS.parseReliableMessageFIN(buffer);
                                bobReceiver.handleFIN((long) parsed[4]);
                            }
                        }
                    }
                } catch (Exception e) {
                    // Suppress errors during shutdown
                    if (!Thread.interrupted() && e.getMessage() != null) {
                        System.err.println("Bob listener error: " + e.getMessage());
                    }
                }
            }
        }, "BobListener");
        bobListener.setDaemon(true);
        bobListener.start();
        
        // Allow listeners to start
        Thread.sleep(100);
        
        // Alice sends message to Bob
        String testMessage = "Hello Bob, this is Alice!";
        System.out.println("\n📤 Alice sending: \"" + testMessage + "\"");
        
        CompletableFuture<Boolean> sendFuture = aliceSender.sendMessage(
            "bob",
            testMessage.getBytes(),
            bobAddr
        );
        
        // Wait for completion
        boolean success = sendFuture.get(5, TimeUnit.SECONDS);
        messageLatch.await(5, TimeUnit.SECONDS);
        
        // Verify
        assert success : "Send failed!";
        assert testMessage.equals(receivedMessage[0]) : "Message mismatch!";
        
        System.out.println("✅ Message verified!");
        
        // Cleanup
        aliceListener.interrupt();
        bobListener.interrupt();
        aliceSender.shutdown();
        bobReceiver.shutdown();
        aliceChannel.close();
        bobChannel.close();
        aliceSelector.close();
        bobSelector.close();
        
        Thread.sleep(200);
    }
    
    /**
     * Test 2: Large message (50KB, ~45 chunks)
     */
    static void testLargeMessage() throws Exception {
        System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("TEST 2: Large Message (50KB)");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        // Alice (sender)
        DatagramChannel aliceChannel = DatagramChannel.open();
        aliceChannel.configureBlocking(false);
        aliceChannel.bind(new InetSocketAddress("127.0.0.1", 16001));
        InetSocketAddress aliceAddr = (InetSocketAddress) aliceChannel.getLocalAddress();
        
        // Bob (receiver)
        DatagramChannel bobChannel = DatagramChannel.open();
        bobChannel.configureBlocking(false);
        bobChannel.bind(new InetSocketAddress("127.0.0.1", 16002));
        InetSocketAddress bobAddr = (InetSocketAddress) bobChannel.getLocalAddress();
        
        System.out.println("✓ Alice: " + aliceAddr);
        System.out.println("✓ Bob: " + bobAddr);
        
        CountDownLatch messageLatch = new CountDownLatch(1);
        final byte[][] receivedData = new byte[1][];
        
        // Create Bob's receiver
        ReliableMessageReceiver bobReceiver = new ReliableMessageReceiver(
            "bob",
            bobChannel,
            (sender, msgId, message) -> {
                receivedData[0] = message;
                System.out.println("\n🎉 Bob received: " + message.length + " bytes");
                messageLatch.countDown();
            }
        );
        
        // Create Alice's sender
        ReliableMessageSender aliceSender = new ReliableMessageSender("alice", aliceChannel);
        
        // Start packet listeners
        Selector aliceSelector = Selector.open();
        aliceChannel.register(aliceSelector, SelectionKey.OP_READ);
        
        Selector bobSelector = Selector.open();
        bobChannel.register(bobSelector, SelectionKey.OP_READ);
        
        // Alice's listener (receives ACK/NACK from Bob)
        Thread aliceListener = new Thread(() -> {
            ByteBuffer buffer = ByteBuffer.allocate(2048);
            while (!Thread.interrupted()) {
                try {
                    if (aliceSelector.select(10) > 0) {
                        aliceSelector.selectedKeys().clear();
                        
                        buffer.clear();
                        InetSocketAddress from = (InetSocketAddress) aliceChannel.receive(buffer);
                        if (from != null) {
                            buffer.flip();
                            byte packetType = buffer.get(0);
                            
                            if (packetType == LLS.SIG_RMSG_ACK) {
                                buffer.rewind();
                                Object[] parsed = LLS.parseReliableMessageACK(buffer);
                                aliceSender.handleACK((long) parsed[4], (int) parsed[5], (long) parsed[6]);
                            } else if (packetType == LLS.SIG_RMSG_NACK) {
                                buffer.rewind();
                                Object[] parsed = LLS.parseReliableMessageNACK(buffer);
                                aliceSender.handleNACK((long) parsed[4], (int[]) parsed[5], from);
                            }
                        }
                    }
                } catch (Exception e) {
                    if (!Thread.interrupted() && e.getMessage() != null) {
                        System.err.println("Alice listener error: " + e.getMessage());
                    }
                }
            }
        }, "AliceListener-Large");
        aliceListener.setDaemon(true);
        aliceListener.start();
        
        // Bob's listener (receives DATA/FIN from Alice)
        Thread bobListener = new Thread(() -> {
            ByteBuffer buffer = ByteBuffer.allocate(2048);
            while (!Thread.interrupted()) {
                try {
                    if (bobSelector.select(10) > 0) {
                        bobSelector.selectedKeys().clear();
                        
                        buffer.clear();
                        InetSocketAddress from = (InetSocketAddress) bobChannel.receive(buffer);
                        if (from != null) {
                            buffer.flip();
                            byte packetType = buffer.get(0);
                            
                            if (packetType == LLS.SIG_RMSG_DATA) {
                                buffer.rewind();
                                Object[] parsed = LLS.parseReliableMessageChunk(buffer);
                                bobReceiver.handleDataChunk(parsed, from);
                            } else if (packetType == LLS.SIG_RMSG_FIN) {
                                buffer.rewind();
                                Object[] parsed = LLS.parseReliableMessageFIN(buffer);
                                bobReceiver.handleFIN((long) parsed[4]);
                            }
                        }
                    }
                } catch (Exception e) {
                    if (!Thread.interrupted() && e.getMessage() != null) {
                        System.err.println("Bob listener error: " + e.getMessage());
                    }
                }
            }
        }, "BobListener-Large");
        bobListener.setDaemon(true);
        bobListener.start();
        
        Thread.sleep(100);
        
        // Create 50KB message with pattern
        byte[] largeMessage = new byte[50000];
        for (int i = 0; i < largeMessage.length; i++) {
            largeMessage[i] = (byte) (i % 256);
        }
        
        int expectedChunks = (largeMessage.length + 1130) / 1131; // 1131 = max payload
        System.out.println("\n📤 Alice sending: " + largeMessage.length + " bytes (~" + expectedChunks + " chunks)");
        
        CompletableFuture<Boolean> sendFuture = aliceSender.sendMessage(
            "bob",
            largeMessage,
            bobAddr
        );
        
        // Wait for completion (longer timeout for large message)
        boolean success = sendFuture.get(15, TimeUnit.SECONDS);
        messageLatch.await(15, TimeUnit.SECONDS);
        
        // Verify
        assert success : "Send failed!";
        assert receivedData[0] != null : "No data received!";
        assert receivedData[0].length == largeMessage.length : "Size mismatch! Expected " + largeMessage.length + ", got " + receivedData[0].length;
        
        // Verify content byte-by-byte
        for (int i = 0; i < largeMessage.length; i++) {
            if (largeMessage[i] != receivedData[0][i]) {
                throw new AssertionError("Content mismatch at byte " + i + "! Expected " + largeMessage[i] + ", got " + receivedData[0][i]);
            }
        }
        
        System.out.println("✅ Content verified (all " + largeMessage.length + " bytes match)");
        System.out.println("✅ Large Message Test PASSED");
        
        // Cleanup
        aliceListener.interrupt();
        bobListener.interrupt();
        aliceSender.shutdown();
        bobReceiver.shutdown();
        aliceChannel.close();
        bobChannel.close();
        aliceSelector.close();
        bobSelector.close();
        
        Thread.sleep(200);
    }
}
