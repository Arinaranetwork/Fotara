# Phase 10 - CanvasAndSearchImprovements

## Goal
Harden the Canvas Note drawing experience, improve searchability across OCR and DOCX documents, fix UI layout collisions, and establish robust canvas boundaries and autosave guarantees for the Fotara 1.5.2 release.

## Scope
1. Canvas Autosave Engine: Debounced autosave after every committed change, synchronous flush on pause/stop/back, single-writer Mutex queue, atomic file/DB write, retry with backoff, and interactive save-status indicator (Saving / Saved / Failed with tap-to-retry).
2. Canvas Panning & Transform Math: 1:1 finger tracking at any distance, eliminating drift/reversals from focal point changes and double translation, spatial viewport culling, and canvas extent clamping.
3. Image Insertion ("Add with image"): Photo Picker / SAF stream handling, downsampling to max 2048px, EXIF orientation correction, storage copy in app-private storage, new layer placed at viewport center, undoable and autosaved, and user-visible error handling.
4. Canvas Top Bar Layout: Adaptive top bar preventing pill overlaps at 360dp, font scale 1.3, and landscape. Single-line title with ellipsis, compact save status badge, and overflow menu for lower-priority actions.
5. Zoom Pill Placement: Position zoom pill above the collapsible tool panel so fit-to-screen remains accessible regardless of panel expanded/collapsed state and navigation bar insets.
6. Broom-Icon Tool (Active Layer Eraser): Functional stroke eraser removing touched strokes on active layer, distinct active selection state, undoable, and autosaved.
7. Folder Card Facing Orange Glow: Dynamic facing glow orientation between adjacent glowing cards in the home screen grid (edges for horizontal/vertical neighbors, corners for diagonal neighbors).
8. OCR and DOCX Search Indexing: Text extraction from DOCX documents and OCR photo text indexed into SQLite FTS4 with database migration; debounced, diacritic- and case-insensitive multi-word/prefix search; title/subject ranked before content matches; snippet generation with [OCR] / [DOCX] badges.
9. Canvas Import from Existing Notes: Picker showing only image-based notes (photos, scans, drawings) to insert as a new snapshot layer scaled to viewport center.
10. Drawing Mode Hard Limits: Centralized configuration file (`CanvasConfig.kt`) defining 20,000 x 20,000 logical px canvas extent, 50 max layers, 2048px image downscale limit, memory budget limits, and snackbar warnings when limits are reached.
11. Text Note Editor Engine & Rich Interactive Rendering: Pure text-editing transformation functions for all toolbar tools (Undo/Redo, Bold, Italic, Strikethrough, Inline code, H1-H3, Quote, Bullet list, Numbered list, Checkbox, Indent/Outdent, Link, Code block, Horizontal rule), single undo/redo step, toolbar active state tracking caret context, Enter key list continuation/exit, and rich interactive rendering (real interactive checkboxes `- [ ]` <-> `- [x]`, horizontal rule dividers, hanging indent lists) with unit deletion.
12. Canvas Note Contextual Action Panel: Standardize Canvas Note long-press to open the complete note action bottom sheet (Rename, Color Label, Deadline, Schedule Reminder, Move, Share/Export PNG, Select, Delete) instead of jumping directly to the schedule timer dialog.
13. App-Wide Elms Sans Font Migration & Italic Elimination: Adopt Elms Sans with strictly three weights (Light 300, Medium 500, Bold 700) mapped by context, remove all UI italics project-wide (FontStyle.Italic, textStyle="italic", Paint italics), maintain non-italic monospace for code, synthetic slant for editor italic formatting, and bar-styled blockquotes without italics.
14. Folder Screen Header Action Button Spacing: Eliminate layout overlap between the round Add (+) button and the Kebab (⋮) overflow menu button on folder screen headers, preserving 48dp minimum touch targets.

## Out Of Scope
- Modifying or refactoring the Canvas selection tool (bounding box, duplicate, bring to front, send to back, layer menu, delete).
- Changing color palette of existing non-canvas screens.
- Modifying unrelated database entities or cloud Supabase sync logic.

## Features
### Feature 1: Robust Canvas Autosave & Flush Pipeline
- Debounced autosave triggers after stroke commit, element transform, layer mutation, or image insertion.
- Lifecycle observer on `ON_PAUSE` / `ON_STOP` and Compose `BackHandler` triggers immediate flush.
- Single-writer mutex ensures writes are serialized without database locked exceptions.
- Status indicator displays `Saving...`, `Saved`, or `Save Failed (Tap to retry)`.

### Feature 2: 1:1 Canvas Pan/Zoom Engine & Viewport Culling
- Fix translation accumulation: eliminate double-application of translation deltas during zoom gestures.
- Stable gesture handling: avoid delta jumps when switching between 1-finger and 2-finger touches.
- Viewport culling: only render elements whose bounding boxes intersect the active screen viewport.
- Clamped view transform preventing navigation beyond 20,000 x 20,000 extent.

### Feature 3: Resilient Image Insertion Engine
- Safely read URIs with content resolver off main thread.
- Downscale imported bitmaps using `inSampleSize` to at most 2048px on the longest dimension.
- Apply EXIF matrix rotation to store properly oriented PNG files in app-private canvas storage.
- Create new layer at viewport center with undo/redo action recording and autosave trigger.

### Feature 4: Responsive Top Bar Header
- Adaptive layout with `Modifier.weight(1f, fill = false)` on title to ensure it shrinks and ellipsizes without pushing the right action pill.
- Right action pill collapses secondary actions (Share, History) into an overflow popup menu when screen width < 400dp or in landscape.

### Feature 5: Floating Zoom Pill Clearance
- Anchor the zoom pill relative to the collapsible bottom tool panel so it floats comfortably above it whether expanded or collapsed.

### Feature 6: Broom-Icon Stroke Eraser
- Eraser tool tracks pointer path on the active layer and removes strokes whose bounds or path intersect the eraser radius.
- Deletions are committed to history stack for undo/redo and trigger autosave.

### Feature 7: Facing Directional Orange Glow on Folder Cards
- Compute grid coordinates `(row, col)` for each folder card in the home screen grid.
- Check 8-way neighbors for active glow state. If an adjacent card has a glow, orient the gradient glow to face the neighbor (edge for cardinal, corner for diagonal).

### Feature 8: Deep OCR & DOCX Full-Text Search
- Extract text content from Word DOCX notes (paragraphs and tables) using `DocxParser` and store in searchable database table / FTS4 index.
- Migrate database to version 15 (v15 adds document_notes_fts virtual table).
- Provide background backfill for existing notes with OCR and DOCX text.
- Rank search results: Title/Subject exact matches > Title/Subject prefix matches > Note Content matches.
- Format content snippets highlighting matched query terms with `[OCR]` or `[DOCX]` badges.

### Feature 9: Import Image Note to Canvas
- Modal picker allowing navigation through folders and image-based notes only (Photos, Scans, Drawings).
- Inserts chosen note as a cloned bitmap layer without mutating original note.

### Feature 10: Canvas Safety Limits
- `CanvasConfig.kt` holds: `CANVAS_EXTENT = 20000f`, `MAX_LAYERS = 50`, `MAX_IMAGE_DIMENSION = 2048`, `MAX_ELEMENTS = 10000`.
- Pan translation is clamped to `[-10000f, 10000f]`.
- User notifications via snackbar when layer or element limit is reached.

### Feature 11: Text Note Editor Engine & Rich Interactive Rendering
- Pure functions `(text, selection) -> (newText, newSelection)` for every tool:
  - Inline: Bold (`**`), Italic (`*`), Strikethrough (`~~`), Inline code (`` ` ``). Toggle wrap/unwrap, cursor inside when empty.
  - Lines: H1 (`# `), H2 (`## `), H3 (`### `), Quote (`> `), Bullet (`- `), Numbered (`1. `), Checkbox (`- [ ] `). Line-prefix toggle/replace, cursor placed after marker.
  - Indent/Outdent: 2-space tab indent/outdent with numbered list auto-renumbering.
  - Enter Key: auto-continue bullet/numbered/checkbox/quote; empty item exits list.
  - Link: modal dialog inserting `[text](url)`.
  - Code Block: fenced code block insertion.
  - Horizontal Rule: `---` on dedicated line.
  - Single atomic undo/redo step per action.
  - Real interactive checkboxes (`- [ ]` <-> `- [x]`), horizontal dividers, hanging-indent bullets.
  - Safe unit deletion with backspace without cursor jumps into hidden markers.

### Feature 12: Canvas Note Contextual Action Panel Parity
- Long-press on Canvas note opens standard action sheet: Rename, Color Label, Deadline, Schedule Reminder (opens schedule dialog), Move, Share/Export PNG, Select, Delete.
- Exclude unsupported actions (Split to Images).

### Feature 13: Elms Sans Font Migration & Complete Italic Elimination
- Package `ElmsSans-Light.ttf` (300), `ElmsSans-Medium.ttf` (500), `ElmsSans-Bold.ttf` (700) into font resources.
- Strict 3-weight contextual mapping: Light (secondary/captions), Medium (body/buttons/labels), Bold (titles/headers/emphasis).
- Strip `FontStyle.Italic`, `textStyle="italic"`, and Paint italics project-wide.
- Monospace font for code without italics; synthetic slant for editor italic formatting; border/indent styling for blockquotes without italics.

### Feature 14: Folder Screen Header Action Button Spacing
- Add explicit separation between Add (+) circular button and Kebab (⋮) menu button with 48dp minimum touch bounds.

### Feature 15: Direct Hardware-Accelerated Vector Rendering & Black Tile Elimination
- Eliminate tile-missing dark grid holes during zoom and drawing: Replace asynchronous offscreen bitmap tile blitting in `CanvasRenderer` with direct hardware-accelerated vector and image rendering.
- Viewport culling: QuadTree spatial index queries only elements intersecting the visible viewport, sorting elements deterministically by layer order and zIndex.
- Single-point dot precision: Render single-point dots (`pts.size == 1`) with `Paint.Style.FILL` to produce solid, crisp circular points without hollow artifacts or black rectangles.
- Eliminate opaque black tiles: Remove `Bitmap.Config.RGB_565` fallback from `TileCacheManager.obtainBitmap` to guarantee that transparency is never corrupted into pitch-black pixels (`0x0000`).
- Seamless image rendering: Render image elements directly from the decoded in-memory `imageBitmapCache` with hardware acceleration.

## UI Mockup
```
Canvas Top Bar (Narrow 360dp):
[<-] [Title... [Alpha]] [Saved] | [Undo] [Redo] [Layers] [+] [More ⋮]

Home Grid Facing Glow:
+-------------------+ +-------------------+
| Matematika Lanjut | | Kimia             |
|          [Glow \ ]| |                   |
+-------------------+ +-------------------+
+-------------------+ +-------------------+
| Matematika Wajib  | | Fisika            |
| [ / Glow]         | |                   |
+-------------------+ +-------------------+

Folder Header Action Buttons:
[<-] [Folder Title]                   (+)  [12dp]  (⋮)
```

## Logic Notes
- Autosave uses Kotlin `Mutex` and `StateFlow<SaveState>` with states `Idle`, `Saving`, `Saved`, `Error(Throwable)`.
- Coordinate transform math: `worldPoint = (screenPoint - translation) / scale`. Viewport panning applies `deltaScreen / scale` to world translation.
- SQLite FTS4 migration: create `notes_content_fts` virtual table, index DOCX paragraphs, and link via `note_id`.
- Text editor pure functions: `TextEditorOps` object with pure methods for unit testability without Android runtime dependencies.
- Font system: `ElmsSansFontFamily` configured in `Type.kt` and applied to `MaterialTheme`.

## Risks
- Risk: Concurrent database write during backgrounding causing SQLite lock -> Mitigation: Mutex-protected single-writer queue with timeout and atomic transaction.
- Risk: High-resolution images causing OutOfMemoryError -> Mitigation: Sub-sample decoding with `inJustDecodeBounds` bounding max dimension to 2048px.
- Risk: FTS migration corrupting database on app update -> Mitigation: Safe SQLite transaction inside `onUpgrade` with fallback rebuild.
- Risk: OffsetMapping misalignment during rich text rendering -> Mitigation: Strict bidirectional mapping for interactive elements.

## Dependencies
- Phase 8 (Canvas Note Engine)
- Phase 7 (Native Text & DOCX Parser)
- Phase 2 (OCR Engine)

## Acceptance Criteria
- [x] Canvas autosave flushes immediately on back navigation and app backgrounding.
- [x] Pan movement tracks finger 1:1 at 25%, 100%, and 400% zoom without lag or reversal.
- [x] "Add with image" successfully loads high-res and EXIF-rotated photos into a new layer.
- [x] Canvas top bar does not overlap at 360dp width and 1.3 font scale.
- [x] Zoom pill is fully visible and clickable when bottom tool panel is expanded.
- [x] Broom tool cleanly erases touched strokes on active layer and supports undo/redo.
- [x] Folder cards with orange glows orient glows toward adjacent glowing neighbors.
- [x] Search query matches text inside DOCX and OCR notes with badges and snippets.
- [x] "From notes" picker inserts image-based notes as canvas layers.
- [x] Canvas pan is clamped to 20,000 x 20,000 bounds and layer count is capped at 50 with snackbars.
- [x] Item 11: Text editor toolbar operations operate as tested pure functions with correct caret placement, Enter list continuation, single-step undo/redo, real interactive checkboxes, and clean markdown storage.
- [x] Item 12: Long-press on Canvas note opens the full contextual action panel (Rename, Color, Deadline, Schedule, Move, Share/Export, Select, Delete).
- [x] Item 13: Elms Sans font (Light, Medium, Bold) applied app-wide with zero italics in UI.
- [x] Item 14: Folder header Add (+) and Kebab (⋮) buttons do not overlap and maintain 48dp touch targets.
- [x] Item 15: Canvas Direct Hardware-Accelerated Rendering & Black Tile Elimination (eliminate tile-missing dark grid holes during zoom/draw, remove RGB_565 pitch-black tile degradation, direct QuadTree-culled vector and image rendering with solid-fill single point dots).

