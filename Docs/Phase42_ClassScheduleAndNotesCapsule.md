// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

# Phase 42 - ClassScheduleAndNotesCapsule

## Goal
Equip students with proactive daily academic schedule awareness directly inside Fotara by providing a compact 1-line dynamic schedule capsule in the Notes tab, a zero-dependency lightweight parser for Excel (`.xlsx`) and Word (`.docx`) timetable files, and an automated Next-Day Rollover Cutoff engine that switches between today's classes and tomorrow's preparation.

## Scope
- Database table `class_schedules` to persist weekly timetable slots (day of week, start time, end time, subject name, room/location, instructor, linked folder ID, color tag).
- 1-Line Compact Dynamic Schedule Capsule in `NotesScreen.kt` positioned strictly below `WorkspaceTabBar` and immediately above `NotesFilterChipsRow`.
- Lightweight streaming table parser for Excel (`.xlsx`) files extracting cell grids using Android platform `XmlPullParser` on ZIP XMLs (`sheet1.xml`, `sharedStrings.xml`) without Apache POI.
- Lightweight streaming table parser for Word (`.docx`) files extracting table rows and cells from `word/document.xml`.
- Interactive Smart Column Mapping Dialog with auto-detection of column roles (Day, Time, Subject, Room, Instructor) and preview grid.
- Next-Day Rollover Cutoff Engine with configurable `schedule_rollover_time` (Default: `18:00`), dynamically toggling between "Today's Schedule" and "Tomorrow's Schedule".
- Weekly Schedule Management Modal Sheet with full 5/7-day timetable view, manual entry, folder linking, and cutoff time configuration.
- Proactive scheduled notifications via Android `AlarmManager` (Morning digest at 06:30, H-10min pre-lecture heads-up with 1-tap quick capture deep link, and evening cutoff notification).

## Out Of Scope
- Android Home screen timetable widgets (handled in Phase 45).
- SQLite schema v19 Spaces multi-vault migration (handled in Phase 46).
- Live collaborative synchronization of timetables across peer devices (handled in Phase 51).

## Features

### 1-Line Compact Dynamic Schedule Capsule
- **Behavior**: Renders as a sleek, compact pill (~36dp height) directly beneath `WorkspaceTabBar`.
- **Pre-Cutoff Daytime State**: Displays ongoing class with live countdown or next upcoming class: `[ 🟢 Sekarang: Kalkulus II (R. 302) s.d 09:40 | 10:00 Fisika ▾ ]`.
- **Post-Cutoff Evening State**: Automatically switches to tomorrow's schedule: `[ 🌙 Persiapan Besok: 08:00 Kalkulus II (R. 302) • 3 Kelas ▾ ]`.
- **Interactivity**: Tapping opens the Weekly Schedule Management Sheet. Swiping right-to-left hides the capsule for the current session.

### Lightweight Tabular Parser Engine (.xlsx & .docx)
- **Zero Heavy Dependencies**: Unzips `.xlsx` and `.docx` archives directly; parses XML nodes streamingly via `XmlPullParser`. Keeps APK overhead under 150 KB.
- **Auto-Mapping**: Detects keywords in headers (`Hari`, `Day`, `Jam`, `Waktu`, `Mata Kuliah`, `Subject`, `Ruang`, `Room`).
- **Validation**: Rejects malformed files cleanly with a non-crashing user toast and provides manual fallback entry.

### Next-Day Rollover Cutoff Engine
- **Single Master Setting**: `schedule_rollover_time` (Default: 18:00).
- **Rollover Rules**:
  - `00:00` to `cutoff`: Shows today's classes.
  - `cutoff` to `23:59`: Shows tomorrow's classes.
  - Friday after cutoff: Intelligently skips weekend to Monday morning classes (or Saturday if 6-day cycle is enabled).

## UI Mockup

```
┌────────────────────────────────────────────────────────┐
│ ScreenHeader: Notes                             [ 🔍 ] [ ⋮ ] │
├────────────────────────────────────────────────────────┤
│ WorkspaceTabBar: [ All ] [ Semester 3 ] [ + ]          │
├────────────────────────────────────────────────────────┤
│ [ 🟢 Sekarang: Kalkulus II (R. 302) s.d 09:40    ▾ ]   │  <-- 1-Line Capsule
├────────────────────────────────────────────────────────┤
│ NotesFilterChipsRow: (● Semua) ( Teks ) ( Foto ) ( PDF)│
├────────────────────────────────────────────────────────┤
│ Feed Catatan:                                          │
│ Today                                                  │
│   • Catatan Kalkulus II Pagi Ini                       │
└────────────────────────────────────────────────────────┘
```

## Logic Notes
- Database Table `class_schedules`:
  ```sql
  CREATE TABLE class_schedules (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      day_of_week INTEGER NOT NULL, -- 1=Monday .. 7=Sunday
      start_minute INTEGER NOT NULL, -- minutes from midnight (e.g. 480 = 08:00)
      end_minute INTEGER NOT NULL,   -- minutes from midnight (e.g. 580 = 09:40)
      subject_name TEXT NOT NULL,
      room_name TEXT DEFAULT '',
      instructor_name TEXT DEFAULT '',
      linked_folder_id INTEGER,
      color_hex TEXT DEFAULT '#2563EB',
      created_at INTEGER NOT NULL,
      updated_at INTEGER NOT NULL
  );
  ```
- Current active class resolved via `Calendar.getInstance()`: matching `day_of_week` and checking `start_minute <= current_minute < end_minute`.

## Risks
- *Risk*: Student uploads non-standard or merged-cell Excel timetable.
  - *Mitigation*: Smart Column Mapping Dialog presents editable preview grid so students can adjust column roles before committing to SQLite.
- *Risk*: Device battery optimization delays `AlarmManager` pre-class notifications.
  - *Mitigation*: Use `setExactAndAllowWhileIdle` for pre-class heads-up alarms.

## Dependencies
- `NotesScreen.kt`, `WorkspaceTabBar.kt`, `SqliteFolderRepository.kt`.

## Acceptance Criteria
- 1-line schedule capsule displays strictly below `WorkspaceTabBar` and above `NotesFilterChipsRow`.
- Excel `.xlsx` file with schedule rows parsed into timetable entries within 500ms without crashing.
- Word `.docx` table file parsed correctly into timetable entries.
- Changing `schedule_rollover_time` to an earlier hour immediately triggers tomorrow's schedule display.
- Unit tests verify XML table extraction, time range calculations, and cutoff rollover transitions.
