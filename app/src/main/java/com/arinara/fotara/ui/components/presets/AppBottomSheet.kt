// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.components.presets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.theme.ElmsSans
import com.arinara.fotara.ui.theme.ElectricSlateTokens
import com.arinara.fotara.ui.theme.SpringPhysics
import com.arinara.fotara.ui.theme.jigglePressClickable
import kotlinx.coroutines.launch

@Composable
fun AppBottomSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Panel",
    isMinimized: Boolean = false,
    onMinimizedChange: ((Boolean) -> Unit)? = null,
    minimizedPillText: String = title,
    containerColor: Color = ElectricSlateTokens.SurfacePanel,
    content: @Composable ColumnScope.() -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val dragOffsetAnim = remember { Animatable(0f) }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            // Semi-transparent scrim
            if (!isMinimized) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ElectricSlateTokens.BgCanvas.copy(alpha = 0.7f))
                        .jigglePressClickable(onClick = onDismiss)
                )
            }

            if (!isMinimized) {
                // Expanded 28dp top-corner bottom sheet
                Box(
                    modifier = modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            translationY = dragOffsetAnim.value
                        }
                        .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                        .background(containerColor)
                        .border(
                            border = BorderStroke(1.dp, ElectricSlateTokens.BorderSubtle),
                            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                        )
                        .navigationBarsPadding()
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragEnd = {
                                    coroutineScope.launch {
                                        if (dragOffsetAnim.value > 150f) {
                                            if (onMinimizedChange != null) {
                                                onMinimizedChange(true)
                                            } else {
                                                onDismiss()
                                            }
                                        }
                                        dragOffsetAnim.animateTo(0f, SpringPhysics.JigglePressSpec)
                                    }
                                },
                                onDragCancel = {
                                    coroutineScope.launch {
                                        dragOffsetAnim.animateTo(0f, SpringPhysics.JigglePressSpec)
                                    }
                                },
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                    // Elastic resistance formula
                                    val current = dragOffsetAnim.value
                                    val damped = if (dragAmount > 0) dragAmount * 0.75f else dragAmount * 0.35f
                                    coroutineScope.launch {
                                        dragOffsetAnim.snapTo((current + damped).coerceAtLeast(0f))
                                    }
                                }
                            )
                        }
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Elastic Drag Handle
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(36.dp)
                                    .height(4.dp)
                                    .clip(CircleShape)
                                    .background(ElectricSlateTokens.BorderSubtle)
                            )
                        }

                        // Sheet Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = title,
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = ElectricSlateTokens.TextPrimary
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (onMinimizedChange != null) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(ElectricSlateTokens.SurfaceElevated)
                                            .jigglePressClickable { onMinimizedChange(true) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Minimize",
                                            tint = ElectricSlateTokens.TextPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                }

                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(ElectricSlateTokens.SurfaceElevated)
                                        .jigglePressClickable(onClick = onDismiss),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = ElectricSlateTokens.TextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        content()
                    }
                }
            } else {
                // Minimized floating pill bar at bottom edge
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(bottom = 16.dp, start = 20.dp, end = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(ElectricSlateTokens.SurfaceElevated)
                            .border(BorderStroke(1.dp, ElectricSlateTokens.PrimaryAccent), CircleShape)
                            .jigglePressClickable { onMinimizedChange?.invoke(false) }
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Expand",
                            tint = ElectricSlateTokens.PrimaryAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = minimizedPillText,
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = ElectricSlateTokens.TextPrimary
                        )
                    }
                }
            }
        }
    }
}
