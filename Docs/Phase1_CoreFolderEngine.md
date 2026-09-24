# Phase 1 - CoreFolderEngine

## Goal
Establish the core foundation for Fotara: the Android project scaffolding, Room database architecture for folders/subfolders/photos, the custom folder card UI design system faithful to the reference mockup, and home screen folder management.

## Scope
- Android project scaffolding with Kotlin DSL, Jetpack Compose, and Room Database.
- Brand design system: midnight navy background, custom ivory-tabbed royal blue folder card shape, bold italic typography, floating bottom dock.
- Data entities and DAOs: `FolderEntity`, `SubfolderEntity`, `PhotoEntity` with full CRUD support.
- Repositories and ViewModels: `FolderRepository`, `PhotoRepository`, `HomeViewModel`, `FolderDetailViewModel`.
- Home Screen: 2-column grid of subject folder cards, "+New" folder card, search bar / quick filter dock.
- Folder creation dialog with color label selection and pinning toggle.
- Folder detail screen with horizontal subfolder tabs and photo grid placeholder state.
- Offline-first architecture with unit tests verifying database operations.

## Out Of Scope
- Full ML Kit on-device text recognition pipeline execution (prepared and structured in Phase 1, fully wired with camera capture stream in Phase 2).
- PDF folder export and batch export utilities (Phase 3).
- Deadline notifications via Android AlarmManager/WorkManager (Phase 2).

## Features
### Folder System & Card UI
Displays folders in a 2-column grid. Each card renders an ivory/cream top tab, royal blue card body, bold italic subject title, and a color tag in the top-right corner.
- **Empty state**: Clean dashboard showing the "+New" folder card inviting the student to create their first subject.
- **Populated state**: Grid of subject folders sorted by pinned status followed by recent activity.
- **Error state**: Transient snackbar or retry state if folder persistence encounters an SQLite exception.

### "+New" Subject Folder Creation
Allows the student to input a folder title, choose an accent color (Crimson, Amber, Emerald, Violet, Cyan), and toggle pin to top.
- **Validation**: Folder name must be non-empty and trimmed. Duplicate folder names trigger an inline warning.

### Subfolder Navigation Layer
Inside a folder view, subfolders are surfaced as horizontal scrollable tabs ("All", "Lectures", "Assignments", etc.) rather than nested inside the parent card.
- **Default**: Shows "All" tab aggregating all photos within the folder.

### Bottom Action Pill / Dock
Floating pill at the bottom of the home screen providing quick search entry and camera shortcut.

## UI Mockup
```
+------------------------------------------+
|  Fotara                 [Search / Filter]|
|                                          |
|  [=== Added Today / Deadlines Strip ===] |
|                                          |
|  +----------------+  +----------------+  |
|  | /---\ Ivory Tab|  | /---\ Ivory Tab|  |
|  | |   |__________|  | |   |__________|  |
|  |                |  |                |  |
|  |  Biology       |  |  Calculus      |  |
|  |                |  |                |  |
|  +----------------+  +----------------+  |
|                                          |
|  +----------------+  +----------------+  |
|  | /---\ Ivory Tab|  | /---\ Ivory Tab|  |
|  | |   |__________|  | |   |__________|  |
|  |                |  |                |  |
|  |  History       |  |  +New Folder   |  |
|  |                |  |                |  |
|  +----------------+  +----------------+  |
|                                          |
|         ( ====== Floating Dock ====== )   |
+------------------------------------------+
```

## Logic Notes
- Database operations run asynchronously using Kotlin Coroutines and Dispatchers.IO.
- Folder lists expose `Flow<List<FolderEntity>>` to allow instant UI updates.
- Folder card shape uses a custom Compose `Shape` or `Canvas` drawing Path combining an asymmetrical top tab with a rounded bottom body.

## Risks
- Custom card tab shape complexity in Jetpack Compose -> Mitigated by creating `FolderCard` utilizing custom Canvas drawing with cubic bezier curves.
- Room database schema changes -> Mitigated by declaring schema versioning with `exportSchema = false` for Phase 1 pre-release.

## Dependencies
- Android Gradle Plugin 9.0.1
- Jetpack Compose BOM & Material 3
- AndroidX Navigation 3 & Lifecycle ViewModel Compose
- Coroutines Test & JUnit 4

## Acceptance Criteria
- [x] Project compiles and builds successfully with Gradle.
- [x] Domain entities and models created for Folder, Subfolder, and Photo.
- [x] Reusable `FolderCard` composable renders ivory tab, royal blue body, bold italic title, and color badge matching UI reference.
- [x] Home screen displays 2-column grid of folders and functional "+New" folder card.
- [x] Student can create a folder with title and color label, and it persists in repository.
- [x] Folder detail screen opens with horizontal subfolder tabs and displays folder contents.
- [x] Unit tests pass for Folder and Photo database operations.
- [x] Correct Bug A (16dp dock elevation & window insets) and Bug B (search tap decoupled from folder creation).
- [x] Active search bar (`ActiveSearchBar.kt`) docks above software keyboard with video-buffering indicator and live keystroke matching.
- [x] Inline folder & subfolder text renaming with `HapticFeedbackType.LongPress` tactile pulse and keyboard Done commit.
- [x] Folder top bar features Add Photo `[+]` button and highlighted circular chip kebab menu `[(⋮)]`.
- [x] Popup Slider Modal (`CaptureReviewSliderModal.kt`) handles multi-photo capture triage at 75% height / 90% width.
- [x] Full-Screen Note Inspector (`PhotoViewerDialog.kt`) provides pinch-to-zoom, pan, and expandable offline OCR reader.
- [x] Verified zero compiler warnings, all unit tests pass, and packaged `Output/Release/Fotara_1.0.0_Beta.apk`.
