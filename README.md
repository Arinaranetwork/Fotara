<!-- --- Arinara Network (c) 2026 --- -->
<!-- Exclusive property of Arinara Network. -->
<!-- Unauthorized use, reproduction, distribution, or modification of this code, -->
<!-- in whole or in part, for any purpose, is strictly prohibited without prior -->
<!-- written consent from Arinara Network as sole legal owner of this codebase. -->

<div align="center">

<img src="Assets/Logos/FotaraLogo.png" alt="Fotara App Icon" width="112" />

# Fotara — Built for Your Everyday Study & Work

**An intelligent, offline-first Android workspace uniting structured subject folders, a rich STEM Note Studio, an infinite vector drawing canvas, local Wi-Fi collaboration, smart document scanning, and adaptive class schedules.**

[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com)
[![UI](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Architecture](https://img.shields.io/badge/Architecture-Offline--First%20%26%20Encrypted-7A5AF8?style=flat-square)](#privacy--security-by-design)
[![License](https://img.shields.io/badge/License-Proprietary%20%26%20Educational-2A9D8F?style=flat-square)](#license)
[![Releases](https://img.shields.io/github/v/release/Arinaranetwork/Fotara?style=flat-square&color=00B4D8&label=Releases)](https://github.com/Arinaranetwork/Fotara/releases/latest)

<br />

<img src="Assets/Banners/Fotara%20Github%20Main%20Promotional%20Banner.jpg" alt="Fotara Promotional Banner" width="100%" />

<p align="center">
  <i>Fotara on Android — Infinite vector sketching, structured subject spaces, rich STEM note studio, and live class timetable tracking.</i>
</p>

[Download Releases](https://github.com/Arinaranetwork/Fotara/releases) • [Overview](#overview) • [Core Capabilities](#core-capabilities) • [Privacy & Security](#privacy--security-by-design) • [Release Channels](#release-channels) • [Getting Started](#getting-started)

</div>

---

## Overview

**Fotara** is an all-in-one workspace crafted for students, educators, researchers, and everyday creators who want complete ownership over their notes and coursework. Built around an **offline-first architecture** with **hardware-backed local encryption**, Fotara keeps your lectures, sketches, documents, and schedules fast, organized, and private—with zero mandatory cloud dependency.

Instead of scattering lecture photos across your camera roll, equations across separate editors, and schedules across different apps, Fotara brings everything together into clean **Spaces** and **Subject Folders** designed in the signature **Electric Slate** dark theme.

---

## Core Capabilities

### 🗂️ Spaces, Folders & Effortless Organization
- **Dedicated Workspaces**: Separate semesters, projects, personal study, and research into clean, independent spaces.
- **Subject Folders & Subfolders**: Organize materials by course or topic with color-coded folder cards, custom note labels, and pinned favorites.
- **Dynamic `!` Title Expressions**: Automate note and folder naming using live inline tokens—such as dates, times, sequence counters, and folder context—complete with an interactive autocomplete popup and literal `no!` escape syntax.
- **Home Screen Shortcuts**: Pin frequently used folders and individual notes directly to your Android home screen for instant, one-tap access.

### 📝 Hybrid Note Studio (Markdown, Math, Code, Tables, Charts & Voice)
- **Segmented Rich Markdown Editor**: Write effortlessly with inline top-bar title editing, live formatting controls, checklists, and instant preview.
- **Native LaTeX Mathematics**: Render inline (`$...$`) and display (`$$...$$`) equations, matrices, integrals, and Greek notation with authentic mathematical serif typography—powered natively in Jetpack Compose with zero WebView overhead.
- **Syntax-Highlighted Code Blocks**: Embed clean, dark-slate code cards with multi-language syntax highlighting, one-tap copy, and line-wrap toggles.
- **Interactive Typed Tables**: Build structured tables inside notes with typed column headers (`Text`, `Number`, `Checkbox`, `Date`), instant sorting, filtering, search, and rapid row/column expansion.
- **Built-In Charts & CSV Importer**: Visualize data directly inside notes with **Bar/Column**, **Line/Area** (including linear regression trendlines), **Pie/Donut**, **Radar/Spider**, and **Scatter** plots—or import raw CSV datasets with automatic delimiter and header detection.
- **Voice Dictation & Inline Audio Clips**: Capture lectures hands-free with continuous speech-to-text dictation right into your cursor, or record live-waveform audio clips embedded directly inside your notes with variable-speed playback.

### 🎨 Infinite Vector Canvas & Expressive Sketching
- **Multi-Layer Infinite Viewport**: Sketch, diagram, and brainstorm on an unbounded canvas with customizable paper grids (dot, graph, ruled, or blank), multi-sheet project support, and granular layer opacity and stacking controls.
- **Expressive Brush Engine**: Draw with realistic textured brushes—including **Ballpoint**, **Fountain / Calligraphy**, **Marker / Chisel**, **Pencil / Sketch**, and **Dashed Diagram Pens**—alongside a gap-tolerant **Flood-Fill Bucket**, **Eyedropper Color Loupe**, and precision **Stroke-Carving Eraser**.
- **Hold-to-Snap Smart Shapes**: Pause briefly at the end of any freehand stroke to automatically snap lines, arrows, rectangles, circles, ellipses, and triangles into crisp geometric vectors.

### 🤝 Local Wi-Fi Collaborative Study Rooms
- **Peer-to-Peer Canvas Sync**: Host or join live study sessions over the same Wi-Fi network using a custom room code—requiring no external servers or internet connection.
- **Live Cursors, Strokes & Voice Chat**: Sketch together in real time with synchronized strokes, images, text annotations, live peer cursors, and built-in low-latency voice chat.
- **Host Moderation & Access Controls**: Manage room privacy policies, promote admins, mute participants, or toggle drawing and sheet-creation permissions on a global or per-member basis.

### 📸 Rapid Document Scanner, PDF Studio & On-Device OCR
- **Rapid Multi-Shot Camera & Perspective Dewarp**: Capture whiteboards, handouts, and textbook pages rapidly with a live filmstrip tray, 4-corner perspective correction loupe, and readability filters.
- **In-Situ PDF & Document Studio**: Read and annotate PDFs directly, reorder or manage pages, attach page-specific audio recordings, and export flattened vector PDFs.
- **Instant Offline Full-Text Search**: Every handwritten scan, printed page, and note is indexed automatically on-device via Optical Character Recognition (OCR) and full-text search so you can find any keyword in milliseconds.

### 📅 Adaptive Timetable Widget & Smart Class Alarms
- **Live Home Screen Timetable**: Keep track of your daily classes, room numbers, and day-specific notes with a resizable Android home screen widget.
- **Flexible Schedule Overrides**: Toggle one-time or recurring weekly day notes and temporarily pause schedules for holidays or breaks.
- **Floating Subject Countdown & Alarms**: Receive reboot-resilient class reminders and a smooth, top-down floating countdown pill before your next subject begins—with easy one-tap, swipe, or volume-key dismissal.

---

## Privacy & Security by Design

- **100% Offline-First Operation**: All notes, vector canvases, OCR indexes, voice recordings, and schedules live locally on your device.
- **Hardware-Backed Private Space & Folder Locks**: Protect sensitive spaces and locked folders with `AndroidKeyStore` AES-GCM encryption at rest and salted PBKDF2 passcodes.
- **Flexible Authentication & Recovery**: Choose between biometric unlock or strict PIN-only mode, configure optional security question recovery, and rely on automatic lockout protection.
- **Full-Fidelity `.fotara` Backups**: Export and restore your entire workspace—or selected spaces and folders—using self-contained, integrity-verified `.fotara` archive bundles.

---

## Release Channels

Fotara publishes builds across three release tracks so you can choose between rock-solid daily reliability and early access to new tools:

| Channel | Purpose | Availability |
|:---|:---|:---|
| **Stable** | Production-ready releases verified for everyday academic and professional workflows. | [GitHub Releases](https://github.com/Arinaranetwork/Fotara/releases) |
| **Beta** | Feature-complete milestone builds open for broader testing and feedback before promotion to Stable. | [GitHub Releases (Pre-Release)](https://github.com/Arinaranetwork/Fotara/releases) |
| **Alpha** | Early preview builds showcasing experimental studio tools, canvas enhancements, and upcoming features. | [GitHub Releases](https://github.com/Arinaranetwork/Fotara/releases) |

---

## Getting Started

1. Visit the [**Releases**](https://github.com/Arinaranetwork/Fotara/releases) page and download the APK from your preferred release channel.
2. Open the downloaded APK on your Android device and allow installation if prompted by your system.
3. Launch **Fotara** and complete the quick offline setup to create your first Space and Subject Folders.
4. Updates can be downloaded and resumed directly inside the app via **Settings → Check for Updates**.

---

## License

Fotara is proprietary software developed by and exclusive property of **Arinara Network**.  
All rights reserved. Free for personal, educational, and individual study use.  
Commercial redistribution, reverse engineering, or unauthorized mirror hosting is strictly prohibited without prior written consent from Arinara Network.

<div align="center">
  <sub>Crafted by <b>Arinara Network</b> • Electric Slate Productivity Ecosystem</sub>
</div>
