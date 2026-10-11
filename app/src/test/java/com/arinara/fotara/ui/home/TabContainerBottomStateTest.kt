// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.home

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests verifying that [TabContainerBottomState] prevents layout jumps
 * by guaranteeing deterministic initialization and non-zero retention.
 */
class TabContainerBottomStateTest {

    private val testDensity = Density(density = 2.0f, fontScale = 1.0f)

    @Test
    fun testInitialValueIsDeterministicNonZero() {
        val state = TabContainerBottomState()
        assertEquals(TabContainerBottomState.DEFAULT_BOTTOM_CLEARANCE_DP, state.bottomOverlayPaddingDp)
        assertEquals(126.dp, state.bottomOverlayPaddingDp)
    }

    @Test
    fun testHeightMeasurementUpdatesValue() {
        val state = TabContainerBottomState()
        // 260px at density 2.0 = 130dp
        state.onHeightMeasured(260, testDensity)
        assertEquals(130.dp, state.bottomOverlayPaddingDp)
    }

    @Test
    fun testZeroOrNegativeMeasurementNeverResetsClearance() {
        val state = TabContainerBottomState()
        state.onHeightMeasured(260, testDensity)
        assertEquals(130.dp, state.bottomOverlayPaddingDp)

        // Simulate screen unmount or zero size measurement
        state.onHeightMeasured(0, testDensity)
        assertEquals(130.dp, state.bottomOverlayPaddingDp)

        state.onHeightMeasured(-10, testDensity)
        assertEquals(130.dp, state.bottomOverlayPaddingDp)
    }

    @Test
    fun testTabSwitchSimulationRetainsLastMeasuredClearance() {
        val state = TabContainerBottomState()
        state.onHeightMeasured(280, testDensity) // 140dp
        assertEquals(140.dp, state.bottomOverlayPaddingDp)

        // Multiple cycles of "tab switch" measurements
        state.onHeightMeasured(0, testDensity)
        assertEquals(140.dp, state.bottomOverlayPaddingDp)

        state.onHeightMeasured(280, testDensity)
        assertEquals(140.dp, state.bottomOverlayPaddingDp)
    }
}
