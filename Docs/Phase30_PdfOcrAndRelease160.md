<!-- --- Arinara Network (c) 2026 --- -->
<!-- Exclusive property of Arinara Network. -->
<!-- Unauthorized use, reproduction, distribution, or modification of this code, -->
<!-- in whole or in part, for any purpose, is strictly prohibited without prior -->
<!-- written consent from Arinara Network as sole legal owner of this codebase. -->

# Phase 30 - PdfOcrAndRelease160

## Goal
Implement Batch 4 (Final) for Fotara 1.6.0: reliable background PDF page OCR indexing for search without any visual changes or text leakage, built-in offline update banner with channel pills, updated What's New and About & Legal release representations, and the final 1.6.0 Stable release build.

## Scope
- Task 0: Verify earlier batches (Batches 2A, 2B, 3 including Task 4B profile borders) with file evidence.
- Task 1: Background page-by-page PDF OCR for search indexing, NULL vs empty string semantics, failure retries, resumable throttled backfill, incremental FTS indexing, and strict zero-display of OCR text in cards, viewer, menus, and export.
- Task 2: Built-in update banner composable (`UpdateBanner`) with gradient styling from theme tokens, ElmsSans Bold version text, tappable release link, and `ChannelPill` ("Stable" in green/blue, "Beta" in amber). Integrated into `NewUpdateDialog` and `UpdateScreen`. Robust version comparator in `UpdateVersionUtils` (handling Beta vs Stable).
- Task 3: What's New screen populated with authentic 1.6.0 Stable highlights and previous releases (1.5.6, 1.5.7, 1.5.8), Breaking Changes entry for workspaces, unified `ChannelPill`, and dynamic versioning from `PackageManager` / `BuildConfig`.
- Task 4: Final 1.6.0 Stable release: bump `versionName` to `"1.6.0"`, `versionCode` to 26; verify database migrations on test copies; 100% passing unit tests; assemble release APK; report path and size; finalize `Changelog_1.6.md` and `Progress.md`.

## Out Of Scope
- Any change to how PDFs look or behave in the viewer.
- Any display of OCR text or text excerpts for PDF search results.
- Any change to database schemas or FTS table definitions (`DATABASE_VERSION` remains 16).
- Any modifications to `ViewportTransform.kt` or `fotara.fileprovider`.
- Any new third-party dependencies or network calls.

---

## TASK 0 AUDIT & VERIFICATION EVIDENCE
1. **DATABASE_VERSION & Migration**:
   - `FotaraDbHelper.kt:712`: `DATABASE_VERSION = 16`.
   - Lines 646-676: Migration 15 -> 16 creates `workspaces` table (`id`, `name`, `sort_order`, `icon_name`, `color_id`, `created_at`, `updated_at`), seeds HOME (id 1, `sort_order` 0) and ARCHIVE (id 2, `sort_order` 1), and adds `workspace_id INTEGER NOT NULL DEFAULT 1 REFERENCES workspaces(id)` to `folders`.
2. **Workspaces Wiring**:
   - `WorkspaceTabBar.kt` is wired into `HomeScreen.kt:646` and `NotesScreen.kt:241`.
   - Single source of truth `WorkspaceRepository.selectedWorkspaceId` observed by both Home and Notes.
   - Dialogs wired in `WorkspaceDialogs.kt`: `MoveToWorkspaceDialog` (used in `HomeScreen.kt` menu and multi-select mode, and `FolderDetailScreen.kt`), `DeleteWorkspaceDialog` (Move to Home, Move to Trash, Erase permanently), and `RestoreFolderDestinationDialog` (used in `TrashScreen.kt`).
   - Shared folder picker `WorkspaceFolderPicker.kt` groups folders by workspace when multiple workspaces exist.
   - Search scope row in `ActiveSearchBar.kt:349-408` provides filtering across "All" and specific workspaces.
3. **Shared Screen Header**:
   - `ScreenHeader.kt` with `ScreenHeaderDefaults.TaglineHeight = 20.dp` is utilized uniformly in `HomeScreen.kt:509`, `NotesScreen.kt:175`, and `SettingsScreen.kt:427`.
4. **Notes Tab Workspace Filter**:
   - Notes tab contains `WorkspaceTabBar` at line 241; `NotesViewModel.kt` filters notes by `selectedWorkspaceId` and displays `R.string.notes_empty_workspace` when empty.
5. **ProfileAvatar Deployment**:
   - `ProfileAvatar.kt` with normalized inner circle fit, 1.5% overlap seam, and 45° invariant edit button is used in `SettingsScreen.kt:439` (72.dp), `ProfileScreen.kt:293` (96.dp), and `BorderPickerDialog.kt:104` (84.dp).
6. **Current Versions in `app/build.gradle.kts`**:
   - `versionCode = 25`, `versionName = "1.5.11 Beta"`. Target for Task 4 is `versionCode = 26`, `versionName = "1.6.0"`.
7. **Git History for 1.5.7 and 1.5.8 Work**:
   - Commit `3a71749`: LinkIt cluster glow, profile image loading fix, fullscreen crop editor for 1.5.8 Beta.
   - Commit `ccd9aad`: Bulletproof in-memory normalized PNG crop and safe universal URI streaming for 1.5.10 Beta.
   - Commit `ba8ba05`: WhatsNewScreen and version configurations for 1.5.10 Beta.
   - Commit `d288b50`: Resilient multi-tier image decoder and async photo picking for 1.5.11 Beta.
   - Phases 23 (1.5.7) and 24 (1.5.8) are verified as completed in `Docs/Progress.md`.

---

## TASK 1 AUDIT & SPECIFICATION (PDF CONTENT OCR FOR SEARCH)
### Audit Findings
- **Pipeline Today**: `importPdf` renders pages to bitmaps, saves page images into app storage via `photoStorageManager.saveDocumentPageBitmap`, inserts rows into `document_pages` with `ocr_text = NULL`. Then in a background coroutine, loops through pages and calls `ocrEngine.extractText(pageFile)`.
- **NULL vs Empty String Today**: Previously, if `ocrResult.fullText.isNotBlank()` was true, it updated `document_pages.ocr_text`; but if the page had no text (or blank text), it did not write anything, leaving `ocr_text` as `NULL`. This conflated "not yet processed" with "processed, no text found".
- **Auto OCR Setting**: The setting `key_auto_ocr` exists in `fotara_settings` (read by `SettingsRepository`), but was never checked during document import or backfill.
- **Interrupted / Failed Runs**: Any interrupted run left uncompleted pages with `ocr_text = NULL` with no resume mechanism.
- **Search Snippets**: In `ActiveSearchBar.kt:1863-1901`, `SearchDocumentResultCard` previously rendered an OCR badge and snippet whenever `document.extractedText` was not blank. This violated the strict rule that PDF OCR text must never be displayed in search result cards.
- **Rebuild Search Index**: `PhotoRepository.rebuildSearchIndex()` previously only deleted and re-indexed `photos_fts`; `document_notes_fts` was omitted from search index rebuilding.
- **DOCX Text**: DOCX text is extracted and stored in `document_notes.extracted_text`, and indexed in `document_notes_fts` with `doc_type = "DOCX"`.
- **Battery Safety Check**: The codebase does not track device battery saving mode; per instruction, this check is documented as skipped.

### Implementation Specification
1. **Semantics Enforcement**:
   - `NULL` = Not yet processed.
   - `""` (Empty string) = Processed, no text detected.
   - Once a page is processed, it is never left as `NULL`.
   - If an exception occurs during OCR for a page, it remains `NULL` and an in-memory retry counter tracks attempts (max 3 retries per session).
2. **Incremental FTS Updates**:
   - As each page is OCRed, `document_pages.ocr_text` is updated immediately.
   - `document_notes.extracted_text` and `document_notes_fts` are updated incrementally (e.g. after page 0, every 3 pages, and upon completion of the document).
3. **Resumable Backfill Routine**:
   - `backfillPdfOcr()` finds active (non-trashed) PDF notes having pages with `ocr_text IS NULL`.
   - Processes one PDF at a time, throttled with delays and `currentCoroutineContext().ensureActive()`.
   - State lives strictly in SQLite (`ocr_text IS NULL`), making it fully resumable if killed.
   - Skips when `isAutoOcrEnabled()` is false.
   - Triggered on app launch (after short delay) and when Auto OCR setting is toggled on.
4. **Rebuild Search Index**:
   - `DocumentRepository.rebuildSearchIndex()` atomically wipes and rebuilds `document_notes_fts` in a transaction for all active documents.
   - `SettingsRepository.rebuildSearchIndex()` combines photo and document index counts.
5. **Zero OCR Text Display**:
   - In `ActiveSearchBar.kt`, `SearchDocumentResultCard` is updated so snippet and badge display are strictly restricted to `DocumentType.DOCX`. For PDF notes, no excerpt, badge, or OCR text is rendered.
6. **Privacy & Trash**:
   - Trashing a document removes it from `document_notes_fts`. Restoring a document re-indexes its text into `document_notes_fts`.

---

## TASK 2 AUDIT & SPECIFICATION (UPDATE BANNER & CHANNEL PILLS)
### Audit Findings
- `NewUpdateDialog.kt` previously loaded a remote banner image via Coil's `AsyncImage` with an overlaid title.
- `UpdateScreen.kt` also loaded a remote/fallback banner image via `AsyncImage`.
- `UpdateVersionUtils.kt` stripped tags to numeric components and did not differentiate between a numeric equal Stable vs Beta release (e.g. 1.6.0 vs 1.6.0 Beta).

### Implementation Specification
1. **Built-in `UpdateBanner` Composable**:
   - Built entirely with Jetpack Compose using existing theme tokens: subtle linear gradient (`Color(0xFF131D38)` to `Color(0xFF0C1326)`), border `BorderStroke(1.dp, MidnightCardOutline)`.
   - Left side: Large bold version text in `ElmsSans`, tappable release link below it displaying clean readable format (`github.com/Arinaranetwork/Fotara/releases/tag/v...`), opening via `UriHandler` safely.
   - Right side: `ChannelPill` ("Stable" with `TagEmerald` or "Beta" with `TagAmber`).
   - Clean top-right close 'X' button without colliding with the channel pill.
   - Reusable across `NewUpdateDialog` (with rounded top corners) and `UpdateScreen`.
2. **Channel Resolution & Version Comparison**:
   - `UpdateVersionUtils.resolveChannel(version, isPrerelease)` determines channel (`Channel.BETA` if prerelease or contains "Beta", else `Channel.STABLE`).
   - `isNewerVersion(remote, current)` accurately compares version numbers, ensuring 1.6.0 > 1.5.11 Beta, and a Stable build is newer than a Beta of the same numeric version (1.6.0 > 1.6.0 Beta).

---

## TASK 3 AUDIT & SPECIFICATION (WHAT'S NEW & RELEASE INFORMATION)
1. **`WhatsNewScreen`**:
   - Populated with authentic 1.6.0 Stable highlights: Workspaces, Workspace Search Scope, Text Editor Repairs, Canvas Vector & Eraser, Profile Borders & Banner, Searchable PDF OCR, UI Consistency Pass.
   - Breaking Changes entry: "Favorit and Arsip tabs were replaced by workspaces. Existing folders were moved to Home."
   - Includes historical entries for 1.5.8 Beta, 1.5.7 Beta, and 1.5.6 Beta.
   - 1.6.0 Stable expanded by default; previous versions collapsible.
   - Uses shared `ChannelPill` composable.
2. **`SettingsScreen` About & Legal**:
   - Displays "Fotara 1.6.0" with `ChannelPill("Stable")`, removing hardcoded "Beta" suffix logic.
   - Version resolved via `PackageManager` / `BuildConfig`.

---

## TASK 4 RELEASE CHECKLIST (FOTARA 1.6.0 STABLE)
1. Set `versionName = "1.6.0"`, `versionCode = 26` in `app/build.gradle.kts`.
2. Verify all references to "Beta" are removed from version presentation.
3. Append Batch 4 to `Changelog/Changelog_1.6.md`, finalize header to `Released: 2026-10-04 Status: Released`.
4. Ensure zero TODO/FIXME in `app/src/main`.
5. Run full test suite (`.\gradlew.bat testDebugUnitTest`) and ensure 100% pass rate.
6. Assemble release APK (`.\gradlew.bat assembleRelease`), verify `versionName` and `versionCode` using `aapt`/`apkanalyzer`, place in `Output/Release/Fotara_1.6.0.apk`.
7. Prepare git commands for the owner.

## Acceptance Criteria
- [ ] Database version remains 16 with existing migrations verified.
- [ ] PDF OCR runs page-by-page off main thread, writes `""` for empty text, keeps `NULL` on failure with retry limit.
- [ ] Resumable throttled backfill runs when Auto OCR is enabled.
- [ ] Incremental FTS updates allow search to find PDF content while processing.
- [ ] PDF search results do NOT display OCR text snippets or badges.
- [ ] Built-in `UpdateBanner` works offline with gradient styling, clean URL, and `ChannelPill`.
- [ ] `UpdateVersionUtils` correctly compares Stable vs Beta versions (1.6.0 > 1.5.11 Beta, 1.6.0 > 1.6.0 Beta).
- [ ] What's New screen displays 1.6.0 Stable with verified changelog entries and Breaking Changes notice.
- [ ] About & Legal displays "Fotara 1.6.0" with Stable pill.
- [ ] Release APK built successfully with `versionName = "1.6.0"` and `versionCode = 26`.
