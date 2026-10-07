# Phase 36 - WorkspaceIconsPdfPageMenuAndDrawingEditor
## Goal
Finalize 1.8.0 Beta by delivering workspace icons, PDF page menu with pins and Save to Gallery, saved-image location setting, deep-linked search navigation with transient highlights for PDFs and text notes, and the dedicated PDF page drawing editor with seamless viewer integration.

## Scope
- Database: bump `DATABASE_VERSION` from 17 to 18 with explicit migrations (`icon_key` on `workspaces`, `pdf_page_pins`, `pdf_page_drawings`), foreign key cascade and orphan cleanups.
- Workspace icons: 24 bundled icons, `WorkspaceIcons` registry, add/edit workspace dialogs with 6-column grid, custom tabs on Home and Notes showing icons via extended display function.
- PDF page menu: three-dot button on each page card in `PdfViewerScreen`, Save to gallery, Pin/Unpin (max 3 pins with top chip row and scroll-to), Draw on page, Hide/Show drawing, Clear drawing.
- Save to gallery: 300 dpi render (max 4096 px), MediaStore (Android 10+) and SAF (Android 7-9), optional stroke flattening with Multiply blending, saved image location setting in Settings.
- Search deep-linking: PDF hits open `PdfViewerScreen` directly at first matching page (`targetPageIndex`), transient ML Kit OCR element highlights; text note hits open at matching range with fading highlight.
- PDF page drawing data layer: extraction of coordinate-space-agnostic drawing engine shared with photo drawing, storage in PDF page points, `PdfPageDrawingRepository`.
- PDF page editor: dedicated `PdfPageEditorNavKey` screen, top bar, floating tool dock, vector stroke rendering, smooth 1x-6x GPU pinch/pan, autosave debounce, viewer integration.
- Release finalize: What's New entry, "1.8.0 Beta" version bump, complete unit tests pass, debug/release build artifacts.

## Out Of Scope
- Modifying FTS table definitions or database schema beyond v18.
- Editing `ViewportTransform.kt` or `fotara.fileprovider`.
- Introducing new network requests or external dependencies.
- Modifying share, PDF folder export, or combine paths to include drawings.

## Features
### Workspace Icons
Registry of 24 icons with default "folder" fallback. Workspace dialogs show 6-column grid with 44dp cells. Custom tabs, search chips, and destination rows display icon via extended `Workspace.getDisplayInfo()`.

### PDF Page Menu & Pins
Small 32dp circular button (44dp target) at top-right of page cards (hidden while zoomed). Menu options for gallery saving, pinning, and drawing. Pins capped at 3 per document, rendered as chip row below top bar with animated scroll-to.

### Save to Gallery & Location Setting
Renders page at 300 dpi with white background and visible drawings flattened. Saved image location setting in Settings ("Pictures/Fotara", "DCIM/Fotara", or custom folder).

### Search Jump & Highlight
PDF search results navigate directly into `PdfViewerScreen` at the matching page. Transient OCR locates word boxes for translucent accent highlights. Text notes open with match range highlight fading over 4 seconds.

### PDF Page Drawing Engine & Editor
Shared drawing engine with photo drawing mode. Coordinates in floating-point PDF points. Dedicated editor screen with Pen, Highlighter (Multiply blend), Eraser, 8-color palette, 100-step undo, 600ms autosave.

## Logic Notes
- Single SQLite migration in `FotaraDbHelper.onUpgrade` (version 17 to 18).
- Backward-compatible JSON backup carrying `icon_key`.
- Translucent search highlight rectangles drawn in Compose draw phase; never persisted.
- Smooth gesture tracking with pointer IDs to eliminate centroid jumps.

## Risks
- Matrix mismatch between PDF render points and screen pixels -> Normalized scale factor derived from `page_width` / `page_height` points.
- Drawing editor memory consumption on high-res pages -> Visible viewport sub-sampling and bitmap budget enforcement matching viewer.

## Dependencies
- Phase 35 (Batch 1 legal consent, dialog coordinator, device registry).

## Acceptance Criteria
- Full unit test suite passes 100%.
- Workspaces persist and display custom icons.
- PDF page menu permits up to 3 pins and gallery export.
- Search navigates directly to target PDF page with element highlights.
- PDF page drawing editor opens, records vector strokes, autosaves, and reflects in viewer.
- Version string parses to "1.8.0 Beta" with channel BETA.
