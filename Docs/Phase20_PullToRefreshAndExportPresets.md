# Phase 20 - PullToRefreshAndExportPresets

## Goal
Deliver Batch 3 (Final) of Fotara release 1.5.6: add pull-to-refresh with an animated curved-arrow vector indicator across Home, Notes, and FolderDetailScreen; rebuild the "Add to Group" dialog grouped by folder with current folder first and move notes to the destination group's folder/subfolder; provide an optional custom file name in the "Share As" dialog with keyboard safety; introduce a combined file name preset setting with token chips in Settings; and finalize release 1.5.6 with zero warnings, full test suite passes, and a validated release build.

## Scope
- **Task 1: Pull to Refresh with Animated Curved-Arrow Indicator**:
  - Gated pull-to-refresh on Home (folder grid), Notes (activity timeline), and FolderDetailScreen (note grid).
  - Open circular arc with arrowhead vector indicator (Material `Icons.Default.Refresh`). Absolutely zero emojis or text glyphs.
  - Pull progress drives slide-in translation, scale (0 to 1), alpha (0 to 1), and rotation (full 360° turn at threshold) inside `graphicsLayer` without per-frame recompositions.
  - Release past threshold triggers continuous spin at resting position, guarantees at least 700ms visibility, and times out after 10 seconds. Release before threshold animates back and cancels.
  - Positioned above top edge fade, using card surface and accent colors from existing design tokens.
  - Gating: scroll position at 0, disabled during multi-select, active search, open overlays, and empty Home tabs.
  - Triggers data reload across repositories (`refresh()`) and retries missing assets. Documented that no separate photo OCR/thumbnail background scanner exists.
- **Task 2: "Add to Group" Dialog Grouped by Folder**:
  - Reorganize group picker: Current folder groups first under "This folder: <Name>" header; subsequent folders in Home order with folder-name headers. Folders without groups excluded.
  - Muted caption under title when other-folder sections exist: "Groups in other folders move your notes to that folder."
  - Max-height scrollable container keeping title and Cancel button anchored.
  - Atomic move: adding photos to a group updates `group_id`, `folder_id`, and `subfolder_id` atomically, reusing folder move pathways to keep FTS, scheduling, and Notes timeline synchronized. `added_at` remains unchanged.
- **Task 3: Optional Custom File Name in "Share As" Dialog**:
  - Optional single-line text field between options and Cancel: label "File name (optional)", placeholder showing evaluated preset (base name without extension), and muted helper line: "Applies to PDF and Word. Original Files keep their own names."
  - Sanitization: trim, collapse whitespace, strip illegal filename characters (`\ / : * ? " < > |`), remove trailing dots/spaces, limit to 80 chars, reject pure-dot names, and prevent duplicate `.pdf`/`.docx` extensions.
  - Fallback to preset if input is empty or invalid. Suffix deduplication for existing cache files.
  - Keyboard-safe layout with `imePadding` and scrollable body.
- **Task 4: File Name Preset Setting for Combined Files**:
  - Add "Combined file name" row in Settings under General category.
  - Preset editing dialog with text field, token chips (`{folder}`, `{date}`, `{time}`, `{count}`), live preview, validation, Save, Cancel, and "Reset to default".
  - Default preset: `{folder}_{date}` (closest equivalent to previous timestamped naming).
  - Persisted in SharedPreferences `fotara_settings` under `key_combine_name_preset` through `SettingsRepository`.
- **Task 5: Finalize and Build Release 1.5.6**:
  - Verify `versionName "1.5.6 Beta"` and `versionCode 20`.
  - Update `Changelog_1.5.md` with Batch 3 and finalized status, and mark Phase 20 complete in `Progress.md`.
  - Release hygiene: zero TODO/FIXME in main source, remove unused dead code from earlier iterations.
  - Execute full unit test suite (100% pass) and assemble release APK (`Fotara_1.5.6_Beta.apk`).

## Out Of Scope
- Group support for PDF and DOCX.
- Workspaces and photo drawing (post-1.5.6 features).
- Database schema changes (DATABASE_VERSION remains 14).
- Changes to `ViewportTransform.kt`, FTS table definitions, or `fotara.fileprovider`.

## Features
### Pull to Refresh
- Reusable `PullToRefreshLayout` wrapping lazy lists with `NestedScrollConnection`.
- Zero recomposition overhead: translation, rotation, scale, and alpha computed exclusively in `graphicsLayer`.
- Minimum 700ms visibility to avoid visual flicker on fast local reloads.

### Folder-Grouped Add to Group Dialog
- Multi-section list separating current folder from remote folders.
- Transparent note migration to target group's folder and subfolder.

### Custom Combined File Name & Settings Preset
- Live placeholder resolution from active preset.
- Robust sanitization preventing corrupt or invalid filesystem entries.
- Settings dialog with interactive token insertion chips.

## UI Mockup
```
================ Add to Group Dialog ================
[ Add to Group                                      ]
  "Groups in other folders move your notes to that folder."
----------------------------------------------------
  THIS FOLDER: Biology                              <- Small muted header
  [ (Layers) Chapter 1 Notes                       ]
  [ (Layers) Lab Photos                            ]

  Chemistry                                         <- Other folder header
  [ (Layers) Organic Reactions                     ]
----------------------------------------------------
                                            [ Cancel ]
====================================================

================ Share As Dialog ===================
[ Share As                                          ]
  5 items selected • ~8 total pages
----------------------------------------------------
  [ (Image)  Original Files                        ]
  [ (Pdf)    Combined PDF Document                 ]
  [ (Docx)   Word Document (.docx)                 ]

  File name (optional)
  [ Biology_2026-10-04                            ] <- OutlinedTextField
  "Applies to PDF and Word. Original Files keep their own names."
----------------------------------------------------
                                            [ Cancel ]
====================================================

============ Combined File Name Setting =============
[ Combined File Name                                ]
  Edit file name template for combined exports.
  [ {folder}_{date}                               ]
  [+ {folder}] [+ {date}] [+ {time}] [+ {count}]
  Preview: Biology_2026-10-04.pdf
----------------------------------------------------
  [ Reset to default ]              [ Cancel ] [ Save ]
====================================================
```

## Logic Notes
- **Pull Indicator Resistance**: Post-scroll delta scaled by `0.5f` until threshold (`80.dp`). Past threshold, arrow rotates 360° linearly with pull distance.
- **Refresh Execution**: Launches background coroutine running `refresh()` on repositories, followed by `delay` to satisfy the 700ms minimum display floor. Wrapped in `withTimeoutOrNull(10000L)`.
- **Existing Scan / Retry Mechanisms Finding**: `PhotoRepository.refresh()`, `FolderRepository.refresh()`, `DocumentRepository.refresh()`, `TextNoteRepository.refresh()`, and `CanvasNoteRepository.refresh()` re-query local SQLite tables and update Flow states. DOCX FTS backfill executes in `DocumentRepository.refresh()`. Photos do not possess an automated background scanner daemon for missing thumbnails; as instructed, no new scanner daemon is introduced.
- **Current Add to Group Behavior Finding**: In existing code, `PhotoRepository.addPhotosToExistingGroup` updated `group_id`, `folder_id = targetGroup.folderId`, and `subfolder_id = targetGroup.subfolderId`, but `addPhotosToGroup` only updated `group_id`. We unified both paths to ensure photos always inherit the target group's folder and subfolder.
- **Combined File Name Rule Finding**: CombineManager previously generated `${cleanName}_${System.currentTimeMillis()}`. Because epoch milliseconds are not a valid user token, `{folder}_{date}` is established as the default preset, with automatic numeric deduplication `(1)`, `(2)` if identical files exist.

## Risks
- *Risk*: NestedScrollConnection conflicts with normal list fling or nested scroll containers.
  *Mitigation*: Pre-scroll only consumes when indicator is already pulled down. Post-scroll only consumes when `canScrollBackward == false` (list is hard at position 0).
- *Risk*: Keyboard overlapping input in Share As dialog on compact screens.
  *Mitigation*: Wrap dialog body in vertical scroll with `imePadding()`.

## Dependencies
- Phase 19 (Batch 2 Highlighter, Blending, Dock, Home Scroll Edges).

## Acceptance Criteria
- [x] Pull-to-refresh functions on Home, Notes tab, and FolderDetailScreen with curved arrow indicator.
- [x] Pull progress smoothly drives rotation, scale, alpha, and translation inside graphicsLayer with zero recomposition per frame.
- [x] Minimum 700ms visibility floor and 10s safety timeout enforced.
- [x] Gating disables refresh during multi-select, active search, or open overlays.
- [x] "Add to Group" dialog displays current folder groups first, other folders in Home order, and hides folders without groups.
- [x] Adding photos to a group updates `folder_id` and `subfolder_id` to match the target group.
- [x] "Share As" dialog features optional file name field with preset placeholder and sanitization.
- [x] Settings contains "Combined file name" preset editor with token chips and live preview.
- [x] Full unit test suite passes with 0 failures and 0 compiler warnings.
- [x] Release APK compiled and verified as versionName "1.5.6 Beta", versionCode 20.
