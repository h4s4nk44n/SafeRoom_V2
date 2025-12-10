package com.saferoom.webrtc.pipeline;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Resolution-keyed pool of ARGB int[] buffers to avoid per-frame allocations.
 * 
 * <h2>Neden Buffer Pool?</h2>
 * <p>Her video frame için ~1.2 MB (640x480x4 bytes) allocation yapmak GC'yi zorlar.
 * Pool, buffer'ları yeniden kullanarak allocation sayısını minimize eder.</p>
 * 
 * <h2>Limit Hesabı</h2>
 * <pre>
 * Pipeline depth: 3 (WebRTC queue + FrameProcessor queue + VideoPanel latestFrame)
 * Safety margin: x2 (burst protection)
 * Recommended: 6 buffer per resolution
 * 
 * Multi-participant: Her VideoPanel kendi FrameProcessor'ına sahip,
 * dolayısıyla aynı resolution için shared pool'dan buffer alırlar.
 * 4 katılımcı × 3 active = 12 buffer gerekebilir (worst case)
 * </pre>
 * 
 * <h2>Darboğaz Riski</h2>
 * <p>Pool boşaldığında yeni buffer allocate edilir (donma olmaz).
 * Ama sık allocation GC pause'a yol açabilir.</p>
 */
final class ArgbBufferPool {

    /**
     * Buffer limit per resolution.
     * 
     * Hesap:
     * - 1 video stream: 3 buffer yeterli
     * - 4 video stream (aynı resolution): 4 × 3 = 12 buffer gerekli
     * - Safety margin için 8 seçildi (4 stream için yeterli)
     */
    private static final int DEFAULT_PER_RESOLUTION_LIMIT = 8;
    
    /**
     * Maximum total buffers across all resolutions.
     * 640x480 = 1.2 MB, 1080p = 8.3 MB per buffer.
     * 32 buffer × 1.2 MB = ~38 MB max pool size (reasonable)
     */
    private static final int MAX_TOTAL_BUFFERS = 32;

    private final Map<Long, ArrayBlockingQueue<int[]>> pools = new ConcurrentHashMap<>();
    private final int perResolutionLimit;
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // STATISTICS (for monitoring)
    // ═══════════════════════════════════════════════════════════════════════════════
    private final AtomicLong poolHits = new AtomicLong(0);      // Buffer reused from pool
    private final AtomicLong poolMisses = new AtomicLong(0);    // New buffer allocated
    private final AtomicLong poolDrops = new AtomicLong(0);     // Buffer dropped (pool full)
    private final AtomicLong totalBuffersCreated = new AtomicLong(0);

    ArgbBufferPool() {
        this(DEFAULT_PER_RESOLUTION_LIMIT);
    }

    ArgbBufferPool(int perResolutionLimit) {
        this.perResolutionLimit = Math.max(1, perResolutionLimit);
    }

    /**
     * Acquire a buffer from the pool.
     * 
     * @return Reused buffer from pool, or new buffer if pool is empty
     */
    int[] acquire(int width, int height) {
        long key = toKey(width, height);
        ArrayBlockingQueue<int[]> queue = pools.computeIfAbsent(
            key, ignored -> new ArrayBlockingQueue<>(perResolutionLimit));
        
        int[] buffer = queue.poll();
        
        if (buffer != null && buffer.length >= width * height) {
            // ✅ Pool hit - reusing existing buffer
            poolHits.incrementAndGet();
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
        
        return new int[width * height];
    }

    /**
     * Release a buffer back to the pool.
     * 
     * @param buffer Buffer to release (will be dropped if pool is full)
     */
    void release(int width, int height, int[] buffer) {
        if (buffer == null || buffer.length < width * height) {
            return;
        }
        
        long key = toKey(width, height);
        ArrayBlockingQueue<int[]> queue = pools.computeIfAbsent(
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
            int width = (int)(key >> 32);
            int height = (int)(key & 0xFFFFFFFFL);
            sb.append(String.format("%dx%d: %d/%d, ", width, height, queue.size(), perResolutionLimit));
        });
        return sb.length() > 0 ? sb.substring(0, sb.length() - 2) : "empty";
    }

    private static long toKey(int width, int height) {
        return (((long) width) << 32) | (height & 0xFFFFFFFFL);
    }
}

