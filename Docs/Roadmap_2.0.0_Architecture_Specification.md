// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

# Fotara 2.0.0: Strategic Architectural Blueprint & Feature Specification

## 1. Executive Summary & Product Vision
Fotara is evolving from a localized, folder-based coursework photo and OCR organizer into a comprehensive, **Local-First Academic Operating System** for college and high school students. 

The 2.0.0 milestone introduces a groundbreaking five-tier organizational hierarchy spearheaded by **Spaces**, an impenetrable **3-Tier Stealth Privacy System** featuring pull-to-refresh gesture discovery, persistent sub-screen navigation with canonical pop-to-root interactions, drawing and note superpowers (LaTeX rendering, shape auto-correction, text layers, multi-sheet canvas), proactive academic scheduling, and an opt-in **Live Collaborative Drawing Canvas** paired with an in-app **Friends System**.

All designs in this document strictly adhere to Fotara's foundational invariants:
- **Local-First Reliability**: Zero network dependency for core note-taking, organization, drawing, and review workflows.
- **Strict Privacy Guarantees**: Absolute isolation of private vaults, zero unauthorized data exfiltration, and clean device boundaries.
- **Ergonomic Design**: Consistent utilization of the `ElmsSans` typography, `MidnightNavy` design language, dynamic bottom clearances via `LocalBottomOverlayPadding`, and haptic feedback.

---

## 2. Super-Hierarchy: The Space System (Multi-Vault Organization)

### 2.1 Conceptual Model
The organizational hierarchy of Fotara expands from four tiers to five distinct levels:

$$\mathbf{Space} \longrightarrow \mathbf{Workspace} \longrightarrow \mathbf{Folder} \longrightarrow \mathbf{Subfolder} \longrightarrow \mathbf{Notes / Media}$$

A **Space** represents a completely isolated container (a distinct academic or personal vault). Switching Spaces swaps the entire operational context:
- Workspaces and custom tabs.
- Course folders and nested subfolders.
- Photos, documents, text notes, and canvas sketches.
- Search indexing, recent query history, and pinned PDF pages.

#### Typical Student Workspaces
1. **Academic Space** (e.g., *"Undergraduate Engineering"*): Contains Workspaces for Semester 1, Semester 2, with Folders for Calculus, Physics, Circuits.
2. **Campus Organization Space** (e.g., *"Student Council"*): Contains Workspaces for Event Planning, Advocacy, with Folders for Meeting Minutes, Proposals.
3. **Personal Vault Space** (e.g., *"Personal & Portfolio"*): Contains Workspaces for Independent Projects, Identity Documents, Private Journals.

### 2.2 Database Architecture (SQLite Schema v19 Migration)
To maintain high performance and zero-redundancy indexing, `space_id` is introduced at the Workspace level rather than duplicating foreign keys across millions of child entities.

```sql
-- New spaces table
CREATE TABLE spaces (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    uuid TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    icon_key TEXT DEFAULT 'grid',
    color_hex TEXT DEFAULT '#2563EB',
    is_private INTEGER DEFAULT 0,
    sort_order INTEGER DEFAULT 0,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

-- Migration delta for workspaces table
ALTER TABLE workspaces ADD COLUMN space_id INTEGER REFERENCES spaces(id) ON DELETE CASCADE;
```

#### Backward Compatibility & Auto-Seeding
During the v18 -> v19 database migration:
1. The migration automatically inserts a default Space:
   `INSERT INTO spaces (id, uuid, name, icon_key, color_hex, sort_order, created_at, updated_at) VALUES (1, '00000000-0000-4000-8000-000000000101', 'Default Space', 'school', '#2563EB', 0, ...);`
2. All existing Workspaces (`HOME`, `ARCHIVE`, and `CUSTOM`) are linked to `space_id = 1`.
3. Zero existing data is orphaned, ensuring instantaneous, non-destructive migration.

### 2.3 User Interface & Interaction Flow
* **Header Brand Switcher**:
  - The top bar title on `HomeScreen` and `NotesScreen` displays: `Fotara ▾ [Active Space Name]`.
  - Tapping the brand title triggers a smooth slide-down **Space Switcher Bottom Sheet**:
    - **Current Space Card**: Visual indicator with active space name, color accent badge, and workspace count.
    - **Space Grid/List**: Visual cards for available spaces with custom icons.
    - **Actions**: `[+ Create Space]` button and `[Manage Spaces]` sheet (Rename, Color Picker, Export Backup, Delete).
* **Space Switching Transition**:
  - Selecting an alternate Space updates `currentSpaceId` in `HomeViewModel`.
  - Reactive `StateFlow` streams immediately filter all underlying queries with a subtle 150ms crossfade transition without requiring Activity recreation.

---

## 3. 3-Tier Stealth Privacy System

Fotara provides three escalating layers of privacy protection, tailored for students who routinely share their devices with classmates while keeping personal or sensitive coursework secure.

```
┌─────────────────────────────────────────────────────────────┐
│ LEVEL 3: PRIVATE SPACE (Stealth Vault)                      │
│ Trigger: Long-press brand title "Fotara" for 2.0s           │
└──────────────────────────────┬──────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────┐
│ LEVEL 2: PRIVATE WORKSPACE (Ghost Tab)                      │
│ Trigger: Long-press Home Tab Bar for 1.5s                   │
└──────────────────────────────┬──────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────┐
│ LEVEL 1: PRIVATE FOLDER (Hidden Subject)                    │
│ Trigger: Pull-to-refresh & hold 2.0s                        │
└─────────────────────────────────────────────────────────────┘
```

### 3.1 Level 1: Private Folder (Pull-to-Refresh & Hold 2 Seconds)
* **Discovery & Unlock Interaction**:
  - On the folder grid screen, the user performs a **Pull-to-Refresh** gesture.
  - Instead of releasing immediately, the user **holds the pull for 2.0 seconds**.
  - A hidden lock icon smoothly morphs and animates into view below the header with haptic feedback (`HapticFeedbackType.LongPress`).
  - The app invokes Android's native `BiometricPrompt` (Fingerprint, Face Unlock, or Device PIN).
  - Upon successful verification, hidden Private Folders slide gracefully into view at the top of the folder grid with an elegant indigo/gold border accent.
* **Security & Invariants**:
  - **Blurred Previews**: Before authentication, card thumbnails are completely obfuscated with a frosted glass Gaussian blur.
  - **Search Immunity**: All photos, OCR tokens, and text notes inside Private Folders are strictly excluded from global search indexing (`FTS` query suppression) until the folder is authenticated.
  - **Auto-Relock**: Navigating away from the folder or backgrounding the application automatically re-locks and conceals the folder.

### 3.2 Level 2: Private Workspace (Ghost Mode)
* **Concept & Trigger**:
  - A dedicated workspace tab configured as private can be toggled into **Ghost Mode**.
  - In Ghost Mode, the tab is completely invisible in the `WorkspaceTabBar`.
  - **Reveal Gesture**: Long-pressing the static `Home` workspace tab for **1.5 seconds** triggers the biometric prompt.
  - Upon authentication, the Private Workspace tab smoothly slides into the tab bar with a `🔒` badge.
* **Auto-Lock Lifecycle**:
  - The moment the user switches to another workspace tab, the private tab locks itself and fades from the bar.

### 3.3 Level 3: Private Space (Stealth Vault)
* **Concept & Trigger**:
  - A dedicated Space marked `is_private = 1` does not appear in the standard Space Switcher dropdown.
  - **Secret Entry**: **Long-pressing the top bar title "Fotara" for 2.0 seconds** triggers an intentional discrete haptic tick followed by biometric authentication.
  - Once verified, the entire application switches into the Private Space vault.
* **Filesystem Shielding**:
  - All stored photos, documents, and exported canvases within a Private Space are written to isolated app-internal directories accompanied by an empty `.nomedia` file.
  - Android system media scanners (Google Photos, Samsung Gallery) are strictly prevented from indexing or exposing these photos to the rest of the OS.

---

## 4. Navigation Architecture: Tab Persistence & Pop-to-Root

### 4.1 Root Cause & Solution: Screen State Persistence
* **The Problem**: Currently, when navigating deep into a Settings sub-screen (e.g., `ProfileScreen`, `GeneralSettings`, or `StorageSettings`), switching bottom tabs from Settings to Home and back resets the screen to the Settings root menu.
* **Solution Architecture**:
  1. **Tab Content State Preservation**:
     - Replace destructive unmounting in `HomeScreen.kt` with state-preserving composition using `SaveableStateHolder` or state hoisting inside `HomeViewModel`.
     - When switching between `HomeNavTab.HOME`, `HomeNavTab.NOTES`, and `HomeNavTab.SETTINGS`, the inner sub-screen state (`activeSection`, scroll positions, and text inputs) is retained in memory.
  2. **Lifecycle Invariant**:
     - Switching between tabs does **not** reset sub-screen progress. If a user is adjusting their profile or reading legal documents, switching tabs to check a note and returning preserves their exact viewport and active sub-screen.

### 4.2 Re-Tap Navigation (Pop-to-Root & Scroll-to-Top)
To deliver fluid one-handed ergonomics, the floating bottom navigation bar (`HomeBottomNavBar`) implements the canonical Android re-tap contract:

```
User taps active tab again:
├── Is the tab currently inside a Sub-Screen (Child Level)?
│   └── YES -> POP-TO-ROOT (Closes sub-screen, returns smoothly to root menu)
└── Is the tab already at Root Level?
    └── YES -> SCROLL-TO-TOP (Animates scroll state to 0)
```

#### Concrete Examples
1. **Settings Tab**:
   - User is inside `ProfileScreen` or `FriendsScreen`: Tapping the active **Settings** tab in the bottom bar acts as an immediate **Back / Pop-to-Root** action, returning to the Settings root list.
   - User is at the Settings root list: Tapping **Settings** animates `rootScrollState` smoothly back to position 0.
2. **Home Tab**:
   - User is inspecting a PDF in `PdfViewerScreen` or browsing a deep folder: Tapping **Home** pops back to the top-level Workspace Dashboard.
3. **Notes Tab**:
   - User is editing in `TextNoteEditorScreen`: Tapping **Notes** saves changes and pops back to the notes overview.

---

## 5. Settings Screen Layout & Dedicated "Friends" Integration

### 5.1 Placement & Visual Hierarchy
In accordance with the approved user interface layout, the **Friends** management card is placed prominently in the root Settings screen:
**Position**: Directly below `Profile` and directly above `General`.

```
┌────────────────────────────────────────────────────────┐
│ Settings (Header with 16:9 Banner & Avatar Preview)    │
│ Buchori Muslim (buchorimuslimmmx1@gmail.com)           │
├────────────────────────────────────────────────────────┤
│ [ 👤 ] Profile                                         │
│        Photo, border, banner, name, email              │
├────────────────────────────────────────────────────────┤
│ [ 👥 ] Friends                             (NEW CARD)  │
│        Manage friends, sharing, collaboration          │
├────────────────────────────────────────────────────────┤
│ [ ⊞ ] General                                          │
│        Grid density, default sort order, image storage │
├────────────────────────────────────────────────────────┤
│ [ 🎨 ] Appearance                                      │
│        Theme mode and display options                  │
├────────────────────────────────────────────────────────┤
│ [ 🔤 ] OCR & Recognition                               │
│        On-device text extraction, auto-index           │
├────────────────────────────────────────────────────────┤
│ [ 🔔 ] Notifications & Deadlines                       │
│        Reminders and test alert                        │
├────────────────────────────────────────────────────────┤
│ [ 💾 ] Storage & Data Management                       │
│        Cache size, backup export/import                │
├────────────────────────────────────────────────────────┤
│ [ ℹ️ ] About & Legal                                   │
│        Privacy policy, terms of service, version info  │
└────────────────────────────────────────────────────────┘
```

### 5.2 Card Specifications
* **Component**: `SettingsRowItem` styled in `SettingsCardSurface`.
* **Icon**: `Icons.Default.People` (24dp, tint `#60A5FA` / `AccentBlue`).
* **Title**: `stringResource(R.string.settings_item_friends)` ("Friends").
* **Subtitle**: `stringResource(R.string.settings_item_friends_sub)` ("Manage friends, sharing, collaboration").
* **Trailing**: Right chevron arrow (`Icons.AutoMirrored.Filled.ArrowForwardIos`).
* **Target Sub-Screen**: `SettingsSection.FRIENDS` leading to `FriendsScreen`.

### 5.3 Friends Sub-Screen Capabilities
The dedicated `FriendsScreen` includes:
1. **User Identity Header**:
   - Displays current user handle / tag (e.g., `@buchori_m`) with a one-tap copy button and personal QR Code generator.
2. **Add Friend Bar**:
   - Search field allowing discovery by exact tag or camera QR scan.
3. **Friends List Tab**:
   - Categorized by:
     - **Online / Studying**: Friends currently in active study sessions.
     - **Offline**: Sorted alphabetically.
   - Friend cards feature avatar, status badge, and quick action: **[ 🎨 Invite to Canvas ]**.
4. **Pending Requests & Privacy Controls**:
   - Toggle to accept collaboration invites automatically or require explicit consent.

---

## 6. Canvas & Drawing Engine Superpowers

The existing vector drawing architecture (`DrawingViewTransform`, `DrawingEngine`, and `PdfPageEditorScreen`) is expanded with four advanced creative tools.

### 6.1 Text Layers in Drawing Mode & PDF Editor
* **Tool Dock Addition**:
  - A new tool item **`[ T ]` (Text Layer)** is added to the drawing dock alongside Pen, Highlighter, and Eraser.
* **Interaction Flow**:
  1. Selecting `[ T ]` and tapping anywhere on the canvas places a dynamic text box.
  2. The keyboard opens with an attached floating format bar:
     - **Font Size Slider**: 12sp to 48sp.
     - **Typography**: `ElmsSans` font family weights (Regular, Medium, Bold).
     - **Color Palette**: Fotara's 8 curated stroke colors.
     - **Box Style**: Transparent, Frosted Dark Card (`#1E293B`), or Solid Light Card.
  3. Bounding box handles enable smooth dragging, width resizing, and rotation.
* **Data Model**:
  - Saved as a distinct vector entity `DrawingElement.TextLayer` in SQLite table `pdf_page_drawings` / `canvas_notes`, preserving lossless scalability.

### 6.2 Shape & Curve Auto-Correct (Draw & Hold Gesture)
* **Gesture Mechanism**:
  - Users sketching with the Pen tool draw approximate geometric figures (rectangles, triangles, circles, ellipses, arrows, or straight lines).
  - Upon completing the stroke, **holding the stylus or finger on the screen for 400ms without lifting** triggers auto-correction.
  - A subtle haptic vibration fires, and the hand-drawn stroke snaps instantaneously into a mathematically perfect geometric primitive.
  - While still holding, dragging the contact point resizes and reorients the shape before committing.
* **Algorithms**:
  - **Douglas-Peucker Simplification**: Reduces stroke point noise.
  - **Convex Hull & Aspect Fitting**: Evaluates circularity, collinearity, and corner angles to classify the target shape.
  - **Bézier Smoothing**: Organic handwriting strokes that are not held are processed with cubic Bézier interpolation to eliminate jagged edges on high-DPI displays.

### 6.3 Multi-Sheet Drawing Mode (Max 10 Sheets)
* **UI Placement**:
  - Directly above the bottom tool dock rests the **Sheet Pagination Bar**:
    `[ Sheet 1 ]  [ Sheet 2 ]  [ Sheet 3 ]  [ + ]`
* **Functional Rules**:
  - Supports up to **10 sheets** per Canvas Note.
  - When the count reaches 10, the `[ + ]` button is cleanly disabled with a polite toast: *"Maximum 10 sheets per sketch note"*.
  - Independent Undo/Redo stacks per sheet.
  - Reordering sheets via drag-and-drop.
  - **Multi-Page Export**: The export engine renders all sheets into a single combined multi-page PDF document or an image zip package.

---

## 7. STEM Text Note Superpowers: LaTeX Math & Structured Tables

### 7.1 Input & Rendering Syntax
`TextNoteEditorScreen` incorporates real-time mathematical typesetting for engineering and science students:
* **Inline Formulas**: Delimited by single dollar signs:
  `$E = mc^2$` or `$\vec{F} = m\vec{a}$`
* **Display Block Formulas**: Delimited by double dollar signs:
  ```latex
  $$
  \int_{0}^{\infty} \frac{\sin x}{x} dx = \frac{\pi}{2}
  $$
  ```

### 7.2 Editor Toolbar & Live Preview
* **Toolbar Action**:
  - A dedicated **`[ fx ]`** button on the formatting toolbar above the keyboard inserts a LaTeX skeleton template at the current cursor position.
* **Hybrid Live Rendering**:
  - While editing inside math markers, raw LaTeX syntax is shown with subtle monospace syntax highlighting.
  - Moving the cursor outside the formula instantly renders high-definition mathematical typography using a lightweight, offline KaTeX layout engine.
  - A persistent top bar toggle offers `[ Edit | Split | Preview ]` modes.

### 7.3 Structured Tables (Rows, Columns & Formatting Constraints)
* **Insert & Grid Manipulation**:
  - A dedicated **`[ ▦ Table ]`** button on the formatting toolbar inserts a standard 3x3 table with a highlighted header row.
  - Contextual pill controls appear adjacent to the active cell for 1-tap table management:
    - Add Row Above / Below (`+ Row`)
    - Add Column Left / Right (`+ Col`)
    - Delete Row / Delete Column
    - Column Text Alignment (Left, Center, Right)
* **Strict Formatting Constraint (Core Features Only, Rest Grayed Out)**:
  - When the cursor or text selection is inside any table cell, **only core inline formatting tools remain active**:
    - **Active & Clickable**: Bold (`B`), Italic (`I`), Strikethrough (`S`), Inline Code (`` ` ``), and Inline Math (`$fx$`).
  - **All block-level formatting tools are strictly disabled and visually grayed out**:
    - **Disabled / Grayed Out (alpha = 0.38f)**: Headings (H1, H2, H3), Bulleted Lists (`•`), Numbered Lists (`1.`), Checkboxes (`☑`), Blockquotes (`>`), Multi-line Code Blocks (```), and Horizontal Dividers (`---`).
  - **Architectural Rationale**: Prevents markdown syntax corruption and layout breakage, maintaining 100% compliant CommonMark/GFM table serialization and clean on-device rendering.

---

## 8. Academic Scheduling, Timetables, & Anti-Procrastination Alarms

### 8.1 Notes Tab Integration: 1-Line Compact Dynamic Schedule Capsule
* **Placement & Design Invariant**:
  - Positioned **strictly below `WorkspaceTabBar` and immediately above `NotesFilterChipsRow`** in [`NotesScreen.kt`](file:///C:/Users/sepli/OneDrive/Documents/File%20DD/Coding/Fotara/app/src/main/java/com/arinara/fotara/ui/notes/NotesScreen.kt).
  - Placing above workspace tabs is explicitly forbidden to preserve global navigation hierarchy consistency (`ScreenHeader` $\rightarrow$ `WorkspaceTabBar` $\rightarrow$ Contextual Banners $\rightarrow$ Filter Chips $\rightarrow$ Content Feed).
* **Visual & Functional Specification**:
  - **Single-Line Height**: Compact capsule (~36dp height), non-intrusive to notes scroll space.
  - **Live State (Daytime / Pre-Cutoff)**:
    ```
    ┌────────────────────────────────────────────────────────┐
    │ [ All ] [ Semester 3 ] [ Laboratory ]                  │  <-- WorkspaceTabBar
    ├────────────────────────────────────────────────────────┤
    │ [ 🟢 Sekarang: Kalkulus II (R. 302) s.d 09:40   |  10:00 Fisika ▾ ] │  <-- Schedule Capsule
    ├────────────────────────────────────────────────────────┤
    │ (● Semua)  ( Catatan Teks )  ( Gambar )  ( PDF )       │  <-- NotesFilterChipsRow
    ```
  - **Rollover State (Evening / Post-Cutoff)**:
    ```
    │ [ 🌙 Persiapan Besok: 08:00 Kalkulus II (R. 302)  •  3 Kelas ▾ ] │
    ```
  - **Tap Interaction**: Tapping the capsule triggers a full-featured **Schedule Management Modal Sheet**:
    - Full weekly timetable grid (Monday – Saturday/Sunday).
    - File import triggers (`.xlsx` & `.docx`).
    - Manual class entry and subject folder link mapping.
    - Rollover cutoff time selector and notification toggles.
  - **Swipe / Hide Capability**: Can be swiped to dismiss for the active session or toggled off via *Settings > General > Show Schedule Capsule*.

### 8.2 Tabular File Ingestion & Parsing Engine (Excel `.xlsx` & Word `.docx`)
* **Lightweight Parsing Architecture (Zero Heavy Dependencies)**:
  - To preserve Fotara's strict base APK size budget (<25 MB), heavy libraries such as Apache POI are explicitly prohibited.
  - **Excel (`.xlsx`) Streaming Parser**:
    - Treats `.xlsx` as a ZIP archive.
    - Uses Android platform streaming `XmlPullParser` on `xl/worksheets/sheet1.xml` paired with `xl/sharedStrings.xml`.
    - Parses tabular cell coordinates (`A1`, `B1`, etc.) with minimal memory footprint and zero OOM risk.
  - **Word (`.docx`) Table Parser**:
    - Decompresses `word/document.xml` from the `.docx` archive.
    - Extracts structured table rows (`<w:tbl>` $\rightarrow$ `<w:tr>` $\rightarrow$ `<w:tc>`) and cell paragraphs.
* **Smart Column Mapping Dialog**:
  - Displays an intuitive mapping dialog allowing students to assign table columns:
    - *Day Column*: Auto-matches headers containing "Hari", "Day", "Waktu", "Senin".
    - *Time Interval Column*: Auto-matches patterns like "08:00 - 09:40", "Jam", "Waktu".
    - *Subject Column*: Auto-matches "Mata Kuliah", "Mapel", "Subject", "Course".
    - *Room / Location Column*: Auto-matches "Ruangan", "Room", "Lab", "Gedung".
    - *Instructor / Lecturer Column (Optional)*: Auto-matches "Dosen", "Guru", "Lecturer".
  - **Preview & Verification Grid**: Displays parsed entries in a formatted preview table for validation before persisting into SQLite `class_schedules`.

### 8.3 Next-Day Rollover Cutoff Logic (*Cutoff Time Engine*)
* **Single Master Setting**: `schedule_rollover_time` (Default: `18:00` / 6:00 PM; user configurable).
* **State Machine & Behavior**:
  1. **Before Cutoff (00:00 – Cutoff Time)**:
     - Dashboard capsule and Android widget display **Today's Schedule**.
     - Classes are tagged dynamically:
       - `ACTIVE`: Current ongoing class with countdown timer.
       - `UPCOMING`: Subsequent classes scheduled for today.
       - `COMPLETED`: Finished classes rendered in subtle muted text.
  2. **After Cutoff (Cutoff Time – 23:59)**:
     - Automatically transitions both the Notes capsule and Home Screen Widget to display **Tomorrow's Schedule**.
     - Header updates to *"Persiapan Besok (Hari, Tanggal)"* / *"Tomorrow's Preparation"*.
     - Purpose: Enables students to review next-day coursework, prepare physical books/bags, and resolve homework deadlines before sleeping.
  3. **Weekend & Holiday Transition**:
     - On Friday evening after cutoff time, system intelligently rolls over to Monday morning's schedule (or Saturday if 6-day school week is selected).

### 8.4 Android Home Screen Schedule Widget (`AppWidgetProvider`)
* **Widget Features**:
  - Sizes: 4x2 (Compact daily list) and 4x3 / 4x4 (Detailed multi-slot timetable with room & lecturer).
  - **Real-Time Rollover Synchronization**: Directly listens to system time ticks and `schedule_rollover_time` updates without requiring app launch.
  - **One-Tap Subject Navigation**: Tapping any class item directly deep-links into that specific subject's Folder in Fotara.
  - **Embedded `[ 📷 Quick Capture ]` Action**: Opens the Fotara camera immediately and auto-routes snaps into the active class's folder.

### 8.5 Automated Reminders & Proactive Notifications
* **Morning Daily Digest (`06:30` or user-defined)**:
  - Concise morning summary: *"Hari ini ada 3 mata pelajaran: Kalkulus (08:00), Fisika (10:00), Algoritma (13:00)"*.
* **Pre-Class Heads-Up (H-15 or H-10 Minutes)**:
  - Fires 10 minutes prior to lecture start with immediate action buttons:
    - `[ 📁 Buka Catatan ]`: Deep-links to folder.
    - `[ 📷 Buka Kamera ]`: Immediate whiteboard capture.
* **Evening Rollover Digest (At Cutoff Time)**:
  - Fires at the exact rollover time: *"Jadwal besok telah aktif. Cek 4 mata pelajaran untuk besok dan siapkan tugas."*

### 8.6 Spaced Repetition (Review Reminder Engine)
* **Forgetting Curve Prevention**:
  - On any important photo, document, or text note, students can toggle: **"Schedule Spaced Review"**.
  - Automatically calculates optimal review intervals:
    - **Interval 1**: H+1 day (reinforcing initial comprehension).
    - **Interval 2**: H+3 days (retaining concepts).
    - **Interval 3**: H+7 days (mastering material before the next lecture).
  - Review notifications feature note thumbnail previews and deep-link directly into the content.

### 8.7 Urgent Anti-Procrastination Alarm (With Dismiss Challenges)
* **Alarm Escalation**:
  - For critical deadlines, students select `ScheduleAlertType.ALARM` with the **"Anti-Procrastination Challenge"** enabled.
  - At the trigger time, Fotara initiates a full-screen, continuous audio alarm that overrides Do-Not-Disturb modes using the system alarm stream.
* **Dismiss Challenge Options**:
  1. **Photo Proof Challenge**: The alarm will not silence until the student launches the camera and snaps a photo of their completed coursework.
  2. **Checklist Submission Challenge**: The student must check off their submission criteria and solve a short math verification challenge before dismissing.


---

## 9. Online Collaboration: Live Study Canvas

Fotara embraces a **Hybrid Local-First Architecture**: the app is completely self-sufficient offline, but unlocks peer-to-peer drawing rooms on demand.

### 9.1 Session-Based Collaboration (Room Codes)
* **Access Point**:
  - Top right corner of the Canvas Editor: **`[ 👥 Collab ]`** button.
* **Workflow**:
  1. Host taps `[ 👥 Collab ]` -> Selects **"Start Live Study Room"**.
  2. The app generates a human-friendly **6-Digit Room Code** (e.g., `FT-8821`) and an invite link.
  3. Peers open Fotara -> Tap `Join Room` -> Enter `FT-8821` (or click the invite link).
  4. All participants are connected to the live session.

### 9.2 Real-Time Synchronization Engine
* **Protocol**: Low-latency WebSocket broadcasting or WebRTC Data Channels.
* **Stroke Streaming**: Vector point coordinates, color tokens, and stroke widths are transmitted as compressed binary packets (<50ms latency).
* **Collaborator Indicators**:
  - Real-time colored cursor pins with peer names display where collaborators are currently sketching.
  - Non-destructive conflict handling: strokes are layered chronologically without overwriting peer data.

---

## 10. Android Home Screen Photo Widget

### 10.1 Widget Formats
* **Specific Single Photo Card (1x1, 2x2, or resizable)**:
  - Allows the user to select and specify **exactly one specific photo** from their coursework folders (e.g., a periodic table, a specific math formula sheet, class timetable, or key diagram) to stay pinned on their home screen indefinitely.
  - Widget configuration activity (`PhotoWidgetConfigActivity`) launches an in-app photo picker to bind the exact `photo_id`.
  - Tapping the widget opens that exact photo directly inside Fotara's full-screen viewer.
* **Mini Pinned Card (2x2)**:
  - Dynamically displays the latest pinned study photo or pinned PDF page chip across active folders.
* **Coursework Carousel / Grid (4x2)**:
  - Displays 3–4 recent coursework photos taken today, accompanied by a quick **`[ + Camera ]`** button that launches instant capture.

### 10.2 Technical Implementation
* Built using modern `Jetpack Glance` / `AppWidgetProvider`.
* Memory-efficient thumbnail loading via `ContentProvider` without waking the full application process.

---

## 11. Active Recall Masking & Knowledge Evaluation

### 11.1 Diagnostic Occlusion & Masking
* **Purpose**: Empowers students in STEM, biology, medicine, and languages to practice active retrieval directly on coursework photographs and multi-page PDFs without manual transcription into external applications.
* **Occlusion Tool**:
  - A dedicated **Occlusion Tape** tool in the viewer and editor dock.
  - Students drag rectangular masks over critical labels, anatomical terms, or mathematical proof steps.
* **Knowledge Evaluation Mode**:
  - Tapping **`[ Active Recall ]`** conceals all masked regions with opaque privacy tabs.
  - Tapping any individual tab uncovers the hidden answer with immediate self-assessment logging (Correct, Uncertain, Re-evaluate).

---

## 12. Synchronized Audio Annotations & PDF Page Anchoring

### 12.1 Spoken Commentary Capture
* **Recording Workflow**:
  - Embedded microphone trigger allows capturing verbatim professor explanations (up to 5 minutes duration) directly attached to coursework artifacts.
  - Audio encoded locally via hardware AAC/M4A compression (128 kbps) with minimal storage footprint.
  - **Zero-Friction Default Save**: When finished recording, the student can tap **`[ Save Audio ]`** to immediately bind the recording to the entire document or note without being forced through a page assignment workflow.

### 12.2 Multi-Page PDF Assignment (Strictly Optional)
* **Optional Page Anchoring**:
  - Page anchoring is **never mandatory**. If a student chooses not to assign a page, the audio remains attached globally at the document level (accessible via top-bar player).
  - If the student desires page-specific context, they can optionally tap **`[ Anchor to Page ]`**:
    - **Method A (Numerical)**: Directly input the page number: *"Anchor to Page: [ 14 ] / [ Total: 48 ]"*.
    - **Method B (Drag-and-Drop)**: Drag the floating audio pin badge onto the target page thumbnail in the bottom carousel.
* **Visual Anchor & Synchronous Playback**:
  - **Document-Level (Unassigned)**: Renders a universal audio player pill in the top app bar: **`[ 🎧 02:45 | Lecture Audio ]`**, playable across any page.
  - **Page-Anchored**: Renders an unobtrusive badge in the margin of the assigned page. Tapping it plays the recording synchronously while reviewing that specific page.
* **Global Configuration Toggle**:
  - In Settings under *General*, an optional toggle: **"Lecture Audio Annotations"** (On/Off). Disabling it completely conceals microphone triggers across viewers for students who prefer a strictly visual/text workflow.

---

## 13. Document Perspective Rectification & Optical Enhancement

### 13.1 Real-Time Quadrilateral Boundary Detection
* **On-Device Vision Pipeline**:
  - The camera viewfinder performs real-time edge detection analyzing high-contrast quadrilateral paper boundaries.
  - An animated alignment overlay snaps to document corners when stability is achieved.

### 13.2 Perspective Dewarping & Contrast Filtering
* **Homography Transformation**:
  - Dewarps perspective tilt, desk clutter, and keystoning artifacts into flat, rectangular aspect ratios.
* **Optical Filter Presets**:
  - **Clean Document / Whiteboard**: Eliminates background shadowing, enhances pen contrast, and renders paper as crisp, clean white with deep ink tones.
  - **High-Contrast B&W**: Optimizes handwritten ink strokes for downstream on-device OCR indexing.

---

## 14. Academic Performance & Syllabus Weight Evaluator

### 14.1 Weighted Grading Distribution Engine
* **Syllabus Configuration**:
  - Configurable directly within each subject folder (`FolderDetailScreen`).
  - Input categories with percentage weights: Assignments ($W_1$), Practicums ($W_2$), Midterm ($W_3$), Final Exam ($W_4$), where $\sum W_i = 100\%$.

### 14.2 Prognosis & Required Score Calculation
* **Formulas**:
  - Cumulative Weighted Standing:
    $$\text{Current Average} = \frac{\sum (W_i \times \frac{S_i}{M_i})}{\sum W_i} \times 100\%$$
  - Target Score Projection:
    $$\text{Required Final Score} = \frac{\text{Target Grade} - \sum_{\text{completed}} (W_i \times \frac{S_i}{M_i} \times 100)}{W_{\text{final}}}$$
* **UI Presentation**:
  - An expandable card under the folder header:
    `[ 📊 Syllabus Evaluation: 82.4% Cumulative (Target: Grade A) ▾ ]`.
  - Computes exact requirements to achieve target grades (e.g., *"Requires 88.9% on Final Exam to maintain Grade A"*).

---

## 15. Coursework Archive & Interchange Bundle (`.fotara`)

### 15.1 Academic Coursework Portability
* **Interchange Bundle Specification**:
  - Generates a standalone, compressed `.fotara` archive (open ZIP container) encompassing:
    - Subject folder metadata and subfolder hierarchy.
    - Full-resolution photos with perspective-corrected scans.
    - Vector drawing annotations, multi-sheet sketches, and text notes.
    - Synchronized audio recordings and PDF page pin manifests.
* **One-Tap Colleague Sharing**:
  - Fellow students with Fotara tap the bundle file to import the entire semester course structure in seconds with zero data degradation.

---

## 16. Package Manager & Modular Add-On Architecture

### 16.1 Modular Distribution Strategy
To ensure the base application remains ultra-lightweight (<25 MB), fast to install, and fully accessible on low-spec Android devices, advanced specialized engines are detached from the Core APK into **Modular Add-On Packages**.

* **Core APK (Always Included)**:
  - Spaces, Workspaces, Folders, Subfolders, SQLite v19 schema.
  - Core Camera, Photo Viewer, PDF Page Viewer & Pinning.
  - Standard Text Notes (Markdown, Tables, Checklists).
  - Basic Vector Drawing (Pen, Highlighter, Eraser).
  - 3-Tier Stealth Privacy System (Private Folders, Ghost Workspace, Stealth Space).
  - Class Timetable, Alarms, Spaced Repetition, and Settings.
* **Modular Packages (Optional Add-Ons)**:
  - Heavy font packages, machine learning models, and real-time networking stacks are downloaded only when the user requests or uses the feature.

### 16.2 Package Catalog & Boundary Specifications
| Package ID | Add-On Name | Size | Contents & Justification |
|---|---|---|---|
| `fotara.pkg.latex` | **LaTeX Mathematical Typesetting** | ~4.2 MB | KaTeX offline JavaScript runtime, Computer Modern & AMS math fonts (`.woff2`/`.ttf`), matrix layout engine. Keeps math fonts out of non-STEM users' devices. |
| `fotara.pkg.scanner` | **Document Perspective Rectification** | ~7.8 MB | On-device quadrilateral boundary detection model, perspective dewarping homography transform, high-contrast B&W document filter shaders. |
| `fotara.pkg.collab` | **Live Collaborative Canvas** | ~3.1 MB | WebRTC Data Channel binaries, WebSocket real-time room streaming, multi-peer cursor synchronization engine. Solitary study users avoid network overhead. |
| `fotara.pkg.audio` | **Synchronized Audio Annotations** | ~2.0 MB | Hardware AAC/M4A audio capture pipeline and dynamic waveform visualizer components. |
| `fotara.pkg.ocr_extra` | **Extended Language OCR Models** | ~12.5 MB | Non-Latin offline OCR script models (Japanese, Korean, Arabic, Cyrillic, Devanagari). Latin/English OCR remains built into Core. |

### 16.3 Just-In-Time (JIT) Interception & Installation Dialog
When a student triggers a tool requiring an uninstalled package (e.g., tapping `[ fx ]` in Text Notes without `fotara.pkg.latex`, or opening the Document Scanner mode without `fotara.pkg.scanner`):
1. **Action Interception**: The tool action is paused.
2. **Modal Dialog Prompt**:
   - Presents an informative dialog displaying package name, size, offline guarantee, and progress:
     ```
     ┌────────────────────────────────────────────────────────┐
     │                [ fx ] LaTeX Math Add-On                │
     ├────────────────────────────────────────────────────────┤
     │ Mathematical typesetting requires the offline LaTeX   │
     │ package (KaTeX runtime & AMS font symbols).            │
     │                                                        │
     │ • Package Size: 4.2 MB                                 │
     │ • Offline Ready: 100% functional without internet once │
     │   installed.                                           │
     │                                                        │
     │   [==========================         ] Downloading 68%│
     │                                                        │
     │ [ Cancel ]                       [ Install Add-On ]   │
     └────────────────────────────────────────────────────────┘
     ```
3. **Seamless Resume**: Upon download completion and checksum verification, the package activates dynamically in memory, and the student's original action immediately executes with zero app restart.

### 16.4 Package Manager Settings Hub
* **Location in Settings**: A dedicated entry under *Storage & Data Management* (or direct card): **"Add-On Packages"**.
* **Capabilities**:
  - View all installed vs. available packages.
  - Track storage consumption per package.
  - 1-tap **Uninstall** button to instantly reclaim device storage.
  - 1-tap **Check for Package Updates**.

### 16.5 Standalone Dynamic Package Loader Architecture
* **Vendor-Independent Local Package Delivery**:
  - Independent of Google Play Dynamic Feature Delivery, enabling 100% compatibility across GitHub Releases, F-Droid, and sideloaded APKs.
  - Packages are distributed as cryptographically signed `.fpkg` archives (ZIP-compressed assets).
  - Downloaded to internal isolated storage: `context.filesDir/packages/<package_id>/`.
  - Loaded dynamically into runtime ClassLoader / AssetManager at runtime with SHA-256 verification.

---

## 17. Implementation Phasing & Release Roadmap

```
┌────────────────────────────────────────────────────────────────────────┐
│ PHASE 42: FOUNDATION, HIERARCHY & PERSISTENCE                          │
│ 1. Space Super-Hierarchy & SQLite Schema v19 Migration                │
│ 2. Tab State Persistence (SaveableStateHolder & Sub-Screen Retention) │
│ 3. Canonical Re-Tap Bottom Bar Pop-to-Root Navigation                  │
├────────────────────────────────────────────────────────────────────────┤
│ PHASE 43: 3-TIER STEALTH PRIVACY SYSTEM                                │
│ 1. Private Folder (Pull-to-Refresh & Hold 2s Gesture)                 │
│ 2. Private Workspace (Ghost Mode & Tab Bar Reveal)                     │
│ 3. Private Space (Title Long-Press Vault & .nomedia Shielding)         │
├────────────────────────────────────────────────────────────────────────┤
│ PHASE 44: NOTE & CANVAS SUPERPOWERS                                    │
│ 1. LaTeX Mathematical Typesetting & Structured Tables in Text Notes    │
│ 2. Shape Auto-Correct (Draw & Hold 400ms) & Bézier Smoothing           │
│ 3. Text Layers in Drawing Canvas & PDF Page Editor                     │
│ 4. Multi-Sheet Drawing Canvas (Max 10 Sheets) & Combined Export        │
├────────────────────────────────────────────────────────────────────────┤
│ PHASE 45: ACADEMIC SCHEDULING, ALARMS & WIDGETS                        │
│ 1. Weekly Class Timetable & H-10min Quick-Capture Notification         │
│ 2. Spaced Repetition Review Engine (H+1, H+3, H+7 Days)                │
│ 3. Urgent Anti-Procrastination Alarm (Photo Proof Dismiss Challenge)   │
│ 4. Android Home Screen Photo Widgets (Specific Single & Carousel)      │
├────────────────────────────────────────────────────────────────────────┤
│ PHASE 46: ACADEMIC EVALUATION, AUDIO ANNOTATIONS & DOCUMENT CAPTURE   │
│ 1. Synchronized Audio Annotations with PDF Page Drag/Number Anchoring │
│ 2. Document Perspective Rectification & Optical Enhancement Filter     │
│ 3. Academic Performance & Syllabus Weight Evaluator in Folders         │
│ 4. Active Recall Masking & Occlusion Assessment                        │
│ 5. Coursework Archive & Interchange Bundle (.fotara) Export/Import     │
├────────────────────────────────────────────────────────────────────────┤
│ PHASE 47: FOTARA PACKAGE MANAGER & MODULAR ADD-ONS                     │
│ 1. Dynamic Package Loader Engine (Signed .fpkg Runtime Asset Loader)   │
│ 2. Just-In-Time (JIT) Feature Interception & Installation Dialogs      │
│ 3. Package Management Hub in Settings (Download, Storage, Uninstall)   │
├────────────────────────────────────────────────────────────────────────┤
│ PHASE 48: FRIENDS SYSTEM & SETTINGS INTEGRATION                        │
│ 1. Dedicated "Friends" Card in Settings (Below Profile, Above General) │
│ 2. FriendsScreen (Tag Discovery, QR Code Scan, Online Indicators)      │
├────────────────────────────────────────────────────────────────────────┤
│ PHASE 49: LIVE COLLABORATIVE CANVAS & 2.0.0 RELEASE SHIP               │
│ 1. Room Code Session Engine (6-Digit Codes & Vector Broadcasting)      │
│ 2. Peer Cursor Visuals & Live Stroke Merging                           │
│ 3. Full Verification, Release 2.0.0 Build Assembly & Documentation     │
└────────────────────────────────────────────────────────────────────────┘
```
