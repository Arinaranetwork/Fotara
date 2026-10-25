// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.feature.collab.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import com.arinara.fotara.feature.collab.engine.CollabSessionEngine
import com.arinara.fotara.feature.collab.engine.CollabSessionState
import com.arinara.fotara.feature.collab.model.CollabAudioBitrate
import com.arinara.fotara.feature.collab.model.CollabMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Base64
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Low-CPU real-time voice communication engine for collaborative study canvas sessions.
 *
 * - Captures 16-bit mono PCM audio frames via [AudioRecord] (`VOICE_COMMUNICATION` with hardware AEC/NS)
 *   when the session is active, `RECORD_AUDIO` is granted, and the local user is unmuted.
 * - Uses lightweight RMS silence gating so silent background frames are not transmitted over LAN/Wi-Fi.
 * - Plays incoming collaborator [CollabMessage.AudioStreamFrame] packets via [AudioTrack] while applying
 *   per-user volume gain (`0%–200%`) and local/moderator mute controls from [CollabSessionEngine].
 */
class CollabVoiceEngine(
    private val context: Context,
    private val sessionEngine: CollabSessionEngine
) : AutoCloseable {

    companion object {
        const val DEFAULT_SAMPLE_RATE_HZ = 16_000
        private const val FRAME_DURATION_MS = 40
        private const val SILENCE_PEAK_THRESHOLD = 40
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val isRunning = AtomicBoolean(false)
    private val sequenceCounter = AtomicLong(0L)

    @Volatile
    private var captureJob: Job? = null

    @Volatile
    private var playbackTrack: AudioTrack? = null

    @Volatile
    private var playbackSampleRateHz: Int = 0

    private val playbackLock = Any()

    init {
        sessionEngine.onAudioFrameReceived = { frame ->
            playIncomingFrame(frame)
        }
        scope.launch {
            sessionEngine.state.collect { state ->
                if (state == CollabSessionState.IN_ROOM) {
                    startVoiceIfPermitted()
                } else {
                    stopVoice()
                }
            }
        }
    }

    /**
     * Checks whether `RECORD_AUDIO` runtime permission is currently granted.
     */
    fun hasRecordAudioPermission(): Boolean {
        return try {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * Starts or restarts voice capture if the session is active and permission is granted.
     */
    fun startVoiceIfPermitted() {
        if (!sessionEngine.isSessionActive) return
        if (isRunning.compareAndSet(false, true)) {
            captureJob?.cancel()
            captureJob = scope.launch {
                runCaptureLoop()
            }
        }
    }

    /**
     * Stops active microphone capture and releases the playback [AudioTrack].
     */
    fun stopVoice() {
        isRunning.set(false)
        captureJob?.cancel()
        captureJob = null
        releasePlaybackTrack()
    }

    private suspend fun runCaptureLoop() {
        var audioRecord: AudioRecord? = null
        var currentSampleRate = 0
        var useMicSourceFallback = false
        var consecutiveZeroFrames = 0
        try {
            while (scope.isActive && isRunning.get() && sessionEngine.isSessionActive) {
                if (!sessionEngine.canLocalUserSpeak || !hasRecordAudioPermission()) {
                    if (audioRecord != null) {
                        try {
                            audioRecord.stop()
                            audioRecord.release()
                        } catch (_: Throwable) {
                        }
                        audioRecord = null
                        currentSampleRate = 0
                    }
                    delay(120L)
                    continue
                }

                val targetSampleRate = resolveSampleRateForBitrate(sessionEngine.audioBitrateKbps.value)
                if (audioRecord == null || currentSampleRate != targetSampleRate) {
                    try {
                        audioRecord?.stop()
                        audioRecord?.release()
                    } catch (_: Throwable) {
                    }
                    audioRecord = createAudioRecord(targetSampleRate, useMicSourceFallback)
                    currentSampleRate = targetSampleRate
                    consecutiveZeroFrames = 0
                    if (audioRecord == null) {
                        delay(500L)
                        continue
                    }
                    try {
                        audioRecord.startRecording()
                    } catch (_: Throwable) {
                        try {
                            audioRecord.release()
                        } catch (_: Throwable) {
                        }
                        audioRecord = null
                        delay(500L)
                        continue
                    }
                }

                val samplesPerFrame = (currentSampleRate * FRAME_DURATION_MS) / 1000
                val shortBuffer = ShortArray(samplesPerFrame)
                val readCount = try {
                    audioRecord.read(shortBuffer, 0, samplesPerFrame)
                } catch (_: Throwable) {
                    -1
                }

                if (readCount <= 0) {
                    delay(20L)
                    continue
                }

                // Silence gate: check peak amplitude so we do not flood the network when silent
                var peak = 0
                for (i in 0 until readCount) {
                    val amp = abs(shortBuffer[i].toInt())
                    if (amp > peak) peak = amp
                }
                if (peak <= 2 && !useMicSourceFallback) {
                    consecutiveZeroFrames++
                    if (consecutiveZeroFrames >= 15) {
                        useMicSourceFallback = true
                        try {
                            audioRecord.stop()
                            audioRecord.release()
                        } catch (_: Throwable) {
                        }
                        audioRecord = null
                    }
                    continue
                }
                consecutiveZeroFrames = 0
                if (peak < SILENCE_PEAK_THRESHOLD) {
                    continue
                }

                val byteBuffer = ByteBuffer.allocate(readCount * 2).order(ByteOrder.LITTLE_ENDIAN)
                for (i in 0 until readCount) {
                    byteBuffer.putShort(shortBuffer[i])
                }
                val base64 = Base64.getEncoder().encodeToString(byteBuffer.array())
                sessionEngine.broadcastAudioFrame(
                    pcmDataBase64 = base64,
                    sequenceNumber = sequenceCounter.incrementAndGet(),
                    sampleRateHz = currentSampleRate
                )
            }
        } finally {
            try {
                audioRecord?.stop()
            } catch (_: Throwable) {
            }
            try {
                audioRecord?.release()
            } catch (_: Throwable) {
            }
        }
    }

    private fun playIncomingFrame(frame: CollabMessage.AudioStreamFrame) {
        if (!sessionEngine.isSessionActive) return
        val gain = sessionEngine.getEffectivePeerVolume(frame.peerId)
        if (gain <= 0.001f) return

        val rawBytes = try {
            Base64.getDecoder().decode(frame.pcmDataBase64)
        } catch (_: Throwable) {
            return
        }
        if (rawBytes.size < 2) return

        val scaledBytes = if (abs(gain - 1.0f) < 0.02f) {
            rawBytes
        } else {
            applyGainToPcm16Le(rawBytes, gain)
        }

        val sampleRate = frame.sampleRateHz.coerceIn(8_000, 48_000)
        synchronized(playbackLock) {
            var track = playbackTrack
            if (track == null || playbackSampleRateHz != sampleRate || track.state != AudioTrack.STATE_INITIALIZED) {
                releasePlaybackTrack()
                track = createAudioTrack(sampleRate)
                playbackTrack = track
                playbackSampleRateHz = sampleRate
                try {
                    track?.play()
                } catch (_: Throwable) {
                }
            }
            if (track != null && track.state == AudioTrack.STATE_INITIALIZED) {
                try {
                    track.write(scaledBytes, 0, scaledBytes.size)
                } catch (_: Throwable) {
                }
            }
        }
    }

    private fun applyGainToPcm16Le(pcmBytes: ByteArray, gain: Float): ByteArray {
        val sampleCount = pcmBytes.size / 2
        val inBuf = ByteBuffer.wrap(pcmBytes).order(ByteOrder.LITTLE_ENDIAN)
        val outBytes = ByteArray(sampleCount * 2)
        val outBuf = ByteBuffer.wrap(outBytes).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until sampleCount) {
            val sample = inBuf.short.toInt()
            val scaled = (sample * gain).roundToInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            outBuf.putShort(scaled.toShort())
        }
        return outBytes
    }

    private fun resolveSampleRateForBitrate(bitrateKbps: Int): Int {
        return when {
            bitrateKbps <= CollabAudioBitrate.LOW_DATA_16_KBPS -> 8_000
            bitrateKbps <= CollabAudioBitrate.BALANCED_32_KBPS -> 12_000
            else -> DEFAULT_SAMPLE_RATE_HZ
        }
    }

    @Suppress("MissingPermission")
    private fun createAudioRecord(sampleRateHz: Int, preferMicSource: Boolean = false): AudioRecord? {
        if (!hasRecordAudioPermission()) return null
        return try {
            val minBuf = AudioRecord.getMinBufferSize(
                sampleRateHz,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            if (minBuf <= 0) return null
            val bufferSize = maxOf(minBuf * 2, (sampleRateHz * FRAME_DURATION_MS / 1000) * 4)
            val sources = if (preferMicSource) {
                intArrayOf(MediaRecorder.AudioSource.MIC, MediaRecorder.AudioSource.VOICE_COMMUNICATION)
            } else {
                intArrayOf(MediaRecorder.AudioSource.VOICE_COMMUNICATION, MediaRecorder.AudioSource.MIC)
            }
            for (source in sources) {
                try {
                    val record = AudioRecord(
                        source,
                        sampleRateHz,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize
                    )
                    if (record.state == AudioRecord.STATE_INITIALIZED) {
                        return record
                    }
                    record.release()
                } catch (_: Throwable) {
                }
            }
            null
        } catch (_: Throwable) {
            null
        }
    }

    private fun createAudioTrack(sampleRateHz: Int): AudioTrack? {
        return try {
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRateHz,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            if (minBuf <= 0) return null
            val bufferSize = maxOf(minBuf * 2, (sampleRateHz * FRAME_DURATION_MS / 1000) * 4)
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            val format = AudioFormat.Builder()
                .setSampleRate(sampleRateHz)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()
            val track = AudioTrack(
                attributes,
                format,
                bufferSize,
                AudioTrack.MODE_STREAM,
                android.media.AudioManager.AUDIO_SESSION_ID_GENERATE
            )
            if (track.state == AudioTrack.STATE_INITIALIZED) {
                track
            } else {
                track.release()
                null
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun releasePlaybackTrack() {
        synchronized(playbackLock) {
            val track = playbackTrack
            playbackTrack = null
            playbackSampleRateHz = 0
            if (track != null) {
                try {
                    track.pause()
                    track.flush()
                } catch (_: Throwable) {
                }
                try {
                    track.release()
                } catch (_: Throwable) {
                }
            }
        }
    }

    override fun close() {
        stopVoice()
        if (sessionEngine.onAudioFrameReceived != null) {
            sessionEngine.onAudioFrameReceived = null
        }
        scope.cancel()
    }
}
