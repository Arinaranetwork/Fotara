// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.debug

/**
 * RELEASE no-op implementation of FrameIntervalRecorder.
 * Ensures zero overhead, zero allocations, and zero debug inspection in release builds.
 */
object FrameIntervalRecorder {
    fun startGesture() {}
    fun stopGesture() {}
    fun getSummary(): String = ""
}
