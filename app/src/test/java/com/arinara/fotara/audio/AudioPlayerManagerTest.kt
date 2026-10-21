// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.audio

import com.arinara.fotara.audio.player.AudioPlayerManager
import com.arinara.fotara.audio.player.AudioPlayerState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioPlayerManagerTest {

    @Test
    fun formatDuration_formatsCorrectly() {
        assertEquals("00:00", AudioPlayerManager.formatDuration(0L))
        assertEquals("00:45", AudioPlayerManager.formatDuration(45000L))
        assertEquals("03:10", AudioPlayerManager.formatDuration(190000L))
        assertEquals("12:05", AudioPlayerManager.formatDuration(725000L))
    }

    @Test
    fun formatDuration_negativeValueClampedToZero() {
        assertEquals("00:00", AudioPlayerManager.formatDuration(-5000L))
    }

    @Test
    fun playerState_sealedSubclassesPreservePayloads() {
        val idle = AudioPlayerState.Idle
        assertTrue(idle is AudioPlayerState)

        val playing = AudioPlayerState.Playing(
            currentPositionMs = 25000L,
            totalDurationMs = 120000L,
            filePath = "/path/test.m4a"
        )
        assertEquals(25000L, playing.currentPositionMs)
        assertEquals(120000L, playing.totalDurationMs)
        assertEquals("/path/test.m4a", playing.filePath)

        val paused = AudioPlayerState.Paused(
            currentPositionMs = 50000L,
            totalDurationMs = 120000L,
            filePath = "/path/test.m4a"
        )
        assertEquals(50000L, paused.currentPositionMs)
        assertEquals(120000L, paused.totalDurationMs)
        assertEquals("/path/test.m4a", paused.filePath)

        val completed = AudioPlayerState.Completed(
            filePath = "/path/test.m4a",
            totalDurationMs = 120000L
        )
        assertEquals("/path/test.m4a", completed.filePath)
        assertEquals(120000L, completed.totalDurationMs)

        val error = AudioPlayerState.Error(message = "File not found")
        assertEquals("File not found", error.message)
    }

    @Test
    fun initialState_isIdle() {
        val manager = AudioPlayerManager()
        assertEquals(AudioPlayerState.Idle, manager.playerState.value)
    }

    @Test
    fun pause_whenIdle_doesNotThrowAndRemainsIdle() {
        val manager = AudioPlayerManager()
        manager.pause()
        assertEquals(AudioPlayerState.Idle, manager.playerState.value)
    }

    @Test
    fun stop_resetsStateToIdle() {
        val manager = AudioPlayerManager()
        manager.stop()
        assertEquals(AudioPlayerState.Idle, manager.playerState.value)
    }

    @Test
    fun release_resetsStateToIdle() {
        val manager = AudioPlayerManager()
        manager.release()
        assertEquals(AudioPlayerState.Idle, manager.playerState.value)
    }

    @Test
    fun play_nonExistentFile_emitsErrorState() {
        val manager = AudioPlayerManager()
        val result = manager.play("/non/existent/path/audio.m4a")
        assertFalse(result)
        assertTrue(manager.playerState.value is AudioPlayerState.Error)
        val error = manager.playerState.value as AudioPlayerState.Error
        assertTrue(error.message.contains("Cannot play non-existent"))
    }
}
