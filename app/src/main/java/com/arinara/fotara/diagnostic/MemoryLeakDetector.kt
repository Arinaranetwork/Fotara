// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.diagnostic

enum class LeakSeverity {
    INFO,
    WARNING,
    CRITICAL
}

data class MemoryLeakHint(
    val severity: LeakSeverity,
    val title: String,
    val description: String,
    val suggestedFix: String
)

object MemoryLeakDetector {

    fun analyze(
        ramSamples: List<RamSample>,
        workerRecords: List<WorkerMemoryRecord>,
        events: List<DiagnosticEvent>,
        heapMaxMb: Long
    ): List<MemoryLeakHint> {
        val hints = mutableListOf<MemoryLeakHint>()

        // 1. Monotonic upward creep analysis
        if (ramSamples.size >= 8) {
            val recent = ramSamples.takeLast(8)
            var strictlyIncreasingCount = 0
            for (i in 1 until recent.size) {
                if (recent[i].jvmUsedMb > recent[i - 1].jvmUsedMb) {
                    strictlyIncreasingCount++
                }
            }
            val netDelta = recent.last().jvmUsedMb - recent.first().jvmUsedMb
            if (strictlyIncreasingCount >= 6 && netDelta >= 15) {
                hints.add(
                    MemoryLeakHint(
                        severity = if (netDelta >= 35) LeakSeverity.CRITICAL else LeakSeverity.WARNING,
                        title = "Monotonic Heap Creep Detected (+${netDelta}MB)",
                        description = "JVM heap has grown steadily across ${strictlyIncreasingCount} of the last 8 samples without garbage collection recovery.",
                        suggestedFix = "Check for unclosed coroutine flows, uncancelled jobs in ViewModels, or accumulating caches."
                    )
                )
            }
        }

        // 2. High Heap Pressure Threshold
        if (ramSamples.isNotEmpty() && heapMaxMb > 0) {
            val latest = ramSamples.last()
            val usagePercent = (latest.jvmUsedMb * 100) / heapMaxMb
            if (usagePercent >= 85) {
                hints.add(
                    MemoryLeakHint(
                        severity = LeakSeverity.CRITICAL,
                        title = "Critical Heap Pressure (${usagePercent}% of Max)",
                        description = "Current JVM heap usage (${latest.jvmUsedMb}MB of ${heapMaxMb}MB) is approaching OutOfMemoryError territory.",
                        suggestedFix = "Run Force Garbage Collection or trim bitmap caches in Coil and Canvas TileCache."
                    )
                )
            } else if (usagePercent >= 70) {
                hints.add(
                    MemoryLeakHint(
                        severity = LeakSeverity.WARNING,
                        title = "Elevated Heap Consumption (${usagePercent}% of Max)",
                        description = "Current JVM heap (${latest.jvmUsedMb}MB) is above 70% of maximum allocated heap.",
                        suggestedFix = "Inspect active documents, large PDF canvases, or un-recycled bitmap allocations."
                    )
                )
            }
        }

        // 3. Worker Heap Spike Analysis
        if (workerRecords.isNotEmpty()) {
            val heavyWorkers = workerRecords.filter { it.deltaHeapMb >= 20 }
            if (heavyWorkers.isNotEmpty()) {
                val lastHeavy = heavyWorkers.last()
                hints.add(
                    MemoryLeakHint(
                        severity = LeakSeverity.WARNING,
                        title = "Heavy Allocation in [${lastHeavy.workerName}] (+${lastHeavy.deltaHeapMb}MB)",
                        description = "Worker '${lastHeavy.workerName}' expanded the JVM heap by +${lastHeavy.deltaHeapMb}MB in ${lastHeavy.durationMs}ms.",
                        suggestedFix = "Verify that temporary bitmaps, buffers, or streams in '${lastHeavy.workerName}' are promptly closed and unreferenced."
                    )
                )
            }
        }

        // 4. Repeated Navigation without GC Reclamation
        val navEvents = events.filter { it.tag == "Navigation" }
        if (navEvents.size >= 10 && ramSamples.size >= 10) {
            val startMb = ramSamples.first().jvmUsedMb
            val endMb = ramSamples.last().jvmUsedMb
            if (endMb - startMb >= 25) {
                hints.add(
                    MemoryLeakHint(
                        severity = LeakSeverity.INFO,
                        title = "Retained Navigation Memory (+${endMb - startMb}MB across ${navEvents.size} transitions)",
                        description = "Memory increased across multiple screen transitions without returning to baseline.",
                        suggestedFix = "Ensure Screen composables detach listeners and dispose heavy ViewModels upon back-navigation."
                    )
                )
            }
        }

        return hints
    }
}
