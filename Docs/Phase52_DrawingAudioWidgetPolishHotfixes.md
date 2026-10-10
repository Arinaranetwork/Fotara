// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

# Phase 52 - DrawingAudioWidgetPolishHotfixes

## Goal
Address user evaluation findings and upgrade core drawing, audio, and widget capabilities for Fotara 2.0.1:
1. Expose visible, configurable stroke smoothing and geometric shape snapping controls in drawing toolbars.
2. Upgrade vector text annotations to allow post-creation text editing, text alignment (Left, Center, Right), multi-line handling, and styling.
3. Fix audio annotations in PDF with Android runtime microphone permission handling, page-anchored audio pins with card badges, and robust AAC recording to disk.
4. Correct photo widget SQLite queries from `file_uri` to `file_path`, and replace confusing "coursework" phrasing with clear universal study terminology.

## Scope
- **Drawing Smoothing & Shapes**:
  - Add visible Shape & Auto-Smooth tool button to `CanvasScreen` and `PdfPageEditorScreen` tool docks.
  - Add Auto-Smoothen and Shape Snapping toggles with smoothing strength slider to tool options popup.
  - Render smoothed cubic Bézier curves on stroke release.
- **Text Layer Editing & Alignment**:
  - Extend `TextLayerElement` with `alignment: TextLayerAlignment = TextLayerAlignment.LEFT`.
  - Update `CanvasRenderer` and `PhotoDrawingRenderer` to align text (Left, Center, Right) and support multi-line text blocks.
  - Enable tapping/selecting existing text layers to reopen text editor dialog pre-filled with existing text string, alignment, and styling.
  - Add text alignment selection buttons in `CanvasScreen` and `PdfPageEditorScreen`.
- **Real PDF Audio Annotations**:
  - Add `rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission())` for `RECORD_AUDIO` in `PdfViewerScreen`, `PdfPageEditorScreen`, and `TextNoteEditorScreen`.
  - Pass active PDF page index when saving voice note so recordings are anchored to the viewed page.
  - Display interactive audio badge on PDF page cards in grid and thumbnail views.
- **Glance Photo Widgets**:
  - Fix SQL column name in `PhotoCarouselGlanceWidget`, `PhotoPinGlanceWidget`, and `PhotoPinConfigActivity` from `p.file_uri` to `p.file_path`.
  - Update user-facing strings from "coursework" to "Study Notes" / "Recent Photos".

## Out Of Scope
- Cloud synchronization of audio files (pure local-first SQLite and filesystem storage).
- Cloud AI speech-to-text transcription.

## Features

### Visible Shape & Auto-Smoothen Tool
- New tool in dock with category/shapes icon.
- Tool options popup features:
  - Auto-Smoothen switch ("Smooth handwriting & lines")
  - Shape Snap switch ("Snap to rectangle, circle, arrow on hold")
  - Smoothing Strength slider (Subtle, Balanced, Strong)

### Text Layer Post-Creation Editing
- Clicking on any existing text layer selects it and offers "Edit Text" action.
- Dialog allows editing the text string, changing alignment (`LEFT`, `CENTER`, `RIGHT`), adjusting font size, weight, color, and background style.
- Can delete text layer directly from edit dialog.

### Real Audio Annotations with Permission Gating
- User taps "Record Audio", app prompts for microphone runtime permission if not granted.
- Once granted, starts real `MediaRecorder` recording in AAC (.m4a) format.
- Stops and saves to SQLite `audio_annotations` with `pdf_doc_id` and `pdf_page_index`.
- PDF pages with audio recordings display a pill badge with audio note count; tapping opens the audio playback bar.

### Photo Widget Fix
- Resolves SQLite queries using `p.file_path`.
- Loads and downsamples photos under 512px.
- Displays clear titles: "Fotara Study Notes", "Recent Study Photos", "No photos in Fotara yet".

## Acceptance Criteria
- Shape and smoothing controls visible in drawing toolbars.
- Existing text layers can be edited, aligned, and styled after placement.
- Audio recording in PDF prompts for permission, records audio, and attaches to specific pages.
- Home screen photo widgets load photos cleanly from database without exceptions.
- 100% passing unit tests.
