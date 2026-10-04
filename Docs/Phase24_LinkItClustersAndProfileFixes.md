<!-- --- Arinara Network (c) 2026 --- -->
<!-- Exclusive property of Arinara Network. -->
<!-- Unauthorized use, reproduction, distribution, or modification of this code, -->
<!-- in whole or in part, for any purpose, is strictly prohibited without prior -->
<!-- written consent from Arinara Network as sole legal owner of this codebase. -->

# Phase 24 - LinkItClustersAndProfileFixes

## Goal
Deliver release 1.5.8 Beta:
1. LinkIt cluster glow converging on the shared intersection for groups of 3 or 4 cards arranged in a 2x2 grid block, while preserving pairwise side glows for lines and non-cluster pairs.
2. Resolve profile picture and banner loading defects with complete root-cause verification, safe WebP encoding across API 24-36, reactive flow timestamp invalidation, and zero-replay error notifications.
3. Replace the profile crop step with a high-performance, aspect-locked fullscreen crop editor reading from original pixels via region decoding, featuring pan, corner resize, pinch zoom, reset, and clean visual guides.
4. Finalize release 1.5.8 Beta (versionCode 22).

## Scope
- LinkIt cluster math: extend pure function `computeGridGlowAnchors` to output `Set<GlowAnchor>` (supporting `TOP, BOTTOM, LEFT, RIGHT, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT`) across grid densities 2, 3, and 4.
- LinkIt rendering: render corner radial glow and arc stroke with the card's accent color clipped to the card shape, converging on the shared block intersection.
- Profile data layer repairs: verify WebP compression format for API 24-29 (`Bitmap.CompressFormat.WEBP`) vs API 30+ (`Bitmap.CompressFormat.WEBP_LOSSY`), check compress boolean and byte count, atomic rename with backup, `avatarUpdatedAt` and `bannerUpdatedAt` timestamps in `UserProfile` and SharedPreferences for instantaneous Coil cache invalidation.
- Shared Crop Editor: `CropBoxGeometry` pure geometry class (pan, corner resize, pinch zoom, min/max clamping, display-to-source mapping) + `ProfileCropScreen` composable with circular guide for avatar (1:1) and layout-matched ratio for banner, cancel/reset/save actions, and region decoding.
- Version bump to 1.5.8 Beta (versionCode 22), changelog entry, and verified release APK.

## Out Of Scope
- Database schema changes (profile and link groups remain on existing tables and SharedPreferences).
- Folder reordering and gesture drag sorting (cancelled).
- Workspaces and multi-scope search data models.
- Changes to `ViewportTransform.kt`, FTS virtual tables, or `fotara.fileprovider`.

## LinkIt Architecture & Cluster Rules
### 1. Group Definition & Maximum Size
- In Fotara, a LinkIt group is defined by a `link_group_id` foreign key referencing the `link_groups` SQLite table.
- A group links 2 to 4 items together (or legacy groups up to 4+ items). When trashing or unlinking reduces a group to <= 1 item, the group auto-dissolves.
- Cards in the same link group share visual connection indicators when laid out in the same grid.

### 2. Current Glow Drawing Mechanism
- Currently implemented in `LinkItGlow.kt` via `Modifier.linkItCornerGlow`.
- Uses `drawWithCache` to pre-calculate radial gradient brushes and corner arc/side line strokes without runtime recomposition overhead.
- Supports cardinal side edges (`TopEdge, BottomEdge, LeftEdge, RightEdge`) and diagonal corners (`TopLeft, TopRight, BottomLeft, BottomRight`).
- Cards call `computeFolderGlowOrientations` (Home) or `computeGridFacingGlowCorners` (FolderDetail), which previously returned a single `GlowCorner?` per card.

### 3. Cluster Glow Rules (v1.5.8)
For cards currently laid out in the same grid of density $C \in \{2, 3, 4\}$:
1. **Pairwise Rule**: Exactly two members that are horizontal or vertical neighbors glow at the MIDDLE of the facing sides (`LEFT/RIGHT` or `TOP/BOTTOM`).
2. **Cluster Rule**: For every $2 \times 2$ block of grid cells $[r, r+1] \times [c, c+1]$ in which 3 or 4 cells contain members of the same link group, each member in that block glows at its CORNER touching the block center point:
   - Cell $(r, c)$ (top-left) $\rightarrow$ `BOTTOM_RIGHT`
   - Cell $(r, c+1)$ (top-right) $\rightarrow$ `BOTTOM_LEFT`
   - Cell $(r+1, c)$ (bottom-left) $\rightarrow$ `TOP_RIGHT`
   - Cell $(r+1, c+1)$ (bottom-right) $\rightarrow$ `TOP_LEFT`
   - An empty fourth cell in an L-shaped 3-cluster receives no glow.
3. **Exclusion & Multiple Anchors**:
   - Any adjacent pair whose members are both inside a qualifying $2 \times 2$ block receives ONLY the corner glow, suppressing the side glow.
   - Any adjacent pair not inside a qualifying $2 \times 2$ block retains its pairwise middle-of-side glow.
   - A card can possess multiple anchors (e.g. in 5+ member clusters spanning multiple blocks or adjacent lines).
   - Groups forming straight lines (3 in a row, 4 in a column) retain pairwise side glows.

## Root Cause Analysis: Profile Picture & Banner Not Loading
1. **Empty Top Bar Save Lambda**: In `ProfileCropScreen.kt`, the visible "Save" button in the top bar had an empty `onClick = { // Will trigger crop save callback }` lambda. The user tapped Save, but the callback never executed; pressing Back discarded the crop.
2. **Unchecked WebP Compression Return**: `ProfileImageUtils.saveWebpAtomically` did not check the boolean result of `Bitmap.compress()`. If compression returned false, a zero-byte file was flushed and substituted as the active file.
3. **WebP Compatibility across API Levels**: API 24-29 requires `Bitmap.CompressFormat.WEBP`, while API 30+ uses `Bitmap.CompressFormat.WEBP_LOSSY`. Zero-byte detection and explicit format branching are mandatory.
4. **Coil Cache Staleness on In-Place File Overwrite**: When `avatar.webp` or `banner.webp` was replaced, the path string remained unchanged. Coil's cache key was insensitive to rapid file replacements. Introducing explicit `avatarUpdatedAt` and `bannerUpdatedAt` timestamps in `UserProfile` ensures instantaneous cache invalidation upon saving.

## Crop Editor Architecture (Task 3)
- **Geometry Core**: `CropBoxGeometry.kt` handles pure aspect-ratio-locked math:
  - Initial box: maximum centered rectangle matching target aspect ratio inside displayed image bounds.
  - Drag pan: translates box clamped strictly within displayed image bounds.
  - Corner resize: anchors the opposite corner, scales width and height proportionally to locked aspect ratio, clamped within bounds and above minimum size (64dp / 64 source pixels).
  - Two-finger pinch: scales box around center, clamped to image bounds.
  - Source mapping: maps display crop rectangle back to original source pixels.
- **Original Pixel Decoding**: Uses `BitmapRegionDecoder` to extract the full-resolution cropped sub-rectangle directly from original image stream/file, preventing quality degradation and avoiding high-resolution full-image allocations.
- **Output Formats**:
  - Avatar: 512x512 WebP (1:1 with circular guide)
  - Banner: max 1080px wide WebP maintaining layout aspect ratio.

## Acceptance Criteria
- [x] `GlowAnchor` enum defined (`TOP, BOTTOM, LEFT, RIGHT, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT`).
- [x] `computeGridGlowAnchors` outputs `Map<Long, Set<GlowAnchor>>` correctly solving 2-pairs, 3-member L-clusters (in all 4 orientations), 4-member full 2x2 blocks, 3-in-a-row, 4-in-a-column, and 5+ member combinations.
- [x] Home folder cards and all FolderDetail cards (Photo, Group, PDF, DOCX, Text, Canvas) wired to receive and render `Set<GlowAnchor>`.
- [x] Profile picture and banner save, reload, and render immediately in ProfileScreen preview and Settings header.
- [x] WebP encoding verified across API 24-36 with checked boolean, non-zero file size, and atomic rename.
- [x] Fullscreen crop editor with locked aspect ratio, circular avatar guide, layout banner ratio, corner drag resize, pan, pinch zoom, reset, and original pixel region decoding.
- [x] All UI strings in `strings.xml`.
- [x] Full test suite passes 100% and release APK built with `versionName "1.5.8 Beta"` and `versionCode 22`.
