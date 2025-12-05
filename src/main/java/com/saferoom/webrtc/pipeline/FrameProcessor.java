package com.saferoom.webrtc.pipeline;

import dev.onvoid.webrtc.media.video.I420Buffer;
import dev.onvoid.webrtc.media.video.VideoFrame;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Virtual-thread based frame processor. Each instance owns a dedicated virtual thread that
 * performs decode → convert steps off the JavaFX thread and pushes paint-ready frames to a consumer.
 */
public final class FrameProcessor implements AutoCloseable {

    private static final String QUEUE_CAPACITY_PROPERTY = "saferoom.video.queue.capacity";
    private static final int DEFAULT_QUEUE_CAPACITY =
        Integer.getInteger(QUEUE_CAPACITY_PROPERTY, 12);
    private static final Duration POLL_TIMEOUT = Duration.ofMillis(50);
    private static final long STALL_THRESHOLD_NANOS = Duration.ofSeconds(2).toNanos();
    private static final long STALL_LOG_INTERVAL_NANOS = Duration.ofSeconds(5).toNanos();

    private final BlockingQueue<VideoFrame> queue;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicBoolean paused = new AtomicBoolean(false);
    private final Consumer<FrameRenderResult> consumer;
    private final Thread workerThread;
    private final VideoPipelineStats stats = new VideoPipelineStats();

    public FrameProcessor(Consumer<FrameRenderResult> consumer) {
        this(consumer, DEFAULT_QUEUE_CAPACITY);
    }

    public FrameProcessor(Consumer<FrameRenderResult> consumer, int capacity) {
        this.consumer = Objects.requireNonNull(consumer, "consumer");
        int resolvedCapacity = capacity > 0 ? capacity : DEFAULT_QUEUE_CAPACITY;
        this.queue = new ArrayBlockingQueue<>(Math.max(1, resolvedCapacity));
        
        // ALWAYS use platform thread for FrameProcessor
        // Virtual threads can have issues with native code (webrtc I420Buffer operations)
        // This affects BOTH Windows and Linux
        this.workerThread = Thread.ofPlatform()
            .name("frame-processor-" + System.identityHashCode(this))
            .daemon(true)
            .unstarted(this::processLoop);
        System.out.println("[FrameProcessor] Using platform thread for native interop");
        
        this.workerThread.start();
    }

    public void submit(VideoFrame frame) {
        if (!running.get() || frame == null) {
            return;
        }
        if (paused.get()) {
            return;
        }
        frame.retain();
        while (!queue.offer(frame)) {
            stats.recordDrop();
            VideoFrame dropped = queue.poll();
            if (dropped == null) {
                break;
            }
            dropped.release();
        }
    }

    // Debug counter
    private volatile long processedCount = 0;
    private volatile long lastProcessedLog = 0;
    
    private void processLoop() {
        System.out.println("[FrameProcessor] Process loop started on thread: " + Thread.currentThread().getName());
        
        while (running.get()) {
            try {
                VideoFrame frame = queue.poll(POLL_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
                if (frame == null) {
                    logIfStalled();
                    continue;
                }
                if (paused.get()) {
                    frame.release();
                    continue;
                }
                try {
                    long start = System.nanoTime();
                    FrameRenderResult result = convertFrame(frame);
                    long processingTimeMs = (System.nanoTime() - start) / 1_000_000;
                    stats.recordProcessed(System.nanoTime() - start, queue.size());
                    
                    // Log processing stats every 100 frames
                    processedCount++;
                    if (processedCount - lastProcessedLog >= 100) {
                        System.out.printf("[FrameProcessor] Processed %d frames (last took %dms, queue=%d)%n",
                            processedCount, processingTimeMs, queue.size());
                        lastProcessedLog = processedCount;
                    }
                    
                    consumer.accept(result);
                } finally {
                    frame.release();
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                System.out.println("[FrameProcessor] Process loop interrupted");
                break;
            } catch (Throwable t) {
                System.err.println("[FrameProcessor] ERROR in processLoop: " + t.getMessage());
                t.printStackTrace();
            }
        }
        System.out.println("[FrameProcessor] Process loop ended, processed total: " + processedCount);
        drainQueue();
    }

    public VideoPipelineStats getStats() {
        return stats;
    }

    private void logIfStalled() {
        long now = System.nanoTime();
        if (stats.shouldLogStall(now, STALL_THRESHOLD_NANOS, STALL_LOG_INTERVAL_NANOS)) {
            System.err.printf("[FrameProcessor] ⚠️ Pipeline stalled: %s%n", stats);
        }
    }

    private FrameRenderResult convertFrame(VideoFrame frame) {
        I420Buffer buffer = frame.buffer.toI420();
        try {
            return FrameRenderResult.fromI420(buffer, frame.timestampNs);
        } finally {
            buffer.release();
        }
    }

    private void drainQueue() {
        VideoFrame frame;
        while ((frame = queue.poll()) != null) {
            frame.release();
        }
    }

    @Override
    public void close() {
        running.set(false);
        workerThread.interrupt();
        drainQueue();
    }

    public void pause() {
        paused.set(true);
    }

    public void resume() {
        paused.set(false);
    }
}

