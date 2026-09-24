// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui

import com.arinara.fotara.test.FakeFolderRepository
import com.arinara.fotara.test.FakeSettingsRepository
import com.arinara.fotara.ui.onboarding.OnboardingStep
import com.arinara.fotara.ui.onboarding.OnboardingViewModel
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeFolderRepo: FakeFolderRepository
    private lateinit var fakeSettingsRepo: FakeSettingsRepository
    private lateinit var viewModel: OnboardingViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeFolderRepo = FakeFolderRepository(initialFolders = emptyList(), initialSubfolders = emptyList())
        fakeSettingsRepo = FakeSettingsRepository()
        viewModel = OnboardingViewModel(fakeFolderRepo, fakeSettingsRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_isPermissionsStep() = runTest(testDispatcher) {
        val state = viewModel.uiState.value
        assertEquals(OnboardingStep.PERMISSIONS, state.currentStep)
        assertFalse(state.isCameraPermissionGranted)
        assertFalse(state.isStoragePermissionGranted)
        assertFalse(state.isCreatingFolders)
        assertEquals(4, state.starterFolders.size)
    }

    @Test
    fun updatePermissions_updatesUiState() = runTest(testDispatcher) {
        viewModel.updatePermissions(cameraGranted = true, storageGranted = true)
        val state = viewModel.uiState.value
        assertTrue(state.isCameraPermissionGranted)
        assertTrue(state.isStoragePermissionGranted)
    }

    @Test
    fun proceedToStarterFolders_transitionsToNextStep() = runTest(testDispatcher) {
        viewModel.proceedToStarterFolders()
        assertEquals(OnboardingStep.STARTER_FOLDERS, viewModel.uiState.value.currentStep)
    }

    @Test
    fun createDefaultFolders_createsAllStarterFoldersAndCompletes() = runTest(testDispatcher) {
        var completedCalled = false

        viewModel.createDefaultFolders {
            completedCalled = true
        }
        advanceUntilIdle()

        assertTrue(completedCalled)
        assertTrue(fakeSettingsRepo.isOnboardingCompleted())

        val folders = fakeFolderRepo.getFolders().first()
        assertEquals(4, folders.size)
        val folderNames = folders.map { it.name }
        assertTrue(folderNames.contains("Math"))
        assertTrue(folderNames.contains("Science"))
        assertTrue(folderNames.contains("History"))
        assertTrue(folderNames.contains("Literature"))
    }

    @Test
    fun startEmpty_completesWithoutCreatingFolders() = runTest(testDispatcher) {
        var completedCalled = false

        viewModel.startEmpty {
            completedCalled = true
        }
        advanceUntilIdle()

        assertTrue(completedCalled)
        assertTrue(fakeSettingsRepo.isOnboardingCompleted())

        val folders = fakeFolderRepo.getFolders().first()
        assertEquals(0, folders.size)
    }
}
