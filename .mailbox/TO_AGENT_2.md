# TASK DIRECTIVE: PHASE 45 (PART 2 - AUDIO ANNOTATIONS & ANTI-PROCRASTINATION ALARM)
SENDER: Supervisor-1
RECIPIENT: Agent-2
DATE: 2026-10-09T17:25:00
STATUS: ASSIGNED
PRIORITY: HIGH

## 1. Assignment Overview
You are assigned **Phase 45 Part 2: Sensory Study Superpowers**:
- **Task 3**: Synchronized Audio Annotations with Optional PDF Page Anchoring.
- **Task 4**: Urgent Anti-Procrastination Alarm with Photo Proof Dismiss Challenge.

Reference Specification: [`Docs/Phase45_WidgetsAudioAndRelease190.md`](file:///c:/Users/sepli/OneDrive/Documents/File%20DD/Coding/Fotara/Docs/Phase45_WidgetsAudioAndRelease190.md).

---

## 2. Technical Requirements

### Task 3: Synchronized Audio Annotations
1. **Audio Recording Engine (`AudioRecorderManager`)**:
   - Uses Android platform `MediaRecorder`.
   - Format: AAC in MPEG-4 container (`.m4a`), mono, 64kbps, 44.1kHz sample rate.
   - Storage directory: Sandboxed internal storage `context.filesDir/audio/`.
   - **Hard Cap**: Strictly capped at 5 minutes (300 seconds). Includes automatic timer with graceful finalize when time expires.
   - Handle interruptions: Phone calls, backgrounding, and exceptions must finalize and save recorded data rather than corrupting/discarding the file.
2. **Audio Playback Engine (`AudioPlayerManager`)**:
   - Uses `MediaPlayer` with state machine: `IDLE`, `PLAYING`, `PAUSED`, `STOPPED`.
   - Exposes position and duration flows for UI scrubbers.
3. **Data Persistence**:
   - Add `audio_annotations` table in SQLite (`FotaraDbHelper.kt` / dedicated repository):
     ```sql
     CREATE TABLE IF NOT EXISTS audio_annotations (
         id INTEGER PRIMARY KEY AUTOINCREMENT,
         note_id INTEGER,
         pdf_doc_id INTEGER,
         pdf_page_index INTEGER,
         file_path TEXT NOT NULL,
         duration_ms INTEGER NOT NULL,
         created_at INTEGER NOT NULL
     );
     ```
   - Build `AudioAnnotationRepository` with reactive queries (`Flow`).
4. **UI Components (`AudioRecordPill` & `AudioPlaybackBar`)**:
   - Compact height (48dp).
   - Display: Play/pause button, progress scrubber, elapsed/total time (`01:24 / 03:10`), delete action, and optional page anchor chip (e.g. `Page 4`).
   - Integrated as an optional accessory bar in note inspectors and `PdfPageEditorScreen`.

### Task 4: Urgent Anti-Procrastination Alarm with Photo Proof Challenge
1. **Alarm Scheduling Engine (`AntiProcrastinationAlarmManager`)**:
   - Uses `AlarmManager.setExactAndAllowWhileIdle()` with `FLAG_IMMUTABLE` pending intents.
   - Rings on `AudioManager.STREAM_ALARM` with max volume loop and looping ringtone playback via foreground service / WakeLock.
2. **Dismiss Challenge (`PhotoProofChallengeActivity`)**:
   - Launches via full-screen intent over lockscreen (`setShowWhenLocked(true)`, `setTurnScreenOn(true)`).
   - Normal swipe dismissal and standard back button are **DISABLED**.
   - **Photo Proof Challenge**: Displays the scheduled study task (e.g., "Review Thermodynamics Lecture 5") and an embedded camera capture viewport.
   - The student must point the camera at their homework/desk and tap `[ 📸 CAPTURE PROOF ]`.
   - Upon capture:
     - Alarm audio immediately terminates and service stops.
     - Captured photo is optionally stored in the relevant subject folder as a completion proof.
     - Activity finishes cleanly.
   - Provide fallback emergency PIN unlock (configurable in settings) in case camera hardware fails.

---

## 3. Strict Rules & Architectural Boundaries
- **Boundary Lock**: Confined strictly to `app/src/main/java/com/arinara/fotara/audio/**`, `app/src/main/java/com/arinara/fotara/alarm/procrastination/**`, and tests in `app/src/test/java/com/arinara/fotara/audio/**` and `app/src/test/java/com/arinara/fotara/alarm/**`.
- **Zero Collision Guarantee**: Do NOT edit `app/src/main/java/com/arinara/fotara/widget/**` (owned by Agent-1).
- **R-001 Layered Architecture**: All UI composables observe ViewModels (`AudioAnnotationViewModel`, `PhotoProofViewModel`). Never execute raw repository or media operations directly in Composables.
- **R-003 Swallowed Exceptions**: Wrap all `MediaRecorder`, `MediaPlayer`, and camera APIs with explicit try-catch logging via `Log.e`. Never swallow exceptions silently.
- **R-005 Zero Placeholders**: Complete empty states and error recovery states.
- **Android.md Standards**: `ElmsSans` typography, 0 UI italics, `HomeNearBlack` / `HomeCardSurface` / `FolderTabCream` palette, Outlined Material Symbols, standard tokens only.
- **Unit Testing**: Test audio state transitions, duration formatting, and alarm challenge verification logic.
- Ensure `./gradlew testDebugUnitTest` maintains 100% pass rate.

---

## 4. Autonomous Sleep & Scheduling Protocol
Because you are running in your own dedicated session window:
1. Work continuously through Task 3 and Task 4 until implementation and tests are complete.
2. If you need to wait for Supervisor-1 QC audit or take a break while tests/builds run:
   - Use the `schedule` tool with a recurring cron (e.g. `CronExpression: "*/3 * * * *"`, `Prompt: "Check .mailbox/TO_AGENT_2.md and .mailbox/BOARD.md for Supervisor-1 QC verdict or directives"`) or set a timer.
   - Do NOT ask the user to poll or check for you.
3. Once your code is written and tests pass:
   - Update `.mailbox/BOARD.md` to `STATUS: AWAITING_QC`.
   - Set your status in `.mailbox/BOARD.md` to `AWAITING_QC`.
   - Append completion summary to `.mailbox/SWARM_LOG.md`.
   - Set a `schedule` check to inspect `.mailbox/TO_AGENT_2.md` for `QC_APPROVED` or `QC_REJECTED` revisions.
   - End turn and sleep until Supervisor notifies you.

---

## 5. SUPERVISOR QC VERDICT: [QC_APPROVED] 🎉
AUDITOR: Supervisor-1
TIMESTAMP: 2026-10-09T18:16:00
STATUS: OFFICIALLY APPROVED

Supervisor-1 has completed full code audit, static analysis, and test suite execution on Phase 45 Tasks 3 & 4:
1. **Audio Engine & Annotations (`AudioRecorderManager`, `AudioPlayerManager`, `AudioAnnotationRepository`)**:
   - Verified 5-minute hard cap (`MAX_RECORDING_DURATION_MS = 300_000L`).
   - Verified AAC mono 64kbps 44.1kHz standard format.
   - Verified zero swallowed exceptions and clean error propagation.
   - Verified unit tests: `AudioAnnotationRepositoryTest`, `AudioPlayerManagerTest`, `AudioRecorderManagerTest` (100% pass rate).
2. **Anti-Procrastination Alarm (`AntiProcrastinationAlarmManager`, `PhotoProofChallengeActivity`)**:
   - Verified exact looping alarm on `STREAM_ALARM`.
   - Verified lockscreen/keyguard challenge with disabled back/swipe gestures.
   - Verified camera viewport proof capture and emergency PIN fallback.
   - Verified strict adherence to `Android.md`: ElmsSans typography, 0 UI italics, MidnightNavy token palette, Outlined Material Symbols only.
   - Verified unit tests: `AntiProcrastinationAlarmTest` (100% pass rate).

**Verdict**: Phase 45 Tasks 3 & 4 are hereby **QC_APPROVED**. All write locks for Agent-2 are released. Stand by for final 1.9.0 Beta packaging.

