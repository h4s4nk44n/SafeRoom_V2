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
        // Start Server on random port using REAL DB (embedded in RoomServiceImpl)
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
    public void testFullRoomLifecycle() throws InterruptedException {
        // NOTE: This test requires a running MariaDB instance accessible by DBManager.
        // If DB is offline, test will fail with Communications link failure.

        String roomName = "Real DB Test Room " + System.currentTimeMillis();
        String ownerId = "owner-" + System.currentTimeMillis(); // Unique owner
        String memberId = "member-" + System.currentTimeMillis();
        String pubKey = "pub-key-1";

        // 1. Create Room
        var createResp = client.createRoom(roomName, ownerId, false);

        // If DB fails, these assertions will fail
        assertTrue(createResp.getSuccess(), "Create room should be successful (Check DB connection)");
        assertNotNull(createResp.getRoom(), "Should return room metadata");
        assertEquals(roomName, createResp.getRoom().getName());
        String createdRoomId = createResp.getRoom().getRoomId();
        assertNotNull(createdRoomId, "Room ID should be generated");

        // 2. List Rooms
        var listResp = client.listRooms(roomName); // Search specifically for this room
        boolean found = false;
        for (var r : listResp.getRoomsList()) {
            if (r.getRoomId().equals(createdRoomId)) {
                found = true;
                assertEquals(roomName, r.getName());
                break;
            }
        }
        assertTrue(found, "Created room should appear in list (Check DB persistence)");

        // 3. Join Room
        JoinRoomResponse joinResp = client.joinRoom(createdRoomId, memberId, pubKey);
        assertTrue(joinResp.getSuccess(), "Join should be successful");

        // 4. Verify Presence Event
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

        // Wait for event
        RoomEvent event = events.poll(5, TimeUnit.SECONDS);
        assertNotNull(event, "Should receive presence event");
        assertEquals(RoomEvent.EventType.ROOM_PRESENCE, event.getType());
    }
}
