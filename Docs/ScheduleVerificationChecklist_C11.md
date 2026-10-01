<!-- --- Arinara Network (c) 2026 --- -->
<!-- Exclusive property of Arinara Network. -->
<!-- Unauthorized use, reproduction, distribution, or modification of this code, -->
<!-- in whole or in part, for any purpose, is strictly prohibited without prior -->
<!-- written consent from Arinara Network as sole legal owner of this codebase. -->

# Study Note Schedules & Alarms Verification Checklist (C11)

This document specifies the end-to-end device testing steps to execute on physical Android devices and emulators during the final validation chunk (C11).

---

## 1. Prerequisites
- Target device running Android 7.0 (API 24) to Android 14+ (API 34/35/36).
- Fotara 1.5.0 installed with Room database upgraded to v13.
- Device volume set to audible level for Alarm and Notification streams.

---

## 2. Manual Device Test Matrix (C11)

| Test ID | Scenario | Procedure | Expected Verification | Pass / Fail |
| :--- | :--- | :--- | :--- | :--- |
| **SCH-01** | Create Schedule on Photo Note | Open a photo note -> Tap Overflow (⋮) -> Tap "Schedule..." -> Enter "Review Chapter 1", select date + time 2 min in future -> Choose "Notification" -> Save. | Card displays bottom-right schedule badge (`CardScheduleBadge`). Detail view displays under-title chip. | [ ] |
| **SCH-02** | Create Schedule on Photo Group | Open a photo group -> Tap Overflow (⋮) -> Tap "Schedule..." -> Enter "Group Flashcard Session" -> Choose "Alarm Ring" -> Save. | Group card shows badge with alarm icon. Detail view displays under-title chip. | [ ] |
| **SCH-03** | Create Schedule on PDF Document Note | Open PDF document -> Tap Overflow (⋮) -> Tap "Schedule..." -> Select future time -> Save. | Card shows badge in bottom-right without colliding with LinkIt glow or color dot. | [ ] |
| **SCH-04** | Create Schedule on DOCX Document Note | Open DOCX document -> Tap Overflow (⋮) -> Tap "Schedule..." -> Select future time -> Save. | Badge displays on card. Chip displays under document name in viewer screen. | [ ] |
| **SCH-05** | Create Schedule on Native Text Note | In Text Note Editor -> Tap Schedule button in toolbar (Group 6, after word count) or Overflow -> Save schedule. | Badge displays on text note card. Chip displays under note title in both Edit and Preview modes. | [ ] |
| **SCH-06** | Fire Alert with Screen Off (Doze) | Schedule an Urgent Alarm note for 3 minutes in future. Lock device screen immediately. Wait for fire time. | Screen wakes up or presents high-priority heads-up full-screen alarm alert with alarm ringtone, note title, and custom schedule name. | [ ] |
| **SCH-07** | Device Reboot Resilience | Schedule two notes (one notification, one alarm) for +15 minutes. Reboot the device (`adb reboot`). | Upon reboot completion, `BootReceiver` triggers `rescheduleAllUpcoming()`. Alarms fire at the scheduled moment without opening app. | [ ] |
| **SCH-08** | Clock & Time Zone Change | Schedule note for 4:00 PM. Change device system time zone in Android Settings (e.g. UTC+7 to UTC+9). | `BootReceiver` receives `TIMEZONE_CHANGED` / `TIME_SET`. Schedule calculation remains epoch-consistent and fires at the intended UTC instant. | [ ] |
| **SCH-09** | Permission Denial Fallback | On Android 12+, revoke "Alarms & Reminders" exact alarm permission in Settings -> In Fotara, attempt to set schedule. | Explanation dialog appears with "Open Settings" shortcut and "Continue Anyway" fallback. | [ ] |
| **SCH-10** | Snooze Action (10 Minutes) | When notification fires, tap "Snooze (10m)". | Active notification dismissed immediately. Alarm re-armed for exact current time + 10 minutes. Fires 10 minutes later. | [ ] |
| **SCH-11** | Dismiss Action | When alarm/notification fires, tap "Dismiss". | Notification dismissed and audio stops immediately. No lingering background playback. | [ ] |
| **SCH-12** | Deep Link Open Note | When notification fires, tap "Open" action or tap notification body. | App launches directly and opens the specific scheduled note (Photo, Group, PDF, DOCX, or Text Note). | [ ] |
| **SCH-13** | Edit Existing Schedule | Tap existing under-title schedule chip on note screen -> Modify time to +30 min -> Save. | Database updated, badge reflects new time, previous AlarmManager pending intent replaced. | [ ] |
| **SCH-14** | Trash & Restore Lifecycle | Move note with active schedule to Trash. Check alarms -> Restore note from Trash while time is still in future. | Trashing cancels AlarmManager pending intent. Restoring re-arms the alarm because `scheduledAt > System.currentTimeMillis()`. | [ ] |
| **SCH-15** | Move Note Consistency | Move scheduled note to a different folder or subfolder. | Schedule remains active and fires normally. | [ ] |
