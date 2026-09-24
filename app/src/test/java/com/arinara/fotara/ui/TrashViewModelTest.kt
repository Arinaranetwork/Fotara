// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui

import com.arinara.fotara.data.model.Folder
import com.arinara.fotara.data.model.Photo
import com.arinara.fotara.test.FakeFolderRepository
import com.arinara.fotara.test.FakePhotoRepository
import com.arinara.fotara.ui.trash.TrashTab
import com.arinara.fotara.ui.trash.TrashViewModel
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
class TrashViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var folderRepository: FakeFolderRepository
    private lateinit var photoRepository: FakePhotoRepository
    private lateinit var viewModel: TrashViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        folderRepository = FakeFolderRepository()
        photoRepository = FakePhotoRepository(folderRepository)
        viewModel = TrashViewModel(folderRepository, photoRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialLoad_loadsTrashedFoldersAndPhotos() = runTest(testDispatcher) {
        advanceUntilIdle()

        // Trash folder 2 and photo 3
        folderRepository.deleteFolders(listOf(2L))
        photoRepository.deletePhotos(listOf(3L))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.trashedFolders.size)
        assertEquals(2L, state.trashedFolders.first().id)
        assertEquals(1, state.trashedPhotos.size)
        assertEquals(3L, state.trashedPhotos.first().id)
    }

    @Test
    fun selectTab_updatesSelectedTab() = runTest(testDispatcher) {
        assertEquals(TrashTab.ALL, viewModel.uiState.value.selectedTab)
        viewModel.selectTab(TrashTab.FOLDERS)
        assertEquals(TrashTab.FOLDERS, viewModel.uiState.value.selectedTab)
        viewModel.selectTab(TrashTab.PHOTOS)
        assertEquals(TrashTab.PHOTOS, viewModel.uiState.value.selectedTab)
    }

    @Test
    fun restoreFolder_movesFolderOutOfTrash() = runTest(testDispatcher) {
        folderRepository.deleteFolders(listOf(1L))
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.trashedFolders.size)

        val folder = viewModel.uiState.value.trashedFolders.first()
        viewModel.requestRestoreFolder(folder)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.trashedFolders.isEmpty())
        assertTrue(viewModel.uiState.value.activeFolders.any { it.id == 1L })
        assertEquals("Folder \"Biology\" restored", viewModel.uiState.value.userMessage)
    }

    @Test
    fun restorePhoto_nonOrphan_restoresDirectly() = runTest(testDispatcher) {
        // Photo 1 belongs to active folder 1
        photoRepository.deletePhotos(listOf(1L))
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.trashedPhotos.size)

        val photo = viewModel.uiState.value.trashedPhotos.first()
        viewModel.requestRestorePhoto(photo)
        advanceUntilIdle()

        assertTrue("Photo should be restored directly without orphan prompt", viewModel.uiState.value.trashedPhotos.isEmpty())
        assertNull(viewModel.uiState.value.orphanPhotoToRestore)
        assertEquals("Note restored to original folder", viewModel.uiState.value.userMessage)
    }

    @Test
    fun restorePhoto_orphan_promptsResolutionAndRestoresBoth() = runTest(testDispatcher) {
        // Trash both parent folder 1 and photo 1
        folderRepository.deleteFolders(listOf(1L))
        photoRepository.deletePhotos(listOf(1L))
        advanceUntilIdle()

        val photo = viewModel.uiState.value.trashedPhotos.first()
        viewModel.requestRestorePhoto(photo)
        advanceUntilIdle()

        // Should prompt orphan dialog
        assertNotNull(viewModel.uiState.value.orphanPhotoToRestore)
        assertEquals(1L, viewModel.uiState.value.orphanPhotoToRestore?.id)

        // Confirm restore both
        viewModel.confirmRestoreBoth(photo, 1L)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.orphanPhotoToRestore)
        assertTrue(viewModel.uiState.value.trashedPhotos.isEmpty())
        assertTrue(viewModel.uiState.value.trashedFolders.isEmpty())
        assertEquals("Folder and note restored", viewModel.uiState.value.userMessage)
    }

    @Test
    fun restorePhoto_toActiveFolder_restoresToTarget() = runTest(testDispatcher) {
        // Trash parent folder 1 and photo 1
        folderRepository.deleteFolders(listOf(1L))
        photoRepository.deletePhotos(listOf(1L))
        advanceUntilIdle()

        val photo = viewModel.uiState.value.trashedPhotos.first()
        viewModel.requestRestorePhoto(photo)
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.orphanPhotoToRestore)

        // Restore photo 1 to active folder 2 (Mathematics)
        viewModel.confirmRestorePhotoToActiveFolder(photo, 2L)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.orphanPhotoToRestore)
        assertTrue(viewModel.uiState.value.trashedPhotos.isEmpty())
        val restoredPhoto = photoRepository.getPhotoById(1L)
        assertNotNull(restoredPhoto)
        assertEquals(2L, restoredPhoto?.folderId)
        assertEquals("Note restored to selected folder", viewModel.uiState.value.userMessage)
    }

    @Test
    fun purgePermanently_removesItemsFromTrash() = runTest(testDispatcher) {
        folderRepository.deleteFolders(listOf(2L))
        photoRepository.deletePhotos(listOf(3L))
        advanceUntilIdle()

        val photo = viewModel.uiState.value.trashedPhotos.first()
        viewModel.requestDeletePhotoPermanently(photo)
        viewModel.confirmDeletePhotoPermanently()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.trashedPhotos.isEmpty())
        assertNull(photoRepository.getPhotoById(3L))

        val folder = viewModel.uiState.value.trashedFolders.first()
        viewModel.requestDeleteFolderPermanently(folder)
        viewModel.confirmDeleteFolderPermanently()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.trashedFolders.isEmpty())
    }

    @Test
    fun emptyTrash_purgesAllTrashedItems() = runTest(testDispatcher) {
        folderRepository.deleteFolders(listOf(2L))
        photoRepository.deletePhotos(listOf(1L, 2L))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.trashedFolders.isEmpty())
        assertFalse(viewModel.uiState.value.trashedPhotos.isEmpty())

        viewModel.requestEmptyTrash()
        assertTrue(viewModel.uiState.value.showEmptyTrashDialog)

        viewModel.confirmEmptyTrash()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showEmptyTrashDialog)
        assertTrue(viewModel.uiState.value.trashedPhotos.isEmpty())
        assertEquals("Trash emptied", viewModel.uiState.value.userMessage)
    }
}
