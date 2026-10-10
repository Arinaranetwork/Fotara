// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

# Phase 50 - Dedicated In-App Friends System

## Goal
Implement a dedicated, privacy-focused, offline-first peer study network ("Friends System") for Fotara. Enables students to maintain academic study buddy contacts, broadcast real-time or local presence (offline, studying, in lecture, open to collaborate), discover peers via user tags (`@handle`) and QR share matrix codes, initiate study invitations, and access the friends network directly from a primary entry card in Settings positioned between Profile and General.

## Scope
- Domain and data model in `com.arinara.fotara.feature.friends.model`:
  - `StudyPresenceStatus` enum: `OFFLINE`, `STUDYING`, `IN_LECTURE`, `OPEN_TO_COLLAB`.
  - `FriendProfile` data class: `id`, `handle`, `displayName`, `avatarUrl`, `studyStatus`, `currentSubject`, `joinedAt`, `isFavorite`.
- Persistent repository interface and implementation in `com.arinara.fotara.feature.friends.data`:
  - `FriendsRepository` interface with `getFriendsFlow()`, `addFriend()`, `removeFriend()`, `updatePresence()`, `getMyHandle()`, `generateShareCode()`, `toggleFavorite()`, etc.
  - `LocalFriendsRepository`: Persistent implementation using SharedPreferences and JSON serialization with default profile seeding and validation.
- Composable UI components in `com.arinara.fotara.feature.friends.ui`:
  - `FriendsSettingsCard.kt`: Reusable settings entry card placed strictly between Profile and General in `SettingsScreen`.
  - `FriendsScreen.kt`: Full-featured friends screen featuring search bar, personal presence status selector chip row, buddy list grouped into Active Study Buddies and Offline, quick action buttons (`[ Invite to Study ]`, `[ Share Notes ]`), buddy deletion, favorite toggling, and add friend dialog.
  - `FriendQrDialog.kt`: QR code matrix visualizer for personal handle/share code and interactive scan dialog.
- Integration into `SettingsScreen.kt` and `AppContainer.kt`.
- Comprehensive unit test suite in `app/src/test/java/com/arinara/fotara/feature/friends/`:
  - `FriendProfileTest.kt`
  - `FriendsRepositoryTest.kt`

## Out Of Scope
- Multi-device cloud relay server or remote web sockets (collaborative canvas engine in Phase 51).
- Bluetooth Low Energy mesh discovery (reserved for future versions).
- External contact book synchronization.

## Features

### Friend Profile & Presence Models
- Four universal study presence statuses:
  - `OFFLINE`: Idle or outside study hours.
  - `STUDYING`: Actively engaged in revision or coursework.
  - `IN_LECTURE`: Attending university or school class lectures.
  - `OPEN_TO_COLLAB`: Welcoming peer study sessions, collaborative canvas, or note review.
- Attributes support unique student tag (`@handle`), full display name, avatar URI, active subject tag (e.g. "Linear Algebra", "Cell Biology"), joined timestamp, and favorite bookmark.

### Local Friends Repository
- Persistent local store tracking study contacts, my student handle, and current presence status.
- Share code generation: standard canonical format `FOTARA-FRIEND-<handle>-<hex>`.
- Graceful error handling with `Result<FriendProfile>`: validates `@` prefix, handle length, uniqueness, and display name constraints.

### Dedicated Settings Card & Screen Flow
- Settings integration: card rendered immediately below Profile card and above General card with distinct icon, badge count of active peers, and clear description.
- Friends screen provides seamless search filtering, quick presence switching, group categorization (Active Collaborators vs Offline Contacts), and QR matrix code modal.

## UI Mockup
```
┌────────────────────────────────────────────────────────┐
│ ←  Study Buddies & Friends               [ QR ] [ + ]  │
├────────────────────────────────────────────────────────┤
│ [ 🔍 Search friends or @handles...                   ] │
├────────────────────────────────────────────────────────┤
│ MY PRESENCE:                                           │
│ [ ● Studying: Biology ] [ In Lecture ] [ Open Collab ] │
├────────────────────────────────────────────────────────┤
│ ACTIVE COLLABORATORS (2)                               │
│ ┌────────────────────────────────────────────────────┐ │
│ │ 🟢 @alex_m — Alex Miller             [ Invite ] [ ★ ]│ │
│ │    Studying Organic Chemistry                      │ │
│ └────────────────────────────────────────────────────┘ │
│ OFFLINE BUDDIES (3)                                    │
│ ┌────────────────────────────────────────────────────┐ │
│ │ ⚪ @sarah_k — Sarah Kim               [ Share ]  [   ]│ │
│ │    Last seen yesterday                             │ │
│ └────────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────┘
```

## Logic Notes
- Presence updates immediately update StateFlow and persist to preferences.
- Search filter performs case-insensitive query against both `displayName` and `@handle`.
- Adding friend auto-formats handle to guarantee lowercase with leading `@`.
- QR dialog renders dynamic vector matrix pattern based on hash of share code, ensuring 100% offline generation without external image service.

## Risks
- Corrupt JSON in SharedPreferences -> Mitigated with safe fallbacks and empty list default.
- Handle collision -> Mitigated with unique ID generation and handle uniqueness validation.

## Dependencies
- Android.md design tokens: MidnightNavy base (`#0A0D14`), Card surface (`#111726`), Primary (`#2563EB`), Accent (`#EFE8DA`).
- Typography: ElmsSans hierarchy with Title (22sp), Subtitle (16sp), Content (14sp).

## Acceptance Criteria
- `FriendProfile.kt` defines `StudyPresenceStatus` and `FriendProfile`.
- `FriendsRepository.kt` defines interface and persistent `LocalFriendsRepository`.
- `FriendsSettingsCard.kt` rendered strictly between Profile and General in `SettingsScreen`.
- `FriendsScreen.kt` and `FriendQrDialog.kt` provide complete peer management UI with zero placeholders.
- 100% unit tests pass via `./gradlew.bat testDebugUnitTest --tests "com.arinara.fotara.feature.friends.*"`.
