<!-- --- Arinara Network (c) 2026 --- -->
<!-- Exclusive property of Arinara Network. -->
<!-- Unauthorized use, reproduction, distribution, or modification of this code, -->
<!-- in whole or in part, for any purpose, is strictly prohibited without prior -->
<!-- written consent from Arinara Network as sole legal owner of this codebase. -->

# Unlimited Canvas Stress & Performance Checklist (C11)

This document specifies the heavy-load and boundary stress testing procedures for the Infinite Canvas engine, to be executed on physical Android hardware and high-refresh-rate emulators during Chunk C11.

---

## 1. Test Targets and Thresholds

| Metric | Target Specification | Failure Boundary |
| :--- | :--- | :--- |
| **In-Progress Stroke Draw Latency** | < 16 ms (120Hz digitizers: < 8.3 ms) | > 24 ms or visible stroke trailing |
| **Tile Cache Memory Cap** | Strictly bounded to 32 MB | OutOfMemoryError or heap > 120 MB |
| **Document Load Time (20,000 Strokes)** | Progressive chunked render < 1.2s | Main-thread ANR (> 5s freeze) |
| **Pan / Zoom Frame Rate (Heavy Doc)** | Consistent 60 fps / 120 fps | Frame drops (< 45 fps) or hitching |
| **Zero Per-Frame Allocations** | 0 bytes GC allocation per draw frame | Repeated GC pauses (> 5ms) |

---

## 2. Manual and Automated Stress Matrix (C11)

| Test ID | Scenario | Procedure | Expected Verification | Pass / Fail |
| :--- | :--- | :--- | :--- | :--- |
| **STR-01** | 20,000 Strokes Heavy Load | Ingest a synthetic benchmark document containing 20,000 valid strokes distributed across 10,000 x 10,000 world units. | Progressive chunked loader streams document in 500-stroke batches without blocking main thread. Memory remains capped within 32MB tile budget. | [ ] |
| **STR-02** | 10 Layers Visibility & Opacity | Create a document with 10 stacked layers. Populate each layer with 1,000 strokes. Toggle layer visibility and opacity (0.1f to 1.0f). | Spatial culling skips hidden layers instantly. Semi-transparent layers composite correctly without memory reallocation. | [ ] |
| **STR-03** | Multiple 4K Downsampled Images | Place 6 high-resolution (4000x3000) image assets on the canvas. Scale and rotate concurrently. | `CanvasAssetManager` downsamples to 2048px maximum dimension. Bitmaps render smoothly without UI freeze or heap exhaustion. | [ ] |
| **STR-04** | Rapid Mid-Stroke 2-Finger Interruption | Begin a rapid 1-finger stroke. While the finger is dragging, abruptly land a 2nd finger on the screen. | In-progress stroke is cancelled immediately without committing broken fragments to document history. Viewport seamlessly switches to smooth two-finger pan/zoom. | [ ] |
| **STR-05** | Stylus Palm Rejection Stress | Rest full palm on the screen (> 60px contact radius) while drawing fine lines with S-Pen / stylus. | Palm touch is rejected cleanly without creating stray dots or disrupting active stroke path. | [ ] |
| **STR-06** | Rapid Area Eraser Splitting | Create a complex cross-hatched grid of 50 long strokes. Perform continuous area erasing zig-zags across the intersection points. | Intersected strokes split into surviving segments without crashing or dropping points. Undo command cleanly restores original strokes. | [ ] |
| **STR-07** | Extreme Viewport Zoom Out (0.05x) | Pinch zoom out to minimum scale (0.05x) over a 20,000 stroke drawing. | Background grid adapts display spacing without looping over thousands of lines. Viewport culling aggregates visible elements without crashing. | [ ] |
| **STR-08** | Extreme Viewport Zoom In (50.0x) | Pinch zoom in to maximum scale (50.0x) on a single stroke dot. | Canvas scale clamps cleanly at 50.0f without NaN or matrix inversion errors. Touch precision remains sub-pixel accurate. | [ ] |
| **STR-09** | Configuration Change Mid-Stroke | Rotate device 90 degrees while an in-progress stroke is active. | Activity recreation cleanly drops uncommitted stroke. Document state and tile cache restore cleanly without dangling pointers. | [ ] |
| **STR-10** | Continuous 100-Step Undo/Redo | Draw 100 consecutive strokes. Rapidly tap Undo 100 times, then Redo 100 times. | All 100 strokes unroll and replay cleanly. Model state matches initial state with zero memory leaks. | [ ] |

---

## 3. C10 UI and Integration Device Verification Matrix (C11)

| Test ID | Feature & Zone | Procedure | Expected Verification | Pass / Fail |
| :--- | :--- | :--- | :--- | :--- |
| **DEV-01** | **Z1-Z8 Zone Map Layout & Insets** | Open canvas on phone (portrait & landscape) and tablet. Observe all 8 zones. | Z1 (top-left title/save state/Alpha tag) and Z2 (top-right toolbar) do not clip status bar. Z3 (bottom dock) and Z7 (bottom-left zoom chip) sit cleanly above navigation bar without overlapping. Floating Z8 contextual bar remains within display margins. | [ ] |
| **DEV-02** | **C10-B Creation Entry Point & Alpha Notice** | In `FolderDetailScreen`, tap floating add button. Tap 'New Canvas (Alpha)'. | 'New Canvas (Alpha)' is listed adjacent to 'New Text Note'. First launch displays experimental Alpha notice explaining infinite canvas capabilities. Confirming opens blank canvas and flags notice as seen. | [ ] |
| **DEV-03** | **C10-C Layers Panel (Z5)** | Open Z5 layers panel via Z2 button. Add 3 layers, rename layer 2, reorder layer 3 to top, toggle visibility and lock, adjust opacity slider to 50%. | Canvas updates immediately upon every change. Tapping undo rolls back reorder, rename, or deletion. Active drawing tool draws strictly onto currently selected layer. Locked layer rejects strokes. | [ ] |
| **DEV-04** | **C10-D Image Import & Centering** | Tap Add Image in Z2. Choose an image from gallery, and another from existing Fotara photo note. | Chosen photo is copied to `filesDir/canvas_assets`, EXIF rotation is corrected, downsampled if >2048px, and placed at center of current visible viewport as a selectable element. User can draw over image. | [ ] |
| **DEV-05** | **C10-E PNG Export & Share Flow** | Draw content. Open Z2 Export dialog. Select 1x scale, then 2x scale. Tap Export and Share. | Content bounds are calculated accurately. Huge canvases are scaled down to 4096px cap to prevent OOM. Export PNG is written to app cache and Android share sheet is launched via `FileProvider`. | [ ] |
| **DEV-06** | **C10-A / C10-C Z8 Contextual Bar** | Select element in Z6. Tap duplicate, bring forward, send backward, delete, and move to layer. | Contextual bar floats adjacent to element bounds. Duplicate offsets clone by +30,+30. Move to layer reassigns element layerId with full undoability. | [ ] |
| **DEV-07** | **C10-A Z7 Zoom Chip & Fit-to-Content** | Pan far away and pinch zoom to 240%. Tap Z7 percentage chip. Tap fit-to-content. | Tapping zoom chip animates scale smoothly back to 100%. Tapping fit-to-content computes bounding box of all strokes and centers viewport. | [ ] |
| **DEV-08** | **C10-A Z4 Tool Options Flyout** | In Z3, tap selected Pen tool again. Flyout pops up. Change stroke size, alpha, and color palette. | Flyout anchors above bottom toolbar. New color appears in recent colors list. Stroke adjustments apply immediately to subsequent drawing. | [ ] |
| **DEV-09** | **C10-F Save-State & Debounced Autosave** | Draw rapid strokes and watch Z1 save indicator. Disconnect or test error state. | Indicator switches from 'Saved' to 'Saving...' during drawing and returns to 'Saved' after debounce save completes. Title tap opens rename dialog and updates document immediately. | [ ] |
| **DEV-10** | **C10-G Folder Privacy Lock & Trash Integration** | Place canvas in PIN-locked folder. Search for canvas from Home screen search bar. | Tapping canvas search result prompts for folder PIN / biometrics before opening canvas note. Deleting canvas moves it to Trash; restoring brings it back; purging removes asset files permanently. | [ ] |
| **DEV-11** | **C10-G Schedule & Deadline Integration** | In Z2 overflow, tap Schedule note. Select reminder time. | NoteScheduleManager registers notification alarm. Notification fires at scheduled time with note title and opens directly to canvas. | [ ] |
| **DEV-12** | **C10-F Canvas Settings Persistence** | In Z2 overflow, toggle Stylus-only mode and Palm rejection. Exit and reopen canvas. | Settings remain persisted in app preferences. Stylus-only mode ignores finger drawing and allows two-finger panning only. | [ ] |

