# Phase 22 - VerificationAndRepair157

## Goal
Execute a comprehensive audit of Release 1.5.7 Beta deliverables. Repair confirmed missing and regressed behaviors with minimal diffs: LinkIt facing edge glow across document and note cards in 2, 3, and 4 column grids; the anchored dropdown menu for Home folder cards matching user specification; one-shot snackbar notifications in Settings without replay; and PDF/DOCX card badge, preview, and pluralization formatting.

## Scope
1. **Audit Table**: Full assessment of the 10 release items with file and function evidence, call sites, and regression root causes.
2. **Repair A - LinkIt Facing Edge Glow**:
   - Provide a single pure function `computeGridFacingGlowCorners` supporting 2, 3, and 4 columns.
   - For same-row neighbors (`dRow == 0`), orient glow to facing edges (`RightEdge` for left card, `LeftEdge` for right card).
   - For vertical neighbors (`dCol == 0`), orient glow to facing edges (`BottomEdge` for top card, `TopEdge` for bottom card).
   - Maintain diagonal and row-wrap behavior for other pairs.
   - Wire computed glow orientations into all card composables (`DetailDocumentCard`, `DetailPhotoCard`, `DetailGroupCard`, `DetailTextNoteCard`, `DetailCanvasCard`, and `FolderCard`).
3. **Repair B - Home Folder Card Anchored Dropdown Menu**:
   - Replace 3-dot trigger behavior in `FolderCard` with an anchored `DropdownMenu` positioned right-aligned beneath the 3-dot `IconButton`.
   - Include exact items: "Pin to Top" / "Unpin from Top", "Rename Folder", "Select", horizontal divider, "Lock Folder (PIN)" / "Remove Folder Lock", "Unlink Folder" (crimson, conditional on link group), and "Move to Trash" (crimson).
   - Preserve long-press card behavior opening the existing dialog variant.
4. **Repair C - Settings One-Shot Notification Consumption**:
   - Ensure feedback messages in `SettingsViewModel` / `SettingsScreen` are consumed as one-shot events.
   - Clear feedback state immediately upon receipt so navigating tabs or rotating screen never replays the snackbar.
   - Keep `SnackbarHost` positioned safely above the floating bottom navigation bar overlay.
5. **Item 8 Repair - PDF & DOCX Card Enhancements**:
   - Supply document page mappings through `DocumentRepository` to `FolderDetailViewModel` so page 1 previews render on PDF cards.
   - DOCX cards: display format badge without page count, and format bottom row with date-only (`dateStr`).
   - PDF cards: render pluralized count string ("1 page" vs "%d pages").
   - Extract user-facing strings into `strings.xml`.
6. **Item 6 Repair - Invert Selection in Both Select Modes**:
   - Home screen folder multi-select mode: add `invertFolderSelection()` in `HomeViewModel` and an Invert Selection action button in `HomeScreen`'s top action bar.
   - Folder detail batch select mode: add `invertSelection()` in `FolderDetailViewModel` and an Invert Selection action button in `FolderDetailScreen`'s top action bar.
   - Subfolder multi-select mode: add `selectAllSubfolders()` and `invertSubfolderSelection()` in `FolderDetailViewModel` with action buttons in `FolderDetailScreen`.
7. **Settings Notification Hardening**:
   - Back `SettingsViewModel` feedback events with a buffered `Channel<String>` exposed via `eventFlow`.
   - `SettingsScreen` collects from `eventFlow` directly in `LaunchedEffect(viewModel)`, ensuring zero message replay on tab switches or screen rotations.
   - Clear messages immediately upon arrival across `HomeScreen` and `FolderDetailScreen`.

## Out Of Scope
- Folder reordering by long-press and drag (explicitly cancelled).
- Shared progress engine rewrite.
- Schema alterations or binary format modifications.
- Modifications to `ViewportTransform.kt` or FTS database schemas.


## Features
### Audit Table
| # | Item | Status | File & Function Evidence | Call Sites | Why Missing / Regressed (Commit Citation) |
|---|------|--------|--------------------------|------------|--------------------------------------------|
| 1 | Version "1.5.7 Beta", versionCode 21 | Implemented | `app/build.gradle.kts:14-15` (`versionCode = 21`, `versionName = "1.5.7 Beta"`), `output-metadata.json` | Gradle build toolchain | Confirmed present in config and release APK artifact |
| 2 | Canvas element stacking, image auto-select, image proportional scaling | Implemented | `CanvasDatabaseSchema.kt:145` (`ORDER BY z_index ASC, rowid ASC`), `CanvasRenderer.kt:316`, `TileCacheManager.kt:215`, `CanvasViewModel.kt:977-986`, `TransformHandlesMath.kt:394` | `CanvasViewModel.insertImage()`, `CanvasToolController.handleTransformGesture()` | Implemented in Phase 21 |
| 3 | Tool options panel layout (48dp chips, wrap content, insets) | Implemented | `CanvasScreen.kt:677, 682, 792, 837` (`.heightIn(max = maxPanelHeightDp)`, `.verticalScroll`, fixed 48dp chips) | Canvas options panel | Implemented in Phase 21 |
| 4 | Shared bottom-overlay height for padding, snackbars, and Notes (+) | Implemented | `HomeBottomNavBar.kt:52` (`LocalBottomOverlayPadding`), `HomeScreen.kt:306`, `SettingsScreen.kt:189`, `NotesScreen.kt:135, 260` | `HomeScreen`, `SettingsScreen`, `NotesScreen` | Implemented in Phase 21 |
| 5 | Overflow menus anchored to trigger | Regressed (Home card) | `NotesScreen.kt:674` (anchored), `HomeScreen.kt:545` (anchored), `HomeScreen.kt:728, 863` (centered `AlertDialog`) | Home screen folder cards | Introduced in commit `ba48ebe` ("Feat: Redesign home screen..."), where tapping 3-dots set `activeContextFolder`, launching a centered dialog instead of an anchored dropdown |
| 6 | Invert selection button in both select modes | Missing | No implementation in `HomeScreen.kt`, `FolderDetailScreen.kt`, or `NotesScreen.kt` | None | Never applied to repository |
| 7 | LinkIt facing glow for PDF, DOCX, photo, and folder pairs | Regressed (Notes & Docs) | `FolderDetailScreen.kt:3618, 3829, 3980`, `DetailTextNoteCard.kt:82`, `DetailCanvasCard.kt:86` hardcoded `GlowCorner.BottomLeft`; `LinkItGlow.kt:70` used legacy corners for same-row folders | Card composables | Grid neighbor orientations were never calculated in `FolderDetailScreen`; legacy `computeFolderGlowOrientations` in commit `ba48ebe` lacked edge glow mapping |
| 8 | PDF and DOCX cards: page 1 preview, DOCX badge without count, date-only bottom row, plural strings | Regressed | `FolderDetailViewModel.kt:210` passes `pages = emptyList()`; `FolderDetailScreen.kt:3686` displays `"$pageCount p"` for DOCX; line 3761 displays `"$pageCount pages · $dateStr"` for DOCX and non-pluralized PDF | `FolderDetailScreen` | Commit `ba48ebe` omitted page flow loading in `FolderDetailViewModel` and used placeholder strings |
| 9 | Shared progress mechanism for six long operations | Missing | Bespoke flags in `SettingsViewModel.kt` and `CombineManager.kt` | None | Never applied to repository |
| 10 | Release 1.5.7 changelog and Progress.md entries | Implemented | `Changelog/Changelog_1.5.md:106-114`, `Docs/Progress.md` Phase 21 | Docs system | Documented during Phase 21 |

### Logic Notes
1. **LinkIt Pure Facing Function**:
   `computeGridFacingGlowCorners(items: List<Pair<Long, Long?>>, columns: Int): Map<Long, GlowCorner>`
   - Index lookup resolves grid position: `row = index / columns`, `col = index % columns`.
   - For linked partner within `abs(dRow) <= 1 && abs(dCol) <= 1`:
     - Same row (`dRow == 0`): `colA < colB -> GlowCorner.RightEdge`, else `GlowCorner.LeftEdge`.
     - Same column (`dCol == 0`): `rowA < rowB -> GlowCorner.BottomEdge`, else `GlowCorner.TopEdge`.
     - Across-row wrapping and diagonals retain corner mappings.
2. **Folder Dropdown Menu**:
   - Anchored via `Box` wrapping the 3-dot `IconButton` on `FolderCard`.
   - `DropdownMenu(expanded = isMenuExpanded, onDismissRequest = { isMenuExpanded = false }, modifier = Modifier.background(HomeCardSurface).border(1.dp, HomeCardBorder, RoundedCornerShape(12.dp)))`.
   - Menu dismisses and triggers appropriate callback upon item selection.
   - Long press on folder card continues to trigger `onCardLongClick`, preserving existing behavior.
3. **One-Shot Settings Snackbar**:
   - `SettingsViewModel` utilizes a buffered `Channel<String>` for feedback events.
   - `SettingsScreen` collects from `feedbackEvents` or consumes `uiState.feedbackMessage` immediately before calling `showSnackbar`.
4. **PDF/DOCX Card Representation**:
   - `DocumentRepository` supplies `getAllDocumentPages(): Flow<Map<Long, List<DocumentPage>>>`.
   - `FolderDetailViewModel` associates documents with their real `DocumentPage` list.
   - `DetailDocumentCard` extracts first page bitmap for PDF; suppresses page count on DOCX; renders date-only footer on DOCX; formats singular/plural pages for PDF.

## Acceptance Criteria
- [ ] LinkIt facing glow renders on facing edges (`RightEdge`/`LeftEdge`) for pairs in the same row, and `BottomEdge`/`TopEdge` for vertical neighbors across PDF, DOCX, Photo, Group, TextNote, CanvasNote cards, and Home Folder cards.
- [ ] Grid densities of 2, 3, and 4 columns compute accurate facing sides.
- [ ] Home folder card 3-dot button opens the anchored `DropdownMenu` matching UI specification.
- [ ] Card long-press behavior remains functional.
- [ ] Settings notifications are consumed once without repeating on tab re-entry or rotation.
- [ ] PDF cards display page 1 thumbnail preview when available.
- [ ] DOCX cards show badge without page count and date-only bottom row.
- [ ] PDF card footer uses pluralized string ("1 page" vs "N pages").
- [ ] All new strings reside in `strings.xml`.
- [ ] All unit tests pass with zero regressions.
