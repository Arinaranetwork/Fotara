// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * State holder that coordinates the tab container's persistent bottom clearance
 * and action routing for the single floating (+) button.
 *
 * Guarantees that:
 * 1. Bottom overlay height is initialized with a deterministic non-zero value, eliminating first-frame jumps.
 * 2. Bottom overlay clearance is never reset to zero across tab switches.
 * 3. The last measured height is retained indefinitely.
 */
class TabContainerBottomState(
    initialHeightDp: Dp = DEFAULT_BOTTOM_CLEARANCE_DP
) {
    var bottomOverlayPaddingDp: Dp by mutableStateOf(initialHeightDp)
        private set

    fun onHeightMeasured(heightPx: Int, density: Density) {
        if (heightPx > 0) {
            val measuredDp = with(density) { heightPx.toDp() }
            if (measuredDp > 0.dp) {
                bottomOverlayPaddingDp = measuredDp
            }
        }
    }

    companion object {
        val DEFAULT_BOTTOM_CLEARANCE_DP: Dp = 126.dp
    }
}
