<!-- --- Arinara Network (c) 2026 --- -->
<!-- Exclusive property of Arinara Network. -->
<!-- Unauthorized use, reproduction, distribution, or modification of this code, -->
<!-- in whole or in part, for any purpose, is strictly prohibited without prior -->
<!-- written consent from Arinara Network as sole legal owner of this codebase. -->

<div align="center">

<img src="Assets/Logos/FotaraLogo.png" alt="Fotara Logo" width="112" />

# Fotara - Intelligent Offline Academic Workspace & Note Engine

**A sovereign, privacy-preserving academic workbench for structured coursework, native LaTeX mathematics, vector sketching, and automated schedule tracking.**

[![Platform](https://img.shields.io/badge/Platform-Android%207.0%2B%20(API%2024%2B)-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20M3-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-Proprietary%20%26%20Educational-2A9D8F?style=flat-square)](#license)
[![Release](https://img.shields.io/github/v/release/Arinaranetwork/Fotara?style=flat-square&color=00B4D8&label=Release)](https://github.com/Arinaranetwork/Fotara/releases/latest)

<br />

<img src="Assets/Banners/FotaraBanner_2.1_2026-10-23.jpg" alt="Fotara Workspace Showcase" width="100%" />

<p align="center">
  <i>Fotara runtime environment on Android: Rich Markdown and KaTeX math editor, infinite vector Drawing Canvas, and adaptive multi-day schedule widget.</i>
</p>

[Downloads & Releases](https://github.com/Arinaranetwork/Fotara/releases) • [Product Overview](#product-overview) • [Core Features](#feature-highlights) • [Release Channels](#release-channels-matrix) • [Architecture](#tech-stack--architecture) • [Build Quickstart](#build--development-quickstart)

</div>

---

## Product Overview

Fotara is an autonomous, academic-grade productivity operating environment engineered for engineering students, researchers, and technical professionals. Designed from first principles to eliminate third-party cloud vulnerabilities and network reliance, Fotara operates strictly on an **offline-first architecture** with **local encryption**, hardware-backed biometric authentication, and **zero cloud dependency**.

Users organize dense intellectual workloads into isolated **modular spaces**, binding structured markdown notes, native mathematical proofs, unbounded multi-layer vector canvas diagrams, and deadline-aware timetable widgets into a single unified workspace.

---

## Feature Highlights

### Modular Spaces & Workspaces
- **Isolated Domain Vaults**: Segregate distinct semesters, academic disciplines, research initiatives, and laboratory projects with dedicated cryptographic access boundaries.
- **Hierarchical Organization**: Multi-tier folder and subfolder categorization with configurable grid densities, custom space palettes, and visual pinning for up to four high-priority references per folder.
- **Syllabus Performance Evaluator**: Integrated course weight analyzer calculating live grade standings and projected final examination targets.

### Native LaTeX Engine
- **Pure Compose Layout Tree**: High-performance mathematical typesetting without WebView overhead or JavaScript bridges.
- **Comprehensive Syntax**: Direct parsing and rendering of `$ ... $` inline formulas and `$$ ... $$` display block equations, matrices, integrals, summation operators, and Greek alphabets.
- **Constrained Markdown Table Editor**: Native GitHub-Flavored Markdown table editor with automated column balancing, zebra shading, and context-aware toolbar disabling block items inside cells.

### Infinite Drawing Canvas
- **Multi-Layer Vector Sketching**: Two-axis unbounded viewport with continuous pinch-to-zoom (0.2x to 5.0x), custom paper grids (blank, dot grid, graph grid, ruled), and independent layer opacity/visibility controls.
- **Geometric Shape & Curve Snapping**: Continuous draw-and-hold recognition snapping rough strokes into exact geometric primitives (rectangles, circles, ellipses, triangles, and arrows) with tactile haptic response.
- **Text Layers & Vector Duplication**: Movable, resizable, and rotatable vector typography annotations alongside multi-element duplicate and delete tools.
- **Multi-Sheet Project Files**: Project documents supporting up to 10 distinct sheets with isolated undo/redo command histories and combined multi-page export.

### Adaptive Class Schedule Widget
- **Jetpack Glance AppWidgets**: Live Home Screen widgets providing real-time daily course tracking, upcoming assignment alerts, and timetable glance overviews.
- **Dynamic 1-Day to 7-Day Matrix**: Contextual schedule tables adapting between compact single-session agenda views and comprehensive weekly academic overviews.
- **Reboot-Resilient Alarms**: Dual notification pipelines supporting unobtrusive reminder banners or high-priority ringing alarms resilient against Android Doze and device reboot events.

### Hierarchical `.fotara` Container Backup
- **Full-Fidelity Archival**: Complete offline backup container storing database states, workspace hierarchies, vector sketches, OCR indices, and media assets in an atomic ZIP archive.
- **Integrity Validation**: SHA-256 manifest verification and schema validation protecting archives against bit rot or partial corruption during transfer.
- **Granular Restoration**: Capability to restore entire application environments or selectively extract designated spaces and course folders.

---

## Release Channels Matrix

Fotara follows three distinct deployment channels to guarantee stability for everyday coursework while continuously advancing experimental capabilities.

| Channel | Identifier | Stability & Intent | Update Cadence | Distribution |
|:---|:---|:---|:---|:---|
| **Stable** | `v<major>.<moderate>.<minor>` (e.g. `v2.1.2`) | Battle-tested production builds intended for primary devices and critical academic schedules. | Monthly / Milestone releases | [GitHub Releases](https://github.com/Arinaranetwork/Fotara/releases) |
| **Beta** | `v<major>.<moderate>.<minor>-beta` (e.g. `v1.9.0-beta`) | Feature-complete milestone builds undergoing broad compatibility validation and field testing. | Bi-weekly releases | [GitHub Releases (Pre-Release)](https://github.com/Arinaranetwork/Fotara/releases) |
| **Alpha** | `v<major>.<moderate>.<minor>-alpha` (e.g. `v1.9.4-alpha`) | Active development branch containing early experimental tools, canvas extensions, and architectural updates. | Continuous preview builds | [GitHub Releases (Tags)](https://github.com/Arinaranetwork/Fotara/releases) |

### Installation Instructions
1. Download the preferred channel release APK from the [Releases](https://github.com/Arinaranetwork/Fotara/releases) page.
2. Transfer or download the APK directly to an Android device running Android 7.0 (API 24) or higher.
3. Open the APK file using the system package installer. If required, allow installation from unknown sources.
4. Complete the offline initial onboarding to set up your primary workspace.

---

## Tech Stack & Architecture

- **Language & Runtime**: Kotlin 2.0.21, Java 17 bytecode target.
- **UI Toolkit**: Jetpack Compose, Material Design 3, Compose Foundation Layout.
- **Local Storage & Indexing**: Room SQLite, raw SQLite FTS4 virtual tables, Android Keystore encryption.
- **Widgets**: Jetpack Glance Material3 AppWidget runtime.
- **Concurrency & Reactivity**: Kotlin Coroutines, StateFlow, SharedFlow.
- **On-Device Vision**: Google ML Kit Digital Optical Character Recognition (OCR).
- **Mathematical Layout**: Pure Compose KaTeX formula parser and layout tree.
- **Architecture Pattern**: Unidirectional Data Flow (UDF), Model-View-ViewModel (MVVM), offline-first repository pattern with manual dependency injection.

---

## Build & Development Quickstart

### Prerequisites
- Android Studio Ladybug (2024.2.1) or higher
- Android SDK Platform 36 (Android 16 Developer Preview / Android 15 Vanilla Ice Cream)
- JDK 17 (bundled with Android Studio or OpenJDK 17)

### Building from Source

Clone the repository:
```bash
git clone https://github.com/Arinaranetwork/Fotara.git
cd Fotara
```

Verify dynamic project versioning:
```bash
./gradlew printVersion
```

Compile and assemble the debug APK:
```bash
./gradlew assembleDebug
```

Execute unit test test suites:
```bash
./gradlew testDebugUnitTest
```

Output binaries are generated in `app/build/outputs/apk/debug/` and verified production releases are packaged in `/Output/Release/`.

---

## License

Fotara is proprietary software developed by and exclusive property of **Arinara Network**.
All rights reserved. Free for personal, educational, and individual academic study use.
Commercial exploitation, redistribution, reverse engineering, or unauthorized mirror hosting is strictly prohibited without prior written consent from Arinara Network.

<div align="center">
  <sub>Arinara Network Engineering • Production Workspace Ecosystem</sub>
</div>
