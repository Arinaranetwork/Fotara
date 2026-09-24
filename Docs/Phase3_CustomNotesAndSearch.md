# Phase 3 - CustomNotesAndSearch (v1.1 Consolidated Batch Revision)

## Goal
Deliver the complete Fotara v1.1 consolidated release: resolve startup latency down to verified performance targets (<1.5s cold, <500ms warm), replace search-result viewer behavior with direct source-folder navigation and highlight, complete subfolder management and photo multi-select with full contextual action bars, implement a comprehensive grouped Settings screen, establish a data-safety Trash/Recycle Bin system, enhance the photo viewer with manual rotate/re-crop, deliver on-device onboarding, implement search filters and recent queries, support responsive tablet/landscape layouts, provide a "Due tomorrow" home-screen widget, and secure folders with biometric/PIN privacy locks.

## Scope
- **Startup Latency Optimization**: Audit main-thread startup bottlenecks; enforce non-blocking initialization and reactive queries (`Flow`) to achieve <1.5s cold start and <500ms warm start.
- **Individual Photo Custom Notes**: Editable `note: String?` field on `Photo` model with interactive inspector editor and FTS4 virtual table indexing.
- **Individual Photo Rename**: Add "Rename" to photo long-press quick-action menu, editing `caption` via a dedicated dialog and updating search index immediately.
- **Subfolder Rename & Delete**: Unified single long-press context menu resolving tab touch-target constraints; delete moves subfolder and contents to Trash.
- **Subfolder Multi-Select & Bulk Delete**: Contextual action bar on tab row with high-contrast filled-chip indicators; bulk move to Trash.
- **Photo Multi-Select within Folders**: Resolves v1.0 baseline gap per Addendum 1 & 3; quick-action "Select" entry, checkmark indicators, and top contextual action bar (Delete, Rename [single only], Move to folder, Color label).
- **Search Result Navigation & Highlight**: Supersedes Revision 4 item 4 popup slider viewer behavior; navigates directly to source folder and subfolder tab with animated auto-scroll and 3-second non-blocking dimmed translucent overlay.
- **Grouped Settings Screen**: 7 categorized sections (Display & Organization, OCR & Processing, Notifications, Storage with thumbnail rebuilding, Data backup/import, Search index rebuild, About).
- **Data Safety (Trash / Recycle Bin)**: Soft-delete architecture (`deleted_at`, status flag), 30-day auto-purge, orphan parent restoration, accessible via Settings and Home overflow menu.
- **Onboarding / First-Run Flow**: Pre-permission explanation for camera/storage, optional common subject folder generator ("Math", "Science", "History", "Literature"), strict microcopy compliance.
- **Photo Viewer Refinements**: 90° manual rotation control, manual re-crop editor replacing stored version.
- **Search Filters & Recents**: Horizontal filter row (Date Range, Color Label) docked above floating keyboard-docked search bar; recent search history list when query is empty.
- **Notifications & Widget**: Home-screen widget displaying "Due tomorrow" deadlines with empty state; notification `[View Note]` direct deep-link action button.
- **Accessibility & Device Adaptation**: Responsive grid columns for tablet/landscape; system font scaling without clipping.
- **Folder Privacy Lock**: BiometricPrompt and PIN folder lock; locked card presentation without thumbnail previews; device lock screen credential recovery.

## Out Of Scope
- Cloud synchronization, remote backups, and multi-user collaborative editing (offline-first local architecture strictly enforced per R-003).
- Rich-text markdown or WYSIWYG note formatting (plain text optimized for fast FTS indexing).
- Candidate items explicitly excluded for this revision:
  1. Cross-folder automatic smart tags / auto-clustering.
  2. Cloud vision API integration (strictly on-device ML Kit per R-003).
  3. External web clipper / browser extension integrations.

---

## Features & Architectural Specifications

### 1. Priority Fix: Startup Latency Audit & Optimization
- **Target Latencies** (Rev 4 Section 1): Cold start **< 1.5 seconds**; Warm start **< 500 milliseconds**.
- **Audit Requirement**: Profile all work executing on the main thread during `Application.onCreate()` and `MainActivity.onCreate()` prior to the first composition frame. Specifically inspect:
  - Synchronous SQLite database reads or schema integrity checks.
  - ML Kit text recognition client warm-up.
  - Initial folder thumbnail decoding and bitmap allocation.
- **Optimization Directives**:
  - Enforce reactive streaming (`Flow<List<Folder>>`) for initial home-screen state.
  - Defer ML Kit client initialization and thumbnail cache priming to background coroutines (`Dispatchers.Default` / `Dispatchers.IO`) strictly *after* the first frame renders.
  - Prohibit any blocking disk I/O on the main thread.
- **Verification**: Document and report actual measured startup latency against benchmark targets.

### 2. Rename Individual Photo/Note (Caption Edit)
- **Entry Point**: Photo long-press quick-action menu in folder grid and triage review modal ([CaptureReviewSliderModal.kt](file:///c:/Users/sepli/OneDrive/Documents/File%20DD/Coding/Fotara/app/src/main/java/com/arinara/fotara/ui/components/CaptureReviewSliderModal.kt)). "Rename" is added to the existing menu (Move, Color label, Share, Delete).
- **Data Target**: Mutates `Photo.caption`.
- **UI Interaction**: Focused modal dialog (`RenamePhotoDialog`) with auto-focused text field, prefilled with existing caption, with "Save" and "Cancel" buttons.
  - *Justification vs. Folder Inline Rename*: Folder headers occupy a wide full-width card with ample room for in-place text editing. Photo thumbnails in 2/3/4-column grids are too compact for inline text input, causing layout clipping and soft-keyboard occlusion. A dedicated dialog provides clear visibility, full keyboard ergonomics, and explicit dismiss boundaries.
- **Commit & Cancel**: Commits on "Save" or keyboard IME Done. Cancels without saving on "Cancel", outside touch, or system back.
- **Search Index Reflection**: Committing immediately updates `photos_fts` virtual table and refreshes parent folder's `updated_at`.

### 3. Subfolder Management (Rename & Delete)
- **Gesture Conflict Resolution**:
  - *Problem*: Folder cards split gestures between long-pressing the title text (rename) and long-pressing the card body (pin/select context menu). Subfolder tabs have tiny surface area where splitting touch targets is unreliable.
  - *Resolution*: Abandon the split gesture for subfolder tabs. A single long-press anywhere on a subfolder tab opens a unified context menu containing **Rename**, **Delete**, and **Select**.
- **Rename Action**: Selecting "Rename" opens the same dedicated dialog pattern decided for photo rename, prefilled with subfolder name.
- **Delete Action**: Moves subfolder and all child photos to Trash (`deleted_at` timestamp). Confirmation dialog shows subfolder photo count and total size, using trash language (*"Move to trash"*).
- **Cancel**: Dismissing context menu leaves subfolder and tab order unchanged.

### 4. Subfolder Multi-Select & Bulk Delete
- **Entry Point**: "Select" item in the subfolder tab context menu.
- **Visual State**: Selected tabs render with an inverted filled background (`FolderBodyBlue` `#0B1BE0` or amber accent) and a leading checkmark icon badge (`Icon(Icons.Default.Check)`), distinguishing them from unselected tabs at a glance.
- **Contextual Action Bar**: Replaces folder top bar with selection count and **Delete** action. Confirmation dialog displays total subfolder count, combined photo count, and total size across selected tabs.
- **Exit**: Cancel button or system back clears selection and returns tabs to single-tap navigation.
- **Simultaneous Mode Conflict Resolution**:
  - *Rule*: Subfolder Tab Multi-Select and Photo Grid Multi-Select **must never be active simultaneously**.
  - *Precedence*: Entering one mode immediately dismisses and clears the other. While one mode is active, triggering the other is disabled.

### 5. Photo Multi-Select within Folder (Addendum 1 & 3)
- **Status**: Closes baseline v1.0 defect where long-press acted only on single photos or selection state had no action bar.
- **Entry Point**: Photo long-press -> quick-action menu -> select "Select".
- **Visual State**: Checkmark overlay indicator on top of selected thumbnails; border highlight.
- **Contextual Action Bar**: Replaces folder top bar while active:
  - Back / Close button (exits multi-select and clears selection).
  - Selected count title (e.g., "3 selected").
  - 4 actions:
    1. **Delete**: Move selected photos to Trash; cancel linked deadline reminders.
    2. **Rename**: Enabled strictly when exactly 1 photo is selected; completely hidden when 2+ are selected.
    3. **Move to Folder**: Batch folder relocation dialog (resolves product brief requirement).
    4. **Color Label**: Batch color tag assignment.
- **Exit**: Tapping Cancel/Close or system back clears selection and restores standard folder top bar.
- **Coexistence**: Coexists cleanly with home-screen folder-level multi-select from v1.0.1.

### 6. Search Result Navigation with Auto-Scroll & Highlight (Superseding Rev 4 Item 4)
- **Superseding Decision**: Replaces Revision 4 item 4's behavior (which opened the popup slider viewer). Search result tap now navigates directly to the photo's source folder.
- **Folder / Subfolder Target Resolution**: Navigates to the parent folder. If the photo belongs to a subfolder, the corresponding subfolder tab is automatically activated on arrival.
- **Auto-Scroll Behavior**:
  - If the matched photo cell is outside the visible grid viewport, the grid executes an animated smooth scroll (`LazyGridState.animateScrollToItem`) bringing the item into view.
  - *Proposed Default Duration*: 300ms fast-out-slow-in animation curve.
- **Highlight Behavior & Timing**:
  - Once visible in the viewport, a non-blocking dimmed translucent overlay appears over the target cell.
  - *Proposed Default Styling*: Translucent warm accent overlay (`FolderTabCream` / soft amber `#FFE082` at 30% opacity with 12dp rounded corners) that leaves the thumbnail clearly recognizable underneath.
  - *Timing Rule*: Highlight persists for exactly 3.0 seconds, then fades out smoothly over 300ms. The 3-second timer begins strictly *after* the auto-scroll animation has finished.
  - *Non-Blocking Interaction*: User can immediately tap the photo (opening standard inspector/viewer) or tap any other control without waiting for the timer.
- **Edge Cases**:
  - *Photo Moved / Deleted*: If photo was moved or trashed between indexing and tap, display a transient Snackbar: *"Photo is no longer in this folder"*, refresh search results, and do not navigate to an invalid state.
  - *Paginated Grid Loading*: If the matched photo index resides in an unrendered pagination page, prefetch the required chunk before initiating the scroll.

### 7. Trash / Recycle Bin (Data Safety Priority)
- **Structural Model**: Soft-delete via `deleted_at: Long?` and `is_trashed: Boolean` on `Photo`, `Subfolder`, and `Folder` records. Trashed items are excluded from all active folder grids and search queries.
- **Retention & Auto-Purge**: Default **30 days** retention window. Items past 30 days are permanently purged via a lightweight background check on startup.
- **Manual Actions in Trash**:
  - "Restore": Restores item to its original folder and subfolder.
  - *Orphan Parent Resolution*: If a photo's original folder is also in Trash, prompt: *"Original folder '[FolderName]' is in Trash. Restore both, or select an active folder?"* Default action: restore both parent and child.
  - "Empty Trash": Immediately purges all trashed items and deletes local image files.
- **Entry Point Conflict Resolution**:
  - *Primary Entry*: Settings screen under "Storage & Data".
  - *Quick Entry*: Home screen top-right Kebab overflow menu (`⋮`), placed at the bottom with a subtle divider.  
  *Justification*: Keeps in-folder top bars uncluttered while giving users fast access from the home dashboard.
- **Confirmation Wording Update**: All delete confirmation dialogs replace permanent deletion warnings with trash language (*"Move to Trash? Items can be restored within 30 days"*).

### 8. Onboarding / First-Run Flow
- **Permission Pre-Prompt**: Clean, focused pre-permission screen explaining why Camera and Storage permissions are required before launching Android OS runtime dialogs.
- **Starter Subject Folders**:
  - Offers: *"Start with common subjects or build your own?"*
  - Default proposed subjects: **"Math"**, **"Science"**, **"History"**, **"Literature"**.
  - Action buttons: "Create Default Folders" vs. "Start Empty" (skip).
- **Microcopy Compliance**: Follows Revision 3 microcopy cleanup: strictly factual, zero marketing taglines or reassuring fluff.

### 9. Photo Viewer Refinements
- **Manual Rotate**: 90-degree clockwise rotation button in top/bottom action bar of popup viewer, rotating the bitmap and saving rotation metadata.
- **Manual Re-crop**: "Adjust Crop" button launching an interactive 4-corner boundary editor.
  - *Decision*: **Replaces the stored processed version directly**.
  - *Justification*: Simplifies the image pipeline, avoids duplicate storage bloat on student devices, and keeps FTS re-indexing linear and deterministic.

### 10. Search Filters & Recent Searches
- **Placement Conflict Resolution**:
  - *Problem*: Revision 2 introduced a floating search bar docked directly above the software keyboard (`WindowInsets.ime`). Filter controls must not obstruct typing.
  - *Resolution*: Filter controls (Date Range, Color Label) render as a single horizontal scrolling chip row docked **directly above the floating search bar** (`Z = 24dp`). When the search bar moves up with the keyboard, the filter row rides with it.
- **Recent Search Terms**: Render in the content area between the top of the screen and the filter row when search input is focused and empty, with a "Clear Recents" option.

### 11. Notifications & Home-Screen Widget
- **Widget**: App widget displaying "Due tomorrow" notes list.
  - *Empty State*: Clean midnight card displaying *"No deadlines due tomorrow"* with a calendar outline icon, avoiding blank or broken UI boxes.
- **Notification Action**: Deadline notification includes `[View Note]` direct action button firing a `PendingIntent` that deep-links directly into the full-screen [PhotoViewerDialog.kt](file:///c:/Users/sepli/OneDrive/Documents/File%20DD/Coding/Fotara/app/src/main/java/com/arinara/fotara/ui/components/PhotoViewerDialog.kt).

### 12. Accessibility & Device Adaptation
- **Tablet / Landscape Layout**: Responsive grid columns using adaptive breakpoints:
  - Phone Portrait: 2 columns (folders) / 3 columns (photos).
  - Tablet / Landscape: 4 to 6 columns based on `LocalConfiguration.current.screenWidthDp`.
- **Dynamic Font Scaling**: All UI text uses standard `sp` units respecting system-level font size accessibility scaling up to 200% without layout truncation or button overlap.

### 13. Folder Privacy Lock
- **Authentication**: Folder lock via PIN or Android `BiometricPrompt` (fingerprint/face unlock).
- **Home Screen Presentation**:
  - Show folder card with a prominent Lock icon (`Icons.Default.Lock`) and folder title, but **completely blank/generic ivory tab and no thumbnail preview**.
  - *Justification vs. Fully Hiding*: Fully hiding the card makes the student feel the folder was deleted or lost, disrupting spatial memory and grid layout. Showing the card with a locked state confirms the folder exists while strictly preventing unauthorized viewing of coursework/photo thumbnails.
- **PIN Recovery Path**:
  - Since Fotara is 100% offline with no cloud server or account, a forgot-PIN flow uses **Android Device Credential Fallback** (`BiometricManager.Authenticators.DEVICE_CREDENTIAL`). If the user authenticates with their device lock screen (phone PIN, pattern, or fingerprint), they are permitted to reset the folder PIN.

### 14. Grouped Settings Screen
Organized into 7 categorized sections:
1. **Display & Organization**:
   - Default sort order (Upload Date / Nearest Deadline / Color Label).
   - Thumbnail grid density (2 / 3 / 4 columns).
   - Theme (Light / Dark / System Default).
2. **OCR & Processing**:
   - Automatic OCR on capture toggle.
   - OCR language selection.
   - Pre-OCR downsampling quality tradeoff: High Quality vs. Fast Processing.
3. **Notifications**:
   - Default reminder lead time before deadline (H-1 / H-3 / Custom).
   - "Due tomorrow" home-screen ribbon toggle.
4. **Storage**:
   - Photo storage location (Internal App Storage vs. Scoped External / SD card).
   - "Rebuild Thumbnails" button (regenerates all cached thumbnails from original images without touching photo files).
   - Total storage usage breakdown (Photos, Thumbnails, Database index).
5. **Data**:
   - Full database backup / export (offline ZIP/JSON export).
   - Import from backup file with confirmation prompt.
6. **Search**:
   - "Rebuild Search Index" action (forces full FTS4 re-indexing from SQLite database).
7. **About**:
   - App version, open source licenses.

---

## UI Mockup

```
SUBFOLDER TAB CONTEXT MENU & MULTI-SELECT
+-------------------------------------------------------------+
| [← Back]              Biology / Cell Structure              |
| [All]  [Lectures (⋮)]  [Lab Notes]  [Assignments]           |
|        +--------------+                                     |
|        | Rename       |                                     |
|        | Delete       |                                     |
|        | Select       |                                     |
|        +--------------+                                     |
+-------------------------------------------------------------+

SUBFOLDER TAB MULTI-SELECT ACTION BAR
+-------------------------------------------------------------+
| [X] 2 subfolders selected                          [Delete] |
+-------------------------------------------------------------+
| [All]  [✓ Lectures]  [✓ Lab Notes]  [Assignments]           |
+-------------------------------------------------------------+

SEARCH RESULT TAP -> FOLDER NAVIGATION WITH HIGHLIGHT
+-------------------------------------------------------------+
| [← Back]              Biology / Cell Structure              |
| [All]  [Lectures (*)]  [Lab Notes]  [Assignments]           |
+-------------------------------------------------------------+
|  +---------------+  +---------------+  +---------------+    |
|  | Photo #1      |  | Photo #2      |  | Photo #3      |    |
|  +---------------+  +---------------+  +---------------+    |
|                                                             |
|  +---------------+  +---------------+  +---------------+    |
|  | Photo #4      |  | [=== MATCH ==]|  | Photo #6      |    |
|  |               |  | (Highlight    |  |               |    |
|  |               |  |  Overlay 3s)  |  |               |    |
|  +---------------+  +---------------+  +---------------+    |
+-------------------------------------------------------------+

SEARCH WITH FLOATING DOCK & FILTER ROW
+-------------------------------------------------------------+
| Recent Searches:                                            |
| [Mitosis (x)]  [Calculus quiz (x)]  [Optics (x)]            |
|                                                             |
| (Content area...)                                           |
|                                                             |
| [Filters: Date Range [∨] | Color Label [∨]]                 |
| +---------------------------------------------------------+ |
| | [Search notes, coursework, deadlines...]            [X] | |
| +---------------------------------------------------------+ |
| [================ KEYBOARD WINDOW INSETS =================] |
+-------------------------------------------------------------+
```

### 15. Photo Groups (v1.1 Addendum 5)
- **Data Model**:
  - `PhotoGroup`: `id: Long`, `folderId: Long`, `subfolderId: Long?`, `name: String`, `tagColor: String?`, `createdAt: Long`, `coverPhotoId: Long?`, `isTrashed: Boolean`, `deletedAt: Long?`.
  - `Photo` gains `groupId: Long?` (nullable) — a photo belongs to at most one group at a time.
- **Group Creation (Multi-Select Integration)**:
  - Reuse photo multi-select mode (long-press photo -> "Select" -> toggle checkboxes).
  - Contextual action bar gains "Group" action enabled strictly when `selectedPhotoIds.size >= 2` (symmetric with "Rename" requiring exactly 1 selection; both rules coexist without conflict).
  - Tapping "Group" opens `CreateGroupDialog` requiring a user-entered name (no blank input, no auto-filled generic placeholders).
  - On confirm: creates `PhotoGroup`, assigns `groupId` to selected photos, sets `coverPhotoId` to photo with earliest `addedAt`, and exits multi-select.
- **Group Display in Folder Grid**:
  - Member photos are grouped into a single cell: cover photo thumbnail, group name label, stacked-photos badge showing member count (e.g., layers icon + "5" count badge), and independent tag color pill.
  - *Sort-Order Placement*:
    - Upload Date: Uses `PhotoGroup.createdAt`.
    - Nearest Deadline: Uses earliest non-null `linkedDeadline` among member photos.
    - Color Label: Uses `PhotoGroup.tagColor` (independent of member photos).
- **Opening a Group (Scoped Slider Viewer)**:
  - Tapping group cell opens the popup slider viewer (Revision 1 item 3) scoped strictly to that group's member photos.
  - Standard per-photo actions remain available (rotate, re-crop, share out).
  - Includes "Remove from group" action setting photo's `groupId = null`.
  - *Auto-Dissolve Rule*: If removal reduces group to exactly 1 photo, the group automatically dissolves: the remaining photo returns to standalone state and the `PhotoGroup` record is deleted.
- **Group-Level Long-Press Context Menu**:
  - Distinct menu from standalone photo quick-actions:
    1. **Rename**: Edits `PhotoGroup.name` via focused dialog.
    2. **Ungroup**: Dissolves group (clears `groupId` on all members, deletes group record; non-destructive, no confirmation required).
    3. **Delete**: Moves group and all member photos to Trash; confirmation dialog displays member count and combined size in trash language.
    4. **Color Label**: Sets `PhotoGroup.tagColor`.
    5. **Select**: Enters folder-level multi-select with this group selected.
    6. **Add Photos**: Opens picker of available standalone photos in the folder/subfolder to fold into this group.
- **Groups Inside Photo Multi-Select Mode**:
  - Group cells can be selected as a single unit (checkmark overlay on the group cell).
  - Mixed selection of standalone photos and groups is supported in the unified action bar:
    - *Delete*: Moves selected standalone photos and entire selected groups (with members) to Trash.
    - *Move to Folder*: Relocates selected photos and selected groups (with members) to target folder.
    - *Color Label*: Assigns color to selected standalone photos and sets `tagColor` for selected groups.
    - *Rename Branching*: Enabled when `totalSelected == 1`. If standalone photo -> edits caption; if group cell -> edits group name.
    - *Group Action*: Enabled strictly when `selectedPhotoIds.size >= 2` and `selectedGroupIds.isEmpty()`.
- **Search Integration**:
  - `PhotoGroup.name` is indexed into search. Matching group results navigate directly to folder and activate a 3-second dimmed translucent highlight on the group cell.
- **PDF Export Integration**:
  - Member photos are exported in sequence at the group's sort position with a group title section header.
- **Conflicts & Proposed Defaults**:
  - *Badge vs. Color Label*: Stack badge placed in top-left; group tag color pill placed in top-right, avoiding visual collision.
  - *Stack-Badge Icon*: `Icons.Default.Layers` paired with bold count text.
  - *Naming Pattern*: Modal dialog (`CreateGroupDialog`) consistent with existing rename dialogs.

---

### v1.1 Addendum 6 — Zoom-Gated Swipe Navigation in Slider/Inspectors
- **Scope Reversal Confirmation**: Reverses v1.1's previous pinch/zoom exclusion specifically to reinstate zoom capability, providing a coherent `scale > 1.0f` state for swipe gating.
- **Gating Rule**: Horizontal swipe navigation (`HorizontalPager.userScrollEnabled`) is enabled strictly while the currently displayed photo is at default fit-to-screen scale (`scale == 1.0f`). While zoomed in (`scale > 1.0f`), horizontal drag gestures exclusively pan the zoomed photo image and are prohibited from advancing pages.
- **Reset to Unzoomed State**:
  - Double-tap: Toggles between 1.0f and 2.0f (proposed default) with 250ms `FastOutSlowInEasing` animation; resets `Offset.Zero`.
  - Pinch back out: Snaps back to `scale = 1.0f` and `offset = Offset.Zero` when scale drops below 1.05f.
  - Page switch: Automatically resets zoom state to 1.0f on page transitions.
- **Proposed Defaults**:
  - Max zoom scale: `3.0f` (scale range: `1.0f .. 3.0f`).
  - Double-tap zoom scale: `2.0f`.
  - Double-tap animation duration: `250ms`.
- **Context Uniformity**: Applied identically across Group-scoped review slider (`GroupSliderViewerModal.kt`), Capture review triage slider (`CaptureReviewSliderModal.kt`), and full folder/scoped note inspector (`PhotoViewerDialog.kt`).

### v1.1.1 — Bug Fixes & Addendum 7 Group Screen Navigation Change
- **Bug A Resolution (Thumbnail Grid Density Wiring & Responsive Scaling)**:
  - Connect `SettingsRepository.settingsFlow` directly to `FolderDetailViewModel` so `FolderDetailUiState.gridDensity` reflects the saved preference (2, 3, or 4 columns) in real time without requiring an app restart.
  - Responsive Scaling Resolution: The user's chosen density serves as the **baseline density** for standard phone portrait (`< 600dp`). Tablet and landscape layouts scale up additively from this baseline (`screenWidthDp >= 840 -> baseDensity + 3`, `screenWidthDp >= 600 -> baseDensity + 1`), preserving user intent proportionally across all orientations and devices.
- **Bug B Resolution (Subfolder Rename/Delete/Bulk-Delete Repeat Failure Diagnosis & Fix)**:
  - Root Cause Diagnosis: The gesture listener was previously declared on a parent `Box(Modifier.combinedClickable(...))` wrapping a Material 3 `FilterChip`. `FilterChip` consumed pointer down/click events internally, completely swallowing gestures before the outer long-press handler could fire.
  - Fix: Eliminated the nested `FilterChip` child on subfolder tabs and unified the tab presentation into a single `Surface` with `clip(RoundedCornerShape(8.dp))` and direct `combinedClickable(onClick, onLongClick)`. Single tap selects the subfolder; long-press triggers haptic feedback and displays the unified dropdown context menu (Rename, Delete with trash stats, Select for multi-select).
- **Bug C Resolution (Swipe to Next Photo Unzoomed Gesture Diagnosis & Fix)**:
  - Root Cause Diagnosis: `detectTransformGestures` consumes pointer events past touch slop by default, regardless of whether zoom/pan is performed. Even when unzoomed (`scale == 1.0f`), dragging with 1 finger was consumed by the transform detector, intercepting horizontal swipes before `HorizontalPager` could handle them.
  - Fix: Implemented a pointer-count guard using `awaitEachGesture`. While unzoomed (`targetScale <= 1.001f`), pointer changes are checked: if fewer than 2 fingers are touching, events are left unconsumed, allowing `HorizontalPager` to advance photos smoothly. When zoomed (`targetScale > 1.001f`), `detectTransformGestures` consumes pointer input for panning while pager scrolling is disabled.
- **Addendum 7: Group Opening Behavior Change (Supersedes Addendum 5's "Opening a Group" Section)**:
  - Tapping a group cell in a folder grid now navigates to a dedicated fullscreen `GroupDetailScreen` instead of immediately opening the popup review slider.
  - **Screen Layout**: Top bar mirrors Folder Screen (Back button, Group name + tag color pill, [+] Add photos button, [(⋮)] overflow menu with Rename, Ungroup, Delete to Trash with confirmation, and Color label).
  - **Grid Content**: Displays member photos in a flat grid with order-added sorting (earliest `addedAt` cover photo appears first) and respects the user's grid density setting.
  - **Quick-Access Shortcut**: Group cell long-press on folder grid is retained as a quick-access shortcut duplicate for Rename, Ungroup, Delete, Color label, and Add photos.
  - **Scoped Viewer**: Tapping a photo inside `GroupDetailScreen` opens the popup slider viewer scoped to group members with zoom-gated swipe navigation and "Remove from group" (triggering auto-dissolve if remaining members <= 1).
- **Addendum 7 Edge Case: Grouped Member Photo Search Highlight**:
  - Searching for OCR text or captions matching a grouped photo highlights the parent group cell in the folder grid as a waypoint (~1-second brief highlight), then automatically navigates into `GroupDetailScreen` and highlights the specific matched photo for the full 3 seconds.

---

## Acceptance Criteria
- [x] Startup latency audit documented; main thread optimized to meet <1.5s cold start and <500ms warm start.
- [x] Photo rename modifies caption via dedicated dialog and immediately updates FTS search index.
- [x] Subfolder tab long-press reveals single context menu (Rename, Delete, Select) resolving tab size limitations.
- [x] Subfolder multi-select highlights selected tabs with inverted filled background and checkmark, allowing bulk move to Trash.
- [x] Subfolder multi-select and photo multi-select are strictly mutually exclusive.
- [x] Photo multi-select within folders reveals contextual action bar with Delete, Rename (single only), Move to folder, and Color label.
- [x] Search result tap navigates directly to source folder with subfolder tab activated, auto-scrolls to target cell, and applies 3-second non-blocking translucent highlight.
- [x] Deleted photos, subfolders, and folders move to Trash with 30-day retention and orphan restore handling.
- [x] Onboarding provides camera/storage pre-permission screen and optional starter subject folders.
- [x] Photo viewer includes 90° manual rotation and manual re-crop replacing stored version.
- [x] Search filters (Date Range, Color Label) dock above keyboard-docked search bar with recent search suggestions.
- [x] "Due tomorrow" home-screen widget renders deadlines or clean empty state; notifications provide `[View Note]` action.
- [x] UI layouts scale responsively for tablet/landscape and respect system font scaling.
- [x] Folders can be locked via PIN or BiometricPrompt with generic locked preview and device credential fallback.
- [x] Settings screen provides all 7 grouped sections with database export/import and search rebuild.
- [x] PhotoGroup entity and database schema with `group_id` foreign key on photos.
- [x] Multi-select "Group" action enabled strictly for 2+ selected photos with mandatory naming dialog.
- [x] Group cell renders cover photo, name, stacked badge count, and tag color pill in grid.
- [x] Group sort order handles upload date (`createdAt`), nearest deadline (earliest member), and color label (`tagColor`).
- [x] Group long-press menu includes Rename, Ungroup, Delete (Trash confirmation), Color label, Select, and Add photos.
- [x] Multi-select supports mixed selection of groups and photos with branching Rename and unified Delete/Move/Color.
- [x] "Add photos" allows folding additional standalone photos into existing group.
- [x] Group name indexed in search with direct navigation and 3-second dimmed highlight on group cell.
- [x] PDF export includes group member photos in sequence with group title section header.
- [x] v1.1 Addendum 6 zoom-gated swipe navigation implemented across all popup slider and inspector contexts with double-tap reset and pinch-to-zoom.
- [x] v1.1.1 Bug A: Photo grid column count live-updates from Settings thumbnail grid density with additive baseline scaling for tablet/landscape.
- [x] v1.1.1 Bug B: Subfolder tab long-press reliably triggers unified context menu (Rename, Delete to Trash, Select) and enters subfolder multi-select with bulk delete.
- [x] v1.1.1 Bug C: Horizontal swipe gestures advance photos when unzoomed (1.0x scale) across all viewer contexts without gesture interception.
- [x] v1.1.1 Addendum 7: Tapping group cell navigates to dedicated fullscreen Group screen with flat member grid, order-added sorting, [+] Add photos button, overflow menu, and scoped popup viewer with single-member auto-dissolve.
- [x] v1.1.1 Addendum 7 Edge Case: Chained search highlight for grouped member photos highlights group cell waypoint (~1s) then auto-navigates to group screen with 3s member photo highlight.


