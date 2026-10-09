// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

# Parallel Dual-Agent Architecture & Execution Strategy

## 1. Objective & Non-Collision Guarantee

To accelerate the 1.9.0 escalation, work is partitioned between two concurrent Gemini agents operating on **disjoint, mutually exclusive file domains**.

```
┌─────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                DUAL-AGENT INDEPENDENCE MATRIX                                   │
├────────────────────────────────────────────────┬────────────────────────────────────────────────┤
│ AGENT 1 (Current Session - Deep Context)       │ AGENT 2 (Fresh Session - High Speed)           │
│ Scope: Phase 42 (Schedule) & Phase 43 (Nav)    │ Scope: Phase 44 (STEM Notes & Canvas Engine)   │
├────────────────────────────────────────────────┼────────────────────────────────────────────────┤
│ EXCLUSIVE WRITE DOMAINS:                       │ EXCLUSIVE WRITE DOMAINS:                       │
│ • ui/home/HomeScreen.kt                        │ • ui/textnote/TextNoteEditorScreen.kt          │
│ • ui/components/HomeBottomNavBar.kt            │ • ui/textnote/TextEditorOps.kt                 │
│ • ui/notes/NotesScreen.kt (Capsule only)       │ • ui/textnote/MarkdownVisualTransformation.kt  │
│ • feature/schedule/** (Parser, Cutoff, Models) │ • ui/canvas/** (DrawingEngine, TextLayer)      │
│ • data/db/** (class_schedules SQLite table)    │ • ui/pdf/PdfPageEditorScreen.kt                │
│ • data/repository/ScheduleRepository.kt        │ • data/model/Drawing*.kt                       │
├────────────────────────────────────────────────┼────────────────────────────────────────────────┤
│ STRICTLY FORBIDDEN FOR AGENT 1:                │ STRICTLY FORBIDDEN FOR AGENT 2:                │
│ ❌ Do NOT touch ui/textnote/**                 │ ❌ Do NOT touch ui/home/**                     │
│ ❌ Do NOT touch ui/canvas/**                   │ ❌ Do NOT touch ui/components/HomeBottomNavBar │
│ ❌ Do NOT touch ui/pdf/PdfPageEditorScreen.kt  │ ❌ Do NOT touch ui/notes/NotesScreen.kt        │
│                                                │ ❌ Do NOT touch feature/schedule/**            │
└────────────────────────────────────────────────┴────────────────────────────────────────────────┘
```

**Collision Risk: 0.0%**. Neither agent touches any file owned by the other agent.

---

## 2. Recommended Git Isolation (Git Worktree)

To prevent file locking and Gradle daemon collisions on Windows when both agents run in parallel:

```powershell
# In terminal, create an isolated worktree branch for Agent 2:
git checkout -b feature/phase44-stem-canvas
cd ..
git worktree add fotara-agent2 feature/phase44-stem-canvas
```
* **Agent 1** works in: `c:\...\Coding\Fotara` (on `release/1.7.0` or `feature/phase42-43-schedule-nav`).
* **Agent 2** works in: `c:\...\Coding\fotara-agent2` (on `feature/phase44-stem-canvas`).
* When both agents finish, merging is clean and automatic: `git merge feature/phase44-stem-canvas`.

---

## 3. Ready-to-Paste Prompt for Agent 2 (Fresh Session)

Copy and paste the exact block below into the new Gemini session:

```markdown
You are an autonomous senior Android engineer for Arinara Network working on Fotara.
You have been assigned EXCLUSIVE ownership of PHASE 44: STEM NOTE & CREATIVE CANVAS SUPERPOWERS.
Another agent is concurrently working on navigation and schedule in a separate space. 

### NON-COLLISION CONTRACT:
You are STRICTLY FORBIDDEN from modifying or touching:
- `ui/home/HomeScreen.kt`
- `ui/components/HomeBottomNavBar.kt`
- `ui/notes/NotesScreen.kt`
- `ui/notes/WorkspaceTabBar.kt`
- `feature/schedule/**`

### YOUR EXCLUSIVE DOMAIN (PHASE 44):
You exclusively own and edit:
1. `app/src/main/java/com/arinara/fotara/ui/textnote/` (TextNoteEditorScreen, TextEditorOps, MarkdownVisualTransformation)
2. `app/src/main/java/com/arinara/fotara/ui/canvas/` (DrawingEngine, DrawingCanvas, DrawingViewModel)
3. `app/src/main/java/com/arinara/fotara/ui/pdf/PdfPageEditorScreen.kt`
4. `app/src/main/java/com/arinara/fotara/data/model/` (Drawing and note models)

### DELIVERABLES:
1. **LaTeX Math Rendering**:
   - Support `$ ... $` inline math and `$$ ... $$` display block equations in Text Notes.
   - Render mathematical formulas using native KaTeX parsing without disrupting existing Markdown spans.
2. **Markdown Table Editor with Formatting Constraints**:
   - Insert and edit Markdown tables in Text Notes.
   - STRICT CONSTRAINT: Inside table cells, only inline formatting (Bold, Italic, Strikethrough, Code, Math) is enabled. All block tools (Headings H1-H3, Blockquotes, Checklists, Dividers) must be disabled (alpha = 0.38f) to protect table syntax.
3. **Shape & Curve Auto-Correct (Draw & Hold 400ms)**:
   - In DrawingEngine Pen tool: holding stylus/touch for 400ms after drawing snaps rough strokes into geometric primitives (rectangle, circle, ellipse, triangle, straight arrow) with haptic feedback and Bézier smoothing.
4. **Text Layers `[ T ]` Tool**:
   - In Drawing Canvas and PdfPageEditorScreen: add `[ T ]` tool to the dock.
   - Tapping places a movable, resizable, rotatable vector text annotation with font size slider (12-48sp), ElmsSans typography, 8-color palette, and background styles.
5. **Multi-Sheet Drawing Canvas (Max 10 Sheets)**:
   - Sheet switcher dock: `[ Sheet 1 ] [ Sheet 2 ] ... [ + ]` (capped at 10 sheets).
   - Per-sheet undo/redo stacks and combined multi-page export.

### ENGINEERING INVARIANTS:
- Offline-first, raw SQLite / direct serialization, manual DI, 0 dummy data.
- UI styling: ElmsSans typography, MidnightNavy palette, haptic feedback on snaps.
- Run tests: `./gradlew testDebugUnitTest` and ensure 100% test pass.
- Proceed autonomously: inspect files, implement Phase 44, test, and report completion.
```
