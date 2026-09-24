// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui

import com.arinara.fotara.data.model.Photo
import com.arinara.fotara.data.model.PhotoSource
import com.arinara.fotara.data.model.TagColor
import com.arinara.fotara.test.FakeFolderRepository
import com.arinara.fotara.test.FakePhotoRepository
import com.arinara.fotara.test.FakeSettingsRepository
import com.arinara.fotara.ui.folder.FolderDetailViewModel
import com.arinara.fotara.ui.folder.FolderGridItem
import com.arinara.fotara.ui.folder.PhotoSortOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class FolderDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var folderRepository: FakeFolderRepository
    private lateinit var photoRepository: FakePhotoRepository
    private lateinit var viewModel: FolderDetailViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        folderRepository = FakeFolderRepository()
        photoRepository = FakePhotoRepository(folderRepository)
        // Folder 1 is "Biology"
        viewModel = FolderDetailViewModel(1L, folderRepository, photoRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialLoad_loadsFolderAndSubfolders() = runTest(testDispatcher) {
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertNotNull(state.folder)
        assertEquals("Biology", state.folder?.name)
        assertTrue("Subfolders should not be empty", state.subfolders.isNotEmpty())
        assertNull("Selected subfolder should initially be null (All)", state.selectedSubfolderId)
    }

    @Test
    fun selectSubfolder_updatesSelectedId() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.selectSubfolder(1L)
        assertEquals(1L, viewModel.uiState.value.selectedSubfolderId)

        viewModel.selectSubfolder(null)
        assertNull(viewModel.uiState.value.selectedSubfolderId)
    }

    @Test
    fun createSubfolder_addsNewSubfolder() = runTest(testDispatcher) {
        advanceUntilIdle()
        val initialCount = viewModel.uiState.value.subfolders.size

        viewModel.createSubfolder("Enzymes & Metabolism")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(initialCount + 1, state.subfolders.size)
        assertTrue(state.subfolders.any { it.name == "Enzymes & Metabolism" })
        assertNotNull(state.userMessage)
    }

    @Test
    fun renameFolder_updatesFolderName() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.renameFolder("Cellular & Molecular Biology")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Cellular & Molecular Biology", state.folder?.name)
        assertEquals("Renamed to \"Cellular & Molecular Biology\"", state.userMessage)
    }

    @Test
    fun renameSubfolder_updatesSubfolderName() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.renameSubfolder(1L, "Cell Architecture")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.subfolders.any { it.id == 1L && it.name == "Cell Architecture" })
    }

    @Test
    fun updateFolderColor_updatesTagColor() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.updateFolderColor(TagColor.VIOLET.hex)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(TagColor.VIOLET.hex, state.folder?.colorLabel)
    }

    @Test
    fun saveCapturedBatch_addsPhotosToFolder() = runTest(testDispatcher) {
        advanceUntilIdle()
        val initialPhotoCount = viewModel.uiState.value.photos.size

        val batch = listOf(
            Photo(
                id = 101L,
                fileUri = "sample://batch_a.jpg",
                folderId = 1L,
                createdAt = System.currentTimeMillis(),
                addedAt = System.currentTimeMillis(),
                caption = "Batch Photo A",
                ocrText = "Ribosomes translation peptide bond synthesis",
                source = PhotoSource.CAMERA
            ),
            Photo(
                id = 102L,
                fileUri = "sample://batch_b.jpg",
                folderId = 1L,
                createdAt = System.currentTimeMillis(),
                addedAt = System.currentTimeMillis(),
                caption = "Batch Photo B",
                ocrText = "Endoplasmic reticulum Golgi apparatus vesicle trafficking",
                source = PhotoSource.CAMERA
            )
        )

        viewModel.saveCapturedBatch(batch, targetSubfolderId = 1L)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(initialPhotoCount + 2, state.photos.size)
        assertTrue(state.photos.any { it.caption == "Batch Photo A" })
        assertTrue(state.photos.any { it.caption == "Batch Photo B" })
    }

    @Test
    fun batchSelectMode_togglesAndSelectsPhotos() = runTest(testDispatcher) {
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isBatchSelectMode)

        viewModel.toggleBatchSelectMode()
        assertTrue(viewModel.uiState.value.isBatchSelectMode)
        assertTrue(viewModel.uiState.value.selectedPhotoIds.isEmpty())

        viewModel.togglePhotoSelection(1L)
        assertTrue(viewModel.uiState.value.selectedPhotoIds.contains(1L))

        viewModel.togglePhotoSelection(1L)
        assertFalse(viewModel.uiState.value.selectedPhotoIds.contains(1L))

        viewModel.toggleBatchSelectMode()
        assertFalse(viewModel.uiState.value.isBatchSelectMode)
    }

    @Test
    fun deleteFolder_callsRepositoryAndCallback() = runTest(testDispatcher) {
        advanceUntilIdle()
        var onDeletedCalled = false
        viewModel.deleteFolder { onDeletedCalled = true }
        advanceUntilIdle()

        assertTrue(onDeletedCalled)
    }

    @Test
    fun subfolderMultiSelect_startToggleExitAndDelete() = runTest(testDispatcher) {
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isSubfolderMultiSelectMode)
        assertTrue(viewModel.uiState.value.selectedSubfolderIds.isEmpty())

        // Start multi-select with subfolder 1
        viewModel.startSubfolderMultiSelect(1L)
        assertTrue(viewModel.uiState.value.isSubfolderMultiSelectMode)
        assertEquals(setOf(1L), viewModel.uiState.value.selectedSubfolderIds)

        // Toggle subfolder 2 on
        viewModel.toggleSubfolderSelection(2L)
        assertEquals(setOf(1L, 2L), viewModel.uiState.value.selectedSubfolderIds)

        // Toggle subfolder 1 off
        viewModel.toggleSubfolderSelection(1L)
        assertEquals(setOf(2L), viewModel.uiState.value.selectedSubfolderIds)

        // Delete selected subfolders
        viewModel.deleteSelectedSubfolders()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSubfolderMultiSelectMode)
        assertTrue(viewModel.uiState.value.selectedSubfolderIds.isEmpty())
        assertFalse("Subfolder 2 should be deleted", viewModel.uiState.value.subfolders.any { it.id == 2L })
        assertTrue("Subfolder 1 should remain", viewModel.uiState.value.subfolders.any { it.id == 1L })
    }

    @Test
    fun mutualExclusion_subfolderAndPhotoMultiSelect() = runTest(testDispatcher) {
        advanceUntilIdle()

        // 1. Activate photo multi-select
        viewModel.startBatchSelection(1L)
        assertTrue(viewModel.uiState.value.isBatchSelectMode)
        assertFalse(viewModel.uiState.value.isSubfolderMultiSelectMode)

        // 2. Activating subfolder multi-select MUST dismiss photo multi-select
        viewModel.startSubfolderMultiSelect(1L)
        assertTrue(viewModel.uiState.value.isSubfolderMultiSelectMode)
        assertFalse(viewModel.uiState.value.isBatchSelectMode)
        assertTrue(viewModel.uiState.value.selectedPhotoIds.isEmpty())

        // 3. Activating photo multi-select MUST dismiss subfolder multi-select
        viewModel.startBatchSelection(2L)
        assertTrue(viewModel.uiState.value.isBatchSelectMode)
        assertFalse(viewModel.uiState.value.isSubfolderMultiSelectMode)
        assertTrue(viewModel.uiState.value.selectedSubfolderIds.isEmpty())

        // 4. Activating subfolder multi-select again, then toggleBatchSelectMode
        viewModel.startSubfolderMultiSelect(1L)
        assertTrue(viewModel.uiState.value.isSubfolderMultiSelectMode)
        viewModel.toggleBatchSelectMode() // toggles photo batch mode on
        assertTrue(viewModel.uiState.value.isBatchSelectMode)
        assertFalse(viewModel.uiState.value.isSubfolderMultiSelectMode)
        assertTrue(viewModel.uiState.value.selectedSubfolderIds.isEmpty())
    }

    @Test
    fun photoBatchActions_deleteMoveAndTagColor() = runTest(testDispatcher) {
        advanceUntilIdle()

        // Test selectAllPhotos
        viewModel.toggleBatchSelectMode()
        viewModel.selectAllPhotos()
        val allIds = viewModel.uiState.value.photos.map { it.id }.toSet()
        assertEquals(allIds, viewModel.uiState.value.selectedPhotoIds)

        // Test updateSelectedPhotosTagColor
        viewModel.updateSelectedPhotosTagColor(TagColor.CRIMSON.hex)
        advanceUntilIdle()
        assertFalse("Batch mode should exit after color assignment", viewModel.uiState.value.isBatchSelectMode)
        assertTrue(viewModel.uiState.value.photos.all { it.tagColor == TagColor.CRIMSON.hex })

        // Test moveSelectedPhotos
        viewModel.startBatchSelection(1L)
        viewModel.moveSelectedPhotos(targetFolderId = 2L, targetSubfolderId = 3L)
        advanceUntilIdle()
        assertFalse("Batch mode should exit after move", viewModel.uiState.value.isBatchSelectMode)
        assertFalse("Photo 1 should no longer be in folder 1", viewModel.uiState.value.photos.any { it.id == 1L })

        // Test deleteSelectedPhotos (Bulk Delete to Trash)
        val remainingPhotoId = viewModel.uiState.value.photos.first().id
        viewModel.startBatchSelection(remainingPhotoId)
        viewModel.deleteSelectedPhotos()
        advanceUntilIdle()
        assertFalse("Batch mode should exit after delete", viewModel.uiState.value.isBatchSelectMode)
        assertFalse("Photo should be removed from folder photos", viewModel.uiState.value.photos.any { it.id == remainingPhotoId })
    }

    @Test
    fun searchNavigation_initialSubfolderAndTargetPhotoHighlight() = runTest(testDispatcher) {
        val searchVm = FolderDetailViewModel(
            folderId = 1L,
            folderRepository = folderRepository,
            photoRepository = photoRepository,
            initialSubfolderId = 2L,
            targetPhotoId = 2L
        )
        advanceUntilIdle()

        val state = searchVm.uiState.value
        assertEquals(2L, state.selectedSubfolderId)
        assertEquals(2L, state.highlightedPhotoId)
        assertEquals(1, state.photos.size)
        assertEquals(2L, state.photos.first().id)

        // Test clearHighlightedPhoto
        searchVm.clearHighlightedPhoto()
        assertNull(searchVm.uiState.value.highlightedPhotoId)
    }

    @Test
    fun rotatePhoto_callsRepositoryAndReturnsUpdatedPhoto() = runTest(testDispatcher) {
        advanceUntilIdle()
        var updated: Photo? = null
        viewModel.rotatePhoto(1L) {
            updated = it
        }
        advanceUntilIdle()
        assertNotNull(updated)
        assertEquals(1L, updated?.id)
        assertEquals("Photo rotated 90°", viewModel.uiState.value.userMessage)
    }

    @Test
    fun cropPhoto_callsRepositoryAndReturnsUpdatedPhoto() = runTest(testDispatcher) {
        advanceUntilIdle()
        var updated: Photo? = null
        viewModel.cropPhoto(1L, 0.1f, 0.1f, 0.9f, 0.9f) {
            updated = it
        }
        advanceUntilIdle()
        assertNotNull(updated)
        assertEquals(1L, updated?.id)
        assertEquals("Photo re-cropped and text re-indexed", viewModel.uiState.value.userMessage)
    }

    @Test
    fun subfolderMultiSelect_andBulkDelete_andMutualExclusion() = runTest(testDispatcher) {
        advanceUntilIdle()

        // 1. Initial state
        assertFalse(viewModel.uiState.value.isSubfolderMultiSelectMode)
        assertTrue(viewModel.uiState.value.selectedSubfolderIds.isEmpty())

        // 2. Start subfolder multi-select (e.g. from context menu "Select")
        viewModel.startSubfolderMultiSelect(1L)
        assertTrue("Subfolder multi-select should be active", viewModel.uiState.value.isSubfolderMultiSelectMode)
        assertEquals(setOf(1L), viewModel.uiState.value.selectedSubfolderIds)

        // 3. Toggle additional subfolder selection
        viewModel.toggleSubfolderSelection(2L)
        assertEquals(setOf(1L, 2L), viewModel.uiState.value.selectedSubfolderIds)

        // 4. Test mutual exclusion: Entering photo batch mode must dismiss subfolder multi-select
        viewModel.startBatchSelection(1L)
        assertTrue("Photo batch mode should be active", viewModel.uiState.value.isBatchSelectMode)
        assertFalse("Subfolder multi-select mode must be cleared", viewModel.uiState.value.isSubfolderMultiSelectMode)
        assertTrue("Selected subfolders must be cleared", viewModel.uiState.value.selectedSubfolderIds.isEmpty())

        // 5. Test mutual exclusion reverse: Starting subfolder multi-select must dismiss photo batch mode
        viewModel.startSubfolderMultiSelect(1L)
        assertTrue("Subfolder multi-select should be active", viewModel.uiState.value.isSubfolderMultiSelectMode)
        assertFalse("Photo batch mode must be cleared", viewModel.uiState.value.isBatchSelectMode)
        assertTrue("Selected photos must be cleared", viewModel.uiState.value.selectedPhotoIds.isEmpty())

        // 6. Test exit subfolder multi-select
        viewModel.exitSubfolderMultiSelect()
        assertFalse("Subfolder multi-select mode should be inactive", viewModel.uiState.value.isSubfolderMultiSelectMode)
        assertTrue(viewModel.uiState.value.selectedSubfolderIds.isEmpty())

        // 7. Test single deleteSubfolder
        viewModel.deleteSubfolder(1L)
        advanceUntilIdle()
        assertFalse("Subfolder 1 should be removed", viewModel.uiState.value.subfolders.any { it.id == 1L })
        assertEquals("Subfolder moved to Trash", viewModel.uiState.value.userMessage)

        // 8. Test bulk deleteSelectedSubfolders
        viewModel.startSubfolderMultiSelect(2L)
        viewModel.deleteSelectedSubfolders()
        advanceUntilIdle()
        assertFalse("Subfolder multi-select should exit after delete", viewModel.uiState.value.isSubfolderMultiSelectMode)
        assertFalse("Subfolder 2 should be removed", viewModel.uiState.value.subfolders.any { it.id == 2L })
    }

    @Test
    fun createGroup_withSelectedPhotos_createsPhotoGroupAndUpdatesGridItems() = runTest(testDispatcher) {
        advanceUntilIdle()

        // Start batch selection with photo 1 and add photo 2
        viewModel.startBatchSelection(1L)
        viewModel.togglePhotoSelection(2L)
        assertEquals(2, viewModel.uiState.value.selectedPhotoIds.size)
        assertEquals(2, viewModel.uiState.value.totalSelectionCount)

        // Create group
        viewModel.createGroupFromSelected("Latihan 1.5")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse("Batch select mode should exit after creating group", state.isBatchSelectMode)
        assertTrue("Selected photos should be cleared", state.selectedPhotoIds.isEmpty())

        // Check group exists in state
        assertEquals(1, state.groups.size)
        val group = state.groups.first()
        assertEquals("Latihan 1.5", group.name)

        // Check gridItems contains Group instead of individual standalone cells
        val groupGridItem = state.gridItems.filterIsInstance<FolderGridItem.Group>().firstOrNull()
        assertNotNull("Grid items must contain group", groupGridItem)
        assertEquals("Latihan 1.5", groupGridItem?.group?.name)
        assertEquals(2, groupGridItem?.memberPhotos?.size)

        // Verify member photos no longer render as standalone
        val standaloneIds = state.gridItems.filterIsInstance<FolderGridItem.StandalonePhoto>().map { it.photo.id }
        assertFalse("Photo 1 should not be standalone", standaloneIds.contains(1L))
        assertFalse("Photo 2 should not be standalone", standaloneIds.contains(2L))
    }

    @Test
    fun renameGroup_updatesGroupName() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.startBatchSelection(1L)
        viewModel.togglePhotoSelection(2L)
        viewModel.createGroupFromSelected("Latihan 1.5")
        advanceUntilIdle()

        val groupId = viewModel.uiState.value.groups.first().id
        viewModel.renameGroup(groupId, "Tugas 2.0")
        advanceUntilIdle()

        val updatedGroup = viewModel.uiState.value.groups.first()
        assertEquals("Tugas 2.0", updatedGroup.name)
        assertEquals("Group renamed", viewModel.uiState.value.userMessage)
    }

    @Test
    fun ungroup_restoresMemberPhotosToStandalone() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.startBatchSelection(1L)
        viewModel.togglePhotoSelection(2L)
        viewModel.createGroupFromSelected("Latihan 1.5")
        advanceUntilIdle()

        val groupId = viewModel.uiState.value.groups.first().id
        viewModel.ungroup(groupId)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Groups should be empty after ungrouping", state.groups.isEmpty())
        val standaloneIds = state.gridItems.filterIsInstance<FolderGridItem.StandalonePhoto>().map { it.photo.id }
        assertTrue("Photo 1 should be restored to standalone", standaloneIds.contains(1L))
        assertTrue("Photo 2 should be restored to standalone", standaloneIds.contains(2L))
        assertEquals("Group dissolved", state.userMessage)
    }

    @Test
    fun removePhotoFromGroup_autoDissolvesWhenOneMemberRemains() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.startBatchSelection(1L)
        viewModel.togglePhotoSelection(2L)
        viewModel.createGroupFromSelected("Latihan 1.5")
        advanceUntilIdle()

        // Remove photo 1 from group -> only photo 2 remains -> group should auto-dissolve
        viewModel.removePhotoFromGroup(1L)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Group must auto-dissolve when dropping to 1 member", state.groups.isEmpty())
        val standaloneIds = state.gridItems.filterIsInstance<FolderGridItem.StandalonePhoto>().map { it.photo.id }
        assertTrue("Photo 1 must be standalone", standaloneIds.contains(1L))
        assertTrue("Photo 2 must be standalone", standaloneIds.contains(2L))
    }

    @Test
    fun addPhotosToGroup_addsMemberPhotoToExistingGroup() = runTest(testDispatcher) {
        advanceUntilIdle()
        // Add another photo in folder 1
        val newPhotoId = photoRepository.addPhoto(
            Photo(
                id = 0L,
                fileUri = "file:///data/photo4.jpg",
                thumbnailUri = "file:///data/photo4_thumb.jpg",
                folderId = 1L,
                subfolderId = 1L,
                caption = "Cell wall"
            )
        )
        advanceUntilIdle()

        viewModel.startBatchSelection(1L)
        viewModel.togglePhotoSelection(2L)
        viewModel.createGroupFromSelected("Latihan 1.5")
        advanceUntilIdle()

        val groupId = viewModel.uiState.value.groups.first().id

        // Add photo to the group
        viewModel.addPhotosToGroup(groupId, listOf(newPhotoId))
        advanceUntilIdle()

        val groupItem = viewModel.uiState.value.gridItems.filterIsInstance<FolderGridItem.Group>().firstOrNull()
        assertNotNull(groupItem)
        assertEquals(3, groupItem?.memberPhotos?.size)
        assertTrue(groupItem?.memberPhotos?.any { it.id == newPhotoId } == true)
    }

    @Test
    fun groupMultiSelect_mixedSelection_deleteMovesBothToTrash() = runTest(testDispatcher) {
        advanceUntilIdle()
        // Add photo in folder 1
        val newPhotoId = photoRepository.addPhoto(
            Photo(
                id = 0L,
                fileUri = "file:///data/photo5.jpg",
                thumbnailUri = "file:///data/photo5_thumb.jpg",
                folderId = 1L,
                subfolderId = 1L,
                caption = "Plant cell chloroplasts"
            )
        )
        advanceUntilIdle()

        viewModel.startBatchSelection(1L)
        viewModel.togglePhotoSelection(2L)
        viewModel.createGroupFromSelected("Latihan 1.5")
        advanceUntilIdle()

        val groupId = viewModel.uiState.value.groups.first().id

        // Start batch selection with group cell
        viewModel.startBatchSelectionWithGroup(groupId)
        assertTrue(viewModel.uiState.value.isBatchSelectMode)
        assertEquals(1, viewModel.uiState.value.selectedGroupIds.size)

        // Select standalone photo
        viewModel.togglePhotoSelection(newPhotoId)
        assertEquals(1, viewModel.uiState.value.selectedPhotoIds.size)
        assertEquals(2, viewModel.uiState.value.totalSelectionCount)

        // Bulk delete both
        viewModel.deleteSelectedPhotos()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse("Batch select mode should exit after delete", state.isBatchSelectMode)
        assertTrue("Groups should be empty after deletion", state.groups.isEmpty())
        assertFalse("Photo should be removed from gridItems", state.gridItems.filterIsInstance<FolderGridItem.StandalonePhoto>().any { it.photo.id == newPhotoId })
    }

    @Test
    fun gridDensity_observesSettingsFlowUpdatesInRealtime() = runTest(testDispatcher) {
        val settingsRepo = FakeSettingsRepository()
        val vm = FolderDetailViewModel(
            folderId = 1L,
            folderRepository = folderRepository,
            photoRepository = photoRepository,
            settingsRepository = settingsRepo
        )
        advanceUntilIdle()
        assertEquals(3, vm.uiState.value.gridDensity)

        // User changes to 4 columns in Settings
        settingsRepo.updateGridDensity(4)
        advanceUntilIdle()
        assertEquals(4, vm.uiState.value.gridDensity)

        // User changes to 2 columns in Settings
        settingsRepo.updateGridDensity(2)
        advanceUntilIdle()
        assertEquals(2, vm.uiState.value.gridDensity)
    }

    @Test
    fun subfolderManagement_renameDeleteAndMultiSelect() = runTest(testDispatcher) {
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.subfolders.size)

        // 1. Rename subfolder
        viewModel.renameSubfolder(1L, "Cell Biology Renamed")
        advanceUntilIdle()
        val renamedSub = viewModel.uiState.value.subfolders.firstOrNull { it.id == 1L }
        assertNotNull(renamedSub)
        assertEquals("Cell Biology Renamed", renamedSub?.name)

        // 2. Subfolder multi-select flow
        viewModel.startSubfolderMultiSelect(1L)
        assertTrue(viewModel.uiState.value.isSubfolderMultiSelectMode)
        assertEquals(setOf(1L), viewModel.uiState.value.selectedSubfolderIds)

        viewModel.toggleSubfolderSelection(2L)
        assertEquals(setOf(1L, 2L), viewModel.uiState.value.selectedSubfolderIds)

        // 3. Bulk delete selected subfolders
        viewModel.deleteSelectedSubfolders()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isSubfolderMultiSelectMode)
        assertTrue(viewModel.uiState.value.selectedSubfolderIds.isEmpty())
        assertTrue(viewModel.uiState.value.subfolders.none { it.id in listOf(1L, 2L) })
    }
}

