// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.canvas

import org.junit.Assert.assertEquals
import org.junit.Test

class CanvasDockLayoutHelperTest {

    @Test
    fun testComputeZ4BottomPadding_MeasuredAndCollapsedStates() {
        // Expanded with 64dp measured dock height
        val z4Expanded = CanvasDockLayoutHelper.computeZ4BottomPadding(dockHeightDp = 64f, isCollapsed = false)
        assertEquals(88f, z4Expanded, 0.01f)

        // Collapsed with 20dp measured dock height
        val z4Collapsed = CanvasDockLayoutHelper.computeZ4BottomPadding(dockHeightDp = 20f, isCollapsed = true)
        assertEquals(44f, z4Collapsed, 0.01f)

        // Fallbacks when unmeasured (0dp)
        val z4FallbackExpanded = CanvasDockLayoutHelper.computeZ4BottomPadding(dockHeightDp = 0f, isCollapsed = false)
        assertEquals(86f, z4FallbackExpanded, 0.01f)

        val z4FallbackCollapsed = CanvasDockLayoutHelper.computeZ4BottomPadding(dockHeightDp = 0f, isCollapsed = true)
        assertEquals(48f, z4FallbackCollapsed, 0.01f)
    }

    @Test
    fun testComputeZ7BottomPadding_MeasuredAndCollapsedStates() {
        // Collapsed state always returns 16dp
        assertEquals(16f, CanvasDockLayoutHelper.computeZ7BottomPadding(dockHeightDp = 20f, isCollapsed = true), 0.01f)
        assertEquals(16f, CanvasDockLayoutHelper.computeZ7BottomPadding(dockHeightDp = 0f, isCollapsed = true), 0.01f)

        // Expanded state with measured dock height 64dp
        val z7Expanded = CanvasDockLayoutHelper.computeZ7BottomPadding(dockHeightDp = 64f, isCollapsed = false)
        assertEquals(86f, z7Expanded, 0.01f)

        // Expanded state fallback when unmeasured
        val z7Fallback = CanvasDockLayoutHelper.computeZ7BottomPadding(dockHeightDp = 0f, isCollapsed = false)
        assertEquals(84f, z7Fallback, 0.01f)
    }

    @Test
    fun testComputeZ8TopOffset_DerivedFromMeasuredTopCapsules() {
        // Normal top capsules height 48dp -> offset = 48 + 8 = 56dp
        assertEquals(56f, CanvasDockLayoutHelper.computeZ8TopOffset(48f), 0.01f)

        // Tall status bar / cutout top capsules height 60dp -> offset = 60 + 8 = 68dp
        assertEquals(68f, CanvasDockLayoutHelper.computeZ8TopOffset(60f), 0.01f)

        // Unmeasured fallback
        assertEquals(58f, CanvasDockLayoutHelper.computeZ8TopOffset(0f), 0.01f)
    }
}
