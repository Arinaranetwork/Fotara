<!-- --- Arinara Network (c) 2026 --- -->
<!-- Exclusive property of Arinara Network. -->
<!-- Unauthorized use, reproduction, distribution, or modification of this code, -->
<!-- in whole or in part, for any purpose, is strictly prohibited without prior -->
<!-- written consent from Arinara Network as sole legal owner of this codebase. -->

# Fotara Schedule and Alarm Ringing Architecture

## 1. Executive Summary
Fotara 1.5.0 introduces a unified study note scheduling engine that supports all five note types:
- Photo Notes (`photos`)
- Photo Groups (`photo_groups`)
- PDF Document Notes (`document_notes`)
- DOCX Document Notes (`document_notes`)
- Native Text Notes (`text_notes`)
- Canvas Notes (`canvas_notes`)

All scheduling operations are built upon the shared `SchedulableNote` interface and backed by Room/SQLite schema v13 with optimized B-tree indexes (`idx_*_scheduled_at`).

---

## 2. API Level Reliability & Ringing Matrix (API 24 to 36)

| Android Version / API | Exact Alarm Capability | Notification Permission | Ringing / Wake Strategy |
| :--- | :--- | :--- | :--- |
| **API 24 - 30** (Android 7.0 - 11) | Granted by default via manifest. `AlarmManager.setExactAndAllowWhileIdle()` wakes CPU from Doze. | Granted automatically at install time. | Heads-up notification on `IMPORTANCE_HIGH` channel with `AudioAttributes.USAGE_ALARM`. |
| **API 31 - 32** (Android 12, 12L) | `SCHEDULE_EXACT_ALARM` declared. Verified via `AlarmManager.canScheduleExactAlarms()`. | Granted at install time. | If exact alarm permission is revoked by user or system standby buckets, fallback to inexact `alarmManager.set()` and prompt via `Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM`. |
| **API 33** (Android 13) | `SCHEDULE_EXACT_ALARM` checked. | Runtime permission `POST_NOTIFICATIONS` required before display. | Pre-flight explanation sheet shown before prompting system dialog. If denied, deep-link to app notification settings. |
| **API 34 - 36** (Android 14, 15, 16) | `canScheduleExactAlarms()` enforced. `USE_EXACT_ALARM` declared for clock/reminder core functionality. | Runtime permission `POST_NOTIFICATIONS` enforced. | `USE_FULL_SCREEN_INTENT` permission declared. Verified via `NotificationManager.canUseFullScreenIntent()` for screen-off alarm presentation. |

---

## 3. Audio & Channel Architecture

### Notification Channels
1. **Study Reminders Channel (`fotara_study_reminders`)**:
   - Importance: `NotificationManager.IMPORTANCE_HIGH`
   - Sound: `RingtoneManager.getDefaultUri(TYPE_NOTIFICATION)`
   - Usage: `AudioAttributes.USAGE_NOTIFICATION_EVENT`
   - Content Type: `AudioAttributes.CONTENT_TYPE_SONIFICATION`
   - Vibration: Default system pattern

2. **Urgent Alarms Channel (`fotara_study_alarms`)**:
   - Importance: `NotificationManager.IMPORTANCE_HIGH`
   - Sound: `RingtoneManager.getDefaultUri(TYPE_ALARM)` fallback to `TYPE_RINGTONE`
   - Usage: `AudioAttributes.USAGE_ALARM`
   - Content Type: `AudioAttributes.CONTENT_TYPE_SONIFICATION`
   - Vibration Pattern: `longArrayOf(0, 800, 400, 800, 400, 800)`
   - Full-Screen Intent: Enabled with `setFullScreenIntent(openPendingIntent, true)` to wake device and present alarm heads-up even when keyguard is locked.

---

## 4. Power Management, Doze, and Lifecycle Resilience

1. **Doze Mode & Standby**:
   - Alarms use `AlarmManager.setExactAndAllowWhileIdle(RTC_WAKEUP, ...)` to ensure delivery during deep idle maintenance windows.
2. **Device Reboot & App Update**:
   - `BootReceiver` receives `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, and `QUICKBOOT_POWERON`.
   - Executes background query on `FotaraDbHelper.getUpcomingSchedules(System.currentTimeMillis())` to re-arm all pending alarms.
3. **Time Zone & Clock Changes**:
   - `BootReceiver` registers for `Intent.ACTION_TIME_SET` and `Intent.ACTION_TIMEZONE_CHANGED`.
   - Re-evaluates all scheduled notes against the new system epoch timestamp.
4. **Trash & Restore Lifecycle**:
   - Moving an item to trash invokes `NoteScheduleManager.cancelAlarmOnly(...)`, preventing stale alarms from firing while keeping the database timestamp intact.
   - Restoring an item checks `scheduledAt > now` and invokes `NoteScheduleManager.rearmAlarmIfFuture(...)`.
5. **Interactive Controls**:
   - **Open**: Direct deep-link intent to note.
   - **Snooze (10m)**: Re-arms schedule for `now + 10 minutes` via `NoteScheduleManager.scheduleNote(...)`.
   - **Dismiss**: Cancels active notification and silences alarm sound.
