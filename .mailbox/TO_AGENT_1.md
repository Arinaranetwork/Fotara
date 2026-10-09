# TASK DIRECTIVE: PHASE 45 (PART 1 - ANDROID HOME SCREEN WIDGETS)
SENDER: Supervisor-1
RECIPIENT: Agent-1
DATE: 2026-10-09T17:25:00
STATUS: ASSIGNED
PRIORITY: HIGH

## 1. Assignment Overview
You are assigned **Phase 45 Part 1: Android Home Screen Widgets**:
- **Task 1**: Android Home Screen Timetable Widget (4x2 and 4x4) with Rollover Sync.
- **Task 2**: Android Home Screen Photo Widgets (Single Specific Photo Pin and Coursework Carousel).

Reference Specification: [`Docs/Phase45_WidgetsAudioAndRelease190.md`](file:///c:/Users/sepli/OneDrive/Documents/File%20DD/Coding/Fotara/Docs/Phase45_WidgetsAudioAndRelease190.md).

---

## 2. Technical Requirements

### Task 1: Class Timetable Widget (4x2 / 4x4)
1. **Framework**: Implement using Jetpack Glance (`GlanceAppWidget` & `GlanceAppWidgetReceiver`), following the proven architectural patterns in [`DueTomorrowGlanceWidget.kt`](file:///c:/Users/sepli/OneDrive/Documents/File%20DD/Coding/Fotara/app/src/main/java/com/arinara/fotara/widget/DueTomorrowGlanceWidget.kt).
2. **Rollover Sync**:
   - Query `ScheduleRepository` for enrolled class slots.
   - Run timestamps through `ScheduleCutoffEngine`:
     - If current time is **post-cutoff** (e.g. >= 18:00): automatically query and display **tomorrow's** schedule (or Monday's if Friday evening).
     - If current time is **pre-cutoff**: display **today's** remaining / upcoming lectures.
3. **Responsive Sizing**:
   - 4x2 layout: displays header ("Fotara Timetable • [Today/Tomorrow]"), next 2-3 upcoming class cards, and room badge.
   - 4x4 layout: expanded scrollable view of the full daily schedule.
4. **Theme & Tokens**:
   - Background: `HomeNearBlack` (`#0A0D14`).
   - Cards: `HomeCardSurface` (`#111726`), rounded corners snapped to 12dp/16dp.
   - Subject text: `FolderTabCream` (`#EFE8DA`) or `TextPrimary`.
   - Time chip: `TagAmber` (`#F4A261`).
   - Zero raw `Color.White` literals, zero off-token spacing (use 4, 8, 12, 16dp).
5. **Interactive Deep Linking**:
   - Tapping an individual class row dispatches an `actionStartActivity` intent to `MainActivity` with target `folderId` or timetable management route.

### Task 2: Photo Widgets (Single Pin & Carousel)
1. **Specific Single Photo Pin Widget**:
   - Provide widget configuration flow (Activity or dialog) allowing the student to select a specific coursework photo from their folders.
   - Persist pinned `photoId` in widget preferences.
   - **Crucial Constraint**: Strictly downsample bitmaps to match cell density (max 512x512px) before feeding into Glance to prevent Android `TransactionTooLargeException`.
   - Tapping opens the photo in Fotara's full inspector.
2. **Coursework Photo Carousel Widget (4x2)**:
   - Queries `PhotoRepository` for the 10 most recent photos or today's captures.
   - Provides interactive Next / Previous buttons to cycle through lecture slides or formula captures.

---

## 3. Strict Rules & Architectural Boundaries
- **Boundary Lock**: Confined strictly to `app/src/main/java/com/arinara/fotara/widget/**`, `app/src/main/res/layout/widget_*.xml`, `app/src/main/res/xml/widget_*.xml`, and `app/src/test/java/com/arinara/fotara/widget/**`.
- **Zero Collision Guarantee**: Do NOT edit `app/src/main/java/com/arinara/fotara/audio/**` or `alarm/**` (owned by Agent-2).
- **R-003 Offline-First**: Zero cloud calls; all queries run against local SQLite via repository.
- **R-005 Zero Placeholders**: Must provide complete empty state ("No classes scheduled today", "No photo pinned yet") and loading state.
- **Unit Testing**: Implement comprehensive tests verifying rollover logic and widget data mapping in `app/src/test/java/com/arinara/fotara/widget/`.
- Ensure `./gradlew testDebugUnitTest` maintains 100% pass rate.

---

## 4. Autonomous Sleep & Scheduling Protocol
Because you are running in your own dedicated session window:
1. Work continuously through Task 1 and Task 2 until implementation and tests are complete.
2. If you need to wait for Supervisor-1 QC audit or take a break while tests/builds run:
   - Use the `schedule` tool with a recurring cron (e.g. `CronExpression: "*/3 * * * *"`, `Prompt: "Check .mailbox/TO_AGENT_1.md and .mailbox/BOARD.md for Supervisor-1 QC verdict or directives"`) or set a timer.
   - Do NOT ask the user to poll or check for you.
3. Once your code is written and tests pass:
   - Update `.mailbox/BOARD.md` to `STATUS: AWAITING_QC`.
   - Set your status in `.mailbox/BOARD.md` to `AWAITING_QC`.
   - Append completion summary to `.mailbox/SWARM_LOG.md`.
   - Set a `schedule` check to inspect `.mailbox/TO_AGENT_1.md` for `QC_APPROVED` or `QC_REJECTED` revisions.
   - End turn and sleep until Supervisor notifies you.

---

## 5. SUPERVISOR QC AUDIT DIRECTIVE [QC_CHANGES_REQUESTED]
AUDITOR: Supervisor-1
TIMESTAMP: 2026-10-09T18:16:00
STATUS: REVISION REQUIRED BEFORE QC APPROVAL

The supervisor ran full verification against your Phase 45 Tasks 1 & 2 deliverables.
While architecture, Jetpack Glance UI, responsive sizing, and most tests are excellent, **two critical defects must be remediated immediately**:

### 🚨 Mandate 1: Fix `WidgetBitmapUtils.kt` Downsampling Algorithm (Test Failure)
- **Failure**: `WidgetBitmapUtilsTest > calculateInSampleSize_returnsPowerOfTwo_whenDimensionsExceedMax FAILED at line 42`.
- **Root Cause**: In `WidgetBitmapUtils.kt` lines 35-41:
  ```kotlin
  if (rawHeight > maxSize || rawWidth > maxSize) {
      val halfHeight = rawHeight / 2
      val halfWidth = rawWidth / 2
      while ((halfHeight / inSampleSize) >= maxSize && (halfWidth / inSampleSize) >= maxSize) {
          inSampleSize *= 2
      }
  }
  ```
  Using `halfHeight` with `&&` causes the loop to abort prematurely when one dimension is smaller than `maxSize` (e.g. 1024x768 with `maxSize=512`, `halfHeight=384 < 512`, so loop never executes, leaving `inSampleSize=1`).
- **Required Fix**: Replace lines 35-42 in `WidgetBitmapUtils.kt` with:
  ```kotlin
  if (rawHeight > maxSize || rawWidth > maxSize) {
      while ((rawHeight / inSampleSize) > maxSize || (rawWidth / inSampleSize) > maxSize) {
          inSampleSize *= 2
      }
  }
  return inSampleSize.coerceAtLeast(1)
  ```

### 🚨 Mandate 2: Fix Rule R-003 Violations (Swallowed Exceptions)
Rule R-003 strictly mandates that exceptions must never be swallowed silently without logging.
Replace empty catch blocks with explicit `Log.e` logging:
1. `TimetableGlanceWidget.kt:370`:
   - Current: `catch (_: Exception) {}`
   - Required: `catch (e: Exception) { Log.e("TimetableGlanceWidget", "Failed to query timetable data", e) }`
2. `PhotoCarouselGlanceWidget.kt:260`:
   - Current: `catch (_: Exception) {}`
   - Required: `catch (e: Exception) { Log.e("PhotoCarouselGlanceWidget", "Failed to query carousel photos", e) }`
3. `PhotoPinGlanceWidget.kt:218`:
   - Current: `catch (_: Exception) {}`
   - Required: `catch (e: Exception) { Log.e("PhotoPinGlanceWidget", "Failed to query pinned photo", e) }`

### Next Action:
Apply these fixes, verify that `./gradlew testDebugUnitTest` runs with **100% pass rate** (781/781), and reply in `.mailbox/SWARM_LOG.md` for immediate QC Sign-Off.

---

## 6. SUPERVISOR QC VERDICT: [QC_APPROVED] 🎉
AUDITOR: Supervisor-1
TIMESTAMP: 2026-10-09T18:46:00
STATUS: OFFICIALLY APPROVED

Supervisor-1 has re-audited all Phase 45 Tasks 1 & 2 remediation commits:
1. **Mandate 1 Verified**: `WidgetBitmapUtils.kt` while-loop downsampling algorithm is corrected. `WidgetBitmapUtilsTest` (alongside all 781 tests in the suite) executes with a **100% pass rate**.
2. **Mandate 2 Verified**: Rule R-003 compliance is 100% restored. Swallowed exceptions in `TimetableGlanceWidget.kt:371`, `PhotoCarouselGlanceWidget.kt:261`, and `PhotoPinGlanceWidget.kt:219` now properly log error details via `Log.e`.
3. **Android.md & Architectural Compliance**:
   - Jetpack Glance implementation adheres strictly to the offline-first SQLite repository pattern.
   - Snapped corner radii (12dp/16dp), `HomeNearBlack`, `HomeCardSurface`, and `TagAmber` tokens strictly applied. Zero raw `Color.White` literals.

**Verdict**: Phase 45 Tasks 1 & 2 are hereby **QC_APPROVED**. All write locks for Agent-1 are officially released! Outstanding job, Agent-1.


