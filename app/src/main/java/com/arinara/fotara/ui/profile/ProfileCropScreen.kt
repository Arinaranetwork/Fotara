// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.profile

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import com.arinara.fotara.ui.components.SettingsSubScreenHeader
import com.arinara.fotara.ui.components.SettingsSubScreenHeaderDefaults
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.R
import com.arinara.fotara.theme.ElmsSans
import com.arinara.fotara.theme.HomeMainButtonBlue
import com.arinara.fotara.theme.HomeNearBlack
import com.arinara.fotara.theme.HomeSubtitleGray
import com.arinara.fotara.util.ProfileImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

enum class CropShape {
    CIRCLE,
    RECTANGLE
}

private sealed interface ActiveGesture {
    object None : ActiveGesture
    object DragBox : ActiveGesture
    data class ResizeCorner(val corner: CropCorner) : ActiveGesture
    object PinchZoom : ActiveGesture
}

@Composable
fun ProfileCropScreen(
    imageUri: Uri,
    isAvatar: Boolean,
    aspectRatio: Float = if (isAvatar) 1.0f else (16f / 9f),
    onCropSaved: (Bitmap, NormalizedCropRect) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onCancel() }

    val context = LocalContext.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    var previewBitmap by remember(imageUri) { mutableStateOf<Bitmap?>(null) }
    var decodeFailed by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(imageUri) {
        withContext(Dispatchers.IO) {
            val bmp = ProfileImageUtils.decodeSampledBitmap(context, imageUri, 2048)
            if (bmp != null) {
                previewBitmap = bmp
            } else {
                decodeFailed = true
            }
        }
    }

    if (decodeFailed) {
        LaunchedEffect(Unit) {
            Toast.makeText(context, context.getString(R.string.profile_load_failed), Toast.LENGTH_SHORT).show()
            onCancel()
        }
        return
    }

    val title = if (isAvatar) {
        stringResource(R.string.profile_crop_title_picture)
    } else {
        stringResource(R.string.profile_crop_title_banner)
    }

    // Normalized crop box: [left, top, right, bottom] in 0f..1f relative to displayed image
    val normBoxSaver = remember {
        Saver<MutableState<CropRectF?>, List<Float>>(
            save = { state ->
                state.value?.let { listOf(it.left, it.top, it.right, it.bottom) }
            },
            restore = { list ->
                mutableStateOf(if (list.size == 4) CropRectF(list[0], list[1], list[2], list[3]) else null)
            }
        )
    }
    val normBoxState = rememberSaveable(saver = normBoxSaver) {
        mutableStateOf(null)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(HomeNearBlack)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar using unified SettingsSubScreenHeader
            SettingsSubScreenHeader(
                title = title,
                onBackClick = onCancel,
                navigationIcon = Icons.Default.Close,
                navigationContentDescription = stringResource(R.string.profile_crop_cancel),
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = SettingsSubScreenHeaderDefaults.HorizontalPadding),
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Reset Action
                        TextButton(
                            onClick = {
                                val bmp = previewBitmap
                                if (bmp != null) {
                                    val init = CropBoxGeometry.computeInitialBox(
                                        bmp.width.toFloat(),
                                        bmp.height.toFloat(),
                                        aspectRatio
                                    )
                                    normBoxState.value = CropRectF(
                                        init.left / bmp.width.toFloat(),
                                        init.top / bmp.height.toFloat(),
                                        init.right / bmp.width.toFloat(),
                                        init.bottom / bmp.height.toFloat()
                                    )
                                }
                            },
                            enabled = !isSaving && previewBitmap != null
                        ) {
                            Text(
                                text = stringResource(R.string.profile_crop_reset),
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 14.sp,
                                fontFamily = ElmsSans
                            )
                        }

                        // Save Check Action
                        IconButton(
                            onClick = {
                                if (isSaving) return@IconButton
                                val bmp = previewBitmap ?: return@IconButton
                                val currentNorm = normBoxState.value ?: run {
                                    val init = CropBoxGeometry.computeInitialBox(
                                        bmp.width.toFloat(),
                                        bmp.height.toFloat(),
                                        aspectRatio
                                    )
                                    CropRectF(
                                        init.left / bmp.width.toFloat(),
                                        init.top / bmp.height.toFloat(),
                                        init.right / bmp.width.toFloat(),
                                        init.bottom / bmp.height.toFloat()
                                    )
                                }

                                isSaving = true
                                coroutineScope.launch {
                                    val cropped = withContext(Dispatchers.IO) {
                                        val sub = ProfileImageUtils.cropNormalized(
                                            source = bmp,
                                            normLeft = currentNorm.left,
                                            normTop = currentNorm.top,
                                            normRight = currentNorm.right,
                                            normBottom = currentNorm.bottom
                                        ) ?: return@withContext null

                                        if (isAvatar) {
                                            Bitmap.createScaledBitmap(sub, 512, 512, true)
                                        } else {
                                            val targetWidth = min(sub.width, 1080).coerceAtLeast(1)
                                            val ratio = targetWidth.toFloat() / sub.width.toFloat()
                                            val targetHeight = (sub.height * ratio).toInt().coerceAtLeast(1)
                                            if (sub.width != targetWidth || sub.height != targetHeight) {
                                                Bitmap.createScaledBitmap(sub, targetWidth, targetHeight, true)
                                            } else {
                                                sub
                                            }
                                        }
                                    }

                                    if (cropped != null) {
                                        val normRect = NormalizedCropRect(
                                            left = currentNorm.left,
                                            top = currentNorm.top,
                                            right = currentNorm.right,
                                            bottom = currentNorm.bottom
                                        )
                                        onCropSaved(cropped, normRect)
                                    } else {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.profile_load_failed),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        isSaving = false
                                    }
                                }
                            },
                            enabled = !isSaving && previewBitmap != null
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    color = HomeMainButtonBlue,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = stringResource(R.string.profile_crop_save),
                                    tint = HomeMainButtonBlue
                                )
                            }
                        }
                    }
                }
            )

            // Muted Hint Line Below Top Bar
            Text(
                text = stringResource(R.string.profile_crop_hint),
                color = HomeSubtitleGray,
                fontSize = 12.sp,
                fontFamily = ElmsSans,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Interactive Crop Viewport Area
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                val currentBmp = previewBitmap
                if (currentBmp == null) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        CircularProgressIndicator(
                            color = HomeMainButtonBlue,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    return@BoxWithConstraints
                }

                val viewportW = constraints.maxWidth.toFloat()
                val viewportH = constraints.maxHeight.toFloat()
                val padPx = with(density) { 24.dp.toPx() }
                val usableW = (viewportW - padPx * 2f).coerceAtLeast(10f)
                val usableH = (viewportH - padPx * 2f).coerceAtLeast(10f)

                val bmpW = currentBmp.width.toFloat()
                val bmpH = currentBmp.height.toFloat()

                val fitScale = min(usableW / bmpW, usableH / bmpH)
                val renderW = bmpW * fitScale
                val renderH = bmpH * fitScale

                val imgLeft = (viewportW - renderW) / 2f
                val imgTop = (viewportH - renderH) / 2f

                // Initialize crop box if not set yet
                val normBox = normBoxState.value ?: run {
                    val initialDisplayBox = CropBoxGeometry.computeInitialBox(bmpW, bmpH, aspectRatio)
                    val initialNorm = CropRectF(
                        initialDisplayBox.left / bmpW,
                        initialDisplayBox.top / bmpH,
                        initialDisplayBox.right / bmpW,
                        initialDisplayBox.bottom / bmpH
                    )
                    normBoxState.value = initialNorm
                    initialNorm
                }

                // Fitted preview image
                Image(
                    bitmap = currentBmp.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(with(density) { renderW.toDp() }, with(density) { renderH.toDp() })
                        .offset(with(density) { imgLeft.toDp() }, with(density) { imgTop.toDp() })
                )

                // Interactive Crop Overlay Canvas with Stable Pointer Keys
                val hitRadiusPx = with(density) { 26.dp.toPx() }
                val minScreenSizePx = with(density) { 64.dp.toPx() }
                val minDisplayWidth = max(minScreenSizePx / fitScale, 64f)

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(aspectRatio) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                var activeGesture: ActiveGesture = ActiveGesture.None

                                val curNorm = normBoxState.value ?: return@awaitEachGesture
                                val sLeft = imgLeft + curNorm.left * renderW
                                val sTop = imgTop + curNorm.top * renderH
                                val sRight = imgLeft + curNorm.right * renderW
                                val sBottom = imgTop + curNorm.bottom * renderH

                                // Check corners first (44dp target)
                                val dTL = hypot(down.position.x - sLeft, down.position.y - sTop)
                                val dTR = hypot(down.position.x - sRight, down.position.y - sTop)
                                val dBL = hypot(down.position.x - sLeft, down.position.y - sBottom)
                                val dBR = hypot(down.position.x - sRight, down.position.y - sBottom)

                                val minCornerDist = min(min(dTL, dTR), min(dBL, dBR))
                                if (minCornerDist <= hitRadiusPx) {
                                    activeGesture = when (minCornerDist) {
                                        dTL -> ActiveGesture.ResizeCorner(CropCorner.TOP_LEFT)
                                        dTR -> ActiveGesture.ResizeCorner(CropCorner.TOP_RIGHT)
                                        dBL -> ActiveGesture.ResizeCorner(CropCorner.BOTTOM_LEFT)
                                        else -> ActiveGesture.ResizeCorner(CropCorner.BOTTOM_RIGHT)
                                    }
                                } else if (down.position.x in sLeft..sRight && down.position.y in sTop..sBottom) {
                                    activeGesture = ActiveGesture.DragBox
                                }

                                var previousPinchDistance: Float? = null

                                do {
                                    val event = awaitPointerEvent()
                                    val activePointers = event.changes.filter { it.pressed }

                                    if (activePointers.size >= 2) {
                                        // Two-finger Pinch Gesture
                                        val p1 = activePointers[0].position
                                        val p2 = activePointers[1].position
                                        val currentDistance = hypot(p1.x - p2.x, p1.y - p2.y)
                                        val prevDist = previousPinchDistance

                                        if (prevDist != null && prevDist > 0f && currentDistance > 0f) {
                                            val scaleFactor = currentDistance / prevDist
                                            val activeNorm = normBoxState.value ?: curNorm
                                            val dispBox = CropRectF(
                                                activeNorm.left * bmpW,
                                                activeNorm.top * bmpH,
                                                activeNorm.right * bmpW,
                                                activeNorm.bottom * bmpH
                                            )
                                            val scaledDisp = CropBoxGeometry.pinchScale(
                                                current = dispBox,
                                                scaleFactor = scaleFactor,
                                                imageWidth = bmpW,
                                                imageHeight = bmpH,
                                                aspectRatio = aspectRatio,
                                                minWidth = minDisplayWidth
                                            )
                                            normBoxState.value = CropRectF(
                                                scaledDisp.left / bmpW,
                                                scaledDisp.top / bmpH,
                                                scaledDisp.right / bmpW,
                                                scaledDisp.bottom / bmpH
                                            )
                                        }
                                        previousPinchDistance = currentDistance
                                        event.changes.forEach { it.consume() }
                                    } else if (activePointers.size == 1) {
                                        previousPinchDistance = null
                                        val pointer = activePointers[0]
                                        val activeNorm = normBoxState.value ?: curNorm
                                        val dispBox = CropRectF(
                                            activeNorm.left * bmpW,
                                            activeNorm.top * bmpH,
                                            activeNorm.right * bmpW,
                                            activeNorm.bottom * bmpH
                                        )

                                        when (val g = activeGesture) {
                                            is ActiveGesture.ResizeCorner -> {
                                                val touchInDisplayX = (pointer.position.x - imgLeft) / fitScale
                                                val touchInDisplayY = (pointer.position.y - imgTop) / fitScale
                                                val resizedDisp = CropBoxGeometry.resizeCorner(
                                                    current = dispBox,
                                                    corner = g.corner,
                                                    touchX = touchInDisplayX,
                                                    touchY = touchInDisplayY,
                                                    imageWidth = bmpW,
                                                    imageHeight = bmpH,
                                                    aspectRatio = aspectRatio,
                                                    minWidth = minDisplayWidth
                                                )
                                                normBoxState.value = CropRectF(
                                                    resizedDisp.left / bmpW,
                                                    resizedDisp.top / bmpH,
                                                    resizedDisp.right / bmpW,
                                                    resizedDisp.bottom / bmpH
                                                )
                                                pointer.consume()
                                            }
                                            is ActiveGesture.DragBox -> {
                                                val drag = pointer.positionChange()
                                                if (drag != Offset.Zero) {
                                                    val pannedDisp = CropBoxGeometry.pan(
                                                        current = dispBox,
                                                        dx = drag.x / fitScale,
                                                        dy = drag.y / fitScale,
                                                        imageWidth = bmpW,
                                                        imageHeight = bmpH
                                                    )
                                                    normBoxState.value = CropRectF(
                                                        pannedDisp.left / bmpW,
                                                        pannedDisp.top / bmpH,
                                                        pannedDisp.right / bmpW,
                                                        pannedDisp.bottom / bmpH
                                                    )
                                                    pointer.consume()
                                                }
                                            }
                                            else -> {}
                                        }
                                    }
                                } while (event.changes.any { it.pressed })
                            }
                        }
                ) {
                    val cur = normBoxState.value ?: normBox
                    val bLeft = imgLeft + cur.left * renderW
                    val bTop = imgTop + cur.top * renderH
                    val bRight = imgLeft + cur.right * renderW
                    val bBottom = imgTop + cur.bottom * renderH
                    val bWidth = bRight - bLeft
                    val bHeight = bBottom - bTop

                    // 1. Dimmed Scrim outside Crop Box
                    val cutoutPath = Path().apply {
                        addRect(Rect(bLeft, bTop, bRight, bBottom))
                    }
                    clipPath(cutoutPath, clipOp = ClipOp.Difference) {
                        drawRect(color = Color.Black.copy(alpha = 0.72f))
                    }

                    // 2. Circular Guide inside Square for Profile Picture
                    if (isAvatar) {
                        drawOval(
                            color = Color.White.copy(alpha = 0.45f),
                            topLeft = Offset(bLeft, bTop),
                            size = Size(bWidth, bHeight),
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }

                    // 3. Thin Crop Box Border
                    drawRect(
                        color = Color.White.copy(alpha = 0.9f),
                        topLeft = Offset(bLeft, bTop),
                        size = Size(bWidth, bHeight),
                        style = Stroke(width = 1.5.dp.toPx())
                    )

                    // 4. Four Corner Handles (Small visible dots)
                    val dotRadius = 4.5.dp.toPx()
                    val corners = listOf(
                        Offset(bLeft, bTop),
                        Offset(bRight, bTop),
                        Offset(bLeft, bBottom),
                        Offset(bRight, bBottom)
                    )
                    for (c in corners) {
                        drawCircle(
                            color = Color.Black.copy(alpha = 0.5f),
                            radius = dotRadius + 1.dp.toPx(),
                            center = c
                        )
                        drawCircle(
                            color = Color.White,
                            radius = dotRadius,
                            center = c
                        )
                    }
                }
            }
        }
    }
}

/**
 * Backward compatibility overload for legacy callers.
 */
@Deprecated("Use Uri-based ProfileCropScreen for original pixel decoding")
@Composable
fun ProfileCropScreen(
    bitmap: Bitmap,
    cropShape: CropShape,
    cropAspectRatio: Float = if (cropShape == CropShape.CIRCLE) 1.0f else 1.64f,
    onCropSaved: (Bitmap) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Wrap bitmap into a memory uri or temporary fallback
    val context = LocalContext.current
    var tempUri by remember(bitmap) { mutableStateOf<Uri?>(null) }

    LaunchedEffect(bitmap) {
        withContext(Dispatchers.IO) {
            val file = java.io.File(context.cacheDir, "temp_crop_${System.currentTimeMillis()}.png")
            java.io.FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            tempUri = Uri.fromFile(file)
        }
    }

    val uri = tempUri
    if (uri != null) {
        ProfileCropScreen(
            imageUri = uri,
            isAvatar = cropShape == CropShape.CIRCLE,
            aspectRatio = cropAspectRatio,
            onCropSaved = onCropSaved,
            onCancel = onCancel,
            modifier = modifier
        )
    } else {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .fillMaxSize()
                .background(HomeNearBlack)
        ) {
            CircularProgressIndicator(color = HomeMainButtonBlue)
        }
    }
}

@Composable
fun ProfileCropScreen(
    imageUri: Uri,
    isAvatar: Boolean,
    aspectRatio: Float = if (isAvatar) 1.0f else (16f / 9f),
    onCropSaved: (Bitmap) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    ProfileCropScreen(
        imageUri = imageUri,
        isAvatar = isAvatar,
        aspectRatio = aspectRatio,
        onCropSaved = { bmp, _ -> onCropSaved(bmp) },
        onCancel = onCancel,
        modifier = modifier
    )
}

