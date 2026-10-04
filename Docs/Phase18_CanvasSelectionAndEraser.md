<!-- --- Arinara Network (c) 2026 --- -->
<!-- Exclusive property of Arinara Network. -->
<!-- Unauthorized use, reproduction, distribution, or modification of this code, -->
<!-- in whole or in part, for any purpose, is strictly prohibited without prior -->
<!-- written consent from Arinara Network as sole legal owner of this codebase. -->

# Phase 18 - Canvas Selection and Eraser

## Goal
Implement non-destructive free-form lasso selection (selecting only the enclosed parts of strokes with non-zero winding and lazy splitting), full transform handle manipulation (move, anchor-fixed stretch, rotation, oriented bounding box), unified selection action bar with instant visibility and partial deletion/duplication, and a dedicated capsule-sweep free eraser with a single undo step per gesture for Fotara 1.5.6 (Batch 1).

## Scope
- **Task 1: Free-form lasso selection**:
  - Non-zero winding number polygon enclosure test for self-crossing loops.
  - Partial stroke cutting at exact boundary intersection points with non-destructive reference modeling (`CanvasSelection`, `SelectedElementReference.PartialStroke`).
  - Whole element selection for fully enclosed strokes, single-point dots, and images whose center falls inside.
  - Short remainder absorption rule: outside fragments $< 4\text{dp} / \text{scale}$ adjacent to inside segments are absorbed to prevent specks.
  - Selection bounding box computed exclusively from selected portions with half-width padding.
- **Task 2: Selection action bar & partial delete**:
  - Z8 contextual bar actions: Duplicate, Move to layer, Delete (trash red icon). Remove Bring Forward and Send Backward.
  - Visibility: active when Select tool is active OR selection is non-empty. Actions dimmed and disabled when selection is empty.
  - Immediate appearance upon tap or lasso completion via `onSelectionChanged` reactive binding.
  - Partial delete: materializes split, removes inside portions, preserves outside remainders with original attributes in one undo step.
  - Duplicate: duplicates selected portions with offset without mutating or splitting originals in one undo step.
  - Move to layer: moves selected portions to target layer in one undo step.
  - Selection cleared after undo/redo.
- **Task 3: Select tool: move, stretch, rotate**:
  - Root cause investigation and resolution documented with code evidence.
  - Handle hit-testing (44dp touch target) before lasso initiation; fallback to inside-box drag (-1) or new lasso outside.
  - When box $< 88\text{dp}$, only corner and rotation handles remain active.
  - Corner handles stretch freely with opposite corner fixed; side handles stretch along one axis with opposite side fixed; rotation handle pivots around selection center.
  - Clamp minimum size to 24dp screen equivalent; disable negative scale / mirroring.
  - Stroke width remains constant during stretch/rotate; images scale and rotate proportionally.
  - Oriented bounding box retaining rotation angle.
  - Gesture preview using transform matrix against original points captured at gesture start; single commit at gesture end as `TransformElementsCommand`. Second finger cancels transform.
- **Task 4: Free eraser**:
  - Eliminate stroke-eraser mode and dock mode toggle. Eraser tool is exclusively the free eraser (area eraser).
  - Custom vector drawable icon (`res/drawable/ic_eraser.xml`) replacing the broom icon.
  - Plain tap activates Eraser tool; long-press opens Z4 options popup with size slider only.
  - Capsule sweep between consecutive touch points (including historical points) preventing gaps during fast drags.
  - Remnant rule: surviving pieces shorter than $\max(\text{stroke.width}, 4\text{dp-equivalent})$ are dropped.
  - Single undo step per continuous drag gesture (`AreaEraseCommand`).
- **Task 5: Artifact-free guarantee and regression hardening**:
  - Deselect leaves document identical.
  - TileCacheManager invalidation for old and new bounds on all mutations.
  - NaN/Infinity coordinate sanitization and zero-length fragment rejection.
  - Smooth continuous joints at split points.

## Out Of Scope
- Database schema changes (v14 remains unchanged).
- `StrokeCodec` binary format modifications.
- Touching `ViewportTransform.kt`, SQLite FTS virtual tables, or `fotara.fileprovider`.
- Pen, Highlighter, or color-swatch modifications.
- Batches 2 and 3 features.

---

## Root Cause Analysis

### 1. Task 3: Inability to Move, Stretch, or Rotate Selection
- **Defect**: The selection box and handles (8 resize handles + 1 rotation stem) rendered on screen, but touches were never recognized as handle or body drags.
- **Evidence & Findings**:
  1. `PointerStateMachine.kt` (lines 205–211): In `handleDown`, when `activeMode == ActiveMode.SELECT`, single-finger touches unconditionally set `state = PointerState.Selecting(...)`. It never checked for active selection handles or bounding box hits.
  2. Orphaned State Machine Method: `startTransform(pointerId, handleId, screenX, screenY)` in `PointerStateMachine.kt` (lines 505–512) was never invoked anywhere in the codebase.
  3. Orphaned Tool Controller Method: `hitTestHandles(screenX, screenY, selectionBounds, viewport)` in `CanvasToolController.kt` (lines 272–315) had 0 callers across the application.
  4. Insufficient Touch Radius & Degenerate Bounds: Handle touch radius was hardcoded to `24.0f` (effectively 8–10dp on xxhdpi devices), far below the accessible 44dp target. For straight lines or dots, bounds had 0 width or height, causing `screenX in sLeft..sRight && screenY in sTop..sBottom` to evaluate false.
  5. Touch Slop Conflict: In `PointerState.Selecting`, any touch displacement exceeding 16dp was treated as a lasso gesture. Because handle hits were bypassed, dragging a handle immediately drew a lasso across the selection.
  6. Accumulative Per-Move Transformation: `applyTransformDelta` was called on each `ACTION_MOVE`, pushing a new `TransformElementsCommand` per touch event into `historyManager` and accumulatively corrupting geometry instead of previewing and committing once on `ACTION_UP`. Furthermore, corner/edge handles merely translated elements rather than scaling them.

### 2. Task 2: Action Bar Delayed Appearance Until Tool Switch
- **Defect**: The Z8 selection action bar did not appear immediately after drawing a lasso or tapping an element; it only appeared after switching to the Pen tool.
- **Evidence & Findings**:
  1. Unwired Selection Callback: In `CanvasDrawingView.kt` (lines 218 & 228), `PointerAction.TapSelect` and `PointerAction.LassoComplete` updated `toolController.toolState = toolController.toolState.copy(selectedElementIds = selectedIds)`. However, `CanvasDrawingView` had no `onSelectionChanged` callback.
  2. Delayed UI State Sync: `CanvasViewModel`'s `_uiState` only refreshed its `toolState` property inside `setTool(...)` (line 453). Therefore, Compose's `uiState.toolState.selectedElementIds` remained empty while the user stayed in the Select tool. Tapping the Pen tool in the bottom dock invoked `viewModel.setTool(PEN)`, which updated `_uiState.toolState` with the previously stored `selectedElementIds`, causing Z8 to suddenly pop up.

### 3. Task 4: Old Eraser Behavior with Images
- **Verification**: In `CanvasToolController.kt` (lines 160–167), `eraseAt` explicitly filtered candidates with `document.elements.filterIsInstance<StrokeElement>()`. Neither stroke eraser nor area eraser ever interacted with `ImageElement`. In the free eraser, images remain strictly immune to erasing.

---

## Architectural & Logic Specifications

### 1. Non-Zero Winding Lasso Enclosure
For a lasso polygon $V_0, V_1, \dots, V_n$ ($V_n = V_0$) and point $P = (x, y)$:
$$wn = 0$$
For each edge $V_i \rightarrow V_{i+1}$:
- If $V_i.y \le y$:
  - If $V_{i+1}.y > y$ and $\text{isLeft}(V_i, V_{i+1}, P) > 0$, increment $wn$.
- Else ($V_i.y > y$):
  - If $V_{i+1}.y \le y$ and $\text{isLeft}(V_i, V_{i+1}, P) < 0$, decrement $wn$.

Where $\text{isLeft}(A, B, P) = (B.x - A.x)(P.y - A.y) - (P.x - A.x)(B.y - A.y)$.
A point is inside if and only if $wn \neq 0$. Self-crossing loops enclose all regions with non-zero winding.

### 2. Non-Destructive Lazy Stroke Slicing
When a stroke intersects the lasso polygon:
1. Each segment $P_k \rightarrow P_{k+1}$ is tested for intersections with all polygon edges.
2. Intersections are sorted along the segment by $t \in [0, 1]$. Midpoints of sub-intervals are evaluated via $wn \neq 0$.
3. Inside sub-intervals are aggregated into contiguous `StrokeInsideSegment`s with linearly interpolated boundary coordinates and pressures.
4. Outside fragments shorter than $4\text{dp} / \text{scale}$ adjacent to inside segments are absorbed.
5. The original `CanvasDocument` is left untouched. The selection stores a `SelectedElementReference.PartialStroke`.
6. Materialization occurs only when a mutating command (Delete, Transform, MoveToLayer) executes:
   - Untouched outside portions are preserved with identical IDs or generated sub-IDs, maintaining color, width, tool, layer, and zIndex.
   - Inside portions are deleted or transformed as required, grouped into a single atomic undo command.

### 3. Handle Hit-Testing & Transform Math
- Handle 8 (Rotation stem): centered $28\text{dp}$ above top-center of oriented box; hit radius $\ge 22\text{dp}$ (44dp target).
- Handles 0..7 (Corners & Edges): hit radius $\ge 22\text{dp}$. If oriented box width or height $< 88\text{dp}$, handles 1, 3, 5, 7 are suppressed.
- Inside oriented box (-1): coordinates transformed into local box frame with $16\text{dp}$ padding for thin lines.
- Transform Preview: `transformMatrix` initialized at gesture start from original points. Live preview renders transformed points; commit occurs only on `ACTION_UP` as a single `TransformElementsCommand`.
- Clamp: minimum dimension clamped to $24\text{dp} / \text{scale}$; stroke widths stay constant.

### 4. Capsule Sweep Free Eraser
- For consecutive points $A$ and $B$, the erase zone is the capsule with radius $R_{\text{total}} = R_{\text{eraser}} / \text{scale} + \text{stroke.width} / 2$.
- Distance from point $P$ to segment $AB$:
  $$t = \text{clamp}\left(\frac{(P - A) \cdot (B - A)}{\|B - A\|^2}, 0, 1\right), \quad \text{dist} = \|P - (A + t(B - A))\|$$
- Points with $\text{dist} \le R_{\text{total}}$ are erased; segment transitions are interpolated at exact boundary radius.
- Surviving segments with total length $< \max(\text{stroke.width}, 4\text{dp} / \text{scale})$ are discarded.
- In-memory preview updates during drag; exactly one `AreaEraseCommand` commits upon gesture finish (`ACTION_UP`).

---

## Acceptance Criteria
- [x] Free-form lasso selects only enclosed portions of strokes; self-crossing loops select full enclosed area.
- [x] Deselection leaves the document identical with zero leftover fragments or phantom elements.
- [x] Selection action bar appears immediately upon selection without requiring a tool switch.
- [x] Action bar contains Duplicate, Move to layer, and Delete (red). Bring Forward / Send Backward removed.
- [x] Partial delete removes only inside portions; outside remainders remain intact; single undo step restores original strokes.
- [x] Duplicate copies only selected portions with offset; originals stay untouched; single undo step.
- [x] Dragging inside selection box moves selection; corner handles stretch with opposite corner fixed; side handles stretch along one axis; rotation handle pivots around center.
- [x] Stroke thickness does not change during stretch or rotate; minimum size clamped to 24dp; no mirroring.
- [x] Real eraser vector icon in dock; long-press opens size slider; plain tap selects tool.
- [x] Eraser capsule sweep prevents gaps during fast drags; surviving segments below remnant threshold are removed; one undo step per drag.
- [x] All coordinates finite (NaN/Infinity checked); 100% unit tests passing.
