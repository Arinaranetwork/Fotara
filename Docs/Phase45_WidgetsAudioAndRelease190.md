// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

# Phase 45 - WidgetsAudioAndRelease190

## Goal
Deliver high-impact daily study ergonomics and sensory study support to bridge Fotara into the 1.9.0 release. This phase introduces interactive Android Home Screen Widgets (Next-Day Timetable 4x2/4x4, Single Photo Pin, and Coursework Photo Carousel), 1-tap Synchronized Audio Annotations with optional PDF page anchoring, an Urgent Anti-Procrastination Alarm dismissable only via coursework Photo Proof, and final packaging of the Fotara 1.9.0 Beta release.

## Scope
1. **Android Home Screen Timetable Widget (`AppWidgetProvider` / Jetpack Glance)**:
   - 4x2 and 4x4 interactive home screen widgets.
   - Real-time synchronization with the Phase 42 `ScheduleCutoffEngine` and `ScheduleRepository`:
     - Pre-cutoff (e.g. before 18:00): displays today's schedule and current/upcoming lectures.
     - Post-cutoff (e.g. after 18:00): automatically rolls over to tomorrow's classes (or Monday if Friday evening).
   - Tapping an item opens Fotara and navigates directly to the linked folder or Notes tab.
2. **Android Home Screen Photo Widgets**:
   - **Specific Single Photo Pin Widget**: Configuration activity allowing the student to select and pin 1 specific formula sheet, diagram, or textbook page permanently on the home screen.
   - **Coursework Carousel Widget**: 4x2 widget displaying recent coursework captures with left/right or auto-cycle paging.
3. **Synchronized Audio Annotations**:
   - 1-tap instant lecture/study audio recorder (AAC/m4a format, up to 5 minutes duration) with low-overhead file storage in internal app storage.
   - Playback bar with play/pause, waveform or progress scrubber, and timestamp.
   - Strictly optional PDF page drag/number anchoring allowing voice notes to link to specific PDF pages or study notes.
4. **Urgent Anti-Procrastination Alarm**:
   - High-priority system alarm stream (`AlarmManager.setExactAndAllowWhileIdle()` + full-screen intent).
   - Dismiss challenge: the alarm cannot be dismissed by simple swipe; student must provide **Photo Proof** (launching camera and capturing a photo of their study desk/homework) to silence the alarm.
5. **Fotara 1.9.0 Beta Release Packaging**:
   - Version bump in `app/build.gradle.kts`: `versionCode 34`, `versionName "1.9.0 Beta"`.
   - Update `Changelog/Changelog_1.9.md` and What's New dialog.
   - Assemble signed release artifact `Fotara_1.9.0_Beta.apk` in `/Output/Release/`.
   - Verify 100% test pass on full unit test suite.

## Out Of Scope
- Cloud speech-to-text / transcription APIs (remains offline-first).
- Cloud sync of audio files.
- Space Super-Hierarchy and Schema v19 (reserved for Phase 46 in 2.0.0).
- Collaborative canvas rooms (reserved for Phase 51 in 2.0.0).

## Features

### Timetable Home Screen Widget
- **Sizes**: 4x2 (compact, next 2-3 classes), 4x4 (expanded, full day schedule).
- **Rollover**: Subscribes to `ScheduleCutoffEngine.isAfterCutoff()`. When the cutoff time is crossed or on system date/time change, widget updates its remote views immediately.
- **Visuals**: Dark background (`HomeNearBlack` `#0A0D14`), `HomeCardSurface` (`#111726`) class cards, `FolderTabCream` (`#EFE8DA`) subject titles, and `TagAmber` time chips.
- **Deep Linking**: Tapping any class opens `MainActivity` with deep link intent to the linked subject folder or timetable management sheet.

### Photo Widgets
- **Single Photo Pin Widget**:
  - Configuration dialog allows browsing folders and picking a single note/photo.
  - Renders downsampled bitmap respecting device aspect ratio with subtle rounded corners (snapped to 16dp).
  - Tapping opens the full-screen photo inspector.
- **Coursework Carousel Widget**:
  - Displays today's captured photos or the most recent 10 coursework notes.
  - Next/Previous button controls or stack view.

### Synchronized Audio Annotations
- **Audio Engine**: `MediaRecorder` encoding standard AAC/M4A at 64kbps, mono, 44.1kHz. File storage in `context.filesDir/audio/`.
- **Max Duration**: Hard cap at 5 minutes (300 seconds) with countdown timer and automatic graceful stop.
- **Data Persistence**: `audio_annotations` table in SQLite (`id`, `note_id`, `pdf_doc_id`, `pdf_page_index`, `file_path`, `duration_ms`, `created_at`).
- **UI Component**: Compact `AudioRecordPill` and `AudioPlaybackBar` (48dp height) embedded optionally in note inspectors and PDF page editor.

### Urgent Anti-Procrastination Alarm
- **Alarm Engine**: `AlarmManager` with `USE_EXACT_ALARM` / `setExactAndAllowWhileIdle()`, ringing on `AudioManager.STREAM_ALARM` with max volume loop.
- **Dismiss Challenge**:
  - Fullscreen dismiss activity: `PhotoProofChallengeActivity`.
  - Displays alert title (e.g. "Calculus Problem Set 4") and camera viewfinder.
  - Student must capture a photo. On capture, alarm audio stops, photo is optionally saved to the target folder, and alarm is dismissed.
  - Fallback emergency PIN (configurable in settings) in case camera is unavailable.

## UI Mockup
```
┌────────────────────────────────────────────────────────┐
│ HOME SCREEN WIDGET: Class Timetable (4x2)              │
├────────────────────────────────────────────────────────┤
│ [ Fotara ]  Tomorrow's Classes (Post-18:00)           │
│ ┌────────────────────────────────────────────────────┐ │
│ │ 08:00 - 09:30 │ Calculus II          │ Hall A-101  │ │
│ ├────────────────────────────────────────────────────┤ │
│ │ 10:00 - 11:30 │ General Physics      │ Lab 3       │ │
│ └────────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────┐
│ ANTI-PROCRASTINATION ALARM CHALLENGE                   │
├────────────────────────────────────────────────────────┤
│ ⚠️ URGENT STUDY ALARM: Linear Algebra Assignment       │
│ Take a photo of your study material to dismiss!        │
│ ┌────────────────────────────────────────────────────┐ │
│ │                                                    │ │
│ │                 [ CAMERA VIEW ]                    │ │
│ │                                                    │ │
│ └────────────────────────────────────────────────────┘ │
│                  [ 📸 CAPTURE PROOF ]                  │
└────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────┐
│ AUDIO ANNOTATION BAR (In Note / PDF Page)              │
├────────────────────────────────────────────────────────┤
│ [ ▶ ] 01:24 / 03:10 ━━━━●────────── [ 🗑 ]  Page 4 Pin  │
└────────────────────────────────────────────────────────┘
```

## Logic Notes
- Widgets use Jetpack Glance (`GlanceAppWidget` & `GlanceAppWidgetReceiver`) to share Compose-like mental model with the rest of the application.
- Audio recorder handles background interruption, phone calls, and headset disconnections cleanly by saving the recorded portion rather than discarding.
- Alarm Receiver acquires a partial WakeLock to ensure audio continues playing until the challenge activity is handled.

## Risks
- *Risk*: `AlarmManager` exact alarms restricted on Android 12+ (API 31+).
  - *Mitigation*: Check `AlarmManager.canScheduleExactAlarms()`; request `SCHEDULE_EXACT_ALARM` permission via system settings if not granted, fallback to high-priority notifications.
- *Risk*: Heavy bitmap rendering in Home Screen widgets causes `TransactionTooLargeException` in `RemoteViews`.
  - *Mitigation*: Strictly downsample widget bitmaps to match exact widget cell density (max 512x512px) before feeding into Glance ImageProvider.

## Dependencies
- Phase 42 (`ScheduleRepository`, `ScheduleCutoffEngine`, `FotaraDbHelper`).
- Android Jetpack Glance library.
- Android platform `MediaRecorder` & `MediaPlayer`.

## Acceptance Criteria
- 4x2 and 4x4 Timetable Widgets display correct classes and dynamically roll over based on `ScheduleCutoffEngine`.
- Single Photo Pin Widget allows picking 1 photo and renders it without OOM / TransactionTooLargeException.
- Photo Carousel Widget displays recent captures with working next/previous actions.
- Audio annotations record up to 5 minutes, persist to disk, and play back smoothly with scrubber.
- Audio annotations optionally link to PDF document pages.
- Anti-procrastination alarm triggers at designated time and silences only upon capturing photo proof.
- Full unit test pass across widget data providers, audio manager, and alarm challenge engine.
- 1.9.0 Beta release artifact packaged cleanly adhering to Arinara release naming.
