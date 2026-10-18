// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ocr

import com.arinara.fotara.data.model.Folder
import com.arinara.fotara.data.model.Subfolder
import com.arinara.fotara.data.model.TagColor
import com.arinara.fotara.test.FakeFolderRepository
import com.arinara.fotara.test.FakePhotoRepository
import com.arinara.fotara.ui.folder.FolderDetailViewModel
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
class OcrAndSuggestEngineTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var ocrEngine: MlKitOcrEngine
    private lateinit var folderSuggestEngine: DefaultFolderSuggestEngine

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        ocrEngine = MlKitOcrEngine()
        folderSuggestEngine = DefaultFolderSuggestEngine()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun extractText_identifiesBiologyKeywordsAndSubjectHint() = runTest(testDispatcher) {
        val ocrResult = ocrEngine.extractText(
            imageUri = "sample://photos/bio_cells.jpg",
            rawContentHint = "Mitochondria ATP synthesis and cellular respiration cristae matrix"
        )
        assertNotNull(ocrResult)
        assertTrue(ocrResult.keywords.contains("mitochondria"))
        assertTrue(ocrResult.keywords.contains("atp"))
        assertEquals("Biology", ocrResult.detectedSubjectHint)
    }

    @Test
    fun extractText_identifiesCalculusKeywordsAndSubjectHint() = runTest(testDispatcher) {
        val ocrResult = ocrEngine.extractText(
            imageUri = "sample://photos/calc.jpg",
            rawContentHint = "Definite integral from 0 to 1 of x squared dx equals one third"
        )
        assertNotNull(ocrResult)
        assertTrue(ocrResult.keywords.contains("integral"))
        assertEquals("Calculus", ocrResult.detectedSubjectHint)
    }

    @Test
    fun folderSuggestEngine_suggestsMatchingFolder() = runTest(testDispatcher) {
        val folders = listOf(
            Folder(id = 1L, name = "Biology", colorLabel = TagColor.EMERALD.hex),
            Folder(id = 2L, name = "Calculus", colorLabel = TagColor.SKY.hex)
        )
        val subfolders = mapOf(
            1L to listOf(Subfolder(id = 10L, folderId = 1L, name = "Cell Structure")),
            2L to listOf(Subfolder(id = 20L, folderId = 2L, name = "Integrals"))
        )

        val ocrResult = ocrEngine.extractText(
            imageUri = "sample://doc.jpg",
            rawContentHint = "Cell membrane organelle mitochondria cellular structure"
        )
        val suggestion = folderSuggestEngine.suggestFolder(ocrResult, folders, subfolders)

        assertNotNull(suggestion)
        assertEquals(1L, suggestion?.folderId)
        assertEquals("Biology", suggestion?.folderName)
        assertEquals(10L, suggestion?.suggestedSubfolderId)
    }

    @Test
    fun photoQuickActions_moveColorAndDeadline() = runTest(testDispatcher) {
        val folderRepo = FakeFolderRepository()
        val photoRepo = FakePhotoRepository(folderRepo)
        val viewModel = FolderDetailViewModel(1L, folderRepo, photoRepo)

        advanceUntilIdle()
        val targetPhoto = viewModel.uiState.value.photos.first()
        val photoId = targetPhoto.id

        // Move subfolder
        viewModel.movePhotoToSubfolder(photoId, 3L)
        advanceUntilIdle()
        assertEquals(3L, viewModel.uiState.value.photos.first { it.id == photoId }.subfolderId)

        // Change color
        viewModel.updatePhotoTagColor(photoId, TagColor.CRIMSON.hex)
        advanceUntilIdle()
        assertEquals(TagColor.CRIMSON.hex, viewModel.uiState.value.photos.first { it.id == photoId }.tagColor)

        // Set deadline
        val deadline = System.currentTimeMillis() + 86400000L
        viewModel.setPhotoDeadline(photoId, deadline)
        advanceUntilIdle()
        assertEquals(deadline, viewModel.uiState.value.photos.first { it.id == photoId }.linkedDeadline)

        // Remove deadline
        viewModel.setPhotoDeadline(photoId, null)
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.photos.first { it.id == photoId }.linkedDeadline)
    }
}
