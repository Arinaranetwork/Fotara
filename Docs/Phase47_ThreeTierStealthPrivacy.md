// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

# Phase 47 - ThreeTierStealthPrivacy

## Goal
Implement Fotara's 3-Tier Stealth Privacy System, providing multi-layered protection for sensitive student study notes, personal academic journals, research materials, and exam questions across three increasing levels of stealth: Private Folders (Level 1), Ghost Workspaces (Level 2), and Stealth Spaces with `.nomedia` storage shielding (Level 3).

## Scope
- **Tier 1 (Private Folders)**:
  - Folders flagged with `is_private = 1`.
  - Hidden from default grid display.
  - Revealed by deliberate 2-second pull-down drag gesture on `HomeScreen` folder grid with biometric unlock fallback.
- **Tier 2 (Ghost Workspaces)**:
  - Workspaces flagged with `is_ghost = 1`.
  - Hidden from workspace horizontal tab strip by default.
  - Revealed/toggled via 1.5-second long-press on persistent `Home` bottom navigation tab.
  - Visual indicator feedback with snackbar confirmation ("Ghost Workspaces Revealed" / "Ghost Workspaces Shielded").
- **Tier 3 (Stealth Space Vault & Media Shielding)**:
  - Spaces flagged with `is_private = 1`.
  - Hidden from top header Space Switcher dropdown unless stealth vault mode is active.
  - Activated via 2-second long-press on top-left header brand title (`ScreenHeader`).
  - Automatic filesystem shielding: writes `.nomedia` sentinel files in directory root to prevent Android MediaScanner indexing of confidential study captures in device galleries.
- Reactive `ActiveSpaceManager` orchestration across `HomeScreen` and repositories.
- Complete unit test suite verifying stealth flags, toggle triggers, and `.nomedia` sentinel handling.

## Out Of Scope
- Hardware-backed secure enclave cryptographic key derivation (Phase 52).
- Remote cloud wiping or multi-party encryption sharing.

## Features

### Tier 1: Private Folders (Pull-Down Hold Gesture)
- Folder cards marked private are excluded from standard folder list queries.
- On the `HomeScreen` folder grid, a smooth pull-down gesture held for 2 seconds initiates the unlock prompt.
- Once authenticated, private folders render with a subtle lock emblem and distinct card outline.

### Tier 2: Ghost Workspaces (Home Tab Long-Press)
- Workspaces flagged `is_ghost = 1` are filtered out from `WorkspaceTabBar` during normal operation.
- Long-pressing the persistent bottom navigation `Home` tab for 1.5 seconds invokes `ActiveSpaceManager.toggleGhostWorkspaceVisibility()`.
- State is preserved during the active app lifecycle; backgrounding or lock re-shields the ghost workspaces.

### Tier 3: Stealth Space Vault & .nomedia Shielding
- Entire academic vaults (Spaces) marked private are invisible from the space selector.
- Long-pressing the top header "Fotara ▾" brand title for 2 seconds toggles stealth vault access.
- For private space assets, the app ensures a `.nomedia` file exists in the directory, preventing gallery apps and photo pickers from surfacing study photos outside Fotara.

## UI Mockup
```
┌────────────────────────────────────────────────────────┐
│ FOTARA ▾ [Hold 2s for Stealth Vault]     [ 🔍 ] [ ⚙️ ] │
├────────────────────────────────────────────────────────┤
│ TABS: [ All ] [ Biology ] [ Calculus ]                 │
│ (Ghost tabs hidden until 1.5s long-press on Home tab)   │
├────────────────────────────────────────────────────────┤
│  ↓ Pull-down 2s to reveal Private Folders...           │
│                                                        │
│  ┌──────────────┐  ┌──────────────┐                    │
│  │ 📁 Bio Notes │  │ 📁 Calculus  │                    │
│  └──────────────┘  └──────────────┘                    │
├────────────────────────────────────────────────────────┤
│  [ 🏠 Home (Hold 1.5s) ]   [ 📝 Notes ]   [ ⚙️ Settings ] │
└────────────────────────────────────────────────────────┘
```

## Logic Notes
- `ActiveSpaceManager` maintains `isGhostWorkspaceVisible` and `isStealthVaultUnlocked` `StateFlow` states.
- Repositories apply dynamic filtering: `if (!isGhostVisible) filter { !it.isGhost }`.
- Toggle operations trigger reactive Compose recomposition without database mutations.

## Risks
- *Risk*: Accidental activation of stealth modes by casual taps.
  - *Mitigation*: Enforce strict minimum hold durations (1.5s for bottom nav, 2.0s for header).
- *Risk*: Android media scanner indexing private photos before `.nomedia` is written.
  - *Mitigation*: Create `.nomedia` file synchronously prior to saving any photo in a private space directory.

## Dependencies
- Phase 46 (`ActiveSpaceManager`, `SpaceRepository`).
- Android.md navigation and gesture standards.

## Acceptance Criteria
- Level 1 Private Folders hidden until explicit pull-down unlock.
- Level 2 Ghost Workspaces revealed upon 1.5s long-press on Home tab.
- Level 3 Private Spaces revealed upon 2.0s long-press on header title.
- `.nomedia` sentinel prevents gallery indexing for private space media.
- Zero crashes, zero regressions in standard workspace/folder navigation.
