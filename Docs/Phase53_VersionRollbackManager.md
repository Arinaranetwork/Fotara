# Phase 53 - VersionRollbackManager
## Goal
Provide a comprehensive, production-grade in-app Version Rollback and Release History engine in Fotara, allowing students and educators to browse previous official releases from GitHub, inspect past release notes, and securely rollback/downgrade to an earlier version with automatic data backup safeguards.

## Scope
- GitHub Releases history query and multi-release parsing engine in `UpdateManager.kt` and `UpdateVersionUtils.kt`.
- Semantic version categorization distinguishing newer update candidates vs past rollback candidates across Stable, Beta, and Alpha channels.
- Rollback APK download pipeline with real-time download progress, background notification progress, and FileProvider package installer launch.
- Pre-rollback student data safety dialog with 1-tap backup integration before package downgrade.
- Modern dark-navy "Version History & Rollback" UI section in `UpdateScreen.kt` and direct entry point in `SettingsScreen.kt`.
- Comprehensive unit tests verifying version comparison, rollback filtering, and channel sorting.

## Out Of Scope
- Cloud-based server rollback or server-side downgrade coordination (app remains 100% offline-first and connects directly to official GitHub releases).
- Automatic destructive wiping of user databases.

## Features
### GitHub Multi-Release History Parsing
<Scans all repository releases from GitHub API, parses assets for shippable APK binaries and SHA-256 files, and sorts candidates descending by semantic version.>

### Version Rollback Pipeline
<Allows downloading any past official release APK to local storage and launching Android Package Installer via FileProvider with background download notification.>

### Pre-Rollback Data Safety Guard
<Presents cautionary dialog alerting user of Android package downgrade risks and offering 1-tap backup export before proceeding with rollback.>

### Version History UI in UpdateScreen & Settings
<Expandable dark-navy section displaying past release cards, version tag, release date, channel badge (Alpha, Beta, Stable), release notes preview, and Rollback action.>

## UI Mockup
```
+-----------------------------------------------------------+
| App Update                              [Refresh] [Close] |
+-----------------------------------------------------------+
| [✓ Fotara is Up to Date]                                  |
| Current: v2.0.1  [ Alpha ]                                 |
+-----------------------------------------------------------+
| ▾ Version History & Rollback (5 Previous Releases)        |
|                                                           |
| +-------------------------------------------------------+ |
| | Fotara 2.0.0 Alpha                      [ Rollback ]  | |
| | Released 2026-10-10  |  [ Alpha ]                     | |
| | Local-First Academic Operating System (Space Vaults)  | |
| +-------------------------------------------------------+ |
| | Fotara 1.9.1 Alpha                      [ Rollback ]  | |
| | Released 2026-10-10  |  [ Alpha ]                     | |
| | Timetable fixes, gesture alignment & LaTeX renderer   | |
| +-------------------------------------------------------+ |
| | Fotara 1.9.0 Beta                       [ Rollback ]  | |
| | Released 2026-10-09  |  [ Beta ]                      | |
| | Home screen widgets & anti-procrastination alarms     | |
| +-------------------------------------------------------+ |
+-----------------------------------------------------------+
```

## Logic Notes
- `isOlderVersion(target, current)` evaluates semantic segment comparison `major.minor.patch` followed by channel priority (`Stable > Beta > Alpha`).
- Rollback releases are filtered such that `isOlderVersion(release.version, currentVersion) == true` and `release.downloadUrl != null`.
- Downloads are stored in app-scoped downloads cache (`Fotara_Rollback_<version>.apk`) and cleaned up on cancellation or skip.
- Download progress updates live Compose StateFlow (`0..100`).

## Risks
- Risk: Android Package Installer may reject in-place downgrade if `versionCode` is lower without uninstalling or user permission.
  -> Mitigation: Clear warning dialog explaining Android downgrade behavior and offering 1-tap notes backup before initiating installation.

## Dependencies
- `UpdateManager.kt`, `UpdateScreen.kt`, `UpdateVersionUtils.kt`, `SettingsScreen.kt`, `VersionInfo.kt`.

## Acceptance Criteria
- Unit tests verify `isOlderVersion`, channel resolution (Alpha/Beta/Stable), and rollback list sorting.
- `UpdateManager` parses and publishes `rollbackReleases` StateFlow with all past releases containing APKs.
- `UpdateScreen` displays the Version History & Rollback section with past versions, channel pills, and rollback buttons.
- Rollback confirmation dialog displays cautionary notice and triggers backup or rollback download.
- Full test suite passes with 0 regressions.
