# Fotara 1.9 - Class Schedule, Canvas Superpowers & Dynamic Widgets
Released: 2026-10-09   Status: Released (Beta)

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

