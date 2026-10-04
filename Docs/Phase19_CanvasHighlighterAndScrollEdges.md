# Phase 19 - CanvasHighlighterAndScrollEdges

## Goal
Deliver Batch 2 of Fotara release 1.5.6: resolve translucent highlighter rendering across live preview, committed strokes, tile caching, and PNG exports; introduce layer-isolated blend modes (NORMAL, MULTIPLY, DARKEN, SCREEN) with binary StrokeCodec V2 storage; provide long-press tool panels with haptic feedback and blending selection; reduce the bottom canvas dock height by ~20% with shared anchor positioning; and eliminate Home screen scroll edge defects (hidden last card behind bottom navigation dock and hard cutoff lines beneath the segmented tab bar).

## Scope
- Task 0: Carry-over audit from Batch 1:
  - Density-aware 4dp remnant/absorption threshold conversion (`(4.0f * density) / zoom`).
  - Consolidation of `TransformHandlesMathTest.kt` into `canvas/engine`.
  - Dynamic derivation of Zone Z8 selection action bar vertical offset from measured top capsule height and status bar insets.
- Task 1: Translucent highlighter in all rendering phases (live, committed, tile cache, export) using shared alpha constant `HIGHLIGHTER_BASE_ALPHA = 90` (multiplied by `layer.opacity`); single path rendering preventing self-overlap darkening; custom chisel-tip vector drawable `ic_highlighter.xml` replacing palette icon.
- Task 2: Layer-isolated blend modes for highlighter:
  - `StrokeBlendMode` enum (NORMAL, MULTIPLY, DARKEN, SCREEN).
  - Default MULTIPLY for new highlighter strokes, NORMAL for pen.
  - `StrokeCodec` format version `FORMAT_VERSION_V2 = 0x02` with 1-byte blend mode encoding and backward-compatible V1 decoding as NORMAL.
  - Layer-isolated rendering via `canvas.saveLayer()` only when non-NORMAL strokes exist on the layer, composited via `SRC_OVER` at `layer.opacity`. Direct fast path preserved for all-NORMAL layers.
  - API 29+ `android.graphics.BlendMode` with safe fallback to `PorterDuff.Mode.DARKEN` on API 24–28 to prevent transparency modulation dropouts.
  - Preservation of blend mode across lasso split, eraser, duplicate, move to layer, transform, and undo/redo.
- Task 3: Long-press tool panels for Pen and Highlighter:
  - Long-press on dock icon triggers haptic feedback, activates tool, and opens Z4 options popup.
  - Highlighter popup displays "Blending" row with 4 single-select chips (min 44dp hit target) and localized muted hint.
- Task 4: Shorter bottom dock:
  - ~20% height reduction via tighter vertical padding and a compact collapse handle.
  - Dynamically measured height or shared anchor calculations for Z4 popup and Z7 zoom chip.
  - Guaranteed clearance across gesture and 3-button navigation insets in expanded and collapsed states.
- Task 5: Home screen scroll edges:
  - Reusable `Modifier.verticalEdgeFade(topFade, bottomFade)` using offscreen compositing and `BlendMode.DstIn`.
  - Runtime measured bottom content padding for `LazyVerticalGrid` accounting for FloatingDock, HomeBottomNavBar, spacing, and system bar insets.
  - Smooth 20dp top fade under the segmented tab bar with matched top content padding.
  - Alignment for NotesScreen list and detail grids.

## Out Of Scope
- Canvas eraser or selection changes (completed in Batch 1).
- Batch 3 features (layer panel redesign, export options, advanced gestures).
- Database schema changes (DATABASE_VERSION remains 14; payload changes strictly inside `canvas_elements.data_chunk`).
- Changes to `ViewportTransform.kt`, FTS tables, or `fotara.fileprovider`.

## Features
### Translucent Highlighter
- Live preview, committed render, tile cache render, and PNG export all use `HIGHLIGHTER_BASE_ALPHA = 90` (~35%) multiplied by `layer.opacity`.
- Single-path rendering ensures a continuous stroke overlapping itself does not multiply its own alpha.
- New `res/drawable/ic_highlighter.xml` chisel-tip marker icon replaces the palette icon in the dock.

### Layer-Isolated Blend Modes
- Blending applies strictly within the same layer's content. Content of other layers and canvas background patterns are never read or altered.
- MULTIPLY: Darkens by multiplying color channels. On empty layer space, displays true color like NORMAL.
- DARKEN: Retains the darker pixel value.
- SCREEN: Lightens by inverted multiplication.
- API 24–28 fallback: Uses `PorterDuff.Mode.DARKEN` for MULTIPLY to avoid alpha-squared transparent vanishing.

### Long-Press Tool Panels & Blending Row
- Pen & Highlighter icons support combined click (select) and long-click (select + open Z4 popup + haptic feedback).
- Highlighter options include Blending chip group: Normal, Multiply, Darken, Screen.

### Shorter Canvas Dock & Dynamic Anchoring
- Dock height reduced by ~20% with 44dp touch target compliance.
- Z4 and Z7 padding dynamically bound to measured dock height.

### Edge-Faded Scroll Viewport & Dynamic Bottom Insets
- Home folder grid content padding dynamically measures bottom stack height (search dock + bottom navigation + system navigation bar + IME).
- Top edge applies `Modifier.verticalEdgeFade(top = 20.dp)` with 20dp top padding, eliminating hard clipped lines under the segmented tab bar.

## UI Mockup
```
================ Canvas Dock (Z3) ===============
[ ^ Collapse ]                                     <- Compact 14dp handle
(Select) (Pen) (Highlighter [Chisel]) (Eraser) (O) <- 40dp buttons / 44dp targets
==================================================

============= Z4 Highlighter Popup ===============
[ Size Slider                O================ ]
[ Palette                    ( ) ( ) ( ) ( )   ]
[ Blending                   [Normal] [Multiply*] [Darken] [Screen] ]
  "Blends with content on the same layer"
==================================================

============= Home Screen Top Edge ===============
[ All | Favorit | Arsip ] (Segmented Tab Bar)
~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~  <- 20dp alpha fade (DstIn)
[ Folder Card 1 ]        [ Folder Card 2 ]
...
[ Last Folder Card ]     (Fully visible above dock)
--------------------------------------------------
[ Floating Search Dock (+) ]
[ Home | Notes | Settings ] (Bottom Nav Bar)
==================================================
```

## Logic Notes
- **StrokeCodec V2**: Header byte `0x02` followed by tool ordinal byte, then blend mode ordinal byte. `decode()` checks version: `0x01` decodes as `StrokeBlendMode.NORMAL`; `0x02` reads blend mode.
- **Layer Isolation Predicate**: A layer requires `saveLayer` only if `layer.elements.any { it is StrokeElement && it.blendMode != StrokeBlendMode.NORMAL }` or the live in-progress stroke on that layer is non-NORMAL. Otherwise, direct canvas draw is used with zero extra offscreen buffers.
- **API Version Compatibility**: On API < 29, `PorterDuff.Mode.MULTIPLY` produces `[Sa*Da, Sc*Dc]`, causing strokes on transparent layer surfaces to vanish (`Da=0 => 0`). Falling back to `PorterDuff.Mode.DARKEN` produces `Sc` when `Da=0`, ensuring the stroke remains visible on transparent backgrounds while darkening overlapping content.
- **Scroll Edge Fade Math**: Offscreen compositing strategy applies `BlendMode.DstIn` over a vertical gradient (`Color.Transparent` -> `Color.Black` over `topFadePx`). Draw phase execution guarantees zero recomposition during scrolling.

## Risks
- *Risk*: `canvas.saveLayer()` causes GPU memory pressure if invoked on full viewport every frame.
  *Mitigation*: Gate `saveLayer()` strictly behind `hasBlendedStrokes` check. Pure NORMAL layers use direct hardware-accelerated canvas drawing with zero overhead.
- *Risk*: Floating-point precision issues with dp-to-world conversion across different screen densities.
  *Mitigation*: Multiply `4.0f` by `context.resources.displayMetrics.density` before dividing by `viewport.scale`.

## Dependencies
- Phase 18 (Batch 1 Selection and Free Eraser).

## Acceptance Criteria
- [x] Remnant/absorption threshold ("4dp") converts real display density and zoom.
- [x] `TransformHandlesMathTest.kt` consolidated into a single package location.
- [x] Zone Z8 selection action bar offset dynamically clears measured top capsules in all orientations.
- [x] Highlighter is translucent (`alpha = 90`) during live preview, committed rendering, tile caching, and PNG export.
- [x] Single-path highlighter drawing prevents self-overlap darkening.
- [x] New custom chisel-tip vector drawable `ic_highlighter.xml` used in dock.
- [x] `StrokeElement` supports `StrokeBlendMode` (NORMAL, MULTIPLY, DARKEN, SCREEN).
- [x] `StrokeCodec` V2 writes 1-byte blend mode; V1 decodes as NORMAL; golden-byte tests pass.
- [x] Blending is layer-isolated (`saveLayer`) and leaves other layers and canvas background untouched.
- [x] API 24–28 handles MULTIPLY safely without transparent dropout.
- [x] Long-press on Pen and Highlighter triggers haptic feedback and opens Z4 popup.
- [x] Highlighter Z4 popup contains Blending row with 4 chips and localized description.
- [x] Canvas dock height reduced by ~20%; Z4 and Z7 dynamically follow measured dock height.
- [x] Home screen last card scrolls fully above bottom dock, displaying note count.
- [x] Home screen top edge fades smoothly under tab bar without hard clipping lines.
