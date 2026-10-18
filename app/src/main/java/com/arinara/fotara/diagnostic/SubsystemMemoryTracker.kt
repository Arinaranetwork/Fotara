// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.diagnostic

import android.os.SystemClock

object SubsystemMemoryTracker {

    fun getUsedHeapMb(): Long {
        val runtime = Runtime.getRuntime()
        return (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
    }

    inline fun <T> track(workerName: String, block: () -> T): T {
        val startHeap = getUsedHeapMb()
        val startUptime = SystemClock.elapsedRealtime()
        try {
            return block()
        } finally {
            val endHeap = getUsedHeapMb()
            val duration = SystemClock.elapsedRealtime() - startUptime
            AppDiagnosticLogger.recordWorkerMemory(workerName, startHeap, endHeap, duration)
        }
    }

    suspend inline fun <T> trackSuspend(workerName: String, crossinline block: suspend () -> T): T {
        val startHeap = getUsedHeapMb()
        val startUptime = SystemClock.elapsedRealtime()
        try {
            return block()
        } finally {
            val endHeap = getUsedHeapMb()
            val duration = SystemClock.elapsedRealtime() - startUptime
            AppDiagnosticLogger.recordWorkerMemory(workerName, startHeap, endHeap, duration)
        }
    }
}
