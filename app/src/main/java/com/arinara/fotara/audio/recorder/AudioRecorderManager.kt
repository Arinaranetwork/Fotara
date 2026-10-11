// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.audio.recorder

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface AudioRecorderState {
    object Idle : AudioRecorderState
    data class Recording(val elapsedMs: Long, val filePath: String) : AudioRecorderState
    data class Finalized(val totalDurationMs: Long, val filePath: String) : AudioRecorderState
    data class Error(val message: String) : AudioRecorderState
}

class AudioRecorderManager(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {

    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var recordingStartTime: Long = 0L
    private var timerJob: Job? = null
    private var isCurrentlyRecording = false

    private val _recorderState = MutableStateFlow<AudioRecorderState>(AudioRecorderState.Idle)
    val recorderState: StateFlow<AudioRecorderState> = _recorderState.asStateFlow()

    fun getAudioStorageDirectory(): File {
        val audioDir = File(context.filesDir, AUDIO_DIRECTORY_NAME)
        if (!audioDir.exists()) {
            audioDir.mkdirs()
        }
        return audioDir
    }

    /**
     * Starts audio recording in AAC (.m4a) with a hard cap of 5 minutes (300 seconds).
     * Returns true if recording started successfully, false otherwise.
     */
    fun startRecording(): Boolean {
        if (isCurrentlyRecording) {
            Log.w(TAG, "Recording already in progress")
            return false
        }

        try {
            val audioDir = getAudioStorageDirectory()
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val outputFile = File(audioDir, "audio_note_$timeStamp.m4a")
            currentOutputFile = outputFile

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioChannels(1) // Mono
                setAudioEncodingBitRate(AUDIO_BIT_RATE) // 64kbps
                setAudioSamplingRate(AUDIO_SAMPLE_RATE) // 44.1kHz
                setOutputFile(outputFile.absolutePath)
                
                // Info listener for max duration reached
                setMaxDuration(MAX_RECORDING_DURATION_MS.toInt())
                setOnInfoListener { _, what, _ ->
                    if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                        Log.i(TAG, "Max recording duration of 5 minutes reached, finalizing recording")
                        stopRecording()
                    }
                }
                
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaRecorder error: what=$what, extra=$extra")
                    safeFinalizeOnInterruption()
                }

                prepare()
                start()
            }

            mediaRecorder = recorder
            isCurrentlyRecording = true
            recordingStartTime = System.currentTimeMillis()

            startDurationTimer(outputFile.absolutePath)
            Log.i(TAG, "Audio recording started at: ${outputFile.absolutePath}")
            return true

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start audio recording", e)
            _recorderState.value = AudioRecorderState.Error("Failed to start recording: ${e.message}")
            cleanupRecorderResources()
            return false
        }
    }

    /**
     * Stops the ongoing recording gracefully and finalizes the audio file.
     */
    fun stopRecording(): AudioRecorderState.Finalized? {
        if (!isCurrentlyRecording || mediaRecorder == null) {
            Log.w(TAG, "No active recording to stop")
            return null
        }

        timerJob?.cancel()
        timerJob = null

        val finalDurationMs = (System.currentTimeMillis() - recordingStartTime).coerceAtMost(MAX_RECORDING_DURATION_MS)
        val file = currentOutputFile

        return try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isCurrentlyRecording = false

            if (file != null && file.exists() && file.length() > 0) {
                val finalizedState = AudioRecorderState.Finalized(
                    totalDurationMs = finalDurationMs,
                    filePath = file.absolutePath
                )
                _recorderState.value = finalizedState
                Log.i(TAG, "Audio recording finalized successfully: ${file.absolutePath}, duration: ${finalDurationMs}ms")
                finalizedState
            } else {
                Log.e(TAG, "Recorded audio file is missing or empty")
                _recorderState.value = AudioRecorderState.Error("Recorded file is empty")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception while stopping MediaRecorder", e)
            safeFinalizeOnInterruption()
            null
        } finally {
            cleanupRecorderResources()
        }
    }

    /**
     * Handles interruptions (phone call, screen off, background transition, exception)
     * by safely finalizing and saving whatever audio data has been captured so far.
     */
    fun safeFinalizeOnInterruption() {
        if (!isCurrentlyRecording) return

        timerJob?.cancel()
        timerJob = null

        val finalDurationMs = (System.currentTimeMillis() - recordingStartTime).coerceAtMost(MAX_RECORDING_DURATION_MS)
        val file = currentOutputFile

        try {
            mediaRecorder?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Exception during emergency stop on interruption", e)
        }

        cleanupRecorderResources()

        if (file != null && file.exists() && file.length() > 0) {
            _recorderState.value = AudioRecorderState.Finalized(
                totalDurationMs = finalDurationMs,
                filePath = file.absolutePath
            )
            Log.i(TAG, "Safely saved partial recording upon interruption: ${file.absolutePath}")
        } else {
            _recorderState.value = AudioRecorderState.Error("Recording interrupted with zero bytes saved")
        }
    }

    /**
     * Cancels and discards the current recording.
     */
    fun cancelRecording() {
        timerJob?.cancel()
        timerJob = null

        try {
            mediaRecorder?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Exception while discarding recording", e)
        }
        cleanupRecorderResources()

        currentOutputFile?.let { file ->
            if (file.exists()) {
                file.delete()
            }
        }
        currentOutputFile = null
        _recorderState.value = AudioRecorderState.Idle
    }

    fun resetStateToIdle() {
        _recorderState.value = AudioRecorderState.Idle
    }

    private fun startDurationTimer(filePath: String) {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (isActive && isCurrentlyRecording) {
                val elapsed = System.currentTimeMillis() - recordingStartTime
                if (elapsed >= MAX_RECORDING_DURATION_MS) {
                    stopRecording()
                    break
                }
                _recorderState.value = AudioRecorderState.Recording(
                    elapsedMs = elapsed,
                    filePath = filePath
                )
                delay(TICK_INTERVAL_MS)
            }
        }
    }

    private fun cleanupRecorderResources() {
        try {
            mediaRecorder?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing MediaRecorder", e)
        }
        mediaRecorder = null
        isCurrentlyRecording = false
    }

    companion object {
        private const val TAG = "AudioRecorderManager"
        const val AUDIO_DIRECTORY_NAME = "audio"
        const val MAX_RECORDING_DURATION_MS = 300_000L // 5 minutes (300 seconds)
        const val AUDIO_BIT_RATE = 64_000 // 64kbps
        const val AUDIO_SAMPLE_RATE = 44_100 // 44.1kHz
        private const val TICK_INTERVAL_MS = 200L

        fun formatDuration(ms: Long): String {
            val totalSeconds = (ms / 1000).coerceAtLeast(0)
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }
}
