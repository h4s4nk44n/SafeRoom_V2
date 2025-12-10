package com.saferoom.webrtc.pipeline;

import dev.onvoid.webrtc.media.video.I420Buffer;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Immutable container for a single video frame ready for painting on the FX
 * thread.
 * 
 * <h2>Optimizasyon Notları (v2.0)</h2>
 * <ul>
 * <li><b>Buffer Pooling:</b> Uses DirectByteBuffer from ArgbBufferPool to avoid
 * heap allocation.</li>
 * <li>Row-based bulk ByteBuffer read.</li>
 * <li>Pre-computed UV indices.</li>
 * </ul>
 */
public final class FrameRenderResult {

    private static final ArgbBufferPool BUFFER_POOL = new ArgbBufferPool();

    // ═══════════════════════════════════════════════════════════════════════════════
    // THREAD-LOCAL ROW BUFFERS
    // Pre-allocated for 1080p (1920x1080), reused across frames
    // Avoids per-frame allocation and reduces GC pressure
    // ═══════════════════════════════════════════════════════════════════════════════
    private static final int MAX_WIDTH = 1920;
    private static final int MAX_UV_WIDTH = (MAX_WIDTH + 1) / 2;

    private static final ThreadLocal<byte[]> TL_Y_ROW = ThreadLocal.withInitial(() -> new byte[MAX_WIDTH]);

    // Cached UV rows for 4:2:0 subsampling (reuse across 2 Y rows)
    private static final ThreadLocal<byte[]> TL_U_CACHED = ThreadLocal.withInitial(() -> new byte[MAX_UV_WIDTH]);
    private static final ThreadLocal<byte[]> TL_V_CACHED = ThreadLocal.withInitial(() -> new byte[MAX_UV_WIDTH]);

    private final int width;
    private final int height;
    // Main pixel storage - DirectByteBuffer
    private final ByteBuffer buffer;
    private final long timestampNs;
    private final AtomicBoolean released = new AtomicBoolean(false);

    private FrameRenderResult(int width, int height, ByteBuffer buffer, long timestampNs) {
        this.width = width;
        this.height = height;
        this.buffer = buffer;
        this.timestampNs = timestampNs;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

    public long getTimestampNs() {
        return timestampNs;
    }

    /**
     * Convert I420 (YUV 4:2:0) buffer to ARGB format in a DirectByteBuffer.
     * 
     * @param buffer      I420 video frame buffer from WebRTC
     * @param timestampNs Frame timestamp in nanoseconds
     * @return Paint-ready FrameRenderResult
     */
    public static FrameRenderResult fromI420(I420Buffer buffer, long timestampNs) {
        int width = buffer.getWidth();
        int height = buffer.getHeight();

        // Validate dimensions
        if (width > MAX_WIDTH || height > MAX_WIDTH) {
            return fromI420Legacy(buffer, timestampNs);
        }

        // Acquire DirectByteBuffer (pooled)
        ByteBuffer argbByteBuffer = BUFFER_POOL.acquire(width, height);
        // Create an IntBuffer view for convenient int-based writing
        IntBuffer argbIntBuffer = argbByteBuffer.asIntBuffer();

        ByteBuffer yPlane = buffer.getDataY();
        ByteBuffer uPlane = buffer.getDataU();
        ByteBuffer vPlane = buffer.getDataV();

        int yStride = buffer.getStrideY();
        int uStride = buffer.getStrideU();
        int vStride = buffer.getStrideV();

        // Get thread-local row buffers
        byte[] yRow = TL_Y_ROW.get();
        byte[] uRow = TL_U_CACHED.get();
        byte[] vRow = TL_V_CACHED.get();

        int halfWidth = (width + 1) / 2;

        for (int y = 0; y < height; y++) {
            // ✅ BULK READ: Single JNI call for entire Y row
            yPlane.position(y * yStride);
            yPlane.get(yRow, 0, width);

            // ✅ UV CACHING: Read UV rows only on even Y lines (4:2:0 subsampling)
            if ((y & 1) == 0) {
                int uvY = y / 2;
                uPlane.position(uvY * uStride);
                uPlane.get(uRow, 0, halfWidth);
                vPlane.position(uvY * vStride);
                vPlane.get(vRow, 0, halfWidth);
            }

            int rowOffset = y * width;

            for (int x = 0; x < width; x++) {
                int uvX = x >> 1; // Faster than x / 2

                int Y = (yRow[x] & 0xFF) - 16;
                int U = (uRow[uvX] & 0xFF) - 128;
                int V = (vRow[uvX] & 0xFF) - 128;

                int R = Math.max(0, Math.min(255, (298 * Y + 409 * V + 128) >> 8));
                int G = Math.max(0, Math.min(255, (298 * Y - 100 * U - 208 * V + 128) >> 8));
                int B = Math.max(0, Math.min(255, (298 * Y + 516 * U + 128) >> 8));

                // 0xFF R G B (for IntArgb)
                int argb = 0xFF000000 | (R << 16) | (G << 8) | B;

                // Write to IntBuffer (this updates the underlying ByteBuffer)
                argbIntBuffer.put(rowOffset + x, argb);
            }
        }

        // Reset buffer positions for reuse in next iteration if they were pooled by
        // caller
        yPlane.rewind();
        uPlane.rewind();
        vPlane.rewind();

        // Rewind our new buffer before returning
        argbByteBuffer.rewind();

        return new FrameRenderResult(width, height, argbByteBuffer, timestampNs);
    }

    /**
     * Legacy per-pixel conversion method.
     */
    private static FrameRenderResult fromI420Legacy(I420Buffer buffer, long timestampNs) {
        int width = buffer.getWidth();
        int height = buffer.getHeight();
        ByteBuffer argbByteBuffer = BUFFER_POOL.acquire(width, height);
        IntBuffer argbIntBuffer = argbByteBuffer.asIntBuffer();

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

                int R = Math.max(0, Math.min(255, (298 * Y + 409 * V + 128) >> 8));
                int G = Math.max(0, Math.min(255, (298 * Y - 100 * U - 208 * V + 128) >> 8));
                int B = Math.max(0, Math.min(255, (298 * Y + 516 * U + 128) >> 8));

                int argb = 0xFF000000 | (R << 16) | (G << 8) | B;
                argbIntBuffer.put(y * width + x, argb);
            }
        }

        argbByteBuffer.rewind();
        return new FrameRenderResult(width, height, argbByteBuffer, timestampNs);
    }

    /**
     * Return the underlying pixel buffer to the pool once the frame has been
     * painted.
     */
    public void release() {
        if (released.compareAndSet(false, true)) {
            BUFFER_POOL.release(width, height, buffer);
        }
    }
}
