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
import kotlinx.coroutines.SupervisorJob
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
    data class Recording(
        val elapsedMs: Long,
        val filePath: String,
        val amplitude: Int = 0,
        val isPaused: Boolean = false
    ) : AudioRecorderState
    data class Finalized(val totalDurationMs: Long, val filePath: String) : AudioRecorderState
    data class Error(val message: String) : AudioRecorderState
}

class AudioRecorderManager(
    context: Context,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
) {
    private val context = context.applicationContext
    private val lock = Any()

    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var recordingStartTime: Long = 0L
    private var lastResumeTimeMs: Long = 0L
    private var accumulatedDurationMs: Long = 0L
    private var isPaused = false
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
    fun startRecording(): Boolean = synchronized(lock) {
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
            mediaRecorder = recorder

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

            isCurrentlyRecording = true
            isPaused = false
            accumulatedDurationMs = 0L
            lastResumeTimeMs = System.currentTimeMillis()
            recordingStartTime = lastResumeTimeMs

            startDurationTimer(outputFile.absolutePath)
            Log.i(TAG, "Audio recording started at: ${outputFile.absolutePath}")
            return true

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start audio recording", e)
            _recorderState.value = AudioRecorderState.Error("Failed to start recording: ${e.message}")
            currentOutputFile?.let { file ->
                if (file.exists() && file.length() == 0L) {
                    file.delete()
                }
            }
            currentOutputFile = null
            cleanupRecorderResources()
            return false
        }
    }

    /**
     * Pauses the active recording session.
     */
    fun pauseRecording() {
        synchronized(lock) {
            if (!isCurrentlyRecording || isPaused || mediaRecorder == null) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    mediaRecorder?.pause()
                    isPaused = true
                    accumulatedDurationMs += (System.currentTimeMillis() - lastResumeTimeMs)
                    val file = currentOutputFile
                    val path = file?.absolutePath ?: ""
                    _recorderState.value = AudioRecorderState.Recording(
                        elapsedMs = accumulatedDurationMs,
                        filePath = path,
                        amplitude = 0,
                        isPaused = true
                    )
                    Log.i(TAG, "Audio recording paused at ${accumulatedDurationMs}ms")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to pause MediaRecorder", e)
                }
            }
        }
    }

    /**
     * Resumes the paused recording session.
     */
    fun resumeRecording() {
        synchronized(lock) {
            if (!isCurrentlyRecording || !isPaused || mediaRecorder == null) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    mediaRecorder?.resume()
                    isPaused = false
                    lastResumeTimeMs = System.currentTimeMillis()
                    val file = currentOutputFile
                    val path = file?.absolutePath ?: ""
                    _recorderState.value = AudioRecorderState.Recording(
                        elapsedMs = accumulatedDurationMs,
                        filePath = path,
                        amplitude = 0,
                        isPaused = false
                    )
                    Log.i(TAG, "Audio recording resumed at ${accumulatedDurationMs}ms")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to resume MediaRecorder", e)
                }
            }
        }
    }

    /**
     * Stops recording and returns finalized audio info.
     */
    fun stopRecording(): AudioRecorderState.Finalized? = synchronized(lock) {
        if (!isCurrentlyRecording || mediaRecorder == null) {
            Log.w(TAG, "No active recording to stop")
            return null
        }

        timerJob?.cancel()
        timerJob = null

        val currentResumeDuration = if (isPaused) 0L else (System.currentTimeMillis() - lastResumeTimeMs)
        val finalDurationMs = (accumulatedDurationMs + currentResumeDuration).coerceAtMost(MAX_RECORDING_DURATION_MS)
        val file = currentOutputFile
        val recorder = mediaRecorder
        mediaRecorder = null
        isCurrentlyRecording = false
        isPaused = false

        var stopSuccessful = false
        if (recorder != null) {
            try {
                recorder.stop()
                stopSuccessful = true
            } catch (e: Exception) {
                Log.e(TAG, "Exception while stopping MediaRecorder", e)
            } finally {
                try {
                    recorder.release()
                } catch (e: Exception) {
                    Log.e(TAG, "Exception while releasing MediaRecorder", e)
                }
            }
        }

        accumulatedDurationMs = 0L

        return if (stopSuccessful && file != null && file.exists() && file.length() > 0) {
            val finalizedState = AudioRecorderState.Finalized(
                totalDurationMs = finalDurationMs,
                filePath = file.absolutePath
            )
            _recorderState.value = finalizedState
            Log.i(TAG, "Audio recording finalized successfully: ${file.absolutePath}, duration: ${finalDurationMs}ms")
            finalizedState
        } else {
            file?.let { if (it.exists() && it.length() == 0L) it.delete() }
            currentOutputFile = null
            Log.e(TAG, "Recorded audio file is missing, empty, or stop failed")
            _recorderState.value = AudioRecorderState.Error("Recording failed or was too short")
            null
        }
    }

    /**
     * Handles interruptions (phone call, screen off, background transition, exception)
     * by safely finalizing and saving whatever audio data has been captured so far.
     */
    fun safeFinalizeOnInterruption() {
        synchronized(lock) {
            if (!isCurrentlyRecording) return

            timerJob?.cancel()
            timerJob = null

            val currentResumeDuration = if (isPaused) 0L else (System.currentTimeMillis() - lastResumeTimeMs)
            val finalDurationMs = (accumulatedDurationMs + currentResumeDuration).coerceAtMost(MAX_RECORDING_DURATION_MS)
            val file = currentOutputFile
            val recorder = mediaRecorder
            mediaRecorder = null
            isCurrentlyRecording = false
            isPaused = false

            try {
                recorder?.stop()
            } catch (e: Exception) {
                Log.e(TAG, "Exception during emergency stop on interruption", e)
            } finally {
                try {
                    recorder?.release()
                } catch (e: Exception) {
                    Log.e(TAG, "Exception during emergency release on interruption", e)
                }
            }

            accumulatedDurationMs = 0L

            if (file != null && file.exists() && file.length() > 0) {
                val finalizedState = AudioRecorderState.Finalized(
                    totalDurationMs = finalDurationMs,
                    filePath = file.absolutePath
                )
                _recorderState.value = finalizedState
                Log.i(TAG, "Safely finalized interrupted recording: ${file.absolutePath}, duration: ${finalDurationMs}ms")
            } else {
                file?.let { if (it.exists() && it.length() == 0L) it.delete() }
                currentOutputFile = null
                _recorderState.value = AudioRecorderState.Idle
            }
        }
    }

    /**
     * Cancels the active recording session and deletes the partially recorded file.
     */
    fun cancelRecording() {
        synchronized(lock) {
            if (!isCurrentlyRecording && mediaRecorder == null) return

            timerJob?.cancel()
            timerJob = null
            val file = currentOutputFile
            currentOutputFile = null

            cleanupRecorderResources()

            if (file != null && file.exists()) {
                file.delete()
                Log.i(TAG, "Audio recording cancelled and file deleted: ${file.absolutePath}")
            }
            _recorderState.value = AudioRecorderState.Idle
        }
    }

    fun resetStateToIdle() {
        synchronized(lock) {
            _recorderState.value = AudioRecorderState.Idle
        }
    }

    private fun startDurationTimer(filePath: String) {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (isActive) {
                val (elapsed, isRecording, paused) = synchronized(lock) {
                    if (!isCurrentlyRecording) return@launch
                    val currentResumeDuration = if (isPaused) 0L else (System.currentTimeMillis() - lastResumeTimeMs)
                    val total = accumulatedDurationMs + currentResumeDuration
                    Triple(total, isCurrentlyRecording, isPaused)
                }

                if (!isRecording) break

                if (!paused) {
                    if (elapsed >= MAX_RECORDING_DURATION_MS) {
                        stopRecording()
                        break
                    }
                    val amp = try {
                        synchronized(lock) { mediaRecorder?.maxAmplitude ?: 0 }
                    } catch (_: Exception) {
                        0
                    }
                    synchronized(lock) {
                        if (isCurrentlyRecording && !isPaused) {
                            _recorderState.value = AudioRecorderState.Recording(
                                elapsedMs = elapsed,
                                filePath = filePath,
                                amplitude = amp,
                                isPaused = false
                            )
                        }
                    }
                }
                delay(TICK_INTERVAL_MS)
            }
        }
    }

    private fun cleanupRecorderResources() {
        timerJob?.cancel()
        timerJob = null
        val recorder = mediaRecorder
        mediaRecorder = null
        if (recorder != null) {
            try {
                if (isCurrentlyRecording) {
                    try {
                        recorder.stop()
                    } catch (e: Exception) {
                        Log.w(TAG, "Error stopping recorder during cleanup", e)
                    }
                }
            } finally {
                try {
                    recorder.release()
                } catch (e: Exception) {
                    Log.e(TAG, "Error releasing MediaRecorder", e)
                }
            }
        }
        isCurrentlyRecording = false
        isPaused = false
        accumulatedDurationMs = 0L
    }

    fun release() = synchronized(lock) {
        cleanupRecorderResources()
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
