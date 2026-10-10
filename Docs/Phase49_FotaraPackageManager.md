# Phase 49 - FotaraPackageManager

## Goal
Implement the Fotara Package Manager & Modular Add-On Architecture (.fpkg format, signature verification, on-demand lifecycle manager, Settings management hub, and Just-In-Time interception dialog) to decouple heavy specialized capabilities (Real-Time Collab Canvas, Advanced Mathematical OCR, Predictive Exam Analytics), maintaining a lean base APK size under 25 MB while providing dynamic runtime modularity.

## Scope
- `com.arinara.fotara.feature.packages.model.FpkgManifest`: Core manifest model capturing package metadata, version constraints, permissions, sizes, and integrity signatures.
- `com.arinara.fotara.feature.packages.loader.FpkgSignatureVerifier`: Cryptographic integrity validation utilizing SHA-256 digests and mock Arinara Network authority key verification.
- `com.arinara.fotara.feature.packages.loader.FotaraPackageManager`: Lifecycle management in `context.filesDir/packages/`, package registry cataloging official modules, install/uninstall/toggle state flows, and disk quota accounting.
- `com.arinara.fotara.feature.packages.ui.PackageManagementHubScreen`: Settings screen component listing installed packages, storage reclaimed, and official catalog with one-tap install, toggle, and uninstall actions.
- `com.arinara.fotara.feature.packages.ui.JitFeatureInterceptionDialog`: Interactive modal dialog intercepting uninstalled feature invocations, presenting package details, simulated download progress, and immediate post-install execution callback.
- Comprehensive unit tests covering manifest serialization/logic, signature verification, and package lifecycle operations.

## Out Of Scope
- Remote network HTTP downloading from external CDN servers (mocked on-device simulated download stream).
- Native bytecode dynamic class loading via DexClassLoader on hardened SELinux environments (architecture simulates sandboxed asset expansion).

## Features
### FpkgManifest Definition
Immutable data record containing:
- `packageId`: Unique reverse-domain identifier (e.g. `com.arinara.fotara.pkg.collab`).
- `name`: Human-readable academic module name.
- `version`: Semantic version string.
- `minFotaraVersion`: Minimum host compatibility requirement.
- `description`: Academic function description.
- `iconKey`: Identifier for icon resolution.
- `sizeBytes`: On-disk package footprint in bytes.
- `permissions`: Declared capability scopes.
- `signatureSha256`: Hexadecimal SHA-256 fingerprint for tamper detection.
- `isEnabled`: Runtime active switch.
- `isInstalled`: Local filesystem presence flag.

### Cryptographic Signature & Integrity Verifier
- Computes SHA-256 digest over byte streams.
- Validates authentic payload against declared signature manifest.
- Confirms Arinara Network official authority key authorization header (`ARINARA_PKG_AUTH_v1`).

### Fotara Package Manager
- Maintains package registry containing standard official modules:
  - `com.arinara.fotara.pkg.collab` ("Real-Time Collab Canvas", 3.2 MB / 3,355,443 bytes)
  - `com.arinara.fotara.pkg.whiteboard_ocr` ("Advanced Mathematical OCR", 4.8 MB / 5,033,164 bytes)
  - `com.arinara.fotara.pkg.exam_analytics` ("Predictive Exam Analytics", 2.1 MB / 2,202,009 bytes)
- Manages storage in local directory `files/packages/`.
- Computes total reclaimed storage upon package uninstallation.
- StateFlow-driven reactive inventory updates for composable UI.

### Package Management Hub Screen
- Standardized `SettingsSubScreenHeader` with back navigation.
- Summary banner reporting total active packages and total disk footprint / reclaimed storage.
- List of installed packages with enable/disable switch and trash icon for uninstallation.
- Available official add-on catalog with download button and permission review.

### Just-In-Time (JIT) Interception Dialog
- Displayed when an uninstalled feature trigger is invoked.
- Shows package title, academic description, payload size, and required permissions.
- Animated progress bar during simulated download and signature verification.
- Immediate action callback to launch target feature once installation concludes.

## UI Mockup
```
+-------------------------------------------------------------+
| [<-] Modular Packages & Add-Ons                             |
| Dynamic storage optimization (<25MB Base APK)               |
+-------------------------------------------------------------+
| Storage Footprint Card                                      |
| Total Installed: 1 Modules | Storage In Use: 3.2 MB         |
| Reclaimed Storage: 6.9 MB                                   |
+-------------------------------------------------------------+
| INSTALLED PACKAGES (1)                                      |
| [Icon] Real-Time Collab Canvas (3.2 MB)                     |
|        Version 1.0.0 | Enabled: [Switch ON]     [Delete]    |
+-------------------------------------------------------------+
| AVAILABLE OFFICIAL ADD-ONS (2)                              |
| [Icon] Advanced Mathematical OCR (4.8 MB)                   |
|        LaTeX equations and matrix detection                 |
|        [ Install Add-On ]                                   |
|                                                             |
| [Icon] Predictive Exam Analytics (2.1 MB)                   |
|        Historical study scoring and cutoff forecasts        |
|        [ Install Add-On ]                                   |
+-------------------------------------------------------------+
```

## Logic Notes
- Thread safety: Package registry operations synchronized via mutex / thread-safe state flows.
- Persistence: Stored package records persisted to local JSON metadata or manifest storage files within package directory.
- Failure handling: Signature mismatch immediately aborts installation and emits `SecurityException` with cleanup of corrupted artifacts.

## Risks
- File system I/O errors during package extraction -> Mitigate by writing to temporary `.tmp` staging file before atomic rename to `.fpkg`.
- Inconsistent state when app process killed during installation -> Mitigate with startup sanitation deleting partial `.tmp` files.

## Dependencies
- Jetpack Compose, Material3, Coroutines Flow.

## Acceptance Criteria
- `FpkgManifest` represents package schema cleanly with all required fields.
- `FotaraPackageManager` provides `getInstalledPackages()`, `getAvailablePackages()`, `installPackage()`, `uninstallPackage()`, `togglePackage()`, and `isPackageInstalled()`.
- Official packages catalog contains Collab (3.2 MB), OCR (4.8 MB), and Exam Analytics (2.1 MB).
- `PackageManagementHubScreen` renders storage statistics, installed list, and official catalog with toggle and install actions according to Android.md tokens.
- `JitFeatureInterceptionDialog` displays feature installation modal with progress animation and immediate launch completion callback.
- Unit tests pass at 100% verifying all manifest, verifier, and package manager functionality.
