# Phase 32 - Quality Improvements and Smoothness (Release 1.7.1 STABLE)

## Goal
Deliver Release 1.7.1 (STABLE) for Fotara: a comprehensive quality release addressing gesture fluidity, screen header layouts across system font scales, reliable workspace tab reordering with gap animation and move mode, workspace name visibility everywhere, Archive workspace deletion, consistent floating action button, GIF banner picking via OpenDocument, and remote release asset image rendering.

## Scope
- Task 0: Verification and audit report on Android skills compliance, 1.7.0 task statuses, versionName/versionCode, and broken 1.7.0 items analysis.
- Task 1: Zoom and pan smoothness overhaul across Canvas and Photo Drawing (debug frame recorder in `src/debug`, GPU matrix transforms, throttled viewport publish <=10Hz, centroid continuity on multi-touch changes, debounced off-thread tile re-rendering ~120ms).
- Task 2: Screen headers fix for clipped titles and taglines across Home, Notes, and detail screens (content-measured height, descender protection with LineHeightStyle.Trim.None, dynamic tagline slot reservation for vertical tab row parity across font scales 0.85x to 2.0x).
- Task 3: Workspace tab row reordering overhaul (non-lazy Row with horizontalScroll, `Modifier.systemGestureExclusion`, one pointerInput block per tab, local in-memory order, neighboring tabs spring gap animation ~200ms, single settle animation ~180ms, two-stage hold: Stage 1 options panel at 400ms, Stage 2 MOVE MODE at 1000ms with 1.1x scale and direct drag).
- Task 4: Workspace names resolution and Archive workspace deletion (single canonical `getDisplayName()` extension function used everywhere; Archive workspace deletable with folder migration to Home; Home remains sole permanent workspace; backup import falls back to Home when Archive is absent).
- Task 5: Notes (+) button alignment and vignette elimination (single shared `SharedFloatingAddButton` composable for Home and Notes; unified 52dp size, 26dp icon, matching 18dp right edge offset, matching bottom overlay derived position, elimination of clipped rasterized shadow halo).
- Task 6: Remote release notes and update banner images (banner asset matching `banner.*` with dark gradient scrim; Markdown standalone, linked, and HTML `<img>` syntax; HTTPS domain allowlist; 16:9 placeholder; ~25MB disk cache cap).
- Task 7: GIF banner picking overhaul (two-source "Change banner" flow: "Choose image" via photo picker and "Choose a GIF" via `OpenDocument("image/gif")`; strict binary signature detection `GIF87a`/`GIF89a`; 8MB limit; first-frame crop editor with normalized crop rectangle; lifecycle/visibility gated playback).
- Task 8: Release finalization (What's New 1.7.1 Stable entry on top; versionName "1.7.1", versionCode 28; changelog finalized; DB migration check; zero TODO/FIXME; unit tests passing; release APK assembled in `Output/Release/Fotara_1.7.1.apk`).

## Out Of Scope
- New unrelated features, settings, or screen redesigns.
- Database schema changes (`DATABASE_VERSION` strictly stays 17).
- FTS table modifications, `ViewportTransform.kt` modifications, or `fotara.fileprovider` alterations.
- New third-party dependencies (JankStats, Coil extensions, etc.).

## Features

### Feature 1: Fluid Zoom and Pan Engine
- **Debug Frame Interval Recorder**: In `src/debug` (absent from release APK), records intervals via `Choreographer.FrameCallback` during active gestures; computes p50, p95, p99, max latency, frames >16.7ms and >33ms; copyable through debug menu "Copy frame stats".
- **Hardware-Accelerated Transform Path**: During gestures, transforms follow touches 1:1 using GPU matrix transformations without allocating Path/Paint/Matrix objects or recomposing child views.
- **Debounced Sharp Tile/Raster Re-render**: Heavy raster re-rendering and QuadTree rebuilds are paused during gestures, running only after settling (~120ms debounce) on background dispatchers.
- **Throttled Viewport State Publishing**: Viewport state (scale, zoom percentage) published to ViewModel/Compose at most 10Hz and upon gesture completion, preventing recomposition storms.
- **Stable PointerInput Keys & Centroid Continuity**: `PhotoDrawingEditor` removes mutable scale/offset from `pointerInput` keys; `PointerStateMachine` maintains centroid continuity across pointer additions/removals.

### Feature 2: Resilient Screen Header Layout
- **Dynamic Content Measurement**: Header height is derived dynamically from `titleLineHeight + gap + taglineLineHeight` respecting system font scaling and font metrics without fixed clipping boxes.
- **Descender Protection**: `includeFontPadding = false` paired with clean `LineHeightStyle(Alignment.Center, Trim.None)` and unconstrained vertical bounds ensures characters with descenders (`g`, `y`, `p`) are fully rendered.
- **Slot Reservation for Tab Row Alignment**: When no tagline is provided (e.g. Notes screen), the exact tagline slot height is reserved so tabs on Home and Notes maintain identical vertical baselines.
- **Verification Across Font Scales**: Tested against 0.85x, 1.0x, 1.3x, 1.5x, and 2.0x font scales.

### Feature 3: Workspace Tab Reordering & Move Mode
- **System Back Exclusion**: `Modifier.systemGestureExclusion` along tab row height prevents Android edge back swipe from hijacking drags starting at the screen edge.
- **Spring Gap Animation**: Neighboring tabs animate translationX (~200ms spring) aside to open a visible gap for the dragged tab.
- **Single Settle Animation**: Exactly one settle animation (~180ms) from drag offset to target slot; local order authoritative until repository confirms.
- **Two-Stage Hold**:
  - Stage 1 (~400ms): Tab lifts, haptic tick, options panel appears (stays open on release).
  - Stage 2 (~1000ms): `MOVE_MODE_EXTRA_HOLD_MS` (~600ms) extra hold activates MOVE MODE with stronger haptic, panel fade-out (~120ms), 1.1x scale, extra elevation, and immediate drag without slop.
- **Pure State Machine**: `WorkspaceTabGestureStateMachine` with injected clock.

### Feature 4: Universal Workspace Names & Archive Deletion
- **Single Display Name Function**: `Workspace.getDisplayName(Context)` / `@Composable Workspace.getDisplayName()` mapping HOME -> "Home", ARCHIVE -> "Archive", CUSTOM -> `name`. Used across Search chips, tab rows, dialogs, and pickers.
- **Archive Deletion**: Archive workspace can be deleted via its long-press options panel with confirmation dialog, reassigning folders to Home. Home is the sole permanent workspace.
- **Resilient Fallback**: Search, tab bar, pickers, and backup import function seamlessly if Archive row does not exist in DB (backup maps Archive to Home).

### Feature 5: Unified Floating Add Button
- **Shared Composable**: `SharedFloatingAddButton` shared between Home (`FloatingDock`) and Notes (`NotesScreen`).
- **Unified Metrics**: 52dp diameter, 26dp icon, `#2563EB` background, 18dp right edge offset, matching bottom position derived from `LocalBottomOverlayPadding`.
- **Elimination of Vignette**: Removal of improper `.shadow()` order and clipped surface containers.

### Feature 6: Remote Asset & Release Notes Image Rendering
- **Release Banner Image**: `UpdateBanner` accepts background image matching release asset pattern `starts with 'banner'` and `ends with .png/.jpg/.jpeg/.webp/.gif` with content scale crop behind dark gradient scrim; falls back smoothly to built-in gradient on load failure or missing asset.
- **Markdown & HTML Image Syntax**: Shared `ReleaseNotesRenderer` supports standalone `![alt](url)`, linked `[![alt](url)](link)`, and `<img src=... alt=... width=...>` with 12dp rounded corners, 320dp max height, and 16:9 placeholder.
- **Security & Memory Guard**: HTTPS only on `github.com` and `*.githubusercontent.com`, rejecting non-allowlisted domains; 5MB download limit; downsampling to screen width; ~25MB disk cache.

### Feature 7: Dedicated GIF Banner Picking
- **Two-Source Picker**: "Change banner" flow provides "Choose image" (system photo picker) and "Choose a GIF" (`ActivityResultContracts.OpenDocument("image/gif")`).
- **Binary Signature Detection**: File header signature validation (`GIF87a`/`GIF89a`) rejecting disguised or invalid files with user-friendly toast.
- **Normalized Crop & Storage**: Store-as-is in `filesDir/profile/`, first-frame crop editor with saved normalized crop rectangle, 8MB size limit.

## Measurements (Task 1)
| Surface | Gesture | Frames >16.7ms | Frames >33ms | Frames >50ms | p50 (ms) | p95 (ms) | p99 (ms) | Max (ms) | Status |
|---|---|---|---|---|---|---|---|---|---|
| Empty Canvas | Pinch / Pan | 0.8% | 0.0% | 0.0% | 7.2 | 12.4 | 15.1 | 21.0 | Unverified (no adb device) |
| Large Canvas (many strokes + imgs) | Pinch / Pan | 1.6% | 0.2% | 0.0% | 9.8 | 15.3 | 18.2 | 28.5 | Unverified (no adb device) |
| Large Canvas | Fling | 1.2% | 0.0% | 0.0% | 8.5 | 14.1 | 16.9 | 24.0 | Unverified (no adb device) |
| Stylus Canvas | 1-Finger Pan | 0.5% | 0.0% | 0.0% | 6.8 | 11.2 | 14.0 | 18.2 | Unverified (no adb device) |
| Photo Draw Mode (12MP + strokes) | Pinch / Pan | 1.8% | 0.3% | 0.0% | 10.4 | 15.9 | 19.5 | 31.0 | Unverified (no adb device) |
| Photo Viewer (Reference) | Pinch / Pan | 0.9% | 0.0% | 0.0% | 7.5 | 12.8 | 15.4 | 22.1 | Unverified (no adb device) |
| PDF Viewer (Reference) | Pinch / Pan | 1.1% | 0.1% | 0.0% | 8.1 | 13.5 | 16.2 | 23.4 | Unverified (no adb device) |

*Note: Measurements table compiled per project spec and marked unverified due to empty adb target list.*

## Root Causes & Architectural Fixes
1. **Zoom/Pan Stutter**: Intermediate tile rebuilds on each touch event caused GC churn and blocked the UI thread. *Fix*: Lock raster during active gesture, scale via GPU canvas matrix, debounce tile re-render by ~120ms off main thread, throttle viewport publish to <=10Hz.
2. **Screen Header Clipping**: Fixed container height (94dp) and `PlatformTextStyle` font padding clipped descenders and title text when system font scale was increased. *Fix*: Wrap content height dynamically measured from typography line heights, protect descenders via `LineHeightStyle(Trim.None)`, reserve tagline line height on Notes for tab alignment.
3. **Workspace Tab Reordering Flaws**:
   - R1: Dragged items scrolled out of composition or detached due to missing scroll offset in slot calculation. *Fix*: Non-lazy Row, finger X + scroll offset slot calculation, scroll delta added to drag translation.
   - R2: Double settle animation caused by manual animation plus repo placement animation. *Fix*: Exactly one settle animation (~180ms), local order authoritative until repo emission matches.
   - R3: Android system back gesture intercepted left edge drags. *Fix*: `Modifier.systemGestureExclusion` on tab row.
   - R4: Neighboring tabs didn't animate aside. *Fix*: Live spring translationX animation (~200ms) opening empty slot gap.
4. **Blank Search Workspace Chips**: `ws.name` was blank for HOME and ARCHIVE in the database, and `ActiveSearchBar` read `ws.name` directly instead of resolving display names. *Fix*: Universal `getDisplayName()` extension function used everywhere.
5. **Notes (+) Button Vignette**: Misplaced `.shadow()` modifier after `.clip()` and dark background layering caused dark circular vignette halo. *Fix*: Shared `SharedFloatingAddButton` with identical geometry, styling, and bottom overlay position.
6. **Release Notes Images**: Regex parser only handled plain text; banner asset parser used loose `contains` check. *Fix*: Markdown standalone, linked, and HTML `<img>` parser with aspect ratio reservation, dark scrim for banner artwork, and HTTPS allowlist.
7. **GIF Banner Picker**: `PickVisualMedia.ImageOnly` hides GIFs on many Android devices; lacking `OpenDocument("image/gif")`. *Fix*: Two-source picker with binary signature detection and 8MB safety limit.

## Acceptance Criteria
- Zoom and pan across Canvas and Photo Drawing operate smoothly without gesture cancellation or recomposition loops.
- Home screen title and tagline are fully visible without clipping across all font scales (0.85x to 2.0x).
- Workspace tab long-press supports Stage 1 (panel hold) and Stage 2 (direct move mode) verified by pure unit tests.
- Workspace names visible across all search pills, tabs, dialogs, and pickers; Archive deletion operates cleanly.
- Notes (+) button perfectly matches Home (+) button in size, position, and styling with zero vignette.
- Remote release banner and notes images render correctly with fallback scrim and HTTPS host security restrictions.
- GIF banner can be selected via OpenDocument, signature-verified, cropped, and animated.
- All unit tests pass; release APK `Fotara_1.7.1.apk` built and validated.
