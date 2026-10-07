// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

# Phase 40 - Release 1.8.3 Beta Hotfix

## Goal
Provide transparency and control over the anonymous device count identifier by surfacing the generated random UUID v4 in an expandable inspection panel directly below the "Share anonymous device count" toggle in Settings, allowing users to inspect and copy their anonymous ID.

## Scope
- `DeviceRegistry.kt`:
  - Expose reactive `deviceIdFlow: StateFlow<String?>` initialized from existing stored device ID.
  - Update flow synchronously whenever an ID is generated (`getOrCreateDeviceId`), deleted (`onDeviceCountDisabled`), or cleared.
- `SettingsUiState.kt`:
  - Add `registeredDeviceId: String? = null` field.
- `SettingsViewModel.kt`:
  - Accept optional `DeviceRegistry` in constructor and `provideFactory`.
  - Collect `deviceIdFlow` in `viewModelScope` and update `registeredDeviceId` in `SettingsUiState`.
- `Navigation.kt`:
  - Pass `appContainer.deviceRegistry` into `SettingsViewModel.provideFactory` at both navigation instantiation points.
- `SettingsScreen.kt`:
  - Implement `DeviceIdExpandablePanel` composable placed immediately below the `SettingsRowToggle` for "Share anonymous device count".
  - Gated on `registeredDeviceId != null`: only rendered if a device ID has actually been generated.
  - Collapsed by default (`isExpanded = false`) with 48dp touch target row, leading info icon, "Anonymous Device Identifier" label, and chevron arrow.
  - Expands smoothly to display the full UUID in a clean dark-dominant card with monospace styling, a "Copy ID" button with clipboard integration and toast feedback, and reassuring privacy explanatory text.
  - Complies with `Android.md` tokens (snapped 8dp corner radius, dark-dominant lightness <= 35%, 4n dp spacing, zero emoji, zero italics).
- `WhatsNewScreen.kt`:
  - Add 1.8.3 Beta entry documenting the expandable anonymous device ID panel.
- `app/build.gradle.kts`:
  - Bump `versionCode = 32`, `versionName = "1.8.3 Beta"`.
- Version records and release publication:
  - Create `/Docs/Version/1.0/1.8/1.8.3/Anchor.md` and `Release.md`.
  - Update `/Changelog/Changelog_1.8.md`.
  - Compile, sign, package `Output/Release/Fotara_1.8.3_Beta.apk` and publish to GitHub Releases.

## Out Of Scope
- Database schema changes (remains SQLite schema v18).
- Any hardware IDs, MACs, IMEIs, Advertising IDs, or user info collection (strictly prohibited).
- Modifications to Supabase RPC endpoints (`register_device` / `unregister_device`).

## Features
### Feature 1: Expandable Device ID Panel in Settings
- **Behavior**: If anonymous device count is enabled and an ID has been generated, an expandable panel appears directly below the toggle. Tapping the row toggles expansion. When expanded, the full UUID is shown along with a one-tap copy button and a privacy assurance disclaimer.
- **Edge cases**:
  - When device count is disabled, `DeviceRegistry.onDeviceCountDisabled()` clears the ID, setting `registeredDeviceId = null`, which instantly removes the panel.
  - When re-enabled, upon generation of the new random UUID, the panel reappears reflecting the new ID.
  - Handles clipboard copy on all supported Android versions (API 24-36) with non-blocking feedback.

## UI Mockup
```
Settings > About & Legal / Privacy:
+-------------------------------------------------------------+
| [Storage Icon] Share anonymous device count          [ ON ] |
| Sends a random ID and the app version so the developer ...  |
|                                                             |
| +---------------------------------------------------------+ |
| | [i] Anonymous Device Identifier                     [v] | | <- Tap to expand
| +---------------------------------------------------------+ |
+-------------------------------------------------------------+

When expanded:
+-------------------------------------------------------------+
| [Storage Icon] Share anonymous device count          [ ON ] |
| Sends a random ID and the app version so the developer ...  |
|                                                             |
| +---------------------------------------------------------+ |
| | [i] Anonymous Device Identifier                     [^] | |
| | ------------------------------------------------------- | |
| |  d1a49f7b-9c21-4b71-a083-d23019842c5b                  | |
| |                                                         | |
| |  [Copy ID]                                              | |
| |  Random local UUID v4. No hardware identifiers or       | |
| |  personal data are collected.                           | |
| +---------------------------------------------------------+ |
+-------------------------------------------------------------+
```

## Logic Notes
- State flow: `DeviceRegistry._deviceIdFlow` -> `SettingsViewModel.registeredDeviceId` -> `SettingsScreen`.
- Strict privacy: No device information beyond the locally generated UUID v4 is ever exposed or collected.

## Risks
- Gesture or recomposition lag in Settings lazy list -> Mitigated by using standard Compose state and keeping panel lightweight.

## Dependencies
- Phase 35 (`LegalConsentAndDialogCoordinator`), `DeviceRegistry.kt`.

## Acceptance Criteria
- Device ID panel appears below "Share anonymous device count" when a device ID is generated.
- Panel is collapsed by default and expands on click to reveal full UUID and copy button.
- Copy button places the UUID on the system clipboard and displays feedback.
- When device count is toggled off, panel disappears immediately.
- 100% of unit tests pass.
- Release APK `Fotara_1.8.3_Beta.apk` successfully built, packaged, and published to GitHub.
