// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsFadeMathTest {

    @Test
    fun computeFadeAlpha_atZero_returnsZero() {
        val alpha = SettingsFadeMath.computeFadeAlpha(0f, 160f)
        assertEquals(0f, alpha, 0.0001f)
    }

    @Test
    fun computeFadeAlpha_atNegative_returnsZero() {
        val alpha = SettingsFadeMath.computeFadeAlpha(-50f, 160f)
        assertEquals(0f, alpha, 0.0001f)
    }

    @Test
    fun computeFadeAlpha_atOrAboveThreshold_returnsOne() {
        assertEquals(1f, SettingsFadeMath.computeFadeAlpha(160f, 160f), 0.0001f)
        assertEquals(1f, SettingsFadeMath.computeFadeAlpha(250f, 160f), 0.0001f)
    }

    @Test
    fun computeFadeAlpha_isMonotonic() {
        val threshold = 160f
        var prevAlpha = 0f
        for (i in 0..160 step 5) {
            val offset = i.toFloat()
            val alpha = SettingsFadeMath.computeFadeAlpha(offset, threshold)
            assertTrue("Alpha must be monotonic at offset $offset: $alpha >= $prevAlpha", alpha >= prevAlpha)
            assertTrue("Alpha must be within [0, 1]: $alpha", alpha in 0f..1f)
            prevAlpha = alpha
        }
    }

    @Test
    fun computeFadeAlpha_isSmoothEased() {
        val threshold = 100f
        // Smoothstep: at t=0.5, fraction=0.5 -> 0.25 * (3 - 1) = 0.5
        val midAlpha = SettingsFadeMath.computeFadeAlpha(50f, threshold)
        assertEquals(0.5f, midAlpha, 0.0001f)

        // At t=0.25 -> 0.0625 * (3 - 0.5) = 0.15625 (slower start than linear 0.25)
        val quarterAlpha = SettingsFadeMath.computeFadeAlpha(25f, threshold)
        assertTrue("Easing should start slow: quarterAlpha=$quarterAlpha < 0.25", quarterAlpha < 0.25f)

        // At t=0.75 -> 0.5625 * (3 - 1.5) = 0.84375 (slower end than linear 0.75)
        val threeQuarterAlpha = SettingsFadeMath.computeFadeAlpha(75f, threshold)
        assertTrue("Easing should finish smooth: threeQuarterAlpha=$threeQuarterAlpha > 0.75", threeQuarterAlpha > 0.75f)
    }
}
