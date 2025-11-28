package com.saferoom.webrtc.pipeline;

import dev.onvoid.webrtc.media.video.I420Buffer;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Immutable container for a single video frame ready for painting on the FX thread.
 */
public final class FrameRenderResult {

    private static final ArgbBufferPool BUFFER_POOL = new ArgbBufferPool();

    private final int width;
    private final int height;
    private final int[] argbPixels;
    private final long timestampNs;
    private final AtomicBoolean released = new AtomicBoolean(false);

    private FrameRenderResult(int width, int height, int[] argbPixels, long timestampNs) {
        this.width = width;
        this.height = height;
        this.argbPixels = argbPixels;
        this.timestampNs = timestampNs;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int[] getArgbPixels() {
        return argbPixels;
    }

    public long getTimestampNs() {
        return timestampNs;
    }

    public static FrameRenderResult fromI420(I420Buffer buffer, long timestampNs) {
        int width = buffer.getWidth();
        int height = buffer.getHeight();
        int[] argb = BUFFER_POOL.acquire(width, height);

        ByteBuffer yPlane = buffer.getDataY();
        ByteBuffer uPlane = buffer.getDataU();
        ByteBuffer vPlane = buffer.getDataV();

        int yStride = buffer.getStrideY();
        int uStride = buffer.getStrideU();
        int vStride = buffer.getStrideV();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int yIndex = y * yStride + x;
                int uvIndex = (y / 2) * uStride + (x / 2);
                int vIndex = (y / 2) * vStride + (x / 2);

                int Y = (yPlane.get(yIndex) & 0xFF) - 16;
                int U = (uPlane.get(uvIndex) & 0xFF) - 128;
                int V = (vPlane.get(vIndex) & 0xFF) - 128;

                int R = (298 * Y + 409 * V + 128) >> 8;
                int G = (298 * Y - 100 * U - 208 * V + 128) >> 8;
                int B = (298 * Y + 516 * U + 128) >> 8;

                R = clamp(R);
                G = clamp(G);
                B = clamp(B);

                argb[y * width + x] = 0xFF000000 | (R << 16) | (G << 8) | B;
            }
        }

        return new FrameRenderResult(width, height, argb, timestampNs);
    }

    private static int clamp(int value) {
        if (value < 0) return 0;
        if (value > 255) return 255;
        return value;
    }

    /**
     * Return the underlying pixel buffer to the pool once the frame has been painted.
     */
    public void release() {
        if (released.compareAndSet(false, true)) {
            BUFFER_POOL.release(width, height, argbPixels);
        }
    }
}

