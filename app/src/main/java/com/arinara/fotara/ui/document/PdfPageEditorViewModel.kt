// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.document

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arinara.fotara.canvas.engine.DrawingConstants
import com.arinara.fotara.canvas.model.StrokeElement
import com.arinara.fotara.canvas.model.TextLayerElement
import com.arinara.fotara.data.model.DocumentNote
import com.arinara.fotara.data.model.PdfPageDrawing
import com.arinara.fotara.data.repository.DocumentRepository
import com.arinara.fotara.data.repository.PdfPageDrawingRepository
import com.arinara.fotara.util.PdfPageRenderer
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
import kotlin.math.abs

data class PdfPageEditorUiState(
    val isLoading: Boolean = true,
    val documentTitle: String = "",
    val totalPages: Int = 1,
    val pageIndex: Int = 0,
    val pagePointsWidth: Float = 595.28f,
    val pagePointsHeight: Float = 841.89f,
    val strokes: List<StrokeElement> = emptyList(),
    val textLayers: List<TextLayerElement> = emptyList(),
    val isDrawingVisible: Boolean = true,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val errorMessage: String? = null,
    val sizeMismatchWarning: Boolean = false
)

class PdfPageEditorViewModel(
    val documentId: Long,
    val pageIndex: Int,
    private val documentRepository: DocumentRepository?,
    private val pdfPageDrawingRepository: PdfPageDrawingRepository?
) : ViewModel() {

    private val _uiState = MutableStateFlow(PdfPageEditorUiState(pageIndex = pageIndex))
    val uiState: StateFlow<PdfPageEditorUiState> = _uiState.asStateFlow()

    private var pdfRenderer: PdfPageRenderer? = null
    private val undoStack = ArrayDeque<List<StrokeElement>>()
    private val redoStack = ArrayDeque<List<StrokeElement>>()
    private var autosaveJob: Job? = null

    init {
        loadDocumentAndDrawing()
    }

    private fun loadDocumentAndDrawing() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val doc = documentRepository?.getDocumentNoteById(documentId)
                if (doc == null) {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Document not found") }
                    return@launch
                }

                val file = File(doc.originFileUri)
                if (!file.exists()) {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "PDF file not found") }
                    return@launch
                }

                val renderer = try {
                    PdfPageRenderer(file)
                } catch (e: Exception) {
                    Log.e("PdfPageEditor", "Failed to initialize PdfPageRenderer", e)
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Failed to open PDF document") }
                    null
                }

                pdfRenderer = renderer
                var wPts = 595.28f
                var hPts = 841.89f
                var count = 1
                if (renderer != null) {
                    count = renderer.pageCount.coerceAtLeast(1)
                    val (w, h) = renderer.getPageSizePoints(pageIndex)
                    if (w > 0f && h > 0f) {
                        wPts = w
                        hPts = h
                    }
                }

                val initialDrawing = pdfPageDrawingRepository?.getDrawing(documentId, pageIndex)
                val initialStrokes = initialDrawing?.strokes ?: emptyList()
                val initialTextLayers = initialDrawing?.textLayers ?: emptyList()
                val initialVisible = initialDrawing?.isVisible ?: true
                var sizeMismatch = false

                if (initialDrawing != null && initialDrawing.pageWidth > 0f && initialDrawing.pageHeight > 0f) {
                    val wDelta = abs(initialDrawing.pageWidth - wPts)
                    val hDelta = abs(initialDrawing.pageHeight - hPts)
                    if (wDelta > 10f || hDelta > 10f) {
                        sizeMismatch = true
                    }
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        documentTitle = doc.title,
                        totalPages = count,
                        pagePointsWidth = wPts,
                        pagePointsHeight = hPts,
                        strokes = initialStrokes,
                        textLayers = initialTextLayers,
                        isDrawingVisible = initialVisible,
                        canUndo = false,
                        canRedo = false,
                        sizeMismatchWarning = sizeMismatch
                    )
                }
            } catch (e: Exception) {
                Log.e("PdfPageEditor", "Error loading document or drawing", e)
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Failed to load document") }
            }
        }
    }

    suspend fun renderPage(pageIndex: Int, destWidth: Int, destHeight: Int, renderScale: Float): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                pdfRenderer?.renderPage(pageIndex, destWidth, destHeight, renderScale)
            } catch (e: Exception) {
                Log.e("PdfPageEditor", "Failed to render PDF page bitmap", e)
                null
            }
        }
    }

    fun addStroke(stroke: StrokeElement, previousStrokesSnapshot: List<StrokeElement>?) {
        if (previousStrokesSnapshot != null) {
            undoStack.addLast(previousStrokesSnapshot)
            if (undoStack.size > DrawingConstants.MAX_UNDO_STEPS) {
                undoStack.removeFirst()
            }
            redoStack.clear()
        }
        _uiState.update { current ->
            val updated = current.strokes + stroke
            current.copy(
                strokes = updated,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty()
            )
        }
        scheduleAutosave()
    }

    fun updateStrokesAfterEraser(newStrokes: List<StrokeElement>, previousStrokesSnapshot: List<StrokeElement>?) {
        if (previousStrokesSnapshot != null && previousStrokesSnapshot != newStrokes) {
            undoStack.addLast(previousStrokesSnapshot)
            if (undoStack.size > DrawingConstants.MAX_UNDO_STEPS) {
                undoStack.removeFirst()
            }
            redoStack.clear()
        }
        _uiState.update { current ->
            current.copy(
                strokes = newStrokes,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty()
            )
        }
        scheduleAutosave()
    }

    fun addTextLayer(textLayer: TextLayerElement) {
        _uiState.update { current ->
            val updated = current.textLayers + textLayer
            current.copy(textLayers = updated)
        }
        scheduleAutosave()
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            redoStack.addLast(_uiState.value.strokes)
            val previous = undoStack.removeLast()
            _uiState.update { current ->
                current.copy(
                    strokes = previous,
                    canUndo = undoStack.isNotEmpty(),
                    canRedo = redoStack.isNotEmpty()
                )
            }
            scheduleAutosave()
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            undoStack.addLast(_uiState.value.strokes)
            val next = redoStack.removeLast()
            _uiState.update { current ->
                current.copy(
                    strokes = next,
                    canUndo = undoStack.isNotEmpty(),
                    canRedo = redoStack.isNotEmpty()
                )
            }
            scheduleAutosave()
        }
    }

    fun setVisible(visible: Boolean) {
        _uiState.update { it.copy(isDrawingVisible = visible) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                pdfPageDrawingRepository?.setVisible(documentId, pageIndex, visible)
            } catch (e: Exception) {
                Log.e("PdfPageEditor", "Failed to update drawing visibility", e)
            }
        }
    }

    fun clearDrawing() {
        undoStack.addLast(_uiState.value.strokes)
        if (undoStack.size > DrawingConstants.MAX_UNDO_STEPS) {
            undoStack.removeFirst()
        }
        redoStack.clear()
        _uiState.update { current ->
            current.copy(
                strokes = emptyList(),
                textLayers = emptyList(),
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty()
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                pdfPageDrawingRepository?.clearDrawing(documentId, pageIndex)
            } catch (e: Exception) {
                Log.e("PdfPageEditor", "Failed to clear drawing", e)
            }
        }
    }

    private fun scheduleAutosave() {
        autosaveJob?.cancel()
        autosaveJob = viewModelScope.launch(Dispatchers.IO) {
            delay(DrawingConstants.AUTOSAVE_DEBOUNCE_MS)
            flushSave()
        }
    }

    fun flushSave() {
        val state = _uiState.value
        val drawing = PdfPageDrawing(
            documentId = documentId,
            pageIndex = pageIndex,
            strokes = state.strokes,
            textLayers = state.textLayers,
            pageWidth = state.pagePointsWidth,
            pageHeight = state.pagePointsHeight,
            isVisible = state.isDrawingVisible,
            updatedAt = System.currentTimeMillis()
        )
        viewModelScope.launch(Dispatchers.IO) {
            try {
                pdfPageDrawingRepository?.saveDrawing(drawing)
            } catch (e: Exception) {
                Log.e("PdfPageEditor", "Failed to save PDF drawing", e)
            }
        }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearSizeMismatchWarning() {
        _uiState.update { it.copy(sizeMismatchWarning = false) }
    }

    override fun onCleared() {
        super.onCleared()
        flushSave()
        pdfRenderer?.close()
        pdfRenderer = null
    }
}

class PdfPageEditorViewModelFactory(
    private val documentId: Long,
    private val pageIndex: Int,
    private val documentRepository: DocumentRepository?,
    private val pdfPageDrawingRepository: PdfPageDrawingRepository?
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PdfPageEditorViewModel::class.java)) {
            return PdfPageEditorViewModel(
                documentId = documentId,
                pageIndex = pageIndex,
                documentRepository = documentRepository,
                pdfPageDrawingRepository = pdfPageDrawingRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
