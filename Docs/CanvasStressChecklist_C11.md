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
