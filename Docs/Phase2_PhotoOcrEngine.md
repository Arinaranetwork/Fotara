# Phase 2 - PhotoOcrEngine

## Goal
Deliver Fotara's on-device intelligent coursework engine: multi-capture camera workflow, offline OCR text indexing pipeline, auto-suggest folder matching, perspective document straightening, photo contextual quick-actions (move, color, deadline), and local assignment deadline reminder tracking.

## Scope
- Offline OCR Engine abstraction and text recognition processor (`OcrEngine.kt`).
- Auto-suggest folder engine matching recognized keywords against folder titles (`FolderSuggestEngine.kt`).
- Multi-capture camera & document import interface with live page count counter (`MultiCaptureScreen.kt`).
- Perspective cropping and document straightening processor.
- Photo contextual quick-action menu (move subfolder, tag color, set deadline, share, delete).
- Deadline manager calculating and surfacing "Due Tomorrow" coursework and scheduling reminders.
- Unit tests verifying OCR parsing, auto-suggestion scoring, and deadline aggregation.

## Out Of Scope
- Cloud synchronization and multi-device accounts (Fotara is 100% offline-first).
- Full PDF compilation export (Phase 3).
- Cross-folder smart tag queries (Phase 3).

## Features
### On-Device OCR Pipeline
Processes captured images locally using an offline text recognition pipeline. Extracted text is normalized, indexed in `Photo.ocrText`, and split into searchable keywords for real-time search queries.
- **Empty state**: Photos with no detectable text are indexed with a null/empty OCR string and remain searchable by caption or timestamp.
- **Error handling**: Corrupted images or processing exceptions are logged safely without crashing the capture flow.

### Auto-Suggest Folder on Capture
Analyzes recognized text tokens against existing subject folder names, subfolder topics, and known academic keywords (e.g. "mitosis" -> Biology, "integral" -> Calculus). The suggested folder is pre-selected in the `CaptureReviewSliderModal` with high confidence.

### Multi-Capture Camera & Import Workflow
Students can capture consecutive whiteboard photos or lecture slides in rapid succession without leaving the viewfinder. The captured batch is directly passed into the 75% height `CaptureReviewSliderModal` for triage before saving.

### Photo Quick-Action Menu
Long-pressing any photo card in the grid opens a bottom sheet with actions: Move to Subfolder, Assign Tag Color, Set/Edit Deadline, and Delete.

### Coursework Deadline Reminders
Photos can have an attached `linked_deadline` timestamp. The home screen "Due Tomorrow" banner aggregates all upcoming deadlines within 24–48 hours, highlighting urgent tasks with amber accent styling.

## UI Mockup
```
MULTI-CAPTURE CAMERA VIEW
+------------------------------------------+
|  [✕ Close]                    [⚡ Flash]  |
|                                          |
|       +--------------------------+       |
|       |   Document Viewfinder    |       |
|       |   [Auto-Perspective]     |       |
|       +--------------------------+       |
|                                          |
|  [Gallery]      ( 📸 Capture )   [Review (3)]
+------------------------------------------+

PHOTO QUICK ACTION SHEET (LONG PRESS ON PHOTO)
+------------------------------------------+
|  Note #14: Lecture 4 Whiteboard          |
|  --------------------------------------- |
|  [📁] Move to Subfolder...               |
|  [🏷️] Change Color Label...             |
|  [📅] Set / Reschedule Deadline...       |
|  [🔗] Copy Recognized OCR Text           |
|  [🗑️] Delete Note                        |
+------------------------------------------+
```

## Logic Notes
- OCR parsing runs on `Dispatchers.Default` / `Dispatchers.IO` to avoid blocking UI frame rates.
- Auto-suggest scoring computes keyword intersection between OCR tokens and folder names, choosing the folder with the highest match score (>0.4 threshold).
- Deadline calculations evaluate `photo.linkedDeadline` against current epoch timestamp in local time.

## Risks
- Image memory pressure during rapid multi-capture -> Mitigated by downscaling review bitmaps and releasing references promptly.
- Zero OCR results on low-contrast handwriting -> Mitigated by allowing manual caption editing in the note inspector.

## Dependencies
- Android CameraX / Photo Picker APIs
- Kotlin Coroutines & Flow
- Fotara 1.0 Repository Architecture (Phase 1)

## Acceptance Criteria
- [x] On-device OCR processing pipeline implemented and tested.
- [x] Auto-suggest folder logic recommends the appropriate subject folder based on recognized text keywords.
- [x] Multi-capture camera flow captures multiple shots and launches the triage review slider modal.
- [x] Photo card long-press surfaces the contextual quick action menu (move, color, deadline, delete).
- [x] Deadline scheduling and "Due Tomorrow" home screen banner dynamically update.
- [x] Unit tests pass for OCR parsing, auto-suggestion matching, and deadline calculations.
- [x] Packaged build `Fotara_1.0.0_Beta.apk` passes compilation and tests.
