// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.diagnostic

import android.os.Handler
import android.os.Looper
import android.view.Choreographer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

data class FramePacingSnapshot(
    val currentFps: Int = 0,
    val onePercentLowFps: Int = 0,
    val lastFrameDurationMs: Float = 0f,
    val pacingScorePct: Float = 100f,
    val jankFramesInWindow: Int = 0,
    val rollingDurationsMs: List<Float> = emptyList(),
    val targetFrameIntervalMs: Float = 16.67f
)

/**
 * High-precision, zero-overhead vsync frame pacing engine.
 * Measures inter-frame duration in nanoseconds directly from Choreographer,
 * calculating rolling FPS, 1% Low FPS (worst-case stutter), and pacing consistency.
 */
class FramePacingMonitor(
    private val targetRefreshRateHz: Float = 60f,
    private val maxRollingSamples: Int = 60
) {
    private val targetIntervalMs = 1000f / targetRefreshRateHz.coerceAtLeast(30f)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val isRunning = AtomicBoolean(false)

    private var lastFrameTimeNanos: Long = 0L
    private val recentDurations = ArrayDeque<Float>(maxRollingSamples)
    private var lastFpsCalculationTimeNanos: Long = 0L

    private val _snapshotFlow = MutableStateFlow(
        FramePacingSnapshot(
            targetFrameIntervalMs = targetIntervalMs,
            pacingScorePct = 100f
        )
    )
    val snapshotFlow: StateFlow<FramePacingSnapshot> = _snapshotFlow.asStateFlow()

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!isRunning.get()) return

            if (lastFrameTimeNanos > 0L) {
                val deltaNanos = frameTimeNanos - lastFrameTimeNanos
                val durationMs = (deltaNanos / 1_000_000f).coerceIn(0.1f, 1000f)

                synchronized(recentDurations) {
                    if (recentDurations.size >= maxRollingSamples) {
                        recentDurations.removeFirst()
                    }
                    recentDurations.addLast(durationMs)
                }

                // Update metrics every ~250ms or at least every 15 frames
                if (frameTimeNanos - lastFpsCalculationTimeNanos >= 250_000_000L || recentDurations.size < 15) {
                    lastFpsCalculationTimeNanos = frameTimeNanos
                    recalculateSnapshot(durationMs)
                }
            } else {
                lastFpsCalculationTimeNanos = frameTimeNanos
            }

            lastFrameTimeNanos = frameTimeNanos

            if (isRunning.get()) {
                Choreographer.getInstance().postFrameCallback(this)
            }
        }
    }

    fun start() {
        if (isRunning.compareAndSet(false, true)) {
            lastFrameTimeNanos = 0L
            if (Looper.myLooper() == Looper.getMainLooper()) {
                Choreographer.getInstance().postFrameCallback(frameCallback)
            } else {
                mainHandler.post {
                    if (isRunning.get()) {
                        Choreographer.getInstance().postFrameCallback(frameCallback)
                    }
                }
            }
        }
    }

    fun stop() {
        if (isRunning.compareAndSet(true, false)) {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                Choreographer.getInstance().removeFrameCallback(frameCallback)
            } else {
                mainHandler.post {
                    Choreographer.getInstance().removeFrameCallback(frameCallback)
                }
            }
        }
    }

    fun reset() {
        synchronized(recentDurations) {
            recentDurations.clear()
        }
        _snapshotFlow.value = FramePacingSnapshot(
            targetFrameIntervalMs = targetIntervalMs,
            pacingScorePct = 100f
        )
    }

    private fun recalculateSnapshot(lastDuration: Float) {
        val samples: List<Float>
        synchronized(recentDurations) {
            samples = recentDurations.toList()
        }

        if (samples.isEmpty()) return

        val currentFps = computeAverageFps(samples)
        val onePercentLow = computeOnePercentLowFps(samples)
        val pacingScore = computePacingScore(samples, targetIntervalMs)
        val jankCount = samples.count { it > (targetIntervalMs * 1.5f) }

        _snapshotFlow.value = FramePacingSnapshot(
            currentFps = currentFps,
            onePercentLowFps = onePercentLow,
            lastFrameDurationMs = lastDuration,
            pacingScorePct = pacingScore,
            jankFramesInWindow = jankCount,
            rollingDurationsMs = samples,
            targetFrameIntervalMs = targetIntervalMs
        )
    }

    companion object {
        fun computeAverageFps(durationsMs: List<Float>): Int {
            if (durationsMs.isEmpty()) return 0
            val avgDuration = durationsMs.average().toFloat()
            if (avgDuration <= 0f) return 0
            return (1000f / avgDuration).toInt().coerceIn(0, 240)
        }

        fun computeOnePercentLowFps(durationsMs: List<Float>): Int {
            if (durationsMs.isEmpty()) return 0
            if (durationsMs.size < 5) {
                val worst = durationsMs.maxOrNull() ?: return 0
                return if (worst > 0f) (1000f / worst).toInt().coerceIn(0, 240) else 0
            }

            // Sort ascending: longest frame times are at the end
            val sorted = durationsMs.sorted()
            // 99th percentile index (worst 1% starts at 99% mark)
            val index99 = (sorted.size * 0.99f).toInt().coerceIn(0, sorted.lastIndex)
            val worstFrameTimeMs = sorted[index99]

            return if (worstFrameTimeMs > 0f) {
                (1000f / worstFrameTimeMs).toInt().coerceIn(0, 240)
            } else {
                0
            }
        }

        fun computePacingScore(durationsMs: List<Float>, targetIntervalMs: Float): Float {
            if (durationsMs.isEmpty()) return 100f
            // Acceptable jitter tolerance: within 25% of target vsync interval
            val toleranceMs = targetIntervalMs * 1.25f
            val smoothCount = durationsMs.count { it <= toleranceMs }
            return (smoothCount * 100f / durationsMs.size).coerceIn(0f, 100f)
        }
    }
}
