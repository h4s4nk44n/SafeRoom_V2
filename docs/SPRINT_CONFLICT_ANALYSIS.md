# SafeRoomV2 Sprint Conflict Analysis

**Sprint:** December 7-21, 2025  
**Team:** RoftCore (6 developers)  
**Document Purpose:** Developer → File mapping & Conflict risk analysis

---

## Table of Contents

1. [Branch Overview](#branch-overview)
2. [Task → File Mapping](#task--file-mapping)
3. [Conflict Matrix](#conflict-matrix)
4. [High-Risk Conflict Zones](#high-risk-conflict-zones)
5. [Merge Strategy](#merge-strategy)
6. [Daily Sync Requirements](#daily-sync-requirements)

---

## Branch Overview

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        SPRINT BRANCH STRUCTURE                          │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│   develop (protected)                                                   │
│       │                                                                 │
│       ├── bugfix-meeting-core ──────────────────────────────────────┐  │
│       │       │                                                     │  │
│       │       ├── BUGFIX-01 (Hasan)     Meeting Exit Crash          │  │
│       │       ├── BUGFIX-02 (Resul)     Device Switch               │  │
│       │       ├── BUGFIX-03 (Hayri)     Cross-platform Join         │  │
│       │       ├── BUGFIX-04 (Yaaz)      Ghost Threads               │  │
│       │       └── BUGFIX-08 (Hasan+Resul+Hayri) Ghost Audio         │  │
│       │                                                             │  │
│       ├── fix-crossplatform-capture ────────────────────────────────┤  │
│       │       │                                                     │  │
│       │       ├── BUGFIX-05 (Meriç)     Windows Mic Routing         │  │
│       │       ├── BUGFIX-06 (Karadağ+Hasan+Resul) Linux Capture     │  │
│       │       └── BUGFIX-07 (Hayri)     MacOS Scroll Freeze         │  │
│       │                                                             │  │
│       └── optimization ─────────────────────────────────────────────┤  │
│               │                                                     │  │
│               ├── OPT-01 (Karadağ)      JavaFX CPU Optimization     │  │
│               ├── OPT-02 (Karadağ)      Zero-Copy Buffering         │  │
│               ├── OPT-03 (Karadağ)      Platform-Specific Deps      │  │
│               ├── OPT-04 (Meriç+Hasan+Resul) Memory Leak Fix        │  │
│               ├── OPT-05 (Karadağ)      Dependency Cleanup          │  │
│               └── OPT-06 (Karadağ)      Logging Optimization        │  │
│                                                                     │  │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Task → File Mapping

### Branch: `bugfix-meeting-core`

#### BUGFIX-01 — Meeting Exit Crash
**Owner:** Hasan  
**Files to Modify:**

| File | Change Type | Risk |
|------|-------------|------|
| `CallManager.java` | cleanup() method, state transitions | CRITICAL |
| `WebRTCClient.java` | close() method, resource disposal | CRITICAL |
| `ActiveCallDialog.java` | close() handler, video panel cleanup | HIGH |
| `VideoPanel.java` | dispose(), detachVideoTrack() | HIGH |
| `FrameProcessor.java` | close(), drainQueue() | MEDIUM |

**Lines of Interest:**
- `CallManager.java:766-802` — cleanup() recursion prevention
- `WebRTCClient.java:744-815` — close() resource teardown
- `VideoPanel.java:233-236` — dispose()

---

#### BUGFIX-02 — Device Switch
**Owner:** Resul  
**Files to Modify:**

| File | Change Type | Risk |
|------|-------------|------|
| `WebRTCClient.java` | AudioDeviceModule hot-swap | CRITICAL |
| `CallManager.java` | toggleAudio(), toggleVideo() | MEDIUM |
| `ActiveCallDialog.java` | Button handlers | LOW |
| `CameraCaptureService.java` | Camera switching (if needed) | LOW |

**Lines of Interest:**
- `WebRTCClient.java:141-208` — Windows audio init (COM threading)
- `WebRTCClient.java:1093-1125` — ensurePlayoutStarted(), ensureRecordingStarted()
- `CallManager.java:312-325` — toggleAudio(), toggleVideo()

---

#### BUGFIX-03 — Cross-platform Join
**Owner:** Hayri  
**Files to Modify:**

| File | Change Type | Risk |
|------|-------------|------|
| `WebRTCClient.java` | Platform initialization, codec handling | CRITICAL |
| `WebRTCPlatformConfig.java` | Codec reordering logic | HIGH |
| `CallManager.java` | handleOffer(), addTrack() timing | CRITICAL |
| `FrameProcessor.java` | Platform thread selection | MEDIUM |

**Lines of Interest:**
- `CallManager.java:534-575` — handleOffer() (CRITICAL: track timing)
- `WebRTCPlatformConfig.java:45-83` — detect() platform logic
- `WebRTCClient.java:572-683` — createOffer(), createAnswer()

---

#### BUGFIX-04 — Ghost Threads
**Owner:** Yaaz  
**Files to Modify:**

| File | Change Type | Risk |
|------|-------------|------|
| `WebRTCClient.java` | webrtcExecutor lifecycle | HIGH |
| `FrameProcessor.java` | Worker thread lifecycle | CRITICAL |
| `ScreenShareManager.java` | asyncExecutor lifecycle | MEDIUM |
| `P2PConnectionManager.java` | Thread cleanup | LOW |
| `VideoPipelineStats.java` | Stats thread (if any) | LOW |

**Lines of Interest:**
- `WebRTCClient.java:343-382` — shutdown() executor cleanup
- `FrameProcessor.java:149-154` — close()
- `ScreenShareManager.java:352-358` — close()

---

#### BUGFIX-08 — Ghost Audio
**Owners:** Hasan, Resul, Hayri  
**Files to Modify:**

| File | Change Type | Risk |
|------|-------------|------|
| `WebRTCClient.java` | handleRemoteAudioTrack(), cleanupAllAudioSinks() | CRITICAL |
| `CallManager.java` | cleanup() audio handling | HIGH |
| `ActiveCallDialog.java` | Audio mute/unmute state | MEDIUM |
| `GroupCallManager.java` | Multi-party audio cleanup | MEDIUM |

**Lines of Interest:**
- `WebRTCClient.java:1012-1088` — Audio sink management
- `WebRTCClient.java:1065-1088` — cleanupAllAudioSinks()
- `CallManager.java:766-802` — cleanup()

---

### Branch: `fix-crossplatform-capture`

#### BUGFIX-05 — Windows Mic Routing
**Owner:** Meriç  
**Files to Modify:**

| File | Change Type | Risk |
|------|-------------|------|
| `WebRTCClient.java` | initWindowsAudio(), ensureRecordingStarted() | CRITICAL |
| `CallManager.java` | addAudioTrack() timing | MEDIUM |
| `WebRTCPlatformConfig.java` | Windows-specific config | LOW |

**Lines of Interest:**
- `WebRTCClient.java:141-208` — initWindowsAudio() (COM STA thread)
- `WebRTCClient.java:1111-1125` — ensureRecordingStarted()

---

#### BUGFIX-06 — Linux Capture Crash
**Owners:** Karadağ, Hasan, Resul  
**Files to Modify:**

| File | Change Type | Risk |
|------|-------------|------|
| `WebRTCClient.java` | initLinuxAudio() | HIGH |
| `CameraCaptureService.java` | Camera enumeration | MEDIUM |
| `ScreenShareManager.java` | startLinuxCapture() | HIGH |
| `LinuxScreenShareEngine.java` | FFmpeg pipeline removal | CRITICAL |
| `build.gradle` | OpenCV/FFmpeg removal | LOW |

**Lines of Interest:**
- `WebRTCClient.java:214-235` — initLinuxAudio()
- `ScreenShareManager.java:123-138` — startLinuxCapture()

---

#### BUGFIX-07 — MacOS Scroll Freeze
**Owner:** Hayri  
**Files to Modify:**

| File | Change Type | Risk |
|------|-------------|------|
| `VideoPanel.java` | AnimationTimer, scroll event handling | HIGH |
| `MainController.java` | macOS fullscreen handling | MEDIUM |
| `MacOSFullscreenHandler.java` | macOS-specific code | LOW |
| CSS files | Scroll-related styles | LOW |

**Lines of Interest:**
- `VideoPanel.java:44-87` — AnimationTimer setup
- `MainController.java:520-551` — handleMaximize() macOS

---

### Branch: `optimization`

#### OPT-01 — JavaFX CPU Optimization
**Owner:** Karadağ  
**Files to Modify:**

| File | Change Type | Risk |
|------|-------------|------|
| `VideoPanel.java` | AnimationTimer optimization | CRITICAL |
| `FrameProcessor.java` | Queue management | HIGH |
| `FrameRenderResult.java` | Memory layout | MEDIUM |
| `ArgbBufferPool.java` | Pool sizing | MEDIUM |

---

#### OPT-02 — Zero-Copy Buffering
**Owner:** Karadağ  
**Files to Modify:**

| File | Change Type | Risk |
|------|-------------|------|
| `FrameProcessor.java` | Buffer handling | CRITICAL |
| `FrameRenderResult.java` | Zero-copy wrapper | CRITICAL |
| `ArgbBufferPool.java` | Direct buffer support | HIGH |
| `VideoPanel.java` | PixelWriter integration | HIGH |

---

#### OPT-03 — Platform-Specific Deps
**Owner:** Karadağ  
**Files to Modify:**

| File | Change Type | Risk |
|------|-------------|------|
| `build.gradle` | Platform filters | LOW |
| `settings.gradle` | Build config | LOW |
| `WebRTCPlatformConfig.java` | Platform detection | LOW |
| `PlatformDetector.java` | Detection logic | LOW |

---

#### OPT-04 — Memory Leak Fix
**Owners:** Meriç, Hasan, Resul  
**Files to Modify:**

| File | Change Type | Risk |
|------|-------------|------|
| `WebRTCClient.java` | Resource disposal | HIGH |
| `VideoPanel.java` | Image disposal | HIGH |
| `FrameProcessor.java` | Frame release | HIGH |
| `ArgbBufferPool.java` | Pool cleanup | MEDIUM |
| `ActiveCallDialog.java` | Video panel cleanup | MEDIUM |

---

#### OPT-05 — Dependency Cleanup
**Owner:** Karadağ  
**Files to Modify:**

| File | Change Type | Risk |
|------|-------------|------|
| `build.gradle` | Remove unused deps | LOW |
| `LinuxScreenShareEngine.java` | Remove FFmpeg | MEDIUM |
| `ChatService.java` | Remove PDF preview code | LOW |
| Various imports | Cleanup | LOW |

---

#### OPT-06 — Logging Optimization
**Owner:** Karadağ  
**Files to Modify:**

| File | Change Type | Risk |
|------|-------------|------|
| `WebRTCClient.java` | Log throttling | LOW |
| `CallManager.java` | Log throttling | LOW |
| `FrameProcessor.java` | Log throttling | LOW |
| `Logger.java` | Logging infrastructure | LOW |

---

## Conflict Matrix

### Developer × File Conflict Risk

```
                    │ WebRTC │ Call   │ Video │ Frame │ Platform│ Active │ Screen │
                    │ Client │Manager │ Panel │ Proc  │ Config  │ Dialog │ Share  │
────────────────────┼────────┼────────┼───────┼───────┼─────────┼────────┼────────┤
Hasan               │   ██   │   ██   │   █   │   █   │    █    │   █    │   █    │
(01,06,08,OPT04)    │        │        │       │       │         │        │        │
────────────────────┼────────┼────────┼───────┼───────┼─────────┼────────┼────────┤
Resul               │   ██   │   █    │       │       │         │   █    │        │
(02,06,08,OPT04)    │        │        │       │       │         │        │        │
────────────────────┼────────┼────────┼───────┼───────┼─────────┼────────┼────────┤
Hayri               │   █    │   ██   │   █   │   █   │    █    │        │        │
(03,07,08)          │        │        │       │       │         │        │        │
────────────────────┼────────┼────────┼───────┼───────┼─────────┼────────┼────────┤
Yaaz                │   █    │        │   █   │   ██  │         │        │   █    │
(04)                │        │        │       │       │         │        │        │
────────────────────┼────────┼────────┼───────┼───────┼─────────┼────────┼────────┤
Meriç               │   ██   │   █    │   █   │   █   │    █    │   █    │        │
(05,OPT04)          │        │        │       │       │         │        │        │
────────────────────┼────────┼────────┼───────┼───────┼─────────┼────────┼────────┤
Karadağ             │   █    │   █    │   ██  │   ██  │    █    │        │   █    │
(06,OPT01-06)       │        │        │       │       │         │        │        │
────────────────────┴────────┴────────┴───────┴───────┴─────────┴────────┴────────┘

██ = Primary ownership (will make significant changes)
█  = Secondary involvement (may touch this file)
```

### Task × Task Conflict Matrix

```
        │ 01 │ 02 │ 03 │ 04 │ 05 │ 06 │ 07 │ 08 │OPT1│OPT2│OPT3│OPT4│OPT5│OPT6│
────────┼────┼────┼────┼────┼────┼────┼────┼────┼────┼────┼────┼────┼────┼────┤
  01    │ ── │ 🔴 │ 🔴 │ 🟡 │ 🟡 │ 🟡 │ 🟡 │ 🔴 │ 🟡 │ 🟡 │ 🟢 │ 🔴 │ 🟢 │ 🟢 │
────────┼────┼────┼────┼────┼────┼────┼────┼────┼────┼────┼────┼────┼────┼────┤
  02    │ 🔴 │ ── │ 🔴 │ 🟡 │ 🔴 │ 🟡 │ 🟢 │ 🔴 │ 🟢 │ 🟢 │ 🟢 │ 🟡 │ 🟢 │ 🟢 │
────────┼────┼────┼────┼────┼────┼────┼────┼────┼────┼────┼────┼────┼────┼────┤
  03    │ 🔴 │ 🔴 │ ── │ 🟡 │ 🟡 │ 🟡 │ 🟢 │ 🟡 │ 🟡 │ 🟡 │ 🟢 │ 🟡 │ 🟢 │ 🟢 │
────────┼────┼────┼────┼────┼────┼────┼────┼────┼────┼────┼────┼────┼────┼────┤
  04    │ 🟡 │ 🟡 │ 🟡 │ ── │ 🟢 │ 🟡 │ 🟢 │ 🟡 │ 🔴 │ 🔴 │ 🟢 │ 🔴 │ 🟢 │ 🟡 │
────────┼────┼────┼────┼────┼────┼────┼────┼────┼────┼────┼────┼────┼────┼────┤
  05    │ 🟡 │ 🔴 │ 🟡 │ 🟢 │ ── │ 🟡 │ 🟢 │ 🟡 │ 🟢 │ 🟢 │ 🟢 │ 🟡 │ 🟢 │ 🟢 │
────────┼────┼────┼────┼────┼────┼────┼────┼────┼────┼────┼────┼────┼────┼────┤
  06    │ 🟡 │ 🟡 │ 🟡 │ 🟡 │ 🟡 │ ── │ 🟢 │ 🟡 │ 🟢 │ 🟢 │ 🟡 │ 🟡 │ 🔴 │ 🟡 │
────────┼────┼────┼────┼────┼────┼────┼────┼────┼────┼────┼────┼────┼────┼────┤
  07    │ 🟡 │ 🟢 │ 🟢 │ 🟢 │ 🟢 │ 🟢 │ ── │ 🟢 │ 🔴 │ 🔴 │ 🟢 │ 🔴 │ 🟢 │ 🟢 │
────────┼────┼────┼────┼────┼────┼────┼────┼────┼────┼────┼────┼────┼────┼────┤
  08    │ 🔴 │ 🔴 │ 🟡 │ 🟡 │ 🟡 │ 🟡 │ 🟢 │ ── │ 🟢 │ 🟢 │ 🟢 │ 🟡 │ 🟢 │ 🟢 │
────────┼────┼────┼────┼────┼────┼────┼────┼────┼────┼────┼────┼────┼────┼────┤
 OPT1   │ 🟡 │ 🟢 │ 🟡 │ 🔴 │ 🟢 │ 🟢 │ 🔴 │ 🟢 │ ── │ 🔴 │ 🟢 │ 🔴 │ 🟢 │ 🟢 │
────────┼────┼────┼────┼────┼────┼────┼────┼────┼────┼────┼────┼────┼────┼────┤
 OPT2   │ 🟡 │ 🟢 │ 🟡 │ 🔴 │ 🟢 │ 🟢 │ 🔴 │ 🟢 │ 🔴 │ ── │ 🟢 │ 🔴 │ 🟢 │ 🟢 │
────────┼────┼────┼────┼────┼────┼────┼────┼────┼────┼────┼────┼────┼────┼────┤
 OPT3   │ 🟢 │ 🟢 │ 🟢 │ 🟢 │ 🟢 │ 🟡 │ 🟢 │ 🟢 │ 🟢 │ 🟢 │ ── │ 🟢 │ 🟡 │ 🟢 │
────────┼────┼────┼────┼────┼────┼────┼────┼────┼────┼────┼────┼────┼────┼────┤
 OPT4   │ 🔴 │ 🟡 │ 🟡 │ 🔴 │ 🟡 │ 🟡 │ 🔴 │ 🟡 │ 🔴 │ 🔴 │ 🟢 │ ── │ 🟢 │ 🟢 │
────────┼────┼────┼────┼────┼────┼────┼────┼────┼────┼────┼────┼────┼────┼────┤
 OPT5   │ 🟢 │ 🟢 │ 🟢 │ 🟢 │ 🟢 │ 🔴 │ 🟢 │ 🟢 │ 🟢 │ 🟢 │ 🟡 │ 🟢 │ ── │ 🟢 │
────────┼────┼────┼────┼────┼────┼────┼────┼────┼────┼────┼────┼────┼────┼────┤
 OPT6   │ 🟢 │ 🟢 │ 🟢 │ 🟡 │ 🟢 │ 🟡 │ 🟢 │ 🟢 │ 🟢 │ 🟢 │ 🟢 │ 🟢 │ 🟢 │ ── │
────────┴────┴────┴────┴────┴────┴────┴────┴────┴────┴────┴────┴────┴────┴────┘

🔴 = HIGH conflict risk (same files, same regions)
🟡 = MEDIUM conflict risk (same files, different regions)
🟢 = LOW conflict risk (different files)
```

---

## High-Risk Conflict Zones

### 🔴 CRITICAL: WebRTCClient.java

**Touched by:** BUGFIX-01, 02, 03, 04, 05, 06, 08, OPT-04, OPT-06

| Region | Lines | Owners | Tasks |
|--------|-------|--------|-------|
| Platform init | 75-300 | Hasan, Resul, Meriç, Karadağ | 01, 02, 05, 06 |
| Audio handling | 849-1125 | Resul, Meriç | 02, 05, 08 |
| close() | 744-815 | Hasan, Yaaz | 01, 04 |
| Audio sinks | 1012-1088 | Hasan, Resul, Hayri | 08 |

**Resolution Strategy:**
1. Hasan owns overall file structure
2. Daily sync between Hasan, Resul, Meriç on audio sections
3. Yaaz coordinates with Hasan on close() logic
4. File-level lock during critical merge windows

---

### 🔴 CRITICAL: CallManager.java

**Touched by:** BUGFIX-01, 02, 03, 05, 08, OPT-06

| Region | Lines | Owners | Tasks |
|--------|-------|--------|-------|
| handleOffer() | 534-575 | Hayri | 03 |
| cleanup() | 766-802 | Hasan | 01, 08 |
| toggleAudio() | 312-325 | Resul | 02 |
| State machine | throughout | All | ALL |

**Resolution Strategy:**
1. Hayri MUST NOT change handleOffer() without Hasan review
2. cleanup() changes need coordination with Resul (audio cleanup)
3. State machine changes require team meeting

---

### 🔴 CRITICAL: VideoPanel.java

**Touched by:** BUGFIX-01, 04, 07, OPT-01, OPT-02, OPT-04

| Region | Lines | Owners | Tasks |
|--------|-------|--------|-------|
| AnimationTimer | 44-87 | Yaaz, Karadağ, Hayri | 04, 07, OPT-01 |
| dispose() | 233-236 | Hasan | 01 |
| paintFrame() | 161-198 | Karadağ | OPT-01, OPT-02 |
| handleStall() | 283-320 | Yaaz | 04 |

**Resolution Strategy:**
1. OPT-01 and OPT-02 MUST be sequential (Karadağ owns both)
2. BUGFIX-07 must complete BEFORE OPT-01 starts
3. Hasan's dispose() changes are isolated

---

### 🔴 CRITICAL: FrameProcessor.java

**Touched by:** BUGFIX-01, 03, 04, OPT-01, OPT-02, OPT-04, OPT-06

| Region | Lines | Owners | Tasks |
|--------|-------|--------|-------|
| Constructor | 34-53 | Hayri, Karadağ | 03, OPT-01 |
| processLoop | 77-120 | Yaaz, Karadağ | 04, OPT-01, OPT-02 |
| close() | 149-154 | Hasan, Yaaz | 01, 04 |
| convertFrame | 133-139 | Karadağ | OPT-02 |

**Resolution Strategy:**
1. ALL FrameProcessor changes must go through Karadağ
2. BUGFIX-04 (Yaaz) must merge BEFORE OPT-01
3. OPT-02 is blocked until OPT-01 is stable

---

## Developer Conflict Pairs

### 🔴 HIGH RISK PAIRS

| Developer 1 | Developer 2 | Shared Files | Sync Frequency |
|-------------|-------------|--------------|----------------|
| **Hasan** | **Resul** | WebRTCClient, CallManager | Daily |
| **Hasan** | **Hayri** | CallManager, WebRTCClient | Daily |
| **Yaaz** | **Karadağ** | FrameProcessor, VideoPanel | Daily |
| **Meriç** | **Karadağ** | VideoPanel, FrameProcessor | Every 2 days |
| **Resul** | **Meriç** | WebRTCClient (audio) | Daily |

### 🟡 MEDIUM RISK PAIRS

| Developer 1 | Developer 2 | Shared Files | Sync Frequency |
|-------------|-------------|--------------|----------------|
| Hasan | Yaaz | WebRTCClient (close), FrameProcessor | Every 2 days |
| Hasan | Meriç | WebRTCClient (Windows audio) | Every 2 days |
| Hayri | Karadağ | WebRTCPlatformConfig | Weekly |
| Resul | Hayri | CallManager, WebRTCClient (audio) | Every 2 days |

---

## Merge Strategy

### Recommended Merge Order

```
Week 1 (Dec 7-13): Foundation Fixes
═══════════════════════════════════

Day 1-2: BUGFIX-03 (Hayri) — Cross-platform join
         └── Critical path: enables all other testing

Day 2-3: BUGFIX-02 (Resul) — Device switch
         └── After 03 stabilizes audio path

Day 3-4: BUGFIX-01 (Hasan) — Meeting exit crash
         └── After 02 defines cleanup pattern

Day 4-5: BUGFIX-08 (Hasan+Resul+Hayri) — Ghost audio
         └── After cleanup pattern established

Day 5-6: BUGFIX-04 (Yaaz) — Ghost threads
         └── Final cleanup after all resource fixes


Week 2 (Dec 14-20): Platform Fixes + Optimization
══════════════════════════════════════════════════

Day 7-8: BUGFIX-05 (Meriç) — Windows mic
         └── Platform-specific, less risky

Day 8-9: BUGFIX-06 (Karadağ+Hasan+Resul) — Linux capture
         └── Team coordination required

Day 9-10: BUGFIX-07 (Hayri) — MacOS scroll
          └── Platform-specific, independent

Day 10-12: OPT-05 (Karadağ) — Dependency cleanup
           └── Must be before other OPT tasks

Day 12-13: OPT-01 (Karadağ) — JavaFX CPU
           └── After all bugfixes merged

Day 13-14: OPT-02 (Karadağ) — Zero-copy
           └── After OPT-01 stabilizes

Day 14: OPT-03, OPT-06 (Karadağ) — Build & logging
        └── Low risk, can parallel merge


Buffer Day (Dec 21): Integration Testing
═══════════════════════════════════════

OPT-04 (Meriç+Hasan+Resul) — Memory leak
└── Spans multiple PRs, final validation
```

---

## Daily Sync Requirements

### Required Daily Standups

| Time | Participants | Focus |
|------|--------------|-------|
| 09:00 | Hasan, Resul | Audio pipeline status |
| 11:00 | Hasan, Resul, Hayri | CallManager/WebRTCClient sync |
| 14:00 | Yaaz, Karadağ | Video pipeline status |
| 16:00 | All (async Slack) | PR status, blockers |

### File Lock Protocol

When working on CRITICAL zones:

1. **Announce** in Slack: "Locking WebRTCClient.java:100-200 for 2h"
2. **Create** WIP PR immediately (marks intent)
3. **Merge** same day if possible
4. **Release** lock in Slack when done

### Conflict Resolution Escalation

```
Level 1: Direct developer communication (15 min timeout)
Level 2: Hasan arbitration (for WebRTC/CallManager)
Level 3: Team meeting (for architectural decisions)
```

---

## Pre-Merge Checklist

For each PR before merge:

- [ ] Compiled without errors on all platforms
- [ ] No new linter warnings in modified files
- [ ] All related tests pass
- [ ] Cross-platform tested (if touching platform code)
- [ ] Daily sync completed with conflict pairs
- [ ] No uncommitted changes in conflict zones

---

## Summary Tables

### Developer Workload

| Developer | Primary Tasks | Support Tasks | File Count |
|-----------|---------------|---------------|------------|
| **Hasan** | 01, 06 | 08, OPT-04 | 7 files |
| **Resul** | 02, 06 | 08, OPT-04 | 5 files |
| **Hayri** | 03, 07 | 08 | 6 files |
| **Yaaz** | 04 | - | 4 files |
| **Meriç** | 05 | OPT-04 | 5 files |
| **Karadağ** | 06, OPT-01-06 | - | 12 files |

### Critical File Ownership

| File | Primary Owner | Backup | Final Merge Authority |
|------|---------------|--------|----------------------|
| WebRTCClient.java | Hasan | Resul | Hasan |
| CallManager.java | Hasan | Hayri | Hasan |
| VideoPanel.java | Karadağ | Yaaz | Karadağ |
| FrameProcessor.java | Karadağ | Yaaz | Karadağ |
| WebRTCPlatformConfig.java | Hasan | Hayri | Hasan |
| ActiveCallDialog.java | Meriç | Hasan | Meriç |

---

*Document maintained by RoftCore Team. Last updated: December 7, 2025*

