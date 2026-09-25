# Phase 5 - GroupExpansionAndLinkIt (v1.2 Release)

## Goal
Deliver Fotara v1.2 containing the complete Group Management Expansion batch, the LinkIt spatial layout feature, and the light theme bug fix. Ensure production-grade reliability, testability, and adherence to offline-first principles.

## Scope
- **Part 1 — Bug Fix**:
  - Remove non-functional "Light" theme option from Settings and `ThemeMode` enum.
  - Silently migrate any stored "Light" preference to System Default (`ThemeMode.SYSTEM`).
- **Part 2 — Group Management Expansion**:
  - 2.1 Merge Group: Multi-select 2+ groups to combine into one new group with a user-typed name; delete source groups and re-point members.
  - 2.2 Move Group: Move group and all member photos to another folder/subfolder via folder-grid context menu and Group screen overflow menu.
  - 2.3 Add to Group: Contextual action on 1+ standalone photos to join an existing group app-wide; photos automatically adopt target group's folder/subfolder.
  - 2.4 Direct Capture/Import into Group: Group screen [+] option to capture (CameraX) or import (gallery) directly into the group via review modal.
  - 2.5 Manual Cover Photo: "Set as cover" option on member photos in Group screen overriding the earliest `added_at` default.
  - 2.6 Independent Photo Duplication: "Copy to..." action creating an independent disk file and database row with independent OCR and metadata.
  - 2.7 Undo Action Snackbar: 4-second snackbar with "Undo" action for delete, move, and ungroup operations.
  - 2.8 Recently Used Destinations: Top "Recently used" section in all folder/subfolder/group pickers.
  - 2.9 Group-Level Deadline: `photo_groups.linked_deadline` with independent alarms, notifications, and sort integration.
  - 2.10 Group & Mixed Export: Single-group and multi-selection export supporting PDF and ZIP formats with section headers.
  - 2.11 Trash Severance: Trashing a photo immediately detaches group membership; auto-dissolves group if $\le 1$ member remains; restored photos become standalone.
  - 2.12 Capture Review Rename: Inline caption rename control in `CaptureReviewSliderModal` prior to committing.
  - 2.13 Disabled Widget: Disable widget receiver and service in `AndroidManifest.xml` without deleting implementation code.
- **Part 3 — LinkIt**:
  - `LinkGroup` data model and SQLite table (`item_type`, `member_ids` ordered list max 4, `created_at`).
  - Home screen multi-select linking for 2–4 folders.
  - Folder screen multi-select linking for 2–4 grid items (standalone notes and groups).
  - Chain-link visual badge indicator on linked cells.
  - Consecutive layout arrangement anchored to the highest-ranking member under the active sort mode.
  - Pinned folder rule: If any folder in a link is pinned, the entire link block moves to the pinned section.
  - Unlink action in context menus with auto-dissolve when $\le 1$ member remains.
  - Trashing linked items removes them from the link with auto-dissolve when $\le 1$ remains.

## Out Of Scope
- Subfolder tab linking (subfolders are navigation tabs, not grid cells).
- Cloud synchronization or cross-device collaboration.
- Rich-text formatted notes.

## Features
### Merge Group
Combines two or more selected photo groups into a newly named group. Member photos are unified under the new group, the cover photo defaults to the earliest `added_at` photo, and original group records are deleted.

### Move Group to Subfolder
Allows moving a group and all its member photos together atomically to a target folder or subfolder.

### Add to Group
Enables adding 1 or more standalone photos to any existing group app-wide. The photos relocate to the target group's subfolder.

### LinkIt
Enables grouping 2 to 4 folders or 2 to 4 grid items so they always render consecutively in grid sequence regardless of sort criteria.

## UI Mockup
```
FOLDER MULTI-SELECT (2+ GROUPS SELECTED):
+-------------------------------------------------------------+
| [X] 2 groups selected                         [Merge] [Link]|
+-------------------------------------------------------------+

LINKED CARDS IN GRID:
+-------------------+ +-------------------+
| [(#) Link Badge]  | | [(#) Link Badge]  |
| Calc Formulas     | | Physics Formulas  |
| (Cover Thumbnail) | | (Cover Thumbnail) |
+-------------------+ +-------------------+

RECENTLY USED DESTINATIONS IN PICKER:
+-------------------------------------------------------------+
| Select Destination                                          |
| RECENTLY USED:                                              |
| [Group: Midterm Notes] [Folder: Calculus]                   |
| ----------------------------------------------------------- |
| Biology                                                     |
| Calculus                                                    |
+-------------------------------------------------------------+
```

## Logic Notes
- Database version upgraded to 8 to support `photo_groups.linked_deadline` and `link_groups` table.
- LinkGroup hard cap of 4 enforced in UI multi-select.
- Trash severance applies immediately on `deletePhoto` and `deletePhotos`.

## Acceptance Criteria
- [ ] ThemeMode offers only SYSTEM and DARK; legacy LIGHT migrates silently to SYSTEM.
- [ ] Multi-selecting 2+ groups enables "Merge" and requires a new name dialog.
- [ ] Move group moves group and all member photos to target subfolder.
- [ ] Add to group relocates selected photos to the group's subfolder and attaches them to the group.
- [ ] Group screen [+] allows capture and gallery import directly into the group via review modal.
- [ ] "Set as cover" overrides `cover_photo_id` in Group screen.
- [ ] "Copy to..." creates an independent physical file and DB row.
- [ ] Undo snackbar reverses delete, move, and ungroup operations.
- [ ] Pickers show recently used destinations.
- [ ] Group-level deadline is stored, alarm scheduled, and sorted properly.
- [ ] Export supports PDF and ZIP for single group and mixed selections.
- [ ] Moving photo to trash immediately severs group membership and auto-dissolves group if $\le 1$ remains.
- [ ] Capture review modal provides caption renaming before saving.
- [ ] Widget is disabled in AndroidManifest.xml without deleting code.
- [ ] LinkGroup persists up to 4 ordered member IDs for folders and grid items.
- [ ] Linked items render consecutively in grid order, with block position governed by highest-ranking member.
- [ ] Linked folders move to pinned section if any member is pinned.
- [ ] Unlinking or trashing removes member and dissolves link if $\le 1$ remains.
