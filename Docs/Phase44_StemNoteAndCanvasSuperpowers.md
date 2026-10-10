// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

# Phase 44 - StemNoteAndCanvasSuperpowers

## Goal
Equip students in STEM fields (engineering, mathematics, natural sciences) with powerful technical note-taking capabilities directly inside Fotara. This includes native LaTeX mathematical typesetting and structured Markdown tables with syntax-preserving constraints in Text Notes, alongside intelligent drawing superpowers: 400ms shape and curve auto-correction with Bézier smoothing, vector Text Layers (`[ T ]`) in Canvas and PDF Page editors, and multi-sheet canvas notebooks (up to 10 sheets) with combined multi-page PDF export.

## Scope
1. **LaTeX Mathematical Typesetting**:
   - Inline equations `$ ... $` and display block equations `$$ ... $$` support in Text Notes.
   - Real-time syntax detection and visual transformation rendering formulas with mathematical symbols (Greek letters, operators, fractions, integrals, superscripts, subscripts).
   - Dedicated `[ fx ]` toolbar action for 1-tap template insertion and selection wrapping.
2. **Markdown Table Editor with Formatting Constraints**:
   - Insert and edit Markdown tables in Text Notes with 3x3 default grids.
   - Contextual table operations (`+ Row`, `+ Col`, `- Row`, `- Col`, formatting).
   - Strict formatting constraint: inside table cells, only inline formatting (Bold, Italic, Strikethrough, Code, Math) is active; all block-level tools (H1-H3, Blockquotes, Lists, Checklists, Dividers) are strictly disabled with `alpha = 0.38f` to prevent syntax corruption.
   - Table rendering in `RichMarkdownText` composable.
3. **Shape & Curve Auto-Correct (Draw & Hold 400ms)**:
   - Pen tool gesture: holding touch for 400ms without lifting triggers auto-correction into geometric primitives (Rectangles, Triangles, Circles, Ellipses, Straight Lines, Arrows).
   - Subtle haptic feedback fires upon snap.
   - Dragging while still holding resizes/reorients the snapped shape before committing.
   - Cubic Bézier smoothing for organic handwriting strokes to eliminate jaggedness.
4. **Text Layers Tool (`[ T ]`)**:
   - New `[ T ]` tool in Drawing Canvas and PDF Page Editor tool docks.
   - Tapping places a movable, resizable, rotatable vector text annotation.
   - Formatting bar with font size slider (12-48sp), `ElmsSans` typography (Regular, Medium, Bold), 8-color palette, and card styles (Transparent, Frosted Dark `#1E293B`, Solid Light).
   - Lossless vector representation in `CanvasElement.TextLayer` and drawing codecs.
5. **Multi-Sheet Drawing Canvas (Max 10 Sheets)**:
   - Sheet Pagination Bar directly above the bottom tool dock: `[ Sheet 1 ] [ Sheet 2 ] ... [ + ]`.
   - Capped at 10 sheets per Canvas Note with user notification upon limit.
   - Independent Undo/Redo history stacks per sheet.
   - Combined multi-page export to PDF document.

## Out Of Scope
- Dynamic `.fpkg` package downloading (handled in Phase 49).
- WebRTC peer collaborative room synchronization (handled in Phase 51).
- Cloud syncing or external server rendering.

## Features

### LaTeX Mathematical Typesetting
- **Inline Formulas**: `$E = mc^2$` or `$\vec{F} = m\vec{a}$`.
- **Display Block Formulas**:
  ```latex
  $$
  \int_{0}^{\infty} \frac{\sin x}{x} dx = \frac{\pi}{2}
  $$
  ```
- **Live Preview & Interaction**: When the caret is within math markers, raw LaTeX syntax is shown with dimmed delimiters for editing. When cursor moves outside, formula renders with mathematical typography and symbols.
- **Toolbar Action**: Tapping `[ fx ]` inserts `$formula$` (or `$$...$$` if on empty line) or wraps active selection.

### Markdown Table Editor & Strict Constraints
- **Table Insertion**: Tapping `[ ▦ Table ]` inserts a 3x3 table template.
- **Strict Formatting Guard**:
  - `state.isInsideTable == true` when selection touches a markdown table line.
  - Active: Bold, Italic, Strikethrough, Inline Code, Math (`[ fx ]`).
  - Disabled (`alpha = 0.38f`, non-clickable): H1, H2, H3, Blockquote, Bullet List, Numbered List, Checklist, Indent, Outdent, Horizontal Rule, Code Block.
  - Contextual action bar: `[ + Row ] [ - Row ] [ + Col ] [ - Col ]`.

### Shape & Curve Auto-Correct & Stroke Smoothing
- **Detection Algorithm**:
  - Closed strokes: evaluated for circularity (Circle/Ellipse) and polygon corners (Triangle, Rectangle).
  - Open strokes: evaluated for collinearity (Straight Line, Arrow).
- **Hold Trigger**: 400ms timer with stationary position (<36dp tolerance for finger jitter). On trigger, fires haptic pulse and replaces path with geometric primitive.
- **Dynamic Resizing**: Moving finger after snap dynamically updates primitive bounds.
- **Automatic Bézier Smoothing & Finger-Lift Recognition**: All finished Pen strokes automatically receive cubic Bézier curve smoothing and geometric primitive snapping upon finger lift to eliminate jagged handwriting.

### Text Layers Tool (`[ T ]`)
- **Dock Item**: `[ T ]` icon in drawing dock.
- **Interactive Manipulation**: Tap hit-testing selects existing text layers, rendering dashed selection bounding box and corner drag handles; live dragging updates coordinates in real time, persisted to database on finger lift.
- **Canvas Tool Controller Integration**: Full support for text layers under Select tool including body dragging (`handleId == -1`), corner dimension scaling, and stem rotation.
- **Editing Overlay**: Floating format bar with font size slider (12-48sp), color picker, and style toggles.
- **Vector Serialization**: Stored in `CanvasElement.TextLayer` with bounds, text, font size, color, rotation, and style.

### Multi-Sheet Drawing Canvas
- **Navigation Bar**: Tab row `[ Sheet 1 ] [ Sheet 2 ] ... [ + ]` above dock.
- **Sheet Capacity**: Max 10 sheets. `+` button disabled when count == 10.
- **History Isolation**: Each sheet maintains its own `CanvasHistoryManager` undo/redo stack.
- **Multi-Page Export**: PDF exporter loops through all document sheets to generate a multi-page PDF.

## UI Mockup
```
┌────────────────────────────────────────────────────────┐
│ Text Note: Calculus II                           [ ✓ ] │
├────────────────────────────────────────────────────────┤
│ Newton-Leibniz Formula:                                │
│   ∫[a to b] f(x) dx = F(b) - F(a)                      │  <-- Math Block
│                                                        │
│ Summary Table:                                         │
│ ┌──────────────┬──────────────┬──────────────┐         │
│ │ Function     │ Derivative   │ Integral     │         │
│ ├──────────────┼──────────────┼──────────────┤         │
│ │ sin(x)       │ cos(x)       │ -cos(x)      │         │
│ └──────────────┴──────────────┴──────────────┘         │
├────────────────────────────────────────────────────────┤
│ [ + Row ] [ - Row ] [ + Col ] [ - Col ]                │  <-- Table Bar
├────────────────────────────────────────────────────────┤
│ [ ↩ ] [ ↪ ] [ B ] [ I ] [ S ] [ ` ] [ fx ] [ ▦ Table ] │  <-- Toolbar
└────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────┐
│ Canvas Note: Physics Diagram                     [ ⠇ ] │
├────────────────────────────────────────────────────────┤
│                                                        │
│           ┌─────────────┐   F = 10N                    │
│           │   m = 2kg   │ ────────>                    │  <-- Auto-correct
│           └─────────────┘                              │
│             "Free Body"                                │  <-- Text Layer
│                                                        │
├────────────────────────────────────────────────────────┤
│ [ Sheet 1 ]  [ Sheet 2 ]  [ + ]                        │  <-- Multi-sheet
├────────────────────────────────────────────────────────┤
│ [ Pen ]  [ High ]  [ Eraser ]  [ T ]  [ Lasso ]        │  <-- Dock
└────────────────────────────────────────────────────────┘
```

## Logic Notes
- Math expressions parse tokens `\alpha..\omega`, `\le..\ge`, `\int`, `\sum`, `\frac{a}{b}`, `x^2`, `x_i`, etc.
- Markdown table cell boundary checking determines line pipe occurrences and active cell index.
- Auto-correct uses minimum error metric for circle/ellipse, cross-product angle changes for polygon corners.
- `CanvasDocument` extended to support multiple sheets: `CanvasSheet(id, title, layers, elements)`.

## Risks
- *Risk*: Math parsing disrupts existing markdown links or bold styling.
  - *Mitigation*: Math regex parses distinct `$ ... $` spans without matching escaped `\$` or crossing markdown boundary delimiters.
- *Risk*: Drawing hold 400ms triggers accidentally while writing slow text.
  - *Mitigation*: Require stylus/touch velocity to be strictly below stationary jitter threshold (<3dp) for the entire 400ms duration.

## Dependencies
- `TextNoteEditorScreen.kt`, `EditorToolbar.kt`, `MarkdownVisualTransformation.kt`, `TextEditorOps.kt`.
- `CanvasScreen.kt`, `CanvasViewModel.kt`, `DrawingEngine.kt`, `PdfPageEditorScreen.kt`.

## Acceptance Criteria
- `$ ... $` and `$$ ... $$` render mathematical symbols and formulas in Text Notes.
- Tapping `[ fx ]` inserts math skeleton or wraps selection.
- `[ ▦ Table ]` inserts 3x3 table; when inside table, all block tools disabled (`alpha = 0.38f`) while inline tools stay enabled.
- Drawing rough circle or rectangle and holding for 400ms snaps into geometric primitive with haptic pulse.
- `[ T ]` tool in Drawing Canvas and PDF editor allows placing and editing vector text layers.
- Multi-sheet canvas supports creating up to 10 sheets, switching between them with isolated undo/redo, and exporting as combined multi-page PDF.
- 100% unit tests pass across math parsing, table ops, shape auto-correct, and multi-sheet logic.
