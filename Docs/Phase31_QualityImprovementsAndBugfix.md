<!-- --- Arinara Network (c) 2026 --- -->
<!-- Exclusive property of Arinara Network. -->
<!-- Unauthorized use, reproduction, distribution, or modification of this code, -->
<!-- in whole or in part, for any purpose, is strictly prohibited without prior -->
<!-- written consent from Arinara Network as sole legal owner of this codebase. -->

# Phase 31 - QualityImprovementsAndBugfix

## Goal
Eliminate sudden layout shifts, viewport jumping, and item/thumbnail blinking across all selection modes in Fotara (Home folder multi-select, Folder Detail batch select, and subfolder multi-select). Formalize Release 1.7.0 "Quality improvements and bugfix" as the umbrella release for Batches 1, 2, and 3.

## Scope
- Task 6: Selection Mode Jumping and Blinking:
  - Home folder select mode (long-press and 3-dot menu entry, toggle, select all, invert, tab change, scroll).
  - Folder detail batch select mode (photos, groups, PDF, DOCX, text notes, canvas cards).
  - Folder detail subfolder multi-select mode (subfolder chips).
  - Fixed-height overlay header architecture with zero viewport shift upon entering/leaving select mode.
  - Zero-shift bottom action dock overlay with stable content padding.
  - Item-level recomposition via snapshot state tracking (only toggled item and counter recompose).
  - Stable Coil image requests with memory cache pinning to eliminate thumbnail flashing.
  - Compose UI tests (androidTest) and unit tests for scroll and selection state stability.
- Task 7: Release Name 1.7.0:
  - Transition in-development release from 1.6.1 to 1.7.0 "Quality improvements and bugfix".
  - Create `/Changelog/Changelog_1.7.md` adhering to Rules.md Section 16 moderate versioning.
  - Add explanatory note in `/Changelog/Changelog_1.6.md` clarifying 1.6.1 was never released.
  - Update `/Docs/Progress.md` single source of truth.
  - Record 1.7.0 release title metadata hook for downstream What's New screen feeding.

## Out Of Scope
- No changes to `versionName` or `versionCode` in this batch (Batch 1).
- No new third-party dependencies or Room database schema migrations.
- No release build packaging (debug APK verification only).
- Canvas drawing layer lasso tool alterations (only card grid selection modes are in scope).

## Features
### Selection Mode Viewport Stability & Overlay Architecture
- **Behavior**: Entering, operating, and exiting selection mode preserves exact scroll position and layout metrics across all screens.
- **Top Bar / Header**: In Home, the header slot maintains an invariant height matching `ScreenHeader`. The multi-select bar renders in-place or as an overlay without modifying `Scaffold.innerPadding.calculateTopPadding()`. In Folder Detail, the top bar container color remains consistently dark navy (`MidnightNavy`) and animates title/actions without structural destruction.
- **Bottom Action Dock**: The bottom action dock in Folder Detail renders as an aligned overlay with a fixed, stable content padding in the underlying grid (`LazyVerticalGrid`), eliminating viewport height collapse and expansion when items are selected or deselected.
- **Item-Level Snapshot State Tracking**: `SnapshotStateMap` or per-item snapshot state decouples individual card selection states from the parent grid layout. Toggling an item invalidates only that item and the counter text.
- **Flicker-Free Thumbnail Caching**: `AsyncImage` calls use remembered `ImageRequest` instances with `.crossfade(false)` and dedicated memory/disk cache keys.

## UI Mockup
```
+-------------------------------------------------------------+
| Home Screen - Invariant Header Slot (Height: ~92dp)         |
| Mode A (Normal): [Fotara (38sp)]               [Search] [:] |
| Mode B (Select): [X] [ 3 Selected ]    [Link] [Rename] [All]|
| (Transition via graphicsLayer alpha crossfade, 0dp jump)   |
+-------------------------------------------------------------+
| Workspace Tabs: [ Home ] [ Study ] [ Personal ] [+]         |
+-------------------------------------------------------------+
| Folder Grid (Stable Viewport & Constant Content Padding)    |
| +-------------------------+     +-------------------------+ |
| | [Folder Icon]           |     | [Folder Icon]           | |
| | Biology                 |     | Calculus                | |
| | 12 notes                |     | 8 notes                 | |
| | (Selected Border 2.5dp) |     | (Normal Border 1dp)     | |
| +-------------------------+     +-------------------------+ |
| ...                                                         |
|                                                             |
| +---------------------------------------------------------+ |
| | Floating Dock / Nav (Stable bottom padding reserved)     | |
| +---------------------------------------------------------+ |
+-------------------------------------------------------------+
```

## Logic Notes
- **Home Selection State**: `HomeViewModel` exposes a reactive `SnapshotStateMap<Long, Boolean>` (`selectedFolderMap`) and `isMultiSelectModeFlow`. Toggling updates the map directly so Compose triggers recomposition exclusively on the item matching the key and on derived count readers.
- **Folder Detail Selection State**: `FolderDetailViewModel` maintains item-specific snapshot maps for photos, groups, documents, text notes, and canvas notes. Grid container composables skip recomposition; item cards consume their individual selection boolean.
- **Workspace Tab Switch**: Switching workspaces while in select mode immediately clears the selection map and exits select mode, preserving the invariant that folders cannot be cross-selected across disparate workspaces.
- **Subfolder Tab Switch**: Selecting a subfolder tab during batch select mode updates the active grid items without altering grid viewport padding.

## Recomposition Analysis
- **Before Fix (Baseline)**:
  - Single item selection toggle: Grid recompositions = 1 (entire grid recomposes).
  - Visible item recompositions: N (all N visible cards recompose due to whole-state re-emission and lambda recreation).
  - Counter recompositions: 1.
  - Image requests re-evaluated: N.
- **After Fix (Targeted)**:
  - Single item selection toggle: Grid recompositions = 0 (grid section completely skipped).
  - Non-toggled item recompositions: 0 (completely skipped).
  - Toggled item recompositions: 1.
  - Counter recompositions: 1.
  - Image requests re-evaluated: 0 (cached, no crossfade flash).

## Risks
- *Risk*: `SnapshotStateMap` mutation might desynchronize with existing `_uiState` properties expected by legacy unit tests.
  - *Mitigation*: Bidirectional synchronization ensuring `_uiState.value.selectedFolderIds` mirrors `selectedFolderMap` while UI composables bind to the snapshot map.
- *Risk*: Static bottom padding in Folder Detail could cause excessive empty space when dock is hidden.
  - *Mitigation*: Set grid bottom content padding to standard breathing room (16dp + insets + dock height) so items comfortably scroll above the dock when scrolled to end, while dock overlay slides in over the reserved breathing zone without shifting the grid.

## Dependencies
- Phase 27 & 28 (Workspaces Foundation and Migration).
- Phase 29 (Unified ScreenHeader & UI Consistency).

## Acceptance Criteria
- [x] Entering select mode via long-press or menu item retains identical scroll position and visible item index (zero pixel jump).
- [x] Selecting/deselecting items in Home and Folder Detail recomposes only the toggled card and the counter.
- [x] Action dock in Folder Detail appears and disappears via graphicsLayer overlay without resizing the grid viewport or shifting item positions.
- [x] Thumbnails in photos, groups, documents, and canvas cards do not blink or trigger crossfade animations on selection toggle.
- [x] "Select All" and "Invert Selection" execute smoothly without scroll jumping or visual artifacts.
- [x] Changing workspace tab on Home clears selection while preserving multi-select mode as per existing rules.
- [x] Changelog for 1.7.0 established at `/Changelog/Changelog_1.7.md`, with note in `/Changelog/Changelog_1.6.md`.
- [x] Compose UI tests in `androidTest` and logic tests in `test` validate zero scroll shift and selection invariants.
