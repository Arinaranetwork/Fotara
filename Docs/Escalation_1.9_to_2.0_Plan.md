// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

# Fotara: Strategic Escalation Plan from 1.9.0 to 2.0.0

## 1. Executive Summary & Escalation Rationale

This document establishes the official two-stage escalation roadmap bridging Fotara from the current 1.8 series to the landmark 2.0.0 release.

Rather than attempting an abrupt monolithic leap directly from 1.8.4 to 2.0.0, the roadmap executes a structured, risk-mitigated two-tier rollout:
1. **Fotara 1.9.0 ("Academic Superpowers & Interaction Stability")**:
   - Delivers immediate student utility and UX stabilization without breaking existing database schemas.
   - Focuses on the Daily Class Timetable with lightweight Excel/DOCX table parsing, the Next-Day Rollover Cutoff Engine, the 1-Line Dynamic Schedule Capsule in Notes, Android Home Screen Timetable & Photo Widgets, bottom navigation tab persistence (`SaveableStateHolder`) with canonical Re-Tap Pop-to-Root, STEM note tools (LaTeX math and Markdown tables with constrained formatting), and Drawing Engine superpowers (Shape auto-correct 400ms, text layers, multi-sheet canvas).
2. **Fotara 2.0.0 ("Local-First Academic Operating System")**:
   - Executes the architectural leap to the multi-vault **Space Super-Hierarchy** with SQLite schema v19 migration.
   - Introduces the 3-Tier Stealth Privacy System (Private Folder, Private Workspace, Private Space), Syllabus Evaluator, Active Recall Occlusion Tape, standalone `.fpkg` Package Manager (<25MB base APK), the in-app Friends System in Settings, and the Live Collaborative Study Canvas with 6-digit room codes.

---

## 2. Two-Stage Release Architecture

```
                    ┌────────────────────────────────────────────────────────┐
                    │               CURRENT STATE: FOTARA 1.8.4              │
                    │         Folder-based Coursework Organizer & OCR        │
                    └───────────────────────────┬────────────────────────────┘
                                                │
                                                ▼
┌─────────────────────────────────────────────────────────────────────────────────────────────────┐
│ STAGE 1: FOTARA 1.9.0 — ACADEMIC SUPERPOWERS & INTERACTION STABILITY                           │
│ (Phases 42 – 45)                                                                                │
├─────────────────────────────────────────────────────────────────────────────────────────────────┤
│ • Phase 42: Academic Timetable, Next-Day Rollover & 1-Line Notes Capsule                        │
│ • Phase 43: Tab State Persistence (SaveableStateHolder) & Canonical Re-Tap Pop-to-Root Nav      │
│ • Phase 44: STEM Note & Canvas Superpowers (LaTeX Math, Tables, Shape Auto-Correct, Text Layer) │
│ • Phase 45: Android Timetable & Photo Widgets, Audio Annotations, & Anti-Procrastination Alarm  │
└───────────────────────────────────────────────┬─────────────────────────────────────────────────┘
                                                │
                                                ▼
┌─────────────────────────────────────────────────────────────────────────────────────────────────┐
│ STAGE 2: FOTARA 2.0.0 — LOCAL-FIRST ACADEMIC OPERATING SYSTEM                                   │
│ (Phases 46 – 51)                                                                                │
├─────────────────────────────────────────────────────────────────────────────────────────────────┤
│ • Phase 46: Space Super-Hierarchy & SQLite Schema v19 Migration (5-Tier Vault System)          │
│ • Phase 47: 3-Tier Stealth Privacy System (Private Folder, Ghost Workspace, Stealth Space Vault)│
│ • Phase 48: Academic Evaluation, Coursework Bundle (.fotara) & Document Rectification           │
│ • Phase 49: Fotara Package Manager & Modular Add-On Runtime (.fpkg, <25MB Base APK)            │
│ • Phase 50: Dedicated In-App Friends System (Settings Integration, Tags & QR Scanner)           │
│ • Phase 51: Live Collaborative Study Canvas (6-Digit Room Codes, Stroke Sync & 2.0.0 Ship)      │
└─────────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Stage 1: Fotara 1.9.0 Specifications & Phases

### Phase 42 — Academic Timetable, Next-Day Rollover & 1-Line Notes Capsule
* **Goal**: Provide instant daily schedule awareness for students with zero-friction file ingestion.
* **Deliverables**:
  1. **1-Line Dynamic Schedule Capsule**: Placed strictly below `WorkspaceTabBar` and above filter chips in `NotesScreen.kt` (~36dp height). Shows ongoing class countdown or rolls over to tomorrow's first lecture.
  2. **Lightweight Excel (`.xlsx`) & Word (`.docx`) Table Parser**: Uses Android platform streaming `XmlPullParser` on ZIP XMLs (`sheet1.xml`, `document.xml`) to extract timetable grids with zero heavy external libraries.
  3. **Next-Day Rollover Cutoff Engine**: Single configurable cutoff time `schedule_rollover_time` (Default: `18:00`). Pre-cutoff displays today; post-cutoff automatically displays tomorrow's classes for evening book preparation. Weekend rollover transitions Friday evening to Monday morning.
  4. **Class Timetable Management Sheet**: Full 5/7-day weekly grid editor, manual entry, folder link mapping.
  5. **Proactive Timetable Notifications**: Morning digest at 06:30, H-10min pre-lecture heads-up with 1-tap capture deep link, and evening cutoff notification.

### Phase 43 — Tab State Persistence & Canonical Re-Tap Navigation
* **Goal**: Eliminate active screen state loss when switching bottom tabs and provide standard mobile navigation ergonomics.
* **Deliverables**:
  1. **Bottom Tab State Retention**: Implement `SaveableStateHolder` composition preservation in `HomeScreen.kt` across `Home`, `Notes`, and `Settings` tabs. Form inputs, scroll positions, and sub-pages remain intact.
  2. **Canonical Re-Tap Pop-to-Root**: Tapping the active bottom navigation tab while on a child screen (e.g. Settings > Profile) pops back to the root menu.
  3. **Canonical Re-Tap Scroll-to-Top**: Tapping the active bottom tab while already at root smoothly scrolls `LazyColumn` / `LazyVerticalGrid` to index 0.

### Phase 44 — STEM Note & Canvas Superpowers
* **Goal**: Equip students with rigorous mathematical notation and high-speed drawing intelligence.
* **Deliverables**:
  1. **LaTeX Mathematical Typesetting**: Support `$ ... $` inline math and `$$ ... $$` display block equations in Text Notes rendered via native KaTeX parser.
  2. **Markdown Table Editor with Formatting Constraints**: Cell grid editor where only inline styling (bold, italic, code, math) is enabled, while block-level tools (headings, quotes, lists) are disabled to protect Markdown table syntax.
  3. **Shape & Curve Auto-Correct (Draw & Hold 400ms)**: Pen stroke hold for 400ms triggers haptic snap to geometric primitives (circles, rectangles, triangles, straight arrows) with Bézier smoothing.
  4. **Text Layers `[ T ]` Tool**: Movable, resizable, rotatable vector text annotations on Drawing Canvas and PDF Page Editor with `ElmsSans` typography and 8-color palette.
  5. **Multi-Sheet Drawing Canvas**: Bottom sheet switcher supporting up to 10 sheets per drawing document with multi-page export.

### Phase 45 — Home Screen Widgets, Audio Annotations & Anti-Procrastination Alarm
* **Goal**: Extend Fotara to the Android Home screen and deliver sensory study support.
* **Deliverables**:
  1. **Android Home Screen Timetable Widget (`AppWidgetProvider`)**: 4x2 and 4x4 interactive home screen widgets syncing in real time with the next-day rollover cutoff logic.
  2. **Android Home Screen Photo Widgets**: Specific Single Photo Widget (pinning 1 chosen formula cheat sheet or diagram permanently) and Coursework Carousel Widget (4x2 grid of recent photos).
  3. **Synchronized Audio Annotations**: 1-tap instant lecture voice recorder (up to 5 min) with strictly optional PDF page drag/number anchoring.
  4. **Urgent Anti-Procrastination Alarm**: System alarm stream alert requiring **Photo Proof** (taking a photo of completed coursework) to dismiss.
  5. **1.9.0 Beta Assembly & Release**: Version bump to `1.9.0 Beta` (code 34), test suite pass, changelog, and APK packaging.

---

## 4. Stage 2: Fotara 2.0.0 Specifications & Phases

### Phase 46 — Space Super-Hierarchy & SQLite Schema v19 Migration
* **Goal**: Expand Fotara into a 5-tier organizational hierarchy with multi-vault isolation.
* **Deliverables**:
  1. **5-Tier Organizational Hierarchy**: $\mathbf{Space} \rightarrow \mathbf{Workspace} \rightarrow \mathbf{Folder} \rightarrow \mathbf{Subfolder} \rightarrow \mathbf{Notes/Media}$.
  2. **SQLite Schema v19 Migration**: Create `spaces` table (`id`, `uuid`, `name`, `icon_key`, `color_hex`, `is_private`, `sort_order`, timestamps), add `space_id` foreign key to `workspaces`, and auto-seed existing data into `"Default Space"`.
  3. **Header Space Switcher Dropdown**: Accessible via brand title `Fotara ▾ [Space Name]` in `HomeScreen` and `NotesScreen`.

### Phase 47 — 3-Tier Stealth Privacy System
* **Goal**: Provide impenetrable, zero-leak stealth privacy for sensitive coursework and personal vaults.
* **Deliverables**:
  1. **Private Folder Vault**: Hidden by default; revealed via **Pull-to-refresh & hold 2 seconds** gesture on folder list + `BiometricPrompt`.
  2. **Private Workspace**: Ghost mode workspace tab revealed only via 1.5s long-press on the Home navigation icon.
  3. **Private Space Vault**: Concealed vault opened via 2s long-press on brand title "Fotara" with `.nomedia` media shielding to prevent external gallery indexing.

### Phase 48 — Academic Evaluation, Coursework Bundle & Document Rectification
* **Goal**: Provide academic grading projections, coursework interchange, and camera enhancement.
* **Deliverables**:
  1. **Course Syllabus Weight Evaluator**: Folder-level syllabus grade weighting calculating required final exam scores to achieve target letter grades.
  2. **Active Recall Masking (Occlusion Tape)**: Diagnostic tape tool over anatomical diagrams, formulas, and vocabulary with tap-to-reveal quiz mode.
  3. **Document Perspective Rectification**: Real-time quadrilateral document edge detection and high-contrast whiteboard filter.
  4. **Coursework Archive & Interchange Bundle (`.fotara`)**: Compressed semester package format for 1-tap folder export and peer import.

### Phase 49 — Fotara Package Manager & Modular Add-On Architecture
* **Goal**: Decouple heavy specialized features to keep the base APK strictly under 25 MB.
* **Deliverables**:
  1. **Standalone Signed `.fpkg` Dynamic Loader**: Runtime asset/class loader compatible with GitHub Releases, F-Droid, and sideloaded APKs.
  2. **Just-In-Time (JIT) Interception Dialogs**: Tapping uninstalled features (e.g. scanner or collab) displays an on-demand download dialog and resumes immediately upon completion.
  3. **Package Management Hub in Settings**: Dedicated hub under Storage & Data to view, update, and uninstall packages to reclaim space.

### Phase 50 — Dedicated In-App Friends System
* **Goal**: Enable social academic networking directly within Fotara.
* **Deliverables**:
  1. **Friends Entry Card in Settings**: Placed strictly below Profile and above General.
  2. **FriendsScreen**: User tag management (`@handle`), QR code generation/scanning, study presence status, and 1-tap canvas invites.

### Phase 51 — Live Collaborative Study Canvas & 2.0.0 Release Ship
* **Goal**: Real-time peer drawing and official assembly of the 2.0.0 Major Milestone.
* **Deliverables**:
  1. **6-Digit Room Code Session Engine**: Low-latency vector stroke broadcasting (`FT-XXXX`).
  2. **Peer Cursors & Stroke Merging**: Colored peer cursors with participant tags and offline fallback.
  3. **2.0.0 Release Build Assembly**: Version bump to `2.0.0 Beta` (code 35), full regression testing, banner pairing, release notes, and Stable promotion proposal via Confirm Gate.

---

## 5. Phase Mapping Matrix

| Phase | Target Version | Milestone Focus | Deliverables |
|---|---|---|---|
| **Phase 42** | **1.9.0** | Daily Schedule & Parser | 1-Line Notes Capsule, Excel/DOCX Parser, Cutoff Rollover Engine, Timetable Sheet |
| **Phase 43** | **1.9.0** | Navigation Stability | Tab State Persistence (`SaveableStateHolder`), Canonical Re-Tap Pop-to-Root / Scroll-to-Top |
| **Phase 44** | **1.9.0** | Note & Canvas Superpowers | LaTeX Math ($ & $$), Markdown Tables, Shape Auto-Correct (400ms), Text Layers, Multi-Sheet Canvas |
| **Phase 45** | **1.9.0** | Widgets & Audio Capture | Timetable & Photo Widgets, Audio Annotations (Optional PDF Drag), Anti-Procrastination Alarm, 1.9.0 Release |
| **Phase 46** | **2.0.0** | Hierarchy & Schema Migration | Space Super-Hierarchy, SQLite Schema v19, Title Header Space Switcher |
| **Phase 47** | **2.0.0** | Stealth Privacy | Private Folder (Pull 2s Hold), Ghost Workspace, Stealth Space Vault & `.nomedia` Shielding |
| **Phase 48** | **2.0.0** | Evaluation & Bundles | Syllabus Weight Evaluator, Occlusion Tape, Document Dewarping, `.fotara` Archive Bundle |
| **Phase 49** | **2.0.0** | Package Manager | Dynamic `.fpkg` Loader, JIT Interception Dialogs, Package Settings Management Hub (<25MB Base APK) |
| **Phase 50** | **2.0.0** | Friends System | Settings Card below Profile / above General, `FriendsScreen`, Tags & QR Scanner |
| **Phase 51** | **2.0.0** | Live Collaborative Canvas | 6-Digit Room Codes, Vector Stroke Sync, Peer Cursors, 2.0.0 Major Milestone Ship |
