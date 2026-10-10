# Phase 54 - FriendsUiThemeAndWindowInsetsPolish
## Goal
Harmonize the Friends / Study Buddies UI with the rest of Fotara Settings by eliminating oversaturated accents and card discrepancies, and fix the system status bar overlap so top navigation elements and headings never collide with the hardware battery, clock, and notification area.

## Scope
- Harmonize `FriendsSettingsCard.kt` to match `SettingsCardItem` design system (neutral slate icon tile `#1E2638`, muted icon tint `#94A3B8`, standard 16sp title, 13sp light subtitle, 20dp chevron, and elimination of bright green badges).
- Fix hardware status bar overlap in `FriendsScreen.kt` using `WindowInsets.statusBars` / `Modifier.statusBarsPadding()`.
- Standardize `FriendsScreen` top app bar with `SettingsSubScreenHeader` conventions (40dp circular back button `#131925`, white back arrow, 24sp Medium title, right-aligned action buttons).
- Desaturate and polish `FriendsScreen` presence chips, search bar, contact cards, dialogs, and action buttons to adhere to Arinara dark-dominant tokens (lightness <= 35%).
- Ensure all automated unit tests pass and build a clean release APK (`Fotara_2.1.1_Alpha.apk`).

## Out Of Scope
- Modifying backend SQLite schema for Friends.
- Cloud synchronization or remote WebSocket networking for peer discovery.
- Altering other unrelated Settings screens or components.

## Features
### FriendsSettingsCard Harmonization
- Visual Structure: Directly mirrors `SettingsCardItem` with container `HomeCardSurface` (`#111726`), border `BorderStroke(1.dp, HomeCardBorder)` (`#232B56`), 22dp radius, and 16dp content padding.
- Leading Tile: 42dp x 42dp box with 12dp rounded corners and background `Color(0xFF1E2638)`.
- Icon: `Icons.Outlined.People` in muted tint `Color(0xFF94A3B8)` (22dp size).
- Text: Title "Study Buddies" (16sp Medium Color.White), subtitle (13sp Light HomeSubtitleGray) indicating online buddy count or connection prompt.
- Trailing Chevron: 20dp chevron in `Color(0xFF64748B)`.

### FriendsScreen Status Bar Inset & Header Unification
- Hardware Inset Clearance: Root container or header applies `WindowInsets.statusBars` / `Modifier.statusBarsPadding()`, ensuring the top app bar is positioned below the hardware cutout, camera, clock, battery, and notification indicators on all devices.
- Header Structure: Standard `SettingsSubScreenHeader` with title "Study Buddies", 40dp circular back button with `#131925` background, and right-aligned actions for QR modal and Add Buddy modal.

### Friends Screen Desaturation & Color Harmonization
- Presence Chips: Replaced bright saturated colored backgrounds with dark-dominant fills (`#151D2C`, `#1A2336`, lightness <= 35%), subtle indicator dots, and muted borders.
- Action Buttons: Outlined buttons with subtle borders and muted text, avoiding aggressive saturated fills.
- QR Dialog: Desaturate pill switcher and action buttons to dark slate tokens.

## UI Mockup
```
+----------------------------------------------------+
|  [Phone Hardware Status Bar: 12:45 | 5G | 98%]    |
+----------------------------------------------------+
|                                                    |
|  (<-)  Study Buddies                   [QR] [+]    |
|                                                    |
|  [ Search study buddies or @handle...        [Q] ] |
|                                                    |
|  +-----------------------------------------------+ |
|  | My Study Presence                     @alex   | |
|  | Focus: Calculus II                            | |
|  | [O Offline]  [* Studying]  [* Collab]         | |
|  +-----------------------------------------------+ |
|                                                    |
|  Active Collaborators                          (2) |
|  +-----------------------------------------------+ |
|  | [JD*] John Doe                    [Invite] [*]| |
|  |       @john_d • Studying Physics               | |
|  +-----------------------------------------------+ |
+----------------------------------------------------+
```

## Logic Notes
- `FriendsSettingsCard`: Computes single-line subtitle based on `activeBuddiesCount` and `totalBuddiesCount`.
- `FriendsScreen`: Maintains state observation from `FriendsViewModel`, handling navigation back clicks, QR dialog toggling, Add Friend modal, presence subject updates, and deletion confirmations.
- Status Bar Insets: Uses Compose Foundation `WindowInsets.statusBars` or `Modifier.statusBarsPadding()`.

## Risks
- Device variation in notch/punch hole sizes -> Handled by official Android WindowInsets APIs (`statusBarsPadding`).
- Theme inconsistency across dialogs -> Verified against `Android.md` tokens and `SettingsCardSurface` / `HomeCardSurface`.

## Dependencies
- Phase 50 (`FriendsSystemAndSettings`), Phase 53 (`VersionRollbackManager`).

## Acceptance Criteria
- `FriendsSettingsCard` visually matches `SettingsCardItem` in color, corner radius, tile sizing, and typography.
- No header elements in `FriendsScreen` overlap the device hardware status bar, clock, or battery icons.
- All presence chips, search bars, and dialogs in `FriendsScreen` have fills <= 35% lightness and no oversaturated accents.
- All unit tests pass cleanly (896+ tests).
- Shippable APK `Fotara_2.1.1_Alpha.apk` generated.
