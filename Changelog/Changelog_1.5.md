![Fotara 1.5 Banner](../Assets/Banners/FotaraBanner_1.5_2026-09-30.jpg)

# Fotara 1.5 - Complete Study Note Ecosystem & Unlimited Canvas
Released: 2026-09-30   Status: Beta

## What's New
- Unlimited Drawing Canvas (Alpha): Introducing an infinite 2D vector drawing canvas note type with freeform pan, pinch-to-zoom, pressure-sensitive pen, highlighter, eraser, multi-layer management, image attachments, and high-resolution PNG export.
- Universal Note Scheduling: Attach custom date and time reminder schedules to any note type (Photos, Groups, PDFs, Word DOCX, Text Notes, and Canvas) with user choice between standard notifications and ringing alarm alerts.
- Dedicated "Today" Home Widget: A new rectangular Jetpack Glance widget displaying assignments due today, newly captured study notes, and upcoming scheduled alerts with instant deep-linking.
- Share to Fotara: Seamlessly share images, PDFs, Word documents, Markdown, and plain text directly from other Android apps into Fotara with an intuitive multi-item destination placement screen.
- Search Date Filtering: Filter notes instantly by date added with quick chips (Today, Yesterday, This week, This month, This year) and custom single-day or date-range pickers.
- Rich Text Formatting Toolbar: Upgraded native text editor toolbar with full support for bold, italic, bold-italic, strikethrough, headings, inline code, link creation, dividers, and interactive checklists.
- In-Viewer PDF Zoom: Smooth pinch-to-zoom and two-axis panning directly on PDF pages inside the native viewer with sharp on-demand viewport rasterization.

## Changed
- Global English Standardization: Standardized all application text, dialogs, error messages, settings, notifications, widgets, and release notes exclusively in English.
- Redesigned LinkIt Corner Glow: Replaced the loud amber pill badge with a subtle, elegant radial corner glow that remains crisp and uniform from Android 7.0 (API 24) to Android 16 (API 36).
- Intelligent PDF Split to Images: PDF splitting now generates high-fidelity white-canvas images without dark artifacts, automatically grouping documents with 5 or more pages into a Photo Group while preserving page order.
- Tightened Feedback Limits: Enforced a 5-submission rolling 24-hour quota and a 60-second cooldown timer on feedback and bug reports to ensure reliable service delivery.

## Fixed
- Fixed intermittent UI freezing and frame stutter when scrolling through 30+ page PDF documents.
- Fixed HTTP 400 Bad Request errors when submitting feedback and bug reports to the Supabase backend.
- Fixed non-functional bold, italic, strikethrough, heading, and quote buttons in the native text note editor.
- Fixed transparent page rasterization artifacts in PDF rendering and Split to Images export.
- Removed legacy "Sharp PDF Note" label across all screen subtitles and headers.

## Patches
### 1.5.1 - 2026-10-01
- Photo-Style PDF Page Viewer: New Page View button in the PDF header opens a dedicated fullscreen page viewer with pinch-to-zoom up to 4.0x, free two-axis pan, double-tap zoom, zoom-gated horizontal swipe, and a collapsible Recognized OCR Text bottom sheet with instant copy.
- Viewport-Level List Zoom: Rebuilt PDF list viewer zoom to transform the entire document viewport with natural two-axis pan, double-tap to zoom or reset, and live amber status indicator.
- Resolved slim and distorted PDF page rendering at 1.0x by eliminating the default aspect ratio race condition and computing exact target dimensions with unified layout math.
- Fixed boxed in-card zoom constraints so zooming in the PDF list no longer feels boxed into a single page.
- Fixed high-density bitmap allocation during pinch gestures with memory-bounded caching and automatic resource reclamation.

### 1.5.2 - 2026-10-02
- Canvas Debounced Autosave: Automated 500ms background saves with single-writer mutex queue, atomic writes, synchronous flush on backgrounding, and an interactive save status pill (Saving, Saved, Retry).
- Precision 1:1 Canvas Pan & Zoom: 1:1 finger tracking across any zoom level without lag or jumpiness, coordinate clamping to 20,000 x 20,000 extents, and viewport-culled rendering without duplicate stroke passes.
- Safe Image Attachment: Streamlined ContentResolver imports via single-pass staging, automatic downsampling (<= 2048px), EXIF orientation correction, and insertion as a new dedicated layer centered in the active viewport with undo/redo support.
- Insert From Existing Notes: In-canvas visual image picker allowing insertion of snapshots directly from existing photo and scan notes into new canvas layers.
- Layer-Aware Stroke Eraser: The broom tool now erases vector strokes strictly within the currently active canvas layer without affecting other layers.
- Searchable OCR & DOCX Content: Added SQLite FTS4 virtual table indexing for Word DOCX and PDF documents with multi-word prefix search, relevance ranking (titles before content), and context-centered `[OCR]` / `[DOCX]` result snippets.
- Responsive Canvas Navigation: Single-line adaptive top bar eliminating collisions on compact screens or large fonts, with the floating zoom pill smoothly adjusting above the dock.
- Dynamic Facing Folder Glow: Glowing folder cards on the home screen dynamically face adjacent glowing neighbors along cardinal edges and diagonal corners.
- Drawing Mode Guardrails: Centralized canvas safety limits preventing out-of-bounds panning, memory overruns, and layer explosion (up to 50 layers).
