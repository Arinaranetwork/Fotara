<!-- --- Arinara Network (c) 2026 --- -->
<!-- Exclusive property of Arinara Network. -->
<!-- Unauthorized use, reproduction, distribution, or modification of this code, -->
<!-- in whole or in part, for any purpose, is strictly prohibited without prior -->
<!-- written consent from Arinara Network as sole legal owner of this codebase. -->

# Share to Fotara Verification Checklist (C11)

This document specifies the end-to-end device testing steps to execute on physical Android devices and emulators during the final validation chunk (C11) for Chunk C7 (Share to Fotara).

---

## 1. Prerequisites
- Target device running Android 7.0 (API 24) to Android 15 (API 35).
- Fotara 1.5.0 installed with Room database upgraded to v13.
- Test assets available in device downloads:
  - 1 plain image (JPEG/PNG/WEBP).
  - 1 PDF document (normal, readable).
  - 1 password-protected PDF document.
  - 1 large PDF document (> 25 MB).
  - 1 DOCX document.
  - 1 Markdown file (`.md`).
  - 1 plain text file (`.txt`).
  - 1 corrupt or zero-byte file.
  - Batch of 35 mixed images/documents.
  - At least one regular coursework folder and one privacy-locked folder with PIN enabled.
  - At least one Photo Group inside a coursework folder.

---

## 2. Manual Device Test Matrix (C11)

| Test ID | Scenario | Procedure | Expected Verification | Pass / Fail |
| :--- | :--- | :--- | :--- | :--- |
| **SHR-01** | Single Item Share (No Side Panel) | From Google Photos or Files app, share 1 image to "Save to Fotara". | Share receiver opens. Top-left displays "X" button. No right-side collapsed tab or panel is rendered. Bottom button shows "Place Here (1)". Selecting a folder and tapping "Place Here (1)" creates a photo note and closes the activity, returning to the source app. | [ ] |
| **SHR-02** | Exactly 30 Items Share | In Files app, select exactly 30 images and share to Fotara. | All 30 items stage successfully via streaming. No warning banner for skipped items appears. Docked tab on right edge displays "30". Bottom button displays "Place Here (30)". | [ ] |
| **SHR-03** | 31+ Items Share Cap | In Files app, select 35 mixed files and share to Fotara. | Exactly the first 30 items are staged. A prominent English warning banner displays at the top: "Maximum 30 items allowed per share. 5 extra items were skipped." Bottom button displays "Place Here (30)". | [ ] |
| **SHR-04** | Mixed Formats Staging | Share a bundle containing 2 images, 1 PDF, 1 DOCX, 1 Markdown file, and 1 plain text file (6 items total). | Docked right-side tab shows "6". Expanding panel shows each item with appropriate preview (image thumbnail, PDF first page, DOCX icon, text snippet) and correct type badges (Image, PDF, DOCX, Markdown, Text). | [ ] |
| **SHR-05** | Large PDF Streaming Import | From Files app, share a 30 MB multi-page PDF to Fotara. | File is copied into staging directory using 8KB streaming buffer without OutOfMemoryError. Non-blocking asynchronous first-page thumbnail generation completes. Memory profile remains stable. | [ ] |
| **SHR-06** | Text Note with Subject | Share text from an email app or custom share intent where `EXTRA_SUBJECT = "Lecture 4 Notes"` and `EXTRA_TEXT = "Key points on algorithms..."`. | Staged item displays as Text Note with title "Lecture 4 Notes". Once placed, a native text note is created in the destination folder with that exact title and text content. | [ ] |
| **SHR-07** | Text Note without Subject | Share plain text from a web browser or chat app without subject (`EXTRA_TEXT = "Chapter summary: quick sort runtime is O(N log N)"`). | Staged item title automatically falls back to the first non-empty line of the text body ("Chapter summary: quick sort runtime is..."). Placed note uses this title. | [ ] |
| **SHR-08** | Corrupt & Password-Protected Files | Share a batch containing 1 valid image, 1 zero-byte file, and 1 password-protected PDF. | Valid image is selected and placeable. Corrupt file shows error badge "Unsupported or corrupt file format". Password-protected PDF shows error badge "Password protected PDF". Both erroneous items are excluded from placement count (N=1). Tapping trash icon on error items removes them cleanly from session. | [ ] |
| **SHR-09** | Placement into Privacy-Locked Folder | In destination browser, tap a folder protected by PIN. | `FolderUnlockDialog` is presented. Folder is not selected until valid PIN is entered. Upon successful authentication, folder unlocks and becomes the active destination. | [ ] |
| **SHR-10** | Placement into Photo Group Destination | Select a Photo Group as the destination when session contains 2 images and 1 PDF document. | PDF item displays inline notice: "Photo Groups can only contain images" and is disabled/unchecked. Only the 2 images are eligible for placement. Bottom button updates to "Place Here (2)". Placed images attach directly to the Photo Group. | [ ] |
| **SHR-11** | Partial Placement Workflow | With 5 items staged, open right panel and uncheck 2 items. Navigate to "Physics" folder and tap "Place Here (3)". | 3 selected items are saved into "Physics". Screen does NOT close. Placed items disappear from list. Right-side tab updates to "2". User navigates to "Chemistry" folder and places the remaining 2 items. Screen then automatically finishes upon placing all items. | [ ] |
| **SHR-12** | Screen Rotation Persistence | With 4 items staged and 1 item unchecked, rotate the device from portrait to landscape and back. | Session state, staged file references, selection state, and current destination are completely preserved without re-copying files or crashing. | [ ] |
| **SHR-13** | Process Death Mid-Session | With items staged, background the app, simulate low memory kill (`adb shell am kill com.arinara.fotara`), and bring activity back to foreground. | Session restores staged items from persisted session state or gracefully handles recovery without dangling locks or data corruption. | [ ] |
| **SHR-14** | Incremental Share via onNewIntent | While placement screen is open with 3 items, switch to another app and share 2 additional items to Fotara. | `onNewIntent` is received by `ShareReceiverActivity`. The 2 new items are streamed into staging and appended to the existing session. Total count updates to 5 items without losing current destination or previous selections. | [ ] |
| **SHR-15** | Cancellation Confirmation & Cleanup | Share 3 items. Place 1 item into a folder. Tap top-left "X" button while 2 unplaced items remain. | Confirmation dialog asks: "Discard remaining unplaced items? (1 item already placed will be kept)". Confirming exits the screen and cleans up temporary staging files in `cacheDir/share_staging/`. Previously placed item remains safe in database. | [ ] |
| **SHR-16** | Offline Execution Guarantee | Put device in Airplane Mode (disable Wi-Fi and Cellular). Execute single and multi-item shares. | Staging, thumbnail generation, PDF rendering, DOCX parsing, and Room database persistence execute 100% offline without network calls or failures. | [ ] |
