package com.saferoom.webrtc.pipeline;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Lightweight stats snapshot for a single {@link FrameProcessor}.
 */
public final class VideoPipelineStats {

    private final AtomicLong processedFrames = new AtomicLong();
    private final AtomicLong droppedFrames = new AtomicLong();
    private final AtomicLong totalConversionNanos = new AtomicLong();

    private volatile int lastQueueDepth;
    private volatile long lastFrameTimestampNs = System.nanoTime();
    private volatile long lastLogTimestampNs = System.nanoTime();

    void recordProcessed(long conversionDurationNanos, int queueDepth) {
        processedFrames.incrementAndGet();
        totalConversionNanos.addAndGet(conversionDurationNanos);
        lastQueueDepth = queueDepth;
        lastFrameTimestampNs = System.nanoTime();
    }

    void recordDrop() {
        droppedFrames.incrementAndGet();
    }

    boolean shouldLogStall(long nowNs, long stallThresholdNs, long minGapNs) {
        boolean stalled = nowNs - lastFrameTimestampNs > stallThresholdNs;
        boolean canLog = nowNs - lastLogTimestampNs > minGapNs;
        if (stalled && canLog) {
            lastLogTimestampNs = nowNs;
            return true;
        }
        return false;
    }

    public long getProcessedFrames() {
        return processedFrames.get();
    }

    public long getDroppedFrames() {
        return droppedFrames.get();
    }

    public double getAverageConversionMillis() {
        long processed = processedFrames.get();
        if (processed == 0) {
            return 0.0;
        }
        return (totalConversionNanos.get() / 1_000_000.0) / processed;
    }

    public int getLastQueueDepth() {
        return lastQueueDepth;
    }

    public long getLastFrameTimestampNs() {
        return lastFrameTimestampNs;
    }

    @Override
    public String toString() {
        return String.format("frames=%d dropped=%d avgConvert=%.2fms queue=%d",
            getProcessedFrames(), getDroppedFrames(), getAverageConversionMillis(), lastQueueDepth);
    }
}

