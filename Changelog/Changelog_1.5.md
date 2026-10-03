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
- Text Note Editor Toolbar & Caret Precision: Completely overhauled editor toolbar with pure transformation operations, eliminating caret placement inversion after prefixes, adding automatic list continuation and blank-line exit on Enter, single-stroke backspace prefix deletion, live preview styling with 1:1 active line caret stability, and interactive checkbox toggling.
- Canvas Note Contextual Actions: Long-pressing a drawing note now opens a full quick-action panel offering Rename, Color Label, Deadline, Schedule Reminder, Move to Folder, Share/Export as PNG, Batch Selection, and Delete to Trash.
- Elms Sans Typography Migration: Migrated the entire application typography to Elms Sans with strictly three weights (Light 300, Medium 500, Bold 700) mapped by context, and completely removed italics across all UI titles, folder cards, search bars, and dialogs.
- Folder Screen Header Spacing: Resolved visual and touch target collision between the folder header Add (+) and Kebab (⋮) buttons, establishing clean 10dp separation and dedicated 48dp touch targets.
- Complete Home Screen Redesign: Modernized home dashboard with near-black background (`#0A0D14`), 1:1 dark folder cards (`#111726`, 24dp radius), muted accent icon tiles, facing LinkIt corner stroke glows, segmented tab bar (All, Favorit, Arsip), non-overlapping bottom search dock with '+' action popup, and a floating bottom navigation bar (Home, Notes, Settings).

### 1.5.3 - 2026-10-03
- Section-Oriented Settings Screen: Reorganized settings into 6 main section rounded cards on root (General, Appearance, OCR & Recognition, Notifications & Deadlines, Storage & Data Management, About & Legal) with clean, un-carded detail lists inside each opened category.
- Full-Height Home Viewport & Navigation Clearance: Eliminated artificial vertical constraints on the home dashboard, allowing folder grid content to scroll smoothly using the full screen height while staying completely unobstructed by system navigation buttons.
- Photo Viewer Responsive Header: Reorganized inspector toolbar into a clean title and action row paired with a full-width horizontal status row, ensuring helper text and schedule badges never wrap vertically on narrow phone widths.
- Search Result Auto-Scroll & Exposure Highlight: Tapping any search result (Photos, Groups, PDF documents, Word DOCX, Text Notes, Canvas) navigates directly to the parent folder and active subfolder, auto-scrolls until the item is visible, and applies a subtle 2-second brightness/exposure flash before fading out cleanly.

### 1.5.4 - 2026-10-03
- Direct Hardware-Accelerated Canvas Rendering: Switched canvas rendering to direct hardware-accelerated vector and image drawing, completely eliminating missing tile cutouts and dark grid boxes during pinch-to-zoom and stroke drawing.
- Black Box & Dot Stippling Fix: Fixed solid black rectangular boxes appearing when drawing dots or rapid strokes by removing RGB_565 degradation and rendering single-point strokes with solid fill.
- Multi-Zoom Vector Fidelity: Vector strokes and imported whiteboard/scan images now maintain crisp sub-pixel precision across extreme zoom ranges (up to 500%) with zero tile seams or blurriness.
