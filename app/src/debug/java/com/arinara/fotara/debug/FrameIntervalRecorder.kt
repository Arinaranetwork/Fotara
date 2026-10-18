// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.debug

import android.view.Choreographer
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList

/**
 * DEBUG-ONLY frame interval recorder using native Choreographer.FrameCallback.
 * Collects inter-frame intervals while a gesture is active without adding external dependencies.
 * Computes p50, p95, p99, max latency, and counts of frames exceeding 16.7ms and 33ms.
 */
object FrameIntervalRecorder : Choreographer.FrameCallback {

    private var isRecording = false
    private var lastFrameTimeNanos: Long = 0L
    private val intervalsMs = CopyOnWriteArrayList<Double>()

    fun startGesture() {
        intervalsMs.clear()
        lastFrameTimeNanos = 0L
        isRecording = true
        Choreographer.getInstance().postFrameCallback(this)
    }

    fun stopGesture() {
        isRecording = false
        Choreographer.getInstance().removeFrameCallback(this)
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!isRecording) return

        if (lastFrameTimeNanos > 0L) {
            val deltaMs = (frameTimeNanos - lastFrameTimeNanos) / 1_000_000.0
            intervalsMs.add(deltaMs)
        }
        lastFrameTimeNanos = frameTimeNanos
        Choreographer.getInstance().postFrameCallback(this)
    }

    fun getSummary(): String {
        if (intervalsMs.isEmpty()) return "No frames recorded during gesture."

        val sorted = intervalsMs.sorted()
        val total = sorted.size
        val countAbove16 = sorted.count { it > 16.7 }
        val countAbove33 = sorted.count { it > 33.3 }
        val countAbove50 = sorted.count { it > 50.0 }

        val p50 = percentile(sorted, 50.0)
        val p95 = percentile(sorted, 95.0)
        val p99 = percentile(sorted, 99.0)
        val max = sorted.last()

        val pctAbove16 = (countAbove16.toDouble() / total) * 100.0

        return String.format(
            Locale.US,
            "Frames: %d | >16.7ms: %d (%.1f%%) | >33ms: %d | >50ms: %d | p50: %.1fms | p95: %.1fms | p99: %.1fms | max: %.1fms",
            total, countAbove16, pctAbove16, countAbove33, countAbove50, p50, p95, p99, max
        )
    }

    private fun percentile(sorted: List<Double>, p: Double): Double {
        if (sorted.isEmpty()) return 0.0
        val idx = ((p / 100.0) * (sorted.size - 1)).toInt().coerceIn(0, sorted.size - 1)
        return sorted[idx]
    }
}
