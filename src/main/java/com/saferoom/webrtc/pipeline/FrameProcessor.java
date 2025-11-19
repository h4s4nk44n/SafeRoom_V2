package com.saferoom.webrtc.pipeline;

import dev.onvoid.webrtc.media.video.I420Buffer;
import dev.onvoid.webrtc.media.video.VideoFrame;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Virtual-thread based frame processor. Each instance owns a dedicated virtual thread that
 * performs decode → convert steps off the JavaFX thread and pushes paint-ready frames to a consumer.
 */
public final class FrameProcessor implements AutoCloseable {

    private static final int DEFAULT_QUEUE_CAPACITY = 3;
    private static final Duration POLL_TIMEOUT = Duration.ofMillis(50);

    private final BlockingQueue<VideoFrame> queue;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicBoolean paused = new AtomicBoolean(false);
    private final Consumer<FrameRenderResult> consumer;
    private final Thread workerThread;

    public FrameProcessor(Consumer<FrameRenderResult> consumer) {
        this(consumer, DEFAULT_QUEUE_CAPACITY);
    }

    public FrameProcessor(Consumer<FrameRenderResult> consumer, int capacity) {
        this.consumer = Objects.requireNonNull(consumer, "consumer");
        this.queue = new ArrayBlockingQueue<>(Math.max(1, capacity));
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
        if (!queue.offer(frame)) {
            VideoFrame dropped = queue.poll();
            if (dropped != null) {
                dropped.release();
            }
            queue.offer(frame);
        }
    }

    private void processLoop() {
        while (running.get()) {
            try {
                VideoFrame frame = queue.poll(POLL_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
                if (frame == null) {
                    continue;
                }
                if (paused.get()) {
                    frame.release();
                    continue;
                }
                try {
                    FrameRenderResult result = convertFrame(frame);
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

    private FrameRenderResult convertFrame(VideoFrame frame) throws InterruptedException {
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            StructuredTaskScope.Subtask<FrameRenderResult> conversionTask = scope.fork(() -> {
                I420Buffer buffer = frame.buffer.toI420();
                try {
                    return FrameRenderResult.fromI420(buffer, frame.timestampNs);
                } finally {
                    buffer.release();
                }
            });
            try {
                scope.join();
                scope.throwIfFailed();
                return conversionTask.get();
            } catch (ExecutionException e) {
                throw new RuntimeException(e.getCause());
            }
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

