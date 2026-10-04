# <div align="center">Fotara</div>

<div align="center">
  <img src="https://raw.githubusercontent.com/Arinaranetwork/Fotara/main/Assets/Logos/FotaraLogo.png" alt="Fotara Logo" width="120" onerror="this.style.display='none'"/>
  <br/>
  <h3>The Ultimate Offline-First Study & Coursework Hub for Android</h3>
  <p>Seamlessly organize photo notes, assignments, documents, schedules, and infinite freeform canvas drawings — with zero mandatory cloud lock-in.</p>
</div>

<div align="center">

[![Platform](https://img.shields.io/badge/Platform-Android%207.0%2B%20(API%2024%E2%80%9336)-blue.svg)](#)
[![Latest Release](https://img.shields.io/badge/Release-v1.5.10%20Beta-success.svg)](https://github.com/Arinaranetwork/Fotara/releases/tag/Fotara_1.5.10_Beta)
[![Download APK](https://img.shields.io/badge/Download-Fotara_1.5.10_Beta.apk-blue?logo=android)](https://github.com/Arinaranetwork/Fotara/releases/download/Fotara_1.5.10_Beta/Fotara_1.5.10_Beta.apk)
[![License](https://img.shields.io/badge/License-Proprietary%20%2F%20Educational-lightgrey.svg)](LICENSE)
[![Status](https://img.shields.io/badge/Channel-Beta-orange.svg)](#)

</div>

---

## Overview

**Fotara** is an advanced, offline-first study companion tailored for students, researchers, and self-directed learners. Designed to replace fragmented note-taking workflows, Fotara brings photos, lecture slides, textbooks, assignments, native markdown notes, and vector drawings together into a unified, privacy-respecting hub.

- **Offline-First Storage**: Your notes, documents, and search indexes remain strictly on your device.
- **Unlimited Vector Canvas**: Infinite drawing space powered by hardware acceleration with pressure-sensitive strokes, layers, image attachments, and PNG export.
- **Universal Study Scheduling**: Attach time and deadline reminders to any note type with dual alert modes (unobtrusive notifications or persistent alarm alerts).
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
- **Direct GPU Vector Engine**: Hardware-accelerated direct rendering delivering 120 FPS drawing and zooming without tile cutouts or missing areas.
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

## What's New in v1.5.10 Beta

- **Bulletproof Profile Picture & Banner PNG Crop**: Resolved failures when cropping and saving PNG profile pictures and banners by migrating crop extraction directly into memory from the loaded preview bitmap using normalized coordinates. Eliminates native Skia region decoder failures, closed input streams, and boundary calculation errors across all Android versions.
- **Safe Universal URI Stream Handling**: Hardened image stream resolution to transparently support both `file://` and `content://` schemes across all Android API levels without `SecurityException` or stream reset issues.
- **Clean Profile Sheet Navigation**: Automatically dismisses the profile action bottom sheet upon selecting an action, ensuring the user immediately returns to the updated profile screen after saving.
- **Lossless PNG Profile Picture & Banner Support**: Enforced true PNG format preservation with full 32-bit ARGB_8888 alpha transparency for custom avatars and banners, resolving encoder failures on transparent PNG images.
- **Expanded & Formatted In-App Update Modal**: Upgraded update popup modal with an expanded vertical layout (up to 440dp height) and native Rich Markdown rendering, cleanly displaying headings (H1, H2, H3), bold text, bullet lists, blockquotes, and release note formatting.
- **LinkIt Converging Cluster Glow**: For linked groups of 3 or 4 cards arranged in a 2x2 grid cluster (including L-shaped triads), corner glows now converge toward the shared central intersection point with cardinal side glows suppressed within qualifying blocks. Pairwise cardinal edge glows are preserved for isolated pairs and straight line layouts across grid densities 2, 3, and 4.
- **Updated What's New Dashboard**: In-app "What's New" screen now reflects the 1.5.10 release notes and feature highlights with fallback offline support.
- **Aspect-Locked Fullscreen Crop Editor**: Fullscreen interactive crop editor supporting 1:1 circular guide for profile pictures and exact layout-matched aspect ratio for banners with four accessible 44dp corner handles, two-finger pinch scaling, and drag panning with boundary clamping.

---

## Application Status

| Metric | Details |
|---|---|
| **Current Version** | `v1.5.10 Beta` (Version Code: `24`) |
| **Release Channel** | Beta |
| **Supported Devices** | Android 7.0 (API 24) through Android 16 (API 36) |
| **Architecture** | Offline-First, On-Device Processing |
| **Distribution Model** | **Free for Personal and Educational Use** |
| **APK Checksum (SHA256)** | `ED98E488C665C8D3F0D1C828A180432BBFCBE7F9F63AEEEC0352A4F7E2C9D731` |

---

## Installation

1. Go to the **[Fotara 1.5.10 Beta Release](https://github.com/Arinaranetwork/Fotara/releases/tag/Fotara_1.5.10_Beta)** page.
2. Download **`Fotara_1.5.10_Beta.apk`** (or click [Direct Download](https://github.com/Arinaranetwork/Fotara/releases/download/Fotara_1.5.10_Beta/Fotara_1.5.10_Beta.apk)).
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
