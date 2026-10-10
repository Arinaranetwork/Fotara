// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

# Phase 51 - LiveCollabCanvasAndRelease200

## Goal
Implement the Live Collaborative Study Canvas enabling students to conduct synchronized peer study sessions in real-time using lightweight 6-digit room codes (`FT-XXXX`), low-latency vector stroke synchronization, and peer cursors with participant identification badges. Finalize and package Fotara 2.0.0 Alpha release.

## Scope
- Domain and network protocol models in `com.arinara.fotara.feature.collab.model`:
  - `CollabStrokeVector`, `CollabPeer`, `CollabSessionPacket`, `CollabRoomInfo`.
  - Pure packet serialization and protocol encoding engine (`CollabProtocolEngine`).
- Real-time session coordinator in `com.arinara.fotara.feature.collab.engine`:
  - `CollabSessionEngine`: room lifecycle (create, join via 6-digit code, leave), peer tracking, heartbeat broadcast, and vector stroke stream merging.
- Composable UI elements in `com.arinara.fotara.feature.collab.ui` & `render`:
  - `CollabRoomJoinDialog.kt`: Modal for entering 6-digit room code or initiating a new session.
  - `CollabSessionBar.kt`: Persistent header bar displaying active room code (`FT-XXXX`), active peer chips, and session controls.
  - `PeerCursorOverlay.kt`: Smooth animated peer cursor pointers with student name badges and accent color ring.
- Canvas screen integration in `CanvasScreen.kt`:
  - Menu action item "Collaborative Study Room" in overflow options.
  - Active session header bar rendering at top of canvas.
- Release 2.0.0 Alpha verification, packaging, and artifacts assembly:
  - Bump `versionCode = 36`, `versionName = "2.0.0 Alpha"`.
  - Release APK assembly and SHA-256 generation.
  - Documentation, changelog, and Anchor records completion.

## Out Of Scope
- Heavy external WebRTC audio/video call streaming (voice note audio handled separately in Phase 45).
- Cloud server infrastructure dependencies (designed for local-first peer protocol with simulated/local broadcast).

## Features

### 6-Digit Room Code Protocol (`FT-XXXX`)
- Generates canonical academic room codes formatted as `FT-XXXX` (e.g. `FT-8492`, `FT-1038`).
- Validates room code input formatting with automatic uppercase conversion and hyphen auto-insertion.

### Real-Time Vector Stroke Merging
- Serializes drawing strokes as resolution-independent vector paths with normalized coordinates `(x, y)`.
- Merges peer strokes into canvas display layers with minimal latency.
- Conflict-free resolution ensuring strokes from multiple concurrent writers do not overwrite or distort.

### Peer Cursors & Presence Badges
- Tracks active peers with unique participant IDs, student display names, and distinct color indicators.
- Renders smooth moving cursor indicators (`PeerCursorOverlay`) showing where study partners are pointing or drawing.
- Peer chips in `CollabSessionBar` show active participant count and room connection status.

### Fotara 2.0.0 Alpha Release Packaging
- Complete integration of all Stage 2 milestones (Phases 46-51).
- Full regression verification across all 891 unit tests.
- Production APK build `Fotara_2.0.0_Alpha.apk` with verified SHA-256 checksum.

## UI Mockup
```
┌────────────────────────────────────────────────────────┐
│ [ FT-8492 ] (● 3 peers) [ Alex ] [ Maria ] [ Leave ]   │
├────────────────────────────────────────────────────────┤
│ ← Note: Advanced Calculus Proofs             [ ⚙️ ]    │
├────────────────────────────────────────────────────────┤
│                                                        │
│       ∫ x² dx = x³/3 + C                               │
│                                                        │
│           ↗ [● Alex (Drawing)]                         │
│                                                        │
│                 ↘ [● Maria (Pointing)]                 │
│                                                        │
├────────────────────────────────────────────────────────┤
│ [ ✏️ Pen ]  [ 🖍️ High ]  [ 📐 Shape ]  [ 💬 Text ]     │
└────────────────────────────────────────────────────────┘
```

## Logic Notes
- `CollabSessionEngine` emits `activePeers: StateFlow<Map<String, CollabPeer>>` and `roomCode: StateFlow<String?>`.
- Joining a room subscribes to peer cursor updates and broadcasts local pointer coordinates.
- Leaving a room cleanly clears peer states and resets the session bar.

## Risks
- *Risk*: Network packet jitter causing cursor stutter.
  - *Mitigation*: Interpolate peer cursor positions using Compose `animateFloatAsState` / `animateOffsetAsState`.
- *Risk*: Stroke duplication on packet retry.
  - *Mitigation*: Assign unique UUIDs to every stroke packet and filter duplicates in `CollabSessionEngine`.

## Dependencies
- Phase 44 (Canvas superpowers & drawing view).
- Phase 46 (Space isolation context).

## Acceptance Criteria
- Room creation produces a valid 6-digit code `FT-XXXX`.
- Peers can join and leave session cleanly.
- Peer cursors and strokes render accurately without canvas freezes.
- 100% test pass across collab protocol and session engine tests.
- Release APK `Fotara_2.0.0_Alpha.apk` built and verified.
