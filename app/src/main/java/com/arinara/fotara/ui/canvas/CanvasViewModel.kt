// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.canvas

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arinara.fotara.canvas.engine.AddElementsCommand
import com.arinara.fotara.canvas.engine.AddLayerCommand
import com.arinara.fotara.canvas.engine.CanvasHistoryManager
import com.arinara.fotara.canvas.engine.CanvasRect
import com.arinara.fotara.canvas.engine.ChangeLayerPropsCommand
import com.arinara.fotara.canvas.engine.MoveElementsToLayerCommand
import com.arinara.fotara.canvas.engine.QuadTreeSpatialIndex
import com.arinara.fotara.canvas.engine.RemoveElementsCommand
import com.arinara.fotara.canvas.engine.RemoveLayerCommand
import com.arinara.fotara.canvas.engine.ReorderLayersCommand
import com.arinara.fotara.canvas.engine.SetBackgroundStyleCommand
import com.arinara.fotara.canvas.engine.StrokeProcessor
import com.arinara.fotara.canvas.engine.TransformElementsCommand
import com.arinara.fotara.canvas.engine.ViewportState
import com.arinara.fotara.canvas.engine.ViewportTransform
import com.arinara.fotara.canvas.gesture.PointerStateMachine
import com.arinara.fotara.canvas.model.CanvasBackgroundStyle
import com.arinara.fotara.canvas.model.CanvasDocument
import com.arinara.fotara.canvas.model.CanvasDocumentOperations
import com.arinara.fotara.canvas.model.CanvasElement
import com.arinara.fotara.canvas.model.CanvasLayer
import com.arinara.fotara.canvas.model.ImageElement
import com.arinara.fotara.canvas.model.StrokeElement
import com.arinara.fotara.canvas.model.StrokeToolType
import com.arinara.fotara.canvas.persistence.CanvasAssetManager
import com.arinara.fotara.canvas.persistence.CanvasRepository
import com.arinara.fotara.canvas.render.CanvasRenderer
import com.arinara.fotara.canvas.render.TileCacheManager
import com.arinara.fotara.canvas.tool.CanvasToolController
import com.arinara.fotara.canvas.tool.CanvasToolState
import com.arinara.fotara.canvas.tool.CanvasToolType
import com.arinara.fotara.data.model.Folder
import com.arinara.fotara.data.repository.CanvasNoteRepository
import com.arinara.fotara.data.repository.FolderRepository
import com.arinara.fotara.data.repository.PhotoRepository
import com.arinara.fotara.data.repository.SettingsRepository
import com.arinara.fotara.util.NoteScheduleManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

enum class SaveState {
    SAVED,
    SAVING,
    ERROR
}

data class CanvasUiState(
    val canvasId: Long? = null,
    val folderId: Long = 0L,
    val subfolderId: Long? = null,
    val title: String = "Untitled Canvas",
    val document: CanvasDocument = CanvasDocument(),
    val activeLayerId: String = "layer_default",
    val toolState: CanvasToolState = CanvasToolState(),
    val recentColors: List<Long> = listOf(
        0xFFEBD8B8, // FolderTabCream
        0xFFF4D03F, // TagAmber
        0xFFE74C3C, // TagCrimson
        0xFF2ECC71, // SageGreen
        0xFF3498DB, // RoyalBlue
        0xFFFFFFFF, // PureWhite
        0xFF95A5A6, // SlateGray
        0xFF0D1B2A  // MidnightNavy
    ),
    val zoomPercentage: Int = 100,
    val saveState: SaveState = SaveState.SAVED,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val isBottomDockCollapsed: Boolean = false,
    val showToolOptions: Boolean = false,
    val showLayersPanel: Boolean = false,
    val showRenameDialog: Boolean = false,
    val showBackgroundPicker: Boolean = false,
    val showSettingsDialog: Boolean = false,
    val showScheduleDialog: Boolean = false,
    val showInfoDialog: Boolean = false,
    val showExportDialog: Boolean = false,
    val showMoveDialog: Boolean = false,
    val showDeleteConfirmDialog: Boolean = false,
    val showImageSourceDialog: Boolean = false,
    val showExistingNotesDialog: Boolean = false,
    val showExperimentalNotice: Boolean = false,
    val isExporting: Boolean = false,
    val exportProgress: Float = 0f,
    val scheduledAt: Long? = null,
    val alertType: String? = null,
    val userMessage: String? = null,
    val folders: List<Folder> = emptyList()
)

class CanvasViewModel(
    private val initialCanvasId: Long?,
    private val initialFolderId: Long,
    private val initialSubfolderId: Long?,
    private val canvasNoteRepository: CanvasNoteRepository,
    private val canvasRepository: CanvasRepository,
    private val canvasAssetManager: CanvasAssetManager,
    private val photoRepository: PhotoRepository? = null,
    private val folderRepository: FolderRepository? = null,
    private val settingsRepository: SettingsRepository? = null,
    private val scheduleManager: NoteScheduleManager? = null
) : ViewModel() {

    // Core C8 & C9 Engine Components
    val historyManager = CanvasHistoryManager(maxHistorySize = 100)
    val spatialIndex = QuadTreeSpatialIndex(worldBounds = CanvasRect(-50000f, -50000f, 50000f, 50000f))
    val tileCacheManager = TileCacheManager(assetManager = canvasAssetManager)
    val pointerStateMachine = PointerStateMachine()
    val toolController = CanvasToolController()
    val canvasRenderer by lazy { CanvasRenderer(tileCacheManager = tileCacheManager, assetManager = canvasAssetManager) }

    private val _uiState = MutableStateFlow(
        CanvasUiState(
            canvasId = initialCanvasId,
            folderId = initialFolderId,
            subfolderId = initialSubfolderId
        )
    )
    val uiState: StateFlow<CanvasUiState> = _uiState.asStateFlow()

    private var autosaveJob: Job? = null

    init {
        // Load folders for Move action
        folderRepository?.let { repo ->
            viewModelScope.launch {
                repo.getFolders().collect { folderList ->
                    _uiState.update { it.copy(folders = folderList) }
                }
            }
        }

        if (initialCanvasId != null && initialCanvasId > 0) {
            loadExistingCanvas(initialCanvasId)
        } else {
            createNewCanvasRecord()
        }
    }

    private fun loadExistingCanvas(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(saveState = SaveState.SAVING) }
            val note = canvasNoteRepository.getCanvasNoteByIdOnce(id)
            if (note != null) {
                val doc = canvasRepository.loadDocument(id) ?: CanvasDocument(title = note.title)
                spatialIndex.rebuild(doc.elements)
                tileCacheManager.invalidateAll()

                val primaryLayerId = doc.getPrimaryLayerId()
                _uiState.update {
                    it.copy(
                        canvasId = note.id,
                        title = note.title,
                        document = doc,
                        activeLayerId = primaryLayerId,
                        scheduledAt = note.scheduledAt,
                        alertType = note.alertType,
                        saveState = SaveState.SAVED,
                        canUndo = historyManager.canUndo,
                        canRedo = historyManager.canRedo
                    )
                }
            } else {
                _uiState.update { it.copy(saveState = SaveState.ERROR, userMessage = "Failed to load canvas document.") }
            }
        }
    }

    private fun createNewCanvasRecord() {
        viewModelScope.launch {
            _uiState.update { it.copy(saveState = SaveState.SAVING) }
            val initialTitle = "Canvas Note"
            val initialDoc = CanvasDocument(title = initialTitle)
            val newId = canvasNoteRepository.createCanvasNote(
                folderId = initialFolderId,
                subfolderId = initialSubfolderId,
                title = initialTitle
            )

            canvasRepository.saveDocumentImmediate(newId, initialDoc)
            spatialIndex.rebuild(initialDoc.elements)

            _uiState.update {
                it.copy(
                    canvasId = newId,
                    title = initialTitle,
                    document = initialDoc,
                    activeLayerId = initialDoc.getPrimaryLayerId(),
                    saveState = SaveState.SAVED,
                    showExperimentalNotice = true // One-time Alpha notice
                )
            }
        }
    }

    // ==========================================
    // Document Synchronization & Autosave
    // ==========================================
    fun onDocumentModified(updatedDoc: CanvasDocument, dirtyRect: CanvasRect?) {
        _uiState.update {
            it.copy(
                document = updatedDoc,
                canUndo = historyManager.canUndo,
                canRedo = historyManager.canRedo,
                toolState = toolController.toolState,
                saveState = SaveState.SAVING
            )
        }
        spatialIndex.rebuild(updatedDoc.elements)
        if (dirtyRect != null) {
            tileCacheManager.invalidateRegion(dirtyRect)
        } else {
            tileCacheManager.invalidateAll()
        }
        triggerDebouncedAutosave()
    }

    fun setZoomPercentage(scale: Float) {
        val pct = (scale * 100f).toInt().coerceIn(5, 5000)
        _uiState.update { it.copy(zoomPercentage = pct) }
    }

    private fun triggerDebouncedAutosave() {
        autosaveJob?.cancel()
        autosaveJob = viewModelScope.launch(Dispatchers.IO) {
            delay(500) // 500ms debounce window
            val currentId = _uiState.value.canvasId ?: return@launch
            val currentDoc = _uiState.value.document
            try {
                canvasRepository.saveDocumentImmediate(currentId, currentDoc)
                _uiState.update { it.copy(saveState = SaveState.SAVED) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        saveState = SaveState.ERROR,
                        userMessage = "Autosave failed. Your changes remain safely in memory."
                    )
                }
            }
        }
    }

    // ==========================================
    // Z2: Operations (Undo / Redo / Export / Settings)
    // ==========================================
    fun undo() {
        val currentDoc = _uiState.value.document
        val undoneDoc = historyManager.undo(currentDoc) ?: return
        spatialIndex.rebuild(undoneDoc.elements)
        tileCacheManager.invalidateAll()
        _uiState.update {
            it.copy(
                document = undoneDoc,
                canUndo = historyManager.canUndo,
                canRedo = historyManager.canRedo,
                saveState = SaveState.SAVING
            )
        }
        triggerDebouncedAutosave()
    }

    fun redo() {
        val currentDoc = _uiState.value.document
        val redoneDoc = historyManager.redo(currentDoc) ?: return
        spatialIndex.rebuild(redoneDoc.elements)
        tileCacheManager.invalidateAll()
        _uiState.update {
            it.copy(
                document = redoneDoc,
                canUndo = historyManager.canUndo,
                canRedo = historyManager.canRedo,
                saveState = SaveState.SAVING
            )
        }
        triggerDebouncedAutosave()
    }

    fun setBackgroundStyle(newStyle: CanvasBackgroundStyle) {
        val currentDoc = _uiState.value.document
        if (currentDoc.backgroundStyle == newStyle) return
        val updated = historyManager.execute(
            SetBackgroundStyleCommand(currentDoc.backgroundStyle, newStyle),
            currentDoc
        )
        tileCacheManager.invalidateAll()
        _uiState.update {
            it.copy(
                document = updated,
                canUndo = historyManager.canUndo,
                canRedo = historyManager.canRedo,
                showBackgroundPicker = false
            )
        }
        triggerDebouncedAutosave()
    }

    fun setStylusOnlyMode(enabled: Boolean) {
        toolController.toolState = toolController.toolState.copy(stylusOnlyDrawing = enabled)
        pointerStateMachine.stylusOnlyDrawing = enabled
        _uiState.update { it.copy(toolState = toolController.toolState) }
    }

    fun setPalmRejection(enabled: Boolean) {
        pointerStateMachine.palmRejectionRadiusThreshold = if (enabled) 60.0f else 9999.0f
    }

    fun renameCanvas(newTitle: String) {
        val clean = newTitle.trim()
        if (clean.isBlank()) return
        val id = _uiState.value.canvasId ?: return
        viewModelScope.launch {
            canvasNoteRepository.renameCanvasNote(id, clean)
            val updated = _uiState.value.document.copy(title = clean)
            _uiState.update { it.copy(title = clean, document = updated, showRenameDialog = false) }
            triggerDebouncedAutosave()
        }
    }

    fun moveCanvas(targetFolderId: Long, targetSubfolderId: Long?, onMoved: () -> Unit) {
        val id = _uiState.value.canvasId ?: return
        viewModelScope.launch {
            canvasNoteRepository.moveCanvasNote(id, targetFolderId, targetSubfolderId)
            _uiState.update {
                it.copy(
                    folderId = targetFolderId,
                    subfolderId = targetSubfolderId,
                    showMoveDialog = false
                )
            }
            onMoved()
        }
    }

    fun deleteToTrash(onDeleted: () -> Unit) {
        val id = _uiState.value.canvasId ?: return
        viewModelScope.launch {
            canvasNoteRepository.deleteCanvasNote(id)
            _uiState.update { it.copy(showDeleteConfirmDialog = false) }
            onDeleted()
        }
    }

    // ==========================================
    // Z3 & Z4: Tools, Colors, and Dock Controls
    // ==========================================
    fun setTool(tool: CanvasToolType) {
        val current = toolController.toolState.activeTool
        val toggleOptions = if (current == tool) !_uiState.value.showToolOptions else false

        toolController.toolState = toolController.toolState.copy(activeTool = tool)
        pointerStateMachine.activeMode = when (tool) {
            CanvasToolType.SELECT -> com.arinara.fotara.canvas.gesture.ActiveMode.SELECT
            CanvasToolType.ERASER_STROKE, CanvasToolType.ERASER_AREA -> com.arinara.fotara.canvas.gesture.ActiveMode.ERASE
            else -> com.arinara.fotara.canvas.gesture.ActiveMode.DRAW
        }

        _uiState.update {
            it.copy(
                toolState = toolController.toolState,
                showToolOptions = toggleOptions
            )
        }
    }

    fun toggleEraserMode() {
        val next = if (toolController.toolState.activeTool == CanvasToolType.ERASER_STROKE) {
            CanvasToolType.ERASER_AREA
        } else {
            CanvasToolType.ERASER_STROKE
        }
        setTool(next)
    }

    fun toggleBottomDock() {
        _uiState.update { it.copy(isBottomDockCollapsed = !it.isBottomDockCollapsed) }
    }

    fun toggleToolOptions() {
        _uiState.update { it.copy(showToolOptions = !it.showToolOptions) }
    }

    fun setPenColor(color: Long) {
        toolController.toolState = toolController.toolState.copy(penColor = color)
        val updatedRecent = (listOf(color) + _uiState.value.recentColors.filter { it != color }).take(8)
        _uiState.update {
            it.copy(
                toolState = toolController.toolState,
                recentColors = updatedRecent
            )
        }
    }

    fun setPenSize(size: Float) {
        toolController.toolState = toolController.toolState.copy(penSize = size)
        _uiState.update { it.copy(toolState = toolController.toolState) }
    }

    fun setPenAlpha(alpha: Float) {
        toolController.toolState = toolController.toolState.copy(penAlpha = alpha)
        _uiState.update { it.copy(toolState = toolController.toolState) }
    }

    fun setHighlighterColor(color: Long) {
        toolController.toolState = toolController.toolState.copy(highlighterColor = color)
        val updatedRecent = (listOf(color) + _uiState.value.recentColors.filter { it != color }).take(8)
        _uiState.update {
            it.copy(
                toolState = toolController.toolState,
                recentColors = updatedRecent
            )
        }
    }

    fun setHighlighterSize(size: Float) {
        toolController.toolState = toolController.toolState.copy(highlighterSize = size)
        _uiState.update { it.copy(toolState = toolController.toolState) }
    }

    fun setEraserRadius(radius: Float) {
        toolController.toolState = toolController.toolState.copy(eraserRadius = radius)
        _uiState.update { it.copy(toolState = toolController.toolState) }
    }

    // ==========================================
    // Z5: Layers Management (Pure Commands)
    // ==========================================
    fun addLayer(layerName: String) {
        val currentDoc = _uiState.value.document
        val nextOrder = (currentDoc.layers.maxOfOrNull { it.order } ?: 0) + 1
        val newLayer = CanvasLayer(
            id = "layer_${UUID.randomUUID()}",
            name = layerName.ifBlank { "Layer ${nextOrder + 1}" },
            order = nextOrder
        )

        val updated = historyManager.execute(AddLayerCommand(newLayer), currentDoc)
        _uiState.update {
            it.copy(
                document = updated,
                activeLayerId = newLayer.id,
                canUndo = historyManager.canUndo,
                canRedo = historyManager.canRedo
            )
        }
        tileCacheManager.invalidateAll()
        triggerDebouncedAutosave()
    }

    fun deleteLayer(layerId: String) {
        val currentDoc = _uiState.value.document
        if (currentDoc.layers.size <= 1) {
            _uiState.update { it.copy(userMessage = "Cannot delete the only remaining layer.") }
            return
        }

        val layer = currentDoc.layers.find { it.id == layerId } ?: return
        val members = currentDoc.elements.filter { it.layerId == layerId }
        val updated = historyManager.execute(RemoveLayerCommand(layer, members), currentDoc)

        val fallbackActiveLayer = updated.layers.first().id
        _uiState.update {
            it.copy(
                document = updated,
                activeLayerId = fallbackActiveLayer,
                canUndo = historyManager.canUndo,
                canRedo = historyManager.canRedo
            )
        }
        spatialIndex.rebuild(updated.elements)
        tileCacheManager.invalidateAll()
        triggerDebouncedAutosave()
    }

    fun moveLayerUp(layerId: String) {
        val currentDoc = _uiState.value.document
        val sorted = currentDoc.layers.sortedBy { it.order }.map { it.id }
        val idx = sorted.indexOf(layerId)
        if (idx == -1 || idx >= sorted.size - 1) return

        val reordered = sorted.toMutableList()
        reordered[idx] = sorted[idx + 1]
        reordered[idx + 1] = sorted[idx]

        val updated = historyManager.execute(ReorderLayersCommand(sorted, reordered), currentDoc)
        _uiState.update {
            it.copy(
                document = updated,
                canUndo = historyManager.canUndo,
                canRedo = historyManager.canRedo
            )
        }
        tileCacheManager.invalidateAll()
        triggerDebouncedAutosave()
    }

    fun moveLayerDown(layerId: String) {
        val currentDoc = _uiState.value.document
        val sorted = currentDoc.layers.sortedBy { it.order }.map { it.id }
        val idx = sorted.indexOf(layerId)
        if (idx <= 0) return

        val reordered = sorted.toMutableList()
        reordered[idx] = sorted[idx - 1]
        reordered[idx - 1] = sorted[idx]

        val updated = historyManager.execute(ReorderLayersCommand(sorted, reordered), currentDoc)
        _uiState.update {
            it.copy(
                document = updated,
                canUndo = historyManager.canUndo,
                canRedo = historyManager.canRedo
            )
        }
        tileCacheManager.invalidateAll()
        triggerDebouncedAutosave()
    }

    fun renameLayer(layerId: String, newName: String) {
        val currentDoc = _uiState.value.document
        val layer = currentDoc.layers.find { it.id == layerId } ?: return
        val updatedLayer = layer.copy(name = newName.trim().ifBlank { layer.name })
        val updated = historyManager.execute(
            ChangeLayerPropsCommand(layerId, layer, updatedLayer),
            currentDoc
        )
        _uiState.update {
            it.copy(
                document = updated,
                canUndo = historyManager.canUndo,
                canRedo = historyManager.canRedo
            )
        }
        triggerDebouncedAutosave()
    }

    fun toggleLayerVisibility(layerId: String) {
        val currentDoc = _uiState.value.document
        val layer = currentDoc.layers.find { it.id == layerId } ?: return
        val updatedLayer = layer.copy(isVisible = !layer.isVisible)
        val updated = historyManager.execute(
            ChangeLayerPropsCommand(layerId, layer, updatedLayer),
            currentDoc
        )
        _uiState.update {
            it.copy(
                document = updated,
                canUndo = historyManager.canUndo,
                canRedo = historyManager.canRedo
            )
        }
        tileCacheManager.invalidateAll()
        triggerDebouncedAutosave()
    }

    fun toggleLayerLock(layerId: String) {
        val currentDoc = _uiState.value.document
        val layer = currentDoc.layers.find { it.id == layerId } ?: return
        val updatedLayer = layer.copy(isLocked = !layer.isLocked)
        val updated = historyManager.execute(
            ChangeLayerPropsCommand(layerId, layer, updatedLayer),
            currentDoc
        )
        _uiState.update {
            it.copy(
                document = updated,
                canUndo = historyManager.canUndo,
                canRedo = historyManager.canRedo
            )
        }
        triggerDebouncedAutosave()
    }

    fun setLayerOpacity(layerId: String, opacity: Float) {
        val currentDoc = _uiState.value.document
        val layer = currentDoc.layers.find { it.id == layerId } ?: return
        val updatedLayer = layer.copy(opacity = opacity.coerceIn(0f, 1f))
        val updated = historyManager.execute(
            ChangeLayerPropsCommand(layerId, layer, updatedLayer),
            currentDoc
        )
        _uiState.update {
            it.copy(
                document = updated,
                canUndo = historyManager.canUndo,
                canRedo = historyManager.canRedo
            )
        }
        tileCacheManager.invalidateAll()
        triggerDebouncedAutosave()
    }

    fun setActiveLayer(layerId: String) {
        _uiState.update { it.copy(activeLayerId = layerId) }
    }

    // ==========================================
    // Z8: Contextual Object Actions
    // ==========================================
    fun duplicateSelectedElements() {
        val state = _uiState.value
        val selectedIds = state.toolState.selectedElementIds
        if (selectedIds.isEmpty()) return

        val toDuplicate = state.document.elements.filter { it.id in selectedIds }
        if (toDuplicate.isEmpty()) return

        val maxZ = (state.document.elements.maxOfOrNull { it.zIndex } ?: 0) + 1
        val duplicated = toDuplicate.mapIndexed { idx, el ->
            when (el) {
                is StrokeElement -> el.copy(
                    id = UUID.randomUUID().toString(),
                    zIndex = maxZ + idx
                ).translated(24f, 24f)
                is ImageElement -> el.copy(
                    id = UUID.randomUUID().toString(),
                    zIndex = maxZ + idx
                ).translated(24f, 24f)
            }
        }

        val updated = historyManager.execute(
            AddElementsCommand(duplicated, description = "Duplicate Elements"),
            state.document
        )

        val newSelectedIds = duplicated.map { it.id }.toSet()
        toolController.toolState = toolController.toolState.copy(selectedElementIds = newSelectedIds)

        spatialIndex.rebuild(updated.elements)
        tileCacheManager.invalidateAll()
        _uiState.update {
            it.copy(
                document = updated,
                toolState = toolController.toolState,
                canUndo = historyManager.canUndo,
                canRedo = historyManager.canRedo
            )
        }
        triggerDebouncedAutosave()
    }

    fun deleteSelectedElements() {
        val state = _uiState.value
        val selectedIds = state.toolState.selectedElementIds
        if (selectedIds.isEmpty()) return

        val toDelete = state.document.elements.filter { it.id in selectedIds }
        if (toDelete.isEmpty()) return

        val updated = historyManager.execute(
            RemoveElementsCommand(toDelete, description = "Delete Elements"),
            state.document
        )

        toolController.toolState = toolController.toolState.copy(selectedElementIds = emptySet())
        spatialIndex.rebuild(updated.elements)
        tileCacheManager.invalidateAll()
        _uiState.update {
            it.copy(
                document = updated,
                toolState = toolController.toolState,
                canUndo = historyManager.canUndo,
                canRedo = historyManager.canRedo
            )
        }
        triggerDebouncedAutosave()
    }

    fun bringForwardSelection() {
        val state = _uiState.value
        val selectedIds = state.toolState.selectedElementIds
        if (selectedIds.isEmpty()) return

        val selected = state.document.elements.filter { it.id in selectedIds }
        val otherElements = state.document.elements.filter { it.id !in selectedIds }
        val currentMaxZ = selected.maxOfOrNull { it.zIndex } ?: 0
        val nextAbove = otherElements.filter { it.zIndex >= currentMaxZ }.minByOrNull { it.zIndex }

        val delta = if (nextAbove != null) {
            (nextAbove.zIndex + 1) - currentMaxZ
        } else {
            1
        }

        val modified = selected.map { it.withZIndex(it.zIndex + delta) }

        val updated = historyManager.execute(
            TransformElementsCommand(before = selected, after = modified, description = "Bring Forward"),
            state.document
        )
        spatialIndex.rebuild(updated.elements)
        tileCacheManager.invalidateAll()
        _uiState.update {
            it.copy(
                document = updated,
                canUndo = historyManager.canUndo,
                canRedo = historyManager.canRedo
            )
        }
        triggerDebouncedAutosave()
    }

    fun sendBackwardSelection() {
        val state = _uiState.value
        val selectedIds = state.toolState.selectedElementIds
        if (selectedIds.isEmpty()) return

        val selected = state.document.elements.filter { it.id in selectedIds }
        val otherElements = state.document.elements.filter { it.id !in selectedIds }
        val currentMinZ = selected.minOfOrNull { it.zIndex } ?: 0
        val nextBelow = otherElements.filter { it.zIndex <= currentMinZ }.maxByOrNull { it.zIndex }

        val targetZ = if (nextBelow != null) {
            (nextBelow.zIndex - 1).coerceAtLeast(0)
        } else {
            (currentMinZ - 1).coerceAtLeast(0)
        }
        val delta = targetZ - currentMinZ

        val modified = selected.map { it.withZIndex((it.zIndex + delta).coerceAtLeast(0)) }

        val updated = historyManager.execute(
            TransformElementsCommand(before = selected, after = modified, description = "Send Backward"),
            state.document
        )
        spatialIndex.rebuild(updated.elements)
        tileCacheManager.invalidateAll()
        _uiState.update {
            it.copy(
                document = updated,
                canUndo = historyManager.canUndo,
                canRedo = historyManager.canRedo
            )
        }
        triggerDebouncedAutosave()
    }

    fun moveSelectionToLayer(targetLayerId: String) {
        val state = _uiState.value
        val selectedIds = state.toolState.selectedElementIds
        if (selectedIds.isEmpty()) return

        val activeLayer = state.activeLayerId
        val updated = historyManager.execute(
            MoveElementsToLayerCommand(
                elementIds = selectedIds,
                fromLayerId = activeLayer,
                toLayerId = targetLayerId
            ),
            state.document
        )
        spatialIndex.rebuild(updated.elements)
        tileCacheManager.invalidateAll()
        _uiState.update {
            it.copy(
                document = updated,
                canUndo = historyManager.canUndo,
                canRedo = historyManager.canRedo
            )
        }
        triggerDebouncedAutosave()
    }

    fun onElementsChanged(newDoc: CanvasDocument) {
        spatialIndex.rebuild(newDoc.elements)
        tileCacheManager.invalidateAll()
        _uiState.update { it.copy(document = newDoc) }
    }

    fun setSelectedElementIds(ids: Set<String>) {
        toolController.toolState = toolController.toolState.copy(selectedElementIds = ids)
        _uiState.update { it.copy(toolState = toolController.toolState) }
    }

    // ==========================================
    // C10-D: Image Attachment & Import
    // ==========================================
    fun addImage(
        uri: Uri,
        viewport: ViewportState,
        screenWidth: Float,
        screenHeight: Float
    ) {
        val canvasId = _uiState.value.canvasId ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val assetInfo = canvasAssetManager.importImage(uri, canvasId)
            if (assetInfo == null) {
                _uiState.update {
                    it.copy(userMessage = "Failed to import image. File may be unsupported or corrupted.")
                }
                return@launch
            }

            // Place image centered on current visible viewport
            val (centerX, centerY) = ViewportTransform.screenToWorld(
                screenWidth / 2f,
                screenHeight / 2f,
                viewport
            )

            val displayWidth = minOf(assetInfo.width.toFloat(), 600f)
            val displayHeight = (assetInfo.height.toFloat() / assetInfo.width.toFloat()) * displayWidth

            val imageElement = ImageElement(
                id = UUID.randomUUID().toString(),
                layerId = _uiState.value.activeLayerId,
                assetId = assetInfo.assetId,
                x = centerX - displayWidth / 2f,
                y = centerY - displayHeight / 2f,
                width = displayWidth,
                height = displayHeight,
                bounds = CanvasRect(
                    left = centerX - displayWidth / 2f,
                    top = centerY - displayHeight / 2f,
                    right = centerX + displayWidth / 2f,
                    bottom = centerY + displayHeight / 2f
                ),
                zIndex = (_uiState.value.document.elements.maxOfOrNull { it.zIndex } ?: 0) + 1
            )

            val updated = historyManager.execute(
                AddElementsCommand(listOf(imageElement), description = "Add Image"),
                _uiState.value.document
            )

            spatialIndex.rebuild(updated.elements)
            tileCacheManager.invalidateAll()

            toolController.toolState = toolController.toolState.copy(
                activeTool = CanvasToolType.SELECT,
                selectedElementIds = setOf(imageElement.id)
            )

            _uiState.update {
                it.copy(
                    document = updated,
                    toolState = toolController.toolState,
                    canUndo = historyManager.canUndo,
                    canRedo = historyManager.canRedo
                )
            }
            triggerDebouncedAutosave()
        }
    }

    // ==========================================
    // C10-E: High-Fidelity PNG Export & Share
    // ==========================================
    fun exportCanvas(
        context: Context,
        exportSelectionOnly: Boolean,
        scale: Float
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isExporting = true, exportProgress = 0.1f) }
            try {
                val doc = _uiState.value.document
                val elementsToRender = if (exportSelectionOnly && toolController.toolState.selectedElementIds.isNotEmpty()) {
                    doc.elements.filter { it.id in toolController.toolState.selectedElementIds }
                } else {
                    doc.elements
                }

                if (elementsToRender.isEmpty()) {
                    _uiState.update { it.copy(isExporting = false, userMessage = "No elements to export.") }
                    return@launch
                }

                var bounds = CanvasRect.Empty
                for (el in elementsToRender) {
                    bounds = bounds.union(el.bounds)
                }

                // Add 32dp padding
                val paddedBounds = CanvasRect(
                    left = bounds.left - 32f,
                    top = bounds.top - 32f,
                    right = bounds.right + 32f,
                    bottom = bounds.bottom + 32f
                )

                _uiState.update { it.copy(exportProgress = 0.3f) }

                // Safe pixel size calculation to prevent OOM
                val maxDim = 4096f
                var targetW = ceil(paddedBounds.width * scale).toInt().coerceAtLeast(100)
                var targetH = ceil(paddedBounds.height * scale).toInt().coerceAtLeast(100)

                if (targetW > maxDim || targetH > maxDim) {
                    val ratio = maxDim / max(targetW, targetH)
                    targetW = (targetW * ratio).toInt()
                    targetH = (targetH * ratio).toInt()
                }

                val bitmap = try {
                    Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
                } catch (_: OutOfMemoryError) {
                    // Half-resolution fallback
                    Bitmap.createBitmap(targetW / 2, targetH / 2, Bitmap.Config.RGB_565)
                }

                val canvas = android.graphics.Canvas(bitmap)
                // Fill background
                canvas.drawColor(0xFF0D1B2A.toInt()) // MidnightNavy

                val scaleX = bitmap.width.toFloat() / paddedBounds.width
                val scaleY = bitmap.height.toFloat() / paddedBounds.height

                canvas.save()
                canvas.scale(scaleX, scaleY)
                canvas.translate(-paddedBounds.left, -paddedBounds.top)

                val layerMap = doc.layers.associateBy { it.id }
                val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                }
                val imgPaint = Paint(Paint.ANTI_ALIAS_FLAG)

                _uiState.update { it.copy(exportProgress = 0.6f) }

                for (el in elementsToRender) {
                    val layer = layerMap[el.layerId] ?: continue
                    if (!layer.isVisible) continue

                    when (el) {
                        is StrokeElement -> {
                            strokePaint.color = el.color.toInt()
                            strokePaint.strokeWidth = el.width
                            strokePaint.alpha = (255 * layer.opacity).toInt().coerceIn(0, 255)

                            val pts = el.points
                            if (pts.size >= 2) {
                                for (i in 0 until (pts.size - 1)) {
                                    canvas.drawLine(pts[i].x, pts[i].y, pts[i + 1].x, pts[i + 1].y, strokePaint)
                                }
                            } else if (pts.size == 1) {
                                canvas.drawCircle(pts[0].x, pts[0].y, el.width / 2f, strokePaint)
                            }
                        }
                        is ImageElement -> {
                            val file = canvasAssetManager.getAssetFile(el.assetId)
                            if (file.exists()) {
                                try {
                                    val img = BitmapFactory.decodeFile(file.absolutePath)
                                    if (img != null) {
                                        canvas.save()
                                        canvas.translate(el.x, el.y)
                                        if (el.rotationDegrees != 0f) {
                                            canvas.rotate(el.rotationDegrees, el.width / 2f, el.height / 2f)
                                        }
                                        val src = Rect(0, 0, img.width, img.height)
                                        val dst = RectF(0f, 0f, el.width, el.height)
                                        imgPaint.alpha = (255 * layer.opacity).toInt().coerceIn(0, 255)
                                        canvas.drawBitmap(img, src, dst, imgPaint)
                                        canvas.restore()
                                        img.recycle()
                                    }
                                } catch (_: Exception) {}
                            }
                        }
                    }
                }

                canvas.restore()

                _uiState.update { it.copy(exportProgress = 0.8f) }

                val exportDir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
                val exportFile = File(exportDir, "Canvas_${System.currentTimeMillis()}.png")
                FileOutputStream(exportFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                bitmap.recycle()

                _uiState.update { it.copy(exportProgress = 1.0f, isExporting = false, showExportDialog = false) }

                // Share through FileProvider
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    exportFile
                )

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(shareIntent, "Share Canvas PNG").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        userMessage = "Export failed: ${e.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
        }
    }

    // Dialog & UI Visibility Toggles
    fun setLayersPanelVisible(visible: Boolean) = _uiState.update { it.copy(showLayersPanel = visible) }
    fun setRenameDialogVisible(visible: Boolean) = _uiState.update { it.copy(showRenameDialog = visible) }
    fun setBackgroundPickerVisible(visible: Boolean) = _uiState.update { it.copy(showBackgroundPicker = visible) }
    fun setSettingsDialogVisible(visible: Boolean) = _uiState.update { it.copy(showSettingsDialog = visible) }
    fun setScheduleDialogVisible(visible: Boolean) = _uiState.update { it.copy(showScheduleDialog = visible) }
    fun setInfoDialogVisible(visible: Boolean) = _uiState.update { it.copy(showInfoDialog = visible) }
    fun setExportDialogVisible(visible: Boolean) = _uiState.update { it.copy(showExportDialog = visible) }
    fun setMoveDialogVisible(visible: Boolean) = _uiState.update { it.copy(showMoveDialog = visible) }
    fun setDeleteConfirmDialogVisible(visible: Boolean) = _uiState.update { it.copy(showDeleteConfirmDialog = visible) }
    fun setImageSourceDialogVisible(visible: Boolean) = _uiState.update { it.copy(showImageSourceDialog = visible) }
    fun setExistingNotesDialogVisible(visible: Boolean) = _uiState.update { it.copy(showExistingNotesDialog = visible) }
    fun dismissExperimentalNotice() = _uiState.update { it.copy(showExperimentalNotice = false) }
    fun clearUserMessage() = _uiState.update { it.copy(userMessage = null) }
}

class CanvasViewModelFactory(
    private val canvasId: Long?,
    private val folderId: Long,
    private val subfolderId: Long?,
    private val canvasNoteRepository: CanvasNoteRepository,
    private val canvasRepository: CanvasRepository,
    private val canvasAssetManager: CanvasAssetManager,
    private val photoRepository: PhotoRepository? = null,
    private val folderRepository: FolderRepository? = null,
    private val settingsRepository: SettingsRepository? = null,
    private val scheduleManager: NoteScheduleManager? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return CanvasViewModel(
            initialCanvasId = canvasId,
            initialFolderId = folderId,
            initialSubfolderId = subfolderId,
            canvasNoteRepository = canvasNoteRepository,
            canvasRepository = canvasRepository,
            canvasAssetManager = canvasAssetManager,
            photoRepository = photoRepository,
            folderRepository = folderRepository,
            settingsRepository = settingsRepository,
            scheduleManager = scheduleManager
        ) as T
    }
}
