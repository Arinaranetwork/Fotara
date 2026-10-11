// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.home

/**
 * Pure calculation helpers for Home screen layout and bottom content padding.
 */
object HomeLayoutHelper {

    /**
     * Computes the dynamic bottom content padding for scrollable grids/lists above floating docks.
     *
     * @param measuredBottomStackHeightDp The runtime measured height of the floating dock + navigation bar + insets.
     * @param additionalBufferDp Extra spacing to ensure the last item is comfortably above the floating dock.
     * @param fallbackPaddingDp Fallback padding when height has not yet been measured (0dp).
     */
    fun computeBottomContentPadding(
        measuredBottomStackHeightDp: Float,
        additionalBufferDp: Float = 16f,
        fallbackPaddingDp: Float = 160f
    ): Float {
        return if (measuredBottomStackHeightDp > 0f) {
            measuredBottomStackHeightDp + additionalBufferDp
        } else {
            fallbackPaddingDp
        }
    }
}
