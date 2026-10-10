# Fotara 2.0.1 Alpha

## What's Changed
- **Visible Drawing Shape & Auto-Smoothen Controls**: Added dedicated `[ Shapes & Smooth ]` button with vector icon to drawing toolbar docks in both Drawing Canvas (`CanvasScreen.kt`) and PDF Page Editor (`PdfPageEditorScreen.kt`). Added intuitive configuration options: Auto-Smoothen switch, Shape Snapping on hold, smoothing strength presets, and explicit geometric shape selectors (Square / Rectangle, Circle, Arrow, Line, Auto Snap).
- **Text Layer Post-Creation Editing & Alignment**: Enhanced vector text layer annotations in both Canvas and PDF Editor with full post-placement editing. Tapping any placed text layer opens an edit modal to modify text strings, adjust alignment (`Left`, `Center`, `Right`), change typography weight and size, alter card background style, or delete the layer directly.
- **Real Functional Audio Annotations in PDF**: Replaced mock audio recording paths with fully functional local AAC voice recording. Added runtime `RECORD_AUDIO` permission gating with Android system dialog prompts. Voice recordings are anchored to specific PDF pages and display interactive `[ 🎙️ Audio Notes ]` pill badges on PDF page cards, with tapping opening the dedicated audio dock and playback bar. Also hardened audio recording in Text Notes.
- **Glance Photo Widgets Database Fix & Copy Clarification**: Fixed SQLite column query in `PhotoCarouselGlanceWidget`, `PhotoPinGlanceWidget`, and `PhotoPinConfigActivity` by selecting valid `p.file_path` rather than unmapped `p.file_uri`. Replaced confusing "coursework" phrasing with clear universal terminology ("Fotara Study Notes", "Recent Study Photos").
- **Universal English Academic Terminology in Desktop App**: Overhauled all sample data, folders, notes, and search command palette placeholders in `FotaraDesktop` from Indonesian leftovers ("Jadwal Psts", "Sejarah", "Seni Budaya", "Matematika Lanjut", "Matematika Wajib", "Fisika") to universal English academic terminology ("Class Schedule", "History", "Arts & Culture", "Advanced Mathematics", "Calculus", "Physics").
- **LaTeX Math Parser Prefix Ordering Fix**: Fixed macro replacement ordering in `KatexMathRenderer.kt` by evaluating Greek and operator macros descending by length before subscripts and superscripts, and matching using negative letter lookahead. Prevents shorter prefix collisions (`\in` corrupting `\infty`, `\le` corrupting `\leq`, `\ge` corrupting `\geq`, `\ne` corrupting `\neq`, `\int` corrupting `\iint`).

## Verification
- **Unit Test Suite**: 892 / 892 unit tests passing (100% pass rate).
- **Desktop Production Build**: Vite React TypeScript build cleanly compiled with 0 errors.
- **SHA-256 Checksum**:
  `CC6511882409BA0A1F9FD40E0A9C0C46F437B1A189CDA83E10445B648D0C1CC7  Fotara_2.0.1_Alpha.apk`
