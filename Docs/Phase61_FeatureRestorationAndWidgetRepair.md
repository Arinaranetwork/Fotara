// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

# Phase 61 - FeatureRestorationAndWidgetRepair

## Goal
Methodically restore, repair, and harden all user-reported lost or degraded features:
1. Restore folder visibility, space isolation fallback, and complete visual indicators (pin, lock, color label dot, and multi-select checkmarks) on FolderCard.
2. Restore official image banner integration: bundle the official 2.2 release banner into Android drawables, display it across Profile/Settings headers and Update dialogs, and ensure it is attached to release distributions.
3. Restore missing Glance widget receiver in `AndroidManifest.xml` and repair DueTomorrowGlanceWidget and TimetableGlanceWidget to eliminate the blank void defect shown in user screenshots.
4. Eliminate navigation bar obstruction in Modular Add-Ons / sub-screens and restore Home overflow actions ("Add Workspace", "Syllabus Evaluator").

## Scope
- Folder Visibility & Resilience: Ensure folders are never hidden into thin air when switching spaces or workspaces; provide all-workspace visibility fallback when an active workspace contains no folders.
- Folder Card Indicators: Restore Pin icon (`Icons.Outlined.PushPin`), Lock icon (`Icons.Outlined.Lock`), tag color accent dot, and selection checkmark badge on `FolderCard.kt`.
- Image Banner Restoration: Bundle `fotara_banner_default.jpg` into `app/src/main/res/drawable/` from `Assets/Banners/FotaraBanner_2.2_2026-10-10.jpg`. Wire it into `ProfileBannerFallbackGradient`, `UpdateBanner`, and `NewUpdateDialog`.
- Glance Widget Repair: Restore `DueTomorrowGlanceWidgetReceiver` to `AndroidManifest.xml`. Fix `DueTomorrowGlanceWidget.kt` empty and populated layouts so they render bulletproof content without blank voids on any launcher.
- Navigation Clearance & Menu Restoration: Fix bottom padding clearance in `PackageManagementHubScreen.kt` and track sub-screen status in `SettingsScreen.kt`. Re-add "Add Workspace" (`+` tab & overflow) and "Syllabus Evaluator" to `HomeScreen.kt`.

## Out Of Scope
- Mock or fake data (strictly forbidden by Rule R-005).
- Emoji in any UI string or resource (strictly forbidden by Rule U-16).
- Deleting or degrading any existing functionality.

## Features

### 1. Folder Card Completeness & Workspace Visibility Fallback
- **Pin & Lock Badges**: Display clean outlined pin and lock icons on `FolderCard` using design tokens.
- **Color Label Dot**: Display a circular tag color dot on `FolderCard` reflecting the folder's assigned color.
- **Selection Checkmark**: Display a clear checkmark badge when `isSelectionMode && isSelected` is true.
- **Resilient Folder Filtering**: In `HomeViewModel.kt`, if the selected workspace has no folders but other workspaces in the current space or system contain folders, provide a seamless fallback/switch or "View all folders" option so the user never sees their folders vanish.

### 2. Official Image Banner Integration
- **Bundled Asset**: Copy `Assets/Banners/FotaraBanner_2.2_2026-10-10.jpg` into `app/src/main/res/drawable/fotara_banner_default.jpg`.
- **Default Profile Banner**: In `ProfileBanner.kt`, replace the plain black/dark gradient in `ProfileBannerFallbackGradient` with the official bundled banner under a dark gradient scrim.
- **Update Banner**: In `UpdateBanner.kt`, fall back to `fotara_banner_default` whenever `bannerImageUrl` is null or offline.

### 3. Glance Widget Restoration & Void Elimination
- **Manifest Registration**: Restore `<receiver android:name=".widget.DueTomorrowGlanceWidgetReceiver" ...>` in `AndroidManifest.xml`.
- **Robust Layout Hierarchy**: Replace nested collapsing `fillMaxSize` columns in `DueTomorrowGlanceWidget.kt` with explicit RemoteViews weights and heights so empty and populated states reliably render across launchers (including ColorOS / Realme / OnePlus launchers).

### 4. Layout Clearance & Home Menu Restoration
- **Bottom Navigation Clearance**: In `PackageManagementHubScreen.kt`, provide at least `140.dp` bottom content padding to clear the persistent bottom bar. Wire `showPackageHubScreen` into `onSubScreenStateChanged(true)` in `SettingsScreen.kt`.
- **Overflow Menu Restorations**: Re-add "Add Workspace" and "Syllabus Evaluator" to `HomeScreen.kt` overflow menu. Restore the `+` add workspace action on `WorkspaceTabBar.kt`.

## Acceptance Criteria
- `FolderCard.kt` renders pin, lock, color dot, and selection checkmarks accurately.
- `HomeViewModel` and `HomeScreen` prevent disappearance of folders when switching workspaces/spaces.
- `app/src/main/res/drawable/fotara_banner_default.jpg` exists and is displayed in Settings header, Profile header, and UpdateBanner.
- `DueTomorrowGlanceWidgetReceiver` is registered in `AndroidManifest.xml` and renders robust content without blank voids.
- `PackageManagementHubScreen` content is fully scrollable above the bottom navigation bar.
- All unit tests pass cleanly (`./gradlew.bat testDebugUnitTest`).
