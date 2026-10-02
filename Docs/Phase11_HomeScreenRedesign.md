<!-- --- Arinara Network (c) 2026 --- -->
<!-- Exclusive property of Arinara Network. -->
<!-- Unauthorized use, reproduction, distribution, or modification of this code, -->
<!-- in whole or in part, for any purpose, is strictly prohibited without prior -->
<!-- written consent from Arinara Network as sole legal owner of this codebase. -->

# Phase 11 - HomeScreenRedesign

## Goal
Transform the Fotara home screen, bottom navigation, and Settings screen to precisely match the target design in IMAGE A, modernizing visual ergonomics with near-black aesthetics (`#0A0D14`), 1:1 dark folder cards with accent icon tiles, corner-stroke LinkIt glows, a floating bottom navigation pill, and a cohesive rounded-card Settings layout.

## Scope
- Redesign `HomeScreen.kt` to match IMAGE A layout, hierarchy, typography, and colors.
- Header: Large upright "Fotara" (Medium, ~38-40sp), subtitle "Your notes, organized" (Light, ~15sp), and dual circular buttons (magnifier + options menu).
- Segmented tab bar: "All" (active blue pill with 2x2 grid icon), "Favorit" (star outline), "Arsip" (archive outline) with selective 1dp dividers.
- Folder cards: 2-column grid of ~1:1 dark cards (`#111726`, 24dp radius), top-left accent icon tile (~42dp, 12dp radius) with folder outline icon, top-right 3-dots menu button, left-aligned title (Medium, ~22sp) and note count (Light, ~15sp).
- LinkIt glow: Thin ~2dp corner stroke running ~55dp along edges with soft interior corner glow in the folder's own bright accent color, oriented facing adjacent partner cards in a 2-column grid.
- Bottom stack: Full-pill search bar (`#141B2A`, 52dp) and circular "+" button (`#2563EB`, 52dp) opening an upward menu ("New folder" and "Capture notes").
- Floating bottom navigation: 66dp pill with 12dp side margins, 3 equal tabs: Home, Notes (blank placeholder), Settings.
- Settings screen redesign: Vertical list of rounded rectangular cards (`#111726`, 20-24dp radius, 12dp spacing) with 42dp icon tiles, preserving all existing options and dialogs.
- Typography: Elms Sans (Light 300, Medium 500, Bold 700), zero italics in UI.
- All touch targets >= 48dp.

## Out Of Scope
- Modifying folder detail screens, photo viewers, text note editor, or canvas note editor.
- Implementing backend or storage for Favorit/Arsip (kept blank).
- Adding new settings or altering existing settings keys.

## Features
### Header & Subtitle
- Top-left header with "Fotara" (Medium, ~38sp) and tagline "Your notes, organized" (Light, ~15sp, `#6B7280`).
- Top-right dual buttons in 40dp circles (`#131925`):
  - Magnifier: triggers search bar focus and soft keyboard.
  - Overflow menu: includes Multi-Select, What's New, Updates, Feedback, Support, Settings, Trash.

### Segmented Tab Bar
- Pill container (`#121826`, 48dp tall, 18dp horizontal margins).
- "All" selected with `#1B4FC4` filled pill inset 3dp, white text + grid icon.
- "Favorit" and "Arsip" unselected with outline icons, muted text, and 1dp vertical divider between unselected items.

### Modern Dark Folder Cards
- Card surface `#111726`, 24dp corner radius, aspect ratio ~1:1.
- Top-left 42dp tile with 12dp radius in folder accent color (`#1E3A6B`, `#5C4030`, `#3F3270`, `#1E4A38`, `#6B2A30`, `#343C52`) with lighter folder outline icon.
- Top-right 40dp touch target with 3-dots icon opening folder action menu (Rename, Color, Pin, Lock, Link It / Unlink, Delete).
- Content: Folder name (Medium, ~22sp, max 2 lines) and note count (Light, ~15sp, muted) flowing downward.

### Facing Corner LinkIt Glow
- Renders only when linked partner is adjacent.
- Same row: left card glows at BottomRight, right card glows at BottomLeft.
- Across rows: upper-right card glows at BottomLeft, lower-left card glows at TopRight.
- 2dp stroke along corner arc extending 55dp along edges, fading to transparent with soft interior glow.

### Search Bar & Main Action Button Row
- Search pill (`#141B2A`, 52dp) with magnifier and hint "Search notes, subjects, text...".
- Floating circular button (`#2563EB`, 52-56dp) with white "+".
- Tapping "+" opens popup menu anchored above: "New folder" and "Capture notes".
- Rides above keyboard on IME focus with bottom nav hiding during active search.

### Floating Bottom Navigation Bar
- 66dp floating pill container with 12dp margins, `#111726` surface, `#1C2333` subtle border.
- 3 items: Home (active house with soft highlight), Notes (document icon), Settings (gear icon).
- Notes screen displays empty state with bottom nav visible.
- Settings screen renders rounded rectangle rows with bottom nav visible.

## UI Mockup
```
+---------------------------------------------+
| Fotara                               (Q) (:) |
| Your notes, organized                       |
|                                             |
| [  [::] All  |    * Favorit   |   [v] Arsip ]|
|                                             |
| +-----------------+     +-----------------+ |
| | [F]          (:)|     | [F]          (:)| |
| | testing         |     | Matematika      | |
| | 14 notes        |     | Lanjut          | |
| |                 |     | 1 note       /G | |
| +-----------------+     +-----------------+ |
|                                             |
| +-----------------+     +-----------------+ |
| | [F]          (:)|     | [F]          (:)| |
| | Matematika   G\ |     | Fisika          | |
| | Wajib           |     | 4 notes         | |
| | 3 notes         |     |                 | |
| +-----------------+     +-----------------+ |
|                                             |
| [ Q  Search notes, subjects, text... ]  (+) |
|                                             |
|     ( [H] Home   |   [D] Notes   |   [*] Set )
+---------------------------------------------+
```

## Logic Notes
- Persistent folder color palette assignment: default stable mapping for legacy `#00B4D8` folders.
- Bottom padding on LazyVerticalGrid accounts for bottom navigation (66dp) + search row (52dp) + spacing (16dp) + navigation bar insets.
- Keyboard IME listener: hides bottom nav when keyboard appears and positions search bar above IME.
- Multi-select mode: top app bar transitions to multi-select actions (Select All, Link, Rename, Delete).

## Risks
- Risk: Keyboard overlay obscuring search bar during typing -> Mitigation: `WindowInsets.ime` and `imePadding()` applied to search bar row while suppressing bottom nav.
- Risk: Inset collision on 3-button navigation devices -> Mitigation: Explicit `navigationBarsPadding()` added to bottom stack container.
- Risk: Link glow disorientation when grid reorders -> Mitigation: Dynamic recalculation of facing corners in `computeFolderGlowOrientations` on any list change.

## Dependencies
- Phase 1 (Core Folder Engine)
- Phase 5 (LinkIt Grouping)
- Rules.md R-004

## Acceptance Criteria
- [x] Screen background is `#0A0D14` edge-to-edge.
- [x] Header displays "Fotara" (Medium, ~38sp) and "Your notes, organized" (Light, ~15sp) with dual circular action buttons.
- [x] Segmented tab bar displays "All", "Favorit", "Arsip" with selective 1dp dividers and active blue pill.
- [x] Folder cards match 1:1 aspect ratio, 24dp radius, top-left accent tile, top-right menu, and downward text flow.
- [x] LinkIt glow appears on adjacent linked folder pairs with folder-specific bright accent colors facing each other.
- [x] Search bar and "+" button sit in a row with "+" menu offering "New folder" and "Capture notes".
- [x] Floating bottom nav has 3 tabs (Home, Notes, Settings) with active highlight on Home.
- [x] Notes tab displays blank area with bottom nav visible.
- [x] Settings tab displays vertical list of rounded rectangles with bottom nav visible.
- [x] Touch targets >= 48dp and zero italics across all redesigned UI elements.
