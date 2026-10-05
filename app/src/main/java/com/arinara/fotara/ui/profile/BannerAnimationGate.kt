// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.profile

object BannerAnimationGate {
    /**
     * Determines whether GIF banner animation should run.
     * Requires:
     * - API Level >= 28 (ImageDecoder / AnimatedImageDrawable support)
     * - Animator duration scale > 0 (animations enabled in system settings)
     * - Screen is RESUMED (stops in background)
     * - Banner is currently on-screen in the viewport
     */
    fun shouldAnimate(
        isResumed: Boolean,
        isOnScreen: Boolean,
        animatorDurationScale: Float,
        apiLevel: Int
    ): Boolean {
        if (apiLevel < 28) return false
        if (animatorDurationScale <= 0f) return false
        return isResumed && isOnScreen
    }
}
