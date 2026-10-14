# Fotara - Project & Agent Guidelines

## 1. Design System & Tokens (Electric Slate Dark Theme)
All screens, composables, and components must strictly adhere to the Electric Slate Dark Theme tokens. Never use hardcoded hex colors or arbitrary values.

### Color Tokens
- `BgCanvas`: `#0D1117` (Main background & dot-grid canvas)
- `SurfacePanel`: `#161C26` (Cards, bottom docks, settings panel)
- `SurfaceElevated`: `#1F2736` (Dropdowns, floating overlays, tooltips, connected inspector)
- `BorderSubtle`: `#262F3E` (1dp subtle structural outline)
- `TrackInactive`: `#283245` (Inactive tracks, neutral containers, secondary buttons)
- `TextPrimary`: `#F8FAFC` (High-contrast primary text, headings)
- `TextMuted`: `#94A3B8` (Secondary labels, descriptions, subtitles, inactive icons)
- `PrimaryAccent`: `#7A5AF8` (Electric violet for active states, slider thumbs, droplet neck, FABs)
- `Destructive`: `#EF4444` (Delete buttons, error banners, critical warnings)

### Universal Spring Physics (`SpringPhysics.kt`)
Zero rigid linear motion. All touch feedback and animated transitions must use spring physics:
- **JigglePress**: `spring(dampingRatio = 0.5f, stiffness = 180f)` via `Modifier.jigglePressClickable` (scale 0.90f on touch down, 1.08f overshoot, settles at 1.00f).
- **ViscousGooey**: `spring(dampingRatio = 0.45f, stiffness = 100f)` for droplet stretching, organic slider expansion, and fluid transitions.
- **DismissFade**: `spring(dampingRatio = 0.6f, stiffness = 300f)` for scale-pop exit and alpha fade.

---

## 2. Standardized Component Presets (`ui/components/presets/`)
Every screen must be assembled solely from these presets:
1. `AppScreen`: Outer screen wrapper enforcing `#0D1117` canvas background, status/navigation bar insets, and `>= 100.dp` safe bottom clearance.
2. `AppTopBar`: Symmetrical pill-capsule top header with back button rotation jiggle (`-12deg` to `+6deg` to `0deg`), title, and action slots.
3. `AppCard`: 24dp rounded corners, `SurfacePanel`, 1dp `BorderSubtle`, integrated `JigglePress` on tap, optional ambient glow.
4. `AppFloatingBar`: 999dp pill capsule with spring slide-up entrance for floating docks and toolbars.
5. `ConnectedInspector`: Floating settings panel attached via a cubic Bézier organic droplet neck (`ViscousGooey` spring physics) to the active tool on the floating dock.
6. `AppSlider`: Stepped discrete dot indicators (`4.dp` circles in `BorderSubtle`; lit points turn to `PrimaryAccent`), thumb touch scale pop (1.0f -> 1.25f), and magnetic snap bounce.
7. `AppBottomSheet`: 28dp top corners, elastic drag resistance, minimizable into a bottom floating pill.
8. `AppListItem`: Standardized list row with leading icon box, dual text slots, and trailing Chevron, Bouncy Switch, or Shaking Trash.
9. `AppButton` & `AppFAB`: Primary, Secondary, Destructive, and circular FAB variants with scale-pop press physics.
10. `AppPillChip`: 999dp capsule chip with scale-pop toggle selection.
11. `AppNoticeBanner`: Warning/info banner with downward overshoot entry and `DismissFade` exit.

---

## 3. Strict Android UI Rules (`AR-001` - `AR-007`)

### AR-001: Strict Token Adherence
All UI elements must resolve dimensions, colors, and line heights from tokens. No ad-hoc hardcoded values.

### AR-002: Stable PointerInput Keys
Pointer input modifiers must never pass mutable gesture offsets or scales as modifier keys, preventing coroutine restart stutter.

### AR-003: Mandatory Component Presets
All screens and views must be composed exclusively from the standardized component presets in `ui/components/presets/`. Raw Jetpack Compose components (`Button`, `Card`, custom `Box` containers with one-off styling) are strictly forbidden.

### AR-004: Standardized Shape Hierarchy
Corner radii must strictly resolve to standard shape tokens:
- `999.dp` (Capsule / Pill)
- `28.dp` (Bottom Sheet top corners)
- `24.dp` (Card / Major modules)
- `20.dp` / `16.dp` (Panel / Inspector / Dialogs)
- `12.dp` / `14.dp` (List Row / Text Inputs)
- `CircleShape` (FAB / Circular Controls)

### AR-005: Max 3 Active Colors Per Screen
A single screen view must never display more than 3 active semantic colors simultaneously: Neutral Base/Surface hierarchy, Electric Violet Primary Accent (`#7A5AF8`), and Destructive Alert (`#EF4444`). Prohibit rainbow toolbars.

### AR-006: Safe Bottom Page Clearance
Every screen and scrollable container must reserve a minimum of `100.dp` safe clearance at the bottom of the page (`AppScreen`, `contentPadding = PaddingValues(bottom = 100.dp)`, or `BottomClearanceSpacer()`) so floating bottom bars do not occlude content.

### AR-007: Centralized Muted Tone Discipline
Secondary text, descriptions, subtitles, inactive slider tracks, switch tracks, and structural dividers must strictly resolve to `TextMuted` (`#94A3B8`), `BorderSubtle` (`#262F3E`), or `TrackInactive` (`#283245`).

---

## 4. Engineering & Execution Protocols
- **Build Policy**: Do not run full APK builds (`assembleDebug` / `assembleRelease` are forbidden). Only unit and Compose tests are allowed (`./gradlew testDebugUnitTest`).
- **Completion Notification**: When all implementations and tests are verified, run:
  ```cmd
  .agents\scripts\notify.cmd "Task Complete" "All implementations and unit tests verified."
  ```
