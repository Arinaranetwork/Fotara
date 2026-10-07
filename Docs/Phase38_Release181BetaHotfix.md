// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

# Phase 38 - Release 1.8.1 Beta Hotfix

## Goal
Resolve three critical regressions/glitches discovered in release 1.8.0 Beta relating to the user profile and banner workflow:
1. Prevent unintended fallback/flash to Home screen when completing photo selection in the system photo picker before reaching the crop editor.
2. Fix unscaled tiny banner thumbnail rendering in the top-left corner of the 230dp banner container for animated GIF banners.
3. Eliminate banner area blinking/flashing when opening and returning from Profile settings.

## Scope
- `HomeScreen.kt`: Persist `selectedNavTab` state using `rememberSaveable` to withstand background memory pressure and activity recreation during external Photo Picker activity launches.
- `SettingsScreen.kt`: Persist `activeSection` state using `rememberSaveable` so returning from external intents keeps the active sub-screen intact.
- `ProfileCropScreen.kt`: Fast image loading without frame stutter or delays.
- `ProfileBanner.kt`:
  - Enhance `AnimatedBannerView` to recompute crop parameters on `onSizeChanged(w, h, ...)` and `onDraw(canvas)` fallback so GIF banners fill the container according to `BannerCropTransform` / `ContentScale.Crop`.
  - Provide `ProfileBannerMemoryCache` to retain decoded drawables / first frames across composable unmounts.
  - Disable Coil crossfade (`crossfade(false)`) on static banner images to prevent crossfade flashing on back navigation.
- Update `Changelog_1.8.md` and `WhatsNewScreen.kt`.
- Bump version to `1.8.1 Beta` (`versionCode = 30`).

## Out Of Scope
- Database schema changes (remains SQLite schema v18).
- New permissions or network dependencies.
- Changes to unrelated viewers or canvas features.

## Root Cause Audits
### Issue 1: Navigation Drop to Home on Photo Pick
- **Observed Behavior**: User in Profile settings triggers system photo picker (or document picker). Upon selecting a photo, the app briefly flashes/navigates to Home screen before jumping into the crop editor.
- **Root Cause**: `selectedNavTab` in `HomeScreen.kt` was declared as `remember { mutableStateOf(HomeNavTab.HOME) }`, and `activeSection` in `SettingsScreen.kt` was declared as `remember { mutableStateOf<SettingsSection?>(null) }`. Because external pickers launch a separate system Activity, Android background lifecycle / memory reclamation destroyed `MainActivity`. On recreation, `selectedNavTab` reset to `HomeNavTab.HOME`, drawing the Home screen for initial frame(s) before activity results restored navigation.
- **Fix**: Convert `selectedNavTab` and `activeSection` to `rememberSaveable`.

### Issue 2: Small Banner / Unscaled Top-Left Image
- **Observed Behavior**: Cropped GIF banner appears as a small unscaled rectangular thumbnail in the top-left corner of the 230dp banner container.
- **Root Cause**: `AnimatedBannerView` in `ProfileBanner.kt` relied on `update` lambda in `AndroidView` to compute `cropParams`. During the initial `update` call, `view.width == 0` and `view.height == 0`, leaving `cropParams = null`. `AnimatedBannerView` lacked `onSizeChanged()`. In `onDraw()`, when `cropParams == null`, it drew unscaled intrinsic pixels directly at `(0, 0)`.
- **Fix**: Recompute `cropParams` in `onSizeChanged(w, h)` and implement on-the-fly computation and `ContentScale.Crop` centering fallback inside `onDraw(canvas)`.

### Issue 3: Banner Blinking on Navigation to Profile Settings and Back
- **Observed Behavior**: Navigating between Settings and Profile sub-screen causes a visible dark blink/flash in the 230dp banner container.
- **Root Cause**:
  1. For static images, Coil was configured with `.crossfade(true)`, causing an alpha fade on each remount.
  2. For GIF banners, `animatedDrawable` initialized to `null` while `ImageDecoder` ran on `Dispatchers.IO` in `LaunchedEffect`, causing `ProfileBannerFallbackGradient` to render for 1-3 frames before the image popped in.
- **Fix**: Set `.crossfade(false)` in Coil request and implement `ProfileBannerMemoryCache` to provide immediate synchronous memory availability of the first frame / drawable, eliminating the fallback gradient flash.

## Acceptance Criteria
- [x] Photo picker return preserves Settings and Profile navigation state instantly with zero fallback to Home screen.
- [x] GIF and static banners fully scale, clip, and center across the 230dp container with zero unscaled top-left rendering.
- [x] Navigating between Settings and Profile sub-screen produces zero blink, flash, or fallback gradient artifact.
- [x] Version bumped to 1.8.1 Beta (versionCode 30).
- [x] 100% of unit tests pass.
