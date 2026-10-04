// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.notes

import com.arinara.fotara.data.model.CanvasNote
import com.arinara.fotara.data.model.DocumentNote
import com.arinara.fotara.data.model.DocumentType
import com.arinara.fotara.data.model.Folder
import com.arinara.fotara.data.model.Photo
import com.arinara.fotara.data.model.TextNote
import com.arinara.fotara.test.FakeCanvasNoteRepository
import com.arinara.fotara.test.FakeDocumentRepository
import com.arinara.fotara.test.FakeFolderRepository
import com.arinara.fotara.test.FakePhotoRepository
import com.arinara.fotara.test.FakeTextNoteRepository
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
class NotesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var folderRepo: FakeFolderRepository
    private lateinit var photoRepo: FakePhotoRepository
    private lateinit var docRepo: FakeDocumentRepository
    private lateinit var textRepo: FakeTextNoteRepository
    private lateinit var canvasRepo: FakeCanvasNoteRepository
    private lateinit var viewModel: NotesViewModel

    private val folder1 = Folder(id = 1L, name = "Kimia")
    private val folder2 = Folder(id = 2L, name = "Fisika")

    private val now = System.currentTimeMillis()
    private val yesterday = now - 24 * 60 * 60 * 1000L
    private val threeDaysAgo = now - 3 * 24 * 60 * 60 * 1000L

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        folderRepo = FakeFolderRepository(listOf(folder1, folder2))

        val initialPhotos = listOf(
            Photo(id = 10L, fileUri = "file://10.jpg", folderId = 1L, caption = "Hukum Hess", addedAt = now),
            Photo(id = 11L, fileUri = "file://11.jpg", folderId = 2L, caption = "Catatan GLBB", addedAt = yesterday),
            Photo(id = 12L, fileUri = "file://12.jpg", folderId = 1L, caption = "Trashed Photo", addedAt = now, isTrashed = true)
        )
        photoRepo = FakePhotoRepository(folderRepo, initialPhotos)

        val initialDocs = listOf(
            DocumentNote(
                id = 20L,
                folderId = 1L,
                name = "DOCX Termokimia",
                docType = DocumentType.DOCX,
                originFileUri = "content://doc.docx",
                addedAt = threeDaysAgo
            )
        )
        docRepo = FakeDocumentRepository(initialDocs)

        val initialTextNotes = listOf(
            TextNote(
                id = 30L,
                folderId = 1L,
                title = "Kisi-Kisi Kimia",
                bodyMarkdown = "Ringkasan ujian kimia",
                createdAt = yesterday,
                updatedAt = yesterday,
                addedAt = yesterday
            )
        )
        textRepo = FakeTextNoteRepository(initialTextNotes)

        val initialCanvasNotes = listOf(
            CanvasNote(
                id = 40L,
                folderId = 2L,
                title = "Diagram Vektor",
                addedAt = now
            )
        )
        canvasRepo = FakeCanvasNoteRepository(initialCanvasNotes)

        viewModel = NotesViewModel(
            photoRepository = photoRepo,
            documentRepository = docRepo,
            textNoteRepository = textRepo,
            canvasNoteRepository = canvasRepo,
            folderRepository = folderRepo
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialUiState_aggregatesAllActiveNotesExcludingTrashed() = runTest(testDispatcher) {
        advanceUntilIdle()
        val state = viewModel.uiState.value

        // 2 active photos + 1 doc + 1 text note + 1 canvas note = 5 active notes total
        assertEquals(5, state.totalItemCount)
        assertTrue(state.dateGroups.isNotEmpty())

        val allItems = state.dateGroups.flatMap { it.items }
        assertFalse("Trashed photo must not be present", allItems.any { it.title == "Trashed Photo" })
        assertTrue("Hukum Hess must be present", allItems.any { it.title == "Hukum Hess" })
        assertTrue("DOCX Termokimia must be present", allItems.any { it.title == "DOCX Termokimia" })
        assertTrue("Kisi-Kisi Kimia must be present", allItems.any { it.title == "Kisi-Kisi Kimia" })
        assertTrue("Diagram Vektor must be present", allItems.any { it.title == "Diagram Vektor" })
    }

    @Test
    fun filterChips_filtersNotesByType() = runTest(testDispatcher) {
        advanceUntilIdle()

        // 1. Photos Filter
        viewModel.setFilter(NoteFilterChip.PHOTOS)
        advanceUntilIdle()
        var state = viewModel.uiState.value
        assertEquals(2, state.totalItemCount)
        assertTrue(state.dateGroups.flatMap { it.items }.all { it.type == UnifiedNoteType.PHOTO })

        // 2. Documents Filter
        viewModel.setFilter(NoteFilterChip.DOCUMENTS)
        advanceUntilIdle()
        state = viewModel.uiState.value
        assertEquals(1, state.totalItemCount)
        assertTrue(state.dateGroups.flatMap { it.items }.all { it.type == UnifiedNoteType.DOCUMENT })

        // 3. Text Notes Filter
        viewModel.setFilter(NoteFilterChip.TEXT)
        advanceUntilIdle()
        state = viewModel.uiState.value
        assertEquals(1, state.totalItemCount)
        assertTrue(state.dateGroups.flatMap { it.items }.all { it.type == UnifiedNoteType.TEXT })

        // 4. Canvas Filter
        viewModel.setFilter(NoteFilterChip.CANVAS)
        advanceUntilIdle()
        state = viewModel.uiState.value
        assertEquals(1, state.totalItemCount)
        assertTrue(state.dateGroups.flatMap { it.items }.all { it.type == UnifiedNoteType.CANVAS })

        // 5. Back to ALL
        viewModel.setFilter(NoteFilterChip.ALL)
        advanceUntilIdle()
        state = viewModel.uiState.value
        assertEquals(5, state.totalItemCount)
    }

    @Test
    fun searchFiltering_filtersNotesByQuery() = runTest(testDispatcher) {
        advanceUntilIdle()

        // Search by title
        viewModel.setSearchQuery("Hukum")
        advanceUntilIdle()
        var state = viewModel.uiState.value
        assertEquals(1, state.totalItemCount)
        assertEquals("Hukum Hess", state.dateGroups[0].items[0].title)

        // Search by subject/folder name "Fisika" (Catatan GLBB, Diagram Vektor)
        viewModel.setSearchQuery("Fisika")
        advanceUntilIdle()
        state = viewModel.uiState.value
        assertEquals(2, state.totalItemCount)
        assertTrue(state.dateGroups.flatMap { it.items }.all { it.folderName == "Fisika" })

        // Search by text note body markdown "Ringkasan"
        viewModel.setSearchQuery("Ringkasan")
        advanceUntilIdle()
        state = viewModel.uiState.value
        assertEquals(1, state.totalItemCount)
        assertEquals("Kisi-Kisi Kimia", state.dateGroups[0].items[0].title)

        // Clear search
        viewModel.setSearchQuery("")
        advanceUntilIdle()
        state = viewModel.uiState.value
        assertEquals(5, state.totalItemCount)
    }

    @Test
    fun deleteNote_movesToTrashAndUpdatesFlow() = runTest(testDispatcher) {
        advanceUntilIdle()
        val photoItem = viewModel.uiState.value.dateGroups.flatMap { it.items }.first { it.id == 10L }

        viewModel.deleteNote(photoItem)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(4, state.totalItemCount)
        assertFalse(state.dateGroups.flatMap { it.items }.any { it.id == 10L })
    }

    @Test
    fun renameNote_updatesTitle() = runTest(testDispatcher) {
        advanceUntilIdle()
        val textItem = viewModel.uiState.value.dateGroups.flatMap { it.items }.first { it.id == 30L }

        viewModel.renameNote(textItem, "Kimia Bab 2")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.dateGroups.flatMap { it.items }.any { it.title == "Kimia Bab 2" })
    }

    @Test
    fun moveNote_updatesFolder() = runTest(testDispatcher) {
        advanceUntilIdle()
        val docItem = viewModel.uiState.value.dateGroups.flatMap { it.items }.first { it.id == 20L }
        assertEquals("Kimia", docItem.folderName)

        viewModel.moveNote(docItem, targetFolderId = 2L) // move to Fisika
        advanceUntilIdle()

        val state = viewModel.uiState.value
        val updatedDoc = state.dateGroups.flatMap { it.items }.first { it.id == 20L }
        assertEquals("Fisika", updatedDoc.folderName)
    }
}
