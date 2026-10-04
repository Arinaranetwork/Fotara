// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clipToBounds
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
import kotlinx.coroutines.launch
import java.io.File

/**
 * Generic ZoomableBox implementing zoom-gated swipe navigation, pinch-to-zoom,
 * continuous 2-axis direct panning, and animated double-tap toggling.
 * Reused across Photo Viewer, Review Sliders, and PDF Page Viewer.
 */
@Composable
fun ZoomableBox(
    modifier: Modifier = Modifier,
    maxScale: Float = 4.0f,
    doubleTapScale: Float = 2.5f,
    onZoomChanged: (Boolean) -> Unit = {},
    onSettled: (Float) -> Unit = {},
    content: @Composable (scale: Float) -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val coroutineScope = rememberCoroutineScope()

    // Notify parent whenever zoom state crosses 1.001f
    LaunchedEffect(scale) {
        val isZoomed = scale > 1.001f
        onZoomChanged(isZoomed)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds() // Strictly clamp image to viewport bounds
            // Double-tap gesture detector (animates between 1.0x and doubleTapScale)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { tapOffset ->
                        coroutineScope.launch {
                            val startScale = scale
                            val startOffset = offset
                            val targetScale: Float
                            val targetOffset: Offset

                            if (startScale > 1.001f) {
                                // Reset to fit-to-screen
                                targetScale = 1f
                                targetOffset = Offset.Zero
                            } else {
                                // Zoom into tap point
                                targetScale = doubleTapScale
                                val centerX = size.width / 2f
                                val centerY = size.height / 2f
                                val panX = (centerX - tapOffset.x) * (doubleTapScale - 1f)
                                val panY = (centerY - tapOffset.y) * (doubleTapScale - 1f)
                                val maxOffsetX = (size.width * (doubleTapScale - 1f)) / 2f
                                val maxOffsetY = (size.height * (doubleTapScale - 1f)) / 2f
                                targetOffset = Offset(
                                    panX.coerceIn(-maxOffsetX, maxOffsetX),
                                    panY.coerceIn(-maxOffsetY, maxOffsetY)
                                )
                            }

                            val anim = Animatable(0f)
                            anim.animateTo(
                                targetValue = 1f,
                                animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)
                            ) {
                                val fraction = this.value
                                scale = startScale + (targetScale - startScale) * fraction
                                offset = Offset(
                                    startOffset.x + (targetOffset.x - startOffset.x) * fraction,
                                    startOffset.y + (targetOffset.y - startOffset.y) * fraction
                                )
                            }
                            if (targetScale <= 1.001f) {
                                scale = 1f
                                offset = Offset.Zero
                            }
                            onSettled(targetScale)
                        }
                    }
                )
            }
            // Continuous 1:1 pinch-to-zoom & pan gesture detector (never cancelled during pinch)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val canceled = event.changes.any { it.isConsumed }
                        if (canceled) break

                        val pressedCount = event.changes.count { it.pressed }
                        if (pressedCount >= 2) {
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()
                            val centroid = event.calculateCentroid(useCurrent = true)

                            if (kotlin.math.abs(zoom - 1f) > 0.001f || pan != Offset.Zero) {
                                val newScale = (scale * zoom).coerceIn(1f, maxScale)
                                if (newScale <= 1.01f) {
                                    scale = 1f
                                    offset = Offset.Zero
                                } else {
                                    val centerX = size.width / 2f
                                    val centerY = size.height / 2f
                                    // Exact focal-shift anchoring: keep content under centroid stationary
                                    val focalShiftX = (1f - zoom) * (centroid.x - centerX - offset.x)
                                    val focalShiftY = (1f - zoom) * (centroid.y - centerY - offset.y)

                                    val maxOffsetX = (size.width * (newScale - 1f)) / 2f
                                    val maxOffsetY = (size.height * (newScale - 1f)) / 2f

                                    scale = newScale
                                    offset = Offset(
                                        (offset.x + pan.x + focalShiftX).coerceIn(-maxOffsetX, maxOffsetX),
                                        (offset.y + pan.y + focalShiftY).coerceIn(-maxOffsetY, maxOffsetY)
                                    )
                                }
                                event.changes.forEach { it.consume() }
                            }
                        } else if (pressedCount == 1 && scale > 1.01f) {
                            val pan = event.calculatePan()
                            if (pan != Offset.Zero) {
                                val maxOffsetX = (size.width * (scale - 1f)) / 2f
                                val maxOffsetY = (size.height * (scale - 1f)) / 2f
                                offset = Offset(
                                    (offset.x + pan.x).coerceIn(-maxOffsetX, maxOffsetX),
                                    (offset.y + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                                )
                                event.changes.forEach { it.consume() }
                            }
                        }
                    } while (event.changes.any { it.pressed })
                    onSettled(scale)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
            contentAlignment = Alignment.Center
        ) {
            content(scale)
        }
    }
}

/**
 * ZoomablePhotoViewport implements v1.1 Addendum 6 & v1.5.3 direct response:
 * - Gates horizontal swipe navigation on scale == 1.0f (fit-to-screen).
 * - Enables pinch-to-zoom up to maxScale = 4.0f with instantaneous gesture tracking.
 * - Enables direct 1:1 panning when zoomed in (scale > 1.0f).
 * - Double-tap toggles between 1.0f and 2.5f with a 250ms animation.
 * - Pinching back out (< 1.02f) resets to 1.0f and centers offset.
 * - Reports zoom state to parent via [onZoomChanged] so HorizontalPager can disable swiping.
 */
@Composable
fun ZoomablePhotoViewport(
    photo: Photo,
    imageVersion: Long = 0L,
    modifier: Modifier = Modifier,
    maxScale: Float = 4.0f,
    doubleTapScale: Float = 2.5f,
    onZoomChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val file = remember(photo.fileUri) { File(photo.fileUri) }

    ZoomableBox(
        modifier = modifier,
        maxScale = maxScale,
        doubleTapScale = doubleTapScale,
        onZoomChanged = onZoomChanged
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
