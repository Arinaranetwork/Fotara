// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

# Phase 56 - CoreExperienceRepairs

## Goal
Resolve critical usability, drawing, audio, space isolation, feedback submission, and math editor defects to restore a flawless core experience without regression.

## Scope
- Repair PDF Voice / Audio annotations: Fix `PdfViewerScreen` layout so the audio dock is properly positioned and visible rather than pushed off-screen, wire page-anchored audio recording from page options, and ensure recordings map to the intended page.
- Fix drawing curve distortion and unwanted shape auto-correction in PDF editor and canvas: Eliminate Catmull-Rom endpoint modulo wrapping in `ShapeAutoCorrectEngine.kt` that generates unnatural joints and curved hooks on open strokes; restrict shape snapping to intentional draw-and-hold gestures so normal pen strokes are not mutated.
- Make the Academic Space feature genuine and persistent: Filter workspaces and folders by active space ID in `WorkspaceRepository.kt`, remove mock/hardcoded descriptions in `SpaceSwitcherBottomSheet.kt`, and display real workspace/folder statistics.
- Fix feedback submission HTTP 400 error (`PGRST204`): Prevent in-place mutation of the remote JSON payload with local-queue `"synced"` property in `FeedbackManager.kt`.
- Remove obstructive Inline Math Preview popup in `TextNoteEditorScreen.kt`: Eliminate the floating card covering user text while typing LaTeX equations.
- Execute automated tests to guarantee stability without assembling a release build.

## Out Of Scope
- Release APK build assembly or publication (explicitly excluded by owner directive).
- New feature additions outside the reported defect scope.

## Features

### 1. Functional PDF Voice Annotations
- **Behavior**: In `PdfViewerScreen.kt`, wrap the page viewport and audio dock in a properly weighted layout (`Modifier.weight(1f)` for the document viewport) so the dock is docked at the bottom with navigation bar insets and visible on demand.
- **Page Menu Integration**: Add "Add Voice Note" in the 3-dot page menu so users can record directly for any individual page.
- **Target Page Mapping**: Anchor recordings to the selected page filter or visible page index.

### 2. Natural Curve Smoothing & Explicit Shape Snapping
- **Bézier Smoothing Correction**: In `ShapeAutoCorrectEngine.kt` (`smoothPointsBezier`), clamp endpoint tangent guide points for open strokes (`isClosed == false`) instead of wrapping modulo `count`.
- **Pen Stroke Preservation**: In `PdfPageEditorScreen.kt` and `CanvasToolController.kt`, never trigger `recognizeAndSnap` on standard pen finger lifts unless `isShapeSnapped` was triggered by a sustained draw-and-hold gesture.

### 3. Real Multi-Vault Space Isolation
- **Data Scoping**: In `WorkspaceRepository.kt`, filter workspaces by `space_id` for the currently active space. When a new space is created, provision its default Home and Archive workspaces.
- **Genuine UI Representation**: In `SpaceSwitcherBottomSheet.kt`, replace hardcoded mock descriptions with dynamic statistics (e.g. workspace count and folder count).

### 4. Clean Feedback Submission Payload
- **Payload Integrity**: In `FeedbackManager.kt`, ensure `saveToLocalQueue` does not mutate the `JSONObject` payload sent over the network, and strip any non-schema properties prior to transmission.

### 5. Unobstructed Text Note Math Typing
- **Floating Card Removal**: In `TextNoteEditorScreen.kt`, remove the intrusive floating `Inline Math Preview` surface that blocks user typing.

## UI Mockup
```
┌────────────────────────────────────────────────────────┐
│ [←] PDF Viewer                           [🎤] [⋮]      │
├────────────────────────────────────────────────────────┤
│                                                        │
│   [ Page 1 Viewport ]                                  │
│   ...                                                  │
│                                                        │
├────────────────────────────────────────────────────────┤
│ [ Audio Notes (1) ]                                [X] │
│ [ 🎤 Record Audio ]                                    │
│ [ ▶ 00:15 / 00:45 - Page 1                  [🗑] ]     │
└────────────────────────────────────────────────────────┘
```

## Logic Notes
- `ShapeAutoCorrectEngine.smoothPointsBezier`: For open curves, `p0 = controlPoints[maxOf(0, i - 1)]`, `p3 = controlPoints[minOf(count - 1, i + 2)]`.
- `WorkspaceRepository`: Observe `activeSpaceId` from `SpaceRepository` or pass `spaceId` into workspace queries.
- `FeedbackManager`: Deep-copy or sanitize payload before serialization.

## Risks
- *Risk*: Modifying workspace queries could cause legacy workspaces with `space_id = 0` or `null` to become invisible.
  - *Mitigation*: Ensure legacy workspaces default to `space_id = 1` (Default Space).

## Dependencies
- Phase 44 (STEM note & drawing auto-correct engine).
- Phase 45 (PDF audio annotations).
- Phase 46 (Space super-hierarchy).

## Acceptance Criteria
- PDF Audio dock appears on screen and records/plays audio without clipping or disappearing.
- Drawing open lines leaves clean, natural ends without hooks, joints, or unprompted shape snapping.
- Switching spaces isolates workspaces and folders; mock descriptions are replaced with live statistics.
- Feedback form submits cleanly without `PGRST204` schema cache errors.
- Text note math editing does not produce an obstructive card over user input.
- Unit tests pass with 0 failures; release APK is NOT assembled.
