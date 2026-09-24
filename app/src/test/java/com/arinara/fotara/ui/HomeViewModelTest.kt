// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui

import com.arinara.fotara.data.model.SearchDateFilter
import com.arinara.fotara.data.model.TagColor
import com.arinara.fotara.test.FakeFolderRepository
import com.arinara.fotara.test.FakePhotoRepository
import com.arinara.fotara.test.FakeSettingsRepository
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
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var folderRepository: FakeFolderRepository
    private lateinit var photoRepository: FakePhotoRepository
    private lateinit var settingsRepository: FakeSettingsRepository
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        folderRepository = FakeFolderRepository()
        photoRepository = FakePhotoRepository(folderRepository)
        settingsRepository = FakeSettingsRepository()
        viewModel = HomeViewModel(folderRepository, photoRepository, settingsRepository = settingsRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialUiState_loadsFolders() = runTest(testDispatcher) {
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertTrue("Folders should be loaded", state.folders.isNotEmpty())
    }

    @Test
    fun dialogState_toggleControlsWork() = runTest(testDispatcher) {
        assertFalse(viewModel.uiState.value.showNewFolderDialog)
        viewModel.openNewFolderDialog()
        assertTrue(viewModel.uiState.value.showNewFolderDialog)
        viewModel.closeNewFolderDialog()
        assertFalse(viewModel.uiState.value.showNewFolderDialog)
    }

    @Test
    fun createFolder_addsFolderAndShowsMessage() = runTest(testDispatcher) {
        advanceUntilIdle()
        val initialCount = viewModel.uiState.value.folders.size

        viewModel.createFolder(
            name = "Organic Chemistry II",
            colorHex = TagColor.CRIMSON.hex,
            isPinned = false
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(initialCount + 1, state.folders.size)
        assertTrue(state.folders.any { it.name == "Organic Chemistry II" })
        assertNotNull(state.userMessage)
        assertFalse(state.showNewFolderDialog)
    }

    @Test
    fun searchOperations_activateAndFilterCorrectly() = runTest(testDispatcher) {
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isSearchActive)

        viewModel.activateSearch()
        assertTrue(viewModel.uiState.value.isSearchActive)

        viewModel.onSearchQueryChanged("Bio")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Bio", state.searchQuery)
        assertTrue("Matching folders should contain Biology", state.folderSearchResults.any { it.name == "Biology" })

        viewModel.deactivateSearch()
        assertFalse(viewModel.uiState.value.isSearchActive)
        assertEquals("", viewModel.uiState.value.searchQuery)
    }

    @Test
    fun renameFolder_updatesStateSuccessfully() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.renameFolder(1, "Advanced Molecular Biology")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.folders.any { it.id == 1L && it.name == "Advanced Molecular Biology" })
        assertEquals("Renamed to \"Advanced Molecular Biology\"", state.userMessage)
    }

    @Test
    fun onPhotoSearchResultClicked_validPhoto_triggersNavigationAndDeactivatesSearch() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.activateSearch()
        viewModel.onSearchQueryChanged("DNA")
        advanceUntilIdle()

        val photo = photoRepository.getPhotoById(2L)
        assertNotNull(photo)

        var navigatedFolderId: Long? = null
        var navigatedSubfolderId: Long? = null
        var navigatedPhotoId: Long? = null

        viewModel.onPhotoSearchResultClicked(photo!!) { fId, subId, pId ->
            navigatedFolderId = fId
            navigatedSubfolderId = subId
            navigatedPhotoId = pId
        }
        advanceUntilIdle()

        assertEquals(1L, navigatedFolderId)
        assertEquals(2L, navigatedSubfolderId)
        assertEquals(2L, navigatedPhotoId)
        assertFalse("Search should be deactivated upon navigation", viewModel.uiState.value.isSearchActive)
    }

    @Test
    fun onPhotoSearchResultClicked_trashedPhoto_showsTransientMessageAndRefreshes() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.activateSearch()
        viewModel.onSearchQueryChanged("Cell")
        advanceUntilIdle()

        // Delete photo 1
        photoRepository.deletePhoto(1L)
        advanceUntilIdle()

        val stalePhoto = com.arinara.fotara.data.model.Photo(
            id = 1L,
            fileUri = "file:///data/photo1.jpg",
            folderId = 1L,
            subfolderId = 1L
        )

        var wasNavigated = false
        viewModel.onPhotoSearchResultClicked(stalePhoto) { _, _, _ ->
            wasNavigated = true
        }
        advanceUntilIdle()

        assertFalse("Should not navigate to deleted/trashed photo", wasNavigated)
        assertEquals("Photo is no longer in this folder", viewModel.uiState.value.userMessage)
    }

    @Test
    fun searchFilters_colorAndDateFiltering() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.activateSearch()

        // Filter by color CRIMSON
        viewModel.setColorFilter(TagColor.CRIMSON.hex)
        advanceUntilIdle()
        assertEquals(TagColor.CRIMSON.hex, viewModel.uiState.value.searchColorFilter)

        // Filter by date TODAY
        viewModel.setDateFilter(SearchDateFilter.TODAY)
        advanceUntilIdle()
        assertEquals(SearchDateFilter.TODAY, viewModel.uiState.value.searchDateFilter)

        // Clear filters
        viewModel.clearFilters()
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.searchColorFilter)
        assertEquals(SearchDateFilter.ALL, viewModel.uiState.value.searchDateFilter)
    }

    @Test
    fun recentSearches_addRemoveAndClear() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.activateSearch()

        viewModel.submitSearch("Thermodynamics")
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.recentSearches.contains("Thermodynamics"))

        viewModel.submitSearch("Electromagnetism")
        advanceUntilIdle()
        assertEquals("Electromagnetism", viewModel.uiState.value.recentSearches.first())

        viewModel.removeRecentSearch("Thermodynamics")
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.recentSearches.contains("Thermodynamics"))

        viewModel.clearRecentSearches()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.recentSearches.isEmpty())
    }

    @Test
    fun folderPrivacyLock_lockAndUnlock_updatesState() = runTest(testDispatcher) {
        advanceUntilIdle()

        viewModel.lockFolder(1L, "4321")
        advanceUntilIdle()

        val folder = viewModel.uiState.value.folders.firstOrNull { it.id == 1L }
        assertNotNull(folder)
        assertTrue("Folder should be locked", folder?.isLocked == true)
        assertEquals("4321", folder?.lockPin)
        assertEquals("Folder locked with PIN", viewModel.uiState.value.userMessage)

        viewModel.updateFolderPin(1L, "8888")
        advanceUntilIdle()
        val updated = viewModel.uiState.value.folders.firstOrNull { it.id == 1L }
        assertEquals("8888", updated?.lockPin)

        viewModel.unlockFolder(1L)
        advanceUntilIdle()
        val unlocked = viewModel.uiState.value.folders.firstOrNull { it.id == 1L }
        assertNotNull(unlocked)
        assertFalse("Folder should be unlocked", unlocked?.isLocked == true)
        assertNull(unlocked?.lockPin)
        assertEquals("Folder lock removed", viewModel.uiState.value.userMessage)
    }

    @Test
    fun smartTags_loadedIntoUiStateAndFilteredCrossFolder() = runTest(testDispatcher) {
        advanceUntilIdle()

        // Add cross-folder notes with hashtags
        photoRepository.addPhoto(
            com.arinara.fotara.data.model.Photo(
                fileUri = "sample://physics1.jpg",
                folderId = 1L,
                caption = "Newton's laws #formula",
                note = "Study for #midterm"
            )
        )
        photoRepository.addPhoto(
            com.arinara.fotara.data.model.Photo(
                fileUri = "sample://calculus1.jpg",
                folderId = 2L,
                caption = "Integration by parts #formula",
                note = "Important #exam"
            )
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Available smart tags should contain formula", state.availableSmartTags.contains("formula"))

        // Activate search and select smart tag
        viewModel.activateSearch()
        viewModel.selectSmartTag("formula")
        advanceUntilIdle()

        val searchState = viewModel.uiState.value
        assertEquals("formula", searchState.selectedSmartTag)
        assertEquals(2, searchState.searchResults.size)
        assertTrue(searchState.searchResults.any { it.folderId == 1L })
        assertTrue(searchState.searchResults.any { it.folderId == 2L })

        // Toggle tag off
        viewModel.selectSmartTag("formula")
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.selectedSmartTag)
    }
}
