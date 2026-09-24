// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui

import com.arinara.fotara.data.model.DownsampleQuality
import com.arinara.fotara.data.model.ImportResult
import com.arinara.fotara.data.model.SortOrder
import com.arinara.fotara.data.model.StorageBreakdown
import com.arinara.fotara.data.model.StorageLocation
import com.arinara.fotara.data.model.ThemeMode
import com.arinara.fotara.data.model.UserSettings
import com.arinara.fotara.data.repository.SettingsRepository
import com.arinara.fotara.ui.settings.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
import com.arinara.fotara.test.FakeSettingsRepository


@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepo: FakeSettingsRepository
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeSettingsRepository()
        viewModel = SettingsViewModel(fakeRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_loadsSettingsAndStorageBreakdown() = runTest(testDispatcher) {
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals(SortOrder.UPLOAD_DATE, state.userSettings.defaultSortOrder)
        assertEquals(3, state.userSettings.gridDensity)
        assertEquals(ThemeMode.SYSTEM, state.userSettings.themeMode)
        assertTrue(state.userSettings.autoOcrEnabled)
        assertEquals(1048576L, state.storageBreakdown.photosSizeBytes)
    }

    @Test
    fun updateDisplaySettings_updatesStateCorrectly() = runTest(testDispatcher) {
        advanceUntilIdle()

        viewModel.updateSortOrder(SortOrder.NEAREST_DEADLINE)
        viewModel.updateGridDensity(4)
        viewModel.updateThemeMode(ThemeMode.DARK)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(SortOrder.NEAREST_DEADLINE, state.userSettings.defaultSortOrder)
        assertEquals(4, state.userSettings.gridDensity)
        assertEquals(ThemeMode.DARK, state.userSettings.themeMode)
    }

    @Test
    fun updateOcrAndNotificationSettings_updatesStateCorrectly() = runTest(testDispatcher) {
        advanceUntilIdle()

        viewModel.updateAutoOcr(false)
        viewModel.updateOcrLanguage("English")
        viewModel.updateDownsampleQuality(DownsampleQuality.FAST_PROCESSING)
        viewModel.updateReminderLeadTime(3)
        viewModel.updateDueTomorrowRibbon(false)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.userSettings.autoOcrEnabled)
        assertEquals("English", state.userSettings.ocrLanguage)
        assertEquals(DownsampleQuality.FAST_PROCESSING, state.userSettings.downsampleQuality)
        assertEquals(3, state.userSettings.reminderLeadTimeHours)
        assertFalse(state.userSettings.dueTomorrowRibbonEnabled)
    }

    @Test
    fun updateStorageLocation_updatesState() = runTest(testDispatcher) {
        advanceUntilIdle()

        viewModel.updateStorageLocation(StorageLocation.EXTERNAL)
        advanceUntilIdle()

        assertEquals(StorageLocation.EXTERNAL, viewModel.uiState.value.userSettings.storageLocation)
    }

    @Test
    fun rebuildThumbnails_triggersSuccessMessage() = runTest(testDispatcher) {
        advanceUntilIdle()

        viewModel.rebuildThumbnails()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isRebuildingThumbnails)
        assertNotNull(state.feedbackMessage)
        assertTrue(state.feedbackMessage!!.contains("Thumbnails rebuilt successfully"))
    }

    @Test
    fun rebuildSearchIndex_triggersSuccessMessage() = runTest(testDispatcher) {
        advanceUntilIdle()

        viewModel.rebuildSearchIndex()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isRebuildingSearchIndex)
        assertNotNull(state.feedbackMessage)
        assertTrue(state.feedbackMessage!!.contains("Search index rebuilt successfully"))
    }

    @Test
    fun exportBackup_showsExportDialogWithJson() = runTest(testDispatcher) {
        advanceUntilIdle()

        viewModel.requestExportBackup()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.showExportDialog)
        assertEquals("{\"app\":\"Fotara\",\"folders\":[],\"photos\":[]}", state.exportedJsonString)

        viewModel.dismissExportDialog()
        assertFalse(viewModel.uiState.value.showExportDialog)
        assertNull(viewModel.uiState.value.exportedJsonString)
    }

    @Test
    fun importBackup_executesImportAndShowsMessage() = runTest(testDispatcher) {
        advanceUntilIdle()

        viewModel.requestImportBackup()
        assertTrue(viewModel.uiState.value.showImportDialog)

        viewModel.executeImportBackup("{\"app\":\"Fotara\"}")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.showImportDialog)
        assertEquals("Import success", state.feedbackMessage)
    }

    @Test
    fun licensesDialog_togglesVisibility() = runTest(testDispatcher) {
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showLicensesDialog)
        viewModel.setLicensesDialogVisible(true)
        assertTrue(viewModel.uiState.value.showLicensesDialog)
        viewModel.setLicensesDialogVisible(false)
        assertFalse(viewModel.uiState.value.showLicensesDialog)
    }
}
