package com.saferoom.rooms;

import com.saferoom.rooms.client.RoomSignalingClient;
import com.saferoom.rooms.grpc.GetSeedsResponse;
import com.saferoom.rooms.grpc.JoinRoomResponse;
import com.saferoom.rooms.grpc.RoomEvent;
import com.saferoom.rooms.server.RoomServiceImpl;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

public class RoomsIntegrationTest {

    private static final Logger logger = Logger.getLogger(RoomsIntegrationTest.class.getName());
    private static Server server;
    private static RoomSignalingClient client;
    private static int port;

    @BeforeAll
    public static void setup() throws IOException {
        // Start Server on random port
        server = ServerBuilder.forPort(0)
                .addService(new RoomServiceImpl())
                .build()
                .start();

        port = server.getPort();
        logger.info("Test Server started on port: " + port);

        // Connect Client
        client = new RoomSignalingClient();
        client.connect("localhost", port);
    }

    @AfterAll
    public static void teardown() throws InterruptedException {
        if (client != null) {
            client.shutdown();
        }
        if (server != null) {
            server.shutdown();
        }
    }

    @Test
    public void testJoinRoomAndEventStream() throws InterruptedException {
        String roomId = "test-room-1";
        String nodeId = "node-1";
        String pubKey = "pub-key-1";

        // 1. Join Room
        JoinRoomResponse response = client.joinRoom(roomId, nodeId, pubKey);

        assertTrue(response.getSuccess(), "Join should be successful");
        assertEquals(1L, response.getCurrentEpoch(), "Initial epoch should be 1");

        // 2. Verify Presence Event (via listener)
        BlockingQueue<RoomEvent> events = new LinkedBlockingQueue<>();
        client.addListener(new RoomSignalingClient.RoomEventListener() {
            @Override
            public void onEvent(RoomEvent event) {
                events.offer(event);
            }

            @Override
            public void onDisconnected() {
            }
        });

        // Wait for event (Join triggers broadcastPresence)
        RoomEvent event = events.poll(5, TimeUnit.SECONDS);
        assertNotNull(event, "Should receive presence event");
        assertEquals(RoomEvent.EventType.ROOM_PRESENCE, event.getType());
        assertEquals(1, event.getPresence().getConnectedPeersCount());

        // 3. Get Seeds
        GetSeedsResponse seeds = client.getSeeds(roomId, 1L);
        assertEquals(1L, seeds.getEpoch());
        // Might be empty if logic limits to 5 and we have 1 peer (who is also leaf)
        // Adjust test implementation or server logic if needed, but basic call works
    }
}
