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
        this.workerThread = Thread.ofVirtual()
            .name("frame-processor-" + System.identityHashCode(this))
            .unstarted(this::processLoop);
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

    private void processLoop() {
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
                    stats.recordProcessed(System.nanoTime() - start, queue.size());
                    consumer.accept(result);
                } finally {
                    frame.release();
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
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

