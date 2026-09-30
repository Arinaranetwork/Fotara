<!-- --- Arinara Network (c) 2026 --- -->
<!-- Exclusive property of Arinara Network. -->
<!-- Unauthorized use, reproduction, distribution, or modification of this code, -->
<!-- in whole or in part, for any purpose, is strictly prohibited without prior -->
<!-- written consent from Arinara Network as sole legal owner of this codebase. -->

# Phase 8 - MasterV150EcosystemAndCanvas

## Goal
Transform Fotara into a unified, resilient, fully localized English study ecosystem for Release 1.5.0 Beta. This master release resolves persistent foundation issues (language consistency, feedback HTTP 400 errors, PDF stability, Split to Images artifacts, and text editor controls), introduces ubiquitous scheduling across all note types with an interactive Today Glance widget, enables seamless incoming Share to Fotara workflows, and introduces the high-performance Unlimited Canvas (Alpha) note engine—all consolidated under version 1.5.0 Beta with zero placeholder implementations.

## Scope
Release 1.5.0 Beta delivers five internal sequential milestones on a single release cycle:
- **Milestone 1 - Foundations**:
  - Global English-only localization across code, resources, strings, notifications, widgets, What's New, and historical changelogs.
  - Hardened Supabase feedback engine fixing HTTP 400 errors, synchronizing client and server schemas, enforcing 5 submissions/24h rolling limit and 60-second cooldown, and graceful offline queuing.
  - PDF stability repair for 30+ page rendering with off-main-thread execution, cancellation of stale jobs, and bounded LRU caching.
  - Split to Images repair eliminating black background artifacts with pre-filled white canvas, auto-creating a Photo Group for >4 pages, and standalone notes for <=4 pages.
  - Header text cleanups removing "Sharp PDF Note" labels across the app.
  - PDF per-page pinch-to-zoom and freeform pan directly within the native PDF Document Note viewer with viewport-density re-rendering.
  - Immutable `dateAdded` metadata column across all note types with database migration and backfill, paired with comprehensive search date filters (Today, Yesterday, This week, This month, This year, custom single-day and date range pickers).
  - Native text note editor toolbar full restoration (fixing bold, italic, strikethrough, headings, quotes) and rich expansion (inline code, links dialog, dividers, checklists with tappable boxes, indent/outdent, word/character count, and find/replace).
  - LinkIt corner glow redesign replacing the amber badge with an API 24–36 compatible gradient radial stroke.
- **Milestone 2 - Schedules & Settings**:
  - Universal note scheduling for photo notes, photo groups, PDF notes, DOCX notes, text notes, and canvas notes.
  - Dual alert modes: standard notification vs. ringing alarm alert with dismiss, snooze, and open deep links, resilient across reboots and Doze.
  - Interactive rectangular "Today" Jetpack Glance widget displaying due items, notes added today, and upcoming schedules with responsive reflow.
  - Fully functional Settings Notification section and comprehensive audit of all Settings controls to eliminate dead toggles or placeholders.
- **Milestone 3 - Share to Fotara**:
  - Registered `ACTION_SEND` and `ACTION_SEND_MULTIPLE` intent receiver for images, PDFs, DOCX, Markdown, and plain text (up to 30 items per share).
  - Safe stream staging into private application storage with background thumbnailing and rotation/process death resilience.
  - Dedicated Placement Screen featuring destination folder/group browsing, cancellation with confirmation, full-width "Place Here (N)" action, and collapsible multi-item selection drawer.
- **Milestone 4 - Unlimited Canvas (Alpha)**:
  - New note type: Infinite 2D drawing canvas with vector stroke encoding and compact binary layer serialization.
  - Strict 8-zone architectural layout (Z1 to Z8) as specified in `Docs/CanvasZoneMap.md`.
  - Core drawing tools (pen, highlighter, stroke/area eraser, stylus pressure, undo/redo, layers management, background styles, and safe PNG export).
  - Deep cross-cutting integration: folder placement, Trash retention, privacy locks, schedules, search indexing, and date added.
- **Milestone 5 - Final Hardening (No New Features)**:
  - Full cross-API regression (API 24 to API 36) across all 6 note types.
  - Performance and memory benchmarks (<1,500ms cold start, smooth 1,000+ notes grid scroll, leak detection).
  - Tested Room migration path from 1.4.0 to 1.5.0 with real data preservation.
  - Security audit of exported components, intents, and file provider paths.
  - Complete documentation and README rewrite for Release 1.5.0 Beta.

## Out Of Scope
- Intermediate public releases (no 1.6.0, 1.7.0, 1.8.0, or 2.0.0 releases; all milestones ship collectively in 1.5.0 Beta).
- Real-time collaborative canvas editing or cloud multi-device sync.
- Destructive database migrations.
- Introduction of any new features or screens in Milestone 5.

## Features

### 1.5-A: English-Only Global Localization
- Audit and translate all user-facing strings across `app/src/main/res/values/strings.xml`, UI composables, ViewModels, dialogs, error handlers, toasts, snackbars, and notifications.
- Remove all legacy Indonesian resource directories (`values-in`, `values-id`) and configure `localeFilters` / `resourceConfigurations = ["en"]`.
- Format all dates, timestamps, month names, and day headers with explicit `Locale.ENGLISH`.
- Translate historical changelog entries (`Changelog_1.1.md` through `Changelog_1.4.md`) into idiomatic English.
- User-generated titles, captions, and note contents remain untouched.

### 1.5-B: Feedback Engine Repair & Rate Limit Hardening
- Eliminate HTTP 400 Bad Request by aligning JSON payload serialization exactly with `Docs/Supabase_Schema.sql`:
  - Enforce exact column names (`id`, `uuid`, `category`, `content`, `email`, `diagnostics`, `created_at`).
  - Send `category` matching database constraints (`BUG_REPORT`, `SUGGESTION`, `FEATURE_IDEA`, `GENERAL`).
  - Send `diagnostics` as structured JSON object or sanitized string according to column type.
  - Include headers: `apikey`, `Authorization: Bearer <anon_key>`, `Content-Type: application/json`, `Prefer: return=minimal`.
- Lower daily submission limit from 30 to 5 per 24-hour rolling window per install UUID.
- Increase client and server cooldown from 3 seconds to 60 seconds with live countdown indicator.
- Harden offline queue: distinguish 4xx permanent client errors from 5xx/network retryable errors.

### 1.5-C: High-Stability PDF Engine & Split to Images Repair
- Resolve scroll freezing on 30+ page PDFs by moving rasterization off the main thread, dispatching rendering jobs via single-threaded coroutine worker with `Mutex`, cancelling out-of-viewport page renders, and prefetching adjacent pages.
- Fix black background artifacts in Split to Images by pre-filling `Color.WHITE` prior to native `PdfRenderer` drawing.
- Auto-grouping threshold: PDFs with >= 5 pages automatically package extracted images into a new `PhotoGroup` titled after the PDF; PDFs with <= 4 pages split into individual standalone photo notes.
- Completely excise the legacy string "Sharp PDF Note" from all subtitles and headers.

### 1.5-D: Virtualized In-Doc PDF Zoom
- Enable pinch-to-zoom and two-axis panning directly on individual pages inside `PdfViewerScreen` without triggering Split to Images.
- Dynamically calculate viewport scale, re-rendering visible sub-regions at high resolution during active zoom, and shedding cached tiles when returning to 1.0x scale.
- Seamless gesture arbitration: single-finger vertical scroll passes through to virtualized `LazyColumn` when zoom == 1.0x; pan gestures take priority when zoom > 1.0x.

### 1.5-E: Immutable Date Added Metadata & Search Date Filters
- Add immutable `date_added` (INTEGER epoch milliseconds) column to all note entities (`photos`, `photo_groups`, `document_notes`, `text_notes`, `canvas_notes`).
- Versioned Room migration backfilling existing records with created timestamp or file modified time.
- Expand `ActiveSearchBar`:
  - Quick filter chips: Today, Yesterday, This week, This month, This year.
  - Custom single-day picker and custom date-range calendar dialog.
  - Sort order toggle: Newest Added vs. Oldest Added.
- Expose date added in note info dialogs in English format.

### 1.5-F: Text Note Editor Toolbar Overhaul
- Repair inactive formatting controls: Bold, Italic, Strikethrough, Heading 1–3, and Blockquote.
- Support both selection-wrap and active cursor-toggle semantics.
- Expand formatting suite:
  - Inline: Bold, Italic, Bold-Italic, Strikethrough, Inline Code, Link Insertion Dialog (URL validation, label edit).
  - Block: H1, H2, H3, Paragraph reset, Blockquote, Code block, Horizontal rule.
  - Lists: Bullet list, Numbered list, Interactive Checklists with tappable boxes, Indent, Outdent.
  - Utilities: Clear formatting, Find & Replace bar, Live word and character counters.
- Ensure round-trip fidelity between Markdown storage and `RichMarkdownText` Compose renderer.

### 1.5-G: LinkIt Corner Glow Redesign
- Remove the intrusive amber outline badge and chain icon from linked folder cards and note cards.
- Implement subtle radial gradient corner glow using `Modifier.drawWithCache` / `drawBehind` on the bottom-left or top-left corner.
- Ensure 100% compatibility from API 24 to API 36 without relying on API 31+ `RenderEffect.createBlurEffect`.
- Preserve accessibility via semantic descriptions (`"Linked with <folder_name>"`).

### 1.6-A: Universal Scheduling Engine
- Enable custom date and time scheduling across all 6 note types (Photo, Group, PDF, DOCX, Text, Canvas).
- Support user-selected alert mode: Normal Notification vs. Ringing Alarm Alert (with audio stream, full-screen intent fallback, and loop).
- Notification action buttons: Dismiss, Snooze (custom duration), and Open Note.
- Resilient background scheduling: Android `AlarmManager.setExactAndAllowWhileIdle()`, boot completion receiver (`BOOT_COMPLETED`), and Doze/battery optimization handling.
- Card badge indicator: Compact clock glyph with date/time docked at the bottom-right corner of card bodies (never conflicting with LinkIt or color tags).
- Detail screen chip and overflow menu entry point.

### 1.6-B: Jetpack Glance "Today" Widget
- Create a dedicated rectangular Glance widget displaying:
  - Items due today (deadlines and schedules).
  - Notes added today (powered by the shared `date_added` query).
  - Upcoming scheduled events for the rest of the day.
- Dark brand aesthetic matching Fotara design guidelines.
- Responsive layout handling varying widget column widths.
- Interactive tap targets deep-linking to corresponding notes, plus quick camera capture launch button.

### 1.6-C & 1.6-D: Settings Audit & Notification Controls
- Build out the Settings Notification section with live permission status, system settings link, master enable switch, default alert mode toggle, default snooze length selector, and interactive test alert trigger.
- Audit all existing Settings sections (Display, Theme, OCR, Storage, Updates, QRIS support, Feedback) to ensure zero dead toggles or placeholder actions.

### 1.7-A to 1.7-E: Share to Fotara Receiver
- Register `SendActivity` for `ACTION_SEND` and `ACTION_SEND_MULTIPLE` supporting images (`image/*`), PDFs (`application/pdf`), Word (`application/vnd.openxmlformats-officedocument.wordprocessingml.document`), Markdown (`text/markdown`), and plain text (`text/plain`).
- Enforce 30-item maximum per share bundle with informative skip alert for overflow items.
- Safely stream incoming URIs into sandboxed cache staging storage without memory pressure.
- Interactive Placement Screen:
  - Destination browser (Folders, Subfolders, Groups) with inline folder creation.
  - "Place Here (N)" primary bottom action.
  - Collapsible side panel listing items with preview thumbnails, checkboxes, and select/deselect all.
  - Persistent session state allowing placement across multiple separate folders until all items are distributed.

### 1.8-A to 1.8-D: Unlimited Canvas (Alpha)
- Implement infinite pan-and-zoom vector drawing canvas structured according to the 8-Zone architectural specification in `Docs/CanvasZoneMap.md`:
  - Z1: Back, Title rename, Save state.
  - Z2: Top-right toolbar (Undo, Redo, Layers, Add Image, Schedule, Export, Overflow).
  - Z3: Bottom floating tool bar (Select, Pen, Highlighter, Eraser, Color Swatch).
  - Z4: Tool options popup (Stroke size, Opacity, Pen profile, Color palette).
  - Z5: Layers manager bottom sheet / side panel.
  - Z6: Interactive drawing viewport with palm rejection and stylus pressure.
  - Z7: Bottom-left zoom percentage chip and fit-to-content trigger.
  - Z8: Contextual object action bar (Duplicate, Delete, Lock, Reorder).
- Compact vector stroke serialization with chunked Room database storage.
- Tiled viewport rendering preventing memory spikes on high-density displays.
- PNG export supporting whole canvas or targeted selection.

### 2.0-A to 2.0-G: Final Hardening & Quality Gate
- Comprehensive regression testing across all note types and subsystems on minSdk (API 24) and targetSdk (API 36).
- Strict performance profiling: Startup < 1.5s cold / < 500ms warm; steady 60 FPS scrolling on large libraries.
- Verified Room database migration path 1.4.0 -> 1.5.0 with zero data loss.
- Full security review: Component export restrictions, FileProvider path guards, zero committed secrets.
- Comprehensive English language audit ensuring zero residual Indonesian text.

## UI Mockup

### 1. Canvas Screen Layout (Zone Map Z1–Z8)
```
+---------------------------------------------------------------+
| [Z1: <- Title  Saved]                   [Z2: Undo Redo Layer + : ] |
|---------------------------------------------------------------|
|                                                               |
|                                                               |
|                        [Z6: Canvas Viewport]                  |
|                   (Strokes, Images, Infinite Pan)             |
|                                                               |
|                     +-----------------------+                 |
|                     | [Z8: Object Bar]     |                 |
|                     | [Dup] [Del] [Lock] [^]|                 |
|                     +-----------------------+                 |
|                                                               |
| [Z7: 100% [Fit]]                                              |
|                     +-----------------------------+           |
|                     | [Z4: Tool Options Popup]    |           |
|                     | Size: ---o--- Color: [ ][ ] |           |
|                     +-----------------------------+           |
|                     | [Z3: [V] [Pen] [High] [Era] [Color] ]   |
|                     +-----------------------------+           |
+---------------------------------------------------------------+
```

### 2. Share Placement Screen Layout
```
+---------------------------------------------------------------+
| [X Cancel]   Select Destination Folder           [Items: (4)>]|
|---------------------------------------------------------------|
|  [Folder Card: Biology]       [Folder Card: Calculus]         |
|  [Folder Card: History]       [Folder Card: Literature]       |
|                                                               |
|  [+ New Folder]                                               |
|                                                               |
|---------------------------------------------------------------|
|                [ Place Here (4 Items) ]                       |
+---------------------------------------------------------------+
```

### 3. Folder Card with Redesigned LinkIt Corner Glow & Schedule Glyph
```
+------------------------------------+
|  [Tab: Math Coursework]            |
+------------------------------------+
| (* Glow)                           |
|   MATH 201: LINEAR ALGEBRA         |
|   14 Notes · Updated 2h ago        |
|                                    |
| [Dot]                      [(O) Oct 12, 10:00 AM] |
+------------------------------------+
```

## Logic Notes
- **Room Migration (v10 -> v11)**:
  - Add `date_added` (INTEGER NOT NULL DEFAULT 0) to tables `photos`, `photo_groups`, `document_notes`, `text_notes`.
  - Create table `schedules` (`id` TEXT PRIMARY KEY, `target_id` TEXT NOT NULL, `target_type` TEXT NOT NULL, `title` TEXT NOT NULL, `trigger_time` INTEGER NOT NULL, `is_alarm` INTEGER NOT NULL, `is_active` INTEGER NOT NULL).
  - Create table `canvas_notes` (`id` TEXT PRIMARY KEY, `folder_id` TEXT NOT NULL, `title` TEXT NOT NULL, `date_added` INTEGER NOT NULL, `last_modified` INTEGER NOT NULL, `background_style` TEXT NOT NULL, `thumbnail_path` TEXT, `is_trashed` INTEGER NOT NULL DEFAULT 0);
  - Create table `canvas_layers` (`id` TEXT PRIMARY KEY, `canvas_id` TEXT NOT NULL, `name` TEXT NOT NULL, `order_index` INTEGER NOT NULL, `is_visible` INTEGER NOT NULL, `is_locked` INTEGER NOT NULL, `opacity` REAL NOT NULL);
  - Create table `canvas_strokes` (`id` TEXT PRIMARY KEY, `canvas_id` TEXT NOT NULL, `layer_id` TEXT NOT NULL, `tool_type` TEXT NOT NULL, `color` INTEGER NOT NULL, `size` REAL NOT NULL, `points_blob` BLOB NOT NULL);
  - Create table `canvas_images` (`id` TEXT PRIMARY KEY, `canvas_id` TEXT NOT NULL, `layer_id` TEXT NOT NULL, `image_path` TEXT NOT NULL, `x` REAL NOT NULL, `y` REAL NOT NULL, `width` REAL NOT NULL, `height` REAL NOT NULL, `rotation` REAL NOT NULL);
- **Scheduler & Alarm Engine**:
  - `ScheduleAlarmManager` wraps `AlarmManager.setExactAndAllowWhileIdle()` using `PendingIntent.getBroadcast()`.
  - Notification payload carries `TARGET_NOTE_ID`, `TARGET_NOTE_TYPE`, and `SCHEDULE_ID`.
  - Action intents for `DISMISS`, `SNOOZE` (+10 minutes default), and `OPEN` routing to `MainActivity` deep-link.

## Risks & Mitigations
- **Risk**: Memory overflow when drawing or panning on high-resolution canvas with thousands of vector points.
  - *Mitigation*: Vector spatial quad-tree segmentation rendering only visible viewport strokes; raster tile caching for static background layers.
- **Risk**: Exact alarms blocked or throttled by OEM battery management or API 33+ permission denial.
  - *Mitigation*: Graceful degradation to standard notification scheduling via WorkManager when exact alarm capability is denied; dedicated explainer UI guiding users to system battery optimization settings.
- **Risk**: Staging storage exhaustion during large bulk file sharing.
  - *Mitigation*: Strictly enforce 30-item limit per session, stream copy with 8KB buffer, and opportunistic cleanup of staging directories on session completion or cancellation.

## Dependencies
- Phase 7 (Native Text Notes and High-Fidelity Documents)
- AndroidX Glance 1.1.0 (Widget infrastructure)
- Android AlarmManager and NotificationManager APIs

## Acceptance Criteria
1. English-only compliance: 0 Indonesian strings in `app/src/main/res`, Kotlin source files, notifications, or changelogs.
2. Supabase feedback submissions succeed with 200/201 response across all four categories; 5/day and 60s cooldown strictly enforced on client and database.
3. Smooth 60 FPS scrolling on 30+ page PDF documents without UI freezing or memory spikes.
4. Split to Images generates clean white-background photos; creates PhotoGroup when page count >= 5, standalone notes when <= 4.
5. In-viewer PDF pinch-to-zoom and pan functions smoothly without triggering Split to Images.
6. Search date filters accurately restrict results by Today, Yesterday, This week, This month, This year, or custom date ranges.
7. Text note editor toolbar buttons (Bold, Italic, Headings, Lists, Checklists, Quotes) operate reliably on selections and active typing.
8. LinkIt corner glow displays consistently without visual artifacts from API 24 to 36.
9. Schedules can be attached to every note type, triggering notifications or alarms reliably across reboots and app termination.
10. Glance Today widget renders correctly and refreshes dynamically upon data modifications and midnight rollover.
11. Sharing 1 to 30 external files opens the placement screen and routes notes cleanly into target destinations.
12. Unlimited Canvas note allows fluid vector sketching, layering, image placement, and PNG export with 0 ANRs or memory leaks.
13. Complete automated unit test suite passing with 0 errors and verified clean release APK build for Fotara 1.5.0 Beta.
