# Fotara — UX Interaction Specification (Revision 2)
**Project**: Fotara — Android Photo Organization App  
**Document**: UX Interaction Specification  
**Revision**: 2.0 (Supersedes and incorporates Revision 1)  
**Date**: 2026-09-23  
**Status**: Approved Specification  

---

## 1. Overview & Core Interaction Architecture

Fotara is an offline-first Android application that enables students to organize coursework, notes, whiteboards, and assignments into structured, searchable subject folders with on-device OCR indexing. 

This document defines the complete sequential user experience (UX) interaction specification for Fotara. It incorporates all requirements from Revision 1, specifies mandatory bug corrections identified in the initial application scaffold, establishes the newly revised keyboard-docked search behavior, and resolves architectural conflicts.

### 1.1 Definition of Key Interaction Concepts
To establish an unambiguous baseline, the following interaction patterns are defined:
- **Long-Press**: A continuous touch gesture where the user holds their finger on a target element for a duration exceeding 500 milliseconds (standard Android `ViewConfiguration.getLongPressTimeout()`), accompanied by haptic feedback, used to trigger alternate or contextual management modes without cluttering primary tap targets.
- **Modal**: An interface state or container that temporarily halts interaction with the underlying screen, requiring the user to explicitly confirm, dismiss, or complete an action before interacting with background elements.
- **Popup Slider**: A floating, non-fullscreen modal sheet that rests above the current screen content, displaying horizontally pageable media items that can be swiped left and right.
- **Scrim**: A semi-transparent overlay (dark tinted surface, typically 40%–60% black) rendered directly behind a modal or dialog to visually recede background content and communicate that underlying controls are temporarily inert.
- **Indeterminate Progress Bar**: A continuous animated visual indicator used when the duration or exact item count of a background operation cannot be precomputed; it displays a moving highlight along a track to communicate active computational work.
- **Kebab Menu**: An overflow menu icon represented by three vertically aligned dots (`⋮`), indicating secondary, destructive, or administrative actions related to the current context.

---

## 2. Bug Fixes Against Current Scaffold

During evaluation of the initial application scaffold, two critical interaction defects were identified in the bottom navigation and search docking region. These are formally cataloged here as structural corrections, distinct from new feature specifications.

### 2.1 Bug A Correction: Search Bar Visual Obstruction & Stacking Order
- **Defect in Current Scaffold**: The floating search/dock bar was visually occluded or intersected by the system navigation bar / gesture inset area and adjacent bottom action layers on various screen aspect ratios.
- **Root Cause**: Missing edge-to-edge window inset handling (`WindowInsetsCompat.navigationBars()`) and insufficient z-index hierarchy.
- **Specification Correction**:
  1. **Strict Stacking Order (Z-Index / Elevation)**:
     - *Layer 0 (Base, Z=0dp)*: Screen background (`MidnightNavy`) and scrollable folder card grid.
     - *Layer 1 (Overlay Content, Z=8dp)*: Section banners, header elements, and floating card headers.
     - *Layer 2 (System Window Insets, Z=12dp)*: System navigation scrim/gesture handling zone.
     - *Layer 3 (Floating Search Dock, Z=16dp)*: The floating search container must render at an elevation of `16.dp` with a mandatory bottom padding matching `WindowInsets.navigationBars + 16.dp`.
     - *Layer 4 (Modals, Sliders & Search Expansion, Z=24dp)*: Active keyboard-docked search surfaces, dialogs, and popup sliders.
  2. **Stacking Rule**: Under no condition may the search bar share elevation with or sit underneath any action row, navigation button, or gesture bar. It must always render as a discrete floating capsule above all content and navigation surfaces.

### 2.2 Bug B Correction: Tap-Target Separation for Search vs. Folder Creation
- **Defect in Current Scaffold**: Tapping the search bar container triggered the "+New Subject Folder" creation dialog instead of activating search input.
- **Root Cause**: An erroneous tap-target event binding in the scaffold composable where the search container was assigned the folder creation listener.
- **Specification Correction**:
  1. **Single-Purpose Search Target**: The search bar’s sole and exclusive function on tap is to initiate the search input state, reposition above the software keyboard, and focus the text field.
  2. **Isolated Folder Creation Target**: Folder creation is strictly bound to dedicated creation controls:
     - The explicit "+New" folder card situated in the home screen grid.
     - The secondary action within the folder screen overflow menu (for subfolders).
  3. **Mandatory Prohibition**: The search bar tap surface and the folder creation trigger must never share tap targets, bounding boxes, or click listeners.

---

## 3. Top Bar Architecture & Component Layout

Within any folder view, the screen top bar establishes navigational context, content labeling, and primary actions.

```
+-----------------------------------------------------------------------+
| [< Back]  Biology                           [+] [ (⋮) Overflow ]      |
+-----------------------------------------------------------------------+
```

### 3.1 Spatial Ordering (Left to Right)
1. **Back Navigation**: Leftmost position (`48x48dp` touch target, icon: left arrow `←`). Returns the user to the parent home screen or parent folder.
2. **Folder Name & Color Indicator**: Left-aligned adjacent to the back button, expanding to occupy available horizontal space (`weight(1f)`). Truncates with ellipsis if the title exceeds single-line boundaries.
3. **Add Photo Action Button (`+`)**: Positioned on the right side, immediately preceding the overflow menu.
4. **Highlighted Overflow Action (`⋮`)**: Positioned at the far right terminal edge (`16.dp` right margin).

### 3.2 Ordering Rationale
- **Primary vs. Terminal Placement**: In Western reading order (LTR), the far-right corner is standard for terminal overflow menus across modern Android applications (Material 3 TopAppBar pattern). Placing the Add Photo (`+`) button immediately to the left of the overflow menu keeps primary content creation grouped logically within the right thumb's natural reach zone while preserving the expected far-right location for menu settings.
- **Highlighted / Filled Overflow Menu State**: Unlike standard outline icons, the three-dot kebab icon is housed within a slightly elevated, tinted container (a `36x36dp` circular chip filled with `DockSlatePill` `#4A4C68` at 40% opacity with a pure white icon). This visual elevation communicates to students that this button is not merely an OS setting, but contains core coursework utilities: PDF export, batch organization, and folder color customization.

### 3.3 Add Photo Action Behavior (`+`)
- **Interaction Decision**: Tapping the `+` button displays an immediate modal action sheet anchored to the bottom of the screen with two distinct options:
  1. **Open Multi-Capture Camera** (Default / Prominent option): Launches Fotara’s high-speed document capture mode with automatic perspective cropping.
  2. **Import from Gallery**: Opens the Android photo picker for existing note photos.
- **Justification Against Direct Camera Launch**: While direct camera launch saves one tap, Fotara specifically supports importing existing screenshots and reference slides students take with their native camera app throughout the day. Presenting a zero-latency bottom sheet (with Multi-Capture pre-selected as the primary action and hardware camera access immediate) accommodates both behaviors without locking the user out of bulk imports.

### 3.4 Overflow Menu Contents & Justification
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

## 4. End-to-End Sequential Interaction Flows

Each interaction flow is specified using the mandatory sequence:  
**Trigger → Visual State Change → Available User Actions → Exit / Cancel Paths**.

---

### Flow 1: Home Screen on App Open
- **Trigger**: App cold start or returning to the root destination via back navigation.
- **Visual State Change**:
  - Deep dark navy background (`MidnightNavy` `#03071E`) initializes.
  - The top header renders the bold wordmark **Fotara** alongside a quick status summary (e.g., `"2 assignment notes due tomorrow"` inside an amber-accented pill).
  - Below the header, a 2-column scrollable grid renders subject folders styled as custom rounded-rectangle cards.
  - Pinned folders display a distinct pin icon in their upper tab and always occupy the top positions in the grid.
  - The final item in the grid is the **+New Folder** card, rendered with a translucent dark body, cream tab, and dashed border.
  - Floating above the bottom navigation margin rests the **Search Bar Dock** (Z-index 16dp, immune to Bug A obstruction).
- **Available User Actions**:
  1. *Tap a Folder Card*: Navigates into Flow 2 (Opening a Folder).
  2. *Long-Press on Folder Name Text*: Navigates into Flow 3 (Renaming a Folder).
  3. *Tap "+New Folder" Card*: Opens the New Subject Folder creation modal dialog.
  4. *Tap Search Bar Dock*: Navigates into Flow 7 (Search Interaction).
  5. *Tap Camera Icon in Dock*: Launches the instant multi-capture camera.
- **Exit / Cancel Paths**:
  - System back gesture from root exits the application.

---

### Flow 2: Opening a Folder
- **Trigger**: Tapping any folder card from the home screen grid.
- **Visual State Change**:
  - Fluid shared-axis transition: the folder card expands into the full screen while the top bar morphs to display the back arrow, folder name, color badge, `+` button, and highlighted overflow menu.
  - Directly beneath the top bar, a horizontal scrollable tab row slides into view, showing `"All Notes"` (active by default) followed by each defined subfolder (e.g. `"Lectures"`, `"Assignments"`, `"Lab Reports"`) and an inline `"+ Subfolder"` chip.
  - Below the tab row, a 2-column grid loads the folder’s photo notes. Each photo card displays an image thumbnail, manual caption or OCR excerpt, timestamp, and optional deadline indicator.
  - If the folder is empty, a clean centered illustration appears with the copy: *"No notes in this folder yet. Tap + to capture handwritten notes or slides."*
- **Available User Actions**:
  1. *Tap a Subfolder Tab*: Instantly filters the photo grid to items filed under that subfolder.
  2. *Tap "+ Subfolder" Chip*: Opens an alert dialog to name and create a new subfolder tab.
  3. *Long-Press a Subfolder Tab Title*: Activates inline renaming for that subfolder tab (Flow 3).
  4. *Tap Photo Card*: Navigates into Flow 5 (Viewing an Existing Photo).
  5. *Tap `+` Button*: Enters Flow 4 (Adding a Photo End-to-End).
  6. *Tap Overflow Menu (`⋮`)*: Opens the dropdown menu (Flow 6).
- **Exit / Cancel Paths**:
  - Tapping the Top Bar Back Arrow (`←`) or triggering the system back gesture transitions smoothly back to the Home Screen.

---

### Flow 3: Renaming a Folder or Subfolder (Inline Rename)
- **Trigger**: Long-press directly on the folder name text (duration > 500ms). Holding the rest of the card does NOT trigger rename.
- **Visual State Change**:
  - *Haptic Feedback*: A single crisp tactile haptic pulse (`HapticFeedbackType.LongPress`) is fired immediately upon registering the 500ms threshold.
  - *Visual Transition*: The static text label crossfades into an active inline editable text field.
  - The entire current text string is automatically selected (highlighted in brand royal blue), and a vertical cursor appears at the end.
  - The software keyboard deploys automatically with the action key set to `Done` / `Check`.
  - A subtle glowing outline (`FolderBodyBlue` `#0316A8`) wraps the text field to signal edit mode.
- **Available User Actions**:
  1. *Type new name*: Replaces the highlighted text or modifies individual characters.
  2. *Commit via Keyboard*: Tapping the keyboard `Done`/`Enter` checkmark commits the new name immediately.
  3. *Commit via Tap Outside*: Tapping anywhere outside the active text box commits the current input.
- **Exit / Cancel Paths**:
  - *System Back Button / Gesture*: Dismisses the keyboard and cancels editing without saving, restoring the previous folder name.
  - *Empty Text Reversion*: If the user clears all characters and attempts to commit, the operation cancels, reverts to the previous name, and displays a transient tooltip: *"Folder name cannot be empty."*

---

### Flow 4: Adding a Photo End-to-End (Capture → Popup Slider → Confirm)
- **Trigger**: Tapping the `+` action button in the folder top bar, selecting "Multi-Capture Camera", and capturing one or more note photos.
- **Visual State Change (Popup Slider Activation)**:
  - The camera viewfinder closes, returning to the folder screen.
  - A dark translucent scrim (50% black `#00000080`) dims the entire background folder screen.
  - The **Popup Slider Modal** animates upwards from the bottom, settling as a floating card covering approximately **75% of screen height and 90% of screen width**.
  - The underlying folder view remains partially visible around the margins and top behind the dimmed scrim, preserving navigational context.
  - Inside the Popup Slider:
    - *Header*: Displays batch count badge (e.g. `"Review: 3 notes captured"`), an individual photo discard button (trash icon), and an edit perspective crop icon.
    - *Body*: A horizontal pager displaying the captured photos. Swiping left/right flips between captured pages with snap-to-center physics.
    - *Footer*: A prominent primary button labeled **"Save to [Folder Name]"**, with an optional secondary toggle: *"Apply auto-suggested subfolder"*.
- **Available User Actions**:
  1. *Swipe Left / Right*: Page through captured notes to inspect sharpness and automatic straightening.
  2. *Tap Discard Icon on a Page*: Removes that specific photo from the batch. If only one photo remains and is discarded, the popup slider closes automatically.
  3. *Tap Perspective Crop Icon*: Opens quick quadrilateral corner adjustments if whiteboard edges were skewed.
  4. *Tap "Save to [Folder Name]"*: Persists all remaining photos into the folder database, triggers asynchronous on-device ML Kit OCR text indexing, dismisses the modal, and inserts cards into the grid with an animated entry.
- **Exit / Cancel Paths**:
  - *Tap Scrim or System Back*: Triggers a confirmation prompt: *"Discard all captured notes?"* with options *Discard* and *Keep Reviewing*. Tapping *Discard* aborts without saving.

---

### Flow 5: Viewing an Existing Photo from the Grid
- **Trigger**: Tapping an existing photo card within the folder grid.
- **Visual State Change & Architectural Decision**:
  - **Does NOT reuse the Popup Slider**. Instead, it opens a dedicated **Full-Screen Note Inspector**.
  - *Rationale*: The Popup Slider is intentionally designed for rapid, low-friction *capture triage* (confirming readability and discarding blurs before persisting). In contrast, viewing an *existing* note requires reading handwritten text, inspecting fine details with multi-touch pinch-to-zoom, copying indexed OCR text, adjusting linked deadlines, and exporting. A 75% popup card is too constrained for studying high-density note photos.
  - The Full-Screen Note Inspector provides:
    - Deep pinch-to-zoom and double-tap zoom.
    - Bottom expandable sheet revealing full recognized OCR text with a "Copy All Text" button.
    - Attached deadline badge with quick reminder rescheduling.
- **Available User Actions**:
  1. *Pinch / Pan*: Zoom into handwritten diagrams.
  2. *Swipe Up OCR Panel*: Inspect and copy searchable text.
  3. *Tap Edit / Move*: Re-assign to a different subfolder or tag color.
  4. *Tap Share / Export*: Export image as PNG or PDF.
- **Exit / Cancel Paths**:
  - Tapping the top-left back button or performing a vertical pull-down dismiss gesture closes the inspector and returns to the folder grid.

---

### Flow 6: Overflow Menu Interaction
- **Trigger**: Tapping the highlighted kebab menu button (`⋮`) on the right side of the folder top bar.
- **Visual State Change**:
  - An elevated Material 3 dropdown card smoothly cascades down from the anchor button (Z-index 24dp).
  - Background is styled in `MidnightSurface` (`#070D2B`) with a subtle `MidnightCardOutline` border.
  - The 6 defined menu items (Sort Order, Export to PDF, Batch Select, Folder Color, Rename, Delete) are rendered with high-contrast text and leading icons.
- **Available User Actions**:
  1. *Select "Sort Order"*: Expands inline sort options (*Newest, Oldest, Nearest Deadline*). Selecting an option updates the grid and dismisses the menu.
  2. *Select "Export Folder to PDF"*: Opens a progress sheet showing pages compiling into a PDF, followed by the system share sheet.
  3. *Select "Batch Select Mode"*: Dismisses menu, updates top bar to show selected count (`"0 selected"`), and renders circular checkboxes on all photo cards.
  4. *Select "Folder Color Label"*: Displays a dialog with Fotara’s 6 brand color swatches to update the folder’s tag indicator.
  5. *Select "Rename Folder"*: Programmatically triggers Flow 3 (inline renaming).
  6. *Select "Delete Folder"*: Displays a destructive confirmation dialog (*"Delete [Folder Name] and all its photos? This action cannot be undone."*).
- **Exit / Cancel Paths**:
  - Tapping anywhere outside the dropdown menu card or pressing the system back button immediately dismisses the menu without action.

---

## 5. Revision 2 Search Interaction Specification

The search interaction is completely specified below, replacing all prior search drafts and resolving Bug A and Bug B.

```
STATE 1: RESTING (HOME SCREEN)
+-----------------------------------------------------------------------+
|  Fotara                                                               |
|  [=== Due Tomorrow Strip ===]                                         |
|  [ Card 1 ]       [ Card 2 ]                                          |
|                                                                       |
|  (  🔍 Search notes, subjects, text...                           📷 ) | <-- Resting Dock (Z=16dp)
+-----------------------------------------------------------------------+

STATE 2: SEARCH ACTIVATED (KEYBOARD DEPLOYED)
+-----------------------------------------------------------------------+
|  <- [ bio                      ] [✕]                                  | <-- Repositioned Search Bar
|  [=========================== (Indeterminate Buffer Track) =========] |
|  RESULTS (Instant keystroke query):                                   |
|  • Biology > Cell Structure ("...ATP synthase in mitochondria...")     |
|  • Bio 101 > Midterm Review ("...mitosis phases diagram...")          |
|                                                                       |
|  +-----------------------------------------------------------------+  |
|  |                    SOFTWARE KEYBOARD                            |  |
|  +-----------------------------------------------------------------+  |
+-----------------------------------------------------------------------+
```

### 5.1 Activation Repositioning
1. **Trigger**: User taps directly on the resting search bar.
2. **Transition Animation**:
   - As the software keyboard initiates its entrance animation, the search bar transitions from its resting position at the bottom of the home screen to sit **directly docked above the top edge of the keyboard** (tracking `WindowInsets.ime`).
   - *Recommended Transition*: **Fluid Spring Animation** (duration: 250ms, damping ratio: 0.82, stiffness: Spring.StiffnessMediumLow).
   - *Justification*: Snapping instantly creates visual jarring and disconnects the user's focal point. Animating with an easing curve matched to the keyboard's velocity ensures the search bar feels physically tethered to the rising keyboard, maintaining continuous visual continuity.
3. **Background & Content State**:
   - The home screen folder grid is obscured by an active search results canvas (`MidnightNavy` background with 95% opacity).
   - The resting bottom action buttons and system dock are completely concealed behind the search canvas to prevent any tap confusion.

### 5.2 Instant Keystroke Search Behavior
1. **Zero-Latency Execution**:
   - Because all folder names, photo captions, and ML Kit OCR text are indexed locally in SQLite / Room, search queries execute instantly on the UI coroutine dispatcher (`Dispatchers.IO` debounced at 0ms).
   - There is **no submit button** and **no required press of the enter key**.
2. **Result Display on Keystroke**:
   - *0 Characters (Query Empty)*: The screen displays a clean "Recent Searches" list and "Frequently Searched Subjects" chips.
   - *1+ Characters*: Results populate dynamically in real time below the search bar, grouped by relevance:
     1. Exact folder title matches (rendered as miniature folder cards).
     2. OCR text matches (displaying a photo thumbnail, folder path breadcrumb, and a 2-line snippet with the matched keyword bolded).
   - *Query with Zero Matches*: Displays a centered empty state: *"No notes found matching '[query]'. Check spelling or search by general topic."*
3. **Clearing Query**:
   - When text is present, a clear button (`✕`) appears at the right edge of the search bar.
   - Tapping `✕` clears all text instantly and restores the 0-character state without closing the keyboard or exiting search mode.

### 5.3 Loading Indicator During Search (Video Buffering Pattern)
1. **Visual Pattern**:
   - A razor-thin horizontal track (height: `2.5dp`) rendered along the bottom edge of the repositioned search container.
   - It features an indeterminate highlight segment that slides repeatedly from left to right, where the length of the filled highlight segment randomly modulates between 20% and 50% of track width on each pass (emulating video buffering indicators).
2. **Anchor Position**:
   - Anchored directly flush against the bottom border of the active search input container, bridging the search bar and the search results list.
3. **Appearance Trigger & Performance Justification**:
   - *Decision*: The indicator is **hidden during normal instantaneous on-device Room queries (< 16ms)**, and **revealed only if query evaluation exceeds 100ms** (e.g. during heavy full-text OCR indexing scans across hundreds of documents).
   - *Justification*: Showing a flashing progress bar on every single keystroke when local SQLite queries return in 4ms creates distracting visual strobe artifacts. A 100ms threshold ensures instantaneous typing feels clean and frictionless, while still providing feedback if large OCR databases require a brief background scan.

### 5.4 Exiting Search
1. **Exit Mechanisms**:
   - *Explicit Back Arrow*: Tapping the back arrow (`←`) at the left of the search bar.
   - *System Back Gesture*: Swiping the Android back gesture or pressing the hardware back button dismisses search.
   - *Keyboard Dismissal*: Dismissing the keyboard while the query is empty automatically exits search mode.
2. **Return Transition**:
   - The search bar animates smoothly back to its resting floating position at the bottom of the home screen, and the folder grid crossfades back into view.
3. **Query Persistence Policy**:
   - *Immediate Reopen*: If the user exits search with an active query and taps the search bar again within 30 seconds, the query text and results are preserved.
   - *Navigating into a Result*: Tapping a search result navigates directly to that note or folder; returning back automatically resets the search query to clean state.

---

## 6. Architectural Conflict Register

The following potential conflicts between specified requirements and the product brief have been analyzed and formally resolved:

| Item | Conflict Description | Resolution & Technical Rule |
| :--- | :--- | :--- |
| **C-01: Top vs. Bottom Search Bar** | The product brief (Section 4) states *"A search bar sits at the top of the home screen"*, whereas UI reference (`photo_6167897522295214567_y.jpg`) and Revision 2 require a bottom floating dock that docks above the keyboard. | **Resolved**: The resting search bar is docked at the bottom of the home screen matching the visual mockup. When activated, it repositions directly above the software keyboard, satisfying both the ergonomics of thumb reach and Revision 2 requirements. |
| **C-02: Direct Camera vs. Add Photo Chooser** | Rapid note-taking benefits from instant camera launch, but students also import screenshots from gallery. | **Resolved**: Tapping `+` displays a zero-latency bottom sheet with *Multi-Capture Camera* as the primary prominent option and *Import from Gallery* as secondary. |
| **C-03: Subfolder Navigation Depth** | Brief discusses tabs vs. breadcrumbs for subfolder hierarchies. | **Resolved**: Horizontal scrollable tabs are used for primary subfolders (depth 1). Deeper nested subfolders (depth 2+) surface an inline breadcrumb bar beneath the tabs. |
| **C-04: Card Click vs. Rename Gesture** | Renaming a folder could collide with opening a folder if whole-card long-press is used. | **Resolved**: Whole-card press opens the folder; whole-card long-press enters batch drag-and-drop mode. Only a long-press *directly on the name text label* triggers inline renaming. |

---

## 7. Proposed Default Parameters

Where specific interaction micro-parameters were not explicitly fixed, the following defaults are formally specified:

- **Long-Press Timeout**: `500ms` (standard Android system baseline).
- **Haptic Feedback**: Android `HapticFeedbackType.LongPress` on rename trigger; `HapticFeedbackType.TextHandleMove` on slider swipe.
- **Search Reposition Animation**: Duration: `250ms`, Easing: `FastOutSlowInEasing`.
- **Indeterminate Progress Track**: Height `2.5dp`, Track Color: `MidnightCardOutline` (`#142055`), Indicator Color: `FolderBodyBlue` (`#0316A8`).
- **Popup Slider Dimensions**: Width `90%`, Height `75%`, Corner Radius `24.dp`, Scrim Color `Color.Black.copy(alpha = 0.55f)`.
- **Empty State Typography**: Headline `16.sp Bold`, Subtitle `13.sp Regular TextSecondary`.
