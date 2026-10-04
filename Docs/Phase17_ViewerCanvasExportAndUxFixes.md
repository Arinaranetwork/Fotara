<!-- --- Arinara Network (c) 2026 --- -->
<!-- Exclusive property of Arinara Network. -->
<!-- Unauthorized use, reproduction, distribution, or modification of this code, -->
<!-- in whole or in part, for any purpose, is strictly prohibited without prior -->
<!-- written consent from Arinara Network as sole legal owner of this codebase. -->

# Phase 17 - Viewer, Canvas, Export & UX Fixes

## Goal
Resolve 9 targeted defects and behavioral gaps across PDF rendering, cross-screen navigation, multi-select export, photo manipulation safety, canvas stroke rendering, layer workflow, and dialog autofocus for Fotara 1.5.5.

## Scope
- Task 1: Sharp high-zoom rendering up to 4.0x in PDF viewer with memory-safe pixel budget, off-main-thread execution, and high-res cache eviction upon zoom reset.
- Task 2: Direct PDF viewer opening from Notes tab (page 1) and Search results with automatic jumping to the first matching OCR page (handling all-terms, any-term, or fallback).
- Task 3: Combine share (PDF and Word DOCX) inclusion for Text Notes (formatted markdown) and Canvas Notes (1x rasterized full canvas), skipping empty canvases.
- Task 4: Rotate and crop safe atomic file writes (temporary file in same dir + atomic replace), consistent OCR re-run, and FTS refresh.
- Task 5: Standardization of three-dot overflow menu header to small muted "Info" across Photo Viewer and PDF Viewer.
- Task 6: Canvas stroke smoothing via unified quadratic Bezier path builder across in-progress and committed strokes, zoom-aware decimation tolerance, and round highlighter caps/joins.
- Task 7: Canvas layers streamlined creation (instant default "Layer N" naming without dialog) and inline name editing via long-press.
- Task 8: New folder dialog immediate soft keyboard display and autofocus via FocusRequester.
- Task 9: Removal of camera option from Home screen (+) dock button, directly triggering New Folder dialog.

## Out Of Scope
- Text note editor core modifications (governed by Phase 16).
- Database schema changes (all tables, schemas, and FTS definitions remain strictly unchanged).
- Modifying ViewportTransform.kt or fotara.fileprovider.
- Adding arbitrary settings, preferences, or visual themes outside the specified list.

---

## Verified Current Behavior (Tasks 4 & 5)

### Task 4 Verification: Rotate & Crop OCR and File Handling
- **Previous Discrepancy**: Prior reviews disagreed on whether photo rotation re-ran OCR.
- **Actual Code Behavior**:
  - In `PhotoStorageManager.kt`, both `rotatePhotoClockwise` and `cropPhoto` directly overwrote the original file in-place using `FileOutputStream(photoFile)`. If decoding, memory allocation, or compression failed mid-stream, the original image was permanently corrupted or truncated to 0 bytes.
  - In `PhotoRepository.kt`, `rotatePhotoClockwise` and `cropPhoto` updated only `file_size_bytes` in SQLite without triggering OCR or FTS.
  - In `FolderDetailViewModel.kt` and `GroupDetailViewModel.kt`, both `rotatePhoto` and `cropPhoto` did invoke `ocrEngine?.extractText(updated.fileUri)` and `photoRepository.updatePhoto(finalPhoto)`. However, if the storage operation failed, no user error was displayed, and the file was left unprotected against corruption.
- **Resolution**:
  - Enforce atomic file replacement: write to a temporary file in the same directory (`.tmp_rotate_...`), sync file descriptor, and atomically replace the target file via `java.io.File.renameTo` (or `Files.move(..., ATOMIC_MOVE)`).
  - Regenerate thumbnail only after successful file replacement.
  - In both rotate and crop flows, always re-run OCR and update FTS index.
  - On any error, leave original file, thumbnail, and OCR text untouched and show user-friendly error string.

### Task 5 Verification: Viewer Menu "Info" Header
- **Code Behavior**:
  - `strings.xml` defines `<string name="viewer_info_header">Info</string>`.
  - Both `PhotoViewerDialog.kt` (line 395) and `PdfViewerScreen.kt` (line 293) reference `stringResource(R.string.viewer_info_header)`.
  - An earlier version displayed "VIEWER CONTROLS" prior to 1.5.5 standardization.
- **Resolution**: Verified both viewers use `R.string.viewer_info_header` without uppercase transformation, rendering "Info".

---

## Features

### 1. High-Zoom Sharp PDF Rendering
- Scale-aware rendering: pass `zoomState.scale` (clamped between 1.0f and 4.0f) to `VirtualizedPdfPageView`.
- Debounce pinch gestures by 150ms before triggering background high-res rasterization.
- Memory budget: `MAX_PIXEL_BUDGET = 4_000_000` pixels (~16MB ARGB_8888). If `(destWidth * scale) * (destHeight * scale) > MAX_PIXEL_BUDGET`, cap the render scale accordingly.
- Double-buffering: retain `baseBitmap` (or previous `highResBitmap`) until new high-res bitmap is fully decoded.
- Cache lifecycle: high-res entries in `PdfPageRenderer` include dimensions in key `${pageIndex}_${targetWidth}x${targetHeight}`. Upon zoom reset (`isZoomed == false`), high-res bitmaps are detached and evicted from LruCache.

### 2. Direct PDF Opening & Page-Aware Search Navigation
- From Notes tab: tapping a PDF note navigates directly into `PdfViewerScreen` at page index 0.
- From Search: query terms are normalized using `normalizeForSearch`. `PdfPageMatcher.findMatchingPageIndex` selects:
  1. First page whose OCR text contains all terms.
  2. First page whose OCR text contains any term.
  3. Default to page 0 if OCR missing or no terms match.
- Navigation state: `FolderDetailNavKey` carries `targetDocumentId`, `openViewerDirectly = true`, and `targetPageIndex`.
- Back navigation maintains hierarchy: Zoom reset -> Exit reading mode -> Return to folder grid.

### 3. Comprehensive Combine Share (Text & Canvas Notes)
- `CombineItem` expanded to:
  - `CombineItem.TextNoteItem(textNote: TextNote)`
  - `CombineItem.CanvasNoteItem(canvasNote: CanvasNote)`
- Text Note Rendering:
  - PDF: Formatted title heading + paginated markdown body (H1-H3, bullets `•`, checkboxes `☐`/`☑`, quotes `▎`, code blocks, bold/italic, links). Paginates dynamically when exceeding page height.
  - Word DOCX: Maps title to heading `<w:b/><w:sz w:val="28"/>` and lines to formatted paragraphs, bullets, and checkboxes.
- Canvas Note Rendering:
  - Full-canvas rasterization at 1.0x scaled to fit standard page dimensions.
  - Empty canvases (`elements.isEmpty()`) are skipped without error.
- Preserves selection order and existing CombineManager file naming.

### 4. Safe Atomic Writes & Consistent OCR
- Photo file operations write to `File(photoFile.parentFile, "${photoFile.name}.tmp")`.
- On successful bitmap compression, atomically replace target file.
- If failure occurs at any stage, temp file is deleted, original file remains intact, and error snackbar is shown.
- Re-run OCR and update database & FTS index for both rotate and crop.

### 5. Canvas Smooth Stroke Engine
- Shared path builder: `StrokePathBuilder.buildStrokePath(path: Path, points: List<StrokePoint>)` used by both in-progress stroke and committed elements.
  - 1 point: Circle dot.
  - 2 points: `moveTo` + `lineTo`.
  - 3+ points: Quadratic Bezier through segment midpoints (`quadTo(curr.x, curr.y, mid.x, mid.y)`), terminating with `lineTo(last.x, last.y)`.
- Live in-progress stroke renders directly in world coordinates via canvas matrix transform, guaranteeing identical geometry before and after finger lift.
- Zoom-aware decimation: tolerance in world units = `0.8f / viewport.scale.coerceAtLeast(0.05f)`.
- Remove placebo `smoothStroke` midpoint insertion.
- Highlighter paint updated to `Cap.ROUND` and `Join.ROUND`.

### 6. Canvas Layer Workflow
- Instant layer creation: `generateNextDefaultLayerName(existingNames)` computes smallest positive integer `N` where `"Layer N"` is unused. Eliminates add-layer dialog.
- Inline rename: long-press on layer name toggles inline `BasicTextField` with full selection, autofocus, and soft keyboard. Confirmed via Done action or focus loss. Blank input reverts to previous name.

### 7. New Folder Dialog Autofocus
- Name field attaches `FocusRequester` and `LocalSoftwareKeyboardController`.
- Launches on display with small 100ms settle delay to guarantee window attachment and keyboard appearance.

### 8. Home Screen (+) Direct Folder Creation
- Removed camera option from `FloatingDock`.
- Tapping (+) button directly invokes `onNewFolderClick`, opening `NewFolderDialog` immediately without intermediate menu.
- Removed unused `menu_capture_notes` string.

---

## UI Mockup

### PDF Viewer Reading Mode Zoom (4.0x Sharp)
```
+-------------------------------------------------------------+
| (<-)  Lecture_05_Calculus.pdf                     (Book) (:) |
+-------------------------------------------------------------+
| [Viewport Zoom 3.5x]                                        |
|                                                             |
|   +-----------------------------------------------------+   |
|   |  Theorem 4.2 (Fundamental Theorem of Calculus)     |   |
|   |  If f is continuous on [a, b], then...              |   |
|   |  [Sharp high-res vector rendering at 3.5x scale]    |   |
|   +-----------------------------------------------------+   |
+-------------------------------------------------------------+
```

### Canvas Layers Inline Rename
```
+------------------------------------+
| Layers                  [+ Add]    |
|------------------------------------|
| [::] [ [Background Draft|] ] (^) (v)|
| [::] Layer 2                 (^) (v)|
| [::] Layer 1                 (^) (v)|
+------------------------------------+
```

---

## Logic Notes

### PDF Matching Algorithm
```kotlin
fun findMatchingPageIndex(pages: List<DocumentPage>, query: String): Int {
    val clean = normalizeForSearch(query)
    val tokens = clean.split("\\s+".toRegex()).filter { it.isNotBlank() }
    if (tokens.isEmpty() || pages.isEmpty()) return 0

    // Priority 1: First page containing all terms
    val allMatch = pages.firstOrNull { page ->
        val text = page.ocrText?.let { normalizeForSearch(it) } ?: return@firstOrNull false
        tokens.all { text.contains(it) }
    }
    if (allMatch != null) return allMatch.pageIndex

    // Priority 2: First page containing any term
    val anyMatch = pages.firstOrNull { page ->
        val text = page.ocrText?.let { normalizeForSearch(it) } ?: return@firstOrNull false
        tokens.any { text.contains(it) }
    }
    return anyMatch?.pageIndex ?: 0
}
```

---

## Risks & Mitigation
- **Risk**: High-resolution 4.0x PDF bitmap causes OutOfMemoryError on low-memory devices.
  - *Mitigation*: Strictly cap pixel budget at `4_000_000` pixels (~16MB) and automatically scale down requested dimensions if budget is exceeded.
- **Risk**: In-progress stroke does not align with committed stroke during zoom.
  - *Mitigation*: Both render through `StrokePathBuilder.buildStrokePath` using canvas viewport matrix scaling rather than manual screen-point conversions.

---

## Acceptance Criteria
- [x] PDF viewer renders sharp text up to 4.0x zoom without blank flickers or OOM.
- [x] High-res bitmaps are evicted from memory when zoom resets to 1.0x.
- [x] Notes tab opens PDF notes directly into `PdfViewerScreen` at page 1.
- [x] Search results jump directly to the matching OCR page in `PdfViewerScreen`.
- [x] Multi-select combine to PDF and Word includes text notes and non-empty canvas notes.
- [x] Rotate and crop use safe atomic temp-file replacement and re-run OCR + FTS.
- [x] Photo viewer and PDF viewer display "Info" in the three-dot menu header.
- [x] Canvas strokes draw smooth C1 curves without angular low-poly segments.
- [x] Canvas highlighter uses round caps and round joins without spikes.
- [x] Tapping "Add Layer" creates "Layer N" immediately; long-pressing layer name enables inline rename.
- [x] New Folder dialog automatically focuses and shows soft keyboard on open.
- [x] Home (+) button directly triggers New Folder dialog without popup menu.
- [x] All pure-logic unit tests pass 100%.
