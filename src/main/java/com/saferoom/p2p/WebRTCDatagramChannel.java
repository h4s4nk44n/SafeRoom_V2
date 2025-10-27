package com.saferoom.p2p;

import dev.onvoid.webrtc.RTCDataChannel;
import dev.onvoid.webrtc.RTCDataChannelBuffer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * WebRTC DataChannel wrapper for sending/receiving LLS protocol packets
 * 
 * This is used by KeepAliveManager to send DNS keep-alive packets
 * and by ReliableMessageSender/Receiver for chunked messaging.
 * 
 * Unlike UDP DatagramChannel, this uses WebRTC's encrypted DataChannel.
 */
public class WebRTCDatagramChannel {
    
    private final RTCDataChannel dataChannel;
    private final InetSocketAddress remoteAddress;
    private final InetSocketAddress localAddress;
    
    // Queue for received messages (WebRTC callback → receive() method)
    private final BlockingQueue<ByteBuffer> receiveQueue = new LinkedBlockingQueue<>();
    
    private volatile boolean open = true;
    
    /**
     * Constructor
     * @param dataChannel The WebRTC DataChannel to wrap
     * @param remoteAddress Remote peer's address (for logging/tracking)
     * @param localAddress Local address (fake port for compatibility)
     */
    public WebRTCDatagramChannel(RTCDataChannel dataChannel, 
                                  InetSocketAddress remoteAddress,
                                  InetSocketAddress localAddress) {
        this.dataChannel = dataChannel;
        this.remoteAddress = remoteAddress;
        this.localAddress = localAddress;
    }
    
    /**
     * Send packet via WebRTC DataChannel
     * Called by KeepAliveManager to send DNS keep-alive packets
     */
    public int send(ByteBuffer src, SocketAddress target) throws IOException {
        if (!open) {
            throw new IOException("Channel is closed");
        }
        
        try {
            // Get data from ByteBuffer
            byte[] data = new byte[src.remaining()];
            src.get(data);
            
            // Send via WebRTC DataChannel
            RTCDataChannelBuffer buffer = new RTCDataChannelBuffer(ByteBuffer.wrap(data), true);
            dataChannel.send(buffer);
            
            return data.length;
        } catch (Exception e) {
            throw new IOException("Failed to send via DataChannel", e);
        }
    }
    
    /**
     * Receive packet from WebRTC DataChannel
     * Called by KeepAliveManager to receive messages
     */
    public SocketAddress receive(ByteBuffer dst) throws IOException {
        if (!open) {
            throw new IOException("Channel is closed");
        }
        
        try {
            // Non-blocking: return null if no data
            ByteBuffer received = receiveQueue.poll();
            if (received == null) {
                return null;
            }
            
            // Copy data to destination buffer
            dst.put(received);
            
            return remoteAddress;
        } catch (Exception e) {
            throw new IOException("Error receiving data", e);
        }
    }
    
    /**
     * Called by WebRTC DataChannel observer when message arrives
     * This pushes data into the receive queue for KeepAliveManager
     */
    public void onDataChannelMessage(RTCDataChannelBuffer buffer) {
        try {
            ByteBuffer data = buffer.data.duplicate();
            receiveQueue.offer(data);
        } catch (Exception e) {
            System.err.println("[WebRTCDatagramChannel] Error queuing message: " + e.getMessage());
        }
    }
    
    public SocketAddress getLocalAddress() {
        return localAddress;
    }
    
    public SocketAddress getRemoteAddress() {
        return remoteAddress;
    }
    
    public boolean isOpen() {
        return open && dataChannel != null;
    }
    
    public void close() {
        open = false;
        // DataChannel will be closed by P2PConnectionManager
    }
    
    public RTCDataChannel getDataChannel() {
        return dataChannel;
    }
}
