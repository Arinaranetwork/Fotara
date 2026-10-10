# Fotara 1.9.1 Alpha

## What's Changed
- **Universal English Academic Terminology**: Complete localization overhaul of the timetable and schedule subsystem from Indonesian to clean, universal academic English ("Class Schedule", "Next-Day Rollover Cutoff: 18:00 Edit", "Course / Subject", "Mon, Tue, Wed, Thu, Fri, Sat, Sun", "No classes scheduled today").
- **Notes Screen Left-Edge Visual Axis Alignment**: Harmonized horizontal content padding to strict 16.dp across ScreenHeader, WorkspaceTabBar, ScheduleCapsule, and NotesScreen note cards, eliminating the 2dp left-edge margin stagger (Skill Rules U-31, U-35, U-36).
- **Text Layer Interactive Drag & Movement**: Fully implemented touch hit-testing, visual selection bounding boxes, and drag repositioning for vector text layers in both PDF Page Editor (`PdfPageEditorScreen.kt`) and Infinite Canvas (`CanvasToolController.kt`).
- **Drawing Stroke Auto-Smoothing**: Added automatic cubic Bézier curve smoothing (`smoothPointsBezier`) upon pen finger lift, ensuring handwritten lines and geometric figures are rendered smoothly without jagged artifacts.
- **Robust Multi-Line LaTeX Math Typesetting**: Expanded `MarkdownParser` and `MarkdownVisualTransformation` to support multi-line equations across both `$` and `$$` syntax. Enhanced `KatexMathRenderer` with full support for `\boxed`, `\begin{cases}`, matrices (`pmatrix`, `bmatrix`), `\displaystyle`, and piecewise formulas, paired with a live math preview card during note editing.
- **Glance Widgets Real-Time Synchronization**: Fixed empty data state in `TimetableGlanceWidget` by searching for upcoming scheduled class days when today has no classes. Expanded `DueTomorrowWidgetProvider` to track deadlines across photos, text notes, and documents. Added automatic broadcast triggers on repository mutations.
- **Wired In-App Audio & Study Alarm**: Connected `AudioRecordPill` into Text Note Editor and PDF Viewer; exposed the Urgent Anti-Procrastination Study Alarm in Settings.

## Verification
- **Unit Test Suite**: 794 / 794 unit tests passing (100% pass rate).
- **SHA-256 Checksum**:
  `f4938995b6628abbd35255fa1da31a81621b88e7f8ebb92f611246a42b42528d  Fotara_1.9.1_Alpha.apk`
