package com.saferoom.webrtc.pipeline;

import java.nio.ByteBuffer;

/**
 * JNI-based Video Encoder Implementation.
 * 
 * <h2>Zero-Copy Architecture</h2>
 * <p>
 * This class serves as the bridge to the native C++ encoding layer.
 * By passing the {@link java.nio.DirectByteBuffer} directly to native code,
 * we avoid all JVM-heap copies. The native code can read the YUV/ARGB data
 * directly from the native memory address of the buffer.
 * </p>
 * 
 * <h2>Usage</h2>
 * 
 * <pre>
 * NativeVideoEncoder encoder = new NativeVideoEncoder();
 * encoder.init(640, 480, 30);
 * encoder.encodeFrame(directBuffer, timestamp);
 * encoder.release();
 * </pre>
 */
public class NativeVideoEncoder {

    /**
     * Encode a frame data directly from a ByteBuffer.
     * 
     * @param buffer DirectByteBuffer containing frame data (YUV or ARGB).
     *               MUST be allocated with ByteBuffer.allocateDirect().
     * @param length Length of data to encode.
     * @param width  Frame width.
     * @param height Frame height.
     * @return Encoded size or error code.
     */
    public native int encodeFrame(ByteBuffer buffer, int length, int width, int height);

    // Example C++ implementation provided in documentation
    /*
     * JNIEXPORT jint JNICALL
     * Java_com_saferoom_webrtc_pipeline_NativeVideoEncoder_encodeFrame
     * (JNIEnv *env, jobject thisObj, jobject buffer, jint length, jint width, jint
     * height) {
     * 
     * // 1. Get direct access to memory (Zero Copy, No GC Pause)
     * void* data = env->GetDirectBufferAddress(buffer);
     * 
     * if (data == NULL) {
     * return -1; // Not a direct buffer
     * }
     * 
     * // 2. Pass to encoder (e.g. FFmpeg / x264)
     * // encoder_encode(ctx, (uint8_t*)data, width, height, ...);
     * 
     * return 0;
     * }
     */
}
