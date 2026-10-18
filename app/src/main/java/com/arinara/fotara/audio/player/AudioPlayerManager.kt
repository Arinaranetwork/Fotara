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
    private val lock = Any()
    private var mediaPlayer: MediaPlayer? = null
    private var progressTrackingJob: Job? = null
    private var currentFilePath: String? = null

    private val _playerState = MutableStateFlow<AudioPlayerState>(AudioPlayerState.Idle)
    val playerState: StateFlow<AudioPlayerState> = _playerState.asStateFlow()

    fun play(filePath: String): Boolean = synchronized(lock) {
        val file = File(filePath)
        if (!file.exists() || file.length() == 0L) {
            val errorMsg = "Cannot play non-existent or empty audio file: $filePath"
            Log.e(TAG, errorMsg)
            _playerState.value = AudioPlayerState.Error(errorMsg)
            return false
        }

        stopInternal()

        var playerToCleanup: MediaPlayer? = null
        return try {
            val player = MediaPlayer().apply {
                playerToCleanup = this
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(file.absolutePath)
            }
            player.prepare()

            mediaPlayer = player
            playerToCleanup = null
            currentFilePath = filePath

            val duration = player.duration.toLong().coerceAtLeast(0L)

            player.setOnCompletionListener {
                Log.i(TAG, "Audio playback completed: $filePath")
                synchronized(lock) {
                    progressTrackingJob?.cancel()
                    progressTrackingJob = null
                    _playerState.value = AudioPlayerState.Completed(filePath, duration)
                }
            }

            player.setOnErrorListener { _, what, extra ->
                val errorMsg = "MediaPlayer error: what=$what, extra=$extra"
                Log.e(TAG, errorMsg)
                synchronized(lock) {
                    stopInternal()
                    _playerState.value = AudioPlayerState.Error(errorMsg)
                }
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
            try {
                playerToCleanup?.release()
            } catch (_: Exception) {}
            stopInternal()
            _playerState.value = AudioPlayerState.Error(errorMsg)
            false
        }
    }

    fun pause() {
        synchronized(lock) {
            try {
                val player = mediaPlayer ?: return
                val isPlaying = try { player.isPlaying } catch (_: Exception) { false }
                if (isPlaying) {
                    player.pause()
                    val currentPos = try { player.currentPosition.toLong() } catch (_: Exception) { 0L }
                    val duration = try { player.duration.toLong() } catch (_: Exception) { 0L }
                    val path = currentFilePath ?: ""
                    progressTrackingJob?.cancel()
                    progressTrackingJob = null
                    _playerState.value = AudioPlayerState.Paused(currentPos, duration, path)
                    Log.i(TAG, "Audio playback paused at ${currentPos}ms")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to pause MediaPlayer", e)
            }
        }
    }

    fun resume() {
        synchronized(lock) {
            try {
                val player = mediaPlayer ?: return
                val path = currentFilePath ?: return
                val isPlaying = try { player.isPlaying } catch (_: Exception) { false }
                if (!isPlaying) {
                    player.start()
                    val duration = try { player.duration.toLong() } catch (_: Exception) { 0L }
                    startProgressTracking(duration, path)
                    Log.i(TAG, "Audio playback resumed")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to resume MediaPlayer", e)
            }
        }
    }

    fun seekTo(positionMs: Long) {
        synchronized(lock) {
            try {
                val player = mediaPlayer ?: return
                val totalDur = try { player.duration.toLong() } catch (_: Exception) { 0L }
                val target = positionMs.coerceIn(0L, totalDur)
                player.seekTo(target.toInt())
                val path = currentFilePath ?: ""

                val isPlaying = try { player.isPlaying } catch (_: Exception) { false }
                if (isPlaying) {
                    _playerState.value = AudioPlayerState.Playing(target, totalDur, path)
                } else {
                    _playerState.value = AudioPlayerState.Paused(target, totalDur, path)
                }
                Log.d(TAG, "Seek to position: ${target}ms")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to seek in MediaPlayer", e)
            }
        }
    }

    fun stop() {
        synchronized(lock) {
            stopInternal()
        }
    }

    private fun stopInternal() {
        progressTrackingJob?.cancel()
        progressTrackingJob = null
        val player = mediaPlayer
        mediaPlayer = null
        if (player != null) {
            try {
                try {
                    if (player.isPlaying) {
                        player.stop()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Exception while stopping MediaPlayer playback", e)
                } finally {
                    player.release()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing MediaPlayer", e)
            }
        }
        _playerState.value = AudioPlayerState.Idle
    }

    fun release() {
        synchronized(lock) {
            stopInternal()
            currentFilePath = null
        }
    }

    private fun startProgressTracking(duration: Long, filePath: String) {
        progressTrackingJob?.cancel()
        progressTrackingJob = scope.launch {
            while (isActive) {
                val player = synchronized(lock) { mediaPlayer }
                if (player == null) break

                val isPlaying = try {
                    player.isPlaying
                } catch (_: Exception) {
                    break
                }
                if (!isPlaying) break

                val pos = try {
                    player.currentPosition.toLong()
                } catch (_: Exception) {
                    break
                }

                _playerState.value = AudioPlayerState.Playing(pos, duration, filePath)
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
            return String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)
        }
    }
}
