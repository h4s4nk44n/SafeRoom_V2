package com.saferoom.webrtc.pipeline;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple resolution-keyed pool of ARGB int[] buffers to avoid per-frame allocations.
 */
final class ArgbBufferPool {

    private static final int DEFAULT_PER_RESOLUTION_LIMIT = 3;

    private final Map<Long, ArrayBlockingQueue<int[]>> pools = new ConcurrentHashMap<>();
    private final int perResolutionLimit;

    ArgbBufferPool() {
        this(DEFAULT_PER_RESOLUTION_LIMIT);
    }

    ArgbBufferPool(int perResolutionLimit) {
        this.perResolutionLimit = Math.max(1, perResolutionLimit);
    }

    int[] acquire(int width, int height) {
        long key = toKey(width, height);
        ArrayBlockingQueue<int[]> queue = pools.computeIfAbsent(
            key, ignored -> new ArrayBlockingQueue<>(perResolutionLimit));
        int[] buffer = queue.poll();
        if (buffer == null || buffer.length < width * height) {
            return new int[width * height];
        }
        return buffer;
    }

    void release(int width, int height, int[] buffer) {
        if (buffer == null || buffer.length < width * height) {
            return;
        }
        long key = toKey(width, height);
        pools.computeIfAbsent(
            key, ignored -> new ArrayBlockingQueue<>(perResolutionLimit))
            .offer(buffer);
    }

    private static long toKey(int width, int height) {
        return (((long) width) << 32) | (height & 0xFFFFFFFFL);
    }
}

