// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

# Phase 41 - Update Lifecycle And App Residue Cleaner

## Goal
Resolve update download lifecycle leaks by ensuring active downloads are immediately halted and purged when a version is skipped, during APK installation, or when a newer release arrives. Provide users with complete transparency and control over app-generated temporary disk residue in Settings under Storage with live byte calculation, a progress bar during deletion, and zero impact on coursework notes or databases.

## Scope
- `UpdateManager.kt`:
  - Terminate active download coroutine and delete partial (`.part`) files upon calling `setSkippedVersion(version)`.
  - Ensure `triggerApkInstall(context, release)` settles download state and cancels active background download jobs.
  - In `checkForUpdates()`, allow scanning even when downloading. If a newer release than the currently downloading version is detected:
    - Cancel active download job and clean up older `.part` and `.apk` files.
    - Set `_latestRelease.value = newerRelease`.
    - Set `_statusNotice.value` to indicate newer update was detected and older download cancelled.
    - Transition state to `UpdateState.UPDATE_AVAILABLE`.
  - Ensure `cancelDownload()` thoroughly cleans partial and orphaned download files.
- `UpdateScreen.kt` & `NewUpdateDialog.kt`:
  - Wire "Skip Version" action to invoke `updateManager.setSkippedVersion(release.version)` and `updateManager.cancelDownload()` before dismissal.
- App Residue Scanner & Cleaner (`AppResidueManager.kt` / `SettingsRepository.kt`):
  - Identify and quantify disposable app residue:
    - Leftover update APKs (`Fotara_Update_*.apk`) and incomplete downloads (`*.apk.part`) in download directories and cache.
    - Share intent staging files (`context.cacheDir/share_staging`).
    - Document and note export temporary files (`context.cacheDir/exports`).
    - DOCX media unpacking directories (`context.cacheDir/docx_media_*`).
    - Image editor crop and canvas staging buffers (`context.cacheDir/temp_crop_*`, `context.cacheDir/canvas_stage_*`).
    - General image decode and temporary cache files (`context.cacheDir`, `context.externalCacheDir`).
  - Strict preservation: NEVER delete or touch SQLite databases (`Fotara.db*`), coursework photo files (`files/photos/*`), profile pictures, workspace custom icons, or shared preferences.
  - Provide reactive residue calculation returning total bytes and file count.
  - Provide asynchronous cleaning routine with real-time `0.0f..1.0f` progress reporting and reclaimed byte sum.
- `SettingsViewModel.kt` & `SettingsUiState.kt`:
  - Add `appResidueBytes: Long = 0L`, `appResidueFileCount: Int = 0`, `isCleaningResidue: Boolean = false`, `residueCleanProgress: Float = 0f`.
  - Expose `cleanAppResidue()` and `refreshAppResidue()` methods.
  - Refresh residue breakdown on storage section entry and after cleanup completes.
- `SettingsScreen.kt`:
  - In `SettingsSection.STORAGE`, render a dedicated App Residue Cleaner Card:
    - Title: "App Trash & Temporary Residue"
    - Subtitle: "Leftover update packages, share staging, and cache files (notes are never deleted)"
    - Live size indicator: Total disk bytes formatted (e.g., `45.2 MB - 14 files`).
    - Clean button ("Clean Residue") with 48dp touch target, snapped 8dp radius, and dark-dominant token styling.
    - Real-time `LinearProgressIndicator` displayed while cleaning with smooth percentage progress.
    - Non-intrusive feedback toast/snackbar upon completion reporting reclaimed storage.
- `WhatsNewScreen.kt`:
  - Document 1.8.4 Beta update lifecycle and app residue cleaner additions.
- Version bump:
  - `app/build.gradle.kts` updated to `versionCode = 33`, `versionName = "1.8.4 Beta"`.

## Out Of Scope
- Deleting or modifying user coursework notes, trashed notes (`is_trashed = 1`), or folder structures.
- Deleting SQLite databases (`Fotara.db`, `Fotara.db-wal`, `Fotara.db-shm`).
- Altering GitHub API endpoints or APK signing keys.

## Features
### Feature 1: Update Download Cancellation and Collision Handling
- When a user skips an update or proceeds with an install, the download thread/job is immediately cancelled and temporary `.part` files are removed from disk.
- When an update check discovers a newer release while an older release is currently downloading, the older download is cancelled, its artifacts purged, and the UI transitions to offer the newer release.

### Feature 2: Settings App Residue Cleaner with Live Progress
- Displays total disk space consumed by app-created disposable residue in the Storage settings.
- Clicking "Clean Residue" initiates background file deletion with a live progress bar tracking percentage completion.
- Reclaims disk space and updates both the residue counter and the overall app storage breakdown.

## UI Mockup
```
Settings > Storage:
+-------------------------------------------------------------+
| App Trash & Temporary Residue                               |
| Leftover update packages, share staging, and cache files    |
|                                                             |
| Disk used: 48.2 MB (12 files)                               |
|                                                             |
| [====================        ] 70%                          | <- While cleaning
|                                                             |
| [ Clean Residue ]                                           |
+-------------------------------------------------------------+
```

## Logic Notes
- Thread safety: File deletion and disk size scanning execute on `Dispatchers.IO`.
- Safety boundary: Only designated disposable directories and temporary file prefixes are considered residue.

## Risks
- File lock during deletion -> Catch individual IOExceptions and continue deleting remaining files, reporting successfully deleted count.

## Dependencies
- `UpdateManager.kt`, `PhotoStorageManager.kt`, `SettingsRepository.kt`, `SettingsViewModel.kt`, `SettingsScreen.kt`.

## Acceptance Criteria
- Skipping an update cancels any active download and deletes `.apk.part` files.
- Installing an update cancels download jobs and cleans transient state.
- Newer release arrival cancels older download and transitions state cleanly.
- App residue cleaner accurately computes total disposable bytes and file count.
- App residue cleaner deletes temporary files with live progress bar reporting.
- Coursework notes, photos, and SQLite databases remain 100% untouched.
- All unit tests pass.
