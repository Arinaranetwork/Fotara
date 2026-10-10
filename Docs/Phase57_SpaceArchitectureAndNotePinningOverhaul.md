// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

# Phase 57 - SpaceArchitectureAndNotePinningOverhaul

## Goal
Implement a major architectural revision to space customization, note pinning, widget reliability, and UI navigation hygiene. This delivers universal note pinning across folders and groups (up to 4 pins), a dedicated Space Settings hub housing the Syllabus Evaluator, removal of workspace creation, fix for markdown table rendering, status bar insets for modular packages, today widget display recovery, and elimination of settings back-navigation jumping artifacts.

## Scope
1. **Markdown Table Visual Rendering in Notes (Image 1)**:
   - Implement structured table parsing and rendering in `RichMarkdownText` / `RichMarkdownColumn` with clean borders, header highlighting, and proportional columns.
   - Enhance edit mode table formatting in `TextNoteEditorScreen` with monospaced column alignment.
2. **Modular Add-Ons Window Insets Fix (Image 2)**:
   - Apply `Modifier.statusBarsPadding()` to `PackageManagementHubScreen.kt` preventing overlap with phone hardware status bar elements.
3. **Glance Today Widget Body Display Recovery (Image 3)**:
   - Fix empty / blank body rendering in `DueTomorrowGlanceWidget.kt`. Ensure both pre-Android 15 and Android 15+ layouts render notes, upcoming schedules, deadlines, or a high-contrast empty card.
4. **Universal Note Pinning in Folders & Groups (Limit 4 Pins)**:
   - Allow pinning of any note type (`Photo`, `DocumentNote`, `TextNote`, `CanvasNote`) in folders and within photo groups.
   - Enforce hard cap of 4 pinned notes per container.
   - Pinned notes float to the top of the grid with distinct pin badges and corner accents.
   - 3-dot contextual menus provide "Pin Note" / "Unpin Note".
5. **Home Screen Three-Dots "Select" Option Restoration**:
   - Restore the "Select" (multi-select mode) item in the Home screen header overflow menu.
6. **Dedicated Space Settings Hub & Syllabus Evaluator Relocation**:
   - Create a dedicated `SpaceSettingsScreen.kt` accessible from the space switcher or space header.
   - Move the Course Syllabus Weight & Exam Evaluator from general settings into the dedicated Space Settings page.
7. **Remove "Add Workspace" Option**:
   - Remove the `[ + ]` / "Add Workspace" button from `WorkspaceTabBar.kt` and `HomeScreen.kt`.
8. **Settings Back Navigation Jump & Residue Text Fix**:
   - Eliminate text jumping and transient artifact residue during back transitions from Settings sub-screens.
9. **Release Banner Recognition in In-App Updates**:
   - Update `UpdateManager.kt` asset parser to match assets where `lowerName.contains("banner")`.
   - Add moderate version banner `FotaraBanner_2.2_2026-10-10.jpg` to `Assets/Banners/`.
10. **Polished Dynamic GitHub Repository Presentation**:
    - Update `README.md` with dynamic release badges, dynamic download links, and 2.x architectural features.
11. **Rollback Download Target Version Resolution**:
    - Fix version display falling back to "1.0.0" when downloading a rollback release; properly bind `activeRollbackTarget` release metadata across download progress and banners.
12. **Dark-Dominant Note Action Bottom Sheets & Cards**:
    - Darken note action bottom sheets (`TextNoteQuickActionSheet`, `CanvasNoteQuickActionSheet`, `PhotoQuickActionSheet`) and note card backgrounds across folders and notes screens from bright navy to deep near-black tokens (`#0F1422` / `HomeCardSurface`), adhering to Arinara U-13.

## Out Of Scope
- Release APK build assembly or publication.
- Deletion of existing workspaces already stored in user databases (legacy workspaces remain read-only or consolidated).

## Features

### 1. Markdown Table Visual Rendering
- **Parser & Composable**: In `RichMarkdownColumn.kt`, detect Markdown table syntax (`| Col 1 | Col 2 |` followed by `| --- | --- |`).
- **Rendering**: Render as an elevated surface table with a tinted header row, 1dp subtle grid borders, comfortable cell padding, and horizontal scroll when exceeding viewport width.

### 2. Window Insets on Modular Add-Ons
- **Status Bar Clearance**: In `PackageManagementHubScreen.kt`, add `Modifier.statusBarsPadding()` so the header title and back button sit comfortably below the Android status bar.

### 3. Glance Today Widget Repair
- **Display Guarantee**: Ensure `DueTomorrowGlanceWidget` accurately computes widget size, loads all notes added or scheduled today, and always renders visible content (or high-contrast empty state with "No notes scheduled today" and tap action) instead of an invisible blank surface.

### 4. Universal Note Pinning (Folder & Group)
- **Data Model & Schema**: Support `is_pinned` column across `photos`, `document_notes`, `text_notes`, and `canvas_notes` tables with indexes.
- **Constraints**: Maximum 4 notes pinned per folder or group. Attempting to pin a 5th displays a toast/notification: "Maximum 4 pinned notes reached".
- **Presentation**: Pinned notes render in a pinned section or sorted ahead with an amber pin icon badge.

### 5. Home Screen Multi-Select Entry
- **Menu Entry**: In `HomeScreen.kt` top-right kebab menu, include `DropdownMenuItem` labeled "Select" with `Icons.Default.Checklist` / select icon to enter folder multi-selection mode.

### 6. Dedicated Space Settings Hub
- **Screen**: `SpaceSettingsScreen.kt` managing active space name, icon, color palette, stealth vault protection, and space statistics.
- **Syllabus Relocation**: Embed the Syllabus & Academic Weight Evaluator as a major section inside Space Settings.

### 7. Remove Workspace Creation
- **UI Clean-up**: In `WorkspaceTabBar.kt`, remove the trailing `[ + ]` button and `CreateWorkspaceDialog` triggers.

### 8. Back Navigation Transition Stability
- **Animation Polish**: Ensure `SettingsScreen` and its sub-screens use clean enter/exit animations without un-hoisted shared state flicker or jumping title residue during popBack.

### 9. Release Banner Recognition in In-App Updates
- **Asset Pattern**: Update `UpdateManager.kt` asset parser to match assets where `lowerName.contains("banner")` rather than restrictive `startsWith("banner")`.
- **Repository Asset**: Add moderate version banner `FotaraBanner_2.2_2026-10-10.jpg` to `Assets/Banners/` and commit to repository so banner displays in app and releases.

### 10. Polished Dynamic GitHub Repository Presentation
- **Documentation**: Update `README.md` with dynamic release badges, dynamic download links to latest release, updated banner image, and full 2.x feature matrix.

### 11. Rollback Download Target Version Resolution
- **Resolution**: In `UpdateScreen.kt`, bind downloading state to `activeRollbackTarget` when downloading a rollback release so the hero banner and notifications display the chosen rollback version (e.g. `v1.9.0`) instead of defaulting to `v1.0.0`.

### 12. Deep Dark Palette for Note Sheets and Cards
- **Dark Theme Alignment**: Deepen container color of note quick action sheets (`TextNoteQuickActionSheet`, `CanvasNoteQuickActionSheet`, `PhotoQuickActionSheet`) from `#141936` to `#0F1422` / `HomeCardSurface`. Darken note cards in `FolderDetailScreen` and `NotesScreen` to match near-black dark-dominant theme.

## Acceptance Criteria
- Markdown tables render cleanly formatted with grid lines and headers.
- Modular Add-Ons screen header does not overlap the phone's status bar.
- Today widget displays notes/schedules or an explicit empty state card.
- Users can pin up to 4 notes of any type in any folder or group.
- "Select" is present in Home screen 3-dot menu.
- Dedicated Space Settings screen exists and contains Syllabus Evaluator.
- "Add Workspace" button is completely removed.
- Returning from Settings sub-screens produces zero visual residue jumping.
- In-app update checker detects release banner images via `contains("banner")`.
- `README.md` is polished with dynamic release badges and up-to-date documentation.
- Rollback download displays the actual target version instead of 1.0.0.
- Note action bottom sheets and note cards use cohesive deep dark background tones.
- All unit tests pass; release APK build omitted.
