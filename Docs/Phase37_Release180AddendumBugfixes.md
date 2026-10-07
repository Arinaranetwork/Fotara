# Phase 37 - Release180AddendumBugfixes
## Goal
Resolve three targeted user-reported defects for Release 1.8.0 Beta:
1. Profile screen header position and banner visual discrepancies compared to other Settings sub-screens.
2. Floating (+) action button jumping when quickly switching tabs between Home and Notes.
3. Drawing stroke offset divergence after zooming in Photo and PDF drawing canvas.

## Scope
- Task 1: Unify header structure and banner rendering across `ProfileScreen`, `ProfileCropScreen`, `LegalDocumentScreen`, and Settings sub-screens using shared components (`SettingsSubScreenHeader` and `ProfileBannerHeader`).
- Task 2: Hoist a single (+) button to the tab container level (`HomeScreen`), maintaining persistent bottom-clearance state across tab changes without position/elevation re-measurement jumps.
- Task 3: Implement pure coordinate transform `DrawingViewTransform` with round-trip mathematical precision, eliminating redundant zoom-inversion in `PhotoDrawingEditor` and `PdfPageEditorScreen`.
- Fix compilation issues in `PdfPageEditorScreen` (`cd_back`, `cd_more_options`, `DangerRed`).
- Unit and UI tests verifying header constants, banner visibility, FAB state stability, and transform precision from 1.0x to 6.0x zoom.
- Append Addendum fixes to `Changelog_1.8.md`.

## Out Of Scope
- No database schema changes (database remains at version 18).
- No new external libraries or dependencies.
- No network requests.
- No modification of `ViewportTransform.kt` or `fotara.fileprovider`.

---

## Audits & Structural Comparisons

### Task 1: Sub-Screen Header Structure Audit Table
| Screen | Back Button Position / Size | Title Style | Top Offset from Status Bar | Header Height | Horizontal Padding | Gap to Content | Largest Font Size Behavior |
|---|---|---|---|---|---|---|---|
| **General** (Reference) | Start / 40x40dp (20dp icon) | 24sp ElmsSans Medium | `innerPadding.top + 8dp` | Wrap content (~54dp) | 18dp | 12dp list spacing | Wraps cleanly, icon remains vertically centered |
| **Appearance** | Start / 40x40dp (20dp icon) | 24sp ElmsSans Medium | `innerPadding.top + 8dp` | Wrap content (~54dp) | 18dp | 12dp list spacing | Wraps cleanly, icon remains vertically centered |
| **OCR** | Start / 40x40dp (20dp icon) | 24sp ElmsSans Medium | `innerPadding.top + 8dp` | Wrap content (~54dp) | 18dp | 12dp list spacing | Wraps cleanly, icon remains vertically centered |
| **Notifications** | Start / 40x40dp (20dp icon) | 24sp ElmsSans Medium | `innerPadding.top + 8dp` | Wrap content (~54dp) | 18dp | 12dp list spacing | Wraps cleanly, icon remains vertically centered |
| **Storage** | Start / 40x40dp (20dp icon) | 24sp ElmsSans Medium | `innerPadding.top + 8dp` | Wrap content (~54dp) | 18dp | 12dp list spacing | Wraps cleanly, icon remains vertically centered |
| **About & Legal** | Start / 40x40dp (20dp icon) | 24sp ElmsSans Medium | `innerPadding.top + 8dp` | Wrap content (~54dp) | 18dp | 12dp list spacing | Wraps cleanly, icon remains vertically centered |
| **Privacy Policy** | Start / 48x48dp (24dp icon) | 20sp ElmsSans Bold | `WindowInsets.statusBars + 8dp` | ~64dp | 8dp | 0dp (divider) | Multi-line title with subtitle |
| **Terms of Service** | Start / 48x48dp (24dp icon) | 20sp ElmsSans Bold | `WindowInsets.statusBars + 8dp` | ~64dp | 8dp | 0dp (divider) | Multi-line title with subtitle |
| **Profile** (Before) | Start / 40x40dp (20dp icon) inside banner Box | 24sp ElmsSans Bold | `WindowInsets.statusBars + 6dp` | Overlay on 220dp banner | 16dp | Variable | Text over banner, offset differed from General by 10dp |
| **Crop: Edit Picture** | Start / 48x48dp (Close icon) | 18sp ElmsSans Bold | `WindowInsets.statusBars + 4dp` | ~56dp | 8dp | Fill container | Ellipsize or wrap |
| **Crop: Edit Banner** | Start / 48x48dp (Close icon) | 18sp ElmsSans Bold | `WindowInsets.statusBars + 4dp` | ~56dp | 8dp | Fill container | Ellipsize or wrap |

### Task 1: Banner Structure Audit Table
| Attribute | Settings Screen Banner | Profile Screen Banner (Before) | Unified Specification |
|---|---|---|---|
| **Height** | 230dp | 220dp | **230dp** |
| **Aspect Ratio Rule** | Full width x 230dp fixed | Full width x 220dp fixed | Full width x 230dp fixed |
| **Crop Aspect Ratio** | 16:9 (`ProfileCropScreen`) | 16:9 (`ProfileCropScreen`) | 16:9 frame matching visible display |
| **Content Scale** | Crop via `ProfileBanner` matrix | Crop via `ProfileBanner` matrix | Shared `ProfileBanner` matrix |
| **Top Scrim** | 95dp vertical gradient (0.65 black -> trans) | 90dp vertical gradient (0.65 black -> trans) | **95dp vertical gradient** |
| **Bottom Fade** | 115dp vertical gradient (trans -> 0.75 -> NearBlack) | 110dp vertical gradient (trans -> 0.75 -> NearBlack) | **115dp vertical gradient** |
| **Avatar Size** | 78dp (bottom-center aligned) | 78dp (bottom-center aligned) | 78dp (bottom-center aligned) |
| **Status Bar Inset** | Covered by top scrim | Covered by top scrim | Extended into status bar |
| **Data Source** | `uiState.userProfile` (`bannerPath`, `bannerCrop`) | `uiState.userProfile` (`bannerPath`, `bannerCrop`) | Identical `UserProfile` entity |

---

### Task 2: Floating (+) Button Jump Audit & Root Cause
- **Hosting**:
  - `HomeScreen`: Hosted in `FloatingDock` inside a bottom pinned `Column` alongside `HomeBottomNavBar`.
  - `NotesScreen`: Hosted in an independent `Box` aligned to `BottomEnd` with `fabBottomPadding = (bottomOverlayPadding + 8.dp).coerceAtLeast(80.dp)`.
- **Transitions**:
  - `HomeScreen` tabs switch via Compose `when (selectedNavTab)` (no crossfade, direct swap).
  - During tab switch, `FloatingDock` unmounts in `HomeScreen`. The bottom pinned container height shrinks from ~130dp (`FloatingDock` + `HomeBottomNavBar`) to ~64dp (`HomeBottomNavBar` only).
  - `bottomStackHeightPx` is remeasured asynchronously in `onSizeChanged`.
  - In `NotesScreen`, `LocalBottomOverlayPadding` updates from 130dp to 64dp on the subsequent frame, causing `fabBottomPadding` to jump from 138dp to 80dp.
- **Root Cause**:
  1. Two separate (+) button instances with different lifecycle and measurement dependencies.
  2. Dynamically collapsing bottom overlay container in `HomeScreen` triggering an asynchronous height update and layout jump.
- **Fix**:
  - Hoist ONE single (+) button at the `HomeScreen` scaffold/root container level, positioned above both tab contents and above the bottom nav bar.
  - Maintain a persistent bottom overlay clearance state that never resets to 0 and uses a deterministic initial height (130dp).
  - Route click action based on `selectedNavTab`: Home creates a folder, Notes opens the note creation sheet/dialog.
  - Keep the (+) button continuously composed between Home and Notes; on Settings, apply alpha fade out without repositioning.

---

### Task 3: Drawing Stroke Offset After Zoom Audit & Root Cause
- **Gesture Architecture**:
  - `PhotoDrawingEditor` and `PdfPageEditorScreen` applied `Modifier.graphicsLayer { scaleX = userScale; scaleY = userScale; translationX = panOffset.x; translationY = panOffset.y }` to the drawing box.
  - `Modifier.pointerInput` was attached to that SAME box directly below `graphicsLayer`.
- **Compose Layer Behavior**:
  - Jetpack Compose automatically inverse-transforms incoming touch events by the layer matrix into local unzoomed box coordinates:
    $$\mathbf{p}_{\text{local}} = \frac{\mathbf{p}_{\text{screen}} - \mathbf{c} - \mathbf{t}}{s} + \mathbf{c}$$
  - Inside the gesture handler, the code performed manual inversion:
    $$\mathbf{p}_{\text{content}} = \frac{\mathbf{p}_{\text{local}} - \mathbf{c} - \mathbf{t}}{s} + \mathbf{c}$$
- **Mathematical Root Cause (Double Inversion)**:
  - The scale and translation inverse was applied **twice**:
    $$\mathbf{p}_{\text{computed}} = \frac{\mathbf{p}_{\text{screen}} - (s+1)(\mathbf{c} + \mathbf{t})}{s^2} + \mathbf{c}$$
  - At $s = 1.0$ and $\mathbf{t} = 0$, $\mathbf{p}_{\text{computed}} = \mathbf{p}_{\text{screen}}$, masking the bug.
  - At $s > 1.0$, the touch point and the computed stroke coordinate diverged proportionally to $s - 1$.
- **Fix**:
  - Implement `DrawingViewTransform` with explicit pure methods:
    - `screenToContent(screenOffset, ...)`
    - `contentToScreen(contentOffset, ...)`
    - `localToContent(localOffset, ...)`
  - Use `localToContent` directly for pointer coordinates received inside layer-transformed nodes, or attach pointerInput to the outer unscaled container and use `screenToContent`.
  - Comprehensive unit test suite verifying zero offset round-trip precision from 1.0x to 6.0x zoom.

---

## Acceptance Criteria
- [ ] Shared `SettingsSubScreenHeader` utilized across all Settings sub-screens, Profile, and crop screens.
- [ ] Shared `ProfileBannerHeader` ensures identical 230dp height, scrims, and crop framing between Settings and Profile preview.
- [ ] Single (+) FAB hosted in tab container with no jump or repositioning during rapid Home/Notes switching.
- [ ] `DrawingViewTransform` achieves exact round-trip identity at scales 1.0x to 6.0x.
- [ ] Zero offset between touch point and stroke tip in Photo and PDF drawing canvas when zoomed.
- [ ] 100% unit tests passing and release APK successfully assembled.
