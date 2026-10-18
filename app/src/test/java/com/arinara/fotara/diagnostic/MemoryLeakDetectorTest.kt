// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.diagnostic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoryLeakDetectorTest {

    @Test
    fun testHealthyStateReturnsNoHints() {
        val samples = listOf(
            RamSample(1000L, 20L, 32L, 10L, "Home"),
            RamSample(2000L, 22L, 32L, 10L, "Folder"),
            RamSample(3000L, 19L, 32L, 10L, "Home"),
            RamSample(4000L, 21L, 32L, 10L, "Settings")
        )
        val hints = MemoryLeakDetector.analyze(
            ramSamples = samples,
            workerRecords = emptyList(),
            events = emptyList(),
            heapMaxMb = 256L
        )
        assertTrue("Healthy memory trajectory should produce 0 leak hints", hints.isEmpty())
    }

    @Test
    fun testMonotonicHeapCreepDetection() {
        val creepSamples = mutableListOf<RamSample>()
        var currentMb = 20L
        for (i in 0..8) {
            currentMb += 3L
            creepSamples.add(RamSample(1000L + (i * 1000L), currentMb, 64L, 12L, "Canvas"))
        }

        val hints = MemoryLeakDetector.analyze(
            ramSamples = creepSamples,
            workerRecords = emptyList(),
            events = emptyList(),
            heapMaxMb = 256L
        )

        assertFalse("Creep trajectory must generate at least 1 hint", hints.isEmpty())
        val creepHint = hints.firstOrNull { it.title.contains("Monotonic Heap Creep") }
        assertTrue("Should detect Monotonic Heap Creep", creepHint != null)
    }

    @Test
    fun testCriticalHeapPressureDetection() {
        val highPressureSamples = listOf(
            RamSample(1000L, 235L, 256L, 20L, "CanvasNote")
        )
        val hints = MemoryLeakDetector.analyze(
            ramSamples = highPressureSamples,
            workerRecords = emptyList(),
            events = emptyList(),
            heapMaxMb = 256L
        )

        val criticalHint = hints.firstOrNull { it.severity == LeakSeverity.CRITICAL }
        assertTrue("Critical pressure > 85% must generate CRITICAL hint", criticalHint != null)
    }

    @Test
    fun testHeavyWorkerSpikeDetection() {
        val workers = listOf(
            WorkerMemoryRecord(
                id = 1L,
                timestampMs = 1000L,
                workerName = "CanvasAutosave",
                startHeapMb = 25L,
                endHeapMb = 55L,
                deltaHeapMb = 30L,
                durationMs = 120L
            )
        )
        val hints = MemoryLeakDetector.analyze(
            ramSamples = emptyList(),
            workerRecords = workers,
            events = emptyList(),
            heapMaxMb = 256L
        )

        val workerHint = hints.firstOrNull { it.title.contains("CanvasAutosave") }
        assertTrue("Heavy worker > 20MB delta must generate warning hint", workerHint != null)
    }
}
