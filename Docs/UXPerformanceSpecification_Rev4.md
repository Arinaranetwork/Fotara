# Fotara — UX & Performance Specification (Revision 4)
**Project**: Fotara — Android Photo Organization App  
**Document**: UX & Performance Specification  
**Revision**: 4.0 (Supersedes and consolidates Revisions 1, 2, and 3)  
**Date**: 2026-09-23  
**Status**: Authoritative Architectural & Interaction Specification  

---

## 1. Document Identity & Overview

Fotara is an offline-first Android application designed for high school and university students who capture photos of coursework, lecture slides, assignments, and whiteboard notes across multiple concurrent academic subjects. Fotara replaces the disorder of unorganized camera rolls by providing structured subject folder cards, instant subfolder navigation, and on-device Optical Character Recognition (OCR) indexing that makes photographed handwritten and printed text searchable in real time.

This specification establishes the authoritative, sequential user experience (UX) and performance architecture for Fotara. It incorporates and supersedes all specifications from Revision 1 (core navigation, top bar ordering, capture triage modal, inline rename), Revision 2 (scaffold bug fixes, keyboard-docked search behavior), and Revision 3 (pin/unpin card management, microcopy cleanup audit, feature proposals). Furthermore, Revision 4 defines the performance architecture required to deliver instantaneous app launch, instantaneous folder opening, and zero-latency search powered by a dedicated full-text index.

---

## 2. Foundational Concepts & Technical Glossary

To prevent ambiguity, the following mobile interaction patterns and architectural concepts are formally defined here upon their first usage:

1. **Long-Press**: A continuous touch gesture where a user maintains fingertip contact on a discrete interactive element for a duration exceeding 500 milliseconds (the standard Android platform threshold defined by `ViewConfiguration.getLongPressTimeout()`). Upon reaching this threshold, the device delivers an instantaneous haptic pulse, transitioning the target into a contextual management or direct-edit mode without occupying screen space with persistent management buttons.
2. **Modal**: An interface state or container element that temporarily interrupts normal interaction with the parent screen. While a modal is active, the underlying screen is visually subdued and non-interactive; the user must explicitly confirm, complete, or dismiss the modal before resuming interaction with background elements.
3. **Popup Slider**: A specialized floating, non-fullscreen modal container that hovers centrally above the current screen (covering 75% of screen height and 90% of screen width). It houses a horizontal pagination pager that allows users to swipe left and right between newly captured or imported photos with center-snapping physics, enabling rapid triage and verification without losing the spatial context of the underlying folder.
4. **Scrim**: A semi-transparent overlay surface (a dark translucent color, specifically 50% opacity black `#00000080`) rendered directly behind a modal or dialog. The scrim visually recesses background elements, establishes depth layering, and signals that underlying controls are temporarily inert.
5. **Indeterminate Progress Bar**: A visual progress track utilized when the duration or exact item count of an asynchronous computational task cannot be calculated in advance. Rather than showing a 0%–100% fill, it animates a sliding segment across a fixed-height track to communicate active background processing.
6. **Context Menu**: A transient floating surface anchored directly to a specific UI component (such as an individual folder card) triggered exclusively by a secondary gesture (such as a long-press). It presents a focused list of contextual actions scoped strictly to the selected component.
7. **Kebab Menu (Overflow Menu)**: A compact action button represented by three vertically stacked dots (`⋮`). It houses secondary, administrative, browsing, or destructive utilities that are relevant to the current screen but omitted from the primary navigation bar to prevent visual clutter.
8. **Search Index (Full-Text Search / FTS)**: A dedicated, pre-computed inverted index data structure (implemented via SQLite FTS5) that tokenizes and maps every word occurring in photo captions, OCR-extracted text, folder titles, and subfolder names directly to its record identifier. Querying an FTS index executes in logarithmic time ($O(\log N)$) through token lookup rather than linearly scanning raw text characters across table rows ($O(N)$).
9. **Reactive Query / Data Stream**: An asynchronous data pipeline (implemented via Kotlin Coroutines `Flow` within Room) where the user interface observes a query stream. The stream immediately emits the first available result subset to the screen upon subscription, and automatically pushes differential updates whenever the underlying database records change, without requiring manual page reloads or blocking UI execution.
10. **Chunked Pagination**: A data retrieval and rendering pattern where a large dataset is partitioned into fixed-size batches (chunks) of uniform length (e.g., 24 photo items per chunk). The application queries and renders only the initial chunk required to populate the active viewport, deferring subsequent chunks until requested.
11. **Prefetching**: An automated background retrieval mechanism that monitors scroll depth. When the user scrolls past a designated threshold (such as 75% of the currently rendered chunk), the system asynchronously requests the next sequential chunk from the local database before the user reaches the bottom, eliminating scroll pauses.

---

## 3. Defective Scaffold Corrections

During the audit of the initial application scaffold, two structural interaction defects were identified in the bottom navigation and search docking regions. These are formally cataloged below as corrections against a defective scaffold rather than product feature changes.

### 3.1 Bug A Correction: Search Bar Visual Obstruction & Elevation Stacking Hierarchy
- **Defect in Defective Scaffold**: On various device aspect ratios and gesture navigation configurations, the floating bottom search dock was partially occluded or intersected by the Android system navigation bar, gesture inset pill, or adjacent action rows.
- **Root Cause**: The scaffold container failed to consume window insets (`WindowInsetsCompat.Type.navigationBars()`) and lacked a defined Z-index elevation hierarchy.
- **Authoritative Specification Correction**:
  1. **Strict 5-Tier Elevation Stacking Order**:
     - *Layer 0 (Base Canvas, Z = 0dp)*: Root background canvas (`MidnightNavy` `#03071E`) and scrollable subject folder grid.
     - *Layer 1 (Section Banners, Z = 8dp)*: Status overview indicators, including the "Due tomorrow" and "Added today" coursework ribbons.
     - *Layer 2 (System Window Insets, Z = 12dp)*: Android system navigation bar and gesture exclusion zones.
     - *Layer 3 (Resting Search Dock, Z = 16dp)*: The resting floating search dock is assigned a strict elevation of `16.dp`, with mandatory bottom padding defined as `WindowInsets.navigationBars + 16.dp`. It floats immutably above all background content and gesture zones.
     - *Layer 4 (Modals, Overlays, & Repositioned Search, Z = 24dp)*: Active keyboard-docked search surface, Popup Slider review modal, and confirmation dialogs.
  2. **Stacking Rule**: Under no circumstances may the search bar share elevation with or sit underneath any action row, navigation button, or gesture bar. It must always render as a discrete floating capsule above all content and navigation surfaces.

### 3.2 Bug B Correction: Tap-Target Decoupling for Search vs. Folder Creation
- **Defect in Defective Scaffold**: Tapping the search bar container triggered the "+New Subject Folder" creation dialog instead of activating search input mode.
- **Root Cause**: Erroneous event listener delegation in the scaffold composable, where the search dock's outer bounding box was bound to the folder creation listener.
- **Authoritative Specification Correction**:
  1. **Single-Purpose Search Target**: The search dock's sole and exclusive function upon tap is to activate the search input state, reposition directly above the software keyboard, and focus the text input field.
  2. **Dedicated Folder Creation Targets**: Folder creation is strictly bound to dedicated creation controls:
     - The explicit "+New Folder" card situated as the terminal element in the home screen grid.
     - The "+ Subfolder" creation chip located in the folder detail screen tab row.
  3. **Mandatory Prohibition**: The search bar tap surface and the folder creation trigger must never share tap targets, bounding boxes, or click listeners.

---

## 4. Performance Architecture: Instant Open & Index-Backed Search

To fulfill the requirements of an academic utility app used between lectures, Fotara enforces hard latency ceilings across app startup, folder exploration, and search. This section specifies the architectural mechanisms that guarantee these latencies at scale, treating the index-backed approach as a fixed architectural requirement and explicitly prohibiting raw text scanning at query time.

### 4.1 Target Latency Benchmarks
The application must adhere to the following verified performance benchmarks on standard Android mid-range hardware (e.g., devices equivalent to Snapdragon 6-series, 4GB RAM):

| Action | Latency Target | Architectural Mechanism |
| :--- | :--- | :--- |
| **App Cold Start** (tap icon → usable home) | **< 1.5 seconds** | Non-blocking initialization, reactive Room query streaming, zero synchronous disk reads on main thread. |
| **App Warm Start** (background → usable home) | **< 500 milliseconds** | State restoration from ViewModel cache, instant memory render. |
| **Folder Open** (tap card → first photos rendered) | **< 300 milliseconds** | Scoped query, chunked pagination (first 24 items), downscaled disk-cached thumbnail rendering. |
| **Search Query** (keystroke → first result rendered) | **< 16 milliseconds** (bounded solely by FTS index lookup) | Dedicated SQLite FTS5 index lookup, prefix matching (`token*`), zero raw-text table scans. |

### 4.2 App Open Architecture (Cold & Warm Start)
1. **Reactive Query Streaming**:
   - The Home Screen does not perform a blocking, synchronous read of the complete database before drawing its first frame.
   - Home screen data (folder entities, pinned status flags, and active deadline timestamps) is loaded via a reactive Room data stream (`Flow<List<Folder>>`).
   - The UI subscribes to this stream and renders immediately upon receipt of the first emitted record set. Subsequent folder updates or background additions push differential updates without re-triggering full screen recomposition.
2. **Main-Thread Computational Ban**:
   - No computationally heavy task—including Optical Character Recognition (OCR), document perspective transformation, bitmap downsampling, or schema migration—is permitted to run on Android’s main UI thread or execute prior to the first rendered frame.
   - All OCR processing and image serialization execute strictly on background coroutine dispatchers (`Dispatchers.Default` / `Dispatchers.IO`).
3. **Legitimate Splash Screen Policy**:
   - A splash/loading screen is legitimate solely while waiting for the Android OS window surface to initialize and the initial reactive query emission to complete.
   - The splash screen must never be used to mask an unoptimized or slow query. If first-frame rendering exceeds 1.5 seconds, the underlying query and database indexing must be corrected rather than extending the duration of the splash animation.

### 4.3 Folder Open Architecture: Scoped Chunking & Thumbnail Pipeline
1. **Scoped Chunked Pagination**:
   - Opening a subject folder queries data scoped exclusively to that folder identifier (and active subfolder tab), retrieving items in uniform chunks of **24 photos**.
   - Under no circumstances is an entire folder containing hundreds of high-resolution photos loaded into memory simultaneously.
2. **Predictive Prefetching**:
   - When the user scrolls through the photo grid and reaches **75% of the current chunk** (photo index 18 of 24), a background prefetch coroutine queries the next 24 items.
   - By the time the user scrolls to item 24, the subsequent batch is already resident in memory, eliminating scroll stutter.
3. **Thumbnail-Only Grid Rendering**:
   - Grid cells render pre-computed, downscaled thumbnail images (`180x180dp`, encoded as 80% quality WebP or JPEG cached in local app storage).
   - Grid cells are strictly prohibited from decoding or rendering full-resolution original camera files (which often exceed 12 megapixels and 5MB per file).
   - This ensures memory consumption and GPU draw call costs remain constant ($O(1)$) regardless of individual camera resolution or folder size. Full-resolution originals are decoded solely when opening the dedicated Full-Screen Note Inspector.

### 4.4 Search Architecture: Dedicated FTS Index vs. Raw-Text Scan Prohibition
1. **The Failure of Raw-Text Scanning**:
   - Scanning raw OCR text columns using SQL pattern matching (`LIKE '%query%'`) requires full table scans across multi-kilobyte text fields on every keystroke. As a student's library expands past several hundred pages, query latencies rapidly degrade from tens of milliseconds to seconds, causing keyboard input lag and battery drain.
   - **Architectural Requirement**: Scanning raw text columns at query time is strictly prohibited.
2. **Dedicated Full-Text Search (FTS) Index**:
   - The database maintains an independent SQLite FTS5 virtual table (`photos_fts_index`) utilizing the Porter tokenizer or unicode61 tokenizer.
   - Indexed fields include: `photo_id`, `folder_name`, `subfolder_name`, `user_caption`, and `ocr_extracted_text`.
   - All search queries hit this FTS5 virtual table exclusively:
     ```sql
     SELECT photo_id, folder_name, subfolder_name, user_caption, ocr_extracted_text
     FROM photos_fts_index
     WHERE photos_fts_index MATCH :queryTokenPrefix
     LIMIT 24 OFFSET :offset;
     ```
3. **Atomic Index Update Timing**:
   - Index records are written at the exact moment their underlying source data is committed:
     - When background OCR completes, the extracted text is written simultaneously to the master `PhotoEntity` and the `photos_fts_index` within an atomic database transaction.
     - When a folder, subfolder, or photo caption is renamed, the corresponding FTS index entry is updated in the same transaction.
   - The search index is never updated lazily, periodically, or on a delayed cron schedule.
4. **Query Pagination**:
   - Keystrokes query the index returning matching item references, which are chunked and prefetched using the identical 24-item pagination pattern used in folder views.
5. **Exact Snippet-Extraction Rule**:
   - To provide immediate context explaining why a photo matched the query, search results display a two-line contextual snippet extracted from the OCR text.
   - **Rule**:
     1. Locate the character offset of the first matched token within `ocr_extracted_text`.
     2. Extract exactly **45 characters before** the match offset and **45 characters after** the end of the matched token.
     3. Snap both the start and end boundaries outward to the nearest whitespace boundary so words are never truncated mid-character.
     4. Prepend leading ellipsis (`...`) if the snippet starts after the beginning of the text, and append trailing ellipsis (`...`) if the snippet ends before the conclusion of the text.
     5. The matched keyword within the snippet is formatted in bold typography (`FontWeight.Bold`, color `FolderBodyBlue` `#0316A8` / dark mode `FolderTabCream` `#EAE3D2`).
     6. If a match occurs on the folder or subfolder name rather than OCR text, suppress the OCR snippet and display the folder breadcrumb path (e.g., `Biology > Cell Structure`).
6. **Search Result Viewer Paging Scope Resolution**:
   - Tapping an individual search result opens the Popup Slider viewer.
   - **Required Decision**: Swiping left and right inside the viewer when launched from a search result **pages exclusively through the active search result set**.
   - *Rationale*: Paging through the parent folder would violate the student's immediate browsing context, forcing them to encounter irrelevant photos that did not match their search query. Scoping navigation to the search results preserves their investigative focus.

### 4.5 Resolution of Search Loading Indicator
Revision 2 introduced an indeterminate horizontal progress bar styled after video buffering indicators (a thin track with a moving highlight segment). Because search queries now hit an indexed FTS5 table executing in under 16ms, displaying this indicator on every keystroke causes distracting visual flicker.

- **Authoritative Resolution**:
  - The video-buffering progress bar is **completely hidden during standard search queries**.
  - It renders **exclusively during defined slow-path conditions**:
    1. During a cold-start first-run index build or database migration.
    2. During background self-healing when index corruption is being repaired.
    3. On low-tier legacy hardware where an index query measurably exceeds **100 milliseconds**.
  - Under all other circumstances, results render instantly without flashing the progress track.

### 4.6 Index Maintenance Edge Cases & Self-Healing
1. **Photo Deletion**:
   - When a photo is deleted from a folder, a cascading SQLite foreign key trigger or repository transaction immediately deletes the matching row from `photos_fts_index`. Deleted photos can never appear as phantom search results.
2. **OCR Failure Searchability**:
   - If OCR processing fails due to extreme blur, corrupted image bytes, or absence of text, the photo remains indexed and fully searchable by its folder name, subfolder name, user caption, and timestamp.
3. **Corrupted or Out-of-Sync Index Self-Healing**:
   - The application maintains an index integrity checksum. If an SQLite FTS error occurs or a query returns a mismatched ID, the repository triggers an automated, silent background rebuild: it clears `photos_fts_index` and repopulates it in batches from `PhotoEntity` using a low-priority background thread.
   - For administrative diagnostics, an explicit "Rebuild Search Index" button is provided in Settings as a manual recovery path.

---

## 5. Top Bar Architecture & Component Layout

Within any folder or subfolder view, the screen top bar establishes navigational context, content labeling, and primary actions.

```
+-----------------------------------------------------------------------------+
| [← Back]  Biology                                      [+]  [ (⋮) Overflow ]|
+-----------------------------------------------------------------------------+
```

### 5.1 Spatial Ordering (Left to Right)
1. **Back Navigation**: Leftmost position (`48x48dp` touch target, icon: left arrow `←`). Returns the user to the parent home screen or parent folder.
2. **Folder Name & Color Indicator**: Positioned adjacent to the back button, expanding to occupy available horizontal space (`weight(1f)`). Truncates with ellipsis if the title exceeds single-line boundaries.
3. **Add Photo Action Button (`+`)**: Positioned on the right side, immediately preceding the overflow menu.
4. **Highlighted Overflow Action (`⋮`)**: Positioned at the far right terminal edge (`16.dp` right margin).

### 5.2 Ordering Rationale
- **Primary vs. Terminal Placement**: In Western reading order (LTR), the far-right corner is standard for terminal overflow menus across modern Android applications (Material 3 TopAppBar pattern). Placing the Add Photo (`+`) button immediately to the left of the overflow menu keeps primary content creation grouped logically within the right thumb's natural reach zone while preserving the expected far-right location for menu settings.
- **Highlighted / Filled Overflow Menu State**: Unlike standard outline icons, the three-dot kebab icon is housed within a slightly elevated, tinted container (a `36x36dp` circular chip filled with `DockSlatePill` `#4A4C68` at 40% opacity with a pure white icon). This visual elevation communicates to students that this button is not merely an OS setting, but contains core coursework utilities: PDF export, batch organization, and folder color customization.

### 5.3 Add Photo Action Behavior (`+`)
- **Interaction Decision**: Tapping the `+` button displays an immediate modal action sheet anchored to the bottom of the screen with two distinct options:
  1. **Open Multi-Capture Camera** (Default / Prominent option): Launches Fotara’s high-speed document capture mode with automatic perspective cropping.
  2. **Import from Gallery**: Opens the Android photo picker for existing note photos.
- **Justification Against Direct Camera Launch**: While direct camera launch saves one tap, Fotara specifically supports importing existing screenshots and reference slides students take with their native camera app throughout the day. Presenting a zero-latency bottom sheet (with Multi-Capture pre-selected as the primary action and hardware camera access immediate) accommodates both behaviors without locking the user out of bulk imports.

### 5.4 Overflow Menu Contents & Justification
Tapping the highlighted overflow button opens a dropdown menu containing:
1. **Sort Order**: Sub-menu to toggle between *Upload Date (Newest/Oldest)*, *Nearest Deadline*, and *Color Label*.
   - *Rationale*: Sorting is a browsing preference; keeping it off the main bar prevents visual noise.
2. **Export Folder to PDF**: Compiles all photos in the current folder/subfolder into a single sequential document.
   - *Rationale*: An infrequent, high-impact export operation that should not occupy a primary top-bar slot.
3. **Batch Select Mode**: Enables multi-photo checkboxes for bulk moves, bulk tagging, and bulk deletion.
   - *Rationale*: Enters an alternate screen editing mode.
4. **Folder Color Label**: Opens a color picker dialog to modify the folder's tag indicator.
   - *Rationale*: Administrative property editing.
5. **Rename Folder**: Alternate access path to the inline long-press rename flow.
   - *Rationale*: Accessibility fallback for users unable to perform long-press gestures.
6. **Delete Folder**: Destructive action (colored `TagCrimson`, prompts confirmation dialog).
   - *Rationale*: Destructive operations must always reside safely behind a secondary menu.

---

## 6. End-to-End Sequential Interaction Flows

Each interaction flow is specified using the mandatory sequence:  
**Trigger → Visual State Change → Available User Actions → Exit / Cancel Paths**.

---

### Flow 1: Home Screen on App Open
- **Trigger**: App cold start, warm start from background, or returning to root via back navigation.
- **Visual State Change**:
  - Deep dark navy background (`MidnightNavy` `#03071E`) initializes.
  - The top header renders the bold wordmark **Fotara** alongside a quick status summary (e.g., `"2 assignment notes due tomorrow"` inside an amber-accented pill).
  - Below the header, a 2-column scrollable grid renders subject folders styled as custom rounded-rectangle cards with cream tabs.
  - Pinned folders display a distinct pin icon in their upper tab and always occupy the top positions in the grid.
  - The final item in the grid is the **+New Folder** card, rendered with a translucent dark body, cream tab, and dashed border.
  - Floating above the bottom navigation margin rests the **Search Bar Dock** (Z-index 16dp, immune to Bug A obstruction).
- **Available User Actions**:
  1. *Tap a Folder Card*: Navigates into Flow 2 (Opening a Folder).
  2. *Long-Press on Folder Name Text*: Navigates into Flow 3 (Renaming a Folder).
  3. *Long-Press on Folder Card Canvas (Excluding Name Text)*: Navigates into Flow 8 (Pin/Unpin Management).
  4. *Tap "+New Folder" Card*: Opens the New Subject Folder creation modal dialog.
  5. *Tap Search Bar Dock*: Navigates into Flow 7 (Search Interaction).
  6. *Tap Camera Icon in Dock*: Launches instant multi-capture camera.
- **Exit / Cancel Paths**:
  - System back gesture from root exits the application to Android home.

---

### Flow 2: Opening a Folder
- **Trigger**: Tapping any folder card from the home screen grid.
- **Visual State Change**:
  - Fluid shared-axis transition: the folder card expands into the full screen while the top bar morphs to display the back arrow, folder name, color badge, `+` button, and highlighted overflow menu.
  - Directly beneath the top bar, a horizontal scrollable tab row slides into view, showing `"All Notes"` (active by default) followed by each defined subfolder (e.g. `"Lectures"`, `"Assignments"`, `"Lab Reports"`) and an inline `"+ Subfolder"` chip.
  - Below the tab row, a 2-column grid loads the folder’s photo notes via chunked pagination. Each photo card displays an image thumbnail, manual caption or OCR excerpt, timestamp, and optional deadline indicator.
  - If the folder is empty, a clean centered illustration appears with the copy: *"No notes in this folder yet. Tap + to capture handwritten notes or slides."*
- **Available User Actions**:
  1. *Tap a Subfolder Tab*: Filters photo grid to items filed under that subfolder.
  2. *Tap "+ Subfolder" Chip*: Opens an alert dialog to name and create a new subfolder tab.
  3. *Long-Press a Subfolder Tab Title*: Activates inline renaming for that subfolder tab (Flow 3).
  4. *Tap Photo Card*: Navigates into Flow 5 (Viewing an Existing Photo in Full-Screen Note Inspector).
  5. *Tap `+` Button*: Enters Flow 4 (Adding a Photo End-to-End).
  6. *Tap Overflow Menu (`⋮`)*: Opens dropdown menu (Flow 6).
- **Exit / Cancel Paths**:
  - Tapping the Top Bar Back Arrow (`←`) or triggering the system back gesture transitions smoothly back to the Home Screen.

---

### Flow 3: Renaming a Folder or Subfolder (Inline Rename)
- **Trigger**: Long-press directly on the folder or subfolder name text (duration > 500ms). Holding the rest of the card does NOT trigger rename.
- **Visual State Change**:
  - *Haptic Feedback*: A single tactile haptic pulse (`HapticFeedbackType.LongPress`) is fired immediately upon registering the 500ms threshold.
  - *Visual Transition*: The static text label crossfades into an active inline editable text field.
  - The entire current text string is automatically selected (highlighted in brand royal blue), and a vertical cursor appears at the end.
  - The software keyboard deploys automatically with the action key set to `Done` / `Check`.
  - A glowing outline (`FolderBodyBlue` `#0316A8`) wraps the text field to signal edit mode.
- **Available User Actions**:
  1. *Type new name*: Replaces the highlighted text or modifies individual characters.
  2. *Commit via Keyboard*: Tapping the keyboard `Done`/`Enter` checkmark commits the new name immediately.
  3. *Commit via Tap Outside*: Tapping anywhere outside the active text box commits the current input.
- **Exit / Cancel Paths**:
  - *System Back Button / Gesture*: Dismisses the keyboard and cancels editing without saving, restoring previous name.
  - *Empty Text Reversion*: If user clears all characters and attempts to commit, operation cancels, reverts to previous name, and displays a transient tooltip: *"Folder name cannot be empty."*

---

### Flow 4: Adding a Photo End-to-End (Capture → Popup Slider → Confirm)
- **Trigger**: Tapping the `+` action button in folder top bar, selecting "Multi-Capture Camera", and capturing one or more note photos.
- **Visual State Change (Popup Slider Activation)**:
  - The camera viewfinder closes, returning to the folder screen.
  - A dark translucent scrim (50% black `#00000080`) dims the entire background folder screen.
  - The **Popup Slider Modal** animates upwards from the bottom, settling as a floating card covering approximately **75% of screen height and 90% of screen width**.
  - The underlying folder view remains partially visible around the margins and top behind the dimmed scrim, preserving navigational context.
  - Inside the Popup Slider:
    - *Header*: Displays batch count badge (e.g. `"Review: 3 notes captured"`), an individual photo discard button (trash icon), and an edit perspective crop icon.
    - *Body*: A horizontal pager displaying captured photos. Swiping left/right flips between captured pages with snap-to-center physics.
    - *Footer*: A prominent primary button labeled **"Save to [Folder Name]"**, with an optional secondary toggle: *"Apply auto-suggested subfolder"*.
- **Available User Actions**:
  1. *Swipe Left / Right*: Page through captured notes to inspect sharpness and automatic straightening.
  2. *Tap Discard Icon on a Page*: Removes that specific photo from batch. If only one photo remains and is discarded, popup slider closes automatically.
  3. *Tap Perspective Crop Icon*: Opens quick quadrilateral corner adjustments if whiteboard edges were skewed.
  4. *Tap "Save to [Folder Name]"*: Persists all remaining photos into database, writes atomic FTS index entries, triggers asynchronous background OCR processing, dismisses modal, and inserts cards into grid with animated entry.
- **Exit / Cancel Paths**:
  - *Tap Scrim or System Back*: Triggers confirmation prompt: *"Discard all captured notes?"* with options *Discard* and *Keep Reviewing*. Tapping *Discard* aborts without saving.

---

### Flow 5: Viewing an Existing Photo from the Grid
- **Trigger**: Tapping an existing photo card within the folder grid.
- **Visual State Change & Architectural Decision**:
  - **Does NOT reuse the Popup Slider**. Instead, it opens a dedicated **Full-Screen Note Inspector**.
  - *Rationale*: The Popup Slider is intentionally designed for rapid, low-friction *capture triage* (confirming readability and discarding blurs before persisting). In contrast, viewing an *existing* note requires reading handwritten text, inspecting fine details with multi-touch pinch-to-zoom, copying indexed OCR text, adjusting linked deadlines, and exporting. A 75% popup card is too constrained for studying high-density note photos.
  - The Full-Screen Note Inspector provides:
    - Deep multi-touch pinch-to-zoom and double-tap zoom.
    - Bottom expandable sheet revealing full recognized OCR text with a "Copy All Text" button.
    - Attached deadline badge with quick reminder rescheduling.
- **Available User Actions**:
  1. *Pinch / Pan*: Zoom into handwritten diagrams and whiteboard equations.
  2. *Swipe Up OCR Panel*: Inspect and copy searchable text.
  3. *Tap Edit / Move*: Re-assign to a different subfolder or tag color.
  4. *Tap Share / Export*: Export image as PNG or PDF.
- **Exit / Cancel Paths**:
  - Tapping top-left back button or performing vertical pull-down dismiss gesture closes inspector and returns to folder grid.

---

### Flow 6: Overflow Menu Interaction
- **Trigger**: Tapping highlighted kebab menu button (`⋮`) on right side of folder top bar.
- **Visual State Change**:
  - An elevated Material 3 dropdown card smoothly cascades down from anchor button (Z-index 24dp).
  - Background is styled in `MidnightSurface` (`#070D2B`) with a subtle `MidnightCardOutline` border.
  - The 6 defined menu items (Sort Order, Export to PDF, Batch Select, Folder Color, Rename, Delete) are rendered with high-contrast text and leading icons.
- **Available User Actions**:
  1. *Select "Sort Order"*: Expands inline sort options (*Newest, Oldest, Nearest Deadline*). Selecting an option updates grid and dismisses menu.
  2. *Select "Export Folder to PDF"*: Opens progress sheet showing pages compiling into a PDF, followed by system share sheet.
  3. *Select "Batch Select Mode"*: Dismisses menu, updates top bar to show selected count (`"0 selected"`), and renders circular checkboxes on all photo cards.
  4. *Select "Folder Color Label"*: Displays dialog with Fotara’s 6 brand color swatches to update folder’s tag indicator.
  5. *Select "Rename Folder"*: Programmatically triggers Flow 3 (inline renaming).
  6. *Select "Delete Folder"*: Displays destructive confirmation dialog (*"Delete [Folder Name] and all its photos? This action cannot be undone."*).
- **Exit / Cancel Paths**:
  - Tapping anywhere outside dropdown menu card or pressing system back button immediately dismisses menu without action.

---

### Flow 7: Search Interaction (Revision 2 + Revision 4 Resolution)
- **Trigger**: User taps directly on resting search dock on home screen.
- **Visual State Change & Activation Repositioning**:
  - As software keyboard deploys, search bar smoothly translates from bottom resting dock to sit **directly docked above top edge of keyboard** (tracking `WindowInsets.ime`).
  - *Transition*: Fluid spring animation (duration: 250ms, damping ratio: 0.82).
  - Resting home screen folder grid is overlaid by search results canvas (`MidnightNavy` at 95% opacity).
  - Video-buffering progress bar remains completely hidden during instantaneous FTS queries (< 16ms).
- **Available User Actions & Real-Time Querying**:
  1. *0 Characters (Empty State)*: Shows recent search history chips and frequently searched subjects.
  2. *1+ Characters*: Instant keystroke matching against SQLite FTS5 index. Results populate dynamically with no submit button and no perceptible debounce:
     - Exact folder name matches (miniature folder cards).
     - Photo OCR matches: Displays thumbnail, folder breadcrumb, and a 2-line snippet with matched term bolded per the 45-character extraction rule.
  3. *No-Match State*: Displays clean message: *"No notes found matching '[query]'. Check spelling or search by general topic."*
  4. *Tap Clear Button (`✕`)*: Clears query string and restores 0-character state while keeping keyboard open.
  5. *Tap Search Result*: Opens Popup Slider viewer scoped strictly to the returned search result set (Flow 4 viewer pattern).
- **Exit / Cancel Paths**:
  - *Explicit Back Arrow*: Tapping `←` on search bar dismisses search and returns bar to bottom dock.
  - *System Back Gesture*: Dismisses keyboard and exits search.
  - *Query Persistence*: Exiting search and reopening within 30 seconds restores previous query and result list.

---

### Flow 8: Pin / Unpin Management on Home Screen (Revision 3)
- **Trigger**: Long-pressing a folder card on the home screen anywhere on the card canvas **except** the folder name text (which is reserved for inline rename).
- **Visual State Change**:
  - A tactile haptic pulse (`HapticFeedbackType.LongPress`) is delivered at 500ms.
  - A floating Card-Level Context Menu opens anchored to the top-right corner of the pressed card, accompanied by a subtle 30% background scrim.
  - The context menu options depend on the folder's current pin state:
    - *If Folder is Currently Pinned*: Menu displays **"Unpin from Top"** (icon: unpin icon).
    - *If Folder is NOT Pinned*: Menu displays **"Pin to Top"** (icon: pin icon).
- **Available User Actions**:
  1. *Select "Pin to Top"*: Marks folder as pinned in database. Card smoothly animates into the top pinned section using a spring reordering transition.
  2. *Select "Unpin from Top"*: Clears pinned flag. Card smoothly animates out of pinned section into its standard alphabetical or recent sorted position.
  3. *Select Secondary Item "Folder Color"*: Allows quick assignment of a color tag without opening folder.
- **Exit / Cancel Paths**:
  - Tapping anywhere outside context menu or pressing system back dismisses menu without modifying pin state.

---

## 7. Microcopy Cleanup Audit Register (Revision 3 Directive)

To eliminate visual clutter and ensure an uncluttered, premium academic aesthetic, all static reassurance badges, marketing taglines, and non-actionable labels have been audited.

**Audit Rule**: *If deleting the string changes nothing about what the user understands regarding their current state or what they can do next, delete it.* Genuinely instructive empty states and permission explanations are retained.

| Screen / Component | Original String | Action Taken | Architectural Justification |
| :--- | :--- | :--- | :--- |
| **Home Header** | `"Fotara — 100% Offline Note Organizer"` | **Trimmed** to `"Fotara"` | Static "100% Offline" badge adds marketing noise; offline behavior is an inherent product architecture, not a daily label. |
| **Home Subtitle** | `"Your study notes, slides, and coursework folders"` | **Removed** | Redundant tagline; visible folder cards immediately communicate purpose. |
| **Folder Card Header** | `"Subject Folder"` | **Removed** | Superfluous metadata; card design and title already communicate subject entity. |
| **New Folder Dialog** | `"Create a space for your notes and study guides"` | **Removed** | Unnecessary instructional reassurance; header `"New Folder"` and text input are self-evident. |
| **Empty Folder State** | `"You don't have any notes here yet. Don't worry, your notes are safely stored offline when added!"` | **Replaced** with *"No notes in this folder yet. Tap + to capture handwritten notes or slides."* | Removed empty reassurance ("Don't worry..."); preserved clear, actionable instruction pointing to `+` button. |
| **Capture Review Modal** | `"All captured photos will be analyzed with on-device OCR technology"` | **Removed** | Developer-facing implementation detail; student needs only the primary action `"Save to [Folder]"`. |
| **Search Bar Placeholder** | `"Search your offline notes, subjects, OCR text, and diagrams..."` | **Trimmed** to `"Search notes, subjects, text..."` | Concise, scannable placeholder fitting within smaller mobile screen widths without truncation. |
| **Photo Viewer Bottom Sheet** | `"OCR Text successfully extracted from image locally"` | **Trimmed** to `"Extracted Text"` | Eliminated redundant confirmation status; simplified to clear section header. |

---

## 8. Feature Proposals Register (Revision 3)

The following four items from Revision 3 are explicitly classified as **Proposals for Future Evaluation**, not decided baseline requirements:

### Proposal 1: Auto-Deadline Suggestion via Regex Pattern Matching
- **Concept**: As OCR extracts text from whiteboard photos or syllabus sheets, an on-device regex evaluator scans for date patterns (e.g., `"Due Oct 14"`, `"Assignment 2: Monday 5pm"`, `"Midterm: 11/04"`).
- **Proposed UX**: If a high-confidence date pattern is detected, the Popup Slider review modal displays an actionable chip: `"+ Link Deadline: Oct 14"`. Tapping it attaches the deadline to the photo with one touch.
- **Evaluation Status**: Proposal only. Requires testing false-positive rates on historical dates mentioned in lecture slides before committing to production.

### Proposal 2: Recent Search Terms in Zero-Character State
- **Concept**: When the search bar is activated but the query input is empty (0 characters), display the student's last 5 unique search queries as dismissable chips directly beneath the search bar.
- **Evaluation Status**: Proposal only. Clean baseline implementation displays "Frequently Searched Subjects" first; query history logging will be evaluated following privacy review.

### Proposal 3: Folder Preview Thumbnail via Most Recent Photo
- **Concept**: Instead of rendering a solid royal blue card body (`FolderBodyBlue`), display a dimmed, blurred, or cropped thumbnail of the most recently added note photo inside the folder card.
- **Identified Conflict**: As documented in Section 9 (C-07), rendering a photo thumbnail directly inside the folder card risks clashing with the high-contrast italic title typography and the top-right color label indicator.
- **Evaluation Status**: Proposal only. Requires design system review to ensure thumbnail integration does not compromise the brand card aesthetic.

### Proposal 4: Android Home-Screen Widget for "Due Tomorrow"
- **Concept**: An Android AppWidget (Glance-based) placed on the student’s operating system home screen, displaying a compact list of notes tagged with deadlines occurring within the next 24–48 hours.
- **Evaluation Status**: Proposal only. Scheduled for architecture review in Phase 3.

---

## 9. Architectural Conflict Register

The following table formally catalogs all cross-revision architectural and interaction conflicts, providing an explicit, authoritative resolution for each:

| Conflict ID | Conflict Description | Authoritative Resolution |
| :--- | :--- | :--- |
| **C-01: Top vs. Bottom Search Bar Placement** | The product brief notes a search bar at the top of the home screen, whereas UI mockups and Revisions 2/4 mandate a bottom floating dock. | **Resolved**: Resting search bar is docked at the bottom of the home screen (Z=16dp). Upon tap, it animates smoothly to dock directly above the software keyboard, providing optimal thumb ergonomics while preserving edge-to-edge content visibility. |
| **C-02: Direct Camera vs. Add Photo Chooser** | Rapid capture favors instant camera launch on `+` tap, while student workflows require gallery screenshot imports. | **Resolved**: Tapping `+` displays a zero-latency bottom sheet with *Multi-Capture Camera* as the primary prominent option and *Import from Gallery* as the secondary option. |
| **C-03: Subfolder Navigation Depth** | Tabs vs. breadcrumbs for nested subfolders. | **Resolved**: Horizontal scrollable tabs are used for primary subfolders (depth 1). Deeper nested subfolders (depth 2+) surface an inline breadcrumb bar beneath the tabs. |
| **C-04: Card Click vs. Rename Gesture** | Whole-card press could conflict with card renaming. | **Resolved**: Tapping the card opens the folder. Long-pressing the card canvas opens the Pin/Unpin context menu. Long-pressing *directly on the name text label* triggers inline text renaming. |
| **C-05: Pin / Unpin Long-Press Symmetry Gap** | Revision 3 specified long-pressing a pinned folder to unpin, but left unpinned folders unspecified. | **Resolved**: Strict symmetry enforced. Long-pressing any unpinned folder card (excluding name text) opens the same context menu displaying "Pin to Top". Cards animate smoothly between pinned and unpinned grid tiers via spring physics. |
| **C-06: Search Result Viewer Paging Scope** | Tapping a search result opens the Popup Slider viewer; swiping could either browse search matches or all photos in the parent folder. | **Resolved**: Swiping inside the viewer when opened from search is strictly scoped to the active search result set, maintaining investigative context. |
| **C-07: Folder Preview Thumbnail vs. Brand Color Indicator** | Proposal 3 photo thumbnails could obscure title legibility and clash with brand color tags. | **Resolved**: Held as proposal only. The production folder card retains the solid royal blue body (`#0B1BE0`), cream tab (`#EAE3D2`), and dedicated top-right color label. |

---

## 10. Proposed Default Micro-Parameters

Where specific micro-interaction parameters were not fixed by platform standards, the following values are formally specified as proposed defaults:

- **Long-Press Activation Duration**: `500ms` (Android `ViewConfiguration.getLongPressTimeout()`).
- **Haptic Pulse Characteristics**: Android `HapticFeedbackType.LongPress` (vibration amplitude: 60ms sharp click).
- **Search Dock Spring Animation**: Duration: `250ms`, Damping Ratio: `0.82`, Stiffness: `Spring.StiffnessMediumLow`.
- **Search Loading Indicator Threshold**: Hidden for queries `< 100ms`; displays indeterminate video-buffering animation only if query latency exceeds `100ms`.
- **Progress Track Dimensions**: Height: `2.5dp`, Background Track Color: `MidnightCardOutline` (`#142055`), Indicator Fill: `FolderBodyBlue` (`#0316A8`).
- **FTS Snippet Extraction**: `45 characters` preceding match, `45 characters` succeeding match, snapped outward to nearest whitespace boundary.
- **Popup Slider Modal Dimensions**: Width: `90%` of screen width, Height: `75%` of screen height, Corner Radius: `24.dp`, Scrim: `Color.Black.copy(alpha = 0.50f)`.
- **Folder Grid Pagination**: Chunk Size: `24 photos` per batch, Prefetch Threshold: `75%` of current chunk scroll depth.
- **Thumbnail Image Cache Specification**: Resolution: `180x180dp`, Format: WebP/JPEG 80% compression, disk cached in app-specific storage.
