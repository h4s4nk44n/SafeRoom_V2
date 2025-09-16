package com.saferoom.natghost;

import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * Represents a registered user in the P2P signaling server
 * Contains both public and local NAT information for optimal P2P connections
 */
public class RegisteredUser {
    public final String username;
    public final InetAddress publicIP;
    public final int publicPort;
    public final InetAddress localIP;
    public final int localPort;
    public final InetSocketAddress clientAddress; // For server to send notifications
    public final long registrationTime;
    
    public RegisteredUser(String username, 
                         InetAddress publicIP, int publicPort,
                         InetAddress localIP, int localPort,
                         InetSocketAddress clientAddress) {
        this.username = username;
        this.publicIP = publicIP;
        this.publicPort = publicPort;
        this.localIP = localIP;
        this.localPort = localPort;
        this.clientAddress = clientAddress;
        this.registrationTime = System.currentTimeMillis();
    }
    
    @Override
    public String toString() {
        return String.format("RegisteredUser{username='%s', public=%s:%d, local=%s:%d, client=%s}", 
            username, publicIP.getHostAddress(), publicPort, 
            localIP.getHostAddress(), localPort, clientAddress);
    }
    
    /**
     * Check if registration is still valid (not expired)
     * @param maxAgeMs Maximum age in milliseconds
     * @return true if registration is still valid
     */
    public boolean isValid(long maxAgeMs) {
        return (System.currentTimeMillis() - registrationTime) < maxAgeMs;
    }
}
