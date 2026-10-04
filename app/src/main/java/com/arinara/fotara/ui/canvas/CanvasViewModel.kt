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
import com.arinara.fotara.canvas.engine.AddLayerAndElementsCommand
import com.arinara.fotara.canvas.engine.AddLayerCommand
import com.arinara.fotara.canvas.engine.CanvasConfig
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
import com.arinara.fotara.canvas.model.CanvasSelection
import com.arinara.fotara.canvas.model.ImageElement
import com.arinara.fotara.canvas.model.SelectedElementReference
import com.arinara.fotara.canvas.model.StrokeElement
import com.arinara.fotara.canvas.model.StrokeInsideSegment
import com.arinara.fotara.canvas.model.StrokeToolType
import com.arinara.fotara.canvas.persistence.CanvasAssetInfo
import com.arinara.fotara.canvas.persistence.CanvasAssetManager
import com.arinara.fotara.canvas.persistence.CanvasRepository
import com.arinara.fotara.canvas.render.CanvasRenderer
import com.arinara.fotara.canvas.render.TileCacheManager
import com.arinara.fotara.canvas.tool.CanvasToolController
import com.arinara.fotara.canvas.tool.CanvasToolState
import com.arinara.fotara.canvas.tool.CanvasToolType
import com.arinara.fotara.data.model.Folder
import com.arinara.fotara.data.model.Photo
import com.arinara.fotara.data.model.Workspace
import com.arinara.fotara.data.repository.CanvasNoteRepository
import com.arinara.fotara.data.repository.FolderRepository
import com.arinara.fotara.data.repository.PhotoRepository
import com.arinara.fotara.data.repository.SettingsRepository
import com.arinara.fotara.data.repository.WorkspaceRepository
import com.arinara.fotara.util.NoteScheduleManager
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    val existingImageNotes: List<Photo> = emptyList(),
    val isLoadingExistingNotes: Boolean = false,
    val showExperimentalNotice: Boolean = false,
    val isExporting: Boolean = false,
    val exportProgress: Float = 0f,
    val scheduledAt: Long? = null,
    val alertType: String? = null,
    val userMessage: String? = null,
    val folders: List<Folder> = emptyList(),
    val workspaces: List<Workspace> = emptyList()
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
    private val scheduleManager: NoteScheduleManager? = null,
    private val workspaceRepository: WorkspaceRepository? = null
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
    private val saveMutex = Mutex()
    @Volatile
    private var isDirty: Boolean = false
    private var retryAttempt: Int = 0

    init {
        // Load folders for Move action
        folderRepository?.let { repo ->
            viewModelScope.launch {
                repo.getFolders().collect { folderList ->
                    _uiState.update { it.copy(folders = folderList) }
                }
            }
        }

        workspaceRepository?.let { repo ->
            viewModelScope.launch {
                repo.observeWorkspaces().collect { wsList ->
                    _uiState.update { it.copy(workspaces = wsList) }
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
                toolController.activeLayerId = primaryLayerId
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
            toolController.activeLayerId = initialDoc.getPrimaryLayerId()
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

    fun triggerDebouncedAutosave() {
        isDirty = true
        _uiState.update { it.copy(saveState = SaveState.SAVING) }
        autosaveJob?.cancel()
        autosaveJob = viewModelScope.launch(Dispatchers.IO) {
            delay(500) // 500ms debounce window
            performSave()
        }
    }

    private suspend fun performSave(): Boolean = withContext(NonCancellable + Dispatchers.IO) {
        val currentId = _uiState.value.canvasId ?: return@withContext false
        saveMutex.withLock {
            if (!isDirty && _uiState.value.saveState == SaveState.SAVED) {
                return@withContext true
            }
            val currentDoc = _uiState.value.document
            _uiState.update { it.copy(saveState = SaveState.SAVING) }
            try {
                canvasRepository.saveDocumentImmediate(currentId, currentDoc)
                isDirty = false
                retryAttempt = 0
                _uiState.update { it.copy(saveState = SaveState.SAVED) }
                true
            } catch (e: Exception) {
                android.util.Log.e("CanvasAutosave", "Autosave failed: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        saveState = SaveState.ERROR,
                        userMessage = "Autosave failed: ${e.localizedMessage ?: "Storage error"}. Tap status to retry."
                    )
                }
                if (retryAttempt < 3) {
                    retryAttempt++
                    val backoffMs = 1000L * (1 shl retryAttempt)
                    autosaveJob = viewModelScope.launch(Dispatchers.IO) {
                        delay(backoffMs)
                        performSave()
                    }
                }
                false
            }
        }
    }

    /**
     * Immediately flushes any pending or dirty changes to persistent storage without debounce delay.
     * Guaranteed to run to completion even if calling scope is cancelled.
     */
    suspend fun flushSave(): Boolean = withContext(NonCancellable + Dispatchers.IO) {
        autosaveJob?.cancel()
        if (!isDirty && _uiState.value.saveState == SaveState.SAVED) {
            return@withContext true
        }
        performSave()
    }

    /**
     * User-initiated tap-to-retry on save failure indicator.
     */
    fun retrySave() {
        autosaveJob?.cancel()
        retryAttempt = 0
        viewModelScope.launch(Dispatchers.IO) {
            performSave()
        }
    }

    // ==========================================
    // Z2: Operations (Undo / Redo / Export / Settings)
    // ==========================================
    fun undo() {
        val currentDoc = _uiState.value.document
        val undoneDoc = historyManager.undo(currentDoc) ?: return
        toolController.clearSelection()
        spatialIndex.rebuild(undoneDoc.elements)
        tileCacheManager.invalidateAll()
        _uiState.update {
            it.copy(
                document = undoneDoc,
                toolState = toolController.toolState,
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
        toolController.clearSelection()
        spatialIndex.rebuild(redoneDoc.elements)
        tileCacheManager.invalidateAll()
        _uiState.update {
            it.copy(
                document = redoneDoc,
                toolState = toolController.toolState,
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
        toolController.toolState = toolController.toolState.copy(activeTool = tool)
        pointerStateMachine.activeMode = when (tool) {
            CanvasToolType.SELECT -> com.arinara.fotara.canvas.gesture.ActiveMode.SELECT
            CanvasToolType.ERASER -> com.arinara.fotara.canvas.gesture.ActiveMode.ERASE
            else -> com.arinara.fotara.canvas.gesture.ActiveMode.DRAW
        }

        _uiState.update {
            it.copy(
                toolState = toolController.toolState,
                showToolOptions = false
            )
        }
    }

    fun showPenOptions() {
        setTool(CanvasToolType.PEN)
        _uiState.update { it.copy(showToolOptions = true) }
    }

    fun showHighlighterOptions() {
        setTool(CanvasToolType.HIGHLIGHTER)
        _uiState.update { it.copy(showToolOptions = true) }
    }

    fun showEraserOptions() {
        setTool(CanvasToolType.ERASER)
        _uiState.update { it.copy(showToolOptions = true) }
    }

    fun setHighlighterBlendMode(mode: com.arinara.fotara.canvas.model.StrokeBlendMode) {
        toolController.setHighlighterBlendMode(mode)
        _uiState.update { it.copy(toolState = toolController.toolState) }
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
    fun addDefaultLayer() {
        val currentDoc = _uiState.value.document
        val defaultName = com.arinara.fotara.canvas.model.CanvasLayerNaming.generateNextDefaultLayerName(
            currentDoc.layers.map { it.name }
        )
        addLayer(defaultName)
    }

    fun addLayer(layerName: String) {
        val currentDoc = _uiState.value.document
        if (currentDoc.layers.size >= CanvasConfig.MAX_LAYERS) {
            _uiState.update { it.copy(userMessage = "Maximum layer limit reached (${CanvasConfig.MAX_LAYERS} layers).") }
            return
        }
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
        toolController.activeLayerId = layerId
        _uiState.update { it.copy(activeLayerId = layerId) }
    }

    // ==========================================
    // Z8: Contextual Object Actions
    // ==========================================
    fun updateSelection(selection: CanvasSelection) {
        toolController.selection = selection
        _uiState.update {
            it.copy(toolState = toolController.toolState)
        }
    }

    fun setSelectedElementIds(ids: Set<String>) {
        val currentDoc = _uiState.value.document
        val refs = ids.associateWith { id ->
            SelectedElementReference.Whole(id)
        }

        val elements = refs.keys.mapNotNull { id -> currentDoc.elements.find { it.id == id } }
        val selBounds = if (elements.isEmpty()) {
            CanvasRect.Empty
        } else {
            elements.map { it.bounds }.reduce { acc, rect -> acc.union(rect) }
        }
        updateSelection(CanvasSelection(references = refs, bounds = selBounds))
    }

    fun duplicateSelectedElements() {
        val state = _uiState.value
        if (toolController.selection.isEmpty) return

        val (updatedDoc, dirtyBounds) = toolController.duplicateSelection(
            document = state.document,
            historyManager = historyManager
        )
        if (dirtyBounds != null) {
            spatialIndex.rebuild(updatedDoc.elements)
            tileCacheManager.invalidateRegion(dirtyBounds)
            _uiState.update {
                it.copy(
                    document = updatedDoc,
                    toolState = toolController.toolState,
                    canUndo = historyManager.canUndo,
                    canRedo = historyManager.canRedo
                )
            }
            triggerDebouncedAutosave()
        }
    }

    fun deleteSelectedElements() {
        val state = _uiState.value
        if (toolController.selection.isEmpty) return

        val (updatedDoc, dirtyBounds) = toolController.deleteSelection(
            document = state.document,
            historyManager = historyManager
        )
        if (dirtyBounds != null) {
            spatialIndex.rebuild(updatedDoc.elements)
            tileCacheManager.invalidateRegion(dirtyBounds)
            _uiState.update {
                it.copy(
                    document = updatedDoc,
                    toolState = toolController.toolState,
                    canUndo = historyManager.canUndo,
                    canRedo = historyManager.canRedo
                )
            }
            triggerDebouncedAutosave()
        }
    }

    fun moveSelectionToLayer(targetLayerId: String) {
        val state = _uiState.value
        if (toolController.selection.isEmpty) return

        val (updatedDoc, dirtyBounds) = toolController.moveSelectionToLayer(
            targetLayerId = targetLayerId,
            document = state.document,
            historyManager = historyManager
        )
        if (dirtyBounds != null) {
            spatialIndex.rebuild(updatedDoc.elements)
            tileCacheManager.invalidateRegion(dirtyBounds)
            _uiState.update {
                it.copy(
                    document = updatedDoc,
                    toolState = toolController.toolState,
                    canUndo = historyManager.canUndo,
                    canRedo = historyManager.canRedo
                )
            }
            triggerDebouncedAutosave()
        }
    }

    fun onElementsChanged(newDoc: CanvasDocument) {
        spatialIndex.rebuild(newDoc.elements)
        tileCacheManager.invalidateAll()
        _uiState.update { it.copy(document = newDoc) }
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
        val currentDoc = _uiState.value.document
        if (currentDoc.elements.size >= CanvasConfig.MAX_TOTAL_ELEMENTS) {
            _uiState.update { it.copy(userMessage = "Maximum element limit reached (${CanvasConfig.MAX_TOTAL_ELEMENTS} elements).") }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val assetInfo = canvasAssetManager.importImage(uri, canvasId)
            if (assetInfo == null) {
                _uiState.update {
                    it.copy(userMessage = "Failed to import image. File may be unsupported or corrupted.")
                }
                return@launch
            }

            insertAssetOnActiveLayer(assetInfo, viewport, screenWidth, screenHeight)
        }
    }

    fun loadExistingNotes() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoadingExistingNotes = true, showImageSourceDialog = false, showExistingNotesDialog = true) }
            val photos = try {
                photoRepository?.getAllActivePhotos() ?: emptyList()
            } catch (e: Exception) {
                android.util.Log.e("CanvasViewModel", "Failed to load existing photo notes: ${e.message}", e)
                emptyList()
            }
            _uiState.update {
                it.copy(
                    existingImageNotes = photos,
                    isLoadingExistingNotes = false
                )
            }
        }
    }

    fun addFromExistingPhoto(
        photo: Photo,
        viewport: ViewportState,
        screenWidth: Float,
        screenHeight: Float
    ) {
        val canvasId = _uiState.value.canvasId ?: return
        val currentDoc = _uiState.value.document
        if (currentDoc.elements.size >= CanvasConfig.MAX_TOTAL_ELEMENTS) {
            _uiState.update {
                it.copy(
                    userMessage = "Maximum element limit reached (${CanvasConfig.MAX_TOTAL_ELEMENTS} elements).",
                    showExistingNotesDialog = false
                )
            }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(showExistingNotesDialog = false) }

            val file = File(photo.fileUri)
            val assetInfo = if (file.exists()) {
                canvasAssetManager.importImageFile(file, canvasId)
            } else {
                val uri = Uri.parse(photo.fileUri)
                canvasAssetManager.importImage(uri, canvasId)
            }

            if (assetInfo == null) {
                _uiState.update {
                    it.copy(userMessage = "Failed to load image from note.")
                }
                return@launch
            }

            insertAssetOnActiveLayer(assetInfo, viewport, screenWidth, screenHeight)
        }
    }

    private fun insertAssetOnActiveLayer(
        assetInfo: CanvasAssetInfo,
        viewport: ViewportState,
        screenWidth: Float,
        screenHeight: Float
    ) {
        val currentDoc = _uiState.value.document
        val targetLayerId = _uiState.value.activeLayerId.ifEmpty {
            currentDoc.getPrimaryLayerId()
        }

        // Center image in current visible viewport
        val (centerX, centerY) = ViewportTransform.screenToWorld(
            screenWidth / 2f,
            screenHeight / 2f,
            viewport
        )

        val zoom = if (viewport.scale > 0f) viewport.scale else 1f
        val visibleWidthWorld = screenWidth / zoom
        val visibleHeightWorld = screenHeight / zoom

        // Longest side is about 60% of visible area (never larger than canvas extent)
        val targetLongestSide = (0.6f * minOf(visibleWidthWorld, visibleHeightWorld))
            .coerceIn(48f, CanvasConfig.CANVAS_EXTENT_WIDTH)

        val assetW = assetInfo.width.toFloat()
        val assetH = assetInfo.height.toFloat()

        val displayWidth: Float
        val displayHeight: Float
        if (assetW >= assetH && assetW > 0f) {
            displayWidth = targetLongestSide
            displayHeight = targetLongestSide * (assetH / assetW)
        } else if (assetH > 0f) {
            displayHeight = targetLongestSide
            displayWidth = targetLongestSide * (assetW / assetH)
        } else {
            displayWidth = targetLongestSide
            displayHeight = targetLongestSide
        }

        val nextZ = CanvasToolController.nextZIndexForLayer(currentDoc, targetLayerId)

        val imageElement = ImageElement(
            id = UUID.randomUUID().toString(),
            layerId = targetLayerId,
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
            zIndex = nextZ
        )

        val updated = historyManager.execute(
            AddElementsCommand(listOf(imageElement), description = "Insert Image"),
            currentDoc
        )

        spatialIndex.rebuild(updated.elements)
        tileCacheManager.invalidateAll()

        val imgRef = SelectedElementReference.Whole(imageElement.id)
        val sel = CanvasSelection(
            references = mapOf(imageElement.id to imgRef),
            bounds = imageElement.bounds,
            rotationDegrees = imageElement.rotationDegrees
        )
        toolController.selection = sel
        toolController.toolState = toolController.toolState.copy(
            activeTool = CanvasToolType.SELECT
        )

        _uiState.update {
            it.copy(
                document = updated,
                activeLayerId = targetLayerId,
                toolState = toolController.toolState,
                canUndo = historyManager.canUndo,
                canRedo = historyManager.canRedo
            )
        }
        triggerDebouncedAutosave()
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
    private val scheduleManager: NoteScheduleManager? = null,
    private val workspaceRepository: WorkspaceRepository? = null
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
            scheduleManager = scheduleManager,
            workspaceRepository = workspaceRepository
        ) as T
    }
}
