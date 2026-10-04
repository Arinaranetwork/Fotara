// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeLayoutHelperTest {

    @Test
    fun testComputeBottomContentPadding_DynamicMeasurementAndFallback() {
        // Measured stack height 164dp with 16dp buffer -> 180dp
        val measured164 = HomeLayoutHelper.computeBottomContentPadding(
            measuredBottomStackHeightDp = 164f,
            additionalBufferDp = 16f,
            fallbackPaddingDp = 170f
        )
        assertEquals(180f, measured164, 0.01f)

        // Measured stack height 140dp with 20dp buffer -> 160dp
        val measured140 = HomeLayoutHelper.computeBottomContentPadding(
            measuredBottomStackHeightDp = 140f,
            additionalBufferDp = 20f,
            fallbackPaddingDp = 170f
        )
        assertEquals(160f, measured140, 0.01f)

        // Unmeasured fallback (0dp)
        val fallback = HomeLayoutHelper.computeBottomContentPadding(
            measuredBottomStackHeightDp = 0f,
            additionalBufferDp = 16f,
            fallbackPaddingDp = 170f
        )
        assertEquals(170f, fallback, 0.01f)
    }
}
