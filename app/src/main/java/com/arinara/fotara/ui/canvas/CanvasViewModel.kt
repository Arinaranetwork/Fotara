// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.canvas

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Paint
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arinara.fotara.data.model.BackgroundStyle
import com.arinara.fotara.data.model.CanvasDocument
import com.arinara.fotara.data.model.CanvasImage
import com.arinara.fotara.data.model.CanvasLayer
import com.arinara.fotara.data.model.CanvasNote
import com.arinara.fotara.data.model.CanvasPoint
import com.arinara.fotara.data.model.CanvasStroke
import com.arinara.fotara.data.model.CanvasTool
import com.arinara.fotara.data.model.EraserMode
import com.arinara.fotara.data.model.NibProfile
import com.arinara.fotara.data.repository.CanvasNoteRepository
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

data class CanvasUiState(
    val canvasId: Long? = null,
    val folderId: Long = 0L,
    val subfolderId: Long? = null,
    val title: String = "Untitled Canvas",
    val document: CanvasDocument = CanvasDocument(),
    val activeLayerId: Int = 1,
    val activeTool: CanvasTool = CanvasTool.PEN,
    val strokeColor: Long = 0xFFEBD8B8, // FolderTabCream
    val strokeSize: Float = 4.0f,
    val strokeAlpha: Float = 1.0f,
    val nibProfile: NibProfile = NibProfile.BALLPOINT,
    val eraserMode: EraserMode = EraserMode.STROKE,
    val zoomScale: Float = 1.0f,
    val panOffset: Offset = Offset.Zero,
    val currentDrawingStroke: CanvasStroke? = null,
    val selectedStrokeIds: Set<String> = emptySet(),
    val selectedImageIds: Set<String> = emptySet(),
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = true,
    val showToolOptions: Boolean = false,
    val showLayersPanel: Boolean = false,
    val showRenameDialog: Boolean = false,
    val showBackgroundPicker: Boolean = false,
    val showScheduleDialog: Boolean = false,
    val showInfoDialog: Boolean = false,
    val userMessage: String? = null,
    val scheduledAt: Long? = null,
    val alertType: String? = null
)

class CanvasViewModel(
    private val initialCanvasId: Long?,
    private val folderId: Long,
    private val subfolderId: Long?,
    private val canvasNoteRepository: CanvasNoteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        CanvasUiState(
            canvasId = initialCanvasId,
            folderId = folderId,
            subfolderId = subfolderId
        )
    )
    val uiState: StateFlow<CanvasUiState> = _uiState.asStateFlow()

    private val undoStack = mutableListOf<CanvasDocument>()
    private val redoStack = mutableListOf<CanvasDocument>()
    private var autosaveJob: Job? = null

    init {
        if (initialCanvasId != null && initialCanvasId > 0) {
            loadExistingCanvas(initialCanvasId)
        } else {
            // New canvas - create record immediately
            createNewCanvasRecord()
        }
    }

    private fun loadExistingCanvas(id: Long) {
        viewModelScope.launch {
            val note = canvasNoteRepository.getCanvasNoteByIdOnce(id)
            if (note != null) {
                val doc = CanvasDocument.deserialize(note.dataBlob)
                _uiState.update {
                    it.copy(
                        canvasId = note.id,
                        title = note.title,
                        document = doc,
                        activeLayerId = doc.layers.firstOrNull()?.id ?: 1,
                        scheduledAt = note.scheduledAt,
                        alertType = note.alertType,
                        isSaved = true
                    )
                }
            }
        }
    }

    private fun createNewCanvasRecord() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val initialTitle = "Canvas Note"
            val initialDoc = CanvasDocument()
            val newId = canvasNoteRepository.createCanvasNote(
                folderId = folderId,
                subfolderId = subfolderId,
                title = initialTitle,
                dataBlob = initialDoc.serialize()
            )
            _uiState.update {
                it.copy(
                    canvasId = newId,
                    title = initialTitle,
                    document = initialDoc,
                    activeLayerId = 1,
                    isSaved = true
                )
            }
        }
    }

    private fun pushUndoState() {
        undoStack.add(_uiState.value.document)
        redoStack.clear()
        if (undoStack.size > 40) {
            undoStack.removeAt(0)
        }
        _uiState.update {
            it.copy(canUndo = true, canRedo = false, isSaved = false)
        }
        triggerDebouncedAutosave()
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val currentDoc = _uiState.value.document
        redoStack.add(currentDoc)
        val previousDoc = undoStack.removeAt(undoStack.size - 1)
        _uiState.update {
            it.copy(
                document = previousDoc,
                canUndo = undoStack.isNotEmpty(),
                canRedo = true,
                isSaved = false
            )
        }
        triggerDebouncedAutosave()
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val currentDoc = _uiState.value.document
        undoStack.add(currentDoc)
        val nextDoc = redoStack.removeAt(redoStack.size - 1)
        _uiState.update {
            it.copy(
                document = nextDoc,
                canUndo = true,
                canRedo = redoStack.isNotEmpty(),
                isSaved = false
            )
        }
        triggerDebouncedAutosave()
    }

    fun setTool(tool: CanvasTool) {
        _uiState.update {
            val togglePopup = if (it.activeTool == tool) !it.showToolOptions else false
            it.copy(activeTool = tool, showToolOptions = togglePopup)
        }
    }

    fun toggleToolOptions() {
        _uiState.update { it.copy(showToolOptions = !it.showToolOptions) }
    }

    fun setStrokeColor(color: Long) {
        _uiState.update { it.copy(strokeColor = color) }
    }

    fun setStrokeSize(size: Float) {
        _uiState.update { it.copy(strokeSize = size) }
    }

    fun setStrokeAlpha(alpha: Float) {
        _uiState.update { it.copy(strokeAlpha = alpha) }
    }

    fun setNibProfile(profile: NibProfile) {
        _uiState.update { it.copy(nibProfile = profile) }
    }

    fun setBackgroundStyle(style: BackgroundStyle) {
        pushUndoState()
        _uiState.update {
            it.copy(document = it.document.copy(backgroundStyle = style))
        }
        triggerDebouncedAutosave()
    }

    fun setZoomScale(scale: Float) {
        _uiState.update { it.copy(zoomScale = scale.coerceIn(0.1f, 5.0f)) }
    }

    fun updatePan(delta: Offset) {
        _uiState.update { it.copy(panOffset = it.panOffset + delta) }
    }

    fun resetZoom() {
        _uiState.update { it.copy(zoomScale = 1.0f, panOffset = Offset.Zero) }
    }

    fun fitToContent() {
        val doc = _uiState.value.document
        val allPoints = doc.layers.flatMap { it.strokes }.flatMap { it.points }
        if (allPoints.isEmpty()) {
            resetZoom()
            return
        }

        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE

        for (p in allPoints) {
            if (p.x < minX) minX = p.x
            if (p.y < minY) minY = p.y
            if (p.x > maxX) maxX = p.x
            if (p.y > maxY) maxY = p.y
        }

        val contentWidth = (maxX - minX).coerceAtLeast(100f)
        val contentHeight = (maxY - minY).coerceAtLeast(100f)
        val centerX = (minX + maxX) / 2f
        val centerY = (minY + maxY) / 2f

        // Fit comfortably
        _uiState.update {
            it.copy(
                zoomScale = 1.0f,
                panOffset = Offset(-centerX, -centerY)
            )
        }
    }

    // Touch Drawing Pipeline
    fun startDrawingStroke(point: CanvasPoint) {
        val state = _uiState.value
        val activeLayer = state.document.layers.find { it.id == state.activeLayerId }
        if (activeLayer == null || activeLayer.isLocked || !activeLayer.isVisible) return

        if (state.activeTool == CanvasTool.ERASER) {
            eraseAt(point)
            return
        }

        val newStroke = CanvasStroke(
            id = UUID.randomUUID().toString(),
            tool = state.activeTool,
            color = state.strokeColor,
            size = state.strokeSize,
            alpha = if (state.activeTool == CanvasTool.HIGHLIGHTER) 0.45f else state.strokeAlpha,
            nibProfile = state.nibProfile,
            points = listOf(point)
        )
        _uiState.update { it.copy(currentDrawingStroke = newStroke) }
    }

    fun appendDrawingPoint(point: CanvasPoint) {
        val state = _uiState.value
        if (state.activeTool == CanvasTool.ERASER) {
            eraseAt(point)
            return
        }

        val current = state.currentDrawingStroke ?: return
        val updatedPoints = current.points + point
        _uiState.update {
            it.copy(currentDrawingStroke = current.copy(points = updatedPoints))
        }
    }

    fun finishDrawingStroke() {
        val stroke = _uiState.value.currentDrawingStroke ?: return
        if (stroke.points.size >= 2) {
            pushUndoState()
            val state = _uiState.value
            val activeLayerId = state.activeLayerId
            val updatedLayers = state.document.layers.map { layer ->
                if (layer.id == activeLayerId) {
                    layer.copy(strokes = layer.strokes + stroke)
                } else layer
            }
            _uiState.update {
                it.copy(
                    document = it.document.copy(layers = updatedLayers),
                    currentDrawingStroke = null
                )
            }
            triggerDebouncedAutosave()
        } else {
            _uiState.update { it.copy(currentDrawingStroke = null) }
        }
    }

    private fun eraseAt(point: CanvasPoint) {
        val state = _uiState.value
        val activeLayerId = state.activeLayerId
        val layer = state.document.layers.find { it.id == activeLayerId } ?: return
        if (layer.isLocked || !layer.isVisible) return

        val eraseRadius = (state.strokeSize * 4f).coerceAtLeast(24f)
        val remainingStrokes = layer.strokes.filterNot { stroke ->
            stroke.points.any { p ->
                val dx = p.x - point.x
                val dy = p.y - point.y
                (dx * dx + dy * dy) <= (eraseRadius * eraseRadius)
            }
        }

        if (remainingStrokes.size != layer.strokes.size) {
            pushUndoState()
            val updatedLayers = state.document.layers.map {
                if (it.id == activeLayerId) it.copy(strokes = remainingStrokes) else it
            }
            _uiState.update {
                it.copy(document = it.document.copy(layers = updatedLayers))
            }
            triggerDebouncedAutosave()
        }
    }

    // Layers Management (Z5)
    fun addLayer(name: String) {
        pushUndoState()
        val doc = _uiState.value.document
        val newId = (doc.layers.maxOfOrNull { it.id } ?: 0) + 1
        val newLayer = CanvasLayer(id = newId, name = name.ifBlank { "Layer $newId" })
        _uiState.update {
            it.copy(
                document = doc.copy(layers = doc.layers + newLayer),
                activeLayerId = newId
            )
        }
        triggerDebouncedAutosave()
    }

    fun toggleLayerVisibility(layerId: Int) {
        pushUndoState()
        val doc = _uiState.value.document
        val updated = doc.layers.map {
            if (it.id == layerId) it.copy(isVisible = !it.isVisible) else it
        }
        _uiState.update { it.copy(document = doc.copy(layers = updated)) }
        triggerDebouncedAutosave()
    }

    fun toggleLayerLock(layerId: Int) {
        pushUndoState()
        val doc = _uiState.value.document
        val updated = doc.layers.map {
            if (it.id == layerId) it.copy(isLocked = !it.isLocked) else it
        }
        _uiState.update { it.copy(document = doc.copy(layers = updated)) }
        triggerDebouncedAutosave()
    }

    fun setLayerOpacity(layerId: Int, opacity: Float) {
        val doc = _uiState.value.document
        val updated = doc.layers.map {
            if (it.id == layerId) it.copy(opacity = opacity) else it
        }
        _uiState.update { it.copy(document = doc.copy(layers = updated)) }
        triggerDebouncedAutosave()
    }

    fun deleteLayer(layerId: Int) {
        val doc = _uiState.value.document
        if (doc.layers.size <= 1) return // Keep at least 1 layer
        pushUndoState()
        val updated = doc.layers.filterNot { it.id == layerId }
        _uiState.update {
            it.copy(
                document = doc.copy(layers = updated),
                activeLayerId = updated.first().id
            )
        }
        triggerDebouncedAutosave()
    }

    fun setActiveLayer(layerId: Int) {
        _uiState.update { it.copy(activeLayerId = layerId) }
    }

    // Image Insertion (Z2)
    fun addImageAttachment(uri: String) {
        pushUndoState()
        val state = _uiState.value
        val newImg = CanvasImage(
            imageUri = uri,
            x = -state.panOffset.x,
            y = -state.panOffset.y,
            width = 300f,
            height = 300f
        )
        val updatedLayers = state.document.layers.map {
            if (it.id == state.activeLayerId) it.copy(images = it.images + newImg) else it
        }
        _uiState.update {
            it.copy(document = state.document.copy(layers = updatedLayers))
        }
        triggerDebouncedAutosave()
    }

    fun renameCanvas(newTitle: String) {
        val clean = newTitle.trim()
        if (clean.isBlank()) return
        val id = _uiState.value.canvasId ?: return
        viewModelScope.launch {
            canvasNoteRepository.renameCanvasNote(id, clean)
            _uiState.update { it.copy(title = clean, showRenameDialog = false) }
        }
    }

    fun setLayersPanelVisible(visible: Boolean) {
        _uiState.update { it.copy(showLayersPanel = visible) }
    }

    fun setRenameDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(showRenameDialog = visible) }
    }

    fun setBackgroundPickerVisible(visible: Boolean) {
        _uiState.update { it.copy(showBackgroundPicker = visible) }
    }

    fun setScheduleDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(showScheduleDialog = visible) }
    }

    fun setInfoDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(showInfoDialog = visible) }
    }

    private fun triggerDebouncedAutosave() {
        _uiState.update { it.copy(isSaving = true, isSaved = false) }
        autosaveJob?.cancel()
        autosaveJob = viewModelScope.launch(Dispatchers.IO) {
            delay(400) // 400ms debounce
            val id = _uiState.value.canvasId ?: return@launch
            val blob = _uiState.value.document.serialize()
            canvasNoteRepository.updateCanvasNoteData(id, blob, thumbnailPath = null)
            _uiState.update { it.copy(isSaving = false, isSaved = true) }
        }
    }

    fun exportPng(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val doc = _uiState.value.document
                val allPoints = doc.layers.flatMap { it.strokes }.flatMap { it.points }
                val w = 1200
                val h = 1600
                val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bitmap)
                canvas.drawColor(android.graphics.Color.WHITE)

                val paint = Paint().apply {
                    isAntiAlias = true
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                    style = Paint.Style.STROKE
                }

                for (layer in doc.layers) {
                    if (!layer.isVisible) continue
                    paint.alpha = (layer.opacity * 255).toInt()
                    for (stroke in layer.strokes) {
                        paint.color = stroke.color.toInt()
                        paint.strokeWidth = stroke.size
                        if (stroke.tool == CanvasTool.HIGHLIGHTER) {
                            paint.alpha = (0.45f * layer.opacity * 255).toInt()
                        }
                        val pts = stroke.points
                        if (pts.size >= 2) {
                            val path = android.graphics.Path()
                            path.moveTo(pts[0].x, pts[0].y)
                            for (i in 1 until pts.size) {
                                path.lineTo(pts[i].x, pts[i].y)
                            }
                            canvas.drawPath(path, paint)
                        }
                    }
                }

                val exportFile = File(context.cacheDir, "export_${System.currentTimeMillis()}.png")
                FileOutputStream(exportFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                bitmap.recycle()

                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", exportFile)
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(shareIntent, "Share Canvas PNG")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            } catch (e: Exception) {
                android.util.Log.e("CanvasViewModel", "PNG export failed", e)
            }
        }
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }
}

class CanvasViewModelFactory(
    private val canvasId: Long?,
    private val folderId: Long,
    private val subfolderId: Long?,
    private val canvasNoteRepository: CanvasNoteRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return CanvasViewModel(
            initialCanvasId = canvasId,
            folderId = folderId,
            subfolderId = subfolderId,
            canvasNoteRepository = canvasNoteRepository
        ) as T
    }
}
