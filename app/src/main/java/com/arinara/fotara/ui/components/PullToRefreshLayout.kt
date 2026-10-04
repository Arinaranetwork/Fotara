// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.arinara.fotara.R
import com.arinara.fotara.theme.HomeCardBorder
import com.arinara.fotara.theme.HomeCardSurface
import com.arinara.fotara.theme.HomeMainButtonBlue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PullToRefreshLayout(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    surfaceColor: Color = HomeCardSurface,
    accentColor: Color = HomeMainButtonBlue,
    borderColor: Color = HomeCardBorder,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val triggerDistancePx = with(density) { PullToRefreshHelper.DEFAULT_TRIGGER_DISTANCE_DP.dp.toPx() }
    val restingOffsetPx = with(density) { PullToRefreshHelper.DEFAULT_RESTING_OFFSET_DP.dp.toPx() }

    val pullOffset = remember { Animatable(0f) }
    var isDragging by remember { mutableStateOf(false) }

    val latestEnabled by rememberUpdatedState(enabled)
    val latestIsRefreshing by rememberUpdatedState(isRefreshing)
    val latestOnRefresh by rememberUpdatedState(onRefresh)

    // Track minimum duration
    var refreshStartTime by remember { mutableStateOf(0L) }
    var isHoldingFloor by remember { mutableStateOf(false) }

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            refreshStartTime = System.currentTimeMillis()
            isHoldingFloor = true
            pullOffset.animateTo(restingOffsetPx, tween(250))
        } else if (isHoldingFloor) {
            val remaining = PullToRefreshHelper.calculateRemainingDisplayTime(
                startTimeMillis = refreshStartTime,
                currentTimeMillis = System.currentTimeMillis()
            )
            if (remaining > 0) {
                delay(remaining)
            }
            isHoldingFloor = false
            pullOffset.animateTo(0f, tween(250))
        }
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (!latestEnabled || latestIsRefreshing) return Offset.Zero

                // If user is pulling up while already dragged down, consume delta to collapse indicator
                if (available.y < 0f && pullOffset.value > 0f) {
                    val newOffset = (pullOffset.value + available.y).coerceAtLeast(0f)
                    coroutineScope.launch {
                        pullOffset.snapTo(newOffset)
                    }
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (!latestEnabled || latestIsRefreshing) return Offset.Zero

                if (available.y > 0f && source == NestedScrollSource.UserInput) {
                    isDragging = true
                    // Apply resistance to the pull
                    val resistance = 0.5f
                    val newOffset = (pullOffset.value + available.y * resistance)
                        .coerceAtMost(triggerDistancePx * 1.5f)
                    coroutineScope.launch {
                        pullOffset.snapTo(newOffset)
                    }
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                isDragging = false
                if (!latestEnabled || latestIsRefreshing) return Velocity.Zero

                if (pullOffset.value >= triggerDistancePx) {
                    latestOnRefresh()
                    pullOffset.animateTo(restingOffsetPx, tween(200))
                } else if (pullOffset.value > 0f) {
                    pullOffset.animateTo(0f, tween(200))
                }
                return Velocity.Zero
            }
        }
    }

    // Continuous spin during refresh
    val infiniteTransition = rememberInfiniteTransition(label = "ptr_spin")
    val spinningAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ptr_spin_angle"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
    ) {
        // Child content (e.g. Grid or List)
        content()

        // Indicator floating strictly above top edge fade
        val activeOrHolding = isRefreshing || isHoldingFloor
        val indicatorVisible = pullOffset.value > 0f || activeOrHolding

        if (indicatorVisible) {
            val contentDesc = stringResource(
                if (activeOrHolding) R.string.refreshing_content_description
                else R.string.pull_to_refresh_content_description
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .graphicsLayer {
                        val currentOffset = pullOffset.value
                        val progress = PullToRefreshHelper.computePullProgress(currentOffset, triggerDistancePx)
                        val scale = PullToRefreshHelper.computeIndicatorScale(progress, activeOrHolding)
                        val alphaVal = PullToRefreshHelper.computeIndicatorAlpha(progress, activeOrHolding)

                        // Offset from top of layout
                        translationY = currentOffset - (40.dp.toPx() / 2f)
                        scaleX = scale
                        scaleY = scale
                        alpha = alphaVal
                    }
                    .shadow(elevation = 6.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .background(surfaceColor)
                    .border(BorderStroke(1.dp, borderColor), CircleShape)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = contentDesc,
                    tint = accentColor,
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer {
                            val progress = PullToRefreshHelper.computePullProgress(pullOffset.value, triggerDistancePx)
                            rotationZ = if (activeOrHolding) {
                                spinningAngle
                            } else {
                                PullToRefreshHelper.computeIndicatorRotation(progress)
                            }
                        }
                )
            }
        }
    }
}
