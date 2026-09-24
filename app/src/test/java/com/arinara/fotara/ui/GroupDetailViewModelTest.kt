// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui

import com.arinara.fotara.data.model.TagColor
import com.arinara.fotara.test.FakeFolderRepository
import com.arinara.fotara.test.FakePhotoRepository
import com.arinara.fotara.test.FakeSettingsRepository
import com.arinara.fotara.ui.group.GroupDetailViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GroupDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var folderRepository: FakeFolderRepository
    private lateinit var photoRepository: FakePhotoRepository
    private lateinit var settingsRepository: FakeSettingsRepository
    private lateinit var viewModel: GroupDetailViewModel

    @Before
    fun setup() = runTest(testDispatcher) {
        Dispatchers.setMain(testDispatcher)
        folderRepository = FakeFolderRepository()
        val p1 = com.arinara.fotara.data.model.Photo(id = 101L, fileUri = "file:///p1.jpg", folderId = 1L, addedAt = 1000L)
        val p2 = com.arinara.fotara.data.model.Photo(id = 102L, fileUri = "file:///p2.jpg", folderId = 1L, addedAt = 2000L)
        val p3 = com.arinara.fotara.data.model.Photo(id = 103L, fileUri = "file:///p3.jpg", folderId = 1L, addedAt = 3000L)
        photoRepository = FakePhotoRepository(folderRepository, listOf(p1, p2, p3))
        settingsRepository = FakeSettingsRepository()

        // Create a test group with photos 101 and 102 in folder 1
        val testGroupId = photoRepository.createGroup(1L, null, "Test Assignment", listOf(101L, 102L))

        viewModel = GroupDetailViewModel(
            groupId = testGroupId,
            folderId = 1L,
            targetPhotoId = 101L,
            photoRepository = photoRepository,
            folderRepository = folderRepository,
            settingsRepository = settingsRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialLoad_loadsGroupAndMembersSortedByAddedAt() = runTest(testDispatcher) {
        advanceUntilIdle()
        val state = viewModel.uiState.value

        assertNotNull(state.group)
        assertEquals("Test Assignment", state.group?.name)
        assertEquals(2, state.photos.size)
        assertEquals(101L, state.highlightedPhotoId)
        assertEquals(3, state.gridDensity)
    }

    @Test
    fun renameGroup_updatesGroupName() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.renameGroup("Final Answers")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Final Answers", state.group?.name)
    }

    @Test
    fun updateGroupColor_updatesTagColor() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.updateGroupColor(TagColor.CRIMSON.name)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(TagColor.CRIMSON.name, state.group?.tagColor)
    }

    @Test
    fun addPhotosToGroup_addsPhotosSuccessfully() = runTest(testDispatcher) {
        advanceUntilIdle()
        // Photo 103 is standalone in folder 1
        viewModel.addPhotosToGroup(listOf(103L))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(3, state.photos.size)
        assertTrue(state.photos.any { it.id == 103L })
    }

    @Test
    fun removePhotoFromGroup_withoutDissolve_removesMember() = runTest(testDispatcher) {
        advanceUntilIdle()
        // Add photo 103 so we have 3 members
        viewModel.addPhotosToGroup(listOf(103L))
        advanceUntilIdle()
        assertEquals(3, viewModel.uiState.value.photos.size)

        var dissolved = false
        viewModel.removePhotoFromGroup(103L) {
            dissolved = true
        }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.photos.size)
        assertEquals(false, dissolved)
    }

    @Test
    fun removePhotoFromGroup_triggersAutoDissolve_whenRemainingIsOne() = runTest(testDispatcher) {
        advanceUntilIdle()
        // Group has 2 members: 101 and 102
        var dissolved = false
        viewModel.removePhotoFromGroup(102L) {
            dissolved = true
        }
        advanceUntilIdle()

        assertTrue("Should invoke onAutoDissolved when remaining members <= 1", dissolved)
    }

    @Test
    fun ungroup_removesGroupAssociation() = runTest(testDispatcher) {
        advanceUntilIdle()
        var completed = false
        viewModel.ungroup {
            completed = true
        }
        advanceUntilIdle()

        assertTrue(completed)
    }

    @Test
    fun deleteGroup_movesToTrash() = runTest(testDispatcher) {
        advanceUntilIdle()
        var completed = false
        viewModel.deleteGroup {
            completed = true
        }
        advanceUntilIdle()

        assertTrue(completed)
    }

    @Test
    fun clearHighlight_clearsHighlightedPhotoId() = runTest(testDispatcher) {
        advanceUntilIdle()
        assertEquals(101L, viewModel.uiState.value.highlightedPhotoId)

        viewModel.clearHighlightedPhoto()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.highlightedPhotoId)
    }

    @Test
    fun gridDensity_updatesLiveFromSettings() = runTest(testDispatcher) {
        advanceUntilIdle()
        assertEquals(3, viewModel.uiState.value.gridDensity)

        settingsRepository.updateGridDensity(4)
        advanceUntilIdle()

        assertEquals(4, viewModel.uiState.value.gridDensity)
    }
}
