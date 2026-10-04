<!-- --- Arinara Network (c) 2026 ---
Exclusive property of Arinara Network.
Unauthorized use, reproduction, distribution, or modification of this code,
in whole or in part, for any purpose, is strictly prohibited without prior
written consent from Arinara Network as sole legal owner of this codebase. -->

<div align="center">
  <img src="Assets/Banners/FotaraBanner_1.5_2026-09-30.jpg" alt="Fotara 1.5 Banner" width="100%" />

  # Fotara
  ### Modern On-Device Visual Coursework & Visual Note Architecture for Android

  [![Platform](https://img.shields.io/badge/Platform-Android_7.0+_(API_24+)--36-3DDC84?style=flat-square&logo=android&logoColor=white)](https://android.com)
  [![Release](https://img.shields.io/badge/Release-v1.5.11_Beta-blue?style=flat-square)](https://github.com/Arinaranetwork/Fotara/releases/tag/Fotara_1.5.11_Beta)
  [![License](https://img.shields.io/badge/License-Proprietary_%26_Educational-F77F00?style=flat-square)](LICENSE)
  [![Status](https://img.shields.io/badge/Architecture-100%25_On--Device-2A9D8F?style=flat-square)](https://github.com/Arinaranetwork/Fotara)
  [![Download APK](https://img.shields.io/badge/Download_APK-v1.5.11_Beta-0077B6?style=flat-square&logo=android)](https://github.com/Arinaranetwork/Fotara/releases/download/Fotara_1.5.11_Beta/Fotara_1.5.11_Beta.apk)

  <p align="center">
    <b>Fotara</b> is a high-performance, privacy-first mobile study companion designed for students, educators, and researchers. Organize lecture photos, multi-page PDF textbooks, Microsoft Word DOCX handouts, rich markdown notes, and vector drawings on an unlimited canvas — with real-time on-device OCR, multi-criteria date filtering, and customizable notification and alarm reminder schedules.
  </p>
</div>

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

## What's New in v1.5.11 Beta

- **Resilient Multi-Tier Image Decoder**: Resolved split-second crop screen dismissal and image decode failures when selecting PNG or banner images. Implements a multi-tiered decoding pipeline prioritizing hardware-accelerated `ImageDecoder` on Android 9+ (API 28+), direct memory-mapped file decoding for staged cache files, seekable `FileDescriptor` streaming, and single-pass in-memory byte buffer decoding to eliminate unbuffered stream mark/reset failures and content provider permission denials.
- **Non-Blocking Asynchronous Photo Picking**: Refactored activity result callbacks in Settings and Profile to pass image URIs directly to background coroutine decoders, preventing main-thread I/O bottlenecks and strict-mode violations when importing large photos from external storage or cloud providers.
- **Bulletproof Profile Picture & Banner PNG Crop**: Resolved failures when cropping and saving PNG profile pictures and banners by migrating crop extraction directly into memory from the loaded preview bitmap using normalized coordinates. Eliminates native Skia region decoder failures, closed input streams, and boundary calculation errors across all Android versions.
- **Lossless PNG Profile Picture & Banner Support**: Enforced true PNG format preservation with full 32-bit ARGB_8888 alpha transparency for custom avatars and banners, resolving encoder failures on transparent PNG images.
- **Clean Profile Sheet Navigation**: Automatically dismisses the profile action bottom sheet upon selecting an action, ensuring the user immediately returns to the updated profile screen after saving.
- **Expanded & Formatted In-App Update Modal**: Upgraded update popup modal with an expanded vertical layout (up to 440dp height) and native Rich Markdown rendering, cleanly displaying headings (H1, H2, H3), bold text, bullet lists, blockquotes, and release note formatting.
- **LinkIt Converging Cluster Glow**: For linked groups of 3 or 4 cards arranged in a 2x2 grid cluster (including L-shaped triads), corner glows now converge toward the shared central intersection point with cardinal side glows suppressed within qualifying blocks. Pairwise cardinal edge glows are preserved for isolated pairs and straight line layouts across grid densities 2, 3, and 4.
- **Updated What's New Dashboard**: In-app "What's New" screen now reflects the 1.5.11 release notes and feature highlights with fallback offline support.
- **Aspect-Locked Fullscreen Crop Editor**: Fullscreen interactive crop editor supporting 1:1 circular guide for profile pictures and exact layout-matched aspect ratio for banners with four accessible 44dp corner handles, two-finger pinch scaling, and drag panning with boundary clamping.

---

## Application Status

| Metric | Details |
|---|---|
| **Current Version** | `v1.5.11 Beta` (Version Code: `25`) |
| **Release Channel** | Beta |
| **Supported Devices** | Android 7.0 (API 24) through Android 16 (API 36) |
| **Architecture** | Offline-First, On-Device Processing |
| **Distribution Model** | **Free for Personal and Educational Use** |
| **APK Checksum (SHA256)** | `3D081059096EF92E634E08107D74A0E7219A9CAE170D7F8C37EFEB31556186EF` |

---

## Installation

1. Go to the **[Fotara 1.5.11 Beta Release](https://github.com/Arinaranetwork/Fotara/releases/tag/Fotara_1.5.11_Beta)** page.
2. Download **`Fotara_1.5.11_Beta.apk`** (or click [Direct Download](https://github.com/Arinaranetwork/Fotara/releases/download/Fotara_1.5.11_Beta/Fotara_1.5.11_Beta.apk)).
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
