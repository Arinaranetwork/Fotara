<!-- --- Arinara Network (c) 2026 --- -->
<!-- Exclusive property of Arinara Network. -->
<!-- Unauthorized use, reproduction, distribution, or modification of this code, -->
<!-- in whole or in part, for any purpose, is strictly prohibited without prior -->
<!-- written consent from Arinara Network as sole legal owner of this codebase. -->

# Phase 27 - WorkspaceFoundation

## Goal
Establish the Workspace Foundation for Fotara 1.6.0 (Batch 2A of 4). Introduces the core workspace data architecture, database migration (v16), ordered workspace tabs replacing legacy Home segments, folder-to-workspace association, workspace creation, renaming, and gesture-driven drag-and-drop reordering with full offline preservation and test coverage.

## Scope
- Database Schema & Migration v16:
  - New `workspaces` table (`id`, `uuid`, `kind`, `name`, `position`, `created_at`) with index on `position`.
  - Add `workspace_id` (INTEGER NOT NULL DEFAULT 1) to `folders` table with index `idx_folders_workspace_id`.
  - Deterministic migration seeding built-in `HOME` (id 1, pos 0) and `ARCHIVE` (id 2, pos 1). All existing folders default/update to `HOME`. All other tables, notes, trash, FTS, schedules, and link groups remain untouched.
- Domain Model & Manual DI:
  - `Workspace` entity with `WorkspaceKind` enum (`HOME`, `ARCHIVE`, `CUSTOM`).
  - `WorkspaceRepository` in `AppContainer`: `observeWorkspaces()`, `observeFoldersIn(workspaceId)`, `createWorkspace(name)`, `renameWorkspace(id, newName)`, `reorderWorkspaces(workspaceIds)`.
- Core Business Rules & Validation (Typed Errors in Repository):
  - Maximum 10 `CUSTOM` workspaces; `HOME` and `ARCHIVE` are excluded from the limit.
  - Workspace names: trimmed, repeated internal whitespace collapsed, length 1..20 chars, case-insensitively unique against all existing workspaces (including localized/built-in "Home" and "Archive").
  - `HOME` is permanently fixed at position 0, non-renamable, non-deletable, non-draggable. `ARCHIVE` is non-renamable and non-deletable, but reorderable among positions > 0. `CUSTOM` workspaces are reorderable, renamable, and (in Batch 2B) deletable.
  - Dense positions (0..N) guaranteed across all operations in atomic transactions.
  - Default folder routing: Folders created in Home tab belong to the currently selected workspace. All other entry points (Share to Fotara, onboarding starter subjects, orphan recovery, legacy backup import) route to `HOME`.
- Backup Export & Import:
  - Backup export includes `workspaces` array (`uuid`, `kind`, `name`, `position`) and folder `workspaceUuid`.
  - Backup import: legacy backups assign folders to `HOME`. Modern backups match built-in `HOME` and `ARCHIVE` by kind, custom by uuid; missing custom workspaces created up to limit of 10, overflowing folders routed safely to `HOME`. Existing workspaces are never deleted or renamed on import.
- UI & Gesture System:
  - Replace legacy `HomeSegment` (All, Favorit, Arsip) with `WorkspaceTabBar`: horizontal scrollable row with persistent right-pinned `(+)` button, 44dp touch target, single-line text with ellipsis, top-edge fade, theme-consistent pills.
  - Built-in tab icons: `HOME` uses grid icon; `ARCHIVE` uses archive icon; custom tabs show text only.
  - Workspace empty states: "No folders in <name> yet" with hint "Tap + to create one".
  - Add Workspace Dialog with auto-focus keyboard, inline validation errors, limit indicator (dimmed `(+)` button + message "You can have up to 10 workspaces").
  - Top 3-dot overflow menu: adds "Add workspace" entry.
  - Tab Gesture State Machine: pure, testable state machine (`idle`, `pressed`, `held`, `dragging`). Long-press on custom tab shows anchored Rename dropdown panel (scale ~1.06, elevation via `graphicsLayer`); movement beyond touch slop (~8dp) dismisses panel and initiates horizontal drag reordering with haptic feedback, auto-scroll at edges, and position clamping (positions after Home only).
  - Selected workspace state survives screen rotation and folder navigation, resetting to Home only on app cold start or when the selected workspace is removed.
- Comprehensive Folder Object Item Count (Mid-development request):
  - Include text notes (`text_notes`), drawing canvas notes (`canvas_notes`), and document notes (`document_notes` - docx/pdf) alongside photos in folder item counts, delete statistics, and trash operations. Folder cards and trash reflect true total object counts across all note media types.

## Out Of Scope
- Batch 2B deliverables: Workspace deletion, folder migration between workspaces, restore destination dialog, workspace filtering in Search screen.
- Batch 3 deliverables: PDF content OCR, release banner generation, release build packaging.
- Version number changes (`versionName` and `versionCode` remain unchanged until release build).

---

## Phase 1 - Architecture & Data Audit

### A. Home Folder Loading & Sorting
- Current query in `SqliteFolderRepository.refreshSync()`:
  ```sql
  SELECT f.id, f.name, f.color_label, f.is_pinned, f.created_at,
         COUNT(p.id) as photo_count,
         COALESCE(SUM(p.file_size_bytes), 0) as total_size,
         f.is_locked, f.lock_pin
  FROM folders f
  LEFT JOIN photos p ON f.id = p.folder_id AND p.is_trashed = 0
  WHERE f.is_trashed = 0
  GROUP BY f.id
  ORDER BY f.is_pinned DESC, f.created_at DESC
  ```
- Folders are combined with `link_groups` in `HomeViewModel.arrangeFoldersWithLinks()` where pinned items and pinned link clusters are sorted to the top, followed by unpinned items.
- Target workspace filtering: `WHERE f.is_trashed = 0 AND f.workspace_id = ?` ensures each workspace tab renders only its own folders while preserving pinned-first and link-cluster rules.

### B. Segmented Tabs Inventory & Retirement
- `HomeSegment` enum (`ALL`, `FAVORIT`, `ARSIP`) existed in `HomeSegmentedTabBar.kt` and `HomeScreen.kt`.
- `FAVORIT` and `ARSIP` rendered empty placeholder boxes (`Box(modifier = Modifier.fillMaxSize())`).
- Resolution: Remove `HomeSegment` and replace with reusable `WorkspaceTabBar` driven by `List<Workspace>` from `WorkspaceRepository`.

### C. Folder Creation Entry Points
| Entry Point | Location | Workspace Target Rule |
| :--- | :--- | :--- |
| **Home Screen (+) Button** | `HomeScreen.kt` -> `HomeViewModel.createFolder` | Selected workspace |
| **Share to Fotara** | `SharePlacementViewModel.kt` (`createFolder`) | `HOME` workspace |
| **Onboarding Starter Subjects** | `OnboardingViewModel.kt` (`createFolder`) | `HOME` workspace |
| **Settings Backup Restore** | `SettingsRepository.kt` (`importDataBackup`) | Mapped workspace UUID or `HOME` |

### D. Backup Format Specification
- JSON Root additions:
  ```json
  "workspaces": [
    {
      "uuid": "4c431a4c-...",
      "kind": "HOME",
      "name": "",
      "position": 0
    },
    {
      "uuid": "7a8b9c0d-...",
      "kind": "CUSTOM",
      "name": "Finance",
      "position": 2
    }
  ]
  ```
- Folder object addition:
  ```json
  "workspaceUuid": "7a8b9c0d-..."
  ```

### E. Database Version
- Previous: `DATABASE_VERSION = 15`
- Target: `DATABASE_VERSION = 16`

---

## Phase 2 - Target Behavior Specification

### 2.1 Workspace Model & Data Invariants
```kotlin
enum class WorkspaceKind { HOME, ARCHIVE, CUSTOM }

data class Workspace(
    val id: Long = 0,
    val uuid: String,
    val kind: WorkspaceKind,
    val name: String,
    val position: Int,
    val createdAt: Long = System.currentTimeMillis()
)
```
- Position 0 is strictly reserved for `HOME` (`id = 1L`).
- Position 1 defaults to `ARCHIVE` (`id = 2L`), movable within positions `1..N`.
- Custom workspaces take positions `2..N`.

### 2.2 Tab Gesture State Machine
```
       [Down]                 [Long-Press Timeout]
IDLE ----------> PRESSED ---------------------------> HELD (Lifted, Dropdown Open)
  ^                |                                    |
  |                | [Up / Tap]                         | [Move > 8dp]
  |                v                                    v
  +----------- SELECT TAB                          DRAGGING (Panel Closed, Reordering)
  |                                                     |
  |             [Up / Drop]                             |
  +-----------------------------------------------------+
```

---

## Acceptance Criteria
- [x] Database upgraded to version 16 with `workspaces` table and `folders.workspace_id`.
- [x] Migration and onCreate seed `HOME` (id 1, pos 0) and `ARCHIVE` (id 2, pos 1); all existing folders land in `HOME`.
- [x] `WorkspaceRepository` enforces 10 custom workspace limit, 1..20 character trimmed/collapsed names, and case-insensitive uniqueness.
- [x] Reorder operations maintain dense positions (0..N) and keep `HOME` fixed at position 0.
- [x] Home tab bar displays `HOME`, `ARCHIVE`, and custom tabs with horizontal scroll, fixed right `(+)` button, and minimum 44dp touch target.
- [x] Empty state renders "No folders in <name> yet" with hint "Tap + to create one".
- [x] Add workspace dialog available from (+) and 3-dot overflow menu with instant keyboard focus.
- [x] Pure gesture state machine powers long-press hold panel and drag reordering with haptic ticks and clamping.
- [x] Backup export and import preserves workspaces and folder associations with legacy fallback.
- [x] Folder object item count includes photos, text notes, canvas drawings, and document notes across folder cards and delete stats.
- [x] All unit tests pass with zero regressions.
