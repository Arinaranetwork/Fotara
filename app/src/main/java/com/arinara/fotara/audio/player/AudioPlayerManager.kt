// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.audio.player

import android.media.AudioAttributes
import android.media.MediaPlayer
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
import java.io.FileInputStream
import java.util.Locale

sealed interface AudioPlayerState {
    object Idle : AudioPlayerState
    data class Playing(val currentPositionMs: Long, val totalDurationMs: Long, val filePath: String) : AudioPlayerState
    data class Paused(val currentPositionMs: Long, val totalDurationMs: Long, val filePath: String) : AudioPlayerState
    data class Completed(val filePath: String, val totalDurationMs: Long) : AudioPlayerState
    data class Error(val message: String) : AudioPlayerState
}

class AudioPlayerManager(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {

    private var mediaPlayer: MediaPlayer? = null
    private var currentFilePath: String? = null
    private var progressTrackingJob: Job? = null

    private val _playerState = MutableStateFlow<AudioPlayerState>(AudioPlayerState.Idle)
    val playerState: StateFlow<AudioPlayerState> = _playerState.asStateFlow()

    fun play(filePath: String): Boolean {
        val file = File(filePath)
        if (!file.exists() || file.length() == 0L) {
            val errorMsg = "Cannot play non-existent or empty audio file: $filePath"
            Log.e(TAG, errorMsg)
            _playerState.value = AudioPlayerState.Error(errorMsg)
            return false
        }

        stop()

        return try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                FileInputStream(file).use { fis ->
                    setDataSource(fis.fd)
                }
                prepare()
            }

            currentFilePath = filePath
            mediaPlayer = player

            val duration = player.duration.toLong().coerceAtLeast(0L)

            player.setOnCompletionListener {
                Log.i(TAG, "Audio playback completed: $filePath")
                progressTrackingJob?.cancel()
                _playerState.value = AudioPlayerState.Completed(filePath, duration)
            }

            player.setOnErrorListener { _, what, extra ->
                val errorMsg = "MediaPlayer error: what=$what, extra=$extra"
                Log.e(TAG, errorMsg)
                progressTrackingJob?.cancel()
                _playerState.value = AudioPlayerState.Error(errorMsg)
                true
            }

            player.start()
            _playerState.value = AudioPlayerState.Playing(0L, duration, filePath)
            startProgressTracking(duration, filePath)
            Log.i(TAG, "Audio playback started: $filePath, duration: ${duration}ms")
            true
        } catch (e: Exception) {
            val errorMsg = "Failed to start audio playback: ${e.message}"
            Log.e(TAG, errorMsg, e)
            _playerState.value = AudioPlayerState.Error(errorMsg)
            release()
            false
        }
    }

    fun pause() {
        try {
            val player = mediaPlayer ?: return
            if (player.isPlaying) {
                player.pause()
                val currentPos = player.currentPosition.toLong()
                val duration = player.duration.toLong()
                val path = currentFilePath ?: ""
                progressTrackingJob?.cancel()
                _playerState.value = AudioPlayerState.Paused(currentPos, duration, path)
                Log.i(TAG, "Audio playback paused at ${currentPos}ms")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pause MediaPlayer", e)
        }
    }

    fun resume() {
        try {
            val player = mediaPlayer ?: return
            val path = currentFilePath ?: return
            if (!player.isPlaying) {
                player.start()
                val duration = player.duration.toLong()
                startProgressTracking(duration, path)
                Log.i(TAG, "Audio playback resumed at ${player.currentPosition}ms")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resume MediaPlayer", e)
        }
    }

    fun seekTo(positionMs: Long) {
        try {
            val player = mediaPlayer ?: return
            val target = positionMs.coerceIn(0L, player.duration.toLong())
            player.seekTo(target.toInt())
            val duration = player.duration.toLong()
            val path = currentFilePath ?: ""

            if (player.isPlaying) {
                _playerState.value = AudioPlayerState.Playing(target, duration, path)
            } else {
                _playerState.value = AudioPlayerState.Paused(target, duration, path)
            }
            Log.d(TAG, "Seek to position: ${target}ms")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to seek in MediaPlayer", e)
        }
    }

    fun stop() {
        progressTrackingJob?.cancel()
        progressTrackingJob = null
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping MediaPlayer", e)
        } finally {
            mediaPlayer = null
            _playerState.value = AudioPlayerState.Idle
        }
    }

    fun release() {
        stop()
        currentFilePath = null
    }

    private fun startProgressTracking(duration: Long, filePath: String) {
        progressTrackingJob?.cancel()
        progressTrackingJob = scope.launch {
            while (isActive) {
                val player = mediaPlayer
                if (player != null && player.isPlaying) {
                    val pos = player.currentPosition.toLong()
                    _playerState.value = AudioPlayerState.Playing(pos, duration, filePath)
                }
                delay(SCRUBBER_POLL_INTERVAL_MS)
            }
        }
    }

    companion object {
        private const val TAG = "AudioPlayerManager"
        private const val SCRUBBER_POLL_INTERVAL_MS = 100L

        fun formatDuration(ms: Long): String {
            val totalSeconds = (ms / 1000).coerceAtLeast(0)
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }
}
