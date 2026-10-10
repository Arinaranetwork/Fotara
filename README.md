<!-- --- Arinara Network (c) 2026 --- -->
<!-- Exclusive property of Arinara Network. -->
<!-- Unauthorized use, reproduction, distribution, or modification of this code, -->
<!-- in whole or in part, for any purpose, is strictly prohibited without prior -->
<!-- written consent from Arinara Network as sole legal owner of this codebase. -->

<div align="center">

![Fotara Banner](Assets/Banners/FotaraBanner_2.2_2026-10-10.jpg)

# Fotara
### Intelligent Local-First Coursework & Study Note Organization for Android

[![Platform](https://img.shields.io/badge/Platform-Android%207.0%2B%20(API%2024%2B)-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com)
[![Latest Release](https://img.shields.io/github/v/release/Arinaranetwork/Fotara?style=flat-square&color=00B4D8&label=Release)](https://github.com/Arinaranetwork/Fotara/releases/latest)
[![Downloads](https://img.shields.io/github/downloads/Arinaranetwork/Fotara/total?style=flat-square&color=2A9D8F)](https://github.com/Arinaranetwork/Fotara/releases)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20M3-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Distribution](https://img.shields.io/badge/License-Proprietary%20%26%20Educational-2A9D8F?style=flat-square)](#distribution--license)
[![Issues](https://img.shields.io/badge/Issues-Open%20%26%20Active-brightgreen?style=flat-square)](https://github.com/Arinaranetwork/Fotara/issues)

<p align="center">
  <b>Fotara</b> is a high-performance, private, on-device study organization ecosystem built for students, researchers, and lifelong learners. Effortlessly structure, search, schedule, and review whiteboard captures, textbook excerpts, handwritten equations, rich text notes, multi-page PDFs, Word documents, and infinite drawing canvases with zero cloud lock-in.
</p>

[Download Latest APK](https://github.com/Arinaranetwork/Fotara/releases/latest) • [Features](#key-features) • [2.x Feature Matrix](#2x-feature-matrix) • [Installation](#installation) • [Architecture](#architecture--tech-stack) • [Issues & Feedback](https://github.com/Arinaranetwork/Fotara/issues)

</div>

---

## Legal

By downloading, installing or using Fotara you acknowledge that you have read, understood and agree to the [Privacy Policy](Legal/PRIVACY.md) and the [Terms of Service](Legal/TERMS.md). The same documents are shown inside the app on first launch and must be accepted to use it.

---

## Overview

Traditional mobile gallery apps dump educational material alongside casual snapshots without structure, academic deadline awareness, or handwritten text recognition.

**Fotara** delivers a unified, local-first study ecosystem engineered for high-density academic workflows:
- **Academic Spaces**: Isolated vaults for distinct courses, semesters, and research projects with biometric lock support.
- **Syllabus Performance Evaluator**: Interactive weighted grade calculator and required final exam benchmark projector.
- **Universal Note Pinning**: Keep up to 4 critical notes, assignments, or cheat-sheets pinned at the top of any folder.
- **Rich Markdown Tables**: Render and format GitHub-Flavored Markdown tables natively within rich text notes.
- **Unlimited Drawing Canvas (Alpha)**: Infinite 2D vector drawing canvas with smooth pan, pinch-to-zoom, pressure-sensitive pen, highlighter, eraser, multi-layer management, image placement, and high-resolution PNG export.
- **Universal Note Scheduling**: Attach reminder notifications or ringing alarms to any note type (Photos, Photo Groups, PDFs, Word DOCX, Text Notes, and Canvas) with full Doze and device reboot resilience.
- **Dedicated Glance Widgets**: Rectangular home screen widgets displaying coursework due tomorrow, today's notes, and upcoming scheduled alerts with instant deep-linking.
- **Share to Fotara**: Receive external images, PDFs, Word documents, Markdown files, or plain text notes directly into Fotara with an intuitive destination placement screen.
- **On-Device Optical Character Recognition (OCR)**: Google ML Kit extracts text from photo notes locally and populates an SQLite FTS4 full-text search index for sub-second retrieval.

---

## 2.x Feature Matrix

| Capability | Module | Highlights |
|---|---|---|
| **Academic Spaces** | `feature.space` | Multi-space isolated vaults for different courses, semesters, and projects with biometric locks and custom icons. |
| **Syllabus Performance Evaluator** | `feature.academic.syllabus` | Dynamic grade standing calculator, weighted coursework projections, and required final exam target slider. |
| **Universal Note Pinning** | `ui.folder`, `ui.group` | Pin up to 4 vital notes (photos, groups, documents, text notes, canvas) to the top of folder grids with visual badges. |
| **Rich Markdown Tables** | `ui.note.components` | Native GFM markdown table rendering with zebra striping, auto column sizing, and quick-insert toolbar. |
| **Glance Coursework Widgets** | `widget` | Real-time home screen widgets for Due Tomorrow alerts, Today's notes, and scheduled exam countdowns. |
| **Unified Timetable & Reminders** | `util.NoteScheduleManager` | High-priority alarm and notification reminders resilient across device reboots and Doze mode. |
| **Unlimited Vector Canvas** | `ui.canvas` | Infinite 2D pan/zoom drawing canvas with pressure-sensitive strokes, layers, and high-res PNG export. |
| **On-Device OCR & FTS4 Search** | `ocr`, `data.db` | Sub-second offline text recognition and indexing powered by Google ML Kit. |

---

## Key Features

### Coursework Hierarchy & Folder Management
- **Subject Folders**: Color-coded subject folders with live note counts and last-active timestamps.
- **Subfolder Tabs**: Clean horizontal tab navigation inside folders (e.g., *Lectures*, *Lab Notes*, *Assignments*) featuring inline long-press rename, trash confirmation, and multi-select bulk operations.
- **Adaptive Grid Density**: Live grid density preferences (2, 3, or 4 columns) with responsive scaling on foldables, landscape, and large-screen tablets (up to 7 columns).
- **Universal Pinning**: Pin up to 4 critical notes per view to keep urgent coursework permanently at the top of your workspace.

### Academic Spaces & Syllabus Evaluator
- **Dedicated Space Settings**: Customize space name, icon, accent theme, and vault privacy lock per space.
- **Embedded Syllabus Evaluator**: Input course components, track weighted earned points, and calculate the exact exam score required to secure your target grade.

### Unlimited Drawing Canvas (Alpha)
- **Infinite 2D Viewport**: Freeform two-axis pan and continuous pinch-to-zoom (0.2x to 5.0x) with single-tap zoom reset and fit-to-content.
- **Vector Stroke Engine**: Smooth Bézier curves with pressure sensitivity across ballpoint, chisel, and brush nib profiles.
- **Layer & Asset Management**: Multi-layer organization with lock, hide, opacity slider, layer reordering, and external image attachments.
- **Background Grids**: Switch seamlessly between blank, graph grid, dot grid, and ruled college-lined paper backgrounds.
- **High-Resolution Export**: Render visible canvas contents to full-quality PNG images shareable via Android system sheet.

### Universal Note Scheduling & Glance Widgets
- **Ubiquitous Reminders**: Attach reminder schedules to photos, groups, documents, text notes, and canvas drawings.
- **Dual Alert Channels**: Choose between standard unobtrusive notifications or persistent ringing alarm alerts with snooze (+10 min) and dismiss options.
- **Glance Home Widgets**: Real-time study dashboard showcasing items due today, tomorrow's deadlines, and scheduled alert countdowns.

### High-Fidelity Documents & Rich Text
- **In-Viewer PDF Zoom & Photo-Style Page Viewer**: Dedicated fullscreen page viewer with pinch-to-zoom up to 4.0x, two-axis pan, double-tap zoom, zoom-gated swipe navigation, and recognized OCR text drawer.
- **Intelligent Split to Images**: Convert PDF documents into white-canvas photos, automatically grouping documents with 5 or more pages into a Photo Group.
- **Native Markdown Notes & Tables**: Full markdown formatting toolbar with word count, character count, find/replace, interactive tables, and dedicated raw MD / TXT sharing.

### On-Device OCR & Sub-Second Search
- **100% Offline Indexing**: ML Kit text recognition extracts text from photos, documents, and notes upon capture or import.
- **Search Date Filters**: Filter notes instantly by date added with quick chips (Today, Yesterday, This week, This month, This year) and custom date pickers.
- **Waypoint Navigation**: Search results automatically open the source folder, select the matching subfolder, smooth-scroll to the card, and highlight it.

---

## Application Status

| Metric | Status |
|---|---|
| **Latest Release** | [GitHub Releases](https://github.com/Arinaranetwork/Fotara/releases/latest) |
| **Release Channel** | Beta |
| **Supported Devices** | Android 7.0 (API 24) through Android 16 (API 36) |
| **Issue Tracker** | **Active & Open** — Bug reports and suggestions are welcome via [GitHub Issues](https://github.com/Arinaranetwork/Fotara/issues). |
| **Distribution Model** | **Free for Personal and Educational Use** |

---

## Distribution & License

Fotara is distributed under the **Arinara Network Proprietary & Educational Software License**.

- **Personal & Educational Use**: You are free to download, install, and use Fotara on any compatible personal Android device for coursework, research, and individual study.
- **Intellectual Property**: Fotara is the exclusive property of **Arinara Network**. All rights reserved.
- **Restrictions**: Commercial redistribution, sublicensing, repackaging, reverse engineering for resale, or unauthorized mirror hosting is strictly prohibited without prior written consent from Arinara Network.

---

## Installation

1. Navigate to the **[Latest Release](https://github.com/Arinaranetwork/Fotara/releases/latest)** page.
2. Under **Assets**, download the shippable release APK (`Fotara_*.apk`).
3. On your Android device, open the downloaded `.apk` file.
4. If prompted, grant permission to *Install from Unknown Sources* for your browser or file manager.
5. Launch Fotara, complete the guided first-run onboarding, and organize your study notes!

---

## Architecture & Tech Stack

```
Fotara/
├── app/src/main/java/com/arinara/fotara/
│   ├── data/
│   │   ├── db/          # SQLite FTS4 virtual table helper & schema v21
│   │   ├── model/       # Domain models (Space, Folder, Photo, Group, Document, TextNote, CanvasNote)
│   │   └── repository/  # Reactive repositories with Kotlin StateFlow & IO coroutines
│   ├── feature/
│   │   ├── academic/    # SyllabusEvaluator, SyllabusEvaluatorSheet
│   │   ├── space/       # ActiveSpaceManager, SpaceSwitcherBottomSheet
│   │   └── friends/     # Local-first study peer sharing
│   ├── ocr/             # Google ML Kit on-device text recognition pipeline
│   ├── theme/           # Premium dark-first palette, typography, and shape tokens
│   ├── util/            # NoteScheduleManager, PdfExporter, ZipExporter, DocxParser
│   ├── widget/          # Jetpack Glance "Today" & "Due Tomorrow" home screen widgets
│   ├── online/          # UpdateManager, FeedbackManager, and Supabase client
│   └── ui/              # Jetpack Compose UI architecture
│       ├── canvas/      # Unlimited Canvas engine, tools dock, layers sheet, viewport
│       ├── space/       # SpaceSettingsScreen, SpaceSwitcherBottomSheet
│       ├── share/       # ShareReceiverActivity, staging, and SharePlacementScreen
│       ├── folder/      # FolderDetailScreen, subfolder tabs, grid cards, multi-select, pin badges
│       ├── group/       # Dedicated GroupDetailScreen & GroupDetailViewModel
│       ├── document/    # Dedicated PdfViewerScreen & DocxViewerScreen
│       ├── note/        # TextNoteEditorScreen with markdown formatting toolbar & tables
│       ├── home/        # Home screen, folder grid, active search dock, date filters
│       ├── onboarding/  # Guided pre-permission and privacy explainer
│       ├── settings/    # Animated sub-screen navigation, display density, OCR, updates
│       ├── support/     # QRIS donation and community backing screen
│       └── trash/       # 30-day soft-delete recycle bin
├── Assets/              # Project release banners, logos, and typography
├── Docs/                # Product Codex, interaction specifications, and version records
└── Output/Release/      # Verified shippable APK release packages
```

---

## Feedback & Issues

Have an idea for a feature or encountered a bug? We encourage you to open an issue:
- [Submit a Bug Report](https://github.com/Arinaranetwork/Fotara/issues/new)
- [Request a Feature](https://github.com/Arinaranetwork/Fotara/issues/new)

---

<div align="center">
  <sub>Designed & Developed with precision by <b>Arinara Network</b> • 2026</sub>
</div>
