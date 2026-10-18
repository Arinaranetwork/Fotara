// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.diagnostic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FramePacingMonitorTest {

    @Test
    fun computeAverageFps_uniform60Hz_returns60Fps() {
        val durations = List(60) { 16.666f }
        val fps = FramePacingMonitor.computeAverageFps(durations)
        assertEquals(60, fps)
    }

    @Test
    fun computeAverageFps_uniform30Hz_returns30Fps() {
        val durations = List(30) { 33.333f }
        val fps = FramePacingMonitor.computeAverageFps(durations)
        assertEquals(30, fps)
    }

    @Test
    fun computeAverageFps_emptyList_returnsZero() {
        val fps = FramePacingMonitor.computeAverageFps(emptyList())
        assertEquals(0, fps)
    }

    @Test
    fun computeOnePercentLowFps_stableFrameTimes_matchesAverageFps() {
        val durations = List(100) { 16.666f }
        val onePercentLow = FramePacingMonitor.computeOnePercentLowFps(durations)
        assertEquals(60, onePercentLow)
    }

    @Test
    fun computeOnePercentLowFps_withSevereStutters_dropsSignificantly() {
        // 98 smooth frames at 16.6ms, but 2 dropped frames spiking at 50ms (20 FPS)
        val durations = ArrayList<Float>()
        repeat(98) { durations.add(16.666f) }
        durations.add(50.0f)
        durations.add(50.0f)

        val avgFps = FramePacingMonitor.computeAverageFps(durations)
        val onePercentLow = FramePacingMonitor.computeOnePercentLowFps(durations)

        // Average FPS remains relatively high (~57 FPS), but 1% Low correctly detects the stutter at 20 FPS!
        assertTrue(avgFps >= 55)
        assertEquals(20, onePercentLow)
    }

    @Test
    fun computePacingScore_perfectPacing_returns100Percent() {
        val targetInterval = 16.667f
        val durations = List(60) { 16.667f }
        val score = FramePacingMonitor.computePacingScore(durations, targetInterval)
        assertEquals(100f, score, 0.01f)
    }

    @Test
    fun computePacingScore_withJank_calculatesAccuratePercentage() {
        val targetInterval = 16.667f
        // 80 frames on time (<= 20.8ms), 20 frames late (> 20.8ms)
        val durations = ArrayList<Float>()
        repeat(80) { durations.add(16.0f) }
        repeat(20) { durations.add(33.3f) }

        val score = FramePacingMonitor.computePacingScore(durations, targetInterval)
        assertEquals(80f, score, 0.01f)
    }
}
