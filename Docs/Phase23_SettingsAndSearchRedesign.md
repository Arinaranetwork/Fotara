# Phase 23 - SettingsAndSearchRedesign

## Goal
Deliver Batch 3 for Fotara release 1.5.7 Beta: complete redesign of the Settings screen (with full-width fading banner, live profile avatar and borders, and 7 structured category cards) and the Search screen (top controls container with 3 pills, search field, recent searches chips, single scope tab, and offline search illustration), supported by a robust local profile data layer.

## Scope
- Optimized asset conversion for 3 profile borders and 1 search illustration to high-quality alpha WebP drawables.
- Profile data layer: `UserProfile` model, border configuration table with verified inner circle ratios, atomic file storage in `filesDir/profile/` (with EXIF handling, downsampling, and atomic replacement), and `SettingsRepository` integration with reactive `Flow<UserProfile>`.
- Settings screen redesign: full-width banner with status-bar scrim and background fade mask, centered avatar with overlaid border and pen edit button, name and optional email, 7 category cards with shortened <=38 char subtitles, and shared bottom navigation overlay clearance.
- Profile editing workflows: modal bottom sheet actions, image picker (`ActivityResultContracts.PickVisualMedia`), fullscreen aspect-ratio-constrained pan/zoom crop composable, live avatar border selection grid, and dedicated `ProfileScreen` with auto-save and one-shot feedback.
- Search screen redesign: top header with back navigation and large title, 3-pill controls container (Sort, Date, Filter) with anchored dropdowns, rounded search field with clear button, Recent Searches card with chip flow and individual removal, single "All" scope tab row (hidden for 1 scope), and offline anime illustration idle state.
- String externalization in `strings.xml`, zero placeholder data, comprehensive unit tests, and production release build verification.

## Out Of Scope
- Folder reordering and gesture drag sorting (cancelled).
- Workspaces and multi-scope search data models (deferred to future release).
- Account synchronization, cloud auth, or remote profile backups (profile is strictly local-only).
- New database schema changes, Room DAOs, or third-party dependencies.
- Changes to `ViewportTransform.kt`, FTS virtual tables, or `fotara.fileprovider`.

## Features

### Feature 1: Profile Asset & Data Engine
- **Behavior**: Provides 3 bundled borders and a "None" option. Avatar is clipped to the border's inner circle diameter, matching with zero gap or overlap. Profile images are stored in `filesDir/profile/avatar.webp` (512x512) and `filesDir/profile/banner.webp` (max width 1080px).
- **Data Flow**: `SettingsRepository` manages keys `key_profile_name`, `key_profile_email`, `key_profile_avatar_path`, `key_profile_banner_path`, and `key_profile_border_id`, exposing `profileFlow: StateFlow<UserProfile>`.
- **Failure Behavior**: Image decoding errors or invalid files surface an error event via one-shot messaging and keep the existing image intact.

### Feature 2: Redesigned Settings Screen (Screenshot 2)
- **Behavior**: Edge-to-edge scrollable view with full-width banner (~220dp height) fading into `HomeNearBlack` via vertical gradient mask. Status bar and back button remain legible over a dark top scrim. Centered profile block displays avatar + selected border (~1.4x avatar diameter) + 32dp circular pen button. Seven category cards in order: Profile, General, Appearance, OCR & Recognition, Notifications & Deadlines, Storage & Data Management, About & Legal.
- **Subtitles**: Verified under 38 characters:
  - Profile: "Photo, border, banner, name, email"
  - General: "Grid density, default sort order"
  - Appearance: "Theme mode and display options"
  - OCR & Recognition: "On-device text extraction"
  - Notifications & Deadlines: "Reminders and test alert"
  - Storage & Data Management: "Usage, trash, backup, re-indexing"
  - About & Legal: "Version info and offline architecture"

### Feature 3: Interactive Profile Management
- **Behavior**: Tapping the pen icon button opens a bottom sheet with "Change profile picture", "Choose border", "Change banner", "Edit name and email", plus "Remove" actions when custom images are active.
- **Crop Experience**: Full-screen gesture canvas supporting 1x-5x pinch zoom and pan, clamped so image always fills the crop window. Circular crop for avatar; rectangular crop for banner.
- **Border Picker**: Live grid displaying borders rendered around the user's active avatar for instant visual confirmation.

### Feature 4: Redesigned Search Screen (Screenshot 1)
- **Behavior**: Single-page search interface with top header ("Search" + Back), 3 control pills in a rounded container, rounded text field, and Recent Searches card when idle.
- **Controls & Dropdowns**:
  - *Sort Pill*: Always highlighted blue with sort icon and active sort name ("Newest" / "Oldest"). Anchored dropdown with sort options.
  - *Date Pill*: Calendar icon + "All dates" or active date filter. Neutral dark when default; highlighted blue when active. Anchored dropdown with preset dates and custom range trigger.
  - *Filter Pill*: Sliders/tune icon + "Filter" label. Neutral dark when default; highlighted blue with active count badge. Anchored dropdown with color tags, smart tags, and clear filter button.
- **Recent Searches**: Flow layout of chips with search icon, query string, and clear (X) button. Tapping chip initiates search; "Clear Recents" clears all history.
- **Idle State**: Displays optimized anime laptop illustration, bold title "Search your notes", and offline index subtitle text.

## Audit Before Coding

### Search Screen Audit
| Control in Screenshot 1 | Existing Fotara Capability | Redesign Implementation |
|---|---|---|
| Sort Pill ("Terbaru", blue) | `SearchSortOrder` (`NEWEST_ADDED`, `OLDEST_ADDED`) | Blue pill with sort icon & text; opens anchored dropdown to toggle/select sort order. |
| Date Pill ("Semua Tanggal") | `SearchDateFilter` (`ALL`, `TODAY`, `YESTERDAY`, `THIS_WEEK`, `THIS_MONTH`, `THIS_YEAR`, `CUSTOM_RANGE`) | Pill with calendar icon & label; highlighted when non-default; opens anchored dropdown with options. |
| Filter Pill ("Filter") | `TagColor` filter, `smartTags` filter | Pill with tune icon & count badge; highlighted when active; opens anchored dropdown for colors and tags. |
| Search Input Box | Text query search with IME action | Rounded card with leading magnifier, clear (X) trailing button when query is present. Scan icon omitted (no backend feature). |
| Recent Searches Card | `key_recent_searches` JSON list in `fotara_settings` | Card with history icon, "Recent Searches", "Clear Recents" button, and `FlowRow` of chips with individual X delete. |
| Scope Tabs ("All", "Favorit", "Arsip") | Single scope "All" supported | Hidden when only 1 scope is available. Structured to accept future workspace scopes. |
| Idle Illustration | Anime character peeking over laptop | `search_illustration.webp` (720px wide) centered with title & descriptive subtitle. |

### Settings Screen Audit
| Category Section | Existing Subtitle | Shortened Subtitle (<= 38 chars) | Destination |
|---|---|---|---|
| **Profile** (New) | N/A | Photo, border, banner, name, email | Opens `ProfileScreen` |
| **General** | Grid density, default sort order, and storage path | Grid density, default sort order | Opens General settings detail list |
| **Appearance** | Theme mode and visual display options | Theme mode and display options | Opens Appearance settings detail list |
| **OCR & Recognition** | On-device text extraction, scripts, and quality | On-device text extraction | Opens OCR settings detail list |
| **Notifications & Deadlines** | Reminders lead time, test alert, and due ribbon | Reminders and test alert | Opens Notifications settings detail list |
| **Storage & Data Management** | Storage breakdown, trash, re-indexing, and backup | Usage, trash, backup, re-indexing | Opens Storage settings detail list |
| **About & Legal** | Version info, offline architecture, and licenses | Version info and offline architecture | Opens About settings detail list |

## Border Table
| Border ID | Master Source | Inner Diam (px) | Image Width (px) | Inner Ratio | Center Offset | Drawable Res |
|---|---|---|---|---|---|---|
| `none` | N/A | N/A | N/A | 1.000f | (0f, 0f) | 0 |
| `file_000000000278820bb0a8901e8fa6612b` | `Assets/Border/file_000000000278820bb0a8901e8fa6612b.png` | 904 | 1254 | 0.7209f | (-0.0435f, -0.0092f) | `R.drawable.file_000000000278820bb0a8901e8fa6612b` |
| `file_000000001c6c820bbc0efdac44c0aa63` | `Assets/Border/file_000000001c6c820bbc0efdac44c0aa63.png` | 879.5 | 1254 | 0.7014f | (+0.0028f, +0.0335f) | `R.drawable.file_000000001c6c820bbc0efdac44c0aa63` |
| `file_000000007fdc81f7b74b9a79e2c63215` | `Assets/Border/file_000000007fdc81f7b74b9a79e2c63215.png` | 853 | 1310 | 0.6511f | (-0.0378f, -0.0088f) | `R.drawable.file_000000007fdc81f7b74b9a79e2c63215` |

## UI Mockup

### Settings Screen (Screenshot 2)
```
+------------------------------------------+
|  <- Settings                             | [Status bar + Scrim]
|                                          |
|         +----------------------+         | [Full-width Banner ~220dp]
|         |     /----------\     |         | [Gradient fade to Black]
|         |    /   AVATAR   \    |         |
|         |    \   CIRCLE   / (E)|         | [Overlaid Border + Pen Button]
|         |     \----------/     |         |
|         +----------------------+         |
|                 Arinara                  | [Large Bold Name]
|           arinara@example.com            | [Muted Email (if present)]
|                                          |
|  +------------------------------------+  |
|  | [User] Profile                  >  |  | [Rounded Category Card 1]
|  | Photo, border, banner, name, email |  |
|  +------------------------------------+  |
|  | [Grid] General                  >  |  | [Rounded Category Card 2]
|  | Grid density, default sort order   |  |
|  +------------------------------------+  |
|  | [Palette] Appearance            >  |  | [Rounded Category Card 3]
|  | Theme mode and display options     |  |
|  +------------------------------------+  |
|  | [OCR] OCR & Recognition         >  |  | [Rounded Category Card 4]
|  | On-device text extraction          |  |
|  +------------------------------------+  |
|  | [Bell] Notifications & Deadlines>  |  | [Rounded Category Card 5]
|  | Reminders and test alert           |  |
|  +------------------------------------+  |
|  | [Storage] Storage & Data Mgmt   >  |  | [Rounded Category Card 6]
|  | Usage, trash, backup, re-indexing  |  |
|  +------------------------------------+  |
|  | [Info] About & Legal            >  |  | [Rounded Category Card 7]
|  | Version info and offline arch      |  |
|  +------------------------------------+  |
|                                          |
|      (Clearance for Floating Nav)        |
+------------------------------------------+
|       [Home]     [Notes]   [Settings*]   | [HomeBottomNavBar]
+------------------------------------------+
```

### Search Screen (Screenshot 1)
```
+------------------------------------------+
|  <- Search                               | [Header Row]
|  +------------------------------------+  |
|  | [Sort: Newest*] [Date: All v] [Fltr]|  | [3-Pill Controls Container]
|  +------------------------------------+  |
|  +------------------------------------+  |
|  | (Q) Search notes, subjects, text(X)|  | [Rounded Search Field]
|  +------------------------------------+  |
|  +------------------------------------+  |
|  | (T) Recent Searches   Clear Recents|  | [Recent Searches Card]
|  |  (Q) NaCl (x)   (Q) BAHASA JAWA (x)|  |
|  |  (Q) soal bahasa jawa (x)          |  |
|  +------------------------------------+  |
|                                          |
|                 [Anime]                  | [Optimized Illustration]
|            Search your notes             | [Bold Title]
|     Type keywords to find handwritten    | [Muted Subtitle]
|     formulas, slide diagrams and         |
|     lecture notes indexed offline.       |
|                                          |
+------------------------------------------+
|       [Home*]    [Notes]    [Settings]   | [HomeBottomNavBar]
+------------------------------------------+
```

## Logic Notes
- **Atomic Image Writes**: New profile avatar and banner bitmaps are compressed into `.tmp` files inside `filesDir/profile/`, then atomically renamed. Old files are cleared only upon successful write.
- **EXIF & Downsampling**: Decode operations read image bounds, calculate `inSampleSize` to bound memory usage, and inspect EXIF orientation tag before matrix rotation.
- **Reactive UI Flow**: `SettingsRepository` emits `UserProfile` updates through a unified state flow.
- **Pill Visual Rule**: Sort pill is always rendered with the active blue highlight style (`HomeMainButtonBlue`). Date and Filter pills use neutral surface style when default, and switch to active blue highlight when non-default (with filter badge showing active filter count).

## Risks
- Large user photos causing OOM -> Decoded using pre-sampled bounds and bounded matrix downsampling.
- Profile image cache stale in Coil -> Coil cache keys append `file.lastModified()` timestamp to guarantee immediate live updates.
- Dropdown menu clipped by screen boundaries -> Anchored dropdowns clamped within screen horizontal bounds.

## Dependencies
- Phase 22 (Verification and Repair 1.5.7).
- Coil (image loading library).
- AndroidX Activity Photo Picker.

## Acceptance Criteria
- [x] 3 border WebP images and 1 search illustration WebP converted, optimized, and copied to `res/drawable/`.
- [x] `ProfileBorders` table defines all border IDs, drawable references, and verified inner ratios with 100% test coverage.
- [x] `SettingsRepository` exposes 5 profile keys, defaults, atomic storage in `filesDir/profile/`, and `profileFlow`.
- [x] Settings screen matches Screenshot 2 with status-bar banner fade, centered avatar + border + pen button, and 7 category cards with <=38 char subtitles.
- [x] Pen button launches bottom sheet with photo picker, crop step (circle avatar, rect banner, pan/zoom clamping), and live border selection grid.
- [x] `ProfileScreen` allows editing name and email with focus-loss/Done autosave and fallback to "Fotara User".
- [x] Search screen matches Screenshot 1 with 3-pill controls container, anchored dropdowns, rounded search box, recent searches chip card, hidden single-scope tab row, and anime illustration idle state.
- [x] Last cards and search content scroll cleanly above the floating bottom navigation using `LocalBottomOverlayPadding`.
- [x] All UI strings externalized in `strings.xml`.
- [x] Full test suite passes and production APK builds successfully with `versionName "1.5.7 Beta"` and `versionCode 21`.
