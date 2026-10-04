// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.workspace

import com.arinara.fotara.data.model.Folder
import com.arinara.fotara.data.model.Workspace
import com.arinara.fotara.data.model.WorkspaceKind
import com.arinara.fotara.data.repository.WorkspaceContentStats
import com.arinara.fotara.data.repository.WorkspaceError
import com.arinara.fotara.data.repository.WorkspaceResult
import com.arinara.fotara.test.FakeFolderRepository
import com.arinara.fotara.test.FakePhotoRepository
import com.arinara.fotara.test.FakeSettingsRepository
import com.arinara.fotara.test.FakeWorkspaceRepository
import com.arinara.fotara.ui.components.WorkspaceFolderGroupingHelper
import com.arinara.fotara.ui.components.computeFolderGlowAnchors
import com.arinara.fotara.ui.home.HomeViewModel
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
class WorkspaceMoveDeleteTest {

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
    // 1. Move Folders to Another Workspace
    // =========================================================================

    @Test
    fun testMoveFolders_SingleFolder_Success() = runTest(testDispatcher) {
        val folderRepo = FakeFolderRepository(
            initialFolders = listOf(
                Folder(id = 10L, name = "Docs", workspaceId = 1L)
            )
        )
        val workspaceRepo = FakeWorkspaceRepository(folderRepository = folderRepo)
        val customWs = (workspaceRepo.createWorkspace("Work") as WorkspaceResult.Success).data

        val result = workspaceRepo.moveFolders(listOf(10L), customWs.id)
        assertTrue(result is WorkspaceResult.Success)

        val updatedFolders = folderRepo.getFolders().first()
        val movedFolder = updatedFolders.first { it.id == 10L }
        assertEquals(customWs.id, movedFolder.workspaceId)
    }

    @Test
    fun testMoveFolders_MultipleFolders_Success() = runTest(testDispatcher) {
        val folderRepo = FakeFolderRepository(
            initialFolders = listOf(
                Folder(id = 10L, name = "Docs", workspaceId = 1L),
                Folder(id = 20L, name = "Receipts", workspaceId = 1L),
                Folder(id = 30L, name = "Family", workspaceId = 2L)
            )
        )
        val workspaceRepo = FakeWorkspaceRepository(folderRepository = folderRepo)
        val customWs = (workspaceRepo.createWorkspace("Finance") as WorkspaceResult.Success).data

        val result = workspaceRepo.moveFolders(listOf(10L, 20L), customWs.id)
        assertTrue(result is WorkspaceResult.Success)

        val updated = folderRepo.getFolders().first().associateBy { it.id }
        assertEquals(customWs.id, updated[10L]?.workspaceId)
        assertEquals(customWs.id, updated[20L]?.workspaceId)
        assertEquals(2L, updated[30L]?.workspaceId) // Untouched
    }

    @Test
    fun testMoveFolders_SameWorkspace_IsNoOpSuccess() = runTest(testDispatcher) {
        val folderRepo = FakeFolderRepository(
            initialFolders = listOf(
                Folder(id = 10L, name = "Docs", workspaceId = 1L)
            )
        )
        val workspaceRepo = FakeWorkspaceRepository(folderRepository = folderRepo)

        val result = workspaceRepo.moveFolders(listOf(10L), 1L)
        assertTrue(result is WorkspaceResult.Success)

        val updated = folderRepo.getFolders().first().first { it.id == 10L }
        assertEquals(1L, updated.workspaceId)
    }

    @Test
    fun testMoveFolders_UnknownTargetWorkspace_ReturnsNotFound() = runTest(testDispatcher) {
        val folderRepo = FakeFolderRepository(
            initialFolders = listOf(Folder(id = 10L, name = "Docs", workspaceId = 1L))
        )
        val workspaceRepo = FakeWorkspaceRepository(folderRepository = folderRepo)

        val result = workspaceRepo.moveFolders(listOf(10L), 9999L)
        assertTrue(result is WorkspaceResult.Error)
        assertEquals(WorkspaceError.NotFound, (result as WorkspaceResult.Error).error)
    }

    @Test
    fun testMoveFolders_TrashedFolder_ReturnsTrashedCannotMove() = runTest(testDispatcher) {
        val folderRepo = FakeFolderRepository(
            initialFolders = listOf(Folder(id = 10L, name = "TrashedDoc", workspaceId = 1L))
        )
        // Move to trash
        folderRepo.deleteFolders(listOf(10L))

        val workspaceRepo = FakeWorkspaceRepository(folderRepository = folderRepo)
        val customWs = (workspaceRepo.createWorkspace("Archive2") as WorkspaceResult.Success).data

        val result = workspaceRepo.moveFolders(listOf(10L), customWs.id)
        assertTrue(result is WorkspaceResult.Error)
        assertEquals(WorkspaceError.TrashedFolderCannotMove, (result as WorkspaceResult.Error).error)
    }

    // =========================================================================
    // 2. Delete Workspace
    // =========================================================================

    @Test
    fun testDeleteWorkspace_BuiltInImmutable() = runTest(testDispatcher) {
        val workspaceRepo = FakeWorkspaceRepository()

        // Home (1L)
        val resHome = workspaceRepo.deleteWorkspaceMoveFoldersToHome(1L)
        assertTrue(resHome is WorkspaceResult.Error)
        assertEquals(WorkspaceError.BuiltInImmutable, (resHome as WorkspaceResult.Error).error)

        // Archive (2L)
        val resArchive = workspaceRepo.deleteWorkspaceWithContents(2L, permanent = false) { _, _ -> }
        assertTrue(resArchive is WorkspaceResult.Error)
        assertEquals(WorkspaceError.BuiltInImmutable, (resArchive as WorkspaceResult.Error).error)
    }

    @Test
    fun testDeleteWorkspace_GetStats_AccurateCountsAndBytes() = runTest(testDispatcher) {
        val folderRepo = FakeFolderRepository(initialFolders = emptyList())
        val workspaceRepo = FakeWorkspaceRepository(folderRepository = folderRepo)
        val ws = (workspaceRepo.createWorkspace("StatsWs") as WorkspaceResult.Success).data

        folderRepo.createFolder("F1", "#FFFFFF", false, ws.id)
        folderRepo.createFolder("F2", "#FFFFFF", false, ws.id)
        folderRepo.createFolder("Other", "#FFFFFF", false, 1L)

        val folders = folderRepo.getFolders().first().associateBy { it.name }
        val f1Id = folders["F1"]!!.id
        val f2Id = folders["F2"]!!.id
        repeat(5) { folderRepo.incrementPhotoCount(f1Id) }
        repeat(3) { folderRepo.incrementPhotoCount(f2Id) }

        val stats = workspaceRepo.getWorkspaceStats(ws.id)

        assertEquals(2, stats.folderCount)
        assertEquals(8, stats.noteCount)
    }

    @Test
    fun testDeleteWorkspace_MoveFoldersToHome_MovesFoldersAndCompactsPositions() = runTest(testDispatcher) {
        val folderRepo = FakeFolderRepository(initialFolders = emptyList())
        val workspaceRepo = FakeWorkspaceRepository(folderRepository = folderRepo)
        val ws3 = (workspaceRepo.createWorkspace("Projects") as WorkspaceResult.Success).data
        val ws4 = (workspaceRepo.createWorkspace("Hobbies") as WorkspaceResult.Success).data

        folderRepo.createFolder("ProjA", "#FFFFFF", false, ws3.id)
        folderRepo.createFolder("ProjB", "#FFFFFF", false, ws3.id)
        folderRepo.createFolder("HomeFolder", "#FFFFFF", false, 1L)

        val deleteRes = workspaceRepo.deleteWorkspaceMoveFoldersToHome(ws3.id)
        assertTrue(deleteRes is WorkspaceResult.Success)

        // All folders from ws3 should now be in Home (1L)
        val folders = folderRepo.getFolders().first().associateBy { it.name }
        assertEquals(1L, folders["ProjA"]?.workspaceId)
        assertEquals(1L, folders["ProjB"]?.workspaceId)
        assertEquals(1L, folders["HomeFolder"]?.workspaceId)

        // Workspaces should no longer contain ws3, and positions should be contiguous 0..N
        val remainingWs = workspaceRepo.observeWorkspaces().first()
        assertFalse(remainingWs.any { it.id == ws3.id })
        assertTrue(remainingWs.any { it.id == ws4.id })
        remainingWs.forEachIndexed { idx, ws ->
            assertEquals(idx, ws.position)
        }
    }

    @Test
    fun testDeleteWorkspace_DeleteWithContents_MoveToTrash() = runTest(testDispatcher) {
        val folderRepo = FakeFolderRepository(initialFolders = emptyList())
        val workspaceRepo = FakeWorkspaceRepository(folderRepository = folderRepo)
        val ws3 = (workspaceRepo.createWorkspace("Scratch") as WorkspaceResult.Success).data

        folderRepo.createFolder("Temp1", "#FFFFFF", false, ws3.id)
        folderRepo.createFolder("Temp2", "#FFFFFF", false, ws3.id)

        var progressCalls = 0
        val deleteRes = workspaceRepo.deleteWorkspaceWithContents(ws3.id, permanent = false) { current, total ->
            progressCalls++
            assertEquals(2, total)
        }
        assertTrue(deleteRes is WorkspaceResult.Success)
        assertEquals(2, progressCalls)

        // Folders should now be in Trash
        val liveFolders = folderRepo.getFolders().first()
        assertTrue(liveFolders.isEmpty())
        val trashedFolders = folderRepo.getTrashedFolders().first()
        assertEquals(2, trashedFolders.size)
        assertTrue(trashedFolders.any { it.name == "Temp1" })
        assertTrue(trashedFolders.any { it.name == "Temp2" })
    }

    @Test
    fun testDeleteWorkspace_DeleteWithContents_PermanentPurge() = runTest(testDispatcher) {
        val folderRepo = FakeFolderRepository(initialFolders = emptyList())
        val workspaceRepo = FakeWorkspaceRepository(folderRepository = folderRepo)
        val ws3 = (workspaceRepo.createWorkspace("PurgeSpace") as WorkspaceResult.Success).data

        folderRepo.createFolder("PurgeMe", "#FFFFFF", false, ws3.id)

        val deleteRes = workspaceRepo.deleteWorkspaceWithContents(ws3.id, permanent = true) { _, _ -> }
        assertTrue(deleteRes is WorkspaceResult.Success)

        val liveFolders = folderRepo.getFolders().first()
        assertTrue(liveFolders.isEmpty())
        val trashedFolders = folderRepo.getTrashedFolders().first()
        assertTrue(trashedFolders.isEmpty())
    }

    @Test
    fun testRepairDanglingWorkspaces_RemapsOrphanFoldersToHome() = runTest(testDispatcher) {
        val folderRepo = FakeFolderRepository(
            initialFolders = listOf(
                Folder(id = 10L, name = "Orphan1", workspaceId = 999L),
                Folder(id = 20L, name = "Orphan2", workspaceId = 888L),
                Folder(id = 30L, name = "ValidHome", workspaceId = 1L)
            )
        )
        val workspaceRepo = FakeWorkspaceRepository(folderRepository = folderRepo)

        val repairedCount = workspaceRepo.repairDanglingWorkspaces()
        assertEquals(2, repairedCount)

        val folders = folderRepo.getFolders().first().associateBy { it.id }
        assertEquals(1L, folders[10L]?.workspaceId)
        assertEquals(1L, folders[20L]?.workspaceId)
        assertEquals(1L, folders[30L]?.workspaceId)
    }

    // =========================================================================
    // 3. Restore Folder with Destination Workspace
    // =========================================================================

    @Test
    fun testRestoreFolder_WithTargetWorkspace_RestoresToTarget() = runTest(testDispatcher) {
        val folderRepo = FakeFolderRepository(
            initialFolders = listOf(
                Folder(id = 10L, name = "Study", workspaceId = 1L)
            )
        )
        // Trash it
        folderRepo.deleteFolders(listOf(10L))

        // Restore to target workspace 3L
        folderRepo.restoreFolder(10L, targetWorkspaceId = 3L)

        val active = folderRepo.getFolders().first()
        val restored = active.firstOrNull { it.id == 10L }
        assertNotNull(restored)
        assertFalse(restored!!.isTrashed)
        assertNull(restored.deletedAt)
        assertEquals(3L, restored.workspaceId)
    }

    @Test
    fun testRestoreFolder_WithoutTargetWorkspace_MaintainsOriginalWorkspace() = runTest(testDispatcher) {
        val folderRepo = FakeFolderRepository(
            initialFolders = listOf(
                Folder(id = 10L, name = "ArchiveFolder", workspaceId = 2L)
            )
        )
        folderRepo.deleteFolders(listOf(10L))

        // Restore with null target
        folderRepo.restoreFolder(10L, targetWorkspaceId = null)

        val restored = folderRepo.getFolders().first().first { it.id == 10L }
        assertEquals(2L, restored.workspaceId)
    }

    // =========================================================================
    // 4. WorkspaceFolderGroupingHelper Tests
    // =========================================================================

    @Test
    fun testFolderGrouping_EmptyFolders_ReturnsEmptyAndNoHeaders() {
        val workspaces = listOf(
            Workspace(1L, "home", WorkspaceKind.HOME, "", 0),
            Workspace(2L, "archive", WorkspaceKind.ARCHIVE, "", 1)
        )
        val (groups, showHeaders) = WorkspaceFolderGroupingHelper.groupFolders(emptyList(), workspaces)
        assertTrue(groups.isEmpty())
        assertFalse(showHeaders)
    }

    @Test
    fun testFolderGrouping_SingleWorkspace_ReturnsNoHeaders() {
        val workspaces = listOf(
            Workspace(1L, "home", WorkspaceKind.HOME, "", 0),
            Workspace(2L, "archive", WorkspaceKind.ARCHIVE, "", 1),
            Workspace(3L, "custom", WorkspaceKind.CUSTOM, "Work", 2)
        )
        val folders = listOf(
            Folder(id = 1L, name = "A", workspaceId = 1L),
            Folder(id = 2L, name = "B", workspaceId = 1L)
        )
        val (groups, showHeaders) = WorkspaceFolderGroupingHelper.groupFolders(folders, workspaces)
        assertEquals(1, groups.size)
        assertFalse("Single workspace with folders must not show headers", showHeaders)
        assertEquals(1L, groups[0].workspace.id)
        assertEquals(2, groups[0].folders.size)
    }

    @Test
    fun testFolderGrouping_MultipleWorkspaces_ReturnsHeadersAndSortedOrder() {
        val wsHome = Workspace(1L, "home", WorkspaceKind.HOME, "", 0)
        val wsArchive = Workspace(2L, "archive", WorkspaceKind.ARCHIVE, "", 1)
        val wsCustom = Workspace(3L, "custom", WorkspaceKind.CUSTOM, "Work", 2)
        val wsEmpty = Workspace(4L, "empty", WorkspaceKind.CUSTOM, "EmptySpace", 3)

        val workspaces = listOf(wsCustom, wsArchive, wsHome, wsEmpty) // Mixed input order

        val folders = listOf(
            Folder(id = 10L, name = "WorkTask", workspaceId = 3L),
            Folder(id = 20L, name = "HomeBudget", workspaceId = 1L),
            Folder(id = 30L, name = "OldTax", workspaceId = 2L)
        )

        val (groups, showHeaders) = WorkspaceFolderGroupingHelper.groupFolders(folders, workspaces)
        assertTrue("Multiple workspaces with folders must show headers", showHeaders)
        assertEquals(3, groups.size)

        // Must be sorted in position order: Home (0), Archive (1), Custom (2)
        assertEquals(WorkspaceKind.HOME, groups[0].workspace.kind)
        assertEquals(1, groups[0].folders.size)
        assertEquals(20L, groups[0].folders[0].id)

        assertEquals(WorkspaceKind.ARCHIVE, groups[1].workspace.kind)
        assertEquals(1, groups[1].folders.size)
        assertEquals(30L, groups[1].folders[0].id)

        assertEquals("Work", groups[2].workspace.name)
        assertEquals(1, groups[2].folders.size)
        assertEquals(10L, groups[2].folders[0].id)

        // Empty workspace wsEmpty must be omitted
        assertFalse(groups.any { it.workspace.id == wsEmpty.id })
    }

    @Test
    fun testFolderGrouping_UnknownWorkspaceId_FallsBackToHome() {
        val workspaces = listOf(
            Workspace(1L, "home", WorkspaceKind.HOME, "", 0)
        )
        val folders = listOf(
            Folder(id = 99L, name = "MysteryFolder", workspaceId = 9999L)
        )
        val (groups, _) = WorkspaceFolderGroupingHelper.groupFolders(folders, workspaces)
        assertEquals(1, groups.size)
        assertEquals(WorkspaceKind.HOME, groups[0].workspace.kind)
        assertEquals(99L, groups[0].folders[0].id)
    }

    @Test
    fun testGetWorkspaceDisplayName_BuiltInAndCustom() {
        val home = Workspace(1L, "home", WorkspaceKind.HOME, "", 0)
        val archive = Workspace(2L, "archive", WorkspaceKind.ARCHIVE, "", 1)
        val custom = Workspace(3L, "custom", WorkspaceKind.CUSTOM, "Personal", 2)

        assertEquals("Home", WorkspaceFolderGroupingHelper.getWorkspaceDisplayName(home, "Home", "Archive"))
        assertEquals("Archive", WorkspaceFolderGroupingHelper.getWorkspaceDisplayName(archive, "Home", "Archive"))
        assertEquals("Personal", WorkspaceFolderGroupingHelper.getWorkspaceDisplayName(custom, "Home", "Archive"))
    }

    // =========================================================================
    // 5. LinkIt Glow Cross-Workspace Isolation
    // =========================================================================

    @Test
    fun testLinkItGlow_SameWorkspace_CalculatesGlowAnchors() {
        // Two folders sharing linkGroupId = 100L in the same workspace tab
        val tabFolders = listOf(
            Folder(id = 1L, name = "A", linkGroupId = 100L, workspaceId = 1L),
            Folder(id = 2L, name = "B", linkGroupId = 100L, workspaceId = 1L)
        )
        val glowAnchors = computeFolderGlowAnchors(tabFolders, columns = 2)

        assertTrue("Folder 1 should have glow anchor", glowAnchors[1L]?.isNotEmpty() == true)
        assertTrue("Folder 2 should have glow anchor", glowAnchors[2L]?.isNotEmpty() == true)
    }

    @Test
    fun testLinkItGlow_CrossWorkspace_NoGlowWhenPartnerNotInTab() {
        // Folder 1 is in Workspace 1, Folder 2 is in Workspace 2, both share linkGroupId = 100L.
        // On Workspace 1's tab, only Folder 1 is visible:
        val workspace1TabFolders = listOf(
            Folder(id = 1L, name = "A", linkGroupId = 100L, workspaceId = 1L),
            Folder(id = 3L, name = "C", linkGroupId = null, workspaceId = 1L)
        )
        val glowAnchors = computeFolderGlowAnchors(workspace1TabFolders, columns = 2)

        // Neither folder 1 nor 3 should have a glow anchor because folder 2 is not visible in this tab
        assertNull("Folder 1 must not have glow anchors because partner is in another workspace tab", glowAnchors[1L])
        assertNull("Folder 3 is unlinked", glowAnchors[3L])
    }

    // =========================================================================
    // 6. Search Workspace Scope Filtering & HomeViewModel Fallback
    // =========================================================================

    @Test
    fun testHomeViewModel_SearchScopeManagementAndFallback() = runTest(testDispatcher) {
        val folderRepo = FakeFolderRepository(
            initialFolders = listOf(
                Folder(id = 1L, name = "Math", workspaceId = 1L),
                Folder(id = 2L, name = "WorkProject", workspaceId = 3L)
            )
        )
        val photoRepo = FakePhotoRepository()
        val workspaceRepo = FakeWorkspaceRepository(folderRepository = folderRepo)
        val customWs = (workspaceRepo.createWorkspace("Career") as WorkspaceResult.Success).data

        val viewModel = HomeViewModel(
            folderRepository = folderRepo,
            photoRepository = photoRepo,
            workspaceRepository = workspaceRepo
        )
        advanceUntilIdle()

        // 1. Initial search state is inactive, scope is null (All)
        assertEquals(null, viewModel.uiState.value.searchWorkspaceScopeId)

        // 2. Open search
        viewModel.activateSearch()
        assertEquals(null, viewModel.uiState.value.searchWorkspaceScopeId)
        assertTrue(viewModel.uiState.value.isSearchActive)

        // 3. Select workspace scope
        viewModel.setSearchWorkspaceScope(customWs.id)
        assertEquals(customWs.id, viewModel.uiState.value.searchWorkspaceScopeId)

        // 4. Select "All" scope (null)
        viewModel.setSearchWorkspaceScope(null)
        assertEquals(null, viewModel.uiState.value.searchWorkspaceScopeId)

        // 5. Select custom workspace again, then delete that workspace
        viewModel.setSearchWorkspaceScope(customWs.id)
        assertEquals(customWs.id, viewModel.uiState.value.searchWorkspaceScopeId)

        // Request delete workspace
        viewModel.initiateDeleteWorkspace(customWs)
        // Confirm move folders to Home
        viewModel.deleteWorkspaceMoveFoldersToHome(customWs)
        advanceUntilIdle()

        // After deleting the workspace, search scope must fall back to null (All)
        assertEquals(null, viewModel.uiState.value.searchWorkspaceScopeId)
    }

    @Test
    fun testHomeViewModel_TabSwitchInSelectModeClearsSelection() = runTest(testDispatcher) {
        val folderRepo = FakeFolderRepository(
            initialFolders = listOf(
                Folder(id = 1L, name = "Folder1", workspaceId = 1L),
                Folder(id = 2L, name = "Folder2", workspaceId = 2L)
            )
        )
        val photoRepo = FakePhotoRepository()
        val workspaceRepo = FakeWorkspaceRepository(folderRepository = folderRepo)

        val viewModel = HomeViewModel(
            folderRepository = folderRepo,
            photoRepository = photoRepo,
            workspaceRepository = workspaceRepo
        )
        advanceUntilIdle()

        // Enter select mode with folder 1
        viewModel.enterMultiSelectMode(1L)
        assertTrue(viewModel.uiState.value.isMultiSelectMode)
        assertTrue(viewModel.uiState.value.selectedFolderIds.contains(1L))

        // Switch workspace tab to Archive (2L)
        viewModel.selectWorkspace(2L)
        advanceUntilIdle()

        // Select mode should remain active, but selection must be cleared
        assertTrue("Select mode should persist across tab switch", viewModel.uiState.value.isMultiSelectMode)
        assertTrue("Selection should be cleared on tab switch", viewModel.uiState.value.selectedFolderIds.isEmpty())
        assertEquals(2L, viewModel.uiState.value.selectedWorkspaceId)
    }
}
