// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.workspace

import com.arinara.fotara.data.model.CanvasNote
import com.arinara.fotara.data.model.DocumentNote
import com.arinara.fotara.data.model.DocumentType
import com.arinara.fotara.data.model.Folder
import com.arinara.fotara.data.model.Photo
import com.arinara.fotara.data.model.TextNote
import com.arinara.fotara.data.model.Workspace
import com.arinara.fotara.data.model.WorkspaceKind
import com.arinara.fotara.test.FakeCanvasNoteRepository
import com.arinara.fotara.test.FakeDocumentRepository
import com.arinara.fotara.test.FakeFolderRepository
import com.arinara.fotara.test.FakePhotoRepository
import com.arinara.fotara.test.FakeTextNoteRepository
import com.arinara.fotara.test.FakeWorkspaceRepository
import com.arinara.fotara.ui.notes.NoteFilterChip
import com.arinara.fotara.ui.notes.NotesViewModel
import com.arinara.fotara.ui.notes.UnifiedNoteType
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotesWorkspaceFilterTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var folderRepo: FakeFolderRepository
    private lateinit var photoRepo: FakePhotoRepository
    private lateinit var docRepo: FakeDocumentRepository
    private lateinit var textRepo: FakeTextNoteRepository
    private lateinit var canvasRepo: FakeCanvasNoteRepository
    private lateinit var workspaceRepo: FakeWorkspaceRepository
    private lateinit var viewModel: NotesViewModel

    private val now = System.currentTimeMillis()

    // Workspaces
    private val homeWs = Workspace(id = 1L, kind = WorkspaceKind.HOME, name = "Home", position = 0)
    private val archiveWs = Workspace(id = 2L, kind = WorkspaceKind.ARCHIVE, name = "Archive", position = 1)
    private val workWs = Workspace(id = 3L, kind = WorkspaceKind.CUSTOM, name = "Work", position = 2)

    // Folders: folder1 in Home (ws 1), folder2 in Work (ws 3)
    private val folderHome = Folder(id = 10L, name = "Personal Notes", workspaceId = 1L)
    private val folderWork = Folder(id = 20L, name = "Project Architecture", workspaceId = 3L)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        folderRepo = FakeFolderRepository(listOf(folderHome, folderWork))
        workspaceRepo = FakeWorkspaceRepository(
            initialWorkspaces = listOf(homeWs, archiveWs, workWs),
            folderRepository = folderRepo
        )

        val photos = listOf(
            Photo(id = 101L, fileUri = "file://101.jpg", folderId = 10L, caption = "Home Receipt", addedAt = now),
            Photo(id = 102L, fileUri = "file://102.jpg", folderId = 20L, caption = "Work Diagram", addedAt = now)
        )
        photoRepo = FakePhotoRepository(folderRepo, photos)

        val docs = listOf(
            DocumentNote(id = 201L, folderId = 10L, name = "Home Budget.pdf", docType = DocumentType.PDF, originFileUri = "file://doc1.pdf", addedAt = now),
            DocumentNote(id = 202L, folderId = 20L, name = "Sprint Spec.pdf", docType = DocumentType.PDF, originFileUri = "file://doc2.pdf", addedAt = now)
        )
        docRepo = FakeDocumentRepository(docs)

        val textNotes = listOf(
            TextNote(id = 301L, folderId = 10L, title = "Grocery List", bodyMarkdown = "Milk, Eggs", createdAt = now, updatedAt = now, addedAt = now),
            TextNote(id = 302L, folderId = 20L, title = "API Endpoints", bodyMarkdown = "GET /v1/notes", createdAt = now, updatedAt = now, addedAt = now)
        )
        textRepo = FakeTextNoteRepository(textNotes)

        val canvasNotes = listOf(
            CanvasNote(id = 401L, folderId = 10L, title = "Doodle", addedAt = now),
            CanvasNote(id = 402L, folderId = 20L, title = "Wireframe", addedAt = now)
        )
        canvasRepo = FakeCanvasNoteRepository(canvasNotes)

        viewModel = NotesViewModel(
            photoRepository = photoRepo,
            documentRepository = docRepo,
            textNoteRepository = textRepo,
            canvasNoteRepository = canvasRepo,
            folderRepository = folderRepo,
            workspaceRepository = workspaceRepo
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun notesWorkspaceFilter_onlyShowsNotesInSelectedWorkspace() = runTest(testDispatcher) {
        advanceUntilIdle()

        // By default selectedWorkspaceId is 1 (Home)
        var state = viewModel.uiState.value
        assertEquals(1L, state.selectedWorkspaceId)
        assertEquals("Home", state.selectedWorkspaceName)
        // 4 notes belong to folderHome (ws 1), 4 belong to folderWork (ws 3)
        assertEquals(4, state.totalItemCount)

        val homeTitles = state.dateGroups.flatMap { it.items }.map { it.title }
        assertTrue(homeTitles.contains("Home Receipt"))
        assertTrue(homeTitles.contains("Home Budget.pdf"))
        assertTrue(homeTitles.contains("Grocery List"))
        assertTrue(homeTitles.contains("Doodle"))
        assertFalse(homeTitles.contains("Work Diagram"))
        assertFalse(homeTitles.contains("Sprint Spec.pdf"))

        // Switch to Work workspace (id = 3L)
        viewModel.selectWorkspace(3L)
        advanceUntilIdle()

        state = viewModel.uiState.value
        assertEquals(3L, state.selectedWorkspaceId)
        assertEquals("Work", state.selectedWorkspaceName)
        assertEquals(4, state.totalItemCount)

        val workTitles = state.dateGroups.flatMap { it.items }.map { it.title }
        assertTrue(workTitles.contains("Work Diagram"))
        assertTrue(workTitles.contains("Sprint Spec.pdf"))
        assertTrue(workTitles.contains("API Endpoints"))
        assertTrue(workTitles.contains("Wireframe"))
        assertFalse(workTitles.contains("Home Receipt"))
    }

    @Test
    fun notesWorkspaceFilter_typeFiltersCombineWithWorkspaceFilter() = runTest(testDispatcher) {
        advanceUntilIdle()

        // In Home workspace (ws 1), select DOCUMENTS filter
        viewModel.setFilter(NoteFilterChip.DOCUMENTS)
        advanceUntilIdle()

        var state = viewModel.uiState.value
        assertEquals(1, state.totalItemCount)
        val items = state.dateGroups.flatMap { it.items }
        assertEquals(1, items.size)
        assertEquals("Home Budget.pdf", items[0].title)
        assertEquals(UnifiedNoteType.DOCUMENT, items[0].type)

        // Switch to Work workspace (ws 3) while DOCUMENTS filter remains active
        viewModel.selectWorkspace(3L)
        advanceUntilIdle()

        state = viewModel.uiState.value
        assertEquals(1, state.totalItemCount)
        val workDocs = state.dateGroups.flatMap { it.items }
        assertEquals("Sprint Spec.pdf", workDocs[0].title)
        assertEquals(UnifiedNoteType.DOCUMENT, workDocs[0].type)
    }

    @Test
    fun notesWorkspaceFilter_emptyStateWhenWorkspaceHasNoNotes() = runTest(testDispatcher) {
        advanceUntilIdle()

        // Switch to Archive workspace (id = 2L), which has no folders and no notes
        viewModel.selectWorkspace(2L)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2L, state.selectedWorkspaceId)
        assertEquals("Archive", state.selectedWorkspaceName)
        assertEquals(0, state.totalItemCount)
        assertTrue(state.dateGroups.isEmpty())
    }

    @Test
    fun notesWorkspaceFilter_fallbackToHomeWhenSelectedWorkspaceDeleted() = runTest(testDispatcher) {
        advanceUntilIdle()

        // Switch to Work workspace (id = 3L)
        viewModel.selectWorkspace(3L)
        advanceUntilIdle()
        assertEquals(3L, viewModel.uiState.value.selectedWorkspaceId)

        // Delete Work workspace (moving its folders to Home)
        workspaceRepo.deleteWorkspaceMoveFoldersToHome(3L)
        advanceUntilIdle()

        // Repository should fall back to HOME_WORKSPACE_ID (1L)
        val state = viewModel.uiState.value
        assertEquals(1L, state.selectedWorkspaceId)
        assertEquals("Home", state.selectedWorkspaceName)
        // Now all 8 notes are in Home workspace because folderWork was moved to Home!
        assertEquals(8, state.totalItemCount)
    }

    @Test
    fun notesFilterToggle_togglingHidesAndResetsFilter() = runTest(testDispatcher) {
        advanceUntilIdle()

        // Initially filters row is hidden
        assertFalse(viewModel.uiState.value.isFiltersVisible)
        assertEquals(NoteFilterChip.ALL, viewModel.uiState.value.selectedFilter)

        // Toggle to show filters
        viewModel.toggleFilters()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isFiltersVisible)

        // Select a specific filter
        viewModel.setFilter(NoteFilterChip.PHOTOS)
        advanceUntilIdle()
        assertEquals(NoteFilterChip.PHOTOS, viewModel.uiState.value.selectedFilter)

        // Toggle to hide filters: should reset filter to ALL
        viewModel.toggleFilters()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isFiltersVisible)
        assertEquals(NoteFilterChip.ALL, viewModel.uiState.value.selectedFilter)
    }
}
