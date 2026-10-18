// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.components

object PullToRefreshHelper {

    const val DEFAULT_TRIGGER_DISTANCE_DP = 72f
    const val DEFAULT_RESTING_OFFSET_DP = 56f
    const val MIN_REFRESHING_DURATION_MS = 700L
    const val REFRESH_TIMEOUT_MS = 10000L

    fun canTriggerRefresh(
        isAtTop: Boolean,
        isMultiSelectActive: Boolean,
        isSearchFocused: Boolean,
        isOverlayOpen: Boolean
    ): Boolean {
        return isAtTop && !isMultiSelectActive && !isSearchFocused && !isOverlayOpen
    }

    fun computePullProgress(pullDistancePx: Float, triggerDistancePx: Float): Float {
        if (triggerDistancePx <= 0f) return 0f
        return (pullDistancePx / triggerDistancePx).coerceIn(0f, 1f)
    }

    fun computeIndicatorRotation(progress: Float): Float {
        return progress.coerceIn(0f, 1f) * 360f
    }

    fun computeIndicatorScale(progress: Float, isRefreshing: Boolean): Float {
        if (isRefreshing) return 1f
        return progress.coerceIn(0f, 1f)
    }

    fun computeIndicatorAlpha(progress: Float, isRefreshing: Boolean): Float {
        if (isRefreshing) return 1f
        return progress.coerceIn(0f, 1f)
    }

    fun calculateRemainingDisplayTime(
        startTimeMillis: Long,
        currentTimeMillis: Long,
        minVisibleDurationMillis: Long = MIN_REFRESHING_DURATION_MS
    ): Long {
        val elapsed = currentTimeMillis - startTimeMillis
        return (minVisibleDurationMillis - elapsed).coerceAtLeast(0L)
    }
}
