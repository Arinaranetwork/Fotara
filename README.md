<!-- --- Arinara Network (c) 2026 --- -->
<!-- Exclusive property of Arinara Network. -->
<!-- Unauthorized use, reproduction, distribution, or modification of this code, -->
<!-- in whole or in part, for any purpose, is strictly prohibited without prior -->
<!-- written consent from Arinara Network as sole legal owner of this codebase. -->

<div align="center">

![Fotara Banner](Assets/Banners/FotaraBanner_1.1_2026-09-24.jpg)

# Fotara
### Intelligent Local-First Coursework & Study Note Organization for Android

[![Platform](https://img.shields.io/badge/Platform-Android%207.0%2B%20(API%2024%2B)-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com)
[![Version](https://img.shields.io/badge/Release-v1.1.1%20Beta-00B4D8?style=flat-square)](https://github.com/Arinaranetwork/Fotara/releases)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20M3-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Distribution](https://img.shields.io/badge/License-Free%20for%20Personal%20Use-2A9D8F?style=flat-square)](#distribution--license)
[![Issues](https://img.shields.io/badge/Issues-Open%20%26%20Active-brightgreen?style=flat-square)](https://github.com/Arinaranetwork/Fotara/issues)

<p align="center">
  <b>Fotara</b> is a high-performance, private, on-device photo organization and visual study assistant built specifically for students, researchers, and lifelong learners. Effortlessly capture, structure, search, and review whiteboard notes, textbook excerpts, handwritten equations, and multi-page assignments — with zero cloud dependency.
</p>

[Download Latest APK (v1.1.1 Beta)](https://github.com/Arinaranetwork/Fotara/releases/latest) • [Features](#key-features) • [Installation](#installation) • [Architecture](#architecture) • [Issues & Feedback](https://github.com/Arinaranetwork/Fotara/issues)

</div>

---

## Overview

Traditional gallery applications dump educational photos alongside memes and personal photos, with no organizational structure, no awareness of academic deadlines, and no OCR indexing for handwritten notes. 

**Fotara** fixes this with a clean, local-first architecture engineered for high-density visual study notes:
- **Instant Optical Character Recognition (OCR)** runs on-device via Google ML Kit to index text without sending a single byte to external servers.
- **Full-Text Search (SQLite FTS4)** enables sub-second retrieval of handwritten and printed terminology directly from note images.
- **Academic Hierarchy**: Subject folders, subfolder tabs, and photo groups keep lectures, lab work, and multi-page homework assignments neatly organized.
- **Private & Resilient**: Device biometrics, PIN encryption, and a 30-day trash safety net ensure your study materials are secure and recoverable.

---

## Key Features

### 📂 Coursework Hierarchy & Folder Management
- **Subject Folders**: Color-coded subject folders with quick visual note counts and last-active indicators.
- **Subfolder Tabs**: Intuitive horizontal tab bar inside folders (e.g., *Lectures*, *Lab Notes*, *Assignments*) featuring inline long-press rename, move-to-trash confirmation, and multi-select bulk deletion.
- **Adaptive Density**: Live grid density preferences (2, 3, or 4 columns) with responsive additive scaling on foldables, landscape, and large-screen tablets (up to 7 columns).

### 📑 Photo Groups (v1.1.1)
- **Multi-Page Bundling**: Combine 2 or more related note photos into a single collapsed grid card with cover thumbnail, member count badge, and color label.
- **Dedicated Fullscreen Group Screen**: Tapping a group card navigates to a dedicated grid view of all member photos with order-added sorting, [+] photo addition, and group overflow management (Rename, Ungroup, Delete to Trash, Color label).
- **Scoped Review Slider**: Browse group photos sequentially with zoom-gated horizontal swiping, 90° manual rotation, non-destructive cropping, and single-member auto-dissolve upon note removal.

### 🔍 On-Device OCR & Sub-Second Search
- **100% Offline Indexing**: On-device text recognition pipeline tokenizes handwritten equations, whiteboard diagrams, and printed textbooks automatically upon capture or import.
- **Active Search Dock**: Keyboard-docked search bar with real-time query filtering, persistent recent searches, date-range filters (Today, This Week, This Month), and color label filters.
- **Waypoint Navigation & Highlight**: Tapping any search result automatically opens the source folder, selects the subfolder tab, smooth-scrolls to the target note, and displays a temporary 3-second non-blocking dimmed highlight. For grouped notes, it highlights the group card waypoint before smoothly transitioning into the group screen.

### 📝 Study Notes & Non-Destructive Image Tools
- **Custom Note Annotations**: Attach personal study annotations, formula derivations, and summary bullet points directly to any photo note with full FTS4 search indexing.
- **Interactive Inspector**: Fullscreen viewer with double-tap zoom (1.0x to 3.0x), zoom-gated swipe navigation, 90° clockwise rotation, and 4-corner perspective cropping without generating duplicate files.

### 🔒 Privacy, Security & Data Safety
- **Folder Privacy Lock**: Protect sensitive coursework behind a 4-digit PIN or Android BiometricPrompt (fingerprint/face recognition), featuring generic card previews with thumbnails concealed and device credential fallback.
- **Data Safety Trash**: 30-day soft-delete retention countdown with individual and bulk permanent purging, orphan parent folder restoration prompts, and startup auto-cleanup.
- **Full Offline Backup & PDF Export**: One-tap JSON database export and import, paired with high-quality multi-page PDF compilation with group title section headers.

---

## Application Status

| Metric | Status |
|---|---|
| **Current Build** | `v1.1.1 Beta` (Build Code: `4`) |
| **Release Channel** | Beta |
| **Supported Devices** | Android 7.0 (API 24) through Android 15 (API 36) |
| **Issue Tracker** | **Active & Open** — Bug reports and feature suggestions are welcome via [GitHub Issues](https://github.com/Arinaranetwork/Fotara/issues). |
| **Distribution Model** | **Free for Personal and Educational Use** |

---

## Distribution & License

Fotara is distributed **free of charge for personal, non-commercial, and educational use**. 

- **Personal Use**: You are free to download, install, and use Fotara on any compatible personal Android device for coursework, research, and individual study.
- **Intellectual Property**: Fotara is the exclusive property of **Arinara Network**. All rights reserved.
- **Restrictions**: Commercial redistribution, sublicensing, repackaging, reverse engineering for resale, or unauthorized mirror hosting is strictly prohibited without prior written consent from Arinara Network.

---

## Installation

1. Navigate to the **[Latest Release](https://github.com/Arinaranetwork/Fotara/releases/latest)** page.
2. Under **Assets**, download `Fotara_1.1.1_Beta.apk`.
3. On your Android device, open the downloaded `.apk` file.
4. If prompted, grant permission to *Install from Unknown Sources* for your browser or file manager.
5. Launch Fotara, complete the guided first-run permission onboarding, and choose your starter subject folders!

---

## Architecture & Tech Stack

```
Fotara/
├── app/src/main/java/com/arinara/fotara/
│   ├── data/
│   │   ├── db/          # SQLite FTS4 virtual table helper & migrations
│   │   ├── model/       # Domain models (Folder, Subfolder, Photo, PhotoGroup, TagColor)
│   │   └── repository/  # Reactive repositories with Kotlin StateFlow & IO coroutines
│   ├── ocr/             # Google ML Kit on-device text recognition pipeline
│   ├── export/          # PDF multi-page document compilation & share provider
│   ├── reminder/        # AlarmManager scheduling & NotificationManager deep-links
│   ├── theme/           # Premium dark-first palette, typography, and shape tokens
│   └── ui/              # Jetpack Compose UI architecture
│       ├── common/      # ZoomablePhotoViewport, ActiveSearchBar, FloatingDock
│       ├── folder/      # FolderDetailScreen, subfolder tabs, grid cards, multi-select
│       ├── group/       # Dedicated GroupDetailScreen & GroupDetailViewModel (v1.1.1)
│       ├── home/        # Home screen, folder grid, and starter subject generation
│       ├── onboarding/  # Guided pre-permission and privacy explainer
│       ├── settings/    # Display density, OCR settings, storage breakdown, backup
│       ├── trash/       # 30-day soft-delete recycle bin
│       └── viewer/      # Fullscreen photo inspector, custom notes editor, 90° rotate
├── Assets/              # Project release banners, logos, and typography
├── Docs/                # Product Codex, interaction specifications, and progress
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
