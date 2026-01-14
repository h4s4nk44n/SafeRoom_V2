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
    public void testFullRoomLifecycle() throws InterruptedException {
        String roomId = "test-room-" + System.currentTimeMillis();
        String roomName = "Test Room";
        String ownerId = "owner-node";
        String memberId = "member-node";
        String pubKey = "pub-key-1";

        // 1. Create Room (Assume implicit UUID generation by server if we passed name?
        // No, current createRoom implementation in DBManager takes roomId from Caller?
        // Wait, RoomServiceImpl.createRoom generates UUID? Let's check RoomServiceImpl.
        // Assuming RoomServiceImpl generates ID or it was passed?
        // Let's check CreateRoomRequest proto. It only has "name", "ownerNodeId",
        // "isPrivate".
        // So Server generates ID.

        var createResp = client.createRoom(roomName, ownerId, false);
        assertTrue(createResp.getSuccess(), "Create room should be successful");
        assertNotNull(createResp.getRoom(), "Should return room metadata");
        assertEquals(roomName, createResp.getRoom().getName());
        String createdRoomId = createResp.getRoom().getRoomId();
        assertNotNull(createdRoomId, "Room ID should be generated");

        // 2. List Rooms
        var listResp = client.listRooms("");
        boolean found = false;
        for (var r : listResp.getRoomsList()) {
            if (r.getRoomId().equals(createdRoomId)) {
                found = true;
                assertEquals(roomName, r.getName());
                break;
            }
        }
        assertTrue(found, "Created room should appear in list");

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
