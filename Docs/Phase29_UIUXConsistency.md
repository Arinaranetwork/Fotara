<!-- --- Arinara Network (c) 2026 --- -->
<!-- Exclusive property of Arinara Network. -->
<!-- Unauthorized use, reproduction, distribution, or modification of this code, -->
<!-- in whole or in part, for any purpose, is strictly prohibited without prior -->
<!-- written consent from Arinara Network as sole legal owner of this codebase. -->

# Phase 29 - UI/UX Consistency (Batch 3)

## Goal
Establish visual and functional consistency across the primary tab destinations (Home, Notes, Settings) for Fotara release 1.6.0. Unify header metrics, clean up redundant menus and back navigation, transform the Notes filter chip row into an on-demand expandable control, integrate the shared workspace tab bar on the Notes screen with synchronized state, and optimize folder-card action button ergonomics.

## Scope
- Audit and document current header metrics across Home, Notes, and Settings.
- Implement unified `ScreenHeader` composable and `ScreenHeaderDefaults` layout tokens.
- Apply `ScreenHeader` to `HomeScreen`, `NotesScreen`, and `SettingsScreen` with reserved tagline slot height to ensure identical vertical tab row positioning.
- Remove redundant back arrow from root `SettingsScreen` while preserving system back navigation to Home and back buttons in settings sub-screens.
- Remove redundant "Settings" items from Home and Notes three-dot overflow menus without leaving dangling dividers.
- Move `FolderCard` three-dot button to top-right corner (~20dp visual center, $\ge 44$dp touch target) across densities 2, 3, 4 without colliding with folder tile or title.
- Audit note cards inside folders and report on their layout ergonomics.
- Remove Notes subtitle and hide filter chips by default, toggling visibility with expand/collapse animation via the header search button.
- Integrate `WorkspaceTabBar` on Notes screen at the identical vertical baseline as Home.
- Elevate `selectedWorkspaceId` into `WorkspaceRepository` as a single shared source of truth between Home and Notes.
- Filter Notes list by `folders.workspace_id == selectedWorkspaceId` with custom empty states.
- Unit tests validating header tokens, menu configurations, chip visibility state transitions, workspace filter synchronization, and folder card button geometry.
- TASK 4B: Profile borders audit, repeatable alpha measurement, normalized ProfileAvatar composable, invariant 45-degree pen button geometry, and collision/contrast analysis.

## Out Of Scope
- PDF content OCR for search (deferred to Batch 4).
- Release banner generation and What's New dialog updates (deferred to Batch 4).
- Production release packaging / APK build (deferred to Batch 4).
- Database schema changes (v16 is preserved).
- Note card menu changes inside folders.
- Art file editing (compensating via math and innerCenter metadata).

## Task 1 Header Audit Table
| Attribute | Home Screen (Reference) | Notes Screen (Current) | Settings Screen (Current) | Unified Target (`ScreenHeader`) |
|---|---|---|---|---|
| Title Text Style | 38.sp, Medium, (-0.5).sp, ElmsSans, Color.White | 36.sp, Bold, (-0.5).sp, ElmsSans, Color.White | 38.sp, Medium, (-0.5).sp, ElmsSans, Color.White | 38.sp, Medium, (-0.5).sp, ElmsSans, Color.White |
| Top Offset (below status bar) | 16.dp | 16.dp | 6.dp | 16.dp (`ScreenHeaderDefaults.TopPadding`) |
| Left / Horizontal Padding | 18.dp | 18.dp | 16.dp (or after back arrow) | 18.dp (`ScreenHeaderDefaults.HorizontalPadding`) |
| Tagline Style & Spacing | 15.sp, Light, 2.dp gap, HomeSubtitleGray | 14.sp, Light, 2.dp gap ("Your activity, all in one place") | None | 15.sp, Light, 2.dp gap. Reserved height = 20.dp when tagline is null |
| Header Total Height (incl. tagline) | ~68dp | ~62dp | ~46dp | Strictly uniform height across all screens |
| Right Action Buttons | 2x (48dp target, 40dp circle, 20dp icon, 12dp gap) | 2x (48dp target, 40dp circle, 20dp icon, 12dp gap) | None | Standardized circular action buttons via `ScreenHeaderActionButton` |
| Gap to Next Element | 12.dp to `WorkspaceTabBar` | 8.dp to Search / Chips | Over banner (115dp scrim) | 12.dp (`ScreenHeaderDefaults.HeaderBottomGap`) to `WorkspaceTabBar` |

## Features
### 1. Unified Screen Header Composable
- `ScreenHeader(title: String, tagline: String?, actions: @Composable (RowScope.() -> Unit)?)`
- Shared tokens in `ScreenHeaderDefaults`: horizontal padding (18dp), top padding (16dp), bottom gap (12dp), title style (38sp, Medium, -0.5sp letter spacing), tagline style (15sp, Light), tagline gap (2dp), tagline reserved slot height (20dp), circular action button metrics (48dp touch target, 40dp circle, 20dp icon, 12dp spacing).
- Home displays title "Fotara" with tagline "Your notes, organized".
- Notes displays title "Notes" with no tagline, reserving the 20dp slot so the tab row below aligns identically with Home.
- Settings displays title "Settings" over the profile banner without back arrow or action buttons.

### 2. Menu and Navigation Cleanup
- Main Settings screen removes explicit back arrow IconButton. System back button from Settings tab triggers `BackHandler` navigating back to Home tab.
- Sub-screens of Settings (Profile, General, Storage, etc.) preserve their back navigation arrow returning to root Settings.
- Home three-dot overflow menu: removes "Settings". Dividers between "Select Folders", "Add Workspace", update items, and "Trash" remain balanced with zero dangling dividers.
- Notes three-dot overflow menu: removes "Settings".
- Folder card three-dot button: moved to top-right corner with icon visual center at ~20dp from top and ~20dp from right, maintaining $\ge 44$dp touch target without overlapping folder icon tile or title.

### 3. Notes Screen Filter on Demand
- Subtitle string `notes_subtitle` removed.
- Inline search text field removed from Notes screen.
- Header search button toggles filter chip row visibility with expand/collapse animation (`AnimatedVisibility`).
- Header search button shows active blue tint when filter chips are visible.
- Content descriptions toggle between `R.string.cd_show_filters` and `R.string.cd_hide_filters`.
- Hiding filter chips automatically resets filter type to `NoteFilterChip.ALL`.
- Filter chip row preserves horizontal scrolling and remembers state during rotation and screen re-entry.

### 4. Workspace Tabs on Notes Screen & State Synchronization
- Reusable `WorkspaceTabBar` integrated on Notes screen directly beneath `ScreenHeader`, achieving exact vertical parity with Home.
- `selectedWorkspaceId` elevated to `WorkspaceRepository` singleton in `AppContainer`. Both `HomeViewModel` and `NotesViewModel` observe and mutate this state.
- When a custom workspace is deleted, `WorkspaceRepository` automatically resets `selectedWorkspaceId` to `HOME_WORKSPACE_ID` (1L).
- Notes list filtered by `folder.workspaceId == selectedWorkspaceId`.
- Empty state updated to "No notes in <workspace name> yet" (`R.string.workspace_notes_empty_state`) when no notes match.
- Notes FAB (+) destination picker retains cross-workspace grouped folder picker.

### 5. Profile Borders: One Consistent Fit (Task 4B)
#### Audit of Existing Implementation
- `ProfileBorders` previously stored `innerRatio`, `centerOffsetX`, and `centerOffsetY` calculated via `measure_true_circle.ps1` from simple bounding box edges without full extent or radial normalization.
- In `ProfileAvatarView`, the container box was sized dynamically with `size(maxOf(avatarSize, avatarSize / border.innerRatio))`. For an avatar of $D=78\text{dp}$, the container box jumped between 78dp (None), 108.2dp (Neon Spark), 111.2dp (Crystal Arc), and 119.8dp (Golden Wings).
- The edit pen button was anchored via `align(Alignment.BottomEnd)` of this expanding container, causing it to jump position by up to ~40dp depending on which border was selected.
- In `BorderPickerDialog`, each card cell expanded or contracted based on the individual border's `innerRatio`, leading to misaligned text labels and irregular grid heights.

#### Alpha Measurement Method & Table
Measurements performed via byte-level direct pixel analysis in `scripts/MeasureBorderAlpha.ps1` scanning equatorial hole boundaries, 360-degree radial raycasting, and alpha $>10$ extent bounding boxes.
- Art files are never edited. Geometric discrepancies (such as Golden Wings width 1310px vs height 1200px and off-center rings) are compensated via exact `innerCenterX` and `innerCenterY` fractions.

| Border ID | Display Name | Canvas W x H | Inner Center (X, Y) | Center Offset (dX, dY) | Inner Diam Ratio | Ring Outer Diam Ratio | Ring Thickness (px / ratio) | Extent Rect [L, T, R, B] | Avg Lum |
|---|---|---|---|---|---|---|---|---|---|
| `none` | None | N/A | (0.5000, 0.5000) | (0.0000, 0.0000) | 1.0000 | 1.0000 | 0.0 / 0.0000 | [0.0000, 0.0000, 1.0000, 1.0000] | N/A |
| `file_000000000278820bb0a8901e8fa6612b` | Neon Spark | 1254 x 1254 | (0.4565, 0.4908) | (-0.0435, -0.0092) | 0.7209 | 0.8517 | 82.0 / 0.0654 | [0.0191, 0.0463, 0.9992, 0.9179] | 98.6 |
| `file_000000001c6c820bbc0efdac44c0aa63` | Crystal Arc | 1254 x 1254 | (0.5036, 0.5347) | (+0.0036, +0.0347) | 0.7026 | 0.8182 | 72.5 / 0.0578 | [0.0279, 0.0303, 0.9769, 0.9689] | 83.7 |
| `file_000000007fdc81f7b74b9a79e2c63215` | Golden Wings | 1310 x 1200 | (0.4622, 0.4912) | (-0.0378, -0.0088) | 0.6511 | 0.7878 | 89.5 / 0.0683 | [0.0344, 0.0308, 0.9916, 0.9700] | 65.1 |

#### Normalized Component Architecture (`ProfileAvatar`)
- Avatar is clipped to a fixed circular diameter $D$, completely invariant to border selection.
- Fixed slot size: `avatarSize` (78dp for main screens, 52dp for picker).
- Border image is scaled so its inner circle diameter matches $D$ with a 1.5% overlap (`overlapFraction = 0.015f`, $D_{inner\_screen} = D \times 0.985$), eliminating gaps or hairlines.
- Border image is rendered with `graphicsLayer { clip = false; translationX = shiftX; translationY = shiftY }` in draw phase. Shift offsets are calculated as `shiftX = -(innerCenterX - 0.5f) * scaleWidth` and `shiftY = -(innerCenterY - 0.5f) * scaleHeight`.
- Edit button (pen) is positioned strictly relative to the avatar circle perimeter at 45 degrees ($x = R \cdot \cos(45^\circ), y = R \cdot \sin(45^\circ)$) with a 32dp visible circle and 44dp touch target.

#### Layout Clearances & Exceptions Report
- **Screen Boundaries at 360dp width**:
  - Clearance to Left: Neon Spark 133.38dp, Crystal Arc 127.98dp, Golden Wings 129.52dp (zero collision).
  - Clearance to Right: Neon Spark 122.16dp, Crystal Arc 128.24dp, Golden Wings 117.53dp (zero collision).
  - Clearance to Title Row (SettingsScreen): $\ge 49.84\text{dp}$ clearance across all borders.
- **Clearance to Name Text / Edit Button**:
  - Pen button: sits at center $(207.58, 218.58)\text{dp}$ with touch bottom at $240.58\text{dp}$.
  - Solid rings: all borders maintain positive clearance to name text ($+3.61\text{dp}$ Neon Spark, $+4.26\text{dp}$ Crystal Arc, $+2.52\text{dp}$ Golden Wings).
  - **Exception 1 (Collision)**: The bottom tip of the decorative central feather on Golden Wings extends to $y = 242.75\text{dp}$, intruding by **2.75dp** into the 10dp top padding of the user name column. Because the name text container has 10dp top padding before the actual glyphs, it does not obscure the text glyphs, but this extent is noted as an exception.
  - **Exception 2 (Contrast)**: Golden Wings features dark bronze/gold lower accents whose luminance drops to 15–25 (average 65.1), which presents lower contrast against the dark midnight scrim (`#0A0D14`) compared to the high-contrast luminescence of Neon Spark (avg 98.6) and Crystal Arc (avg 83.7).
  - **Exception 3 (Visual Weight)**: Golden Wings ring body has a radial thickness of 8.06dp vs 6.32dp on Crystal Arc ($1.275\times$ thicker), and a total decorative width of 118dp vs 106.58dp on Neon Spark ($1.107\times$ wider).

## UI Mockup
```
+------------------------------------------+
| Status Bar (Insets)                      |
|                                          |
| Fotara / Notes / Settings        (O) (:) | <- ScreenHeader (38sp Medium)
| [Tagline / Reserved 20dp slot]           | <- 15sp Light or 20dp reserved spacer
|                                          |
| [Home] [Archive] [Work] [Study]   (+)    | <- WorkspaceTabBar (Identical baseline)
|                                          |
| [All] [Photos] [Docs] [Text] [Canvas]    | <- Expandable Filter Chips (Toggle via (O))
|                                          |
| === Oct 4, 2026 ======================== |
| [Card 1]                     [Card 2]    |
|                                          |
+------------------------------------------+
```

## Logic Notes
- State flow: `WorkspaceRepository._selectedWorkspaceId` -> `StateFlow<Long>` -> observed by `HomeViewModel` and `NotesViewModel`.
- When switching tabs on Notes screen, `WorkspaceRepository.selectWorkspace(id)` updates the flow, immediately updating Home screen state as well.
- When filter chips are toggled closed in Notes, `selectedFilter` resets to `ALL`, ensuring no invisible filters remain active.
- Profile border alignment: pure mathematical translation and scaling in draw phase (`graphicsLayer`) guarantees zero recomposition during frame rendering and zero movement of avatar or edit button when toggling borders.

## Risks & Mitigations
- *Risk*: Tagline height differences causing the workspace tab bar to jump vertically when switching between Home and Notes.
  *Mitigation*: Strict reserved spacer height (20dp) in `ScreenHeader` when `tagline == null`.
- *Risk*: Folder card top-right button clipped by card corner radius.
  *Mitigation*: Corner radius geometry verification showing the 20dp icon center sits well inside the 24dp circular arc boundary.
- *Risk*: Profile border hairline gap or clipping by parent layout.
  *Mitigation*: 1.5% inner radial overlap seam combined with `graphicsLayer { clip = false }` so decorative wings extend smoothly across container bounds.

## Dependencies
- Phase 27 & 28 Workspace foundation and dialogs.
- `WorkspaceRepository` singleton in `AppContainer`.

## Acceptance Criteria
- [x] Task 0 verification report completed with all required evidence.
- [x] Header metrics audit table recorded in phase doc.
- [x] `ScreenHeader` composable and `ScreenHeaderDefaults` implemented and used on Home, Notes, and Settings.
- [x] Workspace tab bar sits at exact identical vertical position on Home and Notes.
- [x] Settings main screen has no back arrow; system back navigates to Home; sub-screens keep back arrow.
- [x] Home and Notes three-dot menus have no "Settings" item and no dangling dividers.
- [x] Folder card three-dot button visual center positioned at ~20dp from top and right with touch target $\ge 44$dp and no overlap with tile or title.
- [x] Note cards inside folders audited and reported.
- [x] Notes subtitle removed; filter chips hidden by default; toggled via header search button with active tint.
- [x] Hiding filter chips resets filter type to All.
- [x] Notes screen includes `WorkspaceTabBar` with full tap, add, rename, delete, reorder support.
- [x] Selected workspace is shared across Home and Notes via `WorkspaceRepository`.
- [x] Notes list is filtered by selected workspace with dedicated empty state.
- [x] Task 4B: Per-border alpha measurement table recorded and stored in `ProfileBorder` model.
- [x] Task 4B: `ProfileAvatar` component created and applied to Settings header, Profile preview, and Border picker grid.
- [x] Task 4B: Avatar circle has fixed diameter $D$ invariant to border selection; edit pen button anchored at 45 degrees.
- [x] Task 4B: Unit tests for on-screen inner diameter ($\le 1\text{px}$ error), inner center ($\le 0.5\text{px}$ error), layout invariance, and component wiring.
- [x] Task 4B: Manual verification script recorded in phase doc.
- [x] 100% unit tests pass with zero regressions.

## Manual Verification Script (Task 4B)
1. **Side-by-side Border Cycling in Settings & Profile Screens**:
   - Open Settings screen. Tap pen edit button to open Profile Edit bottom sheet, then tap "Change Border".
   - Select each border in sequence: None -> Neon Spark -> Crystal Arc -> Golden Wings -> None.
   - Verify that the circular profile picture never changes size (fixed 78dp diameter) or shifts position.
   - Verify that the pen edit button stays fixed at the bottom-right 45-degree angle.
   - Verify that the user name, email, and cards below never jump or shift when switching borders.
2. **Screen Widths & Orientations**:
   - Test on 360dp compact display (e.g. Pixel 4a or 360x640 emulator). Verify decorations do not clip or cause horizontal scrolling.
   - Rotate to Landscape mode. Verify avatar and borders remain cleanly centered without overlapping navigation bars or title rows.
   - Test on tablet / large screen (e.g. 600dp+). Verify avatar and border stay proportioned and sharp.
3. **Avatar Image Variations**:
   - Test with default empty avatar (`Person` icon).
   - Test with small avatar image (e.g. 100x100 PNG).
   - Test with high-resolution avatar image (e.g. 4000x3000 JPEG).
   - Test with non-square original crop image. Verify `ContentScale.Crop` inside circular mask renders with zero distortion.
4. **Border Picker Visual Match**:
   - In Border Picker dialog, verify all 4 options (None, Neon Spark, Crystal Arc, Golden Wings) render with identical avatar sizes (52dp), aligned cards, and equal grid spacing. Verify that selected border matches what displays in Settings.

