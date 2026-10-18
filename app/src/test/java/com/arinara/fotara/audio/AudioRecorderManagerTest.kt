// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.audio

import com.arinara.fotara.audio.recorder.AudioRecorderManager
import com.arinara.fotara.audio.recorder.AudioRecorderState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioRecorderManagerTest {

    @Test
    fun formatDuration_formatsMinutesAndSecondsCorrectly() {
        assertEquals("00:00", AudioRecorderManager.formatDuration(0L))
        assertEquals("00:05", AudioRecorderManager.formatDuration(5000L))
        assertEquals("01:24", AudioRecorderManager.formatDuration(84000L))
        assertEquals("05:00", AudioRecorderManager.formatDuration(300000L))
        assertEquals("10:15", AudioRecorderManager.formatDuration(615000L))
    }

    @Test
    fun hardCap_isStrictlyFiveMinutes() {
        assertEquals(300_000L, AudioRecorderManager.MAX_RECORDING_DURATION_MS)
        assertEquals(64_000, AudioRecorderManager.AUDIO_BIT_RATE)
        assertEquals(44_100, AudioRecorderManager.AUDIO_SAMPLE_RATE)
        assertEquals("audio", AudioRecorderManager.AUDIO_DIRECTORY_NAME)
    }

    @Test
    fun recorderState_sealedSubclassesPreservePayloads() {
        val idle = AudioRecorderState.Idle
        assertTrue(idle is AudioRecorderState)

        val recording = AudioRecorderState.Recording(elapsedMs = 45000L, filePath = "/data/audio/test.m4a")
        assertEquals(45000L, recording.elapsedMs)
        assertEquals("/data/audio/test.m4a", recording.filePath)

        val finalized = AudioRecorderState.Finalized(totalDurationMs = 120000L, filePath = "/data/audio/test.m4a")
        assertEquals(120000L, finalized.totalDurationMs)
        assertEquals("/data/audio/test.m4a", finalized.filePath)

        val error = AudioRecorderState.Error(message = "Mic busy")
        assertEquals("Mic busy", error.message)
    }
}
