package com.saferoom.transport;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.DatagramChannel;

public interface TransportEndpoint extends Closeable {
    DatagramChannel channel();

    default boolean isOpen() {
        try {
            return channel().isOpen();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    default void close() throws IOException {
        channel().close();
    }
}

