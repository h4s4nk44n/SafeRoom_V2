package com.saferoom.voice_engine;

import org.freedesktop.gstreamer.Element;
import org.freedesktop.gstreamer.Pipeline;
import org.freedesktop.gstreamer.Structure;

public class Adaptive_Controller extends Thread {

  // EWMA ve eşikler
  private final double ALPHA   = 0.25;
  private final double HI_LOSS = 0.03;
  private final double LO_LOSS = 0.005;
  private final double HI_LATE = 0.02;
  private final double LO_LATE = 0.003;

  // sınırlar / adımlar
  private final int BR_MIN = 24000;
  private final int BR_MAX = 64000;
  private final int BR_STEP_UP   = 4000;
  private final int BR_STEP_DOWN = 6000;

  private final int FS_LOW  = 20;   // ms
  private final int FS_HIGH = 40;   // ms

  private final int JB_MIN = 40;    // ms
  private final int JB_MAX = 120;   // ms
  private final int JB_STEP_UP   = 20;
  private final int JB_STEP_DOWN = 10;

  // EWMA state
  private double ewmaLoss = 0.0;
  private double ewmaLate = 0.0;

  // sayaçlar
  private long prevPushed = 0, prevLost = 0, prevLate = 0;

  // throttle
  private long lastAdjustMs = 0;
  private final long COOLDOWN_MS = 2000;

  // elemanlar
  private final Element rtpJitterBuffer;
  private final Element opusEnc;
  private final Pipeline pipeline;

  private volatile boolean running = true;

  public Adaptive_Controller(Element rtpJitterBuffer, Element opusEnc, Pipeline pipeline) {
    this.rtpJitterBuffer = rtpJitterBuffer;
    this.opusEnc = opusEnc;
    this.pipeline = pipeline;
  }

  public void shutdown() {
    running = false;
    interrupt();
  }

  @Override
  public void run() {
    while (running && pipeline != null) {
      try { Thread.sleep(500); } catch (InterruptedException ie) { break; }

      if (rtpJitterBuffer == null || opusEnc == null) {
        System.err.println("Adaptive_Controller: Critical elements are null, stopping");
        break;
      }

      Structure stats = null;
      try {
        stats = (Structure) rtpJitterBuffer.get("stats");
      } catch (Exception e) {
        System.err.println("Adaptive_Controller: Error getting stats: " + e.getMessage());
        continue;
      }
      if (stats == null) continue;

      long pushed = getLong(stats, "num-pushed");
      long lost   = getLong(stats, "num-lost");
      long late   = getLong(stats, "num-late");

      long dPushed = Math.max(0, pushed - prevPushed);
      long dLost   = Math.max(0, lost   - prevLost);
      long dLate   = Math.max(0, late   - prevLate);

      double instLoss = (dPushed + dLost) > 0 ? (double)dLost / (dPushed + dLost) : 0.0;
      double instLate = dPushed > 0 ? (double)dLate / dPushed : 0.0;

      ewmaLoss = ALPHA * instLoss + (1.0 - ALPHA) * ewmaLoss;
      ewmaLate = ALPHA * instLate + (1.0 - ALPHA) * ewmaLate;

      long now = System.currentTimeMillis();
      if (now - lastAdjustMs < COOLDOWN_MS) {
        prevPushed = pushed; prevLost = lost; prevLate = late;
        continue;
      }

      int curBr = 0, fs = 0, jb = 0;
      try {
        curBr = getInt(opusEnc, "bitrate");
        fs    = getInt(opusEnc, "frame-size");
        jb    = getInt(rtpJitterBuffer, "latency");
      } catch (Exception e) {
        System.err.println("Adaptive_Controller: Error getting element properties: " + e.getMessage());
        continue;
      }

      boolean adjusted = false;

      // kötüleşme
      if (ewmaLoss > HI_LOSS || ewmaLate > HI_LATE) {
        try {
          int newBr = Math.max(BR_MIN, curBr - BR_STEP_DOWN);
          if (newBr != curBr) { opusEnc.set("bitrate", newBr); adjusted = true; }

          if (jb < JB_MAX) {
            rtpJitterBuffer.set("latency", Math.min(JB_MAX, jb + JB_STEP_UP));
            adjusted = true;
          }

          if (fs < FS_HIGH) { opusEnc.set("frame-size", FS_HIGH); adjusted = true; }
          opusEnc.set("inband-fec", true);
          opusEnc.set("dtx", true);
        } catch (Exception e) {
          System.err.println("Adaptive_Controller: Error adjusting for degradation: " + e.getMessage());
        }

      // iyileşme
      } else if (ewmaLoss < LO_LOSS && ewmaLate < LO_LATE) {
        try {
          if (jb > JB_MIN) {
            rtpJitterBuffer.set("latency", Math.max(JB_MIN, jb - JB_STEP_DOWN));
            adjusted = true;
          }
          if (fs > FS_LOW) { opusEnc.set("frame-size", FS_LOW); adjusted = true; }

          int newBr = Math.min(BR_MAX, curBr + BR_STEP_UP);
          if (newBr != curBr) { opusEnc.set("bitrate", newBr); adjusted = true; }
        } catch (Exception e) {
          System.err.println("Adaptive_Controller: Error adjusting for improvement: " + e.getMessage());
        }
      }

      if (adjusted) lastAdjustMs = now;
      prevPushed = pushed; prevLost = lost; prevLate = late;
    }
  }

  private static int getInt(Element e, String prop) {
    if (e == null || prop == null) return 0;
    try {
      Object v = e.get(prop);
      return (v instanceof Integer) ? (Integer) v : 0;
    } catch (Exception ex) {
      System.err.println("Adaptive_Controller: Error getting property '" + prop + "': " + ex.getMessage());
      return 0;
    }
  }
  private static long getLong(Structure s, String field) {
    if (s == null || field == null) return 0L;
    try { 
      Integer i = s.getInteger(field); 
      if (i != null) return i.longValue(); 
    } catch (Exception ignore) {}
    try { 
      Object obj = s.getValue(field);
      if (obj instanceof Long) return (Long) obj;
      if (obj instanceof Integer) return ((Integer) obj).longValue();
    } catch (Exception ignore) {}
    return 0L;
  }
}
