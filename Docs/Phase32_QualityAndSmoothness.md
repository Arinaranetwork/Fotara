# Phase 32 - Quality Improvements and Smoothness (Release 1.7.1)

## Goal
Deliver Release 1.7.1 (STABLE) for Fotara, focusing purely on gesture fluidity, layout resilience across system font scales, enhanced workspace tab drag interactions, and remote release asset image rendering in update dialogues and release notes.

## Scope
- Task 0: Verification and reporting on Android skills compliance, 1.7.0 tasks status, and dependency checks.
- Task 1: Canvas and photo drawing pan/zoom smoothness overhaul (debug frame recorder, GPU matrix transform isolation during gestures, debounced off-thread tile/raster re-render, throttled viewport publishing <=10Hz, centroid continuity on multi-touch transitions, stable pointerInput keys).
- Task 2: Screen headers fix for clipped titles and taglines across Home, Notes, Settings, Search, Folder Detail, Profile (content-measured height, non-clipped descenders, reserved tagline slot, multi-font scale verification 0.85x to 2.0x).
- Task 3: Workspace tab hold-longer gesture (Stage 1 long-press panel hold; Stage 2 `MOVE_MODE_EXTRA_HOLD_MS` ~600ms extra hold with distinct haptic, panel fade-out, tab 1.1x scale/elevation via graphicsLayer, immediate drag tracking, pure state machine with injected clock).
- Task 4: Images in release notes, update dialog, What's New, and Update screen (banner asset behind dark scrim, Markdown `![alt](url)` and linked `[![alt](url)](link)`, HTML `<img>`, HTTPS allowlist, aspect ratio reservation, disk cache cap ~25MB).
- Task 5: Release finalization (What's New 1.7.1 entry, versionName "1.7.1", versionCode 28, 0 TODO/FIXME, unit test suite pass, release APK build, git hygiene).

## Out Of Scope
- New general features, settings toggles, or redesigns.
- Database schema changes (`DATABASE_VERSION` strictly stays 17).
- FTS table modifications, `ViewportTransform.kt` modifications, or `fotara.fileprovider` alterations.
- New third-party dependencies (JankStats, Coil extensions, etc.).
- Deep-linked PDF search page navigation and highlighting (logged as Class B, Phase 33).

## Features

### Feature 1: Fluid Zoom and Pan Engine
- **Debug Frame Interval Recorder**: In `src/debug` (absent from release APK), records intervals via `Choreographer.FrameCallback` during active gestures; computes p50, p95, p99, max latency, frames >16.7ms and >33ms; copyable through debug menu "Copy frame stats".
- **Hardware-Accelerated Transform Path**: During gestures, transforms follow touches 1:1 using GPU matrix transformations without allocating Path/Paint/Matrix objects or recomposing child views.
- **Debounced Sharp Tile/Raster Re-render**: Heavy raster re-rendering and QuadTree rebuilds are paused during gestures, running only after settling (~120ms debounce) on background dispatchers.
- **Throttled Viewport State Publishing**: Viewport state (scale, zoom percentage) published to ViewModel/Compose at most 10Hz and upon gesture completion, preventing recomposition storms.
- **Stable PointerInput Keys & Centroid Continuity**: `PhotoDrawingEditor` removes mutable scale/offset from `pointerInput` keys; `PointerStateMachine` maintains centroid continuity across pointer additions/removals.

### Feature 2: Resilient Screen Header Layout
- **Dynamic Content Measurement**: Header height is derived dynamically from `titleLineHeight + gap + taglineLineHeight` respecting system font scaling and font metrics without fixed clipping boxes.
- **Descender Protection**: `includeFontPadding = false` paired with clean `LineHeightStyle` and unconstrained vertical bounds ensures characters with descenders (`g`, `y`, `p`) are fully rendered.
- **Slot Reservation for Tab Row Alignment**: When no tagline is provided (e.g. Notes screen), the exact tagline slot height is reserved so tabs on Home and Notes maintain identical vertical baselines.
- **Verification Across Font Scales**: Tested against 0.85x, 1.0x, 1.3x, 1.5x, and 2.0x font scales.

### Feature 3: Two-Stage Workspace Tab Hold Gesture
- **Stage 1 (Long-Press)**: After system long-press timeout, tab elevates with haptic feedback and action panel opens; releasing finger leaves panel open until an action is selected.
- **Stage 2 (Move Mode)**: Continuous hold for additional ~600ms triggers stronger haptic, fades out panel (~120ms), scales tab to 1.1x with elevation, and enters direct move mode without drag slop.
- **Pure State Machine**: `WorkspaceTabGestureStateMachine` with injected clock `() -> Long` covering `Idle`, `Pressed`, `PanelOpen`, `MoveMode`, and `Dragging`.

### Feature 4: Remote Asset & Release Notes Image Rendering
- **Release Banner Image**: `UpdateBanner` accepts background image matching release asset pattern `banner.*` with content scale crop behind dark gradient scrim; falls back smoothly to 1.6.0 gradient on load failure or missing asset.
- **Markdown & HTML Image Syntax**: Shared `ReleaseNotesRenderer` supports standalone `![alt](url)`, linked `[![alt](url)](link)`, and `<img src=... alt=... width=...>` with 12dp rounded corners, 320dp max height, and 16:9 placeholder.
- **Security & Memory Guard**: HTTPS only on `github.com` and `*.githubusercontent.com`, rejecting non-allowlisted domains; 5MB download limit; downsampling to screen width; ~25MB disk cache.

## UI Mockup
```
Screen Header Layout (Home & Notes):
+-------------------------------------------------------------+
| [Fotara]                                         (O)   (O)  |  <- Title (22-38sp, descenders unclipped)
| [Your notes, organized]                                     |  <- Tagline slot (measured, reserved on Notes)
+-------------------------------------------------------------+
| [ Home ]  [ Archive ]  [ Custom Workspace ]  [ + ]          |  <- Tab Row at identical baseline
+-------------------------------------------------------------+

Workspace Tab Long-Press Stages:
Touch Down -> [400ms] -> Stage 1 (Panel Opens, Tab Elevates)
                      -> Release -> Panel Stays Open
                      -> Drag    -> Panel Hides, Drag Reorders
Touch Down -> [400ms + 600ms] -> Stage 2 (MOVE MODE, Stronger Haptic, Tab 1.1x, Drag Immediate)
```

## Logic Notes
- In `CanvasDrawingView`, debounce `tileCacheManager.invalidateAll()` on pan/zoom by 120ms.
- In `PhotoDrawingEditor`, remove `userScale` and `panOffset` from `pointerInput` keys; use `rememberUpdatedState` or read current values inside gesture coroutines.
- In `ScreenHeader`, remove outer fixed `.height(94.dp)` from `HomeScreen.kt`; calculate header height naturally from content with `wrapContentHeight()`.
- In `UpdateBanner`, parse release assets list for entries starting with `banner` and ending with `.png`, `.jpg`, `.jpeg`, `.webp`, `.gif`.

## Risks & Mitigations
- *Risk*: Heavy QuadTree spatial queries during pan causing frame drops.
  *Mitigation*: Retain and scale existing raster viewport during gestures; defer spatial queries until settle.
- *Risk*: Font scaling breaks header layout on small screens.
  *Mitigation*: Test programmatic Compose UI layouts across font scales 0.85x through 2.0x.
- *Risk*: Malicious remote image URLs in release notes.
  *Mitigation*: Strict HTTPS host allowlist limited to official GitHub repository domains.

## Dependencies
- Phase 31 (Release 1.7.0) codebase and assets.
- Android SDK 36, Kotlin 2.0, Compose 1.7.

## Acceptance Criteria
- Zoom and pan across Canvas and Photo Drawing operate smoothly without gesture cancellation or recomposition loops.
- Home screen title and tagline are fully visible without clipping across all font scales (0.85x to 2.0x).
- Workspace tab long-press supports Stage 1 (panel hold) and Stage 2 (direct move mode) verified by pure unit tests.
- Release banner and notes images render correctly with fallback scrim and HTTPS host security restrictions.
- All unit tests pass; release APK `Fotara_1.7.1.apk` built and validated.
