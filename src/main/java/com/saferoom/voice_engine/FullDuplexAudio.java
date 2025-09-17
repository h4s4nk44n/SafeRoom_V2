package com.saferoom.voice_engine;

import java.util.Random;
import org.freedesktop.gstreamer.*;

public class FullDuplexAudio {

  // sadece bunları doldur (CLI'dan da alabiliyorum)
  private static String PEER_IP  = "192.168.1.29";
  private static int    PEER_PORT         = 7001;   // karşı tarafın dinlediği SRT portu
  private static int    MY_LISTENING_PORT = 7000;  // bu tarafın dinlediği SRT portu

  // makul varsayılanlar
  private static int DEFAULT_BITRATE   = 32000; // Opus - lower for stability
  private static int DEFAULT_FRAME_MS  = 20;    // 10/20/40
  private static int DEFAULT_LATENCYMS = 100;   // SRT + jitterbuffer - increased for stability

  public static void main(String[] args) {
    // args: [peer_ip] [peer_port] [my_listen_port]
    if (args != null && args.length >= 3) {
      try {
        PEER_IP           = args[0];
        PEER_PORT         = Integer.parseInt(args[1]);
        MY_LISTENING_PORT = Integer.parseInt(args[2]);
      } catch (NumberFormatException e) {
        System.err.println("Invalid port numbers: " + e.getMessage());
        return;
      }
    }

    // Critical: Initialize GStreamer with proper error handling
    try {
      System.setProperty("jna.library.path", "/usr/lib/x86_64-linux-gnu/gstreamer-1.0");
      Gst.init("FullDuplex", new String[]{"--gst-debug-level=2"});
      
      // Wait for GStreamer to fully initialize
      Thread.sleep(1000);
      
      System.out.println("GStreamer initialized successfully");
    } catch (Exception e) {
      System.err.println("Failed to initialize GStreamer: " + e.getMessage());
      e.printStackTrace();
      return;
    }

    // === TX: mic -> opus -> rtp -> srt (caller) ===
    System.out.println("Creating TX elements...");
    
    Element mic   = createElementSafely("pulsesrc", "mic");  // More stable than autoaudiosrc
    if (mic == null) {
      mic = createElementSafely("alsasrc", "mic");  // Fallback
    }
    if (mic == null) {
      mic = createElementSafely("audiotestsrc", "mic");  // Test source as last resort
      if (mic != null) {
        try {
          mic.set("wave", 0);  // Sine wave
          mic.set("freq", 440); // 440Hz tone
        } catch (Exception e) {
          System.err.println("Failed to configure test source: " + e.getMessage());
        }
      }
    }
    
    Element aconv = createElementSafely("audioconvert", "aconv");
    Element ares  = createElementSafely("audioresample", "ares");
    Element enc   = createElementSafely("opusenc", "opusenc");
    
    if (mic == null || aconv == null || ares == null || enc == null) {
      System.err.println("Failed to create TX audio elements");
      return;
    }
    
    System.out.println("TX elements created successfully");

    try {
      // Conservative Opus settings to prevent crashes
      enc.set("bitrate", DEFAULT_BITRATE);
      enc.set("inband-fec", true);
      enc.set("dtx", false);  // Disable DTX initially for stability
      enc.set("frame-size", DEFAULT_FRAME_MS);
      enc.set("complexity", 5);  // Medium complexity
      enc.set("packet-loss-percentage", 0);
      
      System.out.println("Opus encoder configured");
    } catch (Exception e) {
      System.err.println("Failed to configure opus encoder: " + e.getMessage());
      return;
    }

    Element pay   = createElementSafely("rtpopuspay", "rtpopuspay");
    if (pay == null) {
      System.err.println("Failed to create RTP payloader");
      return;
    }
    
    // SSRC rastgele; çakışma riskini azalt
    int mySsrc = new Random().nextInt() & 0x7fffffff;
    Element qtx   = createElementSafely("queue", "qtx");
    Element srtOut = createElementSafely("srtclientsink", "srtclientsink");
    
    if (qtx == null || srtOut == null) {
      System.err.println("Failed to create TX pipeline elements");
      return;
    }
    
    try {
      pay.set("pt", 96);
      pay.set("ssrc", mySsrc);  // rtpopuspay supports ssrc property
      pay.set("timestamp-offset", 0);
      pay.set("seqnum-offset", 0);
      
      // SRT caller configuration with better parameters
      String srtUri = "srt://" + PEER_IP + ":" + PEER_PORT + 
                     "?mode=caller&latency=" + DEFAULT_LATENCYMS + 
                     "&connect_timeout=5000&rcvbuf=1000000&sndbuf=1000000";
      srtOut.set("uri", srtUri);
      srtOut.set("wait-for-connection", false);
    } catch (Exception e) {
      System.err.println("Failed to configure TX elements: " + e.getMessage());
      return;
    }

    // === RX: srt (listener) -> rtp -> jitterbuffer -> opus -> hoparlör ===
    System.out.println("Creating RX elements...");
    
    Element srtIn  = createElementSafely("srtserversrc", "srtserversrc");
    if (srtIn == null) {
      System.err.println("Failed to create SRT server source");
      return;
    }
    
    try {
      // SRT listener configuration with better parameters
      String srtListenUri = "srt://0.0.0.0:" + MY_LISTENING_PORT + 
                           "?mode=listener&latency=" + DEFAULT_LATENCYMS + 
                           "&rcvbuf=1000000&sndbuf=1000000";
      srtIn.set("uri", srtListenUri);
    } catch (Exception e) {
      System.err.println("Failed to configure SRT server: " + e.getMessage());
      return;
    }

    Element depayStream = createElementSafely("rtpstreamdepay", "rtpstreamdepay");
    if (depayStream == null) {
      System.err.println("Failed to create RTP stream depayloader");
      return;
    }

    // More flexible RTP caps to handle different SSRC values
    Caps caps = null;
    try {
      caps = Caps.fromString(
        "application/x-rtp,media=(string)audio,clock-rate=(int)48000,encoding-name=(string)OPUS,payload=(int)96"
      );
    } catch (Exception e) {
      System.err.println("Failed to create caps: " + e.getMessage());
      return;
    }
    
    Element capsFilter = createElementSafely("capsfilter", "caps");
    if (capsFilter == null) {
      System.err.println("Failed to create caps filter");
      return;
    }
    
    try {
      capsFilter.set("caps", caps);
    } catch (Exception e) {
      System.err.println("Failed to set caps: " + e.getMessage());
      return;
    }

    Element jitter = createElementSafely("rtpjitterbuffer", "rtpjitter");
    Element depay = createElementSafely("rtpopusdepay", "rtpopusdepay");
    Element dec   = createElementSafely("opusdec", "opusdec");
    Element arx1  = createElementSafely("audioconvert", "arx1");
    Element arx2  = createElementSafely("audioresample", "arx2");
    Element qrx   = createElementSafely("queue", "qrx");
    
    // Try different audio sinks for better compatibility
    Element out = createElementSafely("pulsesink", "sink");
    if (out == null) {
      out = createElementSafely("alsasink", "sink");
    }
    if (out == null) {
      out = createElementSafely("fakesink", "sink");  // Last resort - no audio output
      if (out != null) {
        try {
          out.set("dump", true);
        } catch (Exception e) {
          System.err.println("Failed to configure fake sink: " + e.getMessage());
        }
      }
    }
    
    if (jitter == null || depay == null || dec == null || arx1 == null || arx2 == null || qrx == null || out == null) {
      System.err.println("Failed to create RX audio elements");
      return;
    }
    
    System.out.println("RX elements created successfully");
    
    try {
      // Conservative jitter buffer settings
      jitter.set("latency", DEFAULT_LATENCYMS);
      jitter.set("do-lost", false);  // Disable for stability
      jitter.set("drop-on-latency", false);  // Disable for stability
      
      // Conservative decoder settings
      dec.set("use-inband-fec", false);  // Disable initially
      dec.set("plc", false);  // Disable initially
      
      // Safe audio sink settings
      out.set("sync", false);
      if (!out.getName().equals("fakesink")) {
        try {
          out.set("async", false);
        } catch (Exception ignore) {
          // Some sinks don't have async property
        }
      }
      
      System.out.println("RX elements configured");
    } catch (Exception e) {
      System.err.println("Failed to configure RX elements: " + e.getMessage());
      return;
    }

    // === PIPELINE ===
    System.out.println("Creating pipeline...");
    Pipeline p = null;
    try {
      p = new Pipeline("opus-over-srt-full-duplex");
      
      // Add elements safely
      p.addMany(
        // TX
        mic, aconv, ares, enc, pay, qtx, srtOut,
        // RX
        srtIn, depayStream, capsFilter, jitter, depay, dec, arx1, arx2, qrx, out
      );
      
      System.out.println("Pipeline created and elements added");
    } catch (Exception e) {
      System.err.println("Failed to create pipeline: " + e.getMessage());
      return;
    }

    // linkler
    try {
      if (!Element.linkMany(mic, aconv, ares, enc, pay, qtx, srtOut)) {
        System.err.println("Failed to link TX elements");
        return;
      }
      if (!Element.linkMany(srtIn, depayStream, capsFilter, jitter, depay, dec, arx1, arx2, qrx, out)) {
        System.err.println("Failed to link RX elements");
        return;
      }
    } catch (Exception e) {
      System.err.println("Error linking elements: " + e.getMessage());
      return;
    }

    // Enhanced bus logging with better error handling
    Bus bus = p.getBus();
    bus.connect((Bus.ERROR) (source, code, message) -> {
      System.err.println("GST ERROR from " + source.getName() + ": " + message);
      // Don't stop on stream format errors - try to recover
      if (!message.contains("wrong format") && !message.contains("Internal data stream error")) {
        System.err.println("Critical error, stopping pipeline");
      }
    });
    
    bus.connect((Bus.WARNING) (source, code, message) ->
      System.out.println("GST WARNING from " + source.getName() + ": " + message)
    );
    
    bus.connect((Bus.INFO) (source, code, message) ->
      System.out.println("GST INFO from " + source.getName() + ": " + message)
    );

    // adaptif kontrol
    Adaptive_Controller controller = new Adaptive_Controller(jitter, enc, p);
    controller.start();

    // çalıştır with safe state management
    try {
      System.out.println("Starting FullDuplexAudio pipeline...");
      System.out.println("Listening on port: " + MY_LISTENING_PORT);
      System.out.println("Connecting to: " + PEER_IP + ":" + PEER_PORT);
      
      // Safe state transitions
      System.out.println("Setting pipeline to READY...");
      StateChangeReturn ret = p.setState(State.READY);
      if (ret == StateChangeReturn.FAILURE) {
        System.err.println("Failed to set pipeline to READY");
        controller.shutdown();
        return;
      }
      
      // Wait for state change
      Thread.sleep(500);
      
      System.out.println("Setting pipeline to PLAYING...");
      ret = p.setState(State.PLAYING);
      if (ret == StateChangeReturn.FAILURE) {
        System.err.println("Failed to start pipeline");
        controller.shutdown();
        p.setState(State.NULL);
        return;
      }
      
      System.out.println("FullDuplexAudio started successfully");
      System.out.println("Press Ctrl+C to stop...");
      
      // Add shutdown hook for clean exit
      final Pipeline finalPipeline = p;
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        System.out.println("\nShutting down gracefully...");
        try {
          controller.shutdown();
          finalPipeline.setState(State.PAUSED);
          Thread.sleep(100);
          finalPipeline.setState(State.NULL);
        } catch (Exception e) {
          System.err.println("Error during shutdown: " + e.getMessage());
        }
      }));
      
      Gst.main();
    } catch (Exception e) {
      System.err.println("Error running pipeline: " + e.getMessage());
      e.printStackTrace();
    } finally {
      // durdur
      try {
        if (controller != null) {
          controller.shutdown();
        }
        if (p != null) {
          p.setState(State.PAUSED);
          Thread.sleep(100);
          p.setState(State.NULL);
        }
      } catch (Exception e) {
        System.err.println("Error stopping pipeline: " + e.getMessage());
      }
    }
  }
  
  private static Element createElementSafely(String factoryName, String elementName) {
    try {
      System.out.println("Creating element: " + factoryName + " as " + elementName);
      
      // Check if factory exists first
      ElementFactory factory = ElementFactory.find(factoryName);
      if (factory == null) {
        System.err.println("Factory not found: " + factoryName);
        return null;
      }
      
      Element element = ElementFactory.make(factoryName, elementName);
      if (element == null) {
        System.err.println("Failed to create element: " + factoryName + " (" + elementName + ")");
        return null;
      }
      
      System.out.println("Successfully created: " + factoryName + " as " + elementName);
      return element;
    } catch (Exception e) {
      System.err.println("Exception creating element " + factoryName + ": " + e.getMessage());
      e.printStackTrace();
      return null;
    }
  }
}
