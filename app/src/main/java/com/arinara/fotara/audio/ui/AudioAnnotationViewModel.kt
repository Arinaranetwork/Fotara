// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.audio.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arinara.fotara.audio.model.AudioAnnotation
import com.arinara.fotara.audio.player.AudioPlayerManager
import com.arinara.fotara.audio.player.AudioPlayerState
import com.arinara.fotara.audio.recorder.AudioRecorderManager
import com.arinara.fotara.audio.recorder.AudioRecorderState
import com.arinara.fotara.audio.repository.AudioAnnotationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AudioAnnotationUiState(
    val annotations: List<AudioAnnotation> = emptyList(),
    val recorderState: AudioRecorderState = AudioRecorderState.Idle,
    val playerState: AudioPlayerState = AudioPlayerState.Idle,
    val selectedPdfPageIndex: Int? = null,
    val errorMessage: String? = null
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AudioAnnotationViewModel(
    private val repository: AudioAnnotationRepository,
    val recorderManager: AudioRecorderManager,
    val playerManager: AudioPlayerManager,
    private val noteId: Long? = null,
    private val pdfDocId: Long? = null
) : ViewModel() {

    private val _selectedPdfPageIndex = MutableStateFlow<Int?>(null)
    private val _errorMessage = MutableStateFlow<String?>(null)

    private val annotationsFlow = _selectedPdfPageIndex.flatMapLatest { pageIndex ->
        when {
            pdfDocId != null && pageIndex != null -> {
                repository.getAnnotationsForPdfPage(pdfDocId, pageIndex)
            }
            pdfDocId != null -> {
                repository.getAllAnnotationsForPdf(pdfDocId)
            }
            noteId != null -> {
                repository.getAnnotationsForNote(noteId)
            }
            else -> {
                kotlinx.coroutines.flow.flowOf(emptyList())
            }
        }
    }

    val uiState: StateFlow<AudioAnnotationUiState> = combine(
        annotationsFlow,
        recorderManager.recorderState,
        playerManager.playerState,
        _selectedPdfPageIndex,
        _errorMessage
    ) { annotations, recorderState, playerState, pageIndex, errorMsg ->
        AudioAnnotationUiState(
            annotations = annotations,
            recorderState = recorderState,
            playerState = playerState,
            selectedPdfPageIndex = pageIndex,
            errorMessage = errorMsg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AudioAnnotationUiState()
    )

    fun setPdfPageFilter(pageIndex: Int?) {
        _selectedPdfPageIndex.value = pageIndex
    }

    fun startRecording(): Boolean {
        playerManager.stop()
        return recorderManager.startRecording()
    }

    fun stopAndSaveRecording(targetPdfPageIndex: Int? = null) {
        val finalized = recorderManager.stopRecording()
        if (finalized != null) {
            viewModelScope.launch {
                val annotation = AudioAnnotation(
                    noteId = noteId,
                    pdfDocId = pdfDocId,
                    pdfPageIndex = targetPdfPageIndex ?: _selectedPdfPageIndex.value,
                    filePath = finalized.filePath,
                    durationMs = finalized.totalDurationMs
                )
                repository.insertAnnotation(annotation)
                recorderManager.resetStateToIdle()
            }
        }
    }

    fun cancelRecording() {
        recorderManager.cancelRecording()
    }

    fun playAnnotation(annotation: AudioAnnotation) {
        playerManager.play(annotation.filePath)
    }

    fun pausePlayback() {
        playerManager.pause()
    }

    fun resumePlayback() {
        playerManager.resume()
    }

    fun seekPlayback(positionMs: Long) {
        playerManager.seekTo(positionMs)
    }

    fun stopPlayback() {
        playerManager.stop()
    }

    fun deleteAnnotation(id: Long) {
        viewModelScope.launch {
            playerManager.stop()
            repository.deleteAnnotation(id)
        }
    }

    fun dismissError() {
        _errorMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        recorderManager.safeFinalizeOnInterruption()
        playerManager.release()
    }
}

class AudioAnnotationViewModelFactory(
    private val repository: AudioAnnotationRepository,
    private val recorderManager: AudioRecorderManager,
    private val playerManager: AudioPlayerManager,
    private val noteId: Long? = null,
    private val pdfDocId: Long? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AudioAnnotationViewModel(
            repository = repository,
            recorderManager = recorderManager,
            playerManager = playerManager,
            noteId = noteId,
            pdfDocId = pdfDocId
        ) as T
    }
}
