<!-- --- Arinara Network (c) 2026 --- -->
<!-- Exclusive property of Arinara Network. -->
<!-- Unauthorized use, reproduction, distribution, or modification of this code, -->
<!-- in whole or in part, for any purpose, is strictly prohibited without prior -->
<!-- written consent from Arinara Network as sole legal owner of this codebase. -->

# Phase 28 - WorkspacesPart2

## Goal
Implement Batch 2B of Fotara 1.6.0 Workspaces: cross-workspace folder movement, workspace deletion with comprehensive content handling (move to Home, move to Trash, or permanent purge) and non-blocking safety, destination workspace selection on Trash restoration, unified workspace-grouped folder pickers, and workspace-scoped Search filtering across all note types.

## Scope
1. **Move Folders to Another Workspace**:
   - `WorkspaceRepository.moveFolders(folderIds: List<Long>, targetWorkspaceId: Long)`: single atomic transaction updating `folders.workspace_id`.
   - Subfolders, notes, pin states, lock states, LinkIt relationships, colors, schedules, and FTS rows remain intact.
   - Folder card dropdown menu on Home: add "Move to workspace" after "Select" and before the divider.
   - Home select mode: add "Move to workspace" action button to the multi-select TopAppBar.
   - "Move to workspace" dialog with radio list of all workspaces, current workspace disabled for single/uniform selections, enabled Move button only on valid selection.
   - Post-move: exit multi-select mode, show one-shot snackbar "Moved to <name>" / "Moved N folders to <name>", keep user on current tab while moved folders disappear.
   - Tab switching during select mode clears selection while preserving select mode; long-press panel and drag reorder disabled while select mode is active.
   - LinkIt cross-workspace behavior: links stay intact; glow is calculated only among folders visible in the current workspace tab.

2. **Delete Workspace**:
   - Long-press panel on `CUSTOM` workspaces adds "Delete" (red, trash icon). Home and Archive never show Delete.
   - Pre-deletion content stats (folders count, notes count across all media types, total size in bytes) computed on background thread with loading state.
   - Confirm Dialog: "Delete <name>?" with counts and size (or "This workspace is empty.") and checkbox "Also delete everything inside" (unchecked by default).
   - Unchecked path: single transaction moving all folders (active and trashed) to Home, deleting workspace row, and compacting dense positions `0..N`. Message: "Workspace deleted. Folders moved to Home."
   - Checked path: secondary dialog "What should happen to the contents?" with "Move to Trash" and "Delete permanently".
     - Permanent delete requires confirmation: "Delete permanently?" -> "<counts and size> will be erased for good." -> Cancel / "Delete forever".
   - Non-cancellable, modal progress dialog ("Deleting <name>...", "Folder x of N", percent bar) running in application scope to survive rotation.
   - Sequential processing of live folders reusing existing `deleteFolders` (for Trash) or `purgeFolderPermanently` (for Permanent). Final transaction reassigns remaining trashed folders to Home, deletes workspace row, and compacts positions.
   - Partial failure safety: workspace row retained if interrupted, remaining folders stay intact, message "Could not finish deleting. N folders remain.".
   - Startup check repairs any dangling `workspace_id` to Home.
   - Fallback: selected workspace switches to Home if deleted; active Search scope falls back to All.

3. **Restoring Folders from Trash with Destination Choice**:
   - Direct folder restore and orphan note parent revival prompt dialog "Restore to which workspace?".
   - Radio list of Home, Archive, and all custom workspaces in saved order, preselecting folder's `workspace_id`.
   - Restoring a note into an existing live folder requires no dialog and maintains its current workspace.
   - Restored folder reappears in chosen workspace tab; snackbar "Restored to <name>".

4. **Workspaces in Folder Pickers and Search**:
   - Shared composable `WorkspaceFolderPickerColumn` grouping folders by workspace in saved order (Home first, Archive, Custom).
   - If folders exist in only one workspace, renders flat list without headers; if in two or more, displays small muted headers per workspace (empty workspaces omitted).
   - Replaces folder lists across `DestinationPickerDialog` (folder detail, copy, move group, move doc, move text note), `SharePlacementScreen`, `NotesScreen` (+ create & move), `CanvasScreen` (move canvas), and `TrashScreen` (orphan target).
   - Search scope row: always visible segmented container with `All` (grid icon) and all workspaces in saved order.
   - Selecting workspace scope filters all note types (photos, text notes, documents, canvas notes, folders) by `folders.workspace_id`.
   - Survives rotation, resets to `All` on Search open, falls back to `All` if workspace is deleted. Empty state: "No results in <name>".

## Out Of Scope
- Batch 3: PDF content OCR, release banner generation, release packaging.
- Database schema changes (v16 schema from Batch 2A is preserved).
- FTS table modifications, `ViewportTransform.kt`, and `fotara.fileprovider`.

## Acceptance Criteria
- [x] `WorkspaceRepository.moveFolders` updates `workspace_id` in single transaction; subfolders/notes follow.
- [x] Folder dropdown on Home and multi-select TopAppBar include "Move to workspace" action.
- [x] "Move to workspace" picker dialog displays all workspaces with appropriate current workspace disablement.
- [x] Long-press panel on custom workspaces includes red "Delete" action; Home and Archive never offer deletion.
- [x] Pre-deletion stats match folder card note count definition; file size calculated on background thread.
- [x] Unchecked workspace deletion moves live and trashed folders to Home and compacts positions.
- [x] Checked workspace deletion routes live folders through existing Trash or permanent purge with modal progress dialog and partial failure safety.
- [x] Startup repair query safely moves any dangling `workspace_id` to Home.
- [x] Restoring trashed folder or reviving orphan parent prompts workspace destination selection.
- [x] Shared folder picker composable groups folders by workspace when 2+ workspaces have folders and falls back to flat list for single workspace.
- [x] Search screen scope row is always visible, displaying All and all workspaces, filtering all search queries by workspace.
- [x] Unit tests covering moveFolders, deleteWorkspace, restore destination, picker grouping, and search workspace filtering pass with 100% success rate.
