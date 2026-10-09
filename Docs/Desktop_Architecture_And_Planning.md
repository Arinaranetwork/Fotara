// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

# Fotara Desktop: Architecture, Technical Blueprint & Planning

## 1. Executive Summary & Vision

Fotara Desktop is the dedicated widescreen workstation companion to the Fotara Android mobile application. 

While the mobile application specializes in high-speed classroom capture (lecture whiteboard photos, verbatim audio annotations, and quick document scans), **Fotara Desktop** provides an expansive study studio for homework, thesis writing, mathematical typesetting, and deep exam preparation on Windows PCs and laptops.

### Core Product Tenets:
- **Web-Based & Lightweight**: Built with standard web technologies (React 19, TypeScript, Vite) ensuring zero bloat, instant startup (<300ms), and minimal RAM consumption (~35–50 MB).
- **Wrapped into Native `.exe`**: Delivered as a standalone, zero-dependency Windows executable (`Fotara_2.0.0_Beta_Windows_x64.exe`) via Tauri v2 / WebView2, keeping the final binary size under 10 MB.
- **100% Local-First & Private**: Operates entirely offline with zero mandatory cloud accounts. All data lives on the student's local machine.
- **Cross-Platform Compatibility**: Native support for the `.fotara` Coursework Interchange Bundle (Phase 48) allowing effortless phone $\leftrightarrow$ PC import and export.

---

## 2. Desktop Workstation Layout Architecture (Dashboard & Study Studio)

Widescreen desktop monitors (1080p, 1440p, 4K) provide an expansive academic workspace. Fotara Desktop implements a canonical 2-tier workstation layout matching the Arinara visual reference:

```
┌─────────────────────────────────────────────────────────────────────────────────────────────────┐
│ [Fotara]          Home                                      [ 🔍 Search notes... ] [+] [⋮]  — ▢ ✕│
│                   Your notes, organized                                                         │
│ [🏠 Home]         📁 Folders                                       6 folders [ ⊞ Grid ] [ ☰ ]   │
│ [📄 Notes]        ┌───────────┐ ┌───────────┐ ┌───────────┐ ┌───────────┐                       │
│ [⚙️ Settings]     │ [🟥]       │ │ [🟦]       │ │ [🟪]       │ │ [🟨]       │                       │
│                   │Jadwal Psts│ │  Sejarah  │ │Seni Budaya│ │Matematika │                       │
│ Workspaces    (+) │  1 note   │ │  3 notes  │ │  4 notes  │ │  Lanjut   │                       │
│ ⊞ Home            └───────────┘ └───────────┘ └───────────┘ └───────────┘                       │
│ 📦 Archive        ┌───────────┐ ┌───────────┐ ┌───────────┐                                     │
│ 📁 Test           │ [🟪]       │ │ [🟩]       │ │   +     │                                     │
│ 📁 test           │Matematika │ │  Fisika   │ │New Folder │                                     │
│                   │  Wajib    │ │  4 notes  │ │           │                                     │
│ [💾 Storage]      └───────────┘ └───────────┘ └───────────┘                                     │
│ 2.4 / 10 GB used  📄 Recent Notes                                           5 items [ View all → ]
│ [====-----]       • [Canvas] Canvas Note                      📁 Sejarah          10:21        [⋮]│
│                   • [Canvas] Canvas Note                      📁 Seni Budaya    Yesterday 19:05[⋮]│
│                   • [TXT]    testb                            📁 Seni Budaya    Yesterday 19:05[⋮]│
│                   • [DOCX]   KISI-KISI PSTS GANJIL XI T...    📁 Seni Budaya    Yesterday 19:04[⋮]│
│                   • [PDF]    Jadwal_PSTS_Ganjil_2026-2...     📁 Jadwal Psts    Yesterday 18:09[⋮]│
└─────────────────────────────────────────────────────────────────────────────────────────────────┘
```

### Layout Elements:
1. **Left Navigation Sidebar (~250px)**:
   - Brand header with blue squircle Fotara logo.
   - Primary tabs: Home (Dashboard), Notes (Split Study Studio), Settings.
   - Workspaces tree with quick filter ("Home", "Archive", "Test", "test") and `+` creation.
   - Bottom Storage indicator with drive icon, disk quota (2.4 GB of 10 GB used), and progress bar.
2. **Main Dashboard View (Home)**:
   - Header: Large "Home" title, "Your notes, organized" subtitle, pill search bar, blue `+` action button, and window controls.
   - Course Folders Grid: 4-column cards with top-left accent colored tiles (Red, Blue, Purple, Amber with corner glow, Violet, Green) and `+ New Folder`. Supports Grid / List view toggle.
   - Recent Notes List: Live items with distinct type badges (Canvas Note, TXT, DOCX, PDF), folder chips, timestamps, and action menus.
3. **Widescreen Study Studio (Split Pane View)**:
   - Accessible via Notes tab or clicking any note/folder.
   - 50/50 split: PDF.js document viewer on left, live KaTeX LaTeX note editor on right.
   - Audio waveform scrubber and distraction-free Zen Mode.

---

## 3. Technology Stack & Native Packaging

```
┌──────────────────────────────────────────────────────────────────┐
│                   FOTARA DESKTOP RUNTIME STACK                   │
├──────────────────────────────────────────────────────────────────┤
│ UI Layer       : React 19 + TypeScript + Vite + Tailwind CSS     │
│ Editor Core    : KaTeX Math Renderer + Markdown ProseMirror /    │
│                  TipTap Engine + HTML5 Vector Canvas             │
│ Local Storage  : LocalFileSystem API / OPFS / SQLite Wasm        │
│ Native Wrapper : Tauri v2 (Rust Native Shell + WebView2)         │
│ Target Output  : /Output/Release/Fotara_2.0.0_Beta_Windows_x64.exe│
└──────────────────────────────────────────────────────────────────┘
```

### Why This Stack Outperforms Electron:
- **Binary Footprint**: ~7.5 MB `.exe` installer (vs 160 MB in Electron).
- **Memory Footprint**: ~42 MB RAM idle (vs 280 MB in Electron).
- **Cold Boot**: <250 milliseconds from disk execution to interactive frame.
- **Rendering Engine**: Utilizes Microsoft Edge WebView2 pre-installed on Windows 10 and Windows 11.

---

## 4. Phone $\leftrightarrow$ Desktop Interoperability Flow

1. **Coursework Archive Interchange (`.fotara`)**:
   - The student clicks **Export Folder** on the Android app, generating `Calculus_Semester3.fotara`.
   - On Desktop, the student simply **drags and drops** the `.fotara` file into the window.
   - All lecture photos, OCR texts, synchronized audio annotations, and drawing strokes instantly unpack into the desktop workspace.
2. **Local Peer Collaboration (Phase 51)**:
   - Students in the same room enter the 6-digit room code (`FT-XXXX`).
   - The desktop client connects over low-latency WebRTC/WebSocket to mirror canvas strokes in real time.

---

## 5. Release Phasing & Target Milestones

| Milestone | Target Version | Scope & Deliverable |
|---|---|---|
| **Phase A (Scaffold)** | `1.9.0 Series` | Directory `FotaraDesktop`, Vite + React + TS skeleton, 3-pane layout, KaTeX rendering. |
| **Phase B (Packaging)**| `2.0.0 Beta` | Tauri v2 Windows shell integration, `.fotara` archive drag-and-drop import, `Fotara_2.0.0_Beta_Windows_x64.exe` release preview. |
| **Phase C (Parity)**   | `2.1.0` | Full database synchronization, Live Room Code canvas parity, multi-sheet vector canvas on desktop. |
