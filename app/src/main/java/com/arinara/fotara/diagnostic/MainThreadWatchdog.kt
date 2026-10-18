// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.diagnostic

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

data class StuckErrorRecord(
    val id: Long,
    val timestampMs: Long,
    val durationMs: Long,
    val activeScreen: String,
    val topMethod: String,
    val fullStackTrace: String,
    val isSevereFreeze: Boolean = false
)

/**
 * Lightweight, non-blocking watchdog daemon that monitors Main Thread responsiveness.
 * Detects UI hitches (>200ms) and severe freezes (>500ms), capturing stack traces
 * and isolating the offending app class and method.
 */
class MainThreadWatchdog(
    private val hitchThresholdMs: Long = 200L,
    private val freezeThresholdMs: Long = 500L,
    private val checkIntervalMs: Long = 100L,
    private val onStuckDetected: ((StuckErrorRecord) -> Unit)? = null
) {
    private val isRunning = AtomicBoolean(false)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val mainThread = Looper.getMainLooper().thread

    private val lastPingDispatchUptime = AtomicLong(0L)
    private val lastPingCompletedUptime = AtomicLong(0L)
    private val recordCounter = AtomicLong(0L)

    private var watchdogThread: Thread? = null

    private val _latestAlertFlow = MutableStateFlow<StuckErrorRecord?>(null)
    val latestAlertFlow: StateFlow<StuckErrorRecord?> = _latestAlertFlow.asStateFlow()

    private val pingRunnable = Runnable {
        lastPingCompletedUptime.set(SystemClock.uptimeMillis())
    }

    fun start() {
        if (isRunning.compareAndSet(false, true)) {
            val now = SystemClock.uptimeMillis()
            lastPingDispatchUptime.set(now)
            lastPingCompletedUptime.set(now)

            watchdogThread = Thread({
                var isHandlingFreeze = false
                var frozenStartTimeUptime = 0L
                var capturedRecord: StuckErrorRecord? = null

                while (isRunning.get()) {
                    val dispatchTime = SystemClock.uptimeMillis()
                    lastPingDispatchUptime.set(dispatchTime)

                    mainHandler.post(pingRunnable)

                    try {
                        Thread.sleep(checkIntervalMs)
                    } catch (_: InterruptedException) {
                        break
                    }

                    val nowCheck = SystemClock.uptimeMillis()
                    val completedTime = lastPingCompletedUptime.get()
                    val lagMs = nowCheck - completedTime

                    if (lagMs >= hitchThresholdMs) {
                        if (!isHandlingFreeze) {
                            // Fresh freeze/hitch detected!
                            isHandlingFreeze = true
                            frozenStartTimeUptime = completedTime
                            val stackTrace = mainThread.stackTrace
                            val topMethod = extractTopAppMethod(stackTrace)
                            val formattedStack = formatStackTrace(stackTrace)
                            val activeScreen = AppDiagnosticLogger.currentScreen.value

                            val record = StuckErrorRecord(
                                id = recordCounter.incrementAndGet(),
                                timestampMs = System.currentTimeMillis(),
                                durationMs = lagMs,
                                activeScreen = activeScreen,
                                topMethod = topMethod,
                                fullStackTrace = formattedStack,
                                isSevereFreeze = lagMs >= freezeThresholdMs
                            )
                            capturedRecord = record
                            _latestAlertFlow.value = record
                            onStuckDetected?.invoke(record)
                        } else {
                            // Continuing freeze
                            capturedRecord?.let { current ->
                                val updated = current.copy(
                                    durationMs = nowCheck - frozenStartTimeUptime,
                                    isSevereFreeze = (nowCheck - frozenStartTimeUptime) >= freezeThresholdMs
                                )
                                capturedRecord = updated
                                _latestAlertFlow.value = updated
                            }
                        }
                    } else {
                        if (isHandlingFreeze) {
                            // Main thread has just recovered!
                            isHandlingFreeze = false
                            capturedRecord?.let { rec ->
                                onStuckDetected?.invoke(rec)
                            }
                            capturedRecord = null
                        }
                    }
                }
            }, "fotara-ui-watchdog").apply {
                isDaemon = true
                priority = Thread.NORM_PRIORITY + 1
                start()
            }
        }
    }

    fun stop() {
        if (isRunning.compareAndSet(true, false)) {
            watchdogThread?.interrupt()
            watchdogThread = null
            mainHandler.removeCallbacks(pingRunnable)
        }
    }

    fun clearAlert() {
        _latestAlertFlow.value = null
    }

    companion object {
        fun extractTopAppMethod(stackTrace: Array<StackTraceElement>): String {
            for (element in stackTrace) {
                val className = element.className
                if (className.startsWith("com.arinara.fotara") &&
                    !className.contains("MainThreadWatchdog") &&
                    !className.contains("AppDiagnosticLogger") &&
                    !className.contains("FramePacingMonitor")
                ) {
                    val simpleName = className.substringAfterLast('.')
                    return "$simpleName.${element.methodName}(${element.fileName}:${element.lineNumber})"
                }
            }

            // Fallback: first non-dalvik/non-android frame if possible
            for (element in stackTrace) {
                if (!element.className.startsWith("java.") &&
                    !element.className.startsWith("android.os.MessageQueue") &&
                    !element.className.startsWith("android.os.Looper")
                ) {
                    val simpleName = element.className.substringAfterLast('.')
                    return "$simpleName.${element.methodName}"
                }
            }

            return "MainThread (Blocked in Looper)"
        }

        fun formatStackTrace(stackTrace: Array<StackTraceElement>): String {
            val sb = java.lang.StringBuilder()
            for (element in stackTrace.take(30)) {
                sb.append("  at ").append(element.toString()).append("\n")
            }
            return sb.toString().trimEnd()
        }
    }
}
