# Phase 21 - CanvasStackingAndOverlayLayout

## Goal
Establish a unified canvas element stacking hierarchy where layer order and per-layer insertion sequence govern strokes and images identically. Provide proportional scaling and single-axis stretching for images with auto-selection on insertion. Fix canvas tool options panel heights to wrap within available bounds and scroll safely. Standardize floating bottom navigation bar overlay inset calculation across tab screens to prevent snackbars and action buttons from being occluded. Correct popup anchor positioning so top-bar overflow menus open aligned beneath their trigger buttons.

## Scope
- Unified element stacking rule: Layer order first (highest layer on top); inside a layer, creation order decides (newest on top). Strokes and images treated identically across live drawing, committed rendering, tile caching, layer blend isolation, PNG exports, hit-testing, duplication, layer moves, eraser splits, and undo/redo.
- Deterministic database reload ordering by `z_index ASC, rowid ASC`.
- Per-layer next `z_index` assignment (`max(z_index in layer) + 1`).
- Default image insertion scaled to ~60% of visible viewport (clamped to canvas extent), centered in viewport, auto-selected with Select tool activated in a single undo step.
- Specialized image-only transform controls: aspect-ratio locked corner scaling with opposite corner fixed, single-axis side stretching with opposite side fixed, center rotation, drag-to-move, 44dp hit targets, 24dp min size clamp, no mirroring.
- Tool options panels (Highlighter, Pen, Eraser) wrapping content with maximum height bounded between top capsules and bottom dock, scrollable with `Modifier.verticalScroll`, inset-safe (status bar and display cutout). Highlighter blending chips fixed at 48dp in a single row with visible helper text.
- Shared runtime source of truth for measured floating bottom navigation bar height (`LocalBottomOverlayPadding` / state holder with `onSizeChanged`), consumed by list padding, `SnackbarHost`, and floating action buttons across Home, Notes, and Settings.
- Dropdown overflow menu anchoring directly to the trigger `IconButton` (right-aligned under trigger, flipping upward when space is limited, clamped within 8dp screen margins).

## Out Of Scope
- Reorder controls (bring forward / send backward stay removed; cross-layer order is adjusted via the layers panel).
- Tap-to-cycle or alternative selection modes.
- Photo drawing, group support for PDF/DOCX, workspaces (reserved for future phases).
- Database schema changes or StrokeCodec binary format modifications.
- Modifications to `ViewportTransform.kt`, FTS tables, or `fotara.fileprovider`.
- Batch 2 features for release 1.5.7.

## Features
### Element Stacking Uniformity
- Elements on higher layers render above elements on lower layers.
- Within a single layer, elements render strictly in ascending `zIndex` sequence. Strokes and images share the same layer coordinate space and `zIndex` progression without any hardcoded element-type priority.
- Newly drawn strokes and newly imported images receive `nextZIndexForLayer = (layerElements.maxOfOrNull { it.zIndex } ?: 0) + 1`.
- Duplicating elements assigns sequential new `zIndex` values atop the active layer.
- Moving elements to another layer assigns sequential `zIndex` values atop the target layer while preserving relative element order.
- Split stroke pieces resulting from lasso selection or partial erasing preserve the original stroke's `zIndex`.
- Hit testing (`selectTap`) tests elements descending by layer order first, then descending by element `zIndex`, skipping hidden or locked layers.
- Rotated images are hit-tested using local coordinate transformation to ensure precise selection regardless of rotation angle.
- Exporting to PNG (full canvas or selection) and tile caching render in exact layer and element order.

### Image Insertion & Transform Handling
- When an image is inserted, it is sized such that its longest dimension occupies 60% of the visible viewport (clamped to maximum canvas bounds 20,000 x 20,000), centered at current viewport center.
- Image insertion automatically switches the active canvas tool to `Select` and selects the new image, displaying oriented bounding box and handles immediately.
- The entire insertion and selection transition is packaged as a single atomic undo/redo step.
- Transform interactions when selection contains ONLY images:
  - Corner handles (0, 2, 4, 6): Proportional scaling preserving original image aspect ratio, pivoting around the diagonally opposite corner.
  - Side handles (1, 3, 5, 7): 1D directional stretch along the dragged axis (horizontal or vertical), pivoting around the opposite side midpoint.
  - Rotation handle: Rotates image around its center point.
  - Inside box drag: Translates image across canvas.
- Selections containing mixed elements or strokes retain 1.5.6 free corner stretch behavior.
- Handles follow rotated image orientation with 44dp hit targets and minimum 24dp dimension clamp.

### Responsive Tool Options Panels
- Highlighter, Pen, and Eraser popup panels calculate maximum allowable height based on screen height minus top capsule bounds and bottom dock bounds.
- Outer container applies `Modifier.heightIn(max = calculatedMaxHeight)` and `Modifier.verticalScroll(rememberScrollState())`.
- Insets from status bar and display cutout are respected to avoid overlapping system indicators.
- Highlighter blending mode chips are constrained to a fixed 48dp height in a single row; the explanatory helper caption remains visible directly beneath.

### Unified Bottom Overlay Clearance
- Provide `LocalBottomOverlayPadding` supplying dynamic DP padding derived from runtime measurement of the floating bottom navigation bar.
- `StorageSettingsScreen` and other settings detail screens place `SnackbarHost` above the navigation bar clearance.
- `NotesScreen` floating action button (+) and snackbar host apply bottom overlay clearance.
- Adjusts automatically when soft keyboard (IME) is displayed, adhering to `WindowInsets.ime`.

### Anchor-Aligned Overflow Menus
- Top-bar three-dot overflow menus (e.g., Notes tab, Home screen, Folder screen) nest `DropdownMenu` directly inside the bounding container of the triggering `IconButton`.
- Dropdown aligns to the right edge of the trigger button, opens beneath it, flips upward when screen bottom space is insufficient, and maintains at least 8dp padding from screen edges.

## UI Mockup
### Canvas Tool Options Panel (Highlighter)
```
+-------------------------------------------------------+
| Status Bar Inset Area                                 |
+-------------------------------------------------------+
| [Back]   [Undo] [Redo]            [Layers] [Export]   |  <- Top Capsules
+-------------------------------------------------------+
|                                                       |
|       +---------------------------------------+       |
|       | Highlighter Options               [X] |       |  <- Bounded Height
|       | Stroke Width                          |       |
|       | [====O==============================] |       |
|       | Quick Sizes: [XS] [S] [M] [L] [XL]   |       |
|       | Colors: (O) (O) (O) (O) (O) (O)       |       |
|       | Blending Mode                         |       |
|       | [Normal] [Multiply] [Darken] [Screen] |       |  <- 48dp Single Row
|       | Blends with content on the same layer |       |  <- Helper text visible
|       +---------------------------------------+       |
|                                                       |
+-------------------------------------------------------+
| [Pen] [Highlighter] [Eraser] [Lasso] [Select] [Image] |  <- Bottom Dock
+-------------------------------------------------------+
```

### Bottom Overlay Clearance (Notes / Storage)
```
+-------------------------------------------------------+
| Screen Content / List                                 |
|                                                       |
|                        [ (+) FAB ]                    |  <- Sits ABOVE bar
|   +-----------------------------------------------+   |
|   | Notification / Snackbar message               |   |  <- Sits ABOVE bar
|   +-----------------------------------------------+   |
|        +-------------------------------------+        |
|        |  [Home]      [Notes]     [Settings] |        |  <- Floating Nav Bar
|        +-------------------------------------+        |
+-------------------------------------------------------+
```

### Top-Right Overflow Menu Anchoring
```
+-------------------------------------------------------+
| Notes                                         [(+)] [⋮] | <- Trigger Button
|                                               +-----+ |
|                                               |Open | | <- Right-aligned
|                                               |Move | |    directly under
|                                               |Trash| |    trigger [⋮]
|                                               +-----+ |
+-------------------------------------------------------+
```

## Logic Notes
1. **Stacking Order Determinism**:
   - Element comparison key: `(layerOrder, element.zIndex, rowid)`.
   - SQLite query: `SELECT ... FROM canvas_elements WHERE document_id = ? ORDER BY z_index ASC, rowid ASC`.
   - `nextZIndexForLayer(doc, layerId) = (doc.elements.filter { it.layerId == layerId }.maxOfOrNull { it.zIndex } ?: 0) + 1`.
2. **Proportional Transform Geometry**:
   - For an image at angle $\theta$, convert touch coordinates to unrotated bounding box space.
   - When dragging corner $i$, opposite corner $j = (i + 4) \pmod 8$ is fixed anchor $A$.
   - Scale factor: maintain aspect ratio $w_0 / h_0$ such that $w_{new} / w_0 = h_{new} / h_0 = \max(\Delta x / w_0, \Delta y / h_0)$ relative to anchor.
   - For side handles, stretch is 1-dimensional along normal axis with opposite side midpoint fixed.
3. **Bottom Inset State Propagation**:
   - `FloatingNavBar` measures height via `Modifier.onSizeChanged { size -> navBarHeightDp = with(density) { size.height.toDp() } }`.
   - `LocalBottomOverlayPadding` provides this value to descendant composables.

## Risks
- *Risk*: Existing documents may have arbitrary or tied `zIndex` values.
  - *Mitigation*: Deterministic secondary sort by `rowid ASC` guarantees stable loading matching original insertion order without mutating persisted rows.
- *Risk*: Extreme image aspect ratios could collapse when constrained to 60% viewport.
  - *Mitigation*: Longest side scales to 60% viewport while maintaining aspect ratio, clamped to minimum 48px and maximum 20,000px canvas bounds.
- *Risk*: Window insets variance on devices with hardware navigation keys or gesture pill.
  - *Mitigation*: Combine measured composable height with `WindowInsets.navigationBars` and `WindowInsets.ime`.

## Dependencies
- Phase 18 (CanvasSelectionAndEraser)
- Phase 19 (CanvasHighlighterAndScrollEdges)
- Phase 20 (PullToRefreshAndExportPresets)

## Acceptance Criteria
- [x] Layer order strictly precedes element creation order; inside a layer, older elements are below newer elements.
- [x] Drawing strokes after inserting an image renders strokes ON TOP of the image on the same layer.
- [x] Reloading a document from SQLite produces 100% deterministic stacking order.
- [x] Inserting an image sizes it to ~60% of visible viewport, centers it, activates the Select tool, and shows selection handles in a single undo step.
- [x] Image-only selection corner handles scale proportionally with opposite corner fixed; side handles stretch 1-axis with opposite side fixed.
- [x] Rotated images are accurately hit-tested and handles follow image rotation.
- [x] Highlighter, Pen, and Eraser tool option panels wrap content, scroll when constrained, and never overlap top capsules or bottom dock.
- [x] Highlighter blending chips render in a single row with 48dp fixed height and visible helper text.
- [x] Floating navigation bar height is measured dynamically and clears snackbars on Storage & Data screen and the (+) FAB on Notes tab.
- [x] Top-right three-dot overflow menus open anchored beneath their trigger buttons, right-aligned, within screen margins.
