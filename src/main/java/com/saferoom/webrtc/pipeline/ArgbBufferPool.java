package com.saferoom.webrtc.pipeline;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Resolution-keyed pool of DirectByteBuffer to avoid per-frame allocations.
 * 
 * <h2>Why Buffer Pool?</h2>
 * <p>
 * Allocating ~1.2 MB (640x480x4 bytes) for every video frame stresses the GC.
 * This pool implementation manages reusable DirectByteBuffers to minimize
 * allocation rates.
 * </p>
 * 
 * <h2>Limits</h2>
 * 
 * <pre>
 * Pipeline depth: 3 (WebRTC queue + FrameProcessor queue + VideoPanel latestFrame)
 * Safety margin: x2 (burst protection)
 * Recommended: 6-8 buffers per resolution
 * </pre>
 * 
 * <h2>Bottleneck Risk</h2>
 * <p>
 * If pool is empty, a new buffer is allocated (non-blocking).
 * Frequent allocations indicate the pool size is too small for the load.
 * </p>
 */
final class ArgbBufferPool {

    /**
     * Buffer limit per resolution.
     */
    private static final int DEFAULT_PER_RESOLUTION_LIMIT = 16;

    /**
     * Maximum total buffers across all resolutions.
     */
    private static final int MAX_TOTAL_BUFFERS = 32;

    private final Map<Long, ArrayBlockingQueue<ByteBuffer>> pools = new ConcurrentHashMap<>();
    private final int perResolutionLimit;

    // ═══════════════════════════════════════════════════════════════════════════════
    // STATISTICS (for monitoring)
    // ═══════════════════════════════════════════════════════════════════════════════
    private final AtomicLong poolHits = new AtomicLong(0); // Buffer reused from pool
    private final AtomicLong poolMisses = new AtomicLong(0); // New buffer allocated
    private final AtomicLong poolDrops = new AtomicLong(0); // Buffer dropped (pool full)
    private final AtomicLong totalBuffersCreated = new AtomicLong(0);

    ArgbBufferPool() {
        this(DEFAULT_PER_RESOLUTION_LIMIT);
    }

    ArgbBufferPool(int perResolutionLimit) {
        this.perResolutionLimit = Math.max(1, perResolutionLimit);
    }

    /**
     * Acquire a ByteBuffer from the pool.
     * 
     * @return Reused DirectByteBuffer from pool, or new buffer if pool is empty
     */
    ByteBuffer acquire(int width, int height) {
        long key = toKey(width, height);
        ArrayBlockingQueue<ByteBuffer> queue = pools.computeIfAbsent(
                key, ignored -> new ArrayBlockingQueue<>(perResolutionLimit));

        ByteBuffer buffer = queue.poll();

        // Ensure buffer is large enough
        int requiredCapacity = width * height * 4;

        if (buffer != null && buffer.capacity() >= requiredCapacity) {
            // ✅ Pool hit - reusing existing buffer
            poolHits.incrementAndGet();
            buffer.clear(); // Reset position/limit
            return buffer;
        }

        // ⚠️ Pool miss - need to allocate new buffer
        poolMisses.incrementAndGet();
        long created = totalBuffersCreated.incrementAndGet();

        // Log warning if we're creating too many buffers (indicates pool exhaustion)
        if (created > MAX_TOTAL_BUFFERS && created % 10 == 0) {
            System.err.printf("[ArgbBufferPool] ⚠️ High allocation count: %d buffers created (hits=%d, misses=%d)%n",
                    created, poolHits.get(), poolMisses.get());
        }

        // Use DirectByteBuffer for zero-copy interoperability
        ByteBuffer newBuf = ByteBuffer.allocateDirect(requiredCapacity);
        newBuf.order(ByteOrder.nativeOrder()); // Important for JNI interaction
        return newBuf;
    }

    /**
     * Release a buffer back to the pool.
     * 
     * @param buffer Buffer to release (will be dropped if pool is full)
     */
    void release(int width, int height, ByteBuffer buffer) {
        if (buffer == null) {
            return;
        }

        int requiredCapacity = width * height * 4;
        if (buffer.capacity() < requiredCapacity) {
            // Don't pool buffers that are too small (should shouldn't happen but sanity
            // check)
            return;
        }

        long key = toKey(width, height);
        ArrayBlockingQueue<ByteBuffer> queue = pools.computeIfAbsent(
                key, ignored -> new ArrayBlockingQueue<>(perResolutionLimit));

        if (!queue.offer(buffer)) {
            // Pool is full, buffer is dropped
            poolDrops.incrementAndGet();
        }
    }

    /**
     * Get pool statistics for monitoring.
     * 
     * @return String with hit/miss/drop counts
     */
    public String getStats() {
        long hits = poolHits.get();
        long misses = poolMisses.get();
        long total = hits + misses;
        double hitRate = total > 0 ? (hits * 100.0 / total) : 0;

        return String.format("hits=%d, misses=%d, drops=%d, hitRate=%.1f%%, totalCreated=%d",
                hits, misses, poolDrops.get(), hitRate, totalBuffersCreated.get());
    }

    /**
     * Get current pool sizes for each resolution.
     */
    public String getPoolSizes() {
        StringBuilder sb = new StringBuilder();
        pools.forEach((key, queue) -> {
            int width = (int) (key >> 32);
            int height = (int) (key & 0xFFFFFFFFL);
            sb.append(String.format("%dx%d: %d/%d, ", width, height, queue.size(), perResolutionLimit));
        });
        return sb.length() > 0 ? sb.substring(0, sb.length() - 2) : "empty";
    }

    private static long toKey(int width, int height) {
        return (((long) width) << 32) | (height & 0xFFFFFFFFL);
    }
}
