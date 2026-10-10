# Progress - Fotara
Last Updated: 2026-10-10

## Current Phase
Phase 55 - AndroidVersionDowngradeEngine (2.2.0 Alpha) [Completed]

## Phases
| Phase | Name                              | Status      |
|-------|-----------------------------------|-------------|
| 1     | CoreFolderEngine                  | Completed   |
| 2     | PhotoOcrEngine                    | Completed   |
| 3     | CustomNotesAndSearch              | Completed   |
| 4     | AdvancedFeatures                  | Completed   |
| 5     | GroupExpansionAndLinkIt           | Completed   |
| 6     | DocumentNotesAndConnectedFeatures | Completed   |
| 7     | NativeTextNotesAndAdvancedDocs    | Completed   |
| 8     | MasterV150EcosystemAndCanvas      | Completed   |
| 9     | PdfViewerEnhancementsAndFixes     | Completed   |
| 10    | CanvasAndSearchImprovements       | Completed   |
| 11    | HomeScreenRedesign                | Completed   |
| 12    | UIUXRefinementsAndSearchNav       | Completed   |
| 13    | FolderScreenRedesign              | Completed   |
| 14    | NotesScreenAndUpdatePopup         | Completed   |
| 15    | PdfAndPhotoViewerEnhancements     | Completed   |
| 16    | TextNoteEditorRework              | Completed   |
| 17    | ViewerCanvasExportAndUxFixes      | Completed   |
| 18    | CanvasSelectionAndEraser          | Completed   |
| 19    | CanvasHighlighterAndScrollEdges   | Completed   |
| 20    | PullToRefreshAndExportPresets     | Completed   |
| 21    | CanvasStackingAndOverlayLayout    | Completed   |
| 22    | VerificationAndRepair157          | Completed   |
| 23    | SettingsAndSearchRedesign         | Completed   |
| 24    | LinkItClustersAndProfileFixes     | Completed   |
| 25    | ProfilePngSupportAndUpdateModalPolish | Completed   |
| 26    | TextNoteEditorRepair              | Completed   |
| 27    | WorkspaceFoundation               | Completed   |
| 28    | WorkspacesPart2                   | Completed   |
| 29    | UIUXConsistency                   | Completed   |
| 30    | PdfOcrAndRelease160               | Completed   |
| 31    | QualityImprovementsAndBugfix      | Completed   |
| 32    | QualityAndSmoothness              | Completed   |
| 33    | PdfSearchDeepLinkAndHighlight     | Completed   |
| 34    | WorkspacePolishAndUiRefinements   | Completed   |
| 35    | LegalConsentAndDialogCoordinator  | Completed   |
| 36    | WorkspaceIconsPdfPageMenuAndDrawingEditor | Completed   |
| 37    | Release180AddendumBugfixes        | Completed   |
| 38    | Release181BetaHotfix              | Completed   |
| 39    | Release182BetaHotfix              | Completed   |
| 40    | Release183BetaHotfix              | Completed   |
| 41    | UpdateLifecycleAndAppResidueCleaner | Completed   |
| 42    | ClassScheduleAndNotesCapsule      | Completed   |
| 43    | TabPersistenceAndNavigation       | Completed   |
| 44    | StemNoteAndCanvasSuperpowers      | Completed   |
| 45    | WidgetsAudioAndRelease190         | Completed   |
| 46    | SpaceHierarchyAndAlphaFoundation  | Completed   |
| 47    | ThreeTierStealthPrivacy           | Completed   |
| 48    | AcademicEvaluationAndBundles      | Completed   |
| 49    | FotaraPackageManager             | Completed   |
| 50    | FriendsSystemAndSettings          | Completed   |
| 51    | LiveCollabCanvasAndRelease200     | Completed   |
| 52    | DrawingAudioWidgetPolishHotfixes  | Completed   |
| 53    | VersionRollbackManager            | Completed   |
| 54    | FriendsUiThemeAndWindowInsetsPolish | Completed   |
| 55    | AndroidVersionDowngradeEngine     | Completed   |

## Completed
- [x] Task 0: Verification & Android Skills Compliance Audit - Phase 35
- [x] Task 1: Legal documents: one source, two places (`Legal/*.md`, Gradle asset copy, loader, test, README statement) - Phase 35
- [x] Task 2: Privacy Policy and Terms of Service screens in About & Legal, and Privacy section - Phase 35
- [x] Task 3: Dialog coordinator and the consent gate (`AppDialogCoordinator`, `ConsentDialog`, `NetworkGate`, entry gating) - Phase 35
- [x] Task 4: Optional anonymous device count (`key_device_count_enabled`, `DeviceRegistry`, Supabase SQL, backup exclusion) - Phase 35
- [x] Task 5: Finalize Batch 1 (unit tests, debug APK, changelog, hygiene) - Phase 35
- [x] Task 0: Audits and reports (skills rules, branding spec search, PDF viewer, search navigation, tabs/drawings, update channel) - Phase 36
- [x] Task 1: Database: schema bump v17 -> v18 (icon_key, pdf_page_pins, pdf_page_drawings, cascade and orphan cleanups) - Phase 36
- [x] Task 2: Workspace icons (24 vector drawables, WorkspaceIcons registry, add/edit dialogs, custom tabs display, backup export/import) - Phase 36
- [x] Task 3: PDF page menu and pinned pages (3-dot button on page cards, max 3 pins, top chip row, jump to page) - Phase 36
- [x] Task 4: Save page to gallery & saved-image location setting in Settings (300 dpi render, flattened drawings, MediaStore/SAF, location setting) - Phase 36
- [x] Task 5: Search opens PDF at matching page with highlight, and text note match highlight (deep link, ML Kit OCR bounding boxes, 4s text note highlight) - Phase 36
- [x] Task 6: PDF page drawing data layer (shared engine extraction, PDF point coordinate space, data model & Sqlite repo complete, viewer wiring) - Phase 36
- [x] Task 7: PDF page editor dedicated screen (PdfPageEditorNavKey, dock, panels, 1x-6x smooth zoom, autosave, viewer display) - Phase 36
- [x] Task 8: Legal check, What's New 1.8.0 Beta entry, About text - Phase 36
- [x] Task 9: Version bump to 1.8.0 Beta / code 29, tests, release build, changelog, git - Phase 36
- [x] Task 1: Profile screen header position and banner consistency across Settings sub-screens - Phase 37
- [x] Task 2: Floating (+) button tab container hoisting and tab switch stability - Phase 37
- [x] Task 3: Drawing stroke offset after zooming fix via DrawingViewTransform - Phase 37
- [x] Task 1: Photo picker return navigation stability (rememberSaveable tab/section) - Phase 38
- [x] Task 2: Banner resolution & scaling in AnimatedBannerView (onSizeChanged, ContentScale.Crop fallback) - Phase 38
- [x] Task 3: Banner blink prevention across Profile navigation (ProfileBannerMemoryCache, no-crossfade Coil) - Phase 38
- [x] Task 4: Version bump to 1.8.1 Beta, changelogs, unit tests, and GitHub release - Phase 38
- [x] Task 1: Legal document reader bottom clearance (`LegalDocumentScreen.kt`, `LocalBottomOverlayPadding`) - Phase 39
- [x] Task 2: Stable workspace tab reorder drop commit with zero animation replay (`WorkspaceTabBar.kt`) - Phase 39
- [x] Task 3: Version bump to 1.8.2 Beta, changelogs, unit tests, and GitHub release - Phase 39
- [x] Task 1: Expandable device ID panel below anonymous share toggle (`DeviceRegistry.kt`, `SettingsScreen.kt`) - Phase 40
- [x] Task 2: Version bump to 1.8.3 Beta, changelogs, unit tests, APK assembly, and GitHub release - Phase 40
- [x] Task 1: Update download lifecycle cancellation on skip, install, and newer release arrival - Phase 41
- [x] Task 2: App residue and trash cleaner in Settings with disk usage counter and live progress bar - Phase 41
- [x] Task 3: 1.8 image banner integration, version bump to 1.8.4 Beta, test pass, packaging, and Stable tag Confirm Gate - Phase 41
- [x] Task 1: SQLite persistence for class_schedules (schema v19), DAO & ScheduleRepository - Phase 42
- [x] Task 2: Lightweight zero-dependency Excel (.xlsx) & Word (.docx) Table Parser & Mapping Dialog - Phase 42
- [x] Task 3: Next-Day Rollover Cutoff Engine & Proactive Alarm Reminders - Phase 42
- [x] Task 4: 1-Line Dynamic Schedule Capsule in NotesScreen.kt & Timetable Management Sheet - Phase 42
- [x] Task 1: Bottom tab state persistence across sub-screens via SaveableStateHolder in HomeScreen.kt - Phase 43
- [x] Task 2: Canonical Re-Tap Pop-to-Root and Scroll-to-Top in HomeBottomNavBar.kt & HomeScreen.kt - Phase 43
- [x] Task 1: LaTeX Mathematical Typesetting in Text Notes ($ inline, $$ display block) - Phase 44
- [x] Task 2: Markdown Tables with Formatting Constraints (Block tools disabled in cells) - Phase 44
- [x] Task 3: Shape & Curve Auto-Correct (Draw & Hold 400ms) with Bézier Smoothing - Phase 44
- [x] Task 4: Text Layers Tool (`[ T ]`) on Drawing Canvas and PDF Editor - Phase 44
- [x] Task 5: Multi-Sheet Drawing Canvas (Up to 10 Sheets) with Multi-Page Export - Phase 44
- [x] Task 1: Initialize FotaraDesktop workspace (React 19, TS, Vite) & verify build - Fotara Desktop
- [x] Task 2: Implement 3-Pane Studio UI (Side Activity Toolbar, Tabs, Split PDF & KaTeX, Scrubber) - Fotara Desktop
- [x] Task 3: Launch live dev server (http://localhost:5173) & register Desktop Agent 1 - Fotara Desktop
- [x] Task 4: Integrate KaTeX live math engine, PDF.js canvas renderer, and .fotara drag-and-drop bundle parser - Fotara Desktop
- [x] Task 5: Canonical Desktop UI Realization (IMAGE A replica: Home dashboard, folders grid with accent tiles, recent notes list with folded doc badges, and split study studio) - Fotara Desktop
- [x] Task 1: Android Home Screen Timetable Widget (4x2 / 4x4) with Rollover Sync (Agent-1) - Phase 45
- [x] Task 2: Android Home Screen Photo Widgets (Single Specific Photo & Carousel) (Agent-1) - Phase 45
- [x] Task 3: Synchronized Audio Annotations with Optional PDF Drag/Number Anchoring (Agent-2) - Phase 45
- [x] Task 4: Urgent Anti-Procrastination Alarm with Photo Proof Dismiss Challenge (Agent-2) - Phase 45
- [x] Task 5: Fotara 1.9.0 Beta Build Assembly, Verification & Packaging (Joint / Supervisor) - Phase 45
- [x] Task 1: Text Layer Movement & Drawing Auto-Smoothing / Shape Snapping - Phase 44
- [x] Task 2: Universal English Academic Terminology Overhaul across Timetable & Models - Phase 42
- [x] Task 3: NotesScreen Left-Edge 16dp Visual Alignment (U-31, U-35, U-36) - Phase 44
- [x] Task 4: Multi-line LaTeX Math Typesetting and Live Preview - Phase 44
- [x] Task 5: Timetable & Due Tomorrow Widget Data Availability and Broadcasters - Phase 45
- [x] Task 6: Wire In-App Audio Annotations & Anti-Procrastination Alarm Entry - Phase 45
- [x] Task 7: Fotara 1.9.1 Alpha Release Assembly & GitHub Publication - Phase 45
- [x] Task 1: Space Super-Hierarchy & SQLite Schema v20 Migration - Phase 46
- [x] Task 2: Top Header Space Switcher Dropdown (`Fotara ▾ [Space Name]`) - Phase 46
- [x] Task 1: Private Folder Vault (Pull-to-refresh & hold 2s + Biometrics) - Phase 47
- [x] Task 2: Private Workspace Ghost Mode (Home tab long-press 1.5s) - Phase 47
- [x] Task 3: Private Space Stealth Vault (Brand title long-press 2s + .nomedia shielding) - Phase 47
- [x] Task 1: Course Syllabus Weight & Target Exam Grade Evaluator - Phase 48
- [x] Task 2: Active Recall Masking & Diagnostic Occlusion Tape - Phase 48
- [x] Task 3: Document Perspective Rectification & Optical Enhancement Filter - Phase 48
- [x] Task 4: Coursework Archive & Interchange Bundle (`.fotara`) Export/Import - Phase 48
- [x] Task 1: Standalone Signed `.fpkg` Dynamic Package Loader Engine - Phase 49
- [x] Task 2: Just-In-Time (JIT) Download Interception Dialogs & Package Settings Hub - Phase 49
- [x] Task 1: Dedicated Friends Entry Card in Settings (Below Profile, Above General) - Phase 50
- [x] Task 2: FriendsScreen, User Tags (`@tag`), and QR Code Scanner - Phase 50
- [x] Task 1: Live Collaborative Study Canvas with 6-Digit Room Codes (`FT-XXXX`) - Phase 51
- [x] Task 2: Real-Time Vector Stroke Merging & Peer Cursors - Phase 51
- [x] Task 3: Fotara 2.0.0 Release Verification, APK Build Assembly & Documentation - Phase 51

- [x] Task 1: Visible Shape & Auto-Smoothen Tool Controls in Canvas and PDF Editor - Phase 52
- [x] Task 2: Text Layer Post-Creation Editing, Multi-line & Alignment Controls - Phase 52
- [x] Task 3: Real PDF Audio Annotations with Runtime Microphone Permission & Page Anchoring - Phase 52
- [x] Task 4: Photo Widget Database Query Fix (`file_path`) and Universal Terminology - Phase 52
- [x] Task 5: Universal English Academic Terminology Overhaul across Desktop App - Phase 52
- [x] Task 6: LaTeX Mathematical Formula Parser Macro Order Repair (`\infty`, `\leq`, `\geq`, `\neq`, `\iint`) - Phase 52

- [x] Task 1: Multi-Release Query & Parsing in UpdateManager (`rollbackReleases`) - Phase 53
- [x] Task 2: Semantic Version Categorization & `isOlderVersion` Logic - Phase 53
- [x] Task 3: Rollback APK Download & FileProvider Installer Pipeline - Phase 53
- [x] Task 4: Interactive Version History & Rollback UI with Safety Backup Guard - Phase 53
- [x] Task 5: Unit Tests & Regression Verification - Phase 53

- [x] Task 1: Harmonize `FriendsSettingsCard.kt` to match `SettingsCardItem` design tokens (slate tile, muted icon, typography) - Phase 54
- [x] Task 2: Fix status bar hardware overlap in `FriendsScreen.kt` using `WindowInsets.statusBars` / `Modifier.statusBarsPadding()` - Phase 54
- [x] Task 3: Unify `FriendsScreen.kt` header with `SettingsSubScreenHeader` and desaturate presence chips & dialogs - Phase 54
- [x] Task 4: Unit test validation, version bump to 2.1.1 Alpha, release APK assembly - Phase 54

- [x] Task 1: Enable `android:hasFragileUserData="true"` in `AndroidManifest.xml` for native data retention on uninstall - Phase 55
- [x] Task 2: Create `DowngradeManager.kt` with Shizuku `pm install -d -r` shell downgrade pipeline - Phase 55
- [x] Task 3: Implement Automated Vault Backup to public Downloads and public APK staging - Phase 55
- [x] Task 4: Sticky post-uninstall notification & uninstall intent trigger in `DowngradeManager` - Phase 55
- [x] Task 5: Interactive Downgrade Wizard in `UpdateScreen.kt` with Shizuku, Guided Safe Reinstall, and ADB command - Phase 55
- [x] Task 6: Unit tests, regression verification, version bump to 2.2.0 Alpha, and release packaging - Phase 55

## In Progress
None.

## Blocked
None.

## Pending
None.
- [x] Task 1: LinkIt cluster glow for groups of 3 or 4 converging on center point (pure anchor function & renderer) - Phase 24
- [x] Task 2: Profile picture and banner loading defect fix (WebP encode check, API 24-36 format, cache invalidation timestamps) - Phase 24
- [x] Task 3: Fullscreen aspect-locked crop editor with region decoding (pan, corner resize, pinch zoom, reset) - Phase 24
- [x] Task 4: Finalize 1.5.8 Beta (versionName 1.5.8 Beta, versionCode 22, unit tests, release build) - Phase 24
- [x] Task 1: Assets conversion and profile data layer (WebP borders, illustration, ratio table, atomic storage) - Phase 23
- [x] Task 2: Settings screen redesign matching Screenshot 2 (banner fade, avatar & border, 7 cards, bottom nav clearance) - Phase 23
- [x] Task 3: Profile editing features (pen button sheet, gallery photo picker, crop step, live border picker, ProfileScreen) - Phase 23
- [x] Task 4: Search screen redesign matching Screenshot 1 (3 pills container, search field, recent chips, idle illustration) - Phase 23
- [x] Task 5: Finalize and rebuild 1.5.7 (unit tests, APK build, changelog, progress) - Phase 23
- [x] Task 1: Complete Phase 1 release audit and documentation - Phase 22
- [x] Task 2: Implement LinkIt grid facing glow for same-row and vertical neighbor pairs across all card types and densities - Phase 22
- [x] Task 3: Replace Home folder card 3-dot trigger with anchored DropdownMenu matching specification - Phase 22
- [x] Task 4: Fix Settings repeating snackbar notification with one-shot event consumption - Phase 22
- [x] Task 5: Enhance PDF/DOCX cards with page 1 preview, badge cleanup, date-only footer, and pluralization - Phase 22
- [x] Task 6: Unit testing, release build verification, and final report - Phase 22
- [x] Task 7: Implement Invert Selection button in Home and FolderDetail multi-select modes - Phase 22
- [x] Task 8: Harden one-shot notification consumption with Channel/eventFlow to prevent any re-entry snackbar replay - Phase 22
- [x] Task 1: Pull to refresh with animated curved-arrow vector indicator across Home, Notes, and FolderDetailScreen - Phase 20
- [x] Task 2: "Add to Group" dialog grouped by folder with atomic folder/subfolder synchronization and preserved timestamps - Phase 20
- [x] Task 3: Optional custom file name in "Share As" dialog with pure sanitization, keyboard-safe dialog, and unique suffixes - Phase 20
- [x] Task 4: Combined file name preset setting with dynamic tokens ({folder}, {date}, {time}, {count}) and SharedPreferences storage - Phase 20
- [x] Task 5: Finalize and build release 1.5.6 (signed Fotara_1.5.6_Beta.apk, 439 passing unit tests) - Phase 20
- [x] Task 1: Free-form lasso selection (non-zero winding, partial stroke cutting, non-destructive references) - Phase 18
- [x] Task 2: Selection action bar & partial delete (reactive visibility, duplicate, move to layer, partial delete) - Phase 18
- [x] Task 3: Select tool move, stretch, rotate (handle hit targets, oriented box, anchor-fixed stretch, single commit) - Phase 18
- [x] Task 4: Free eraser (capsule sweep, remnant rule, custom icon, size slider, single undo step) - Phase 18
- [x] Task 5: Artifact-free guarantee and regression hardening (tests & validation) - Phase 18
- [x] Phase 11 Task 1: Color palette definition & legacy folder color assignment (Blue, Brown, Purple, Green, Red, Slate) - Phase 11
- [x] Phase 11 Task 2: Facing LinkIt corner stroke glow with folder-specific bright accent colors - Phase 11
- [x] Phase 11 Task 3: Redesign FolderCard to dark rounded rectangle with top-left accent icon tile and top-right menu - Phase 11
- [x] Phase 11 Task 4: Segmented tab bar (All, Favorit, Arsip) with active blue pill - Phase 11
- [x] Phase 11 Task 5: Redesign Header with Fotara title, subtitle tagline, and dual circular buttons - Phase 11
- [x] Phase 11 Task 6: Search bar pill + circular '+' button with popup menu - Phase 11
- [x] Phase 11 Task 7: Floating bottom navigation bar (Home, Notes, Settings) and top-level tab switching - Phase 11
- [x] Phase 11 Task 8: Settings screen redesign as vertical list of rounded rectangle cards - Phase 11
- [x] Phase 11 Task 9: IME keyboard insets & bottom stack non-overlapping layout validation - Phase 11
- [x] Analyze product brief and UI reference image - Phase 1
- [x] Install and verify Android CLI and SDK environment (API 36, Temurin JDK 17) - Phase 1
- [x] Initialize Arinara governance docs (Rules.md, Codex.md, Phase1 spec, Changelog) - Phase 1
- [x] Scaffold Android Gradle project structure with Jetpack Compose - Phase 1
- [x] Implement brand design system and custom Folder Card composable matching UI reference - Phase 1
- [x] Complete UX Interaction Specification Revision 2 document (`Docs/UXInteractionSpecification_Rev2.md`) - Phase 1
- [x] Fix Bug A (search dock obstruction & elevation) and Bug B (search vs folder creation tap binding) in FloatingDock - Phase 1
- [x] Implement keyboard-docked search bar (`ActiveSearchBar.kt`) with fluid spring animation and video-buffering indicator - Phase 1
- [x] Implement inline long-press folder & subfolder renaming with haptic pulse - Phase 1
- [x] Implement folder top bar with Add (+) button and Highlighted Kebab (⋮) menu - Phase 1
- [x] Implement Popup Slider Review Modal (`CaptureReviewSliderModal.kt`) for captured note triage - Phase 1
- [x] Implement Full-Screen Note Inspector (`PhotoViewerDialog.kt`) for studying existing notes - Phase 1
- [x] Initial Phase 1 release packaging and validation - Phase 1
- [x] Implement on-device OCR engine and text tokenization pipeline (`OcrEngine.kt`) - Phase 2
- [x] Implement smart auto-suggest folder scoring engine (`FolderSuggestEngine.kt`) - Phase 2
- [x] Implement multi-capture camera view with live counter and perspective straightening (`MultiCaptureScreen.kt`) - Phase 2
- [x] Implement photo card long-press quick action sheet (move, tag color, deadline, delete) - Phase 2
- [x] Implement deadline manager for "Due Tomorrow" banner and scheduling reminders - Phase 2
- [x] Execute automated tests and package updated Fotara 1.0 Beta APK - Phase 2
- [x] Implement real Google ML Kit on-device text recognition pipeline - v1.0.1
- [x] Implement offline SQLite database with SQLite FTS5 search index (`FotaraDbHelper`) - v1.0.1
- [x] Implement home screen Folder Multi-Select and Bulk Delete with exact byte calculation - v1.0.1
- [x] Implement real PDF exporter (`PdfExporter`) and system share sheet integration - v1.0.1
- [x] Implement deadline reminder alarms via Android `AlarmManager` and `NotificationChannel` - v1.0.1
- [x] Fix startup crash on Android devices by replacing FTS5 with standard Android FTS4 SQLite virtual table and adding graceful SQLite error recovery - v1.0.1
- [x] Startup latency audit: Profile cold-start and warm-start on main thread, identifying blocking operations prior to first frame - Phase 3
- [x] Startup latency optimization: Enforce reactive queries (`Flow`) and migrate heavy operations off main thread to meet targets (<1.5s cold, <500ms warm) - Phase 3
- [x] Individual photo custom notes: Add editable note field to photos for user annotations - Phase 3
- [x] Unified full-text search indexing: Include custom notes in SQLite FTS4 virtual table queries for reliable retrieval - Phase 3
- [x] Interactive note editor in Full-Screen Note Inspector (`PhotoViewerDialog.kt`) - Phase 3
- [x] Individual photo rename: Add "Rename" to photo long-press quick-action menu editing caption with dedicated dialog and immediate FTS index update - Phase 3
- [x] Subfolder management: Unified single long-press context menu on subfolder tabs providing Rename and Delete with Trash confirmation - Phase 3
- [x] Subfolder tab multi-select: Multi-select tab row mode with filled checkmark badges and bulk Delete to Trash action bar - Phase 3
- [x] Subfolder vs. photo multi-select mutual exclusion: Strict state separation preventing simultaneous activation - Phase 3
- [x] Folder photo multi-select: Implement long-press quick-action "Select" entry, checkmark indicators, and contextual action bar (Delete, Rename [single only], Move, Color label) - Phase 3
- [x] Search result navigation: Replace popup slider viewer with direct navigation to source folder, activating target subfolder tab - Phase 3
- [x] Search result auto-scroll: Animated smooth scroll to matched photo cell when outside viewport - Phase 3
- [x] Search result highlight: 3-second non-blocking dimmed translucent overlay initiated after scroll completion with smooth fade-out - Phase 3
- [x] Search result edge case handling: Non-blocking notification for moved/deleted photos and pre-fetching target chunk for unrendered pages - Phase 3
- [x] Data safety trash/recycle bin: Soft-delete architecture with 30-day auto-purge, orphan parent restoration, accessible via Settings and Home overflow menu - Phase 3
- [x] Settings screen implementation: 7 grouped sections (Display, OCR, Notifications, Storage with Trash entry, Data backup/import, Search rebuild, About) - Phase 3
- [x] Onboarding flow: Pre-permission explanation for camera/storage and optional starter subject folder generator ("Math", "Science", "History", "Literature") - Phase 3
- [x] Package intermediate Fotara 1.1.0 Beta APK artifact - Phase 3
- [x] Photo viewer refinements: 90° manual rotation control and manual re-crop tool replacing stored version - Phase 3
- [x] Search filters and recents: Horizontal date/color filter row above keyboard-docked search bar and recent queries list - Phase 3
- [x] Notifications and widget: Android home-screen widget for "Due tomorrow" notes with empty state, and notification `[View Note]` direct deep-link action - Phase 3
- [x] Accessibility and adaptation: Responsive column layout for tablet/landscape and system font scaling compliance - Phase 3
- [x] Folder privacy lock: PIN and Android BiometricPrompt authentication, locked folder card presentation, and device credential fallback - Phase 3
- [x] Photo Groups (v1.1 Addendum 5): PhotoGroup data model, multi-select creation with 2+ photos, grid cell with stack badge, scoped slider viewer with "Remove from group", long-press context menu, multi-select integration, "Add photos", search indexing, and PDF export - Phase 3
- [x] Cross-folder smart tags: Automatic hashtag tokenization, offline aggregation, cross-folder queries, ActiveSearchBar tag chips, and PhotoViewerDialog tag inspector - Phase 4
- [x] Zoom-gated slider/inspector navigation (v1.1 Addendum 6): Horizontal swipe paging gated on scale == 1.0f, pan on scale > 1.0f, double-tap and pinch-out reset across all slider/inspector contexts - Phase 3
- [x] Fix Bug A: Wire grid density setting into FolderDetailScreen and GroupDetailScreen with live updates and responsive additive scaling (2/3/4 + 1/3) - v1.1.1
- [x] Fix Bug B: Fix subfolder tab pointer event interception to reliably trigger context menu (Rename, Delete with Trash item count confirmation, and Select) and multi-select bulk delete - v1.1.1
- [x] Fix Bug C: Resolve horizontal swipe pointer consumption in `ZoomablePhotoViewport` with multi-touch guard so 1.0x unzoomed scale advances photos across all contexts - v1.1.1
- [x] Implement Addendum 7: Dedicated fullscreen Group Screen with responsive grid, order-added sorting, [+] Add photos, overflow menu (Rename, Ungroup, Delete to Trash, Color label), and scoped popup viewer with auto-dissolve - v1.1.1
- [x] Implement Addendum 7 Search Edge Case: Chained search highlight for grouped photo matches (1s waypoint on group cell -> auto-navigate -> 3s member photo highlight) - v1.1.1
- [x] Release packaging: Fotara v1.1.1 Beta APK compiled and verified - v1.1.1
- [x] Bug Fix: Remove placeholder Light theme, offer System / Dark options, silently migrate legacy LIGHT preference to SYSTEM - Phase 5
- [x] LinkIt: Spatial consecutive grouping for 2-4 linked items across Home folders and Folder grid notes/groups with LinkGroup SQLite table, pinned elevation, and auto-dissolve - Phase 5
- [x] Group Management Expansion: Multi-select group merge with earliest photo cover and automatic old group dissolution - Phase 5
- [x] Group Management Expansion: Move groups to folders/subfolders with recent destinations picker and folder counter sync - Phase 5
- [x] Group Management Expansion: Add to Group multi-select & quick action with recents picker and order-added preservation - Phase 5
- [x] Group Management Expansion: Direct capture and gallery import into group with folder-locked triage review - Phase 5
- [x] Group Management Expansion: Manual cover photo assignment via inspector star button and context menu - Phase 5
- [x] Group Management Expansion: Independent photo copy duplicating image files and thumbnail records - Phase 5
- [x] Group Management Expansion: 4-second undo snackbar on delete and move actions - Phase 5
- [x] Group Management Expansion: Group deadline with notification scheduling, due tomorrow banner inheritance, and inspector countdown - Phase 5
- [x] Group Management Expansion: PDF and ZIP archive export for groups and mixed multi-selections via FileProvider - Phase 5
- [x] Group Management Expansion: Trash severance auto-dissolve rule dissolving group when trashing leaves <= 1 member - Phase 5
- [x] Group Management Expansion: Inline caption renaming in popup triage review slider - Phase 5
- [x] Automated Test Suite: 100 unit tests passing covering all v1.2 features and edge cases - Phase 5
- [x] Release packaging: Fotara 1.2.0 Beta APK compiled and verified - Phase 5
- [x] Part 1: DocumentNote data models, SQLite tables, PDF native renderer + OCR, DOCX text extractor, vertical viewer, Split to Images - Phase 6
- [x] Part 2: Jetpack Glance widget rebuild with resizability, deep linking, direct interactive callback, and 30-min sync - Phase 6
- [x] Part 3: Batch rename for 1+ items across Photo/Group/DocumentNote, Home folder rename parity, and Group screen parity - Phase 6
- [x] Part 4: Unified Share As picker (Original, PDF, Word) and multi-select Share Notes - Phase 6
- [x] Part 5: Combine operation UX with determinate progress, cancellation, and partial file cleanup - Phase 6
- [x] Part 6: Online services: GitHub update checker, Supabase suggestion form with UUID rate limiting, QRIS support, What's New - Phase 6
- [x] Part 7: Pre-flight 100-page cap check on combine and export operations - Phase 6
- [x] QRIS 1:1 dimension crop with pure quiet zone, added to assets & resources, integrated into SupportScreen - Phase 6
- [x] Proprietary & Educational Software License added to GitHub repository - Phase 6
- [x] Release packaging: Fotara 1.2.0 Beta APK and Fotara 1.3.0 Beta APK uploaded to GitHub Releases - Phase 6
- [x] Fix Settings and UpdateManager versioning: dynamically read PackageManager versionName, strip prefix in version comparisons, and upload re-signed v1.3.0 Beta APK - Phase 6
- [x] Fix release banner assets: attach release banners to GitHub release assets and embed direct HTTPS URLs in release notes - Phase 6
- [x] Patch 1.3.1: Stabilized feedback and bug report submissions with an offline-first persistent queue.
- [x] Patch 1.3.1: Restructured Folder multi-select with a dedicated contextual action dock preventing action clipping.
- [x] Patch 1.3.1: Enhanced LinkIt badge visibility and spatial layout across Home folders and Folder note cards.
- [x] Patch 1.3.1: Integrated smart folder and subfolder suggestions directly into the camera review workflow.
- [x] Patch 1.3.1: Implemented real 90-degree image rotation in the capture review perspective tool.
- [x] Patch 1.3.1: Wired camera button on the Home screen floating dock.
- [x] Patch 1.3.1: Modernized in-app update engine with automatic launch check and resilient GitHub release asset redirect handling.
- [x] Patch 1.3.1: Dynamically bound Settings screen version display to active application package metadata.

- [x] Patch 1.3.2: Elevated contextual multi-select bottom dock safely above system 3-button navigation bar with navigationBarsPadding.
- [x] Patch 1.3.2: Extended App Update screen viewport with navigationBarsPadding and a 56.dp scroll spacer to eliminate bottom button truncation.
- [x] Patch 1.3.2: Fixed in-app APK installer freeze by adding REQUEST_INSTALL_PACKAGES permission, switching APK storage to getExternalFilesDir, and providing an unknown-apps settings prompt.
- [x] Patch 1.3.2: Safe Navigation Pop Guard preventing backstack exhaustion and app closure upon double-clicking close/back.
- [x] Patch 1.3.2: What's New rich markdown renderer with categorized cards (What's New, Patches, Breaking Changes) and custom chips.
- [x] Patch 1.3.2: App Update screen polish renaming Software Update to App Update with status bar padding.
- [x] Patch 1.3.2: Supabase database schema documentation (Docs/Supabase_Schema.sql) with RLS and 10/hr trigger rate limit.
- [x] Patch 1.3.2: Bumped versionCode to 8, versionName to 1.3.2, compiled and signed Fotara_1.3.2_Beta.apk for GitHub Releases.

- [x] Patch 1.3.3: Bump versionCode to 9, versionName to 1.3.3, build, sign, and publish Fotara_1.3.3_Beta.apk to GitHub Releases for in-app OTA update verification.
- [x] Patch 1.3.3: Implemented universal Rich Markdown component (`RichMarkdownText` & `RichMarkdownColumn`) supporting bold, italic, code, strikethrough, blockquotes, and lists across What's New, App Update, and Study Notes.
- [x] Patch 1.3.3: Added interactive update re-check action to scan all releases and immediately fast-forward/skip to newest available build.

- [x] Patch 1.3.4: Persistent background APK download decoupled from Compose screen lifecycle with application-scoped `updateScope` and `.part` streaming.
- [x] Patch 1.3.4: Interactive background download notifications with progress bar and completion intent routing directly to `UpdateScreen` via `MainActivity` and `MainNavigation`.
- [x] Patch 1.3.4: Added explicit download cancellation (`cancelDownload()`) with partial file cleanup.
- [x] Patch 1.3.4: Automated Supabase feedback email forwarding bridge (`Docs/SupabaseEmailBridge.gs`) delivering styled HTML notifications to `arinaranetwork@gmail.com`.
- [x] Patch 1.3.4: Supabase schema and documentation expansion (`Docs/Supabase_Email_Notification_Guide.md` and `Docs/Supabase_Schema.sql`).

- [x] Patch 1.3.5: Bump versionCode to 11, versionName to 1.3.5 in `app/build.gradle.kts` and update What's New fallback metadata.
- [x] Patch 1.3.5: Upgraded `FeedbackManager` with strict standard HTTP headers (`User-Agent`, `Accept`, `Content-Length`, `setFixedLengthStreamingMode`), 30/day submission quota, 3s throttle, and opportunistic auto-flush.
- [x] Patch 1.3.5: Upgraded `FeedbackDialog` with dynamic 4-category selector chip row (`BUG_REPORT`, `SUGGESTION`, `FEATURE_IDEA`, `GENERAL`).
- [x] Patch 1.3.5: Verified direct automated email bridge to `arinaranetwork@gmail.com` via Supabase database webhooks.
- [x] Patch 1.3.5: Compile, sign, and publish `Fotara_1.3.5_Beta.apk` to GitHub Releases.
- [x] Workstream 1: Full Audit & Quality Improvement - Present audit report and apply fixes - Phase 7
- [x] Workstream 2: PDF Defect Repair (White background fill, viewport scale, bounded cache, pipelined import) - Phase 7
- [x] Workstream 3: In-App Reflowed DOCX Viewer with XML structure parser - Phase 7
- [x] Workstream 4: Native Text Notes (TextNote entity, markdown editor, autosave, grid integration, MD/TXT share) - Phase 7
- [x] Package and sign Fotara_1.4.0_Beta.apk artifact - Phase 7
- [x] Publish Fotara 1.4.0 Beta release notes and signed APK to GitHub Releases - Phase 7
- [x] 1.5-A: Global English-only localization audit and resource restriction - Phase 8 (Milestone 1)
- [x] 1.5-B: Feedback engine repair (schema alignment, HTTP 400 fix, 5/day rolling limit, 60s cooldown) - Phase 8 (Milestone 1)
- [x] 1.5-C: PDF stability on 30+ pages, Split to Images white canvas & auto-grouping, remove "Sharp PDF Note" - Phase 8 (Milestone 1)
- [x] 1.5-D: Virtualized in-doc PDF per-page pinch-to-zoom and freeform pan - Phase 8 (Milestone 1)
- [x] 1.5-E: Immutable dateAdded metadata, Room migration, and search date filtering - Phase 8 (Milestone 1)
- [x] 1.5-F: Text note editor toolbar repair (bold/italic/headings) and rich formatting expansion - Phase 8 (Milestone 1)
- [x] 1.5-G: LinkIt corner glow redesign replacing amber badge with gradient stroke - Phase 8 (Milestone 1)
- [x] 1.6-A: Universal note scheduling engine with notification and alarm alerts across all note types - Phase 8 (Milestone 2)
- [x] 1.6-B: Jetpack Glance "Today" widget with due items, notes added today, and upcoming schedules - Phase 8 (Milestone 2)
- [x] 1.6-C: Functional notification settings (permissions, alert style, snooze, test notification) - Phase 8 (Milestone 2)
- [x] 1.6-D: Full Settings screen audit eliminating all dead toggles and placeholders - Phase 8 (Milestone 2)
- [x] 1.7-A: Accepted content registration for ACTION_SEND / ACTION_SEND_MULTIPLE (max 30 items) - Phase 8 (Milestone 3)
- [x] 1.7-B: Streaming staging into sandboxed cache with background thumbnailing - Phase 8 (Milestone 3)
- [x] 1.7-C: Interactive destination placement screen with "Place Here (N)" and collapsible drawer - Phase 8 (Milestone 3)
- [x] 1.7-D: Streamlined note placement mapping across photos, PDFs, DOCX, and text notes - Phase 8 (Milestone 3)
- [x] 1.8-A: Unlimited Canvas 8-zone architectural layout (Z1-Z8) compliance - Phase 8 (Milestone 4)
- [x] 1.8-B: Core canvas drawing engine (vector strokes, layers, tools, images, PNG export) - Phase 8 (Milestone 4)
- [x] 1.8-C: Canvas persistence (compact binary serialization, chunked Room storage, cross-cutting hooks) - Phase 8 (Milestone 4)
- [x] 1.8-D: Canvas extended features evaluation and implementation - Phase 8 (Milestone 4)
- [x] 2.0-A to 2.0-G: Final hardening pass (cross-API regression, performance benchmarks, security audit, docs) - Phase 8 (Milestone 5)
- [x] C1-0 Setup: Audit repository state, confirm branch release/1.5.0, set versionName to '1.5.0 Beta', raise versionCode to 14, confirm dynamic version resolution in Settings and Update screens, create structured 1.5.0 Changelog and What's New source - Phase 8 (Chunk C1)
- [x] C1-A English-Only App: Global audit, eliminate Indonesian text, enforce English resource restriction via resourceConfigurations = ['en'], enforce explicit Locale.US on all date formatters, verify zero emojis across repo - Phase 8 (Chunk C1)
- [x] C1-B Suggestions & Bug Reports: Fix HTTP 400 root causes (schema alignment, category check constraint support for Title Case and UPPER_CASE, Content-Length byte accuracy, Prefer: return=minimal, PostgREST P0001 trigger classification, offline queue poison prevention, collapsible technical details, 60s cooldown countdown, 5/24h rolling quota, Supabase migration SQL, unit tests, and C11 verification script) - Phase 8 (Chunk C1)
- [x] C2-A PDF Stability & Freeze Root Cause: Identified LazyColumn gesture competition and repeated page open mutex contention; eliminated transformable at 1.0x, implemented lazy page aspect ratio cache, cooperative cancellation of off-screen page render jobs, 32MB bounded LRU bitmap cache, and adjacent page prefetching - Phase 8 (Chunk C2)
- [x] C2-B Split to Images: Fixed dark/black compression artifacts with pre-filled opaque white canvas, unified density-scaled renderer path (1200-2560px), implemented 5+ page Photo Group packaging rule (with Page 1 cover and preserved order) vs <= 4 standalone notes, added determinate progress dialog with cancellation and rollback cleanup, and unit tests - Phase 8 (Chunk C2)
- [x] C2-C Header Text: Removed all occurrences of 'Sharp PDF Note', simplified TopAppBar subtitle to clean page count only ('N pages' / '1 page') across the codebase - Phase 8 (Chunk C2)
- [x] C2-D Virtualized Per-Page Zoom & Freeform Pan: Implemented decoupled testable `PdfPageZoomState` state holder, free two-axis pan with boundary clamping, continuous pinch scale (1.0x-4.0x), double-tap zoom/reset, on-demand high-density 2.0x rendering for visible zoomed regions, memory release on return to 1.0x, smooth pinch activation in shared photo viewport, and unit tests - Phase 8 (Chunk C2)
- [x] C3-A Date Added Metadata: Added immutable `added_at` epoch ms timestamp across all note entities (`Photo`, `PhotoGroup`, `DocumentNote`, `TextNote`, `CanvasNote`), bumped database version to 12 with 5 B-tree indexes (`idx_*_added_at`), backfilled legacy `added_at <= 0` using `created_at` (never zero/null), implemented shared `getNotesAddedBetween(startTime, endTime)` query for widget/date-only search, and verified timestamp semantics (split-to-images timestamp, copy-to new timestamp, preserve on move/rename/trash/restore) - Phase 8 (Chunk C3)
- [x] C3-B Date Filters in Search: Pure class `DateRangeCalculator` computing exact midnight-to-midnight ranges across timezones, daylight saving transitions, leap years, and Sunday vs Monday week starts; extended `SearchDateFilter` with `SINGLE_DAY`; integrated date filtering across all 5 note types in `HomeViewModel`; optimized date-only searches to skip full-text OCR scan; updated `ActiveSearchBar` with Single Day date picker, removable active filter chips, "Reset Filters" action in empty state, and "Added MMM d, yyyy" badges on all search result cards; updated detail inspectors and quick action sheets with formatted added timestamp - Phase 8 (Chunk C3)
- [x] C3-C LinkIt Corner Glow Redesign: Replaced prominent amber badge/icon/text with subtle, elegant corner glow (`Modifier.linkItCornerGlow`) using `drawWithCache` (soft radial gradient corner bleed + 1.8dp corner arc stroke + fading linear gradients); positioned at `GlowCorner.BottomLeft` on royal blue body avoiding folder tab and color dot / schedule badges; full API 24 to 36 hardware acceleration compatibility without RenderEffect/blur runtime overhead; updated across folder cards, detail grid notes, groups, documents, text notes, and canvas cards - Phase 8 (Chunk C3)
- [x] C4-0 to C4-6 Text Note Editor Rebuild: Identified root causes of previous editor breakdown (unhoisted text state, absent `VisualTransformation`, unwired word-wrap semantics, unhandled list newlines). Rebuilt editing core around dedicated `EditorState` holder with coalesced undo/redo history, pure `MarkdownParser` (Headings 1-3, bold, italic, bold-italic, strikethrough, inline code, code blocks, blockquotes, bullet/numbered lists, checklists, links, dividers), monotonic bidirectional `MarkdownOffsetMapping` with live dimmed/hidden marker rendering via `MarkdownVisualTransformation`, complete pure `EditorActions` suite with list auto-continuation and backspace removal, docked 6-group 48dp `EditorToolbar` with live active syntax indicators, enhanced `TextNote.stripMarkdownFormatting` for clean search FTS indexing, and unit tests - Phase 8 (Chunk C4)
- [x] C5-A to C5-E Universal Note Scheduling Engine: Implemented universal `SchedulableNote` interface across all note entities (`Photo`, `PhotoGroup`, `DocumentNote`, `TextNote`, `CanvasNote`), Room/SQLite migration v13 adding `schedule_title` column and 5 B-tree indexes (`idx_*_scheduled_at`), pure `ScheduleMath` class for past detection, 10m snooze computation, and formatting without Android framework dependencies, `NoteScheduleManager` with API 24–36 channel configuration (`IMPORTANCE_HIGH`, `USAGE_ALARM`), `setExactAndAllowWhileIdle()`, full-screen intent for screen-off alarms, `BootReceiver` handling `BOOT_COMPLETED`, `TIME_SET`, and `TIMEZONE_CHANGED`, trash cancellation preserving timestamps and restore re-arming if still future, universal UI entry points ("Schedule..." overflow on every note screen, Schedule button in EditorToolbar Group 6, non-colliding `CardScheduleBadge` at bottom-right, `NoteDetailScheduleChip` under title), interactive `ScheduleNoteDialog` with date/time pickers, custom name field, notification vs alarm choice, inline past validation, quick presets, permission explanation modal, Due Tomorrow widget and Glance widget data feed, architecture documentation (`Docs/ScheduleAndAlarmArchitecture.md`), device verification checklist (`Docs/ScheduleVerificationChecklist_C11.md`), and unit tests (`ScheduleMathTest`, `ScheduleMigrationTest`) - Phase 8 (Chunk C5)
- [x] C7-A to C7-E Share to Fotara: Registered ACTION_SEND and ACTION_SEND_MULTIPLE with exact MIME types (images, PDF, DOCX, Markdown, plain text) and no wildcards; enforced hard 30-item cap with English notice for skipped excess items; non-crashing inline error states for corrupt or password-protected files; 8KB streaming staging buffer into sandboxed cache with asynchronous thumbnail generation; process recreation and singleTop onNewIntent incremental append; destination placement screen with recent destinations, coursework folders, subfolder/group chips, PIN-unlock for privacy-locked folders, and new folder/subfolder dialogs; top-left X button with unplaced items confirmation dialog; bottom full-width button labeled exactly "Place Here (1)" or "Place Here (N)"; right-side docked tab for >1 items expanding item panel with checkboxes and previews; photo group image-only restriction; pure Kotlin ShareSessionEngine with unit tests; and C11 verification checklist (`Docs/ShareVerificationChecklist_C11.md`) - Phase 8 (Chunk C7)
- [x] C8-A to C8-H Unlimited Canvas Part 1: Engine, Data Model, Persistence: Built crash-resistant pure Kotlin canvas foundation; safe unbounded world coordinates with ViewportTransform (world-to-screen, screen-to-world, focal zoom, pan, fit-to-content, zoom limits 0.05x-50x, safe guards against NaN/Inf/0-scale); extensible sealed element model (`CanvasElement`: `StrokeElement`, `ImageElement`) and multi-layer management with undoable deletions; stroke processing (midpoint quadratic smoothing, RDP decimation, pressure normalization, bounds computation, single-tap dot and zero-length safety, hit testing, lasso polygon ray casting, and area erasing splitting strokes into surviving segments); QuadTree spatial index verified 100% against brute force; command pattern undo/redo with history capping; version 14 database schema (`canvas_layers`, `canvas_elements`, `canvas_assets`), compact delta-encoded varint StrokeCodec with safe corruption handling, downsampled image asset manager with EXIF orientation, debounced transactional autosave worker, progressive chunked loading for 20,000+ strokes, shared note abstraction integration with permanent purge cleanup, and comprehensive unit tests (`ViewportTransformTest`, `StrokeProcessorTest`, `QuadTreeSpatialIndexTest`, `CanvasHistoryTest`, `StrokeCodecTest`, `CanvasMigrationTest`, `CanvasDocumentOperationsTest`) - Phase 8 (Chunk C8)
- [x] C9-A to C9-F Unlimited Canvas Part 2: Rendering, Gestures, Drawing Tools: Built crash-resistant rendering pipeline and gesture engine on C8 foundation; architectural selection of AndroidView-hosted custom View (CanvasDrawingView) for batched digitizer historical coordinates (120Hz/240Hz stylus support) and zero Compose recomposition jank; zero-allocation onDraw loop with pre-allocated Paint, Path, and Matrix reuse and immutable snapshot reads; TileCacheManager with 32MB fixed memory budget, LRU eviction, OOM degradation fallback to half-resolution RGB_565, and regional tile invalidation; viewport culling via QuadTreeSpatialIndex respecting layer visibility, opacity, and order; adaptive background pattern (grid, dots, ruled) preventing line explosions at extreme zoom; pure PointerStateMachine managing Idle, Drawing, PanZoom, Selecting, and Transforming states with seamless mid-stroke 2nd finger cancel-to-pan transition, stylus palm rejection, and smooth OverScroller fling; pressure-sensitive Pen, flat semi-transparent non-darkening Highlighter (SRC_OVER compositing), stroke and area erasers with C8 stroke splitting, tap and lasso selection, 8-handle + rotation stem on-canvas transform handles; full command-pattern undo/redo integration; C11 stress test checklist (Docs/CanvasStressChecklist_C11.md); and comprehensive unit tests (PointerStateMachineTest, TileInvalidationTest, TransformHandlesMathTest, ToolControllerTest) - Phase 8 (Chunk C9)
- [x] C10-A to C10-I Unlimited Canvas Part 3: Zone-Map UI, Layers, Images, Export, Integration: Implemented complete 8-zone architectural UI (Z1-Z8) in `Docs/CanvasZoneMap.md` and `CanvasScreen.kt` wired through `CanvasViewModel` to C8 persistence, C8 commands, and C9 tools; clear Alpha indicator and one-time experimental notice modal; 'New Canvas (Alpha)' menu item adjacent to 'New Text Note' in folder add menu and search results navigation; comprehensive Z5 layers panel (add, delete with undo, reorder up/down, rename, show/hide, lock, opacity slider, active selection); image import from gallery and existing Fotara photo notes (EXIF orientation, downsampled to max 2048px, centered in visible viewport on active layer, draw over images); PNG export with 1x/2x scale, content vs selection bounds, 4096px OOM guard with RGB_565 downscaling fallback, and FileProvider sharing; live debounced autosave state indicator (Saved, Saving, Error); stylus-only mode and palm rejection settings toggles; cross-cutting integrations (rename, move to folder, Trash soft-delete and restore, permanent asset purge, folder privacy lock, C5 schedule alarm, date added metadata, search by title); C11 device verification matrix (`Docs/CanvasStressChecklist_C11.md`); and comprehensive unit tests (`CanvasViewModelTest`) - Phase 8 (Chunk C10)
- [x] C6-A to C6-E: Dedicated Glance 'Today' widget, notification settings, and complete settings audit eliminating all dead toggles - Phase 8 (Chunk C6)
- [x] C11-A: Whole-app sweep, global placeholder scan (0 TODO/FIXME), Indonesian elimination, and version consistency audit - Phase 8 (Chunk C11)
- [x] C11-B: Build and test execution, zero compile errors, 100% unit tests passing (267/267 tests), release build assembly with R8 shrinking - Phase 8 (Chunk C11)
- [x] C11-C: Verification matrix on device and hardware testing protocol - Phase 8 (Chunk C11)
- [x] C11-D: Finalize documentation (What's New, Changelog_1.5.md, README.md, CanvasZoneMap.md, Progress.md) - Phase 8 (Chunk C11)
- [x] C11-E: Signing check (SHA-256 matching v1.4.0), secrets scan, release artifact packaging (Fotara_1.5.0_Beta.apk), git branch merge & tag protocol - Phase 8 (Chunk C11)
- [x] C11-F: Final Report and full combined Wiring Table - Phase 8 (Chunk C11)
- [x] P1-0 Root cause analysis: Identify 0.707f default aspect ratio race condition and fix via pre-populated renderer cache - Phase 9 (Chunk P1)
- [x] P1-A Pure math layout utilities: Implement PdfLayoutMath with unit tests for size clamping and aspect preservation - Phase 9 (Chunk P1)
- [x] P1-B Rebuild list-mode zoom: Viewport-level zoom with PdfViewportZoomState, two-axis pan, double-tap toggle, and amber status - Phase 9 (Chunk P1)
- [x] P2-A Header Page View button: Add leftmost icon button to PDF header opening fullscreen page viewer - Phase 9 (Chunk P2)
- [x] P2-B Photo viewer parity: Extract ZoomableBox and reuse across photo viewer and PDF page viewer with zero code duplication - Phase 9 (Chunk P2)
- [x] P2-C Layout and OCR bottom sheet: Implement PdfPageViewerDialog with collapsible OCR text drawer and copy button - Phase 9 (Chunk P2)
- [x] P2-D Rendering & state sync: Density-aware sharp render, adjacent page prefetching, retry state, and scroll position sync - Phase 9 (Chunk P2)
- [x] P3-A Version housekeeping: Update versionName to '1.5.1 Beta', versionCode to 15, update changelog, What's New, and README - Phase 9 (Chunk P3)
- [x] P3-B Verification & tests: 290 unit tests passing 100% across all suites - Phase 9 (Chunk P3)
- [x] P3-C Release build & signing: Assemble release APK, verify signing certificate, packaging, and checksum - Phase 9 (Chunk P3)
- [x] P3-D GitHub publishing protocol: Tag Fotara_1.5.1_Beta, merge to main, and publish GitHub release - Phase 9 (Chunk P3)
- [x] P3-E Final report: Wiring table, slim pages root cause, list zoom strategy, hidden sections report - Phase 9 (Chunk P3)

- [x] Item 1: Canvas Autosave Engine (flush on pause/stop/back, single-writer Mutex queue, atomic write, tap-to-retry) - Phase 10
- [x] Item 2: Canvas Panning Math & Culling (1:1 tracking, eliminate double delta/reversals, bounds clamp) - Phase 10
- [x] Item 3: Fix Add With Image (downsample <=2048px, EXIF orientation, private storage, user error message) - Phase 10
- [x] Item 4: Canvas Top Bar Responsive Overlap Fix (adaptive layout, ellipsized title, compact status, overflow) - Phase 10
- [x] Item 5: Zoom Pill Placement (position above collapsible tool panel with insets clearance) - Phase 10
- [x] Item 6: Broom Tool as Stroke Eraser (active layer stroke eraser, distinct state, undoable, autosaved) - Phase 10
- [x] Item 7: Folder Card Orange Glow (facing edges/corners between adjacent glowing cards on home grid) - Phase 10
- [x] Item 8: Search OCR and DOCX Content (extract DOCX text, SQLite FTS indexing with migration, debounced search, snippets with [OCR]/[DOCX] labels) - Phase 10
- [x] Item 9: Add from Existing Notes (picker for image-based notes only, insert as layer snapshot) - Phase 10
- [x] Item 10: Canvas Hard Limits (centralized CanvasConfig.kt, 20k x 20k extent, max 50 layers, max 2048px image, snackbars) - Phase 10
- [x] Version bump to 1.5.2 (versionName, versionCode 16), Changelog entry in Changelog_1.5.md - Phase 10

- [x] Item 11: Text Note Editor Toolbar & Caret Precision (pure transformation engine, caret offset fix, Enter list continuation and blank-line exit, single-stroke backspace prefix deletion, live preview styling with 1:1 active line caret stability, interactive checkbox toggling) - Phase 10
- [x] Item 12: Canvas Note Long-Press Action Sheet Parity (contextual action sheet with Rename, Color Label, Deadline, Schedule Reminder, Move to Folder, Share as Image PNG, Batch Selection, and Delete to Trash) - Phase 10
- [x] Item 13: Elms Sans Typography Migration & Italic Elimination (app-wide typography migration with strict 3-weight mapping: Light 300, Medium 500, Bold 700; complete removal of UI italics across all screens, headers, cards, dialogs, and search bars) - Phase 10
- [x] Item 14: Folder Screen Header Button Spacing (clean 10dp separation and 48dp minimum touch targets for Add [+] and Kebab [⋮] buttons) - Phase 10
- [x] Item 15: Canvas Direct Hardware-Accelerated Vector Rendering & Black Tile Elimination (eliminate tile-missing dark grid holes during zoom/draw, remove RGB_565 pitch-black tile degradation, direct QuadTree-culled vector and image rendering with solid-fill single point dots) - Phase 10

- [x] Task 1: Redesign SettingsScreen root into 6 main section rounded cards and clean un-carded detail lists - Phase 12
- [x] Task 2: Restore full-height viewport on HomeScreen by eliminating duplicate insets and excessive bottom padding - Phase 12
- [x] Task 3: Fix photo viewer top toolbar layout by decoupling title/actions and widening instructional helper text - Phase 12
- [x] Task 4: Implement search-to-folder auto-scroll and 2-second exposure/brightness highlight across all note types - Phase 12
- [x] Task 5: Build, test verification, and regression check - Phase 12
- [x] Assemble, test, sign, and package Fotara 1.5.3 Beta APK artifact (versionCode 17, versionName 1.5.3 Beta) with Phase 12 UI/UX refinements - Phase 12
- [x] Task 1: Unified Notes Data Model & NotesViewModel (reactive aggregation of Photos, Documents, Text Notes, Canvas Notes, Folder names, excluding trashed, date grouping by Today, Yesterday, calendar dates, type filtering, search) - Phase 14
- [x] Task 2: NotesScreen UI matching Screenshot 1 (Header with Notes title, subtitle, Search & More circular buttons; pill filter chips; date-grouped cards; note item rows with thumbnails, titles, folders, timestamps, 3-dot menus; FAB for new notes; empty states) - Phase 14
- [x] Task 3: In-App Update Engine Enhancements (UpdateManager skippedVersion persistence in SharedPreferences, semantic version check latestVersion > installedVersion && latestVersion != skippedVersion, session dismissal logic, reset mechanism) - Phase 14
- [x] Task 4: In-App Update Modal Popup UI matching Screenshot 2 (Centered modal with 28dp radius, top banner with Coil/fallback, overlaid title, scrollable release notes, tonal "Later" and "Skip this version" pill buttons, integrated into HomeScreen) - Phase 14
- [x] Task 5: Testing, string localization, build validation & verification - Phase 14

- [x] Task 1: Reading Mode Zoom Gating in PDF Viewer (toggle via book icon, detached gestures outside reading mode, state reset on exit) - Phase 15
- [x] Task 2: Smooth 1:1 GPU-Layer Zoom Engine (exact focal point tracking, graphicsLayer lambda, no recomposition, debounced sharpen) - Phase 15
- [x] Task 3: Subtitle Relocation to Three-Dot Menu Info Section (single-line title in top bar, dynamic state-accurate Info header in menu) - Phase 15
- [x] Task 4: Testing & Build Verification - Phase 15
- [x] Task 1: Core Engine Rework (`TextEditorOps.kt`, `EditorActions.kt`): caret invariant, keyboard table (Enter, Backspace, Space), numbered renumbering, typing shortcuts - Phase 16
- [x] Task 2: Live Rendering & Offset Mapping (`MarkdownVisualTransformation.kt`, `MarkdownOffsetMapping.kt`): prefix sprites, inline delimiter hiding, tappable checklist box - Phase 16
- [x] Task 3: Toolbar Overhaul (`EditorToolbar.kt`): scrollable, full button suite, clean padding, active states - Phase 16
- [x] Task 4: Top Bar & Info Section (`TextNoteEditorScreen.kt`, `strings.xml`): single-line title, word/char counts in 3-dot overflow Info menu - Phase 16
- [x] Task 5: Unit & Regression Tests (`TextEditorOpsTest.kt`, regression test suite): full keyboard table, caret invariants, screenshot reproduction cases - Phase 16
- [x] Task 6: Changelog Entry & Documentation Finalization: update `Changelog_1.5.md` and mark Phase 16 complete - Phase 16

- [x] Task 1: High-zoom PDF sharp rendering up to 4.0x with memory budget and cache eviction - Phase 17
- [x] Task 2: Direct PDF viewer opening from Notes tab & search with matching page jump - Phase 17
- [x] Task 3: Combine share support for Text Notes (formatted markdown) & Canvas Notes (1x raster) - Phase 17
- [x] Task 4: Rotate and crop safe atomic file writes, consistent OCR re-run, and FTS refresh - Phase 17
- [x] Task 5: Standardization of three-dot overflow menu header to "Info" across viewers - Phase 17
- [x] Task 6: Canvas smooth stroke Bezier path builder, zoom-aware tolerance, round highlighter - Phase 17
- [x] Task 7: Canvas layer instant creation ("Layer N") and inline long-press renaming - Phase 17
- [x] Task 8: New folder dialog immediate soft keyboard autofocus via FocusRequester - Phase 17
- [x] Task 9: Remove camera option from Home screen (+) dock button, opening folder dialog directly - Phase 17
- [x] Task 10: Pure-logic unit tests and manual test script validation - Phase 17

- [x] Task 0: Carry-over checks (density-aware remnant threshold, consolidate test files, Z8 vertical offset) - Phase 19
- [x] Task 1: Translucent highlighter in all phases, single-path rendering, ic_highlighter.xml icon - Phase 19
- [x] Task 2: Layer-isolated blend modes (NORMAL, MULTIPLY, DARKEN, SCREEN), StrokeCodec V2, API 24-28 fallback - Phase 19
- [x] Task 3: Long-press tool panels with haptic feedback, Highlighter Blending row - Phase 19
- [x] Task 4: Shorter bottom dock (~20% reduction), shared/dynamic dock-anchored offsets - Phase 19
- [x] Task 5: Home screen scroll edges (Modifier.verticalEdgeFade, dynamic bottom padding, Notes parity) - Phase 19

- [x] Task 1: Unified canvas element stacking rule (layer order first, creation order inside layer, equal treatment of strokes and images) - Phase 21
- [x] Task 2: Easy selection, scaling and stretching of images (60% viewport insert, auto-selection, proportional corner scale, 1D side stretch) - Phase 21
- [x] Task 3: Tool options panels layout bug (wrap content, max height bounded between capsules and dock, scrollable, 48dp single-row blending chips) - Phase 21
- [x] Task 4: Bottom overlay spacing (shared dynamic height source of truth, snackbars and Notes (+) FAB clear floating nav bar) - Phase 21
- [x] Task 5: Overflow menu anchor position (nested in trigger IconButton, right-aligned, flipped upward if needed) - Phase 21


