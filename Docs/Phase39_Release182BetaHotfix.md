// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

# Phase 39 - Release 1.8.2 Beta Hotfix

## Goal
Resolve two visual and interaction defects reported in release 1.8.1 Beta:
1. Legal document text cut-off: Terms of Service (TOS) and Privacy Policy (PP) text cut off behind the floating bottom navigation bar and system navigation insets when scrolled to the end.
2. Workspace tab reorder animation replay: When dragging a tab to reorder, holding, and releasing, the tab snaps back and replays the animation instead of sitting stably in its new dropped position.

## Scope
- `LegalDocumentScreen.kt`:
  - Integrate `LocalBottomOverlayPadding` from `HomeBottomNavBar.kt`.
  - Provide dynamic bottom padding spacing ensuring that the last Markdown paragraph and footer notes scroll well above the floating navigation bar and system navigation insets.
- `WorkspaceTabBar.kt`:
  - Introduce optimistic local state `var localWorkspaces by remember(workspaces) { mutableStateOf(workspaces) }`.
  - Wrap tab items in `key(workspace.id)` inside the scrollable Row.
  - Synchronously commit reordered list to `localWorkspaces` at the drop point before resetting gesture state.
  - Zero translation (`0f`) when dragging terminates so tabs sit directly in their new natural layout positions without spring-back or animation replay.
- Version bump to `1.8.2 Beta` (`versionCode = 31`).
- Changelog and What's New documentation updates.

## Out Of Scope
- Database schema changes (remains SQLite schema v18).
- New network calls, dependencies, or permissions.
- Unrelated viewer, canvas, or OCR features.

## Features
### Feature 1: Dynamic Legal Document Bottom Clearance
- **Behavior**: When viewing Terms of Service or Privacy Policy in About & Legal / Privacy section, the scrollable markdown text column provides sufficient bottom clearance so the final sentence, date, and footer text sit fully above the floating pill navigation bar.
- **Edge cases**: Handles 3-button navigation, gesture navigation, tablet/landscape aspect ratios, and standalone usage where `bottomOverlayPadding` is 0dp by falling back to a generous 120dp spacer.

### Feature 2: Stable Drop Commit for Workspace Reordering
- **Behavior**: When dragging a custom or archive workspace tab past the threshold into an adjacent slot, adjacent tabs dynamically shift out of the way. Upon releasing the pointer, the dragged tab and adjacent tabs immediately sit stably in their newly assigned slots with zero bounce, snap-back, or second animation pass.
- **Error states**: If dragged off-screen vertically (>80dp) or canceled, gesture resets to original positions without altering `localWorkspaces`.

## UI Mockup
```
Legal Document Screen:
+---------------------------------------+
| [<] Terms of Service                  |
|     Version 1.0, effective ...        |
|---------------------------------------|
| 1. Acceptance of Terms                |
| ...                                   |
|                                       |
| 14. Contact Us                        |
| Contact: legal@arinara.internal       |
|                                       |  <- Dynamic Spacer (bottomOverlayPadding + 32dp)
|   [==============================]    |  <- Floating HomeBottomNavBar
+---------------------------------------+

Workspace Tab Bar Reordering:
[ Home ] [ Tab A (dragged) ] [ Tab B ] [ + ]
               \------->
[ Home ] [ Tab B (shifted) ] [ Tab A ] [ + ]
Release pointer -> Tabs sit immediately with zero replay.
```

## Logic Notes
- `LocalBottomOverlayPadding`:
  - `effectiveBottomPadding = if (bottomOverlayPadding > 0.dp) bottomOverlayPadding + 32.dp else 120.dp`
  - Consumed at the end of `LegalDocumentScreen`'s scrollable `Column`.
- `WorkspaceTabBar`:
  - Local state: `var localWorkspaces by remember(workspaces) { mutableStateOf(workspaces) }`
  - Reorder drop handler:
    ```kotlin
    val finalWorkspaces = WorkspaceReorderHelper.reorderList(localWorkspaces, workspace.id, targetSlotIndex)
    localWorkspaces = finalWorkspaces
    onReorderWorkspaces(finalWorkspaces.map { it.id })
    dragState = TabDragState(state = TabGestureState.IDLE)
    targetSlotIndex = -1
    ```
  - Translation:
    ```kotlin
    val translationX = if (isBeingDragged) {
        dragState.dragDeltaX
    } else if (isAnyDragging) {
        animatedShiftPx
    } else {
        0f
    }
    ```

## Risks
- Asynchronous SQLite update delay -> Mitigated by optimistic local state in `WorkspaceTabBar`.
- Dynamic bottom bar height variation -> Mitigated by reading `LocalBottomOverlayPadding` directly from composition local.

## Dependencies
- Phase 35 (`LegalDocumentScreen.kt`)
- Phase 34 (`WorkspaceTabBar.kt`, `WorkspaceReorderHelper.kt`)

## Acceptance Criteria
- [x] Terms of Service and Privacy Policy scroll completely clear of the floating navigation pill and system nav bar.
- [x] Workspace tab reordering commits cleanly on pointer up without animation replay or visual snapping.
- [x] Version bumped to 1.8.2 Beta (`versionCode = 31`, `versionName = "1.8.2 Beta"`).
- [x] 100% of unit tests pass (716 tests).
- [x] Release APK assembled, verified, and published to GitHub Releases.
