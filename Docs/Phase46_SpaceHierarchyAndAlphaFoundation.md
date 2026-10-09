// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

# Phase 46 - SpaceHierarchyAndAlphaFoundation

## Goal
Establish the foundational multi-vault Space super-hierarchy architecture for the Fotara 2.0.0 Alpha milestone. This elevates Fotara from a single-workspace coursework organizer into an academic operating system capable of isolating multiple life/study spaces (e.g. "Personal Study", "University Semester 5", "Research Lab") with dedicated workspaces, folders, schedules, and foreign key cascades.

## Scope
- Database schema migration to SQLite v20 introducing the `spaces` table (`id`, `name`, `icon_key`, `color_hex`, `is_private`, `created_at`, `sort_order`).
- Foreign key migration adding `space_id` to `workspaces` table with automatic default space seeding ("Academic Vault").
- Top header Space Switcher dropdown (`Fotara ▾ [Space Name]`) with interactive modal sheet for switching active spaces, creating spaces, and customizing colors.
- Reactive `SpaceRepository` and `ActiveSpaceManager` providing reactive state flows across `HomeScreen`, `NotesScreen`, and `WorkspaceTabBar`.
- Unit test suite verifying space creation, switching, cascade constraints, and default fallback.

## Out Of Scope
- 3-Tier Stealth Privacy biometrics and .nomedia shielding (reserved for Phase 47).
- Academic Evaluation & Syllabus weighting algorithms (reserved for Phase 48).
- Modular `.fpkg` package dynamic loader (reserved for Phase 49).
- Live collaborative study canvas synchronization (reserved for Phase 51).

## Features

### Space Super-Hierarchy Data Architecture
- **Schema v20**:
  ```sql
  CREATE TABLE IF NOT EXISTS spaces (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      name TEXT NOT NULL,
      icon_key TEXT NOT NULL,
      color_hex TEXT NOT NULL,
      is_private INTEGER NOT NULL DEFAULT 0,
      created_at INTEGER NOT NULL,
      sort_order INTEGER NOT NULL DEFAULT 0
  );
  ```
- **Workspaces Integration**:
  - `workspaces` table includes `space_id INTEGER NOT NULL REFERENCES spaces(id) ON DELETE CASCADE`.
  - Automatic migration migration seeds `space_id = 1` ("Default Space") for all existing workspaces, folders, and notes.

### Top Header Space Switcher Dropdown
- **Placement**: Top-left brand title in `HomeScreen` and `NotesScreen`:
  - Renders `Fotara ▾ [Active Space Name]` with standard typography (22sp Title, `TextPrimary`, `ElmsSans`).
  - Tapping opens the `SpaceSwitcherBottomSheet`.
- **Space Switcher Sheet**:
  - Displays list of configured spaces with icon, name, workspace count, and active checkmark.
  - Action card `[ + Create New Space ]`.
  - Color palette selector and icon picker.

## UI Mockup
```
┌────────────────────────────────────────────────────────┐
│ FOTARA ▾ Academic 2026/2027              [ 🔍 ] [ ⚙️ ] │
├────────────────────────────────────────────────────────┤
│ WORKSPACES: [ All ] [ Calculus ] [ Physics ] [ + ]     │
├────────────────────────────────────────────────────────┤
│                                                        │
│  [ SPACE SWITCHER MODAL SHEET ]                        │
│  ┌──────────────────────────────────────────────────┐  │
│  │ 🎓 Academic 2026/2027 (Active)          [ ✓ ]    │  │
│  │ 🔬 Research & Laboratory                [   ]    │  │
│  │ 💼 Internship & Projects                [   ]    │  │
│  │ ──────────────────────────────────────────────── │  │
│  │ [ + Create New Space ]                           │  │
│  └──────────────────────────────────────────────────┘  │
│                                                        │
└────────────────────────────────────────────────────────┘
```

## Logic Notes
- When an active space is changed, `ActiveSpaceManager.activeSpaceIdFlow` emits the new ID.
- `WorkspaceTabBar`, `HomeScreen` folders grid, and `ScheduleCapsule` automatically re-query and filter down to the active space.
- Deleting a space triggers a foreign key cascade removing child workspaces, folders, notes, and photos, accompanied by a double-confirmation safeguard dialog.

## Risks
- *Risk*: Data isolation breach if a query omits the active `space_id` filter.
  - *Mitigation*: Enforce repository-level scoping where `getWorkspacesForActiveSpace()` is the primary reactive entry point.
- *Risk*: Migration failure from v19 to v20 on devices with extensive legacy notes.
  - *Mitigation*: Wrap SQLite `ALTER TABLE` in transaction with fallback check ensuring existing workspace data defaults safely to space ID 1.

## Dependencies
- Phase 42 (`FotaraDbHelper`, SQLite architecture).
- Phase 43 (`SaveableStateHolder` navigation state).
- Phase 45 (1.9.0 release baseline).

## Acceptance Criteria
- SQLite schema successfully migrates to v20 with `spaces` table created and default space seeded.
- Workspaces and folders filter strictly by the currently active space.
- Header title bar renders `Fotara ▾ [Space Name]` and opens the switcher modal.
- Switching space updates all sub-screens instantly with zero crash or memory leak.
- Unit test suite covers 100% of space repository operations and migration logic.
