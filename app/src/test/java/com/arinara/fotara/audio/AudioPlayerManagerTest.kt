// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.audio

import com.arinara.fotara.audio.player.AudioPlayerManager
import com.arinara.fotara.audio.player.AudioPlayerState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioPlayerManagerTest {

    @Test
    fun formatDuration_formatsCorrectly() {
        assertEquals("00:00", AudioPlayerManager.formatDuration(0L))
        assertEquals("00:45", AudioPlayerManager.formatDuration(45000L))
        assertEquals("03:10", AudioPlayerManager.formatDuration(190000L))
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

        val completed = AudioPlayerState.Completed(
            filePath = "/path/test.m4a",
            totalDurationMs = 120000L
        )
        assertEquals("/path/test.m4a", completed.filePath)
        assertEquals(120000L, completed.totalDurationMs)

        val error = AudioPlayerState.Error(message = "File not found")
        assertEquals("File not found", error.message)
    }
}
