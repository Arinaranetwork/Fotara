# Phase 55 - AndroidVersionDowngradeEngine
## Goal
Implement a robust, production-grade Android Version Downgrade Engine in Fotara that solves Android's native `INSTALL_FAILED_VERSION_DOWNGRADE` restriction through dual pathways: 1-tap in-place downgrade via Shizuku Shell service (`pm install -d -r`), and an automated zero-data-loss Guided Reinstall workflow (`hasFragileUserData`, auto-vault backup, public Downloads staging, persistent install notification, and ADB helper).

## Scope
- Integrate Shizuku API (`dev.rikka.shizuku:api` & `dev.rikka.shizuku:provider`) to detect Shizuku daemon, request shell permissions, and execute `pm install -d -r` directly for seamless in-place downgrades without uninstalling.
- Enable `android:hasFragileUserData="true"` in `AndroidManifest.xml` so Android 10+ natively prompts users to retain application databases and coursework files upon uninstallation.
- Automated Pre-Rollback Vault Backup: automatically export coursework databases and settings to external public storage (`Downloads/Fotara_Rollback_Vault_<version>_<timestamp>.json`) prior to downgrade.
- Public Staging Pipeline: stage target rollback APK to public storage (`Downloads/Fotara_<version>.apk`) so it survives application uninstallation.
- Sticky Post-Uninstall Notification: post a persistent system notification before uninstalling that allows launching the downloaded APK installer immediately after uninstall.
- Interactive Downgrade Wizard in `UpdateScreen.kt`: clear multi-path modal showing Shizuku 1-tap option, Guided Safe Reinstall option, and 1-tap copyable `adb install -d -r` terminal command.
- Build verification and automated unit test coverage.

## Out Of Scope
- Rooting non-rooted devices or forcing system partition modifications.
- Modifying Google Play Store installation channels.

## Features
### Shizuku Privileged Shell Downgrade
- Capability Detection: Query Shizuku Binder status (`Shizuku.pingBinder()`).
- Permission Acquisition: Request Shizuku permission with live grant state callback.
- Privileged Execution: Run `pm install -d -r "<stagedApkPath>"` via Shizuku process execution, capturing stdout/stderr and surfacing status to the student.

### Guided Safe Reinstall Downgrade (Universal Fallback)
- For users without Shizuku/ADB:
  1. Automated Vault Backup: Silently exports entire SQLite notes, folders, and preferences into public Downloads folder.
  2. Public APK Copy: Copies the target rollback APK from internal cache into public `Downloads/` directory.
  3. Sticky Notification: Dispatches a high-priority system notification (`Fotara Rollback Ready`) with PendingIntent pointing to the staged APK in Downloads.
  4. Uninstall Trigger: Launches `Intent(Intent.ACTION_DELETE, Uri.parse("package:com.arinara.fotara"))` prompting user to uninstall while checking "Keep app data".
  5. One-Tap Reinstall: After uninstall, tapping the sticky notification opens the Package Installer to install the target version cleanly.

### ADB Wireless / PC Helper
- One-tap button to copy the exact `adb install -d -r Fotara_<version>.apk` command to the system clipboard for students utilizing USB or Wireless ADB debugging.

## UI Mockup
```
+-------------------------------------------------------------+
|  [<-] Version Rollback Options: Fotara 2.0.1 Alpha          |
+-------------------------------------------------------------+
|                                                             |
|  Android OS Security Notice:                                |
|  Android blocks installing older apps over newer apps       |
|  directly without downgrade authorization. Choose a method: |
|                                                             |
|  +-------------------------------------------------------+  |
|  | [⚡] 1-Tap Downgrade via Shizuku        (Recommended) |  |
|  |     In-place downgrade without uninstalling.          |  |
|  |     Status: [Shizuku Active / Ready]                  |  |
|  |     [ Install via Shizuku (-d) ]                      |  |
|  +-------------------------------------------------------+  |
|                                                             |
|  +-------------------------------------------------------+  |
|  | [🛡️] Guided Safe Reinstall (Zero Data Loss)           |  |
|  |     1. Auto-Backup saved to Downloads                 |  |
|  |     2. APK copied to Downloads                        |  |
|  |     3. Uninstall current app (Keep App Data checked)  |  |
|  |     [ Prepare & Uninstall ]                           |  |
|  +-------------------------------------------------------+  |
|                                                             |
|  +-------------------------------------------------------+  |
|  | [💻] ADB Terminal Command                             |  |
|  |     adb install -d -r Fotara_2.0.1_Alpha.apk          |  |
|  |     [ Copy ADB Command ]                              |  |
|  +-------------------------------------------------------+  |
+-------------------------------------------------------------+
```

## Logic Notes
- `DowngradeManager`: Coordinates Shizuku detection, public staging, automated backup, notification dispatch, and intent launching.
- Fallback gracefully when Shizuku is not installed or permission is denied.

## Risks
- User denies "Keep app data" on uninstall -> Mitigated by automatic pre-export of vault backup to `Downloads/`.
- FileProvider permission expiration -> Staging to public external Downloads directory ensures file accessibility across process restarts.

## Dependencies
- Phase 53 (`VersionRollbackManager`), Phase 54 (`FriendsUiThemeAndWindowInsetsPolish`).

## Acceptance Criteria
- `android:hasFragileUserData="true"` present in `AndroidManifest.xml`.
- Shizuku integration detects daemon status and executes `pm install -d -r`.
- Target APK and full notes backup are staged to public Downloads storage.
- Persistent notification posted to allow installing target APK post-uninstall.
- All unit tests pass with zero regressions.
- Shippable APK `Fotara_2.2.0_Alpha.apk` generated.
