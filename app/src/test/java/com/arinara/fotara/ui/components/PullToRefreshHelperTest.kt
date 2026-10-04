// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PullToRefreshHelperTest {

    @Test
    fun testCanTriggerRefresh_gatingRules() {
        // Normal condition: at top, no multi-select, no search, no overlay -> ALLOWED
        assertTrue(
            PullToRefreshHelper.canTriggerRefresh(
                isAtTop = true,
                isMultiSelectActive = false,
                isSearchFocused = false,
                isOverlayOpen = false
            )
        )

        // Scrolled down -> BLOCKED
        assertFalse(
            PullToRefreshHelper.canTriggerRefresh(
                isAtTop = false,
                isMultiSelectActive = false,
                isSearchFocused = false,
                isOverlayOpen = false
            )
        )

        // Multi-select active -> BLOCKED
        assertFalse(
            PullToRefreshHelper.canTriggerRefresh(
                isAtTop = true,
                isMultiSelectActive = true,
                isSearchFocused = false,
                isOverlayOpen = false
            )
        )

        // Search focused/active -> BLOCKED
        assertFalse(
            PullToRefreshHelper.canTriggerRefresh(
                isAtTop = true,
                isMultiSelectActive = false,
                isSearchFocused = true,
                isOverlayOpen = false
            )
        )

        // Overlay/dialog open -> BLOCKED
        assertFalse(
            PullToRefreshHelper.canTriggerRefresh(
                isAtTop = true,
                isMultiSelectActive = false,
                isSearchFocused = false,
                isOverlayOpen = true
            )
        )
    }

    @Test
    fun testComputePullProgress_clampsBetween0And1() {
        val triggerPx = 200f

        assertEquals(0f, PullToRefreshHelper.computePullProgress(0f, triggerPx), 0.001f)
        assertEquals(0.5f, PullToRefreshHelper.computePullProgress(100f, triggerPx), 0.001f)
        assertEquals(1.0f, PullToRefreshHelper.computePullProgress(200f, triggerPx), 0.001f)
        assertEquals(1.0f, PullToRefreshHelper.computePullProgress(300f, triggerPx), 0.001f) // clamped to 1f
        assertEquals(0f, PullToRefreshHelper.computePullProgress(-50f, triggerPx), 0.001f) // clamped to 0f
    }

    @Test
    fun testComputeIndicatorRotation_proportionalFullTurn() {
        assertEquals(0f, PullToRefreshHelper.computeIndicatorRotation(0f), 0.001f)
        assertEquals(180f, PullToRefreshHelper.computeIndicatorRotation(0.5f), 0.001f)
        assertEquals(360f, PullToRefreshHelper.computeIndicatorRotation(1.0f), 0.001f)
    }

    @Test
    fun testComputeIndicatorScaleAndAlpha() {
        // Dragging phase: follows progress
        assertEquals(0.4f, PullToRefreshHelper.computeIndicatorScale(0.4f, isRefreshing = false), 0.001f)
        assertEquals(0.4f, PullToRefreshHelper.computeIndicatorAlpha(0.4f, isRefreshing = false), 0.001f)

        // Refreshing phase: locked to 1.0f
        assertEquals(1.0f, PullToRefreshHelper.computeIndicatorScale(0.2f, isRefreshing = true), 0.001f)
        assertEquals(1.0f, PullToRefreshHelper.computeIndicatorAlpha(0.2f, isRefreshing = true), 0.001f)
    }

    @Test
    fun testCalculateRemainingDisplayTime_minimumFloor() {
        val startTime = 1000L

        // Fast refresh completed in 200ms -> must wait 500ms more (700ms floor)
        val remaining1 = PullToRefreshHelper.calculateRemainingDisplayTime(
            startTimeMillis = startTime,
            currentTimeMillis = 1200L,
            minVisibleDurationMillis = 700L
        )
        assertEquals(500L, remaining1)

        // Slow refresh completed in 900ms -> 0ms remaining
        val remaining2 = PullToRefreshHelper.calculateRemainingDisplayTime(
            startTimeMillis = startTime,
            currentTimeMillis = 1900L,
            minVisibleDurationMillis = 700L
        )
        assertEquals(0L, remaining2)
    }
}
