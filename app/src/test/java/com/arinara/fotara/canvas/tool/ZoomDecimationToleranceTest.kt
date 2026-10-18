// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.canvas.tool

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ZoomDecimationToleranceTest {

    private fun calculateTolerance(viewportScale: Float, screenTolerance: Float = 0.8f): Float {
        return (screenTolerance / viewportScale.coerceAtLeast(0.05f)).coerceAtLeast(0.01f)
    }

    @Test
    fun testToleranceAt1xZoom() {
        val tol = calculateTolerance(1.0f)
        assertEquals(0.8f, tol, 0.001f)
    }

    @Test
    fun testToleranceAtHighZoom_PreservesDetail() {
        // At 2x zoom, tolerance in world units is halved (0.4f), preserving twice as much detail
        val tol2x = calculateTolerance(2.0f)
        assertEquals(0.4f, tol2x, 0.001f)

        // At 4x zoom, tolerance in world units is 0.2f
        val tol4x = calculateTolerance(4.0f)
        assertEquals(0.2f, tol4x, 0.001f)

        assertTrue(tol4x < tol2x)
        assertTrue(tol2x < calculateTolerance(1.0f))
    }

    @Test
    fun testToleranceAtLowZoom_PreventsOversampling() {
        // At 0.5x zoom (zoomed out), world tolerance is 1.6f
        val tol05x = calculateTolerance(0.5f)
        assertEquals(1.6f, tol05x, 0.001f)
    }

    @Test
    fun testToleranceAtExtremeLowScale_ClampedSafely() {
        val tolNearZero = calculateTolerance(0.01f)
        // 0.8 / 0.05 = 16.0f
        assertEquals(16.0f, tolNearZero, 0.001f)
    }
}
