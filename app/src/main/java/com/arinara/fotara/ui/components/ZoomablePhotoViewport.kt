// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.arinara.fotara.data.model.Photo
import com.arinara.fotara.theme.MidnightCardOutline
import java.io.File

/**
 * ZoomablePhotoViewport implements v1.1 Addendum 6:
 * - Gates horizontal swipe navigation on scale == 1.0f (fit-to-screen).
 * - Enables pinch-to-zoom up to maxScale = 3.0f (proposed default).
 * - Enables panning when zoomed in (scale > 1.0f).
 * - Double-tap toggles between 1.0f and 2.0f with a 250ms animation (proposed default).
 * - Pinching back out (< 1.05f) resets to 1.0f and centers offset.
 * - Reports zoom state to parent via [onZoomChanged] so HorizontalPager can disable swiping.
 */
@Composable
fun ZoomablePhotoViewport(
    photo: Photo,
    imageVersion: Long = 0L,
    modifier: Modifier = Modifier,
    maxScale: Float = 3.0f,
    doubleTapScale: Float = 2.0f,
    onZoomChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current

    var targetScale by remember { mutableFloatStateOf(1f) }
    var targetOffset by remember { mutableStateOf(Offset.Zero) }

    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "scaleAnimation"
    )

    val animatedOffset by animateOffsetAsState(
        targetValue = targetOffset,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "offsetAnimation"
    )

    // Notify parent whenever zoom state crosses 1.001f
    LaunchedEffect(targetScale) {
        val isZoomed = targetScale > 1.001f
        onZoomChanged(isZoomed)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            // Double-tap gesture detector (toggles 1.0x vs doubleTapScale)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { tapOffset ->
                        if (targetScale > 1.001f) {
                            // Reset to fit-to-screen
                            targetScale = 1f
                            targetOffset = Offset.Zero
                        } else {
                            // Zoom into tap point
                            targetScale = doubleTapScale
                            // Center offset towards tap
                            val centerX = size.width / 2f
                            val centerY = size.height / 2f
                            val panX = (centerX - tapOffset.x) * (doubleTapScale - 1f)
                            val panY = (centerY - tapOffset.y) * (doubleTapScale - 1f)
                            targetOffset = Offset(panX, panY)
                        }
                    }
                )
            }
            // Pinch-to-zoom & pan gesture detector
            .pointerInput(targetScale) {
                if (targetScale > 1.001f) {
                    // Zoomed in: consume both pan and zoom; HorizontalPager will NOT advance pages
                    detectTransformGestures { _, pan, zoom, _ ->
                        val newScale = (targetScale * zoom).coerceIn(1f, maxScale)
                        if (newScale < 1.05f) {
                            targetScale = 1f
                            targetOffset = Offset.Zero
                        } else {
                            targetScale = newScale
                            // Adjust pan with bounds
                            val maxOffsetX = (size.width * (targetScale - 1f)) / 2f
                            val maxOffsetY = (size.height * (targetScale - 1f)) / 2f
                            val newOffsetX = (targetOffset.x + pan.x).coerceIn(-maxOffsetX, maxOffsetX)
                            val newOffsetY = (targetOffset.y + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                            targetOffset = Offset(newOffsetX, newOffsetY)
                        }
                    }
                } else {
                    // Not zoomed in (scale = 1.0f): ONLY detect two-finger pinch.
                    // Single finger drag passes through freely to HorizontalPager for page navigation.
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        do {
                            val event = awaitPointerEvent()
                            if (event.changes.size >= 2) {
                                val zoom = event.calculateZoom()
                                if (zoom > 1.02f) {
                                    targetScale = (targetScale * zoom).coerceIn(1f, maxScale)
                                    event.changes.forEach { it.consume() }
                                }
                            }
                        } while (event.changes.any { it.pressed })
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        val file = File(photo.fileUri)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = animatedScale,
                    scaleY = animatedScale,
                    translationX = animatedOffset.x,
                    translationY = animatedOffset.y
                ),
            contentAlignment = Alignment.Center
        ) {
            if (file.exists()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(file)
                        .setParameter("v", imageVersion)
                        .crossfade(true)
                        .build(),
                    contentDescription = photo.caption ?: "Note photo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize(0.85f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF070B1F))
                        .border(1.dp, MidnightCardOutline, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Text(
                        text = photo.caption ?: "Note Photo",
                        color = Color.White
                    )
                }
            }
        }
    }
}
