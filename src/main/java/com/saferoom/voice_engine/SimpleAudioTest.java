package com.saferoom.voice_engine;

import org.freedesktop.gstreamer.*;

public class SimpleAudioTest {
  
  public static void main(String[] args) {
    try {
      System.out.println("Initializing GStreamer...");
      Gst.init("SimpleAudioTest", new String[]{"--gst-debug-level=2"});
      
      System.out.println("Creating simple audio pipeline...");
      
      // Simple test: audiotestsrc -> autoaudiosink
      Pipeline pipeline = new Pipeline("test");
      
      Element src = ElementFactory.make("audiotestsrc", "src");
      Element sink = ElementFactory.make("autoaudiosink", "sink");
      
      if (src == null || sink == null) {
        System.err.println("Failed to create elements");
        return;
      }
      
      src.set("freq", 440);
      src.set("wave", 0); // sine wave
      sink.set("sync", false);
      
      pipeline.addMany(src, sink);
      Element.linkMany(src, sink);
      
      // Bus error handling
      Bus bus = pipeline.getBus();
      bus.connect((Bus.ERROR) (source, code, message) -> {
        System.err.println("GST ERROR: " + message);
        Gst.quit();
      });
      
      System.out.println("Starting pipeline...");
      StateChangeReturn ret = pipeline.play();
      
      if (ret == StateChangeReturn.FAILURE) {
        System.err.println("Failed to start pipeline");
        return;
      }
      
      System.out.println("Playing 440Hz tone for 5 seconds...");
      
      // Play for 5 seconds
      new Thread(() -> {
        try {
          Thread.sleep(5000);
          System.out.println("Stopping...");
          pipeline.stop();
          Gst.quit();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }).start();
      
      Gst.main();
      
    } catch (Exception e) {
      System.err.println("Exception: " + e.getMessage());
      e.printStackTrace();
    }
  }
}
