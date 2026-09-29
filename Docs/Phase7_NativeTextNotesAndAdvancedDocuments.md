<!-- --- Arinara Network (c) 2026 --- -->
<!-- Exclusive property of Arinara Network. -->
<!-- Unauthorized use, reproduction, distribution, or modification of this code, -->
<!-- in whole or in part, for any purpose, is strictly prohibited without prior -->
<!-- written consent from Arinara Network as sole legal owner of this codebase. -->

# Phase 7 - NativeTextNotesAndAdvancedDocuments

## Goal
Eliminate all PDF rendering defects (black backgrounds, scrolling lag, low resolution), deliver an in-app reflowed DOCX reader, introduce native rich-text markdown notes as a first-class grid item alongside photos, groups, and documents, and perform a full application audit and quality hardening pass across Fotara v1.4.0.

## Scope
- Complete app audit and quality pass: Performance, Memory, Concurrency, Data Integrity, Error Handling, Placeholder audit, UX Consistency, Accessibility, and Build Health.
- PDF Defect Repair: White background pre-fill, viewport-scaled crisp rendering from original file, virtualized vertical list with bounded LRU memory cache, thread-safe single-writer serialized `PdfRenderer` access, pipelined import with real progress and background OCR, and password-protected PDF error handling.
- In-App DOCX Viewer: Reflowable XML structure parsing (paragraphs, bold/italic/underline/strikethrough runs, headings, lists, tables, embedded images, unsupported element indicators), progressive loading with progress indicator, and "Open with" fallback.
- Native Text Notes: New `TextNote` data model and SQLite table (`text_notes`), markdown source of truth, rich formatting toolbar (bold, italic, strikethrough, headings 1–3, blockquote, bulleted/numbered/check lists, code, HR, link, undo/redo), debounced autosave, grid cell preview, multi-select/batch rename/trash integration, plain-text and markdown export only, and conflict resolution in mixed selections.

## Out Of Scope
- Collaborative cloud editing of text notes (offline-first single-user only).
- WYSIWYG pagination-identical Word reproduction (DOCX is reflowable on mobile).
- Vector annotation drawing tools on PDF pages.

## Features
### Full Application Audit & Quality Hardening
- Audit across 9 distinct axes with measured latency metrics.
- Fix all critical and major defects identified before shipping v1.4.0.
- Ensure strict zero-placeholder discipline and zero unhandled exceptions.

### PDF Defect Repair & High-Performance Viewer
- Destination bitmap explicitly pre-filled with `Color.WHITE` prior to `page.render()`, eliminating all black backgrounds and transparency artifacts.
- Viewport density-scaled rendering directly from `origin_file_uri` for razor-sharp text.
- Virtualized `LazyColumn` with bounded LRU bitmap cache and cancellation of off-screen page render jobs.
- Single-writer coroutine mutex serializing `PdfRenderer` page access to prevent native memory corruption.
- Pipelined PDF import: Page 1 viewable in <100ms; remaining pages and OCR processed asynchronously in background with determinate progress bar and clean cancellation.
- Explicit password-protected PDF dialog handling (`SecurityException`).

### In-App Reflowed DOCX Viewer
- Native XML parser extracting paragraphs, text formatting runs, headings, tables, bullet/numbered lists, and embedded images directly from `word/document.xml` and relationships.
- In-app scrollable Compose viewer rendering reflowed content cleanly on midnight theme.
- Visual placeholder indicator for unsupported complex drawing objects and equations, ensuring no silent content dropping.
- Secondary "Open with external app" action preserved.

### Native Rich-Text Notes
- New first-class entity `TextNote` stored in SQLite with full FTS4 search indexing on `title` and plain-text stripped markdown body.
- Rich-text editor with instant Markdown round-tripping, debounced autosave (300ms), and bottom toolbar anchored above software keyboard insets.
- Grid cell displaying title, formatted body preview snippet, and distinct note icon.
- Full parity with Move, Delete (to Trash), Tag Color, Deadline, Batch Select, Batch Rename, and LinkIt.
- Dedicated Share As restricted to Markdown (`.md`) and Plain Text (`.txt`). Mixed multi-item selections skip text notes for PDF/Word combine with transparent user notification.

## UI Mockup
```
+-------------------------------------------------------------+
| [<] Biology Lecture 04 — Cell Structure       (B) (I) (S)   |
+-------------------------------------------------------------+
| Title: [ Mitosis Stages & Regulation                      ] |
| ----------------------------------------------------------- |
| ## Overview                                                 |
| Mitosis is divided into **four** primary stages:            |
| - Prophase: Chromatin condenses into chromosomes            |
| - Metaphase: Chromosomes align at equatorial plate          |
| - Anaphase: Sister chromatids separate                      |
| - Telophase: Nuclear envelope reforms                       |
|                                                             |
| > *Important for Exam 2: Remember checkpoint triggers*     |
+-------------------------------------------------------------+
| [B] [I] [S] [H1] [H2] [H3] [ " ] [ • ] [ 1. ] [ v ] [ < ] [ >]
+-------------------------------------------------------------+
```

## Logic Notes
- `TextNote` autosave uses a debounce channel (300ms) with `StateFlow`.
- SQLite database upgraded from version 9 to 10 with forward migration in `FotaraDbHelper`.
- FTS4 updated to index text notes (`photos_fts` or dedicated `notes_fts`).
- PDF rendering mutex ensures `PdfRenderer.openPage` is strictly serialized.

## Risks
- Memory pressure during high-resolution PDF rendering on low-RAM devices -> Mitigated by capping maximum rendered bitmap dimension to `2048px` and enforcing a 32MB LRU bitmap cache with immediate recycling.
- Large DOCX files causing UI stutter during XML parsing -> Mitigated by executing XML parsing entirely on `Dispatchers.IO` and streaming paragraphs progressively.

## Dependencies
- Phase 6 (Document notes architecture).

## Acceptance Criteria
- [x] Audit report presented with severity, location, and remedy for all 9 areas.
- [x] Measured before/after metrics verified for startup, PDF open, scroll frames, and import.
- [x] PDF rendering has zero black backgrounds and renders sharp text at device density.
- [x] PDF import features a determinate progress bar and can be cancelled cleanly with partial file purge.
- [x] In-app DOCX viewer displays reflowed text, headings, lists, tables, and images without external apps.
- [x] Text note creation, formatting toolbar, autosave, grid display, search indexing, and MD/TXT sharing fully operational.
