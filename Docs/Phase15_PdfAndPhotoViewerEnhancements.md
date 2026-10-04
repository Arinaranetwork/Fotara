<!-- --- Arinara Network (c) 2026 --- -->
<!-- Exclusive property of Arinara Network. -->
<!-- Unauthorized use, reproduction, distribution, or modification of this code, -->
<!-- in whole or in part, for any purpose, is strictly prohibited without prior -->
<!-- written consent from Arinara Network as sole legal owner of this codebase. -->

# Phase 15 - PDF and Photo Viewer Enhancements (Release 1.5.5)

## Goal
Resolve zoom gesture defects in the PDF and photo viewers: restrict PDF zoom gestures exclusively to reading mode, eliminate jagged/laggy pinch zoom via 1:1 GPU-layer transformation and gesture-cycle preservation, and relocate viewer subtitles into a dedicated non-clickable "Info" header section in the three-dot overflow menus.

## Scope
- Task 1: Reading Mode Zoom Gating (PDF Viewer):
  - Reading mode toggle via top-bar book icon (`Icons.Default.AutoStories`).
  - Outside reading mode: pinch-to-zoom, double-tap zoom, and zoomed panning are fully detached at the gesture-handler level. Page stays at default fit scale. Normal vertical scrolling between and within pages operates without gesture interception.
  - Inside reading mode: pinch, double-tap, and single-finger pan work.
  - Entering reading mode starts at default scale. Leaving reading mode while zoomed resets scale to default.
  - Standalone photo viewer preserves its existing pinch/double-tap zoom.
- Task 2: Smooth 1:1 GPU-Layer Zoom:
  - Eliminate gesture cancellation caused by `scale` being passed as key to `pointerInput(scale)`.
  - Eliminate recomposition churn by transitioning from composition-level `Modifier.graphicsLayer(...)` to lambda `Modifier.graphicsLayer { ... }`.
  - Implement exact focal-point anchored zoom math: `(1 - zoom) * (centroid - center - offset)`.
  - Re-render PDF pages at 2.0x sharper resolution only after the gesture settles (debounced), keeping the base bitmap visible with zero blank flash.
  - Animate only on double-tap zoom and reset (`tween(250)`), never while fingers are down during active pinch.
  - Apply identical optimizations across `PdfViewerScreen`, `PdfViewportZoomState`, and `ZoomableBox` / `ZoomablePhotoViewport`.
- Task 3: Info Section in Three-Dot Overflow Menu:
  - Remove subtitle lines from top bars in both photo viewer and PDF viewer, leaving clean single-line ellipsized titles vertically aligned with icons.
  - Add muted, non-clickable "Info" section at the top of each viewer's three-dot menu followed by a subtle divider before actionable menu items.
  - Photo viewer: displays "Pinch or double-tap to zoom" and "100% Offline".
  - PDF viewer: dynamically displays state-accurate info:
    - Reading mode OFF: "Zoom is available in reading mode" and "100% Offline".
    - Reading mode ON (unzoomed): "Pinch or double-tap to zoom" and "100% Offline".
    - Zoomed: "Zoomed: Pan enabled · Double-tap to reset".
  - All user-facing strings centralized in `strings.xml`.

## Out Of Scope
- Changing toolbar icon buttons or layout orders.
- Altering core PDF page rendering engine or OCR pipeline.
- Modifying unrelated screens (Notes, Settings, Home, Canvas).

## Features
### Reading Mode Gesture Gating
- `isReadingMode: Boolean` state toggled by the book icon button.
- Clean icon tint feedback (`AccentGold` when active, `TabCream` when inactive).
- Detached gesture modifier when `!isReadingMode`, preventing gesture detector creation or pointer consumption.

### 1:1 Compositing GPU Zoom Engine
- Zero recomposition during gestures using `Modifier.graphicsLayer { ... }`.
- Stable pointer input without coroutine restart cycles during pinch.
- Clamped pan bounds preventing content from leaving the viewport.
- Asynchronous high-res sharpening on Dispatchers.IO with 32MB LruCache and no blank frames.

### Three-Dot Menu Info Section
- Non-clickable muted header `Info` (`TextSecondary` / `TabCream.copy(alpha = 0.5f)`).
- Secondary muted text describing current gestures and offline capability.
- Clean horizontal divider separating info from actions (e.g. `Schedule...`).

## UI Mockup
```
Top Bar (Single Line Title):
+----------------------------------------------------+
| (<-)  LKM BIOLOGY 2026...     [Book] (Split) (Share) (:) |
+----------------------------------------------------+

Three-Dot Menu (PDF Viewer - Reading Mode OFF):
+--------------------------------------+
| INFO                                 |
| Zoom is available in reading mode    |
| 100% Offline                         |
|--------------------------------------|
| [Alarm] Schedule...                  |
+--------------------------------------+

Three-Dot Menu (Photo Viewer):
+--------------------------------------+
| INFO                                 |
| Pinch or double-tap to zoom          |
| 100% Offline                         |
|--------------------------------------|
| [Alarm] Schedule...                  |
+--------------------------------------+
```

## Logic Notes
- `PdfViewportZoomState`:
  - `animateDoubleTap(tapX, tapY)`: uses `Animatable` with `tween(250)` for smooth zoom/reset transitions.
  - `animateReset()`: resets scale to 1.0f and pan to (0, 0).
  - `onPinch(zoom, panX, panY, centroidX, centroidY)`: direct 1:1 mathematical update without animation delay.
- `ZoomableBox`:
  - Single `pointerInput(Unit)` listening for 2-finger pinch and 1-finger pan when zoomed.
  - Single `pointerInput(Unit)` for double-tap animation.
  - `graphicsLayer { ... }` isolates changes to draw phase.

## Risks
- Gesture conflicts between single-finger scroll and two-finger pinch -> Mitigation: Pointer count gating so single finger is unconsumed when scale <= 1.01f.
- Memory spikes during rapid zoom in multi-page PDF -> Mitigation: 32MB LruCache and debounced 150ms settling before 2.0x render.

## Dependencies
- `PdfPageRenderer`, `PdfLayoutMath`, `ZoomableBox`, `PhotoViewerDialog`, `PdfViewerScreen`.

## Acceptance Criteria
- [x] Outside reading mode in PDF viewer: pinch, double-tap, and pan are completely disabled; normal list scroll is unaffected.
- [x] Tapping book icon toggles reading mode ON/OFF; entering starts at default scale; leaving while zoomed resets scale.
- [x] Inside reading mode: pinch tracks 1:1 anchored at centroid, double-tap smoothly animates zoom, single finger pans when zoomed.
- [x] Zero recomposition lag during pinch zoom; `graphicsLayer { ... }` used in both PDF viewer and photo viewer.
- [x] Top bar subtitles removed from both photo viewer and PDF viewer, titles vertically centered on a single ellipsized line.
- [x] Three-dot menu in photo viewer displays Info section ("Pinch or double-tap to zoom" and "100% Offline").
- [x] Three-dot menu in PDF viewer dynamically displays accurate Info section depending on reading mode and zoom state.
- [x] All user-facing strings localized in `strings.xml`.
- [x] 100% unit tests pass with zero regressions.
