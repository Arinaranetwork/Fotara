// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

# Phase 59 - SocialUsernameEcosystemAndScheduleWidgetConsolidation

## Goal
Implement a persistent Supabase-backed unique username system for peer discovery, expand the Friends hub with Followers/Following lists and centered empty states, consolidate home screen widgets into a dynamic Class Schedule widget, add class schedule reminder notifications with custom day rollover bounds, temporarily disable anti-procrastination alarms, and fix Modular Add-Ons bottom clipping.

## Scope
1. **Unique Username Claim & Profile System**:
   - Supabase `user_profiles` integration: unique username registration (`@username`).
   - Cooldown enforcement: username changes restricted to at most once per 7 days.
   - Uniqueness validation: duplicate usernames strictly prevented.
   - Sharing: generate QR code with `@username` and add friends directly by searching username.
2. **Friends Terminology & Followers/Following Screens**:
   - Universal terminology replacement: "Study Buddy" -> "Friend".
   - 3-dots menu option in Friends screen to navigate to "Followers" and "Following".
   - User profile detail modal/screen showing profile info, followers count, and following count.
   - Centered empty state layout in Friends screen (Photo 1) with increased height and balanced vertical alignment.
3. **Class Schedule Sample Data & Reminder Alerts**:
   - Pre-populated sample schedule data adhering to the database format for instant testing and display.
   - Settings pill option in Class Schedule screen (bottom-right) to configure automated class reminders (e.g. daily at 6 PM) sending a notification with the text `<day>`.
   - Custom day rollover bounds: prevent 24-hour setting (limit to at most 23 hours), allowing custom start and end times.
4. **Temporary Anti-Procrastination Alarm Disable**:
   - Render the Anti-Procrastination Alarm entry grayed out and disabled while maintaining visible placement.
5. **Modular Add-Ons Screen Polish & Add-On Removal**:
   - Fix bottom cut-off (Photo 2) in `PackageManagementHubScreen.kt` by applying adequate bottom content padding above the floating bottom navigation bar.
   - Remove mock/packaged add-ons while maintaining the screen shell.
6. **Consolidated Schedule Widget**:
   - Replace legacy widgets with a single dedicated "Schedule" widget.
   - Dynamically displays the current day's class schedule based on active day of week.
   - Shows "None" / clean empty state when no classes are scheduled for the day.

## Out Of Scope
- Third-party social network integrations (Google/Apple sign-in).
- Real-time video or audio chat between friends.
- Release APK packaging.

## Features

### 1. Supabase Unique Username Claim
- **Table & Schema**: Table `profiles` or `user_usernames` with columns `id`, `user_id`, `username`, `updated_at`, `created_at`.
- **Validation**: Lowercase alphanumeric + underscore, length 3–20 characters. Uniqueness enforced via unique constraint.
- **Rate Limit**: Store `last_username_change_at` timestamp. Disallow changes if `System.currentTimeMillis() - last_username_change_at < 7 * 24 * 60 * 60 * 1000L`.
- **QR Sharing**: Share profile link / QR code embedding `@username`.

### 2. Friends Hub Polish & Navigation
- **Renaming**: Update all strings from "Study Buddy" / "Buddies" to "Friend" / "Friends".
- **Followers & Following**: 3-dot overflow menu in Friends screen leading to Followers and Following tabbed views.
- **Vertical Alignment**: Centering the empty state illustration and text on `FriendsScreen.kt` (Photo 1) so it sits centered in the viewport.

### 3. Class Schedule Widget & Reminders
- **Schedule Widget**: Single Glance widget querying `class_schedules` for `dayOfWeek == currentDay`. Renders daily time slots, subject names, and rooms, or "None" if empty.
- **Reminder Engine**: Automated `AlarmManager` reminder triggered at user's configured hour (e.g. 18:00) notifying upcoming classes for `<day>`.
- **Rollover Limits**: Validate user-selected rollover time span: cannot span 24 hours (clamped to max 23 hours).

### 4. Modular Add-Ons Viewport Correction
- **Insets**: Add `bottom = 100.dp` content padding to LazyColumn in `PackageManagementHubScreen.kt` so content is never obscured by the bottom bar (Photo 2).
- **Clean Shell**: Clear mock package listings so screen renders clean empty state.

## Acceptance Criteria
- Users can claim a unique username with Supabase validation and a 7-day rate limit.
- Friends screen uses "Friend" terminology and provides Followers/Following views.
- Friends empty state is vertically centered and proportioned (Photo 1).
- Schedule sample data loads cleanly.
- Anti-procrastination alarm is visible but grayed out / disabled.
- Day rollover prevents 24-hour setting and accepts custom start/end times up to 23 hours.
- Modular Add-Ons screen is not cut off by the bottom bar and has no mock add-ons.
- Consolidated Schedule Widget displays daily classes or "None".
- Class schedule settings pill has configurable reminder notifications.
- All unit tests pass; release APK build omitted.
