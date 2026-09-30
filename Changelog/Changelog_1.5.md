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
