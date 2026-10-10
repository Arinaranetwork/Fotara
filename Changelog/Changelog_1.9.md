# Fotara 1.9 - Class Schedule, Canvas Superpowers & Dynamic Widgets
Released: 2026-10-09   Status: Released (Beta)

## 1.9.1 Alpha - 2026-10-10
- **Universal English Academic Terminology**: Complete localization overhaul of the timetable and schedule subsystem from Indonesian to clean, universal academic English ("Class Schedule", "Next-Day Rollover Cutoff: 18:00 Edit", "Course / Subject", "Mon, Tue, Wed, Thu, Fri, Sat, Sun", "No classes scheduled today").
- **Notes Screen Left-Edge Visual Axis Alignment**: Harmonized horizontal content padding to strict 16.dp across ScreenHeader, WorkspaceTabBar, ScheduleCapsule, and NotesScreen note cards, eliminating the 2dp left-edge margin stagger (Skill Rules U-31, U-35, U-36).
- **Text Layer Interactive Drag & Movement**: Fully implemented touch hit-testing, visual selection bounding boxes, and drag repositioning for vector text layers in both PDF Page Editor (`PdfPageEditorScreen.kt`) and Infinite Canvas (`CanvasToolController.kt`).
- **Drawing Stroke Auto-Smoothing**: Added automatic cubic Bézier curve smoothing (`smoothPointsBezier`) upon pen finger lift, ensuring handwritten lines and geometric figures are rendered smoothly without jagged artifacts.
- **Robust Multi-Line LaTeX Math Typesetting**: Expanded `MarkdownParser` and `MarkdownVisualTransformation` to support multi-line equations across both `$` and `$$` syntax. Enhanced `KatexMathRenderer` with full support for `\boxed`, `\begin{cases}`, matrices (`pmatrix`, `bmatrix`), `\displaystyle`, and piecewise formulas, paired with a live math preview card during note editing.
- **Glance Widgets Real-Time Synchronization**: Fixed empty data state in `TimetableGlanceWidget` by searching for upcoming scheduled class days when today has no classes. Expanded `DueTomorrowWidgetProvider` to track deadlines across photos, text notes, and documents. Added automatic broadcast triggers on repository mutations.
- **Wired In-App Audio & Study Alarm**: Connected `AudioRecordPill` into Text Note Editor and PDF Viewer; exposed the Urgent Anti-Procrastination Study Alarm in Settings.

## 1.9.0 Beta - 2026-10-09
- **1-Line Dynamic Schedule Capsule in Notes**: Integrated compact 36dp dynamic schedule capsule strictly below `WorkspaceTabBar` and immediately above `NotesFilterChipsRow` in Notes. Intelligently displays active lecture with live countdown timer or next upcoming class during the day, and automatically transitions to tomorrow's schedule after the cutoff hour.
- **Lightweight Zero-Dependency Excel & Word Timetable Parser**: Parses `.xlsx` and `.docx` schedules directly by streaming archive XML files (`sheet1.xml`, `sharedStrings.xml`, and `word/document.xml`) using platform `XmlPullParser` with zero third-party dependencies, keeping APK size well below 25MB. Includes Smart Column Mapping Dialog with auto-detection for Day, Time, Subject, Room, and Lecturer.
- **Configurable Next-Day Rollover Cutoff Engine**: User-adjustable cutoff time (default `18:00`) that switches timetable view and reminders to the upcoming day's classes for evening book and bag preparation, with weekend auto-skip to Monday.
- **Proactive Timetable Notifications**: Android `AlarmManager` reminders for Morning Digest (06:30), Pre-Class Heads-Up (10 minutes before class with quick capture shortcut), and Evening Rollover Alert.
- **Sub-Screen Composition State Persistence**: Preserved sub-screen UI states across bottom tabs (`Home`, `Notes`, `Settings`) using Compose `SaveableStateHolder`. Form inputs, active sub-sections, and scroll positions are retained when toggling between tabs.
- **Canonical Re-Tap Navigation**: Tapping an active bottom navigation tab pops any open sub-screen back to root; tapping while already at root initiates a smooth animated scroll to top.
- **STEM Note LaTeX Mathematical Typesetting**: Full inline (`$...$`) and block (`$$...$$`) LaTeX math formulas rendered with KaTeX typography in Text Notes with live editing preview.
- **Markdown Table Editor with Formatting Constraints**: Structured grid table editing with formatting guardrails (block tools safely disabled inside table cells to preserve syntax).
- **Shape & Curve Auto-Correct**: Intelligent 400ms draw-and-hold stroke snapping to geometric primitives (rectangles, circles, ellipses, triangles, straight arrows) with cubic Bézier smoothing and haptic feedback.
- **Text Layers Tool (`[ T ]`)**: Vector text annotation tool in Drawing Canvas and PDF Page Editor with ElmsSans typography, 12-48sp font scale, and palette styling.
- **Multi-Sheet Drawing Canvas**: Multi-page drawing support (up to 10 sheets) with independent undo/redo histories and consolidated multi-page PDF export.
- **Android Home Screen Timetable Glance Widget (4x2 / 4x4)**: Real-time home screen schedule widget with responsive 4x2 summary and 4x4 expanded layouts, featuring dynamic evening rollover to next-day classes.
- **Android Home Screen Photo Glance Widgets**: Specific single coursework photo pin widget and recent lecture photo carousel widget with safe bitmap downsampling (max 512px) to prevent `TransactionTooLargeException`.
- **Synchronized Audio Annotations**: 5-minute hard-capped AAC voice recordings seamlessly attached to notes or anchored to individual PDF pages with playback scrubber and error resilience.
- **Urgent Anti-Procrastination Alarm**: Exact looping wake-up study alarm requiring a camera snapshot proof of study materials/desk or emergency PIN unlock to silence, with back/swipe dismissal disabled.

