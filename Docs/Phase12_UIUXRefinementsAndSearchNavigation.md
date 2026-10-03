<!-- --- Arinara Network (c) 2026 --- -->
<!-- Exclusive property of Arinara Network. -->
<!-- Unauthorized use, reproduction, distribution, or modification of this code, -->
<!-- in whole or in part, for any purpose, is strictly prohibited without prior -->
<!-- written consent from Arinara Network as sole legal owner of this codebase. -->

# Phase 12 - UIUXRefinementsAndSearchNavigation

## Goal
Execute a focused UI/UX refinement pass addressing four key structural and behavioral areas: (1) Reorganizing the Settings screen into logical main section cards on root with clean, un-carded list rows in detail views; (2) Restoring full vertical viewport height on the Home screen by eliminating artificial padding/spacers while properly respecting system navigation bars; (3) Fixing narrow-column vertical text wrapping in the photo viewer top toolbar; and (4) Delivering a deterministic search-to-folder navigation flow with programmatic auto-scroll and a subtle 2-second exposure/brightness highlight.

## Scope
- Reorganize `SettingsScreen.kt` root view into 6 logical main section rounded cards (General, Appearance, OCR & Recognition, Notifications & Deadlines, Storage & Data Management, About & Legal).
- Implement clean, card-less settings list inside each opened section (clean rows, icons, labels, descriptions, dividers, toggles, chevrons).
- Correct Home screen (`HomeScreen.kt`) Scaffold, insets, and bottom stack layout: remove duplicate WindowInsets consumption and excessive bottom spacers, letting content use the full vertical application area while keeping system navigation bar unobstructed.
- Refactor photo/note viewer header toolbar in `PhotoViewerDialog.kt`: split into a title/action row and a dedicated full-width horizontal subtitle/status row so instructional text never wraps into a narrow column.
- Implement end-to-end Search -> Folder Navigation with automatic scrolling and 2-second brightness/exposure highlight across all note types (Photos, Groups, Documents, Text Notes, Canvas Notes).
- Support subfolder waypoint navigation when matched item belongs to a subfolder.
- Preserve existing settings persistence, data models, and business logic.

## Out Of Scope
- Redesigning unaffected screens (Note editors, Canvas, Onboarding, Trash).
- Adding new settings or altering existing settings keys.
- Changing database schema or FTS search index behavior.

## Features
### 1. Section-Oriented Settings Hierarchy
- Root view displays 6 compact main section cards with rounded corners (20dp), accent icon tiles (42dp), titles, descriptions, and forward navigation chevrons.
- Tapping a section smoothly displays the secondary section screen with a back button, section title, and a unified settings list.
- Settings inside section use clean row layouts with dividers instead of floating card containers.
- Back navigation (top bar back arrow or system back) returns cleanly to the root settings sections.

### 2. Full-Height Home Viewport & System Insets
- Eliminate artificial double-insets in `HomeScreen.kt` (Scaffold innerPadding vs WindowInsets.systemBars).
- Position bottom floating dock and bottom navigation bar naturally above system navigation bars using standard insets.
- Adjust folder grid bottom padding to ~110dp so the last row scrolls cleanly above the floating dock without introducing empty dead space.
- Keep the header and tab bar at their natural top positions without artificial vertical compression.

### 3. Responsive Photo Viewer Header Toolbar
- Reorganize `PhotoViewerDialog.kt` top bar into an organized two-tier layout:
  - Row 1: Back button, Note title (max 1 line, ellipsized), and action icons.
  - Row 2: Full-width horizontal row for instructional helper text ("Swipe next/prev · Pinch or double-tap to zoom") and deadline/schedule chips.
- Prevents word-by-word vertical text wrapping on narrow phone widths.

### 4. Search Result -> Folder Auto-Scroll & Highlight
- When a user taps a search result (Photo, Group, PDF/DOCX, Text Note, Canvas):
  - Resolves folder and subfolder, opens `FolderDetailNavKey` with target note ID.
  - Folder screen activates the appropriate subfolder tab.
  - Programmatically scrolls LazyVerticalGrid until target item is visible in viewport.
  - Applies a subtle exposure/brightness highlight overlay (fading in, holding for ~2s, smoothly fading out).
  - Skips unnecessary scrolling if target item is already visible.

## UI Mockup
```
Settings Root Screen:
+---------------------------------------------+
| Settings                                    |
|                                             |
| +-----------------------------------------+ |
| | [General] General                     > | |
| | Display density, sorting orders         | |
| +-----------------------------------------+ |
| +-----------------------------------------+ |
| | [Appearance] Appearance               > | |
| | Theme mode, visual styling              | |
| +-----------------------------------------+ |
| +-----------------------------------------+ |
| | [OCR] OCR & Recognition               > | |
| | Text recognition, language script       | |
| +-----------------------------------------+ |
| +-----------------------------------------+ |
| | [Bell] Notifications & Deadlines      > | |
| | Reminder lead time, due ribbon          | |
| +-----------------------------------------+ |
| +-----------------------------------------+ |
| | [Storage] Storage & Data Management   > | |
| | Storage paths, backup, trash, index     | |
| +-----------------------------------------+ |
| +-----------------------------------------+ |
| | [Info] About & Legal                  > | |
| | Version, open source licenses           | |
| +-----------------------------------------+ |
+---------------------------------------------+

Settings Detail (Opened Section):
+---------------------------------------------+
| (<) OCR & Recognition                       |
|                                             |
| Automatic OCR on Capture            [ O/ ]  |
| Extract text automatically after photo snap |
| ------------------------------------------- |
| OCR Recognition Script             Latin >  |
| Latin, Devanagari, Japanese, Chinese, etc.  |
| ------------------------------------------- |
| Downsampling Quality Tradeoff     Fast >    |
| Balance memory usage and OCR precision      |
+---------------------------------------------+
```

## Logic Notes
- Search navigation state: `FolderDetailNavKey` passes `targetPhotoId`, `targetGroupId`, `targetDocumentId`, `targetTextNoteId`, `targetCanvasId`.
- Highlight animation: `Animatable(0f)` with `0.35f` peak alpha white/brightness exposure overlay, 2000ms delay, 400ms fade-out.
- Memory & lifecycle safety: Highlight coroutine launched in `LaunchedEffect` keyed on target IDs; clears state upon completion to prevent stale highlights.

## Risks
- Risk: LazyGrid target item not composed before scroll -> Mitigation: Check index in `gridItems` and use `gridState.animateScrollToItem(index)`.
- Risk: Insets mismatch across gesture navigation vs 3-button navigation -> Mitigation: Use standard `WindowInsets.navigationBars` padding on bottom stack container only once.

## Dependencies
- Phase 11 (Home Screen Redesign)
- Rules.md R-001, R-002, R-004

## Acceptance Criteria
- [x] Settings root screen displays logical main section rounded cards (General, Appearance, OCR & Recognition, Notifications, Storage & Data, About).
- [x] Opening a Settings section displays individual settings as a clean settings list without card wrappers around each item.
- [x] Existing setting functionality, persistence, and dialogs remain completely functional.
- [x] Home screen uses full available vertical height without an artificial bottom dead zone.
- [x] Android system navigation buttons remain unobstructed and Fotara content does not collide with them.
- [x] Photo viewer header instructional text lays out horizontally across full width without vertical word wrapping.
- [x] Tapping a search result navigates to its actual parent folder and activates any target subfolder.
- [x] Destination list automatically scrolls until the selected item is visible (skipping scroll if already visible).
- [x] Target item receives a subtle temporary exposure/brightness highlight that smoothly fades out after ~2 seconds.
- [x] Highlight is transient and does not alter database values or persistent styles.
