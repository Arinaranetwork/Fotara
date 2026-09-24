# Rules - Fotara
Last Updated: 2026-09-23

## Engineering
### R-001 - Strict Layered Architecture
Business logic, data access, and presentation must remain strictly separated. UI composables and activities must only observe ViewModels or state holders, and must never query Room DAOs or execute ML Kit operations directly. Reason: Guarantees testability, isolation, and long-term maintainability. Example violation: Invoking `fotaraDatabase.folderDao().insertFolder(...)` or `TextRecognition.getClient(...)` inside a Composable function.

### R-002 - Android Framework Naming and Placement Conventions
Framework-mandated files and directories (`app/`, `gradle/`, `build.gradle.kts`, `settings.gradle.kts`, `AndroidManifest.xml`, Kotlin packages under `com.arinara.fotara`) are exempt from root PascalCase naming where build tooling requires standard Gradle conventions. Project-level folders created outside the Android module (`/Docs`, `/Changelog`, `/Assets`, `/Output`) must strictly use PascalCase. Reason: Prevents Gradle and Android build tooling breakages while preserving Arinara repository discipline. Example violation: Renaming `build.gradle.kts` to `BuildGradle.kts` or `app` to `App` causing build failures.

### R-003 - Offline-First Local Operations
All photo storage, metadata indexing, on-device OCR, and keyword search must function completely offline with zero mandatory network connectivity. Any failure in OCR or metadata parsing must be caught, surfaced with user-friendly error states, and recorded in logs without swallowing exceptions. Reason: Students frequently study in lecture halls and libraries without reliable internet. Example violation: Using an external cloud vision API or wrapping OCR in an empty `catch (e: Exception) {}`.

## Design
### R-004 - Folder Card Design System Realization
Folder cards must strictly reproduce the brand aesthetic established in the visual reference (`UI/photo_6167897522295214567_y.jpg`): deep dark navy screen background (`#03071E`), rounded rectangle cards with asymmetrical cream/ivory top tabs (`#EAE3D2`), vibrant royal blue body (`#0B1BE0`), high-contrast bold italic title typography, and top-right color label indicators. Reason: Preserves distinctive brand identity and visual hierarchy. Example violation: Rendering generic rectangular Material cards or using standard gray backgrounds.

### R-005 - Zero Placeholder Shipped Work
All shipped screens and components must provide complete empty, loading, error, and populated states. No dummy mock data, TODOs, or fake stubs in production source code (`app/src/main/`). Reason: Ensures production-grade readiness at every milestone. Example violation: Leaving `// TODO: implement OCR` or hardcoding sample folder lists in production viewmodels.

## Release
### R-006 - Release Artifact Discipline
Shippable release artifacts must be packaged and copied into `/Output/Release/` adhering to the standard naming template `Fotara_<x.y.z>[_Beta][_<Platform>_<Arch>].apk`. Intermediate build outputs in `build/` or `app/build/` must remain gitignored. Reason: Ensures clean, reproducible release tracking. Example violation: Copying unversioned `app-debug.apk` directly to repository root.
