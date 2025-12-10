# SafeRoomV2 Architecture Map & Branch Ownership Guide

**Document Version:** 1.0  
**Date:** December 7, 2025  
**Author:** Architecture Team  

---

## Table of Contents

1. [System Overview](#system-overview)
2. [File-Level Architecture Analysis](#part-1--file-level-architecture-analysis)
3. [Branch Impact Map](#part-2--branch-impact-map)
4. [Developer Responsibility Matrix](#part-3--developer-responsibility-matrix)
5. [Conflict Probability Map](#part-4--conflict-probability-map)
6. [Merge Strategy Recommendations](#part-5--merge-strategy-recommendations)

---

## System Overview

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           SafeRoomV2 Architecture                               │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│   ┌─────────────────┐   ┌─────────────────┐   ┌─────────────────┐              │
│   │   JavaFX GUI    │   │    WebRTC       │   │  P2P Messaging  │              │
│   │   Controllers   │◄──┤    Engine       │◄──┤   DataChannel   │              │
│   └────────┬────────┘   └────────┬────────┘   └────────┬────────┘              │
│            │                     │                     │                        │
│            ▼                     ▼                     ▼                        │
│   ┌─────────────────┐   ┌─────────────────┐   ┌─────────────────┐              │
│   │   VideoPanel    │   │   CallManager   │   │  FileTransfer   │              │
│   │   Components    │   │   State Machine │   │    Protocol     │              │
│   └────────┬────────┘   └────────┬────────┘   └────────┬────────┘              │
│            │                     │                     │                        │
│            └──────────────┬──────┴─────────────────────┘                        │
│                           ▼                                                     │
│   ┌─────────────────────────────────────────────────────────────────┐          │
│   │                  WebRTC Native Library (webrtc-java)            │          │
│   │   ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │          │
│   │   │ AudioDevice  │  │  PeerConn    │  │ VideoDevice  │          │          │
│   │   │   Module     │  │  Factory     │  │   Source     │          │          │
│   │   └──────────────┘  └──────────────┘  └──────────────┘          │          │
│   └─────────────────────────────────────────────────────────────────┘          │
│                           │                                                     │
│   ┌───────────────────────┼─────────────────────────────────────────┐          │
│   │                       ▼           Platform Layer                │          │
│   │   ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │          │
│   │   │   Windows    │  │    Linux     │  │    macOS     │          │          │
│   │   │  COM/STA     │  │ PulseAudio   │  │  CoreAudio   │          │          │
│   │   │  Threading   │  │    ALSA      │  │ VideoToolbox │          │          │
│   │   └──────────────┘  └──────────────┘  └──────────────┘          │          │
│   └─────────────────────────────────────────────────────────────────┘          │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

## PART 1 — FILE-LEVEL ARCHITECTURE ANALYSIS

### webrtc/ — Core WebRTC Engine

#### WebRTCClient.java
- **Purpose:** Core WebRTC client manager. Handles PeerConnectionFactory, AudioDeviceModule, ICE/SDP negotiation, and media track lifecycle.
- **Related modules:** CallManager, P2PConnectionManager, VideoPanel, CameraCaptureService
- **Critical code regions:**
  - Lines 75-120: `initialize()` — Static singleton initialization, platform detection
  - Lines 141-208: `initWindowsAudio()` — Windows COM STA thread handling (CRITICAL)
  - Lines 416-567: `createPeerConnection()` — ICE server config, PeerConnectionObserver
  - Lines 572-683: `createOffer()`/`createAnswer()` — SDP creation pipeline
  - Lines 849-901: `addAudioTrack()` — Audio processing options
  - Lines 907-958: `addVideoTrack()` — Camera capture integration
  - Lines 1012-1060: `handleRemoteAudioTrack()` — Per-track audio sink management
  - Lines 1134-1182: `handleRemoteVideoTrack()` — Debug sink for frame flow diagnostics
- **Risk level for conflicts:**  **CRITICAL** — Most branches will touch this file
- **Threading concerns:** 
  - Windows requires dedicated STA thread for COM
  - `playoutStarted`/`recordingStarted` are volatile flags with synchronized blocks
  - Virtual Thread executor for async WebRTC ops

#### CallManager.java
- **Purpose:** Call state machine orchestrator. Manages caller/callee roles, SDP/ICE signal routing, and call lifecycle (IDLE→RINGING→CONNECTING→CONNECTED→ENDED).
- **Related modules:** WebRTCClient, WebRTCSignalingClient, ActiveCallDialog, IncomingCallDialog
- **Critical code regions:**
  - Lines 140-187: `startCall()` — Outgoing call initiation, track addition order
  - Lines 218-259: `acceptCall()` — CRITICAL: Track addition deferred until OFFER received
  - Lines 534-575: `handleOffer()` — CRITICAL: setRemoteDescription BEFORE addTracks
  - Lines 662-708: `setupWebRTCCallbacks()` — Callback wiring for ICE/SDP
  - Lines 766-802: `cleanup()` — Resource teardown, recursion prevention
- **Risk level for conflicts:**  **CRITICAL** — Core call flow logic
- **State machine concerns:**
  - `tracksAddedForIncomingCall` flag prevents duplicate track addition
  - `pendingAudioEnabled`/`pendingVideoEnabled` store media settings for deferred use
  - Order-sensitive: setRemoteDescription → addTracks → createAnswer

#### WebRTCPlatformConfig.java
- **Purpose:** Platform-specific codec preferences. Detects OS and reorders video codecs (VP8/H264/VP9) for optimal cross-platform compatibility.
- **Related modules:** WebRTCClient, CameraCaptureService
- **Critical code regions:**
  - Lines 45-83: `detect()` — Platform detection and codec ordering
  - Lines 98-134: `applyVideoCodecPreferences()` — Transceiver codec configuration
  - Lines 140-179: `reorderCodecsKeepAll()` — Codec sorting algorithm
- **Risk level for conflicts:**  **MEDIUM** — Platform optimization changes
- **Platform concerns:**
  - VP8 preferred for software encoding compatibility
  - H264 kept as fallback for hardware acceleration
  - Windows: Intel QuickSync/NVIDIA NVENC support
  - macOS: VideoToolbox hardware acceleration

#### CameraCaptureService.java
- **Purpose:** Singleton camera capture abstraction. Creates VideoDeviceSource with consistent resolution/FPS across DM and group calls.
- **Related modules:** WebRTCClient, GroupCallManager
- **Critical code regions:**
  - Lines 24-80: `createCameraTrack()` — Camera enumeration, source creation, track setup
- **Risk level for conflicts:**  **LOW** — Isolated service
- **Resource concerns:**
  - Camera resource is shared; must be stopped/disposed properly
  - Default profile: 640x480@30fps

#### WebRTCSignalingClient.java
- **Purpose:** gRPC signaling stream client. Handles CALL_REQUEST, OFFER, ANSWER, ICE_CANDIDATE, and other WebRTC signal types.
- **Related modules:** CallManager, P2PConnectionManager
- **Risk level for conflicts:**  **MEDIUM** — Signal routing changes

#### GroupCallManager.java
- **Purpose:** Multi-party mesh call orchestration. Manages multiple WebRTCClient instances for group calls.
- **Related modules:** WebRTCClient, WebRTCSignalingClient
- **Risk level for conflicts:**  **MEDIUM** — Group call feature development

#### WebRTCSessionManager.java
- **Purpose:** Server-side session management (if running embedded server).
- **Risk level for conflicts:**  **LOW** — Server-side logic

### webrtc/pipeline/ — Video Processing Pipeline

#### FrameProcessor.java
- **Purpose:** Virtual-thread based frame decoder. Converts VideoFrame (I420) to ARGB for JavaFX rendering off the FX thread.
- **Related modules:** VideoPanel, FrameRenderResult, ArgbBufferPool
- **Critical code regions:**
  - Lines 34-53: Constructor — Platform thread selection for native interop
  - Lines 55-70: `submit()` — Frame queue with backpressure (drops oldest)
  - Lines 77-120: `processLoop()` — Main processing loop with stall detection
  - Lines 133-139: `convertFrame()` — I420 → ARGB conversion
- **Risk level for conflicts:**  **HIGH** — CPU optimization focus area
- **Threading concerns:**
  - Uses platform thread (not virtual) due to native code compatibility
  - Queue capacity: 12 frames (configurable via system property)
  - Stall detection: 2s threshold, 5s log interval

#### FrameRenderResult.java
- **Purpose:** Immutable frame data holder for FX thread consumption. Contains ARGB pixel buffer from pooled memory.
- **Related modules:** FrameProcessor, ArgbBufferPool, VideoPanel
- **Risk level for conflicts:**  **MEDIUM** — Zero-copy optimization target

#### ArgbBufferPool.java
- **Purpose:** Recycling buffer pool for ARGB pixel arrays. Reduces GC pressure during video rendering.
- **Related modules:** FrameRenderResult, FrameProcessor
- **Risk level for conflicts:**  **MEDIUM** — Memory optimization target

#### VideoPipelineStats.java
- **Purpose:** Performance statistics collector for video pipeline diagnostics.
- **Risk level for conflicts:**  **LOW** — Logging/diagnostics only

### webrtc/screenshare/ — Screen Sharing

#### ScreenShareManager.java
- **Purpose:** Platform-aware screen share orchestrator. Linux uses FFmpeg engine; Windows/macOS use native ScreenCapturer.
- **Related modules:** ScreenShareController, LinuxScreenShareEngine, CallManager
- **Critical code regions:**
  - Lines 71-99: `startScreenShare()` — Platform routing
  - Lines 123-138: `startLinuxCapture()` — Custom FFmpeg pipeline
  - Lines 141-157: `startNativeCapture()` — Native capturer setup
  - Lines 159-195: `renegotiate()` — SDP renegotiation for track changes
  - Lines 197-240: `publishTrack()`/`detachTrack()` — Track management with replaceTrack
- **Risk level for conflicts:**  **MEDIUM** — Platform-specific capture logic
- **Platform concerns:**
  - Linux: Requires PipeWire/FFmpeg for desktop capture
  - Windows/macOS: Native ScreenCapturer from webrtc-java

#### ScreenShareController.java
- **Purpose:** High-level screen share API for GUI. Wraps ScreenShareManager with async operations.
- **Risk level for conflicts:**  **LOW** — API wrapper

#### ScreenSourceOption.java
- **Purpose:** Value object for screen/window source selection.
- **Risk level for conflicts:**  **LOW** — Data class

### gui/components/ — UI Components

#### VideoPanel.java
- **Purpose:** JavaFX Canvas-based video renderer. Receives frames from FrameProcessor, paints via AnimationTimer.
- **Related modules:** FrameProcessor, ActiveCallDialog, VideoTrack
- **Critical code regions:**
  - Lines 44-87: Constructor — AnimationTimer setup, stall detection
  - Lines 96-133: `attachVideoTrack()` — Sink attachment, processor creation
  - Lines 138-159: `detachVideoTrack()` — Cleanup with safety checks
  - Lines 161-198: `paintFrame()` — Aspect-ratio-aware rendering
  - Lines 283-320: `handleStall()` — Recovery logic for video freeze
- **Risk level for conflicts:**  **HIGH** — UI rendering + optimization target
- **Threading concerns:**
  - AnimationTimer runs on FX thread
  - Frame submission from WebRTC callback thread
  - `latestFrame` is AtomicReference for thread safety

### gui/dialog/ — Call Dialogs

#### ActiveCallDialog.java
- **Purpose:** Active call UI. Shows local/remote video, controls (mute, camera, screen share), and call duration.
- **Related modules:** VideoPanel, CallManager, ScreenShareController
- **Critical code regions:**
  - Lines 85-228: `createDialog()` — UI layout construction
  - Lines 287-344: `toggleMute()`/`toggleCamera()` — Media control handlers
  - Lines 372-431: `handleShareScreen()`/`startScreenSharing()` — Screen share workflow
  - Lines 544-579: `attachLocalVideo()`/`attachRemoteVideo()` — Track attachment
  - Lines 697-715: `registerRemoteTrackCallback()` — Auto-attach remote video
- **Risk level for conflicts:**  **MEDIUM** — UI logic for calls

#### IncomingCallDialog.java
- **Purpose:** Incoming call notification dialog with accept/reject buttons.
- **Risk level for conflicts:**  **LOW** — Simple dialog

#### OutgoingCallDialog.java
- **Purpose:** Outgoing call ringing dialog with cancel button.
- **Risk level for conflicts:**  **LOW** — Simple dialog

### gui/controller/ — View Controllers

#### MainController.java
- **Purpose:** Main application controller. Handles navigation, CallManager initialization, and global call callbacks.
- **Related modules:** CallManager, ActiveCallDialog, IncomingCallDialog
- **Critical code regions:**
  - Lines 133-209: `initialize()` — App startup, CallManager init
  - Lines 703-745: `setupGlobalCallCallbacks()` — Incoming call handling
- **Risk level for conflicts:**  **MEDIUM** — Entry point modifications

#### ChatViewController.java
- **Purpose:** Chat view controller for messaging UI.
- **Risk level for conflicts:**  **LOW** — Chat UI logic

#### MeetingPanelController.java
- **Purpose:** Meeting room panel controller.
- **Risk level for conflicts:**  **MEDIUM** — Meeting feature changes

### gui/service/ — Application Services

#### ChatService.java
- **Purpose:** Singleton message service. Handles P2P messaging, file transfers, and persistence integration.
- **Related modules:** P2PConnectionManager, MessagePersister, ContactService
- **Critical code regions:**
  - Lines 125-204: `sendMessage()` — P2P routing with server fallback
  - Lines 224-296: `receiveP2PMessage()` — Incoming message handling + persistence
  - Lines 303-474: `sendFileMessage()`/`doSendFile()` — File transfer workflow
- **Risk level for conflicts:**  **MEDIUM** — Messaging changes

#### ContactService.java
- **Purpose:** Contact list management service.
- **Risk level for conflicts:**  **LOW** — Contact UI logic

### p2p/ — P2P Connection Management

#### P2PConnectionManager.java
- **Purpose:** WebRTC DataChannel-based P2P messaging/file transfer. Separate from voice/video calls.
- **Related modules:** WebRTCClient, DataChannelReliableMessaging, DataChannelFileTransfer
- **Critical code regions:**
  - Lines 65-114: `initialize()` — Factory sharing with CallManager
  - Lines 172-249: `createConnection()` — Peer connection setup with MESH_* signals
  - Lines 304-381: `handleP2POffer()` — Answer side SDP handling
  - Lines 532-999: `P2PConnection` inner class — Connection lifecycle
- **Risk level for conflicts:**  **MEDIUM** — P2P feature development
- **Signal routing concerns:**
  - Uses MESH_* signal types for reliability
  - Must not conflict with CallManager signals

#### DataChannelReliableMessaging.java
- **Purpose:** Reliable messaging protocol over DataChannel with chunking, ACK/NACK, CRC.
- **Risk level for conflicts:**  **LOW** — Protocol implementation

#### DataChannelFileTransfer.java
- **Purpose:** File transfer coordinator over DataChannel.
- **Risk level for conflicts:**  **LOW** — File transfer protocol

### file_transfer/ — File Transfer Protocol

#### FileTransferRuntime.java
- **Purpose:** Runtime coordinator for file transfer sender/receiver.
- **Risk level for conflicts:**  **LOW**

#### EnhancedFileTransferSender.java
- **Purpose:** Enhanced sender with congestion control and NACK handling.
- **Risk level for conflicts:**  **LOW**

#### FileTransferReceiver.java
- **Purpose:** Chunk-based file receiver with reassembly.
- **Risk level for conflicts:**  **LOW**

#### HybridCongestionController.java
- **Purpose:** Adaptive congestion control algorithm.
- **Risk level for conflicts:**  **LOW**

### storage/ — Persistent Storage

#### LocalDatabase.java
- **Purpose:** SQLite database wrapper with encryption support.
- **Risk level for conflicts:**  **LOW**

#### LocalMessageRepository.java
- **Purpose:** Message persistence repository.
- **Risk level for conflicts:**  **LOW**

### Resources

#### FXML Files (view/*.fxml)
- **Risk level for conflicts:**  **LOW** — UI layout only

#### CSS Files (styles/*.css, css/*.css)
- **Risk level for conflicts:**  **LOW** — Styling only

---

## PART 2 — BRANCH IMPACT MAP

### bugfix/BUGFIX-01-meeting-exit-crash

**Responsible:** TBD  
**Files Modified:**
- `CallManager.java` — cleanup() method, state transitions
- `WebRTCClient.java` — close() method, resource disposal
- `ActiveCallDialog.java` — close() handler
- `VideoPanel.java` — dispose() and detachVideoTrack()
- `FrameProcessor.java` — close() and drainQueue()

**Conflict Risk:**  **HIGH**  
**Rationale:** Meeting exit touches the entire call teardown path. Multiple cleanup methods must coordinate properly. `cleanup()` in CallManager has recursion prevention that can conflict with other exit flow changes.

---

### bugfix/BUGFIX-02-device-switch

**Responsible:** TBD  
**Files Modified:**
- `WebRTCClient.java` — AudioDeviceModule operations, device selection
- `CallManager.java` — toggleAudio(), toggleVideo()
- `ActiveCallDialog.java` — UI button handlers
- `CameraCaptureService.java` — Camera switching

**Conflict Risk:**  **MEDIUM**  
**Rationale:** Device switching is relatively isolated but touches the sensitive AudioDeviceModule. Windows COM threading must be respected.

---

### bugfix/BUGFIX-03-crossplatform-join

**Responsible:** TBD  
**Files Modified:**
- `WebRTCClient.java` — Platform initialization, codec handling
- `WebRTCPlatformConfig.java` — Codec reordering logic
- `CallManager.java` — handleOffer(), addTrack() timing
- `FrameProcessor.java` — Platform thread selection

**Conflict Risk:**  **CRITICAL**  
**Rationale:** This is the current critical bug (Linux→Windows video). Touches SDP negotiation, track addition order, and platform-specific initialization. Direct conflict with BUGFIX-05 and BUGFIX-06.

---

### bugfix/BUGFIX-04-ghost-threads

**Responsible:** TBD  
**Files Modified:**
- `WebRTCClient.java` — webrtcExecutor lifecycle, shutdown()
- `FrameProcessor.java` — Worker thread lifecycle
- `ScreenShareManager.java` — asyncExecutor lifecycle
- `P2PConnectionManager.java` — Thread cleanup

**Conflict Risk:**  **MEDIUM**  
**Rationale:** Thread lifecycle is spread across multiple files but changes are typically additive (adding shutdown hooks).

---

### bugfix/BUGFIX-05-windows-mic-routing

**Responsible:** TBD  
**Files Modified:**
- `WebRTCClient.java` — initWindowsAudio(), ensureRecordingStarted()
- `CallManager.java` — addAudioTrack() timing
- `WebRTCPlatformConfig.java` — Windows-specific config

**Conflict Risk:**  **HIGH**  
**Rationale:** Windows audio initialization is sensitive to COM threading. Direct conflict with BUGFIX-03 (cross-platform) on WebRTCClient.java.

---

### bugfix/BUGFIX-06-linux-capture-crash

**Responsible:** TBD  
**Files Modified:**
- `WebRTCClient.java` — initLinuxAudio()
- `CameraCaptureService.java` — Camera enumeration
- `ScreenShareManager.java` — startLinuxCapture()
- `LinuxScreenShareEngine.java` — FFmpeg pipeline

**Conflict Risk:**  **MEDIUM**  
**Rationale:** Linux-specific changes are isolated to Linux code paths. May conflict with BUGFIX-03 on platform detection.

---

### bugfix/BUGFIX-07-macos-scroll-freeze

**Responsible:** TBD  
**Files Modified:**
- `VideoPanel.java` — AnimationTimer, scroll event handling
- `MainController.java` — macOS fullscreen handling
- `MacOSFullscreenHandler.java` — macOS-specific code

**Conflict Risk:**  **LOW**  
**Rationale:** macOS UI issues are isolated to macOS code paths and UI components.

---

### bugfix/BUGFIX-08-ghost-audio

**Responsible:** TBD  
**Files Modified:**
- `WebRTCClient.java` — handleRemoteAudioTrack(), cleanupAllAudioSinks()
- `CallManager.java` — cleanup() audio handling
- `ActiveCallDialog.java` — Audio mute/unmute

**Conflict Risk:**  **MEDIUM**  
**Rationale:** Audio sink management is centralized in WebRTCClient. May conflict with BUGFIX-05 on audio initialization.

---

### opt/OPT-01-javafx-cpu-optimization

**Responsible:** TBD  
**Files Modified:**
- `VideoPanel.java` — AnimationTimer optimization
- `FrameProcessor.java` — Queue management
- `FrameRenderResult.java` — Memory layout
- `ArgbBufferPool.java` — Pool sizing

**Conflict Risk:**  **HIGH**  
**Rationale:** Core rendering path optimization. Direct conflict with BUGFIX-07 (macOS freeze) on VideoPanel.java.

---

### opt/OPT-02-zero-copy-buffer

**Responsible:** TBD  
**Files Modified:**
- `FrameProcessor.java` — Buffer handling
- `FrameRenderResult.java` — Zero-copy wrapper
- `ArgbBufferPool.java` — Direct buffer support
- `VideoPanel.java` — PixelWriter integration

**Conflict Risk:**  **HIGH**  
**Rationale:** Fundamental change to video data flow. Conflicts with OPT-01 and any VideoPanel changes.

---

### opt/OPT-03-platform-deps

**Responsible:** TBD  
**Files Modified:**
- `build.gradle` — Dependency declarations
- `WebRTCPlatformConfig.java` — Platform detection
- `PlatformDetector.java` — Detection logic

**Conflict Risk:**  **LOW**  
**Rationale:** Build configuration and detection are mostly additive.

---

### opt/OPT-04-memory-leak-fix

**Responsible:** TBD  
**Files Modified:**
- `WebRTCClient.java` — Resource disposal
- `VideoPanel.java` — Image disposal
- `FrameProcessor.java` — Frame release
- `ArgbBufferPool.java` — Pool cleanup

**Conflict Risk:**  **MEDIUM**  
**Rationale:** Memory leak fixes span multiple files but changes are typically defensive additions.

---

### opt/OPT-05-dependency-cleanup

**Responsible:** TBD  
**Files Modified:**
- `build.gradle` — Remove unused deps
- Various imports across codebase

**Conflict Risk:**  **LOW**  
**Rationale:** Build-level changes, minimal code impact.

---

### opt/OPT-06-logging-optimization

**Responsible:** TBD  
**Files Modified:**
- `WebRTCClient.java` — Log statement optimization
- `CallManager.java` — Log statement optimization
- `FrameProcessor.java` — Log statement optimization
- `Logger.java` — Logging infrastructure

**Conflict Risk:**  **LOW**  
**Rationale:** Logging changes are typically non-breaking.

---

## PART 3 — DEVELOPER RESPONSIBILITY MATRIX

### Developer → Subsystem Ownership

| Subsystem | Primary Owner | Backup Owner |
|-----------|--------------|--------------|
| WebRTC negotiation (WebRTCClient, CallManager) | **Hasan** | Karadağ |
| Device switching (Audio/Video toggle) | **Resul** | Hasan |
| Audio routing (AudioDeviceModule) | **Resul** | Hasan |
| Video pipeline (FrameProcessor, VideoPanel) | **Yaaz** | Meriç |
| UI JavaFX controllers | **Meriç** | Yaaz |
| Chat & DM (ChatService, P2P messaging) | **Hayri** | Meriç |
| Screen Share (ScreenShareManager) | **Hasan** | Yaaz |
| Capture - Windows | **Resul** | Hasan |
| Capture - Linux | **Hasan** | Karadağ |
| Capture - macOS | **Yaaz** | Meriç |
| Platform dependencies | **Karadağ** | Hasan |
| Optimization (zero-copy, memory) | **Yaaz** | Karadağ |
| Logging & diagnostics | **Karadağ** | Hayri |
| File Transfer | **Hayri** | Karadağ |
| Storage/Persistence | **Hayri** | Meriç |

### Developer → File Ownership

#### Hasan
- `WebRTCClient.java` (PRIMARY)
- `CallManager.java` (PRIMARY)
- `WebRTCPlatformConfig.java` (PRIMARY)
- `WebRTCSignalingClient.java`
- `GroupCallManager.java`
- `ScreenShareManager.java`
- `CameraCaptureService.java`

#### Resul
- `WebRTCClient.java` — Audio-specific sections
- `ActiveCallDialog.java` — Device controls
- Windows platform audio code

#### Yaaz
- `VideoPanel.java` (PRIMARY)
- `FrameProcessor.java` (PRIMARY)
- `FrameRenderResult.java`
- `ArgbBufferPool.java`
- `VideoPipelineStats.java`
- macOS-specific code

#### Meriç
- `MainController.java`
- `ChatViewController.java`
- `MeetingPanelController.java`
- `ActiveCallDialog.java` — UI layout
- All FXML files
- CSS files

#### Hayri
- `ChatService.java` (PRIMARY)
- `P2PConnectionManager.java`
- `DataChannelReliableMessaging.java`
- `DataChannelFileTransfer.java`
- `LocalDatabase.java`
- `LocalMessageRepository.java`
- `MessagePersister.java`
- File transfer protocol classes

#### Karadağ
- `build.gradle`
- `PlatformDetector.java`
- `Logger.java`
- Dependency management
- CI/CD configuration
- Documentation

---

## PART 4 — CONFLICT PROBABILITY MAP

### HIGH CONFLICT AREAS

#### WebRTCClient.java
- **Touched by:** BUGFIX-01, BUGFIX-02, BUGFIX-03, BUGFIX-04, BUGFIX-05, BUGFIX-06, BUGFIX-08, OPT-04, OPT-06
- **Why:** Central point for all WebRTC operations. Platform initialization, track management, and connection lifecycle all live here.
- **Coordination required:** 
  - BUGFIX-03 + BUGFIX-05 must coordinate on audio initialization order
  - BUGFIX-04 + OPT-04 must coordinate on resource lifecycle
- **Final merge authority:** **Hasan**

#### CallManager.java
- **Touched by:** BUGFIX-01, BUGFIX-02, BUGFIX-03, BUGFIX-05, BUGFIX-08, OPT-06
- **Why:** Call state machine is sensitive to method call order. The recent `addTrack AFTER setRemoteDescription` fix is critical.
- **Coordination required:**
  - Any change to `handleOffer()` or `acceptCall()` must preserve track timing
  - Cleanup changes in BUGFIX-01 must not break BUGFIX-08 audio cleanup
- **Final merge authority:** **Hasan**

#### VideoPanel.java
- **Touched by:** BUGFIX-01, BUGFIX-07, OPT-01, OPT-02, OPT-04
- **Why:** Rendering pipeline is performance-critical. AnimationTimer and frame handling are sensitive.
- **Coordination required:**
  - OPT-01 + OPT-02 must be merged together or sequentially
  - BUGFIX-07 must be resolved before optimization branches
- **Final merge authority:** **Yaaz**

#### FrameProcessor.java
- **Touched by:** BUGFIX-03, BUGFIX-04, OPT-01, OPT-02, OPT-04, OPT-06
- **Why:** Critical path for video processing. Thread selection and buffer management are sensitive.
- **Coordination required:**
  - OPT-02 zero-copy changes fundamentally alter data flow
  - BUGFIX-04 thread cleanup must not conflict with OPT-01 optimizations
- **Final merge authority:** **Yaaz**

###  MEDIUM CONFLICT AREAS

#### WebRTCPlatformConfig.java
- **Touched by:** BUGFIX-03, BUGFIX-05, BUGFIX-06, OPT-03
- **Coordination required:** Platform branches should merge sequentially
- **Final merge authority:** **Hasan**

#### ActiveCallDialog.java
- **Touched by:** BUGFIX-01, BUGFIX-02, BUGFIX-08, OPT-01
- **Final merge authority:** **Meriç**

#### P2PConnectionManager.java
- **Touched by:** BUGFIX-04, OPT-06
- **Final merge authority:** **Hayri**

###  LOW CONFLICT AREAS

- `CameraCaptureService.java`
- `WebRTCSessionManager.java`
- `ChatService.java`
- Storage classes
- FXML/CSS files

---

## PART 5 — MERGE STRATEGY RECOMMENDATIONS

### Recommended Merge Order

```
1. BUGFIX-03-crossplatform-join    ← PRIORITY: Critical bug affecting users
   └── Contains the "addTrack AFTER setRemoteDescription" fix
   
2. BUGFIX-05-windows-mic-routing   ← After BUGFIX-03 stabilizes
   └── Windows audio depends on correct track timing
   
3. BUGFIX-01-meeting-exit-crash    ← After audio path is stable
   └── Cleanup logic depends on init logic
   
4. BUGFIX-08-ghost-audio           ← After cleanup is stable
   └── Audio cleanup depends on cleanup infrastructure
   
5. BUGFIX-04-ghost-threads         ← After all resource paths defined
   └── Thread cleanup is safest after resource lifecycle is final
   
6. BUGFIX-06-linux-capture-crash   ← Platform-specific, independent
7. BUGFIX-07-macos-scroll-freeze   ← Platform-specific, independent
8. BUGFIX-02-device-switch         ← Feature improvement, lowest priority

--- Optimization branches after all bugfixes ---

9. OPT-01-javafx-cpu-optimization  ← Must be before OPT-02
10. OPT-02-zero-copy-buffer        ← Depends on OPT-01 stabilizing
11. OPT-04-memory-leak-fix         ← Can merge in parallel with OPT-06
12. OPT-06-logging-optimization    ← Low risk, can merge anytime
13. OPT-03-platform-deps           ← Build-level, independent
14. OPT-05-dependency-cleanup      ← Build-level, last
```

### Pre-Merge Checklist

For each branch merge:

1. **Test on all three platforms** (Windows, Linux, macOS)
2. **Verify call flow works:**
   - Windows → Windows ✓
   - Linux → Windows ✓
   - Windows → Linux ✓
   - macOS → Windows ✓
3. **Check for ghost threads:** Run app, end call, verify thread count returns to baseline
4. **Check for memory leaks:** Use VisualVM or similar during extended call
5. **Verify no regressions** in P2P messaging (DataChannel still works)

### Conflict Resolution Protocol

1. **Same-file conflicts:** Owner of PRIMARY file has final say
2. **Cross-module conflicts:** Schedule sync meeting between owners
3. **Architecture-level conflicts:** Escalate to **Hasan** (WebRTC lead)
4. **All conflicts affecting CallManager.java:** Require **Hasan** review

### Warning Flags 

- **Never merge two branches that both modify `handleOffer()` or `acceptCall()` without integration testing**
- **Never modify `initWindowsAudio()` without testing on actual Windows hardware**
- **Never change track addition order without full call flow test**
- **Virtual Thread usage in native code paths requires careful review**

---

## Appendix: File Quick Reference

| File | Risk | Primary Owner | Key Concern |
|------|------|---------------|-------------|
| WebRTCClient.java | HIGH | Hasan | Platform init, threading |
| CallManager.java | HIGH | Hasan | State machine, track timing |
| VideoPanel.java | HIGH | Yaaz | AnimationTimer, FX thread |
| FrameProcessor.java | HIGH | Yaaz | Thread selection, native code |
| WebRTCPlatformConfig.java | MEDIUM | Hasan | Codec ordering |
| ActiveCallDialog.java | MEDIUM | Meriç | UI + track attachment |
| P2PConnectionManager.java | MEDIUM | Hayri | Signal routing |
| CameraCaptureService.java | LOW  | Hasan | Resource lifecycle |
| ChatService.java | LOW  | Hayri | P2P + persistence |

---

*Document maintained by Architecture Team. Last updated: December 7, 2025*

