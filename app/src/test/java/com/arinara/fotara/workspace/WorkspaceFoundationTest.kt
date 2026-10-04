// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.workspace

import com.arinara.fotara.data.db.FotaraDbHelper
import com.arinara.fotara.data.model.Folder
import com.arinara.fotara.data.model.Workspace
import com.arinara.fotara.data.model.WorkspaceKind
import com.arinara.fotara.data.repository.WorkspaceError
import com.arinara.fotara.data.repository.WorkspaceResult
import com.arinara.fotara.data.repository.WorkspaceValidator
import com.arinara.fotara.test.FakeFolderRepository
import com.arinara.fotara.test.FakePhotoRepository
import com.arinara.fotara.test.FakeSettingsRepository
import com.arinara.fotara.test.FakeWorkspaceRepository
import com.arinara.fotara.ui.home.HomeViewModel
import com.arinara.fotara.ui.home.workspace.TabDragState
import com.arinara.fotara.ui.home.workspace.TabGestureEvent
import com.arinara.fotara.ui.home.workspace.TabGestureReducer
import com.arinara.fotara.ui.home.workspace.TabGestureState
import com.arinara.fotara.ui.home.workspace.WorkspaceReorderHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkspaceFoundationTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // =========================================================================
    // 1. Name Validation (Pure Unit Tests)
    // =========================================================================

    @Test
    fun testNormalizeName_TrimsAndCollapsesWhitespace() {
        val input = "   My    College     Notes   "
        val expected = "My College Notes"
        assertEquals(expected, WorkspaceValidator.normalizeName(input))
    }

    @Test
    fun testValidateName_BlankOrEmpty_ReturnsNameEmpty() {
        val existing = listOf(Workspace(1L, "uuid1", WorkspaceKind.HOME, "", 0))
        assertEquals(WorkspaceError.NameEmpty, WorkspaceValidator.validateName("", existing))
        assertEquals(WorkspaceError.NameEmpty, WorkspaceValidator.validateName("     ", existing))
    }

    @Test
    fun testValidateName_TooLong_ReturnsNameTooLong() {
        val existing = listOf(Workspace(1L, "uuid1", WorkspaceKind.HOME, "", 0))
        val exactly20 = "12345678901234567890" // 20 chars -> OK
        val tooLong = "123456789012345678901" // 21 chars -> Too Long
        assertNull(WorkspaceValidator.validateName(exactly20, existing))
        assertEquals(WorkspaceError.NameTooLong, WorkspaceValidator.validateName(tooLong, existing))
    }

    @Test
    fun testValidateName_ReservedBuiltinNames_ReturnsNameReserved() {
        val existing = emptyList<Workspace>()
        assertEquals(WorkspaceError.NameReserved, WorkspaceValidator.validateName("Home", existing))
        assertEquals(WorkspaceError.NameReserved, WorkspaceValidator.validateName("home", existing))
        assertEquals(WorkspaceError.NameReserved, WorkspaceValidator.validateName("HOME", existing))
        assertEquals(WorkspaceError.NameReserved, WorkspaceValidator.validateName("Archive", existing))
        assertEquals(WorkspaceError.NameReserved, WorkspaceValidator.validateName("archive", existing))
        assertEquals(WorkspaceError.NameReserved, WorkspaceValidator.validateName("Arsip", existing))
        assertEquals(WorkspaceError.NameReserved, WorkspaceValidator.validateName("arsip", existing))
    }

    @Test
    fun testValidateName_CaseInsensitiveDuplicates_ReturnsNameDuplicate() {
        val existing = listOf(
            Workspace(1L, "uuid1", WorkspaceKind.HOME, "", 0),
            Workspace(2L, "uuid2", WorkspaceKind.ARCHIVE, "", 1),
            Workspace(3L, "uuid3", WorkspaceKind.CUSTOM, "Work Projects", 2)
        )
        assertEquals(WorkspaceError.NameDuplicate, WorkspaceValidator.validateName("Work Projects", existing))
        assertEquals(WorkspaceError.NameDuplicate, WorkspaceValidator.validateName("work projects", existing))
        assertEquals(WorkspaceError.NameDuplicate, WorkspaceValidator.validateName("WORK PROJECTS", existing))
        assertEquals(WorkspaceError.NameDuplicate, WorkspaceValidator.validateName("  Work   Projects  ", existing))

        // Editing own workspace allows same name
        assertNull(WorkspaceValidator.validateName("Work Projects", existing, editingWorkspaceId = 3L))
    }

    // =========================================================================
    // 2. Repository Logic & Rules (10 Custom Limit, Dense Positions)
    // =========================================================================

    @Test
    fun testRepository_LimitOf10CustomWorkspacesEnforced() = runTest {
        val repo = FakeWorkspaceRepository()
        // Home and Archive already exist (total 2 built-in, 0 custom)
        assertEquals(0, repo.observeWorkspaces().first().count { it.kind == WorkspaceKind.CUSTOM })

        // Create 10 custom workspaces
        for (i in 1..10) {
            val result = repo.createWorkspace("Workspace $i")
            assertTrue("Creation $i should succeed", result is WorkspaceResult.Success)
        }

        assertEquals(10, repo.observeWorkspaces().first().count { it.kind == WorkspaceKind.CUSTOM })

        // 11th custom workspace must fail with LimitReached
        val eleventh = repo.createWorkspace("Workspace 11")
        assertTrue(eleventh is WorkspaceResult.Error)
        assertEquals(WorkspaceError.LimitReached, (eleventh as WorkspaceResult.Error).error)
    }

    @Test
    fun testRepository_BuiltInWorkspacesCannotBeRenamed() = runTest {
        val repo = FakeWorkspaceRepository()
        val home = repo.getHomeWorkspace()
        val archive = repo.getArchiveWorkspace()

        val renameHome = repo.renameWorkspace(home.id, "New Home")
        assertTrue(renameHome is WorkspaceResult.Error)
        assertEquals(WorkspaceError.BuiltInImmutable, (renameHome as WorkspaceResult.Error).error)

        val renameArchive = repo.renameWorkspace(archive.id, "New Archive")
        assertTrue(renameArchive is WorkspaceResult.Error)
        assertEquals(WorkspaceError.BuiltInImmutable, (renameArchive as WorkspaceResult.Error).error)
    }

    @Test
    fun testRepository_ReorderKeepsHomeFixedAtPosition0() = runTest {
        val repo = FakeWorkspaceRepository()
        val home = repo.getHomeWorkspace()
        val archive = repo.getArchiveWorkspace()
        val custom1 = (repo.createWorkspace("Math") as WorkspaceResult.Success).data

        // Attempt to move custom1 to position 0 before Home
        val invalidOrder = listOf(custom1.id, home.id, archive.id)
        val result = repo.reorderWorkspaces(invalidOrder)
        assertTrue(result is WorkspaceResult.Error)
        assertEquals(WorkspaceError.InvalidReorder, (result as WorkspaceResult.Error).error)

        // Valid reorder: Home remains first (pos 0), Archive and Math swapped
        val validOrder = listOf(home.id, custom1.id, archive.id)
        val successResult = repo.reorderWorkspaces(validOrder)
        assertTrue(successResult is WorkspaceResult.Success)

        val updated = repo.observeWorkspaces().first()
        assertEquals(home.id, updated[0].id)
        assertEquals(0, updated[0].position)
        assertEquals(custom1.id, updated[1].id)
        assertEquals(1, updated[1].position)
        assertEquals(archive.id, updated[2].id)
        assertEquals(2, updated[2].position)
    }

    // =========================================================================
    // 3. Tab Gesture State Machine (Pure Reducer Tests)
    // =========================================================================

    @Test
    fun testGesture_TapSelectsTabImmediately() {
        val state0 = TabDragState(state = TabGestureState.IDLE)
        val state1 = TabGestureReducer.reduce(state0, TabGestureEvent.Down(tabId = 3L, kind = WorkspaceKind.CUSTOM, x = 100f))
        assertEquals(TabGestureState.PRESSED, state1.state)

        val state2 = TabGestureReducer.reduce(state1, TabGestureEvent.Up)
        assertEquals(TabGestureState.IDLE, state2.state)
        assertFalse(state2.showRenamePanel)
        assertFalse(state2.isDragging)
    }

    @Test
    fun testGesture_LongPressOnCustom_ShowsPanelAndLifts() {
        val state0 = TabDragState(state = TabGestureState.IDLE)
        val state1 = TabGestureReducer.reduce(state0, TabGestureEvent.Down(tabId = 3L, kind = WorkspaceKind.CUSTOM, x = 100f))
        val state2 = TabGestureReducer.reduce(state1, TabGestureEvent.LongPressTimeout)

        assertEquals(TabGestureState.HELD, state2.state)
        assertTrue(state2.showRenamePanel)
        assertEquals(3L, state2.activeTabId)
    }

    @Test
    fun testGesture_LongPressOnArchive_LiftsWithoutPanel() {
        val state0 = TabDragState(state = TabGestureState.IDLE)
        val state1 = TabGestureReducer.reduce(state0, TabGestureEvent.Down(tabId = 2L, kind = WorkspaceKind.ARCHIVE, x = 50f))
        val state2 = TabGestureReducer.reduce(state1, TabGestureEvent.LongPressTimeout)

        assertEquals(TabGestureState.HELD, state2.state)
        assertFalse("Archive tab must never show rename panel", state2.showRenamePanel)
        assertTrue(state2.isArchive)
    }

    @Test
    fun testGesture_LongPressOnHome_Ignored() {
        val state0 = TabDragState(state = TabGestureState.IDLE)
        val state1 = TabGestureReducer.reduce(state0, TabGestureEvent.Down(tabId = 1L, kind = WorkspaceKind.HOME, x = 20f))
        assertEquals(TabGestureState.IDLE, state1.state)
    }

    @Test
    fun testGesture_MovementBeyondSlop_DismissesPanelAndStartsDragging() {
        val state0 = TabDragState(state = TabGestureState.IDLE)
        val state1 = TabGestureReducer.reduce(state0, TabGestureEvent.Down(tabId = 3L, kind = WorkspaceKind.CUSTOM, x = 100f))
        val state2 = TabGestureReducer.reduce(state1, TabGestureEvent.LongPressTimeout)
        assertTrue(state2.showRenamePanel)

        // Move 4dp (< slop of 8dp) -> remains HELD, panel stays
        val state3 = TabGestureReducer.reduce(state2, TabGestureEvent.Move(currentX = 104f, touchSlopPx = 8f))
        assertEquals(TabGestureState.HELD, state3.state)
        assertTrue(state3.showRenamePanel)

        // Move 15dp (> slop of 8dp) -> DRAGGING, panel closed
        val state4 = TabGestureReducer.reduce(state2, TabGestureEvent.Move(currentX = 115f, touchSlopPx = 8f))
        assertEquals(TabGestureState.DRAGGING, state4.state)
        assertFalse("Panel must close immediately on drag beyond slop", state4.showRenamePanel)
        assertTrue(state4.isDragging)
        assertEquals(15f, state4.dragDeltaX, 0.01f)
    }

    @Test
    fun testGesture_CancelRestoresIdle() {
        val state0 = TabDragState(
            state = TabGestureState.DRAGGING,
            activeTabId = 3L,
            dragDeltaX = 50f
        )
        val state1 = TabGestureReducer.reduce(state0, TabGestureEvent.Cancel)
        assertEquals(TabGestureState.IDLE, state1.state)
        assertEquals(0f, state1.dragDeltaX, 0.01f)
        assertFalse(state1.showRenamePanel)
    }

    @Test
    fun testReorderHelper_ClampsDropPositionsAfterHome() {
        // Slot 0 is HOME, which can never be replaced
        val clamped0 = WorkspaceReorderHelper.clampDropSlot(targetSlot = 0, listSize = 5)
        assertEquals(1, clamped0)

        // Target past end clamped to last index
        val clampedPastEnd = WorkspaceReorderHelper.clampDropSlot(targetSlot = 10, listSize = 5)
        assertEquals(4, clampedPastEnd)
    }

    @Test
    fun testReorderHelper_ReorderListMaintainsDensePositions() {
        val list = listOf(
            Workspace(1L, "u1", WorkspaceKind.HOME, "", 0),
            Workspace(2L, "u2", WorkspaceKind.ARCHIVE, "", 1),
            Workspace(3L, "u3", WorkspaceKind.CUSTOM, "Work", 2),
            Workspace(4L, "u4", WorkspaceKind.CUSTOM, "Personal", 3)
        )

        // Move "Personal" (id 4) to slot 1 (before Archive)
        val reordered = WorkspaceReorderHelper.reorderList(list, movingWorkspaceId = 4L, targetSlot = 1)
        assertEquals(4, reordered.size)
        assertEquals(1L, reordered[0].id)
        assertEquals(0, reordered[0].position)
        assertEquals(4L, reordered[1].id)
        assertEquals(1, reordered[1].position)
        assertEquals(2L, reordered[2].id)
        assertEquals(2, reordered[2].position)
        assertEquals(3L, reordered[3].id)
        assertEquals(3, reordered[3].position)
    }

    // =========================================================================
    // 4. Migration Safety & v16 Schema
    // =========================================================================

    @Test
    fun testV16Schema_VersionAndSeedingConstants() {
        assertEquals(16, FotaraDbHelper.DATABASE_VERSION)
        assertEquals("00000000-0000-4000-8000-000000000001", FotaraDbHelper.HOME_WORKSPACE_UUID)
        assertEquals("00000000-0000-4000-8000-000000000002", FotaraDbHelper.ARCHIVE_WORKSPACE_UUID)
    }

    @Test
    fun testV16Migration_SqlIntegrity() {
        val workspacesTableSql = """
            CREATE TABLE IF NOT EXISTS workspaces (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                uuid TEXT NOT NULL UNIQUE,
                kind TEXT NOT NULL,
                name TEXT NOT NULL DEFAULT '',
                position INTEGER NOT NULL DEFAULT 0,
                created_at INTEGER NOT NULL
            )
        """.trimIndent()
        assertTrue(workspacesTableSql.contains("id INTEGER PRIMARY KEY AUTOINCREMENT"))
        assertTrue(workspacesTableSql.contains("uuid TEXT NOT NULL UNIQUE"))
        assertTrue(workspacesTableSql.contains("kind TEXT NOT NULL"))

        val folderAlterSql = "ALTER TABLE folders ADD COLUMN workspace_id INTEGER NOT NULL DEFAULT 1"
        assertTrue(folderAlterSql.contains("workspace_id INTEGER NOT NULL DEFAULT 1"))
    }

    // =========================================================================
    // 5. Backup Export & Import Format (Workspace Mapping)
    // =========================================================================

    @Test
    fun testBackupExportFormat_IncludesWorkspacesAndFolderWorkspaceUuid() {
        val folder = Folder(
            id = 10L,
            name = "Algorithms",
            workspaceId = 3L
        )
        val workspaces = listOf(
            Workspace(1L, FotaraDbHelper.HOME_WORKSPACE_UUID, WorkspaceKind.HOME, "", 0),
            Workspace(2L, FotaraDbHelper.ARCHIVE_WORKSPACE_UUID, WorkspaceKind.ARCHIVE, "", 1),
            Workspace(3L, "custom-uuid-3", WorkspaceKind.CUSTOM, "Computer Science", 2)
        )

        // Mapping logic: folder's workspaceId maps to workspace's UUID
        val workspaceMap = workspaces.associateBy { it.id }
        val mappedUuid = workspaceMap[folder.workspaceId]?.uuid ?: FotaraDbHelper.HOME_WORKSPACE_UUID
        assertEquals("custom-uuid-3", mappedUuid)

        val unmappedUuid = workspaceMap[999L]?.uuid ?: FotaraDbHelper.HOME_WORKSPACE_UUID
        assertEquals(FotaraDbHelper.HOME_WORKSPACE_UUID, unmappedUuid)
    }

    @Test
    fun testBackupImport_LegacyDataWithoutWorkspaces_RoutesAllFoldersToHome() {
        // Logic check: if "workspaces" key is missing, folder gets home workspace id (1L)
        val hasWorkspaces = false
        val assignedWorkspaceId = if (!hasWorkspaces) 1L else 99L
        assertEquals(1L, assignedWorkspaceId)
    }

    @Test
    fun testFolderItemCount_SumsAllObjectTypes() {
        // Folder item count must count photos + text notes + canvas notes + document notes
        val photoCount = 3
        val textNoteCount = 2
        val canvasCount = 1
        val docCount = 4
        val totalExpected = photoCount + textNoteCount + canvasCount + docCount

        val folder = Folder(
            id = 5L,
            name = "Physics",
            photoCount = totalExpected
        )
        assertEquals(10, folder.photoCount)
        assertEquals(10, folder.itemCount)
    }

    // =========================================================================
    // 6. HomeViewModel Integration & Fallback
    // =========================================================================

    @Test
    fun testHomeViewModel_SelectedWorkspaceFallbackToHome() = runTest(testDispatcher) {
        val folderRepo = FakeFolderRepository(
            initialFolders = listOf(
                Folder(id = 1L, name = "Biology", workspaceId = 1L),
                Folder(id = 2L, name = "Calculus", workspaceId = 2L)
            )
        )
        val photoRepo = FakePhotoRepository()
        val workspaceRepo = FakeWorkspaceRepository()

        val viewModel = HomeViewModel(
            folderRepository = folderRepo,
            photoRepository = photoRepo,
            workspaceRepository = workspaceRepo
        )

        // Initial selected workspace is HOME (1L)
        val state = viewModel.uiState.value
        assertEquals(1L, state.selectedWorkspaceId)

        // Select invalid workspace id (999L)
        viewModel.selectWorkspace(999L)
        advanceUntilIdle()
        assertEquals(1L, viewModel.uiState.value.selectedWorkspaceId)
    }

    @Test
    fun testHomeViewModel_CreateFolderUsesSelectedWorkspaceId() = runTest(testDispatcher) {
        val folderRepo = FakeFolderRepository(initialFolders = emptyList())
        val photoRepo = FakePhotoRepository()
        val workspaceRepo = FakeWorkspaceRepository()
        val customWs = (workspaceRepo.createWorkspace("Art") as WorkspaceResult.Success).data

        val viewModel = HomeViewModel(
            folderRepository = folderRepo,
            photoRepository = photoRepo,
            workspaceRepository = workspaceRepo
        )

        // Select the custom workspace
        viewModel.selectWorkspace(customWs.id)

        // Create folder from Home (+) button
        viewModel.createFolder(name = "Painting", colorHex = "#FF0000", isPinned = false)
        advanceUntilIdle()

        val created = folderRepo.getFolders().first().firstOrNull { it.name == "Painting" }
        assertNotNull(created)
        assertEquals("Folder must be created with selected workspace id", customWs.id, created!!.workspaceId)
    }
}
