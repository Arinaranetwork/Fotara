# Phase 6 - DocumentNotesAndConnectedFeatures

## Goal
Expand Fotara to support native PDF and DOCX academic documents as first-class `DocumentNote` entities alongside photos and groups, modernize the home-screen widget with Jetpack Glance, implement batch rename across all selection scopes, establish a unified Share As engine (Original, PDF, Word), provide determinate combine UX with cancellation cleanup, and introduce connected lifecycle services (GitHub release updates, Supabase suggestion/diagnostics, QRIS support) while keeping core organizing, OCR, and search fully offline.

## Scope
- Creation of `DocumentNote` and `DocumentPage` entities outside the `PhotoGroup` hierarchy.
- On-device sequential `PdfRenderer` page rendering and ML Kit OCR for PDF imports.
- Native zipped XML text extraction from `word/document.xml` for DOCX imports.
- Continuous vertical scroll viewer for multi-page PDF notes; external hand-off for DOCX.
- DocumentNote contextual menu: Rename (syncing filename), Share As, Split to Images (PDF irreversible conversion), Delete to Trash, Move, Color Label, Set Deadline, Multi-Select.
- Modernized Jetpack Glance "Due tomorrow" widget with resizability, deep-linking, direct action callback, and 30-minute default refresh with pre-Android 15 fallback.
- Multi-select batch rename across mixed items (`Photo`, `PhotoGroup`, `DocumentNote`) and home-screen folders with sequential indexing.
- Full multi-select and action parity inside the fullscreen Group Screen grid.
- Unified Share As picker (Original, combined PDF, compiled Word `.docx`) with pre-flight 100-page cap check.
- Determinate combine operation UX with animated progress indicator and cancelation residual file cleanup.
- Dynamic GitHub release updates with three-dot state menu ("No Update", "Update Available", "Updating..."), fullscreen update/notes view, background download notification, and sideload explainer dialog.
- Suggestion and bug report form submitting to Supabase with install UUID rate-limiting and optional diagnostic info.
- Support screen displaying QRIS image with download action.
- "What's New" release notes modal auto-displayed after update and accessible via Settings with offline fallback.
- Settings toggles for auto-check updates and opt-in local crash log capture.

## Out Of Scope
- Cloud synchronization or remote storage of user photos/documents.
- Collaborative multi-user folder sharing.
- Rendering formatted DOCX layouts inside the app (external app hand-off utilized).
- Selective folder widget configuration (locked to "Due tomorrow" notes).
- Preserving editable vector text in flattened PDF/Word combine operations.

## Features
### 1. DocumentNote Entity & Import Engine
- Dedicated `document_notes` and `document_pages` SQLite tables.
- PDF: native Android `PdfRenderer` memory-safe sequential page rendering and on-device text recognition.
- DOCX: zipped XML extraction indexing raw document text directly into SQLite FTS4 without OCR.
- Vertical scroll viewer screen for PDF notes; external handoff intent for DOCX.
- Context actions: Rename (file sync), Share As, Split to Images with one-time irreversible warning dialog, Delete to Trash, Move, Color label, Set deadline, Multi-select.

### 2. Jetpack Glance Widget Rebuild
- Resizable Glance widget dynamically reflowing "Due tomorrow" notes.
- Direct interactive tap action on widget surface without opening app.
- Deep linking on note tap directly into note viewer.
- Periodic 30-minute background sync plus reactive data-change triggers.
- Static fallback for Android versions prior to Android 15.

### 3. Batch Rename & Interaction Parity
- Multi-select Rename active for 1+ items: single rename for 1 item, batch rename with sequential numbering ("Base 1", "Base 2") for 2+ items.
- Home screen folder multi-select gains batch rename.
- Full parity in Group Screen member grid (multi-select, delete with auto-dissolve, batch rename, Share As).

### 4. Unified Share As & Combine Engine
- Unified 3-option picker: Original, PDF, Word across single and multi-select contexts.
- Word `.docx` generator compiling photos, PDF pages, and DOCX extracted text into a clean Word document.
- Pre-flight cap check blocking operations exceeding 100 total pages.
- Determinate animated progress dialog and cancelation cleanup deleting partial output files.

### 5. Connected Services
- Dynamic update checking against GitHub Releases with tri-state menu item, fullscreen changelog preview, download progress notification, and installation guide.
- Supabase suggestion form with anonymous UUID rate-limiting (default 5/day) and optional diagnostic metadata.
- QRIS support screen in overflow menu and Settings.
- Post-update What's New screen with cached offline fallback.
- Opt-in local crash log recording and auto-check update setting toggles.

## UI Mockup
```
+-------------------------------------------------------+
|  Biology                              [=] [Select]    |
|  All  Lectures (3)  Assignments (2)                   |
+-------------------------------------------------------+
| [Photo Note]      [Group Stack (4)]    [PDF Doc Note] |
| Cell Diagram      Mitosis Series       Lab Guide.pdf  |
|                                        [PDF 12 pages] |
|                                                       |
| [DOCX Note]       [Photo Note]         [Photo Note]   |
| Syllabus.docx     Enzyme Curve         Membrane Model |
| [DOCX text]                                           |
+-------------------------------------------------------+
| Floating Action Bar (3 selected):                     |
| [X Cancel]   [Rename (Batch)]   [Move]   [Share As]   |
+-------------------------------------------------------+
```

## Logic Notes
- `DocumentNote` never participates in `PhotoGroup` grouping or merging.
- PDF page splitting deletes `origin_file_uri` irreversibly and generates unlinked `Photo` records.
- Batch rename renames the physical disk file for `DocumentNote` to match displayed title.
- Total combine pages = standalone photos + group members + PDF document pages + 1 per DOCX document. Cap = 100.
- Glance widget re-registration reverses v1.2 disabled state.

## Risks
- Memory pressure during large PDF page rendering -> Mitigated by strict sequential one-page-at-a-time `PdfRenderer` lifecycle.
- Partial corrupt files on combine cancel -> Mitigated by transactional cleanup deleting target file in finally block.
- Rate limit abuse on suggestions -> Mitigated by local 15-second cooldown and server-side UUID cap.

## Dependencies
- Android `PdfRenderer` API
- Jetpack Glance (`androidx.glance:glance-appwidget`)
- ML Kit Text Recognition
- Apache POI / light open-xml parsing for DOCX generation

## Acceptance Criteria
- [ ] Importing PDF creates `DocumentNote` and ordered `DocumentPage` records with OCR text.
- [ ] Importing DOCX extracts raw XML text into `extracted_text` and indexes it in FTS4.
- [ ] PDF notes open in continuous vertical scroll screen; DOCX notes open via external intent.
- [ ] "Split to Images" converts PDF pages to standalone photos and permanently deletes original PDF after confirmation dialog.
- [ ] Multi-select Rename supports 1 item (single rename) and 2+ items (sequential numbering), renaming underlying files for document notes.
- [ ] Home folder multi-select includes Rename action.
- [ ] Group screen grid supports multi-select, delete, batch rename, and Share As.
- [ ] Share As provides Original, PDF, and Word options across single and mixed selections.
- [ ] Heavy combine flows display determinate progress, support cancellation, clean up partial files, and reject selections > 100 pages.
- [ ] Jetpack Glance widget displays "Due tomorrow" notes, reflows cleanly, deep-links on tap, and offers direct action callback.
- [ ] GitHub release update flow accurately reflects state in overflow menu, displays fullscreen details, tracks download progress, and opens package installer.
- [ ] Suggestion page submits to Supabase with rate-limiting, and Support page renders QRIS with download button.
