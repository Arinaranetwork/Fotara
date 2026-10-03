<!-- --- Arinara Network (c) 2026 ---
Exclusive property of Arinara Network.
Unauthorized use, reproduction, distribution, or modification of this code,
in whole or in part, for any purpose, is strictly prohibited without prior
written consent from Arinara Network as sole legal owner of this codebase. -->

<div align="center">

<img src="https://github.com/Arinaranetwork/Fotara/releases/download/Fotara_1.5.3_Beta/FotaraBanner_1.5_2026-09-30.jpg" alt="Fotara 1.5 Banner" width="100%" />

# Fotara
### Intelligent Local-First Coursework & Study Note Organization for Android

[![Platform](https://img.shields.io/badge/Platform-Android%207.0%2B%20(API%2024%2B)-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com)
[![Release](https://img.shields.io/badge/Release-v1.5.3%20Beta-00B4D8?style=flat-square)](https://github.com/Arinaranetwork/Fotara/releases/tag/Fotara_1.5.3_Beta)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![UI](https://img.shields.io/badge/UI-Jetpack%20Compose%20M3-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-Proprietary%20%26%20Educational-2A9D8F?style=flat-square)](#distribution--license)
[![Issues](https://img.shields.io/badge/Issues-Open%20%26%20Active-brightgreen?style=flat-square)](https://github.com/Arinaranetwork/Fotara/issues)

<p align="center">
  <b>Fotara</b> is a high-performance, private, on-device study organization ecosystem built for students, researchers, and lifelong learners. Effortlessly structure, search, schedule, and review whiteboard captures, textbook excerpts, handwritten equations, rich text notes, multi-page PDFs, Word documents, and infinite drawing canvases with zero cloud lock-in.
</p>

[Download Latest APK (v1.5.3 Beta)](https://github.com/Arinaranetwork/Fotara/releases/download/Fotara_1.5.3_Beta/Fotara_1.5.3_Beta.apk) • [Features](#key-features) • [Installation](#installation) • [What's New in v1.5.3](#whats-new-in-v153-beta) • [License](#distribution--license) • [Issues & Feedback](https://github.com/Arinaranetwork/Fotara/issues)

</div>

---

## Overview

Traditional mobile gallery apps dump educational material alongside casual snapshots without structure, academic deadline awareness, or handwritten text recognition.

**Fotara** delivers a unified, local-first study ecosystem engineered for high-density academic workflows:
- **Unlimited Drawing Canvas (Alpha)**: Infinite 2D vector drawing canvas with smooth pan, pinch-to-zoom, pressure-sensitive pen, highlighter, eraser, multi-layer management, image placement, and high-resolution PNG export.
- **Universal Note Scheduling**: Attach reminder notifications or ringing alarms to any note type (Photos, Photo Groups, PDFs, Word DOCX, Text Notes, and Canvas) with full Doze and device reboot resilience.
- **Dedicated Glance "Today" Widget**: Rectangular home screen widget displaying coursework due today, newly captured study notes, and upcoming scheduled alerts with instant deep-linking.
- **Share to Fotara**: Receive external images, PDFs, Word documents, Markdown files, or plain text notes directly into Fotara with an intuitive destination placement screen.
- **Native Rich-Text Notes**: Write and format notes with a live Markdown-backed editor, formatting toolbar (bold, italic, strikethrough, headings, lists, quotes, code, links), and instant debounced autosave.
- **High-Fidelity Document Viewer**: Direct in-app rendering for Microsoft Word (.docx) and virtualized PDFs with on-demand viewport rasterization, smooth pinch-to-zoom, two-axis panning, and a dedicated photo-style Page Viewer.
- **On-Device Optical Character Recognition (OCR)**: Google ML Kit extracts text from photo notes locally and populates an SQLite FTS4 full-text search index for sub-second retrieval.
- **Private & Safe**: Device biometrics, PIN encryption, and a 30-day soft-delete trash retention policy keep study materials secure and recoverable.

---

## Key Features

### Coursework Hierarchy & Folder Management
- **Subject Folders**: Color-coded subject folders with live note counts and last-active timestamps.
- **Subfolder Tabs**: Clean horizontal tab navigation inside folders (e.g., *Lectures*, *Lab Notes*, *Assignments*) featuring inline long-press rename, trash confirmation, and multi-select bulk operations.
- **Adaptive Grid Density**: Live grid density preferences (2, 3, or 4 columns) with responsive scaling on foldables, landscape, and large-screen tablets (up to 7 columns).

### Unlimited Drawing Canvas (Alpha)
- **Infinite 2D Viewport**: Freeform two-axis pan and continuous pinch-to-zoom (0.2x to 5.0x) with single-tap zoom reset and fit-to-content.
- **Vector Stroke Engine**: Smooth Bézier curves with pressure sensitivity across ballpoint, chisel, and brush nib profiles.
- **Layer & Asset Management**: Multi-layer organization with lock, hide, opacity slider, layer reordering, and external image attachments.
- **Background Grids**: Switch seamlessly between blank, graph grid, dot grid, and ruled college-lined paper backgrounds.
- **High-Resolution Export**: Render visible canvas contents to full-quality PNG images shareable via Android system sheet.

### Universal Note Scheduling & Glance Widget
- **Ubiquitous Reminders**: Attach reminder schedules to photos, groups, documents, text notes, and canvas drawings.
- **Dual Alert Channels**: Choose between standard unobtrusive notifications or persistent ringing alarm alerts with snooze (+10 min) and dismiss options.
- **Glance "Today" Home Widget**: Real-time study dashboard showcasing items due today, newly added notes, and scheduled alert countdowns.

### Share to Fotara Destination Placement
- **Cross-App Integration**: Share multiple items simultaneously from browsers, chat apps, or file managers.
- **Streaming Sandbox Staging**: Private app cache staging with background thumbnailing and process death recovery.
- **Placement Interface**: Browse target folders and subfolders with live item counts, collapsible staged preview tray, and single-tap "Place Here" confirmation.

### High-Fidelity Documents & Rich Text
- **In-Viewer PDF Zoom & Photo-Style Page Viewer**: Dedicated fullscreen page viewer with pinch-to-zoom up to 4.0x, two-axis pan, double-tap zoom, zoom-gated swipe navigation, and recognized OCR text drawer, alongside viewport-level list zoom with free two-axis pan.
- **Intelligent Split to Images**: Convert PDF documents into white-canvas photos, automatically grouping documents with 5 or more pages into a Photo Group.
- **Native Markdown Notes**: Full markdown formatting toolbar with word count, character count, find/replace, and dedicated raw MD / TXT sharing.

### On-Device OCR & Sub-Second Search
- **100% Offline Indexing**: ML Kit text recognition extracts text from photos, documents, and notes upon capture or import.
- **Search Date Filters**: Filter notes instantly by date added with quick chips (Today, Yesterday, This week, This month, This year) and custom date pickers.
- **Waypoint Navigation**: Search results automatically open the source folder, select the matching subfolder, smooth-scroll to the card, and highlight it with an exposure flash.

### Privacy, Security & Data Safety
- **Offline-First Architecture**: All note management, OCR, search indexing, document rendering, and exports operate entirely on-device without mandatory network connectivity.
- **Folder Privacy Lock**: Protect confidential notes behind a 4-digit PIN or Android BiometricPrompt (fingerprint or face authentication).
- **Recycle Bin**: 30-day soft-delete retention window with individual and bulk permanent purging.

---

## What's New in v1.5.3 Beta

- **Section-Oriented Settings Screen**: Reorganized settings into 6 main section rounded cards on root (*General*, *Appearance*, *OCR & Recognition*, *Notifications & Deadlines*, *Storage & Data Management*, *About & Legal*) with clean, un-carded detail lists inside each opened category.
- **Full-Height Home Viewport & Navigation Clearance**: Eliminated artificial vertical constraints on the home dashboard, allowing folder grid content to scroll smoothly using the full screen height while staying completely unobstructed by system navigation buttons.
- **Photo Viewer Responsive Header**: Reorganized inspector toolbar into a clean title and action row paired with a full-width horizontal status row, ensuring helper text and schedule badges never wrap vertically on narrow phone widths.
- **Search Result Auto-Scroll & Exposure Highlight**: Tapping any search result (Photos, Groups, PDF documents, Word DOCX, Text Notes, Canvas) navigates directly to the parent folder and active subfolder, auto-scrolls until the item is visible, and applies a subtle 2-second brightness/exposure flash before fading out cleanly.

---

## Application Status

| Metric | Details |
|---|---|
| **Current Version** | `v1.5.3 Beta` (Version Code: `17`) |
| **Release Channel** | Beta |
| **Supported Devices** | Android 7.0 (API 24) through Android 16 (API 36) |
| **Architecture** | Offline-First, On-Device Processing |
| **Distribution Model** | **Free for Personal and Educational Use** |
| **APK Checksum (SHA256)** | `33a3be61f7d271534421cead1a24d99222011858b4cefb09b71c709d58bc3d26` |

---

## Installation

1. Go to the **[Fotara 1.5.3 Beta Release](https://github.com/Arinaranetwork/Fotara/releases/tag/Fotara_1.5.3_Beta)** page.
2. Download **`Fotara_1.5.3_Beta.apk`** (or click [Direct Download](https://github.com/Arinaranetwork/Fotara/releases/download/Fotara_1.5.3_Beta/Fotara_1.5.3_Beta.apk)).
3. On your Android device, tap the downloaded APK file.
4. If prompted, grant permission to *Install from Unknown Sources* for your browser or file manager.
5. Open Fotara, complete the guided first-run onboarding, and organize your study notes!

---

## Distribution & License

Fotara is distributed under the **Arinara Network Proprietary & Educational Software License**.

- **Personal & Educational Use**: You are free to download, install, and use Fotara on any compatible personal Android device for coursework, research, and individual study.
- **Intellectual Property**: Fotara, its assets, visual designs, and compiled binaries are the exclusive property of **Arinara Network**. All rights reserved.
- **Restrictions**: Commercial redistribution, sublicensing, repackaging, reverse engineering for resale, modification, or unauthorized mirror hosting is strictly prohibited without prior written consent from Arinara Network.

See the full [LICENSE](LICENSE) file for complete terms.

---

## Feedback & Issues

Have an idea for a feature or encountered a bug? We welcome your input:
- [Submit a Bug Report](https://github.com/Arinaranetwork/Fotara/issues/new)
- [Request a Feature](https://github.com/Arinaranetwork/Fotara/issues/new)

---

<div align="center">
  <sub>Designed & Developed with precision by <b>Arinara Network</b> • 2026</sub>
</div>
