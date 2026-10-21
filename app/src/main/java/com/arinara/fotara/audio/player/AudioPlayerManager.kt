// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.audio.player

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
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

sealed interface AudioPlayerState {
    object Idle : AudioPlayerState
    data class Playing(val currentPositionMs: Long, val totalDurationMs: Long, val filePath: String) : AudioPlayerState
    data class Paused(val currentPositionMs: Long, val totalDurationMs: Long, val filePath: String) : AudioPlayerState
    data class Completed(val filePath: String, val totalDurationMs: Long) : AudioPlayerState
    data class Error(val message: String) : AudioPlayerState
}

class AudioPlayerManager(
    private val context: Context? = null,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    private val lock = Any()
    private var mediaPlayer: MediaPlayer? = null
    private var progressTrackingJob: Job? = null
    private var currentFilePath: String? = null

    private var audioFocusRequest: Any? = null
    private var isPausedDueToAudioFocus = false

    private val _playerState = MutableStateFlow<AudioPlayerState>(AudioPlayerState.Idle)
    val playerState: StateFlow<AudioPlayerState> = _playerState.asStateFlow()

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                synchronized(lock) {
                    if (_playerState.value is AudioPlayerState.Playing) {
                        isPausedDueToAudioFocus = true
                        pause()
                    }
                }
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                synchronized(lock) {
                    if (isPausedDueToAudioFocus) {
                        isPausedDueToAudioFocus = false
                        resume()
                    }
                }
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                synchronized(lock) {
                    isPausedDueToAudioFocus = false
                    pause()
                    abandonAudioFocus()
                }
            }
        }
    }

    private fun requestAudioFocus(): Boolean {
        val ctx = context ?: return true
        val audioManager = ctx.applicationContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return true
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val playbackAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(playbackAttributes)
                    .setAcceptsDelayedFocusGain(false)
                    .setOnAudioFocusChangeListener(audioFocusChangeListener)
                    .build()
                audioFocusRequest = request
                audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(
                    audioFocusChangeListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN
                ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            }
        } catch (e: Exception) {
            Log.w(TAG, "Audio focus request failed", e)
            true
        }
    }

    private fun abandonAudioFocus() {
        val ctx = context ?: return
        val audioManager = ctx.applicationContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                (audioFocusRequest as? AudioFocusRequest)?.let {
                    audioManager.abandonAudioFocusRequest(it)
                }
                audioFocusRequest = null
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(audioFocusChangeListener)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Audio focus abandon failed", e)
        }
    }

    fun play(filePath: String): Boolean = synchronized(lock) {
        val file = File(filePath)
        if (!file.exists() || file.length() == 0L) {
            val errorMsg = "Cannot play non-existent or empty audio file: $filePath"
            Log.e(TAG, errorMsg)
            _playerState.value = AudioPlayerState.Error(errorMsg)
            return false
        }

        stopInternal()
        requestAudioFocus()

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
                    abandonAudioFocus()
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
            // Cancel progress tracking job immediately so in-flight polling doesn't overwrite Paused state
            progressTrackingJob?.cancel()
            progressTrackingJob = null

            val player = mediaPlayer ?: return
            val path = currentFilePath ?: ""

            // Retrieve current position and duration before or during pause
            val currentPos = try {
                player.currentPosition.toLong().coerceAtLeast(0L)
            } catch (_: Exception) {
                (_playerState.value as? AudioPlayerState.Playing)?.currentPositionMs ?: 0L
            }
            val duration = try {
                player.duration.toLong().coerceAtLeast(0L)
            } catch (_: Exception) {
                (_playerState.value as? AudioPlayerState.Playing)?.totalDurationMs ?: 0L
            }

            try {
                player.pause()
            } catch (e: Exception) {
                Log.w(TAG, "Exception while calling MediaPlayer.pause()", e)
            }

            _playerState.value = AudioPlayerState.Paused(currentPos, duration, path)
            Log.i(TAG, "Audio playback paused at ${currentPos}ms")
        }
    }

    fun resume() {
        synchronized(lock) {
            val player = mediaPlayer ?: return
            val path = currentFilePath ?: return

            requestAudioFocus()

            val currentPos = try {
                player.currentPosition.toLong().coerceAtLeast(0L)
            } catch (_: Exception) {
                (_playerState.value as? AudioPlayerState.Paused)?.currentPositionMs ?: 0L
            }
            val duration = try {
                player.duration.toLong().coerceAtLeast(0L)
            } catch (_: Exception) {
                (_playerState.value as? AudioPlayerState.Paused)?.totalDurationMs ?: 0L
            }

            try {
                player.start()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to resume MediaPlayer", e)
                _playerState.value = AudioPlayerState.Error("Failed to resume playback: ${e.message}")
                return
            }

            // Synchronously update state to Playing so UI state and button reflect real state immediately
            _playerState.value = AudioPlayerState.Playing(currentPos, duration, path)
            startProgressTracking(duration, path)
            Log.i(TAG, "Audio playback resumed at ${currentPos}ms")
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
        abandonAudioFocus()
        isPausedDueToAudioFocus = false

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
                delay(SCRUBBER_POLL_INTERVAL_MS)
                synchronized(lock) {
                    val player = mediaPlayer
                    if (player == null || !isActive) return@launch

                    val isPlaying = try {
                        player.isPlaying
                    } catch (_: Exception) {
                        false
                    }
                    if (!isPlaying) return@launch

                    val pos = try {
                        player.currentPosition.toLong().coerceIn(0L, duration)
                    } catch (_: Exception) {
                        return@launch
                    }

                    // Only emit progress updates if the player state is actively Playing
                    if (_playerState.value is AudioPlayerState.Playing) {
                        _playerState.value = AudioPlayerState.Playing(pos, duration, filePath)
                    }
                }
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
