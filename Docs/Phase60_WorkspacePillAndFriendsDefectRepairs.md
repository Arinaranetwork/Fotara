# Phase 60 - WorkspacePillAndFriendsDefectRepairs
## Goal
Resolve production defects reported in Fotara 2.2.2: repair the broken workspace pill with blank content on Home and Notes screens, and eliminate all mock/placeholder accounts ("Jordan Baker", "Sarah Kim", etc.) and fake seeders in the Friends ecosystem to restore 100% production-ready empty states (Rule R-005).

## Scope
- Guarantee workspace tab resilience:
  - If workspace query returns empty (initial load or new/migrated space), `WorkspaceTabBar` safely renders default "Home" and "Archive" tabs instead of an empty 48dp container pill.
  - In `SqliteWorkspaceRepository`, initialize `workspacesFlow` with default Home/Archive workspaces rather than `emptyList()`.
  - In `repairDanglingWorkspaces()`, auto-heal any workspace rows with missing/null `space_id` (`UPDATE workspaces SET space_id = 1 WHERE space_id IS NULL OR space_id <= 0`), purge corrupted custom workspaces with blank names (`DELETE FROM workspaces WHERE kind = 'CUSTOM' AND (name IS NULL OR TRIM(name) = '')`), and ensure Home/Archive entries for space 1 are correctly associated.
  - In `WorkspaceTabBar`, filter out custom workspaces with blank names.
  - In `Workspace.getDisplayName()`, enforce fallback to localized Home/Workspace name if custom workspace name is blank, preventing blank tab pills or empty titles in `ActiveSearchBar` and empty states.
- Eliminate mock/placeholder profiles in Friends system:
  - Remove `seedFollowers()` and `seedFollowing()` from `LocalFriendsRepository.kt`.
  - In `loadFollowers()` and `loadFollowing()`, return `emptyList()` by default and sanitize existing SharedPreferences by purging any legacy seed IDs (`seed-follower-*`, `seed-following-*`) and dummy handles (`@jordan_b`, `@sarah_k`, `@alex_w`, `@taylor_m`).
  - Sanitize hints in `FriendQrDialog` and `FriendsScreen` to use generic `@username` and `Display Name` rather than specific dummy personas ("Alex Miller").
  - Update unit tests (`FriendsRepositoryTest.kt`, `FriendsViewModelTest.kt`) to verify initial empty state counts (0 followers, 0 following) and validate adding/following peers from an authentic zero baseline.

- Peer profile discovery by username, banner integration & Supabase connect:
  - Add `fetchProfileByUsername` in `UsernameManager.kt` allowing lookups by academic `@username` from Supabase `user_profiles`.
  - Add `GRANT SELECT, INSERT, UPDATE ON public.user_profiles TO anon, authenticated;` in `Docs/Supabase/user_profiles.sql` to resolve PostgREST 401 permission errors.
  - Add `findUserByHandle(handle: String)` in `FriendsRepository.kt` combining local friend matches and remote Supabase profile queries.
  - In `FriendsScreen.kt` search bar: when a query matches no local friends, render a "View Profile for @handle" action card that resolves and opens `UserProfileDialog`.
  - In `AddFriendDialog`: allow adding directly by `@username` with auto-resolution of display name from Supabase if left blank.
  - In `UserProfileDialog`: bind user's real profile banner (`userProfile.bannerPath` from `SettingsRepository`) when viewing own profile, and provide academic gradient banner for peers; display avatar initials/image, status dot, subject focus, and interactive followers/following counter tabs.

## Out Of Scope
- New social network features or backend protocol changes.
- Multi-user real-time chat.

## Features
### Resilient Workspace Tab Bar
- Behavior: `WorkspaceTabBar` always renders valid tabs (at minimum Home and Archive) regardless of initial flow loading state or database latency.
- Edge Cases: New space with no workspaces yet, migrated space with null `space_id`, corrupt custom workspace with empty name.
- Failure Behavior: Gracefully defaults to standard tabs with no layout jumps or blank pills.

### Authentic Zero-Placeholder Friends Ecosystem
- Behavior: Clean authentic empty states (`FriendsEmptyState`, `FollowersEmptyState`, `FollowingEmptyState`) display centered when user has no contacts.
- Edge Cases: Existing install having old seeded entries in SharedPreferences -> automatically purged on repository load.

### Real Peer Profile Discovery by Username & Banner Integration
- Behavior: Users can view any peer profile (banner, avatar, followers count, following count, status) by clicking them in Friends, Followers, or Following lists, or by searching any academic `@username` in the search bar.
- Edge Cases: Offline or username not found -> graceful fallback card or toast; Supabase permissions -> proper GRANT in SQL migration.

## UI Mockup
```
Workspace Bar:
+-----------------------------------------------------------+
|  [ (grid) Home ]   [ (archive) Archive ]   [ (folder) ... ] |
+-----------------------------------------------------------+

Followers (Empty):
                     (person icon)
                  No Followers Yet
      When other students follow your study profile,
               they will appear here.

Search By Username:
+-----------------------------------------------------------+
|  Academic Peer Found: @alex                               |
|  [ View Academic Profile ]                                |
+-----------------------------------------------------------+
```

## Logic Notes
- Database consistency: `repairDanglingWorkspaces` runs on boot in `SqliteWorkspaceRepository.init`.
- SharedPreferences sanitation: `parseList(raw).filterNot { it.id.startsWith("seed-") }` and rewrite if cleaned.
- Supabase lookup: `GET /rest/v1/user_profiles?username=eq.<clean>&select=id,install_uuid,username,display_name,updated_at`.

## Risks
- Supabase offline / network error -> gracefully resolve to offline fallback profile with zero crashes.

## Dependencies
- Phase 57 (Workspaces), Phase 50 & 59 (Friends System).

## Acceptance Criteria
- Zero mock profiles in production code and SharedPreferences runtime.
- `FollowersEmptyState` and `FollowingEmptyState` render when lists are empty.
- `WorkspaceTabBar` never renders as an empty container pill without tabs.
- Users can view a profile with banner, avatar, followers, and following count by clicking any friend/follower/following card OR by typing their `@username` into the search bar.
- All unit tests pass cleanly.

