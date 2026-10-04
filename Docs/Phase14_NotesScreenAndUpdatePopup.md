# Phase 14 - Notes Screen and In-App Update Popup

## Goal
Implement the fully functional Notes tab screen and the new update modal dialog matching visual reference specifications (Screenshots 1 & 2), integrating reactively with all Fotara note types and release management.

## Scope
- Notes Screen (Task 1):
  - Top header with "Notes" title, "Your activity, all in one place" subtitle, 40dp circular Search and More buttons.
  - Horizontally scrollable pill filter chips (All, Photos, Documents, Text, Canvas).
  - Date-grouped notes list with colored dots (Blue for Today, Purple for Yesterday, Green/palette for older dates), pill uppercase labels (TODAY, YESTERDAY, MMM d, yyyy), item count headers, and single rounded card containers per group.
  - Multi-type note item rows with ~60dp rounded thumbnails (Photos, Documents, Text, Canvas), 3 text lines (Title, Folder, Timestamp), and 3-dot overflow actions (Open, Rename, Move to Folder, Delete with confirmation).
  - Floating Action Button (FAB) anchored bottom-right above bottom navigation for new note creation.
  - Reactive integration across all 5 note sources (Photos, Documents, Text Notes, Canvas Notes, Folders) excluding trashed items.
  - Inline search filtering and friendly empty states.
- New Update Popup (Task 2):
  - Centered modal dialog with ~28dp rounded corners, dark surface, subtle border, and dimmed background.
  - Top banner illustration with rounded top corners and top-right close "X" button.
  - "New Update {version}" bold white title with dark outline/shadow overlaid at the bottom of the banner.
  - Dark scrollable body displaying release notes / changelog text.
  - Bottom row with two equal-width tonal pill buttons: "Later" and "Skip this version".
  - Semantic version checking (`latestVersion > installedVersion && latestVersion != skippedVersion`), session-only dismissal for "Later" / "X", and persistent storage for "Skip this version".

## Out Of Scope
- Backend cloud synchronization or user accounts.
- Modifying unrelated screens or database schemas.

## Features
### Notes Screen Tab
- Unified observation of PhotoRepository, DocumentRepository, TextNoteRepository, and CanvasNoteRepository.
- Filtering out trashed items across all repositories.
- Grouping notes by calendar date based on `addedAt` timestamp (descending order).
- Date group headers with dot indicators: Blue (#3B82F6) for Today, Purple (#8B5CF6) for Yesterday, Green (#10B981) for older dates.
- Filter chips for type narrowing: All, Photos, Documents, Text, Canvas.
- Real-time search query filtering over title, folder name, and text notes.
- Item actions via 3-dot kebab menu: Open, Rename, Move to Folder, Delete to Trash with confirmation dialog.
- Floating Action Button (+) floating above bottom navigation.

### In-App Update Modal Popup
- Checked on app startup / home screen when auto-check is enabled.
- Modal dialog with ~28dp radius, top banner with Coil AsyncImage and bundled local drawable fallback.
- Overlaid text "New Update {version}" with high legibility.
- Scrollable markdown/bullet release notes.
- "Later" button: dismisses dialog for current session without persistent change.
- "Skip this version" button: saves version to SharedPreferences, never shows again for this version, but triggers if a newer version is released.
- Close button, background tap, and back press mirror "Later" behavior.

## UI Mockup
```
+---------------------------------------------------+
|  Notes                                (Q)  (:)   |
|  Your activity, all in one place                  |
|                                                   |
|  [ All ]  [ Photos ]  [ Documents ]  [ Text ] ... |
|                                                   |
|  * TODAY                                  3 items |
|  +---------------------------------------------+  |
|  | [Img] Hukum Hess                            |  |
|  |       [Dir] Kimia                           |  |
|  |       20:31                             (:) |  |
|  | ------------------------------------------- |  |
|  | [Img] Foto Papan Tulis                      |  |
|  |       [Dir] Kimia                           |  |
|  |       18:42                             (:) |  |
|  +---------------------------------------------+  |
|                                                   |
|  * YESTERDAY                              2 items |
|  +---------------------------------------------+  |
|  | [Img] Materi UH Semester 1                  |  |
|  |       [Dir] Kimia                           |  |
|  |       Yesterday • 22:14                 (:) |  |
|  +---------------------------------------------+  |
|                                                   |
|                                            (+)    |
|       +-----------------------------------+       |
|       |  [Home]       [Notes]  [Settings] |       |
|       +-----------------------------------+       |
+---------------------------------------------------+
```

```
Modal Popup:
+---------------------------------------------+
| /-----------------------------------------\ |
| | [ Landscape Banner Illustration ]   (X) | |
| |                                         | |
| |            New Update 2.0.0             | |
| \-----------------------------------------/ |
|                                             |
|  What's New:                                |
|  • Smooth GPU hardware-accelerated drawing  |
|  • Dark mode performance improvements       |
|  • Date-grouped activity timeline           |
|                                             |
|  (        Later        ) (  Skip this version ) |
+---------------------------------------------+
```

## Logic Notes
- Timestamps: standard epoch millisecond `addedAt` field across all note entities.
- Date calculation: `java.time.LocalDate` with `ZoneId.systemDefault()`.
- Version parsing: semantic numeric version extraction (`major.minor.patch`) via `UpdateManager.isNewerVersion()`.
- Skipped version persistence: `SharedPreferences` in `UpdateManager` under key `skipped_update_version`.
- Session dismissal: `var sessionDismissed: Boolean` in `UpdateManager` or state holder.

## Risks
- Multiple reactive flows causing recomposition churn -> Mitigation: Combine flows with `combine` into a single `StateFlow` in `NotesViewModel`.
- Network timeout on update check blocking UI -> Mitigation: Update check runs on IO dispatcher with silent failure handling.

## Dependencies
- `PhotoRepository`, `DocumentRepository`, `TextNoteRepository`, `CanvasNoteRepository`, `FolderRepository`.
- `UpdateManager`, `Coil`.

## Acceptance Criteria
- [x] Notes screen displays all active notes grouped by Today, Yesterday, and older calendar dates.
- [x] Group headers have colored dot (Blue, Purple, Green), pill label, and item count.
- [x] All items within each group sit inside one rounded card container (#111726).
- [x] Filter chips filter by note type reactively and update counts.
- [x] Tapping note opens correct viewer/editor.
- [x] 3-dot item menu provides Open, Rename, Move, and Delete (with confirmation).
- [x] FAB (+) button floats above bottom nav and opens note creation options.
- [x] New Update dialog appears over dimmed background on Home when newer version available and not skipped.
- [x] Banner displays with overlaid "New Update {version}" title and "X" button.
- [x] "Later" dismisses popup for current session only.
- [x] "Skip this version" persists version string and prevents re-prompting until newer version exists.
- [x] 100% unit tests pass, release build compiles with zero errors.
