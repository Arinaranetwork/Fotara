// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.settings

object SettingsFadeMath {
    /**
     * Computes the alpha of the pinned header fade gradient based on scroll offset.
     * Eased smoothly from 0f at 0px to 1f at or above thresholdPx (~160dp).
     * Strictly monotonic and clamped to [0f..1f].
     */
    fun computeFadeAlpha(scrollOffsetPx: Float, thresholdPx: Float): Float {
        if (thresholdPx <= 0f) return if (scrollOffsetPx > 0f) 1f else 0f
        val fraction = (scrollOffsetPx / thresholdPx).coerceIn(0f, 1f)
        // Standard smoothstep easing: 3*t^2 - 2*t^3
        return fraction * fraction * (3f - 2f * fraction)
    }
}
