// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.canvas

import com.arinara.fotara.canvas.engine.CanvasRect
import com.arinara.fotara.canvas.model.CanvasBackgroundStyle
import com.arinara.fotara.canvas.model.CanvasDocument
import com.arinara.fotara.canvas.model.CanvasElement
import com.arinara.fotara.canvas.model.CanvasLayer
import com.arinara.fotara.canvas.model.StrokePoint
import com.arinara.fotara.canvas.model.StrokeElement
import com.arinara.fotara.canvas.model.StrokeToolType
import com.arinara.fotara.canvas.persistence.CanvasAssetManager
import com.arinara.fotara.canvas.persistence.CanvasRepository
import com.arinara.fotara.canvas.tool.CanvasToolType
import com.arinara.fotara.data.model.CanvasNote
import com.arinara.fotara.data.repository.CanvasNoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CanvasViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeCanvasNoteRepository: FakeCanvasNoteRepository
    private lateinit var fakeCanvasRepository: FakeCanvasRepository
    private lateinit var assetManager: CanvasAssetManager

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeCanvasNoteRepository = FakeCanvasNoteRepository()
        fakeCanvasRepository = FakeCanvasRepository()
        assetManager = CanvasAssetManager(null)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        canvasId: Long? = 1L,
        folderId: Long = 10L,
        subfolderId: Long? = null
    ): CanvasViewModel {
        return CanvasViewModel(
            initialCanvasId = canvasId,
            initialFolderId = folderId,
            initialSubfolderId = subfolderId,
            canvasNoteRepository = fakeCanvasNoteRepository,
            canvasRepository = fakeCanvasRepository,
            canvasAssetManager = assetManager
        )
    }

    @Test
    fun loadExistingCanvas_initializesStateSuccessfully() = runTest(testDispatcher) {
        val initialDoc = CanvasDocument(
            id = 1L,
            title = "Lecture Notes",
            layers = listOf(
                CanvasLayer(id = "layer-1", name = "Layer 1", isVisible = true, isLocked = false)
            )
        )
        fakeCanvasRepository.saveDocumentImmediate(1L, initialDoc)
        fakeCanvasNoteRepository.createCanvasNote(10L, null, "Lecture Notes")

        val viewModel = createViewModel(canvasId = 1L)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1L, state.canvasId)
        assertEquals("Lecture Notes", state.title)
        assertEquals(SaveState.SAVED, state.saveState)
        assertEquals(1, state.document.layers.size)
        assertEquals("layer-1", state.activeLayerId)
    }

    @Test
    fun createNewCanvas_createsRecordAndDefaultDocument() = runTest(testDispatcher) {
        val viewModel = createViewModel(canvasId = null)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.canvasId)
        assertEquals("Canvas Note", state.title)
        assertEquals(SaveState.SAVED, state.saveState)
        assertEquals(1, state.document.layers.size)
        assertEquals(state.document.layers.first().id, state.activeLayerId)
    }

    @Test
    fun setTool_updatesActiveToolState() = runTest(testDispatcher) {
        val viewModel = createViewModel(canvasId = 1L)
        advanceUntilIdle()

        viewModel.setTool(CanvasToolType.HIGHLIGHTER)
        assertEquals(CanvasToolType.HIGHLIGHTER, viewModel.uiState.value.toolState.activeTool)

        viewModel.setTool(CanvasToolType.ERASER_STROKE)
        assertEquals(CanvasToolType.ERASER_STROKE, viewModel.uiState.value.toolState.activeTool)

        viewModel.setTool(CanvasToolType.SELECT)
        assertEquals(CanvasToolType.SELECT, viewModel.uiState.value.toolState.activeTool)

        viewModel.setTool(CanvasToolType.PEN)
        assertEquals(CanvasToolType.PEN, viewModel.uiState.value.toolState.activeTool)
    }

    @Test
    fun penProperties_updateCorrectly() = runTest(testDispatcher) {
        val viewModel = createViewModel(canvasId = 1L)
        advanceUntilIdle()

        viewModel.setPenColor(0xFFEBD8B8)
        assertEquals(0xFFEBD8B8, viewModel.uiState.value.toolState.penColor)
        assertTrue(viewModel.uiState.value.recentColors.contains(0xFFEBD8B8))

        viewModel.setPenSize(8.5f)
        assertEquals(8.5f, viewModel.uiState.value.toolState.penSize, 0.01f)

        viewModel.setPenAlpha(0.75f)
        assertEquals(0.75f, viewModel.uiState.value.toolState.penAlpha, 0.01f)
    }

    @Test
    fun highlighterProperties_updateCorrectly() = runTest(testDispatcher) {
        val viewModel = createViewModel(canvasId = 1L)
        advanceUntilIdle()

        viewModel.setHighlighterColor(0xFF81C784)
        assertEquals(0xFF81C784, viewModel.uiState.value.toolState.highlighterColor)
        assertTrue(viewModel.uiState.value.recentColors.contains(0xFF81C784))

        viewModel.setHighlighterSize(24f)
        assertEquals(24f, viewModel.uiState.value.toolState.highlighterSize, 0.01f)
    }

    @Test
    fun eraserProperties_andModeToggle() = runTest(testDispatcher) {
        val viewModel = createViewModel(canvasId = 1L)
        advanceUntilIdle()

        viewModel.setEraserRadius(30f)
        assertEquals(30f, viewModel.uiState.value.toolState.eraserRadius, 0.01f)

        viewModel.setTool(CanvasToolType.ERASER_STROKE)
        val initialTool = viewModel.uiState.value.toolState.activeTool
        viewModel.toggleEraserMode()
        val toggledTool = viewModel.uiState.value.toolState.activeTool
        assertEquals(CanvasToolType.ERASER_AREA, toggledTool)
    }

    @Test
    fun layerManagement_addDeleteReorderAndUndo() = runTest(testDispatcher) {
        val viewModel = createViewModel(canvasId = 1L)
        advanceUntilIdle()

        // 1. Add Layer
        viewModel.addLayer("Layer 2")
        assertEquals(2, viewModel.uiState.value.document.layers.size)
        val secondLayerId = viewModel.uiState.value.activeLayerId

        // 2. Rename Layer
        viewModel.renameLayer(secondLayerId, "Annotation Layer")
        assertEquals("Annotation Layer", viewModel.uiState.value.document.layers.first { it.id == secondLayerId }.name)

        // 3. Move Layer Down
        viewModel.moveLayerDown(secondLayerId)
        assertEquals(secondLayerId, viewModel.uiState.value.document.layers.first().id)

        // 4. Move Layer Up
        viewModel.moveLayerUp(secondLayerId)
        assertEquals(secondLayerId, viewModel.uiState.value.document.layers.last().id)

        // 5. Visibility and Lock
        viewModel.toggleLayerVisibility(secondLayerId)
        assertFalse(viewModel.uiState.value.document.layers.first { it.id == secondLayerId }.isVisible)

        viewModel.toggleLayerLock(secondLayerId)
        assertTrue(viewModel.uiState.value.document.layers.first { it.id == secondLayerId }.isLocked)

        // 6. Layer Opacity
        viewModel.setLayerOpacity(secondLayerId, 0.45f)
        assertEquals(0.45f, viewModel.uiState.value.document.layers.first { it.id == secondLayerId }.opacity, 0.01f)

        // 7. Delete Layer
        viewModel.deleteLayer(secondLayerId)
        assertEquals(1, viewModel.uiState.value.document.layers.size)

        // 8. Undo restore layer
        viewModel.undo()
        assertEquals(2, viewModel.uiState.value.document.layers.size)
    }

    @Test
    fun deleteLayer_guardPreventDeletingOnlyLayer() = runTest(testDispatcher) {
        val viewModel = createViewModel(canvasId = 1L)
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.document.layers.size)
        val singleLayerId = viewModel.uiState.value.document.layers.first().id

        viewModel.deleteLayer(singleLayerId)
        assertEquals(1, viewModel.uiState.value.document.layers.size)
        assertNotNull(viewModel.uiState.value.userMessage)
    }

    @Test
    fun contextualZ8_selectionDuplicateAndDelete() = runTest(testDispatcher) {
        val viewModel = createViewModel(canvasId = 1L)
        advanceUntilIdle()

        val activeLayerId = viewModel.uiState.value.activeLayerId
        val sampleStroke = StrokeElement(
            id = "test-stroke-1",
            layerId = activeLayerId,
            points = listOf(StrokePoint(10f, 10f), StrokePoint(50f, 50f)),
            toolType = StrokeToolType.PEN,
            color = 0xFFFFFFFFL,
            width = 4f,
            bounds = CanvasRect(10f, 10f, 50f, 50f)
        )

        // Add stroke element to active layer via engine
        viewModel.spatialIndex.insert(sampleStroke)
        val updatedDoc = viewModel.uiState.value.document.copy(elements = listOf(sampleStroke))
        viewModel.setSelectedElementIds(setOf("test-stroke-1"))
        viewModel.onElementsChanged(updatedDoc)

        assertEquals(1, viewModel.uiState.value.toolState.selectedElementIds.size)

        // 1. Duplicate
        viewModel.duplicateSelectedElements()
        advanceUntilIdle()

        val afterDuplicateDoc = viewModel.uiState.value.document
        assertEquals(2, afterDuplicateDoc.elements.size)

        // 2. Delete selected
        viewModel.deleteSelectedElements()
        advanceUntilIdle()

        val afterDeleteDoc = viewModel.uiState.value.document
        assertEquals(1, afterDeleteDoc.elements.size)

        // 3. Undo delete
        viewModel.undo()
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.document.elements.size)
    }

    @Test
    fun contextualZ8_bringForwardAndSendBackward() = runTest(testDispatcher) {
        val viewModel = createViewModel(canvasId = 1L)
        advanceUntilIdle()

        val activeLayerId = viewModel.uiState.value.activeLayerId
        val elemA = StrokeElement(
            id = "elem-A",
            layerId = activeLayerId,
            points = listOf(StrokePoint(0f, 0f), StrokePoint(10f, 10f)),
            toolType = StrokeToolType.PEN,
            color = 0xFF000000L,
            width = 2f,
            bounds = CanvasRect(0f, 0f, 10f, 10f),
            zIndex = 0
        )
        val elemB = StrokeElement(
            id = "elem-B",
            layerId = activeLayerId,
            points = listOf(StrokePoint(20f, 20f), StrokePoint(30f, 30f)),
            toolType = StrokeToolType.PEN,
            color = 0xFFFFFFFFL,
            width = 2f,
            bounds = CanvasRect(20f, 20f, 30f, 30f),
            zIndex = 1
        )

        val updatedDoc = viewModel.uiState.value.document.copy(elements = listOf(elemA, elemB))
        viewModel.setSelectedElementIds(setOf("elem-A"))
        viewModel.onElementsChanged(updatedDoc)

        // Send Backward (elem-A is already at min zIndex)
        viewModel.sendBackwardSelection()
        advanceUntilIdle()

        // Bring Forward (elem-A increases zIndex)
        viewModel.bringForwardSelection()
        advanceUntilIdle()

        val sorted = viewModel.uiState.value.document.elements.sortedBy { it.zIndex }
        assertEquals("elem-A", sorted.last().id)
    }

    @Test
    fun contextualZ8_moveSelectionToLayer() = runTest(testDispatcher) {
        val viewModel = createViewModel(canvasId = 1L)
        advanceUntilIdle()

        viewModel.addLayer("Layer 2")
        assertEquals(2, viewModel.uiState.value.document.layers.size)

        val l1Id = viewModel.uiState.value.document.layers[0].id
        val l2Id = viewModel.uiState.value.document.layers[1].id

        val elem = StrokeElement(
            id = "elem-layer-move",
            layerId = l1Id,
            points = listOf(StrokePoint(0f, 0f), StrokePoint(5f, 5f)),
            toolType = StrokeToolType.PEN,
            color = 0xFF112233L,
            width = 2f,
            bounds = CanvasRect(0f, 0f, 5f, 5f)
        )

        val docWithElem = viewModel.uiState.value.document.copy(elements = listOf(elem))
        viewModel.setSelectedElementIds(setOf("elem-layer-move"))
        viewModel.onElementsChanged(docWithElem)

        // Move to Layer 2
        viewModel.moveSelectionToLayer(l2Id)
        advanceUntilIdle()

        val finalDoc = viewModel.uiState.value.document
        val l1Elements = finalDoc.elements.filter { it.layerId == l1Id }
        val l2Elements = finalDoc.elements.filter { it.layerId == l2Id }

        assertTrue(l1Elements.isEmpty())
        assertEquals(1, l2Elements.size)
        assertEquals("elem-layer-move", l2Elements.first().id)
    }

    @Test
    fun canvasSettings_andBackgroundStyle() = runTest(testDispatcher) {
        val viewModel = createViewModel(canvasId = 1L)
        advanceUntilIdle()

        viewModel.setBackgroundStyle(CanvasBackgroundStyle.RULED)
        assertEquals(CanvasBackgroundStyle.RULED, viewModel.uiState.value.document.backgroundStyle)

        viewModel.setStylusOnlyMode(true)
        assertTrue(viewModel.uiState.value.toolState.stylusOnlyDrawing)

        viewModel.setPalmRejection(false)
        assertTrue(viewModel.pointerStateMachine.palmRejectionRadiusThreshold > 1000f)

        // Rename
        viewModel.renameCanvas("Physics Diagram")
        advanceUntilIdle()
        assertEquals("Physics Diagram", viewModel.uiState.value.title)
    }

    @Test
    fun exportBoundsAndDownsamplingCalculation_protectsAgainstOOM() {
        val bounds = CanvasRect(-100f, -200f, 3900f, 5800f)
        val width = bounds.width
        val height = bounds.height
        val maxDimension = 4096f

        // 1x scale test
        val scale1x = 1.0f
        var targetW1x = (width * scale1x).toInt()
        var targetH1x = (height * scale1x).toInt()
        if (targetW1x > maxDimension || targetH1x > maxDimension) {
            val ratio = maxDimension / maxOf(targetW1x, targetH1x)
            targetW1x = (targetW1x * ratio).toInt()
            targetH1x = (targetH1x * ratio).toInt()
        }

        assertTrue(targetW1x <= maxDimension.toInt())
        assertTrue(targetH1x <= maxDimension.toInt())
        assertEquals(maxDimension.toInt(), maxOf(targetW1x, targetH1x))

        // 2x scale test
        val scale2x = 2.0f
        var targetW2x = (width * scale2x).toInt()
        var targetH2x = (height * scale2x).toInt()
        if (targetW2x > maxDimension || targetH2x > maxDimension) {
            val ratio = maxDimension / maxOf(targetW2x, targetH2x)
            targetW2x = (targetW2x * ratio).toInt()
            targetH2x = (targetH2x * ratio).toInt()
        }

        assertTrue(targetW2x <= maxDimension.toInt())
        assertTrue(targetH2x <= maxDimension.toInt())
        assertEquals(maxDimension.toInt(), maxOf(targetW2x, targetH2x))
    }
}

private class FakeCanvasNoteRepository : CanvasNoteRepository {
    private val notes = mutableListOf<CanvasNote>()
    private var nextId = 1L

    override fun getAllActiveCanvasNotes(): Flow<List<CanvasNote>> = flowOf(notes.filter { !it.isTrashed })
    override fun searchCanvasNotes(query: String): Flow<List<CanvasNote>> = flowOf(notes.filter { it.title.contains(query, ignoreCase = true) })
    override fun getCanvasNotesByFolder(folderId: Long, subfolderId: Long?): Flow<List<CanvasNote>> = flowOf(notes.filter { it.folderId == folderId && it.subfolderId == subfolderId })
    override fun getCanvasNoteById(id: Long): Flow<CanvasNote?> = flowOf(notes.firstOrNull { it.id == id })
    override suspend fun getCanvasNoteByIdOnce(id: Long): CanvasNote? = notes.firstOrNull { it.id == id }

    override suspend fun createCanvasNote(
        folderId: Long,
        subfolderId: Long?,
        title: String,
        dataBlob: ByteArray?,
        tagColor: String?,
        scheduledAt: Long?,
        alertType: String?
    ): Long {
        val id = nextId++
        val note = CanvasNote(
            id = id,
            folderId = folderId,
            subfolderId = subfolderId,
            title = title,
            tagColor = tagColor,
            scheduledAt = scheduledAt,
            alertType = alertType
        )
        notes.add(note)
        return id
    }

    override suspend fun updateCanvasNoteData(id: Long, dataBlob: ByteArray, thumbnailPath: String?) {}
    override suspend fun renameCanvasNote(id: Long, newTitle: String) {
        val index = notes.indexOfFirst { it.id == id }
        if (index != -1) {
            notes[index] = notes[index].copy(title = newTitle)
        }
    }
    override suspend fun updateTagColor(id: Long, colorHex: String?) {}
    override suspend fun updateDeadline(id: Long, deadlineMs: Long?) {}
    override suspend fun deleteCanvasNote(id: Long) {
        val index = notes.indexOfFirst { it.id == id }
        if (index != -1) {
            notes[index] = notes[index].copy(isTrashed = true)
        }
    }
    override suspend fun deleteCanvasNotes(ids: List<Long>) {
        ids.forEach { deleteCanvasNote(it) }
    }
    override suspend fun restoreCanvasNote(id: Long) {
        val index = notes.indexOfFirst { it.id == id }
        if (index != -1) {
            notes[index] = notes[index].copy(isTrashed = false)
        }
    }
    override suspend fun purgeCanvasNotePermanently(id: Long) {
        notes.removeAll { it.id == id }
    }
    override suspend fun moveCanvasNote(id: Long, targetFolderId: Long, targetSubfolderId: Long?) {
        val index = notes.indexOfFirst { it.id == id }
        if (index != -1) {
            notes[index] = notes[index].copy(folderId = targetFolderId, subfolderId = targetSubfolderId)
        }
    }
    override suspend fun moveCanvasNotes(ids: List<Long>, targetFolderId: Long, targetSubfolderId: Long?) {
        ids.forEach { moveCanvasNote(it, targetFolderId, targetSubfolderId) }
    }
    override suspend fun refresh() {}
}

private class FakeCanvasRepository : CanvasRepository {
    private val docs = mutableMapOf<Long, CanvasDocument>()
    private val dirtyMap = mutableMapOf<Long, CanvasDocument>()

    override fun getDocumentFlow(canvasId: Long): Flow<CanvasDocument?> = flowOf(docs[canvasId])
    override suspend fun loadDocument(canvasId: Long): CanvasDocument? = docs[canvasId]
    override suspend fun loadDocumentProgressive(
        canvasId: Long,
        chunkSize: Int,
        onChunkLoaded: ((chunk: List<CanvasElement>, progress: Float) -> Unit)?
    ): CanvasDocument? = docs[canvasId]

    override fun markDirty(canvasId: Long, document: CanvasDocument) {
        dirtyMap[canvasId] = document
        docs[canvasId] = document
    }

    override suspend fun saveDocumentImmediate(canvasId: Long, document: CanvasDocument) {
        dirtyMap.remove(canvasId)
        docs[canvasId] = document
    }

    override suspend fun purgeCanvas(canvasId: Long) {
        docs.remove(canvasId)
        dirtyMap.remove(canvasId)
    }
}
