<!-- --- Arinara Network (c) 2026 ---
Exclusive property of Arinara Network.
Unauthorized use, reproduction, distribution, or modification of this code,
in whole or in part, for any purpose, is strictly prohibited without prior
written consent from Arinara Network as sole legal owner of this codebase. -->

# Phase 25 - ProfilePngSupportAndUpdateModalPolish

## Goal
Resolve root causes preventing PNG profile pictures and banners from being saved and displayed, enforce true lossless PNG format preservation with alpha transparency support, stage picker URIs safely, and expand the startup update modal popup to be longer/taller with rich markdown rendering for headings, lists, bold text, and release notes formatting.

## Scope
- Root cause fix for PNG profile picture and banner saving: replace `WEBP_LOSSY` compression with universal lossless PNG encoding (`Bitmap.CompressFormat.PNG`) in `ProfileImageUtils.savePngAtomically`.
- Support PNG file naming and storage: save to `avatar.png` and `banner.png` in `SettingsRepository.kt` while maintaining backward compatibility with any legacy `.webp` files.
- Picker URI staging: when selecting an avatar or banner via `PickVisualMedia`, stage the input stream immediately to a local seekable cache file to eliminate `SecurityException`, stream reset issues, and `BitmapRegionDecoder` stream failures.
- Update modal dialog overhaul in `NewUpdateDialog.kt`:
  - Increase dialog dimensions: widen to 92% width and increase scrollable text viewport height to 260dp-440dp.
  - Rich Markdown rendering: replace `parseReleaseBullets` flat plain-text parser with `RichMarkdownColumn` supporting `#`, `##`, `###` headings, bullet lists, bold text, blockquotes, and code blocks.
  - Proper typographic hierarchy and theme styling matching Fotara design tokens.
- Unit and logic tests verifying PNG atomic saving, transparency preservation, and update dialog markdown parsing.

## Out Of Scope
- Third-party image formats outside standard Android image pickers.
- Remote server changes to update hosting.
- Database schema changes.

## Features
### PNG Profile Picture & Banner Preservation
- Preserves full 32-bit ARGB_8888 alpha transparency for custom avatars (logos, stickers, cutouts) and custom banner PNGs.
- `Bitmap.CompressFormat.PNG` guarantees zero encoder failures regardless of Android version or OEM libwebp quirks.
- Atomic replacement guarantees no zero-byte or corrupt files on interrupted writes.

### Picker URI Staging
- Staging ContentResolver stream into a local cache file right after selection ensures seekable file descriptor access for `BitmapRegionDecoder`.
- Eliminates multi-read permission expiration or ContentProvider lifecycle issues.

### Expanded & Formatted Update Modal Dialog
- Longer/taller dialog layout giving ample vertical height for multi-section release notes.
- Rich Markdown rendering for headings (`## What's New`, `### Patches`), bold text, bullet lists, and code blocks.

## UI Mockup
```
+-------------------------------------------------------+
|  [Landscape Banner Illustration / Update Header]  [X] |
|                                                       |
|              New Update 1.5.8 Beta                    |
+-------------------------------------------------------+
|  ## What's New                                        |
|  • **LinkIt Cluster Glow**: Corner glows converge...  |
|  • **Profile Picture Fix**: True PNG support with...  |
|                                                       |
|  ### Improvements                                     |
|  • Rich markdown in update notifications              |
|  • Expanded dialog height                             |
|                                                       |
|  [         Later         ]   [   Skip this version  ] |
+-------------------------------------------------------+
```

## Logic Notes
- `ProfileImageUtils.savePngAtomically(bitmap, targetFile, targetWidth, targetHeight)`:
  - Scales if requested, then compresses with `Bitmap.CompressFormat.PNG` to a `.tmp` file.
  - Verifies `tempFile.exists() && tempFile.length() > 0L`.
  - Atomically replaces `targetFile`.
- `SettingsRepository.saveProfileAvatar`:
  - Target: `avatar.png`. Cleans up legacy `avatar.webp`.
  - Emits updated `avatarPath` and `avatarUpdatedAt`.
- `SettingsRepository.saveProfileBanner`:
  - Target: `banner.png`. Cleans up legacy `banner.webp`.
  - Emits updated `bannerPath` and `bannerUpdatedAt`.
- `NewUpdateDialog`:
  - Uses `RichMarkdownColumn` with custom dark theme colors (headings in `#60A5FA`, body text in `White`, bullets with spacing).

## Risks
- Larger file size of PNG vs WebP -> 512x512 PNG avatar is typically 100-300KB, which is well within local device storage limits and loads instantaneously with Coil.

## Dependencies
- Phase 23 (Settings & Profile)
- Phase 24 (LinkIt Clusters & Profile Fixes)

## Acceptance Criteria
- PNG avatars and banners save reliably with transparency preserved.
- `SettingsRepository` saves and resolves `avatar.png` and `banner.png`.
- Staged URI prevents `BitmapRegionDecoder` stream errors.
- `ProfileCropScreen` crops directly from memory-resident `previewBitmap` via `ProfileImageUtils.cropNormalized`, completely eliminating native Skia region decoder failures, closed-stream exceptions, and coordinate transform bugs.
- `ProfileImageUtils.openStream` safely handles both `file://` and `content://` schemes across all Android API levels.
- `SettingsScreen` dismisses `ProfileEditBottomSheet` upon action selection for clean return to profile settings.
- `WhatsNewScreen` updated with 1.5.10 Beta release highlights and default notes.
- `NewUpdateDialog` renders headings (`#`, `##`, `###`), bold text, and bullets properly via `RichMarkdownColumn`.
- `NewUpdateDialog` has an expanded vertical viewport (up to 440dp) avoiding cramped scrolling.
- All unit tests pass with zero regressions.


