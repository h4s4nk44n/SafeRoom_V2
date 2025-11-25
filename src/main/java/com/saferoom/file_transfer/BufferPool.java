package com.saferoom.file_transfer;

import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Simple fixed-size pool for direct ByteBuffers to avoid per-packet allocations
 * when zero-copy mode is disabled.
 */
public final class BufferPool {
    private final ArrayBlockingQueue<ByteBuffer> queue;
    private final int bufferSize;

    public BufferPool(int capacity, int bufferSize) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.bufferSize = bufferSize;
        this.queue = new ArrayBlockingQueue<>(capacity);
        for (int i = 0; i < capacity; i++) {
            queue.offer(ByteBuffer.allocateDirect(bufferSize));
        }
    }

    public ByteBuffer acquire() {
        ByteBuffer buffer = queue.poll();
        if (buffer == null) {
            buffer = ByteBuffer.allocateDirect(bufferSize);
        }
        return buffer;
    }

    public void release(ByteBuffer buffer) {
        if (buffer == null) {
            return;
        }
        buffer.clear();
        if (buffer.capacity() != bufferSize || !queue.offer(buffer)) {
            // drop buffer if pool is full or wrong size
        }
    }
}

