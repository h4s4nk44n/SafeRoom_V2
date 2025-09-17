package com.saferoom.voice_engine;

import java.util.Random;
import org.freedesktop.gstreamer.*;

public class UDPAudio {

  // Configuration
  private static String PEER_IP  = "192.168.1.29";
  private static int    PEER_PORT         = 7000;   // karşı tarafın dinlediği UDP portu
  private static int    MY_LISTENING_PORT = 7001;   // bu tarafın dinlediği UDP portu

  // Audio settings
  private static int DEFAULT_BITRATE   = 32000; // Opus
  private static int DEFAULT_FRAME_MS  = 20;    // 20ms frames

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

    try {
      System.out.println("Initializing GStreamer...");
      Gst.init("UDPAudio", new String[]{"--gst-debug-level=2"});
      Thread.sleep(1000); // Wait for initialization
      
      System.out.println("GStreamer initialized successfully");
    } catch (Exception e) {
      System.err.println("Failed to initialize GStreamer: " + e.getMessage());
      return;
    }

    Pipeline pipeline = null;
    try {
      // === CREATE PIPELINE ===
      pipeline = new Pipeline("udp-audio-duplex");
      
      // === TX: mic -> opus -> rtp -> udp ===
      System.out.println("Creating TX elements...");
      
      Element micSrc = createElementSafely("pulsesrc", "mic");
      if (micSrc == null) {
        micSrc = createElementSafely("audiotestsrc", "mic");
        if (micSrc != null) {
          micSrc.set("wave", 0);  // sine wave
          micSrc.set("freq", 440);
        }
      }
      
      Element audioConvert1 = createElementSafely("audioconvert", "aconv1");
      Element audioResample1 = createElementSafely("audioresample", "ares1");
      Element opusEnc = createElementSafely("opusenc", "opusenc");
      Element rtpOpusPay = createElementSafely("rtpopuspay", "rtpopuspay");
      Element udpSink = createElementSafely("udpsink", "udpsink");
      
      if (micSrc == null || audioConvert1 == null || audioResample1 == null || 
          opusEnc == null || rtpOpusPay == null || udpSink == null) {
        System.err.println("Failed to create TX elements");
        return;
      }
      
      // Configure TX elements
      try {
        opusEnc.set("bitrate", DEFAULT_BITRATE);
        opusEnc.set("frame-size", DEFAULT_FRAME_MS);
        opusEnc.set("inband-fec", true);
        opusEnc.set("dtx", false);
        
        rtpOpusPay.set("pt", 96);
        rtpOpusPay.set("ssrc", new Random().nextInt() & 0x7fffffff);
        
        udpSink.set("host", PEER_IP);
        udpSink.set("port", PEER_PORT);
        udpSink.set("sync", false);
        
        System.out.println("TX elements configured");
      } catch (Exception e) {
        System.err.println("Failed to configure TX elements: " + e.getMessage());
        return;
      }
      
      // === RX: udp -> rtp -> opus -> speaker ===
      System.out.println("Creating RX elements...");
      
      Element udpSrc = createElementSafely("udpsrc", "udpsrc");
      Element rtpOpusDepay = createElementSafely("rtpopusdepay", "rtpopusdepay");
      Element opusDec = createElementSafely("opusdec", "opusdec");
      Element audioConvert2 = createElementSafely("audioconvert", "aconv2");
      Element audioResample2 = createElementSafely("audioresample", "ares2");
      Element audioSink = createElementSafely("pulsesink", "sink");
      
      if (audioSink == null) {
        audioSink = createElementSafely("fakesink", "sink");
        if (audioSink != null) {
          audioSink.set("dump", true);
        }
      }
      
      if (udpSrc == null || rtpOpusDepay == null || opusDec == null || 
          audioConvert2 == null || audioResample2 == null || audioSink == null) {
        System.err.println("Failed to create RX elements");
        return;
      }
      
      // Configure RX elements
      try {
        udpSrc.set("port", MY_LISTENING_PORT);
        udpSrc.set("caps", Caps.fromString("application/x-rtp,media=audio,clock-rate=48000,encoding-name=OPUS,payload=96"));
        
        opusDec.set("use-inband-fec", true);
        opusDec.set("plc", true);
        
        audioSink.set("sync", false);
        
        System.out.println("RX elements configured");
      } catch (Exception e) {
        System.err.println("Failed to configure RX elements: " + e.getMessage());
        return;
      }
      
      // === ADD TO PIPELINE ===
      pipeline.addMany(
        // TX
        micSrc, audioConvert1, audioResample1, opusEnc, rtpOpusPay, udpSink,
        // RX  
        udpSrc, rtpOpusDepay, opusDec, audioConvert2, audioResample2, audioSink
      );
      
      // === LINK ELEMENTS ===
      try {
        if (!Element.linkMany(micSrc, audioConvert1, audioResample1, opusEnc, rtpOpusPay, udpSink)) {
          System.err.println("Failed to link TX elements");
          return;
        }
        
        if (!Element.linkMany(udpSrc, rtpOpusDepay, opusDec, audioConvert2, audioResample2, audioSink)) {
          System.err.println("Failed to link RX elements");
          return;
        }
        
        System.out.println("Elements linked successfully");
      } catch (Exception e) {
        System.err.println("Error linking elements: " + e.getMessage());
        return;
      }
      
      // === BUS HANDLING ===
      Bus bus = pipeline.getBus();
      bus.connect((Bus.ERROR) (source, code, message) -> {
        System.err.println("GST ERROR from " + source.getName() + ": " + message);
      });
      
      bus.connect((Bus.WARNING) (source, code, message) -> {
        System.out.println("GST WARNING from " + source.getName() + ": " + message);
      });
      
      // === START PIPELINE ===
      System.out.println("Starting UDP Audio pipeline...");
      System.out.println("Listening on UDP port: " + MY_LISTENING_PORT);
      System.out.println("Sending to: " + PEER_IP + ":" + PEER_PORT);
      
      final Pipeline finalPipeline = pipeline;
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        System.out.println("\nShutting down gracefully...");
        try {
          finalPipeline.setState(State.NULL);
        } catch (Exception e) {
          System.err.println("Error during shutdown: " + e.getMessage());
        }
      }));
      
      StateChangeReturn ret = pipeline.setState(State.PLAYING);
      if (ret == StateChangeReturn.FAILURE) {
        System.err.println("Failed to start pipeline");
        return;
      }
      
      System.out.println("UDP Audio started successfully");
      System.out.println("Press Ctrl+C to stop...");
      
      Gst.main();
      
    } catch (Exception e) {
      System.err.println("Error running pipeline: " + e.getMessage());
      e.printStackTrace();
    } finally {
      try {
        if (pipeline != null) {
          pipeline.setState(State.NULL);
        }
      } catch (Exception e) {
        System.err.println("Error stopping pipeline: " + e.getMessage());
      }
    }
  }
  
  private static Element createElementSafely(String factoryName, String elementName) {
    try {
      System.out.println("Creating element: " + factoryName + " as " + elementName);
      
      Element element = ElementFactory.make(factoryName, elementName);
      if (element == null) {
        System.err.println("Failed to create element: " + factoryName + " (" + elementName + ")");
        return null;
      }
      
      System.out.println("Successfully created: " + factoryName + " as " + elementName);
      return element;
    } catch (Exception e) {
      System.err.println("Exception creating element " + factoryName + ": " + e.getMessage());
      return null;
    }
  }
}
