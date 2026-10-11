// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.canvas

/**
 * Pure calculation helpers for dock-dependent offsets (Z4 popup and Z7 zoom chip).
 */
object CanvasDockLayoutHelper {

    /**
     * Computes the bottom padding for the Z4 tool options popup anchored above the bottom dock.
     * When dockHeightDp is 0 (unmeasured), falls back to 68dp (~20% reduction from old 88dp).
     */
    fun computeZ4BottomPadding(dockHeightDp: Float, isCollapsed: Boolean): Float {
        val baseDockHeight = if (dockHeightDp > 0f) dockHeightDp else if (isCollapsed) 24f else 62f
        return baseDockHeight + 16f + 8f
    }

    /**
     * Computes the bottom padding for the Z7 zoom / fit chip anchored relative to the bottom dock.
     * When dockHeightDp is 0 (unmeasured), falls back to 16dp when collapsed and 74dp when expanded
     * (~20% reduction from old 92dp).
     */
    fun computeZ7BottomPadding(dockHeightDp: Float, isCollapsed: Boolean): Float {
        return if (isCollapsed) {
            16f
        } else {
            val baseDockHeight = if (dockHeightDp > 0f) dockHeightDp else 62f
            baseDockHeight + 16f + 6f
        }
    }

    /**
     * Computes the vertical offset for the Z8 contextual selection action bar below the top capsules.
     */
    fun computeZ8TopOffset(topCapsulesHeightDp: Float): Float {
        val baseHeight = if (topCapsulesHeightDp > 0f) topCapsulesHeightDp else 50f
        return baseHeight + 8f
    }
}
