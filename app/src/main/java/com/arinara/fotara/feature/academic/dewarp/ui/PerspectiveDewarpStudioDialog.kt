// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.feature.academic.dewarp.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.arinara.fotara.feature.academic.dewarp.BitmapDewarpUtils
import com.arinara.fotara.feature.academic.dewarp.DewarpFilterMode
import com.arinara.fotara.feature.academic.dewarp.QuadPoint
import com.arinara.fotara.ui.components.presets.AppButton
import com.arinara.fotara.ui.components.presets.AppButtonVariant
import com.arinara.fotara.ui.theme.ElectricSlateTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.hypot
import kotlin.math.min

/**
 * Fullscreen Interactive Perspective Dewarp Studio.
 * Provides a 4-corner draggable quadrilateral pin overlay with real-time 2.5x magnifying loupe,
 * Gaussian homography projective transformation, and academic optical contrast filters.
 */
@Composable
fun PerspectiveDewarpStudioDialog(
    imageFile: File,
    onApplyDewarp: (File) -> Unit,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoadingPreview by remember { mutableStateOf(true) }
    var isProcessingWarp by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    // Filter selection state
    var selectedFilter by remember { mutableStateOf(DewarpFilterMode.ORIGINAL) }

    // Normalized quad corners [0f..1f]: Top-Left, Top-Right, Bottom-Right, Bottom-Left
    var tl by remember { mutableStateOf(Offset(0.05f, 0.05f)) }
    var tr by remember { mutableStateOf(Offset(0.95f, 0.05f)) }
    var br by remember { mutableStateOf(Offset(0.95f, 0.95f)) }
    var bl by remember { mutableStateOf(Offset(0.05f, 0.95f)) }

    // Active corner drag index: 0=TL, 1=TR, 2=BR, 3=BL, -1=None
    var activeDragCorner by remember { mutableIntStateOf(-1) }

    // Load preview bitmap safely in background
    LaunchedEffect(imageFile) {
        isLoadingPreview = true
        val bmp = withContext(Dispatchers.IO) {
            BitmapDewarpUtils.loadPreviewBitmap(imageFile, maxDimensionPx = 1400)
        }
        previewBitmap = bmp
        isLoadingPreview = false
    }

    Dialog(
        onDismissRequest = { if (!isProcessingWarp) onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !isProcessingWarp,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = ElectricSlateTokens.BgCanvas
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Top Bar: Dismiss, Title, Reset, Apply
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        enabled = !isProcessingWarp
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel",
                            tint = ElectricSlateTokens.TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Perspective Dewarp",
                        color = ElectricSlateTokens.TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Reset Quad Corners button
                    IconButton(
                        onClick = {
                            tl = Offset(0.05f, 0.05f)
                            tr = Offset(0.95f, 0.05f)
                            br = Offset(0.95f, 0.95f)
                            bl = Offset(0.05f, 0.95f)
                        },
                        enabled = !isProcessingWarp
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Corners",
                            tint = ElectricSlateTokens.TextMuted
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    AppButton(
                        text = "Apply",
                        onClick = {
                            if (isProcessingWarp) return@AppButton
                            isProcessingWarp = true
                            statusMessage = "Rectifying document..."
                            coroutineScope.launch {
                                val success = withContext(Dispatchers.IO) {
                                    val quad = listOf(
                                        QuadPoint(tl.x, tl.y),
                                        QuadPoint(tr.x, tr.y),
                                        QuadPoint(br.x, br.y),
                                        QuadPoint(bl.x, bl.y)
                                    )
                                    BitmapDewarpUtils.dewarpImageFile(
                                        sourceFile = imageFile,
                                        destinationFile = imageFile,
                                        normalizedCorners = quad,
                                        filterMode = selectedFilter
                                    )
                                }
                                isProcessingWarp = false
                                if (success) {
                                    onApplyDewarp(imageFile)
                                    onDismiss()
                                } else {
                                    statusMessage = "Rectification failed"
                                }
                            }
                        },
                        variant = AppButtonVariant.PRIMARY,
                        enabled = !isProcessingWarp && previewBitmap != null
                    )
                }

                // Interactive Quad Canvas with Loupe
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoadingPreview || previewBitmap == null) {
                        CircularProgressIndicator(
                            color = ElectricSlateTokens.PrimaryAccent,
                            modifier = Modifier.size(36.dp)
                        )
                    } else {
                        val bmp = previewBitmap!!
                        val bmpImageBitmap = remember(bmp) { bmp.asImageBitmap() }

                        BoxWithConstraints(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            val containerWidth = constraints.maxWidth.toFloat()
                            val containerHeight = constraints.maxHeight.toFloat()

                            val fitScale = min(
                                containerWidth / bmp.width.toFloat(),
                                containerHeight / bmp.height.toFloat()
                            )
                            val displayedW = bmp.width * fitScale
                            val displayedH = bmp.height * fitScale
                            val offsetX = (containerWidth - displayedW) / 2f
                            val offsetY = (containerHeight - displayedH) / 2f

                            // Helper functions to map normalized <-> screen coordinates
                            fun normToScreen(n: Offset): Offset =
                                Offset(offsetX + n.x * displayedW, offsetY + n.y * displayedH)

                            fun screenToNorm(s: Offset): Offset {
                                val nx = ((s.x - offsetX) / displayedW).coerceIn(0f, 1f)
                                val ny = ((s.y - offsetY) / displayedH).coerceIn(0f, 1f)
                                return Offset(nx, ny)
                            }

                            // Touch event handling for 4 draggable corner pins
                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(Unit) {
                                        detectDragGestures(
                                            onDragStart = { startPos ->
                                                val pTL = normToScreen(tl)
                                                val pTR = normToScreen(tr)
                                                val pBR = normToScreen(br)
                                                val pBL = normToScreen(bl)

                                                val hitRadius = 40.dp.toPx()
                                                val dTL = hypot((startPos.x - pTL.x).toDouble(), (startPos.y - pTL.y).toDouble())
                                                val dTR = hypot((startPos.x - pTR.x).toDouble(), (startPos.y - pTR.y).toDouble())
                                                val dBR = hypot((startPos.x - pBR.x).toDouble(), (startPos.y - pBR.y).toDouble())
                                                val dBL = hypot((startPos.x - pBL.x).toDouble(), (startPos.y - pBL.y).toDouble())

                                                val minDist = minOf(dTL, dTR, dBR, dBL)
                                                if (minDist <= hitRadius) {
                                                    activeDragCorner = when (minDist) {
                                                        dTL -> 0
                                                        dTR -> 1
                                                        dBR -> 2
                                                        else -> 3
                                                    }
                                                } else {
                                                    activeDragCorner = -1
                                                }
                                            },
                                            onDragEnd = { activeDragCorner = -1 },
                                            onDragCancel = { activeDragCorner = -1 },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                if (activeDragCorner >= 0) {
                                                    val deltaNormX = dragAmount.x / displayedW
                                                    val deltaNormY = dragAmount.y / displayedH

                                                    when (activeDragCorner) {
                                                        0 -> tl = Offset((tl.x + deltaNormX).coerceIn(0f, 0.98f), (tl.y + deltaNormY).coerceIn(0f, 0.98f))
                                                        1 -> tr = Offset((tr.x + deltaNormX).coerceIn(0.02f, 1f), (tr.y + deltaNormY).coerceIn(0f, 0.98f))
                                                        2 -> br = Offset((br.x + deltaNormX).coerceIn(0.02f, 1f), (br.y + deltaNormY).coerceIn(0.02f, 1f))
                                                        3 -> bl = Offset((bl.x + deltaNormX).coerceIn(0f, 0.98f), (bl.y + deltaNormY).coerceIn(0.02f, 1f))
                                                    }
                                                }
                                            }
                                        )
                                    }
                            ) {
                                // 1. Draw base preview image scaled to fit
                                drawImage(
                                    image = bmpImageBitmap,
                                    dstOffset = IntOffset(offsetX.toInt(), offsetY.toInt()),
                                    dstSize = IntSize(displayedW.toInt(), displayedH.toInt())
                                )

                                val sTL = normToScreen(tl)
                                val sTR = normToScreen(tr)
                                val sBR = normToScreen(br)
                                val sBL = normToScreen(bl)

                                // 2. Draw quadrilateral fill and structural frame
                                val quadPath = Path().apply {
                                    moveTo(sTL.x, sTL.y)
                                    lineTo(sTR.x, sTR.y)
                                    lineTo(sBR.x, sBR.y)
                                    lineTo(sBL.x, sBL.y)
                                    close()
                                }

                                drawPath(
                                    path = quadPath,
                                    color = ElectricSlateTokens.PrimaryAccent.copy(alpha = 0.14f)
                                )
                                drawPath(
                                    path = quadPath,
                                    color = ElectricSlateTokens.PrimaryAccent,
                                    style = Stroke(width = 2.dp.toPx())
                                )

                                // 3. Draw grid guideline cross
                                drawLine(
                                    color = ElectricSlateTokens.PrimaryAccent.copy(alpha = 0.35f),
                                    start = Offset((sTL.x + sBL.x) / 2f, (sTL.y + sBL.y) / 2f),
                                    end = Offset((sTR.x + sBR.x) / 2f, (sTR.y + sBR.y) / 2f),
                                    strokeWidth = 1.dp.toPx()
                                )
                                drawLine(
                                    color = ElectricSlateTokens.PrimaryAccent.copy(alpha = 0.35f),
                                    start = Offset((sTL.x + sTR.x) / 2f, (sTL.y + sTR.y) / 2f),
                                    end = Offset((sBL.x + sBR.x) / 2f, (sBL.y + sBR.y) / 2f),
                                    strokeWidth = 1.dp.toPx()
                                )

                                // 4. Draw 4 corner anchor handles
                                val corners = listOf(sTL, sTR, sBR, sBL)
                                corners.forEachIndexed { index, cornerPos ->
                                    val isDraggingThis = (activeDragCorner == index)
                                    val outerRadius = if (isDraggingThis) 18.dp.toPx() else 14.dp.toPx()

                                    // Outer elevated circle
                                    drawCircle(
                                        color = ElectricSlateTokens.SurfaceElevated,
                                        radius = outerRadius,
                                        center = cornerPos
                                    )
                                    // Primary accent ring
                                    drawCircle(
                                        color = ElectricSlateTokens.PrimaryAccent,
                                        radius = outerRadius,
                                        center = cornerPos,
                                        style = Stroke(width = 2.5.dp.toPx())
                                    )
                                    // Center indicator dot
                                    drawCircle(
                                        color = ElectricSlateTokens.PrimaryAccent,
                                        radius = 4.dp.toPx(),
                                        center = cornerPos
                                    )
                                }

                                // 5. Magnifying circular loupe positioned above the user's active touch
                                if (activeDragCorner in 0..3) {
                                    val activeScreenPos = corners[activeDragCorner]
                                    val activeNormPos = when (activeDragCorner) {
                                        0 -> tl
                                        1 -> tr
                                        2 -> br
                                        else -> bl
                                    }

                                    val loupeRadius = 48.dp.toPx()
                                    // Position loupe 70.dp above the finger, or below if near top
                                    val loupeCenterY = if (activeScreenPos.y - 80.dp.toPx() > loupeRadius + 10.dp.toPx()) {
                                        activeScreenPos.y - 80.dp.toPx()
                                    } else {
                                        activeScreenPos.y + 80.dp.toPx()
                                    }
                                    val loupeCenter = Offset(
                                        x = activeScreenPos.x.coerceIn(loupeRadius + 10f, containerWidth - loupeRadius - 10f),
                                        y = loupeCenterY
                                    )

                                    val loupePath = Path().apply {
                                        addOval(Rect(loupeCenter, loupeRadius))
                                    }

                                    // Draw loupe background
                                    drawCircle(
                                        color = ElectricSlateTokens.SurfacePanel,
                                        radius = loupeRadius,
                                        center = loupeCenter
                                    )

                                    // Draw 2.5x magnified image inside loupe
                                    clipPath(loupePath) {
                                        val mag = 2.5f
                                        val sampleCenterBmpX = activeNormPos.x * bmp.width
                                        val sampleCenterBmpY = activeNormPos.y * bmp.height
                                        val sampleRadiusBmpX = loupeRadius / (fitScale * mag)
                                        val sampleRadiusBmpY = loupeRadius / (fitScale * mag)

                                        val srcRect = Rect(
                                            left = (sampleCenterBmpX - sampleRadiusBmpX).coerceIn(0f, bmp.width.toFloat()),
                                            top = (sampleCenterBmpY - sampleRadiusBmpY).coerceIn(0f, bmp.height.toFloat()),
                                            right = (sampleCenterBmpX + sampleRadiusBmpX).coerceIn(0f, bmp.width.toFloat()),
                                            bottom = (sampleCenterBmpY + sampleRadiusBmpY).coerceIn(0f, bmp.height.toFloat())
                                        )

                                        drawImage(
                                            image = bmpImageBitmap,
                                            srcOffset = IntOffset(srcRect.left.toInt(), srcRect.top.toInt()),
                                            srcSize = IntSize(srcRect.width.toInt().coerceAtLeast(1), srcRect.height.toInt().coerceAtLeast(1)),
                                            dstOffset = IntOffset((loupeCenter.x - loupeRadius).toInt(), (loupeCenter.y - loupeRadius).toInt()),
                                            dstSize = IntSize((loupeRadius * 2).toInt(), (loupeRadius * 2).toInt())
                                        )
                                    }

                                    // Loupe outer accent ring
                                    drawCircle(
                                        color = ElectricSlateTokens.PrimaryAccent,
                                        radius = loupeRadius,
                                        center = loupeCenter,
                                        style = Stroke(width = 2.5.dp.toPx())
                                    )

                                    // Loupe center crosshair (+)
                                    drawLine(
                                        color = ElectricSlateTokens.PrimaryAccent,
                                        start = Offset(loupeCenter.x - 10.dp.toPx(), loupeCenter.y),
                                        end = Offset(loupeCenter.x + 10.dp.toPx(), loupeCenter.y),
                                        strokeWidth = 1.5.dp.toPx()
                                    )
                                    drawLine(
                                        color = ElectricSlateTokens.PrimaryAccent,
                                        start = Offset(loupeCenter.x, loupeCenter.y - 10.dp.toPx()),
                                        end = Offset(loupeCenter.x, loupeCenter.y + 10.dp.toPx()),
                                        strokeWidth = 1.5.dp.toPx()
                                    )
                                }
                            }
                        }
                    }

                    // Processing overlay spinner
                    if (isProcessingWarp) {
                        Surface(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(ElectricSlateTokens.BgCanvas.copy(alpha = 0.65f)),
                            color = Color.Transparent
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(
                                    color = ElectricSlateTokens.PrimaryAccent,
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = statusMessage ?: "Rectifying document...",
                                    color = ElectricSlateTokens.TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Bottom Panel: Academic Filter Selector
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    color = ElectricSlateTokens.SurfacePanel,
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ElectricSlateTokens.BorderSubtle)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "ACADEMIC OPTICAL FILTER",
                            color = ElectricSlateTokens.TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Filter 1: Original
                            FilterModePill(
                                title = "Original",
                                isSelected = selectedFilter == DewarpFilterMode.ORIGINAL,
                                onClick = { selectedFilter = DewarpFilterMode.ORIGINAL },
                                modifier = Modifier.weight(1f)
                            )

                            // Filter 2: Document Clean
                            FilterModePill(
                                title = "Clean",
                                isSelected = selectedFilter == DewarpFilterMode.DOCUMENT_CLEAN,
                                onClick = { selectedFilter = DewarpFilterMode.DOCUMENT_CLEAN },
                                modifier = Modifier.weight(1f)
                            )

                            // Filter 3: High-Contrast B&W
                            FilterModePill(
                                title = "B&W",
                                isSelected = selectedFilter == DewarpFilterMode.HIGH_CONTRAST_BW,
                                onClick = { selectedFilter = DewarpFilterMode.HIGH_CONTRAST_BW },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun FilterModePill(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = if (isSelected) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.TrackInactive,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.BorderSubtle
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                color = if (isSelected) ElectricSlateTokens.TextPrimary else ElectricSlateTokens.TextMuted,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}
