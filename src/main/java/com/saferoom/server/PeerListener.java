package com.saferoom.server;

import com.saferoom.natghost.LLS;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server Listener:
 * - Her user için: ip, ports[], finished flag'i tutar.
 * - User FIN yolladıktan sonra, onun TÜM port listesini target'a PORT_INFO olarak round-robin yollar.
 * - Ardından target'a ALL_DONE yollar.
 * - ALL_DONE gelmeden istemci çıkmıyor.
 */
public class PeerListener extends Thread {

    static final class PeerState {
        final String host;
        final String target;
        final byte   signal;
        InetAddress  ip;

        final List<Integer> ports = Collections.synchronizedList(new ArrayList<>());
        final Set<Integer>  sentToTarget = Collections.synchronizedSet(new HashSet<>());
        int rrCursor = 0;

        volatile boolean finished = false;    
        volatile boolean allDoneSentToTarget = false; 
        volatile long    lastSeenMs = System.currentTimeMillis();

        PeerState(String host, String target, byte signal, InetAddress ip, int port) {
            this.host = host;
            this.target = target;
            this.signal = signal;
            this.ip = ip;
            this.ports.add(port);
        }

        void add(InetAddress ip, int port) {
            this.ip = ip;
            // Duplicate port kontrolü - aynı portu tekrar ekleme
            if (!this.ports.contains(port)) {
                this.ports.add(port);
            }
            this.lastSeenMs = System.currentTimeMillis();
        }
    }

    private static final Map<String, PeerState> STATES = new ConcurrentHashMap<>();
    public static final int UDP_PORT = 45000;

    private static final long CLEANUP_INTERVAL_MS = 30_000;
    private long lastCleanup = System.currentTimeMillis();

    @Override
    public void run() {
        try (Selector selector = Selector.open();
             DatagramChannel channel = DatagramChannel.open()) {

            channel.configureBlocking(false);
            channel.bind(new InetSocketAddress(UDP_PORT));
            channel.register(selector, SelectionKey.OP_READ);

            System.out.println("UDP Listener running on port " + UDP_PORT);

            while (true) {
                selector.select(5);

                Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                while (it.hasNext()) {
                    SelectionKey key = it.next(); it.remove();
                    if (!key.isReadable()) continue;

                    ByteBuffer buf = ByteBuffer.allocate(1024);
                    SocketAddress from = channel.receive(buf);
                    if (from == null) continue;

                    buf.flip();
                    if (!LLS.hasWholeFrame(buf)) continue;

                    InetSocketAddress inet = (InetSocketAddress) from;
                    InetAddress ip = inet.getAddress();
                    int        port = inet.getPort();

                    byte sig = LLS.peekType(buf);
                    short len = LLS.peekLen(buf);

                    switch (sig) {
                        case LLS.SIG_HELLO, LLS.SIG_FIN -> {
                            List<Object> p = LLS.parseMultiple_Packet(buf.duplicate());
                            String sender = (String) p.get(2);
                            String target = (String) p.get(3);
                            byte   signal = sig;

                            PeerState me = STATES.compute(sender, (k, old) -> {
                                if (old == null) return new PeerState(sender, target, signal, ip, port);
                                old.add(ip, port);
                                return old;
                            });

                            if (sig == LLS.SIG_FIN) {
                                me.finished = true;
                                System.out.printf(">> FIN from %s (ports=%s)\n", sender, me.ports);
                            } else {
                                System.out.printf(">> HELLO %s @ %s:%d (ports=%s)\n",
                                        sender, ip.getHostAddress(), port, me.ports);
                            }

                            PeerState tgt = STATES.get(me.target);
                            if (tgt != null) {
                                pushAllIfReady(channel, me, tgt);
                                pushAllIfReady(channel, tgt, me);
                            }
                        }

                        default -> {
                        }
                    }
                }

                long now = System.currentTimeMillis();
                if (now - lastCleanup > CLEANUP_INTERVAL_MS) {
                    STATES.entrySet().removeIf(e -> (now - e.getValue().lastSeenMs) > 120_000);
                    lastCleanup = now;
                }
            }

        } catch (Exception e) {
            System.err.println("PeerListener ERROR: " + e);
        }
    }

    private void pushAllIfReady(DatagramChannel ch, PeerState from, PeerState to) throws Exception {
        if (!from.finished) return;
        if (from.allDoneSentToTarget) return;
        if (to.ports.isEmpty()) return;

        List<Integer> unsent = new ArrayList<>();
        for (int p : from.ports) {
            if (!from.sentToTarget.contains(p)) unsent.add(p);
        }

        List<Integer> toPorts = new ArrayList<>(to.ports);
        for (int p : unsent) {
            int idx = from.rrCursor % toPorts.size();
            int toPort = toPorts.get(idx);
            from.rrCursor++;

            ByteBuffer portPkt = LLS.New_PortInfo_Packet(from.host, from.target, LLS.SIG_PORT, from.ip, p);
            ch.send(portPkt, new InetSocketAddress(to.ip, toPort));
            from.sentToTarget.add(p);
        }

        for (int toPort : toPorts) {
            ByteBuffer donePkt = LLS.New_AllDone_Packet(from.host, to.host);
            ch.send(donePkt, new InetSocketAddress(to.ip, toPort));
        }
        from.allDoneSentToTarget = true;

        System.out.printf("<< ALL_DONE sent (%s → %s). Ports=%s\n", from.host, to.host, from.ports);
    }
}
