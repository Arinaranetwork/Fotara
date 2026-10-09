// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

# Phase 43 - TabPersistenceAndNavigation

## Goal
Eliminate state loss when switching bottom navigation tabs and introduce canonical iOS/Android navigation behavior: preserve sub-screen composition states (active setting sections, scroll offsets, form fields) across tab switches via `SaveableStateHolder`, and support canonical bottom nav re-tap actions (pop-to-root when on a child screen, and smooth scroll-to-top when at the root of a tab).

## Scope
- Composition state retention across `Home`, `Notes`, and `Settings` using Compose `SaveableStateHolder` in `HomeScreen.kt`.
- Tab re-tap detection in `HomeBottomNavBar.kt` via `onTabReSelected(tab: HomeNavTab)`.
- Canonical Re-Tap Pop-to-Root: If the active tab has a visible sub-screen (e.g., child section in Settings, active legal document, or active search/selection mode), re-tapping the active tab pops back to root.
- Canonical Re-Tap Scroll-to-Top: If the active tab is already at root, re-tapping triggers an animated smooth scroll to the top of the list or grid (`animateScrollToItem(0)`).
- Back handler harmony: Back button and re-tap behavior coordinate smoothly without corrupting navigation history.

## Out Of Scope
- Multi-stack deep navigation backstack replacement (no complex third-party navigation library; pure Compose state architecture).
- Gesture-based swipe between tabs (tabs are explicitly switched via bottom navigation pill).

## Features

### Sub-Screen State Persistence Across Bottom Tabs
- **Behavior**: When the user navigates into a sub-section in Settings (e.g. Storage, Profile, Notifications), switches to Notes or Home, and then returns to Settings, the active sub-section and its state are preserved rather than reset to the root screen.
- **Implementation**: Wrap bottom navigation tab contents with `SaveableStateHolder.SaveableStateProvider(tab)` so Compose remembers the state of non-active tabs.

### Canonical Re-Tap Pop-to-Root
- **Behavior**: When on a sub-screen of any tab (e.g. `SettingsScreen` inside a sub-section, or `LegalDocumentScreen`), tapping the currently selected bottom nav tab immediately pops back to the root of that tab.
- **Handling**: Exposes a reset/pop-to-root callback or hoisted state to gracefully reset child screens to root.

### Canonical Re-Tap Scroll-to-Top
- **Behavior**: When the user is already on the root of the active tab (`Home`, `Notes`, or `Settings`), tapping the tab icon/label initiates a smooth programmatic scroll to index 0 (`animateScrollToItem(0)` or `animateScrollTo(0)`).
- **Haptic/Visual**: Smooth deceleration curve without jumping.

## UI Mockup

```
┌────────────────────────────────────────────────────────┐
│ Settings > Storage & Data Management                   │  <-- Sub-screen
│ [ Back ]                                               │
│                                                        │
│ Clean Cache, App Residue, Disk Breakdown...            │
│                                                        │
├────────────────────────────────────────────────────────┤
│ Floating Bottom Bar:                                   │
│ [ Home ]      [ Notes ]      [ Settings (Active) ]     │  <-- Re-tap here
└────────────────────────────────────────────────────────┘
                       ↓ Re-tap Action
┌────────────────────────────────────────────────────────┐
│ Settings (Root Screen)                                 │  <-- Pops to root!
│ • Profile & Appearance                                 │  (Or scrolls to
│ • Storage & Data Management                            │   top if already
│ • About & Legal                                        │   at root)
└────────────────────────────────────────────────────────┘
```

## Logic Notes
- In `HomeBottomNavBar.kt`, detect re-clicks:
  ```kotlin
  val isCurrent = selectedTab == tab
  HomeNavItem(
      isSelected = isCurrent,
      onClick = {
          if (isCurrent) {
              onTabReSelected?.invoke(tab)
          } else {
              onTabSelected(tab)
          }
      }
  )
  ```
- In `HomeScreen.kt`:
  - Provide `rememberSaveableStateHolder()`.
  - Maintain `settingsActiveSection` or reset trigger for Settings.
  - On re-tap:
    - If `SETTINGS`: if in sub-screen -> pop to root; else -> scroll root list to top.
    - If `NOTES`: scroll notes LazyColumn to 0.
    - If `HOME`: if search active or multi-select -> exit to root; else -> scroll LazyVerticalGrid to 0.

## Risks
- *Risk*: `SaveableStateHolder` memory footprint if many large images remain loaded in dormant tabs.
  - *Mitigation*: Images are memory-cached by Coil with LRU bounds; Compose only retains UI states.
- *Risk*: Rapid double tap triggers both pop-to-root and scroll-to-top simultaneously.
  - *Mitigation*: Check whether sub-screen was open when clicked; pop-to-root consumes the first tap.

## Dependencies
- `HomeScreen.kt`, `HomeBottomNavBar.kt`, `SettingsScreen.kt`, `NotesScreen.kt`.

## Acceptance Criteria
- Switching between Home, Notes, and Settings retains active form inputs and opened sections in Settings.
- Re-tapping Settings while inside a sub-screen immediately returns to the Settings root menu.
- Re-tapping an active root tab smoothly scrolls the list/grid to index 0.
- Unit tests verify re-tap state machines and navigation callbacks.
