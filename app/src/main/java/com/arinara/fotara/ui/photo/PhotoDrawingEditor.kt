// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.photo

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoFixNormal
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LayersClear
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.arinara.fotara.R
import com.arinara.fotara.canvas.engine.StrokeProcessor
import com.arinara.fotara.canvas.model.StrokeBlendMode
import com.arinara.fotara.canvas.model.StrokeElement
import com.arinara.fotara.canvas.model.StrokePoint
import com.arinara.fotara.canvas.model.StrokeToolType
import com.arinara.fotara.canvas.render.PhotoDrawingRenderer
import com.arinara.fotara.data.model.Photo
import com.arinara.fotara.data.model.PhotoDrawing
import com.arinara.fotara.theme.FolderBodyBlue
import com.arinara.fotara.theme.FolderTabCream
import com.arinara.fotara.theme.MidnightCardOutline
import com.arinara.fotara.theme.MidnightNavy
import com.arinara.fotara.theme.MidnightSurface
import com.arinara.fotara.theme.TagAmber
import com.arinara.fotara.theme.TextMuted
import com.arinara.fotara.theme.TextPrimary
import com.arinara.fotara.theme.TextSecondary
import kotlinx.coroutines.delay
import java.io.File

private val CURATED_PALETTE = listOf(
    0xFFEBD8B8, // FolderTabCream
    0xFFF4D03F, // TagAmber
    0xFFE74C3C, // TagCrimson
    0xFF2ECC71, // SageGreen
    0xFF3498DB, // RoyalBlue
    0xFFFFFFFF, // PureWhite
    0xFF95A5A6, // SlateGray
    0xFF0D1B2A  // MidnightNavy
)

private enum class ActiveTool {
    PEN,
    HIGHLIGHTER,
    ERASER
}

@Composable
fun PhotoDrawingEditor(
    photo: Photo,
    initialDrawing: PhotoDrawing?,
    imageVersion: Long = 0L,
    onSaveDrawing: (PhotoDrawing) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    // Resolution discovery
    val (photoWidth, photoHeight) = remember(photo.fileUri, initialDrawing) {
        if (initialDrawing != null && initialDrawing.widthPx > 0 && initialDrawing.heightPx > 0) {
            initialDrawing.widthPx to initialDrawing.heightPx
        } else {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(photo.fileUri, options)
            val w = if (options.outWidth > 0) options.outWidth else 1080
            val h = if (options.outHeight > 0) options.outHeight else 1920
            w to h
        }
    }

    // Drawing state
    val strokes = remember { mutableStateListOf<StrokeElement>().apply {
        if (initialDrawing != null) addAll(initialDrawing.strokes)
    } }
    val undoStack = remember { ArrayDeque<List<StrokeElement>>() }
    val redoStack = remember { ArrayDeque<List<StrokeElement>>() }

    var activeTool by remember { mutableStateOf(ActiveTool.PEN) }
    var activeColor by remember { mutableLongStateOf(0xFFEBD8B8) }
    var penSizeDp by remember { mutableFloatStateOf(4f) }
    var highlighterSizeDp by remember { mutableFloatStateOf(20f) }
    var eraserSizeDp by remember { mutableFloatStateOf(24f) }
    var highlighterBlendMode by remember { mutableStateOf(StrokeBlendMode.MULTIPLY) }

    var isDrawingVisible by remember { mutableStateOf(initialDrawing?.isVisible ?: true) }
    var showColorPalette by remember { mutableStateOf(false) }
    var showSizeSlider by remember { mutableStateOf(false) }

    // Live gesture state
    var livePoints by remember { mutableStateOf<List<StrokePoint>?>(null) }
    var userScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    // Autosave tracking
    var lastEditTimestamp by remember { mutableLongStateOf(0L) }

    fun getCurrentDrawing(): PhotoDrawing {
        return PhotoDrawing(
            photoId = photo.id,
            strokes = strokes.toList(),
            widthPx = photoWidth,
            heightPx = photoHeight,
            isVisible = isDrawingVisible,
            updatedAt = System.currentTimeMillis()
        )
    }

    fun flushSave() {
        onSaveDrawing(getCurrentDrawing())
    }

    // Debounced autosave (600ms)
    LaunchedEffect(lastEditTimestamp) {
        if (lastEditTimestamp > 0L) {
            delay(600)
            flushSave()
        }
    }

    fun pushUndoSnapshot() {
        undoStack.addLast(strokes.toList())
        if (undoStack.size > 100) {
            undoStack.removeFirst()
        }
        redoStack.clear()
        lastEditTimestamp = System.currentTimeMillis()
    }

    fun handleUndo() {
        if (undoStack.isNotEmpty()) {
            redoStack.addLast(strokes.toList())
            val previous = undoStack.removeLast()
            strokes.clear()
            strokes.addAll(previous)
            lastEditTimestamp = System.currentTimeMillis()
        }
    }

    fun handleRedo() {
        if (redoStack.isNotEmpty()) {
            undoStack.addLast(strokes.toList())
            val next = redoStack.removeLast()
            strokes.clear()
            strokes.addAll(next)
            lastEditTimestamp = System.currentTimeMillis()
        }
    }

    BackHandler {
        flushSave()
        onDismiss()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MidnightNavy)
    ) {
        // Main Drawing Canvas with Pinch-Zoom and Single-Finger Drawing
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
        ) {
            val containerW = constraints.maxWidth.toFloat()
            val containerH = constraints.maxHeight.toFloat()

            val fitScale = if (photoWidth > 0 && photoHeight > 0) {
                minOf(containerW / photoWidth.toFloat(), containerH / photoHeight.toFloat())
            } else 1f

            val renderedW = photoWidth * fitScale
            val renderedH = photoHeight * fitScale
            val fitLeft = (containerW - renderedW) / 2f
            val fitTop = (containerH - renderedH) / 2f

            val densityPx = density.density

            val currentActiveTool by rememberUpdatedState(activeTool)
            val currentActiveColor by rememberUpdatedState(activeColor)
            val currentPenSizeDp by rememberUpdatedState(penSizeDp)
            val currentHighlighterSizeDp by rememberUpdatedState(highlighterSizeDp)
            val currentEraserSizeDp by rememberUpdatedState(eraserSizeDp)
            val currentHighlighterBlendMode by rememberUpdatedState(highlighterBlendMode)
            val currentIsDrawingVisible by rememberUpdatedState(isDrawingVisible)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = userScale
                        scaleY = userScale
                        translationX = panOffset.x
                        translationY = panOffset.y
                    }
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            com.arinara.fotara.debug.FrameIntervalRecorder.startGesture()
                            var dragStrokesSnapshot: List<StrokeElement>? = null
                            val currentPathPoints = mutableListOf<StrokePoint>()
                            var prevPhotoPt: Offset? = null

                            do {
                                val event = awaitPointerEvent()
                                val pressedChanges = event.changes.filter { it.pressed }

                                if (pressedChanges.size >= 2) {
                                    // 2-finger Pan & Zoom
                                    livePoints = null
                                    currentPathPoints.clear()
                                    prevPhotoPt = null

                                    val zoom = event.calculateZoom()
                                    val pan = event.calculatePan()
                                    val centroid = event.calculateCentroid(useCurrent = true)

                                    val newScale = (userScale * zoom).coerceIn(1f, 5f)
                                    if (newScale <= 1.01f) {
                                        userScale = 1f
                                        panOffset = Offset.Zero
                                    } else {
                                        val centerX = size.width / 2f
                                        val centerY = size.height / 2f
                                        val focalShiftX = (1f - zoom) * (centroid.x - centerX - panOffset.x)
                                        val focalShiftY = (1f - zoom) * (centroid.y - centerY - panOffset.y)

                                        val maxPanX = (size.width * (newScale - 1f)) / 2f
                                        val maxPanY = (size.height * (newScale - 1f)) / 2f

                                        userScale = newScale
                                        panOffset = Offset(
                                            (panOffset.x + pan.x + focalShiftX).coerceIn(-maxPanX, maxPanX),
                                            (panOffset.y + pan.y + focalShiftY).coerceIn(-maxPanY, maxPanY)
                                        )
                                    }
                                    event.changes.forEach { it.consume() }
                                } else if (pressedChanges.size == 1 && currentIsDrawingVisible) {
                                    // 1-finger Drawing / Erasing
                                    val change = pressedChanges.first()
                                    val touchPos = change.position

                                    val viewCenterX = containerW / 2f
                                    val viewCenterY = containerH / 2f
                                    val contentX = (touchPos.x - viewCenterX - panOffset.x) / userScale + viewCenterX
                                    val contentY = (touchPos.y - viewCenterY - panOffset.y) / userScale + viewCenterY

                                    val photoX = ((contentX - fitLeft) / fitScale).coerceIn(0f, photoWidth.toFloat())
                                    val photoY = ((contentY - fitTop) / fitScale).coerceIn(0f, photoHeight.toFloat())
                                    val pressure = StrokeProcessor.normalizePressure(change.pressure)

                                    if (dragStrokesSnapshot == null) {
                                        dragStrokesSnapshot = strokes.toList()
                                    }

                                    when (currentActiveTool) {
                                        ActiveTool.PEN, ActiveTool.HIGHLIGHTER -> {
                                            val pt = StrokePoint(photoX, photoY, pressure)
                                            currentPathPoints.add(pt)
                                            livePoints = currentPathPoints.toList()
                                        }
                                        ActiveTool.ERASER -> {
                                            val currentPt = Offset(photoX, photoY)
                                            val prev = prevPhotoPt ?: currentPt
                                            val eraserStrokeWidthPx = (currentEraserSizeDp * densityPx) / fitScale
                                            val eraserRadius = eraserStrokeWidthPx / 2f

                                            val updated = mutableListOf<StrokeElement>()
                                            for (s in strokes) {
                                                val split = StrokeProcessor.eraseStrokeWithCapsule(
                                                    stroke = s,
                                                    ax = prev.x,
                                                    ay = prev.y,
                                                    bx = currentPt.x,
                                                    by = currentPt.y,
                                                    eraserRadius = eraserRadius,
                                                    minRemainder = 3f
                                                )
                                                updated.addAll(split)
                                            }
                                            strokes.clear()
                                            strokes.addAll(updated)
                                            prevPhotoPt = currentPt
                                        }
                                    }
                                    change.consume()
                                }
                            } while (event.changes.any { it.pressed })

                            // Gesture Finished (Finger UP)
                            com.arinara.fotara.debug.FrameIntervalRecorder.stopGesture()
                            if (currentActiveTool == ActiveTool.ERASER) {
                                if (dragStrokesSnapshot != null && dragStrokesSnapshot != strokes.toList()) {
                                    undoStack.addLast(dragStrokesSnapshot)
                                    if (undoStack.size > 100) undoStack.removeFirst()
                                    redoStack.clear()
                                    lastEditTimestamp = System.currentTimeMillis()
                                }
                            } else if (currentPathPoints.isNotEmpty()) {
                                val toolType = if (currentActiveTool == ActiveTool.PEN) StrokeToolType.PEN else StrokeToolType.HIGHLIGHTER
                                val sizeDp = if (currentActiveTool == ActiveTool.PEN) currentPenSizeDp else currentHighlighterSizeDp
                                val strokeWidthPhotoPx = (sizeDp * densityPx) / fitScale
                                val blendMode = if (currentActiveTool == ActiveTool.PEN) StrokeBlendMode.NORMAL else currentHighlighterBlendMode

                                val decimated = StrokeProcessor.decimatePoints(currentPathPoints, tolerance = 1.0f)
                                val bounds = StrokeProcessor.computeBounds(decimated, strokeWidthPhotoPx)
                                val newStroke = StrokeElement(
                                    layerId = "photo_layer",
                                    points = decimated,
                                    color = currentActiveColor,
                                    width = strokeWidthPhotoPx,
                                    toolType = toolType,
                                    blendMode = blendMode,
                                    bounds = bounds
                                )

                                if (dragStrokesSnapshot != null) {
                                    undoStack.addLast(dragStrokesSnapshot)
                                    if (undoStack.size > 100) undoStack.removeFirst()
                                    redoStack.clear()
                                }
                                strokes.add(newStroke)
                                livePoints = null
                                lastEditTimestamp = System.currentTimeMillis()
                            }
                            livePoints = null
                        }
                    }
            ) {
                // Background Photo
                val file = remember(photo.fileUri) { File(photo.fileUri) }
                if (file.exists()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(file)
                            .setParameter("v", imageVersion)
                            .crossfade(false)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Drawing Strokes Canvas
                if (isDrawingVisible) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawIntoCanvas { canvas ->
                            val native = canvas.nativeCanvas
                            // 1. Committed strokes
                            PhotoDrawingRenderer.renderStrokes(
                                canvas = native,
                                strokes = strokes,
                                scale = fitScale,
                                offsetX = fitLeft,
                                offsetY = fitTop
                            )

                            // 2. Live in-progress stroke
                            val live = livePoints
                            if (live != null && live.isNotEmpty()) {
                                native.save()
                                native.translate(fitLeft, fitTop)
                                native.scale(fitScale, fitScale)

                                val toolType = if (activeTool == ActiveTool.PEN) StrokeToolType.PEN else StrokeToolType.HIGHLIGHTER
                                val sizeDp = if (activeTool == ActiveTool.PEN) penSizeDp else highlighterSizeDp
                                val strokeWidthPhotoPx = (sizeDp * densityPx) / fitScale
                                val blendMode = if (activeTool == ActiveTool.PEN) StrokeBlendMode.NORMAL else highlighterBlendMode

                                PhotoDrawingRenderer.drawLiveStroke(
                                    canvas = native,
                                    points = live,
                                    toolType = toolType,
                                    color = activeColor,
                                    width = strokeWidthPhotoPx,
                                    blendMode = blendMode
                                )
                                native.restore()
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // Top Bar: Done, Title, Undo, Redo, Eye
        // ==========================================
        Surface(
            color = MidnightNavy.copy(alpha = 0.94f),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    flushSave()
                    onDismiss()
                }) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = stringResource(R.string.cd_draw_done),
                        tint = FolderTabCream
                    )
                }

                Text(
                    text = stringResource(R.string.action_draw),
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp)
                )

                // Undo
                IconButton(
                    onClick = { handleUndo() },
                    enabled = undoStack.isNotEmpty()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = stringResource(R.string.cd_draw_undo),
                        tint = if (undoStack.isNotEmpty()) FolderTabCream else TextMuted.copy(alpha = 0.35f)
                    )
                }

                // Redo
                IconButton(
                    onClick = { handleRedo() },
                    enabled = redoStack.isNotEmpty()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Redo,
                        contentDescription = stringResource(R.string.cd_draw_redo),
                        tint = if (redoStack.isNotEmpty()) FolderTabCream else TextMuted.copy(alpha = 0.35f)
                    )
                }

                // Visibility Toggle
                IconButton(onClick = {
                    isDrawingVisible = !isDrawingVisible
                    lastEditTimestamp = System.currentTimeMillis()
                }) {
                    Icon(
                        imageVector = if (isDrawingVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = stringResource(R.string.cd_draw_visibility),
                        tint = if (isDrawingVisible) FolderTabCream else TagAmber
                    )
                }
            }
        }

        // ==========================================
        // Floating Bottom Dock & Sub-Panels
        // ==========================================
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Palette Popup
            AnimatedVisibility(
                visible = showColorPalette,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 }
            ) {
                Surface(
                    color = MidnightSurface.copy(alpha = 0.96f),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MidnightCardOutline),
                    modifier = Modifier.padding(bottom = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (colorLong in CURATED_PALETTE) {
                            val isSelected = activeColor == colorLong
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(colorLong.toInt()))
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) Color.White else MidnightCardOutline,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        activeColor = colorLong
                                        showColorPalette = false
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = if (colorLong == 0xFFFFFFFF) Color.Black else Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Size & Blend Mode Options Panel
            AnimatedVisibility(
                visible = showSizeSlider,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 }
            ) {
                Surface(
                    color = MidnightSurface.copy(alpha = 0.96f),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MidnightCardOutline),
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .padding(bottom = 10.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        val activeSize = when (activeTool) {
                            ActiveTool.PEN -> penSizeDp
                            ActiveTool.HIGHLIGHTER -> highlighterSizeDp
                            ActiveTool.ERASER -> eraserSizeDp
                        }
                        val range = when (activeTool) {
                            ActiveTool.PEN -> 2f..32f
                            ActiveTool.HIGHLIGHTER -> 10f..60f
                            ActiveTool.ERASER -> 10f..60f
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Size", color = TextSecondary, fontSize = 12.sp)
                            Text("${activeSize.toInt()} dp", color = FolderTabCream, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Slider(
                            value = activeSize,
                            onValueChange = { newSize ->
                                when (activeTool) {
                                    ActiveTool.PEN -> penSizeDp = newSize
                                    ActiveTool.HIGHLIGHTER -> highlighterSizeDp = newSize
                                    ActiveTool.ERASER -> eraserSizeDp = newSize
                                }
                            },
                            valueRange = range,
                            colors = SliderDefaults.colors(
                                thumbColor = FolderTabCream,
                                activeTrackColor = FolderTabCream,
                                inactiveTrackColor = MidnightNavy
                            )
                        )

                        if (activeTool == ActiveTool.HIGHLIGHTER) {
                            HorizontalDivider(color = MidnightCardOutline, modifier = Modifier.padding(vertical = 8.dp))
                            Text("Blend Mode", color = TextSecondary, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                for (mode in listOf(StrokeBlendMode.MULTIPLY, StrokeBlendMode.DARKEN, StrokeBlendMode.SCREEN, StrokeBlendMode.NORMAL)) {
                                    val isModeSelected = highlighterBlendMode == mode
                                    FilterChip(
                                        selected = isModeSelected,
                                        onClick = { highlighterBlendMode = mode },
                                        label = { Text(mode.name, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = FolderBodyBlue,
                                            selectedLabelColor = Color.White,
                                            containerColor = MidnightNavy,
                                            labelColor = TextMuted
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = isModeSelected,
                                            borderColor = MidnightCardOutline,
                                            selectedBorderColor = FolderTabCream
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Main Dock Capsule
            Surface(
                color = MidnightSurface.copy(alpha = 0.94f),
                shape = RoundedCornerShape(26.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MidnightCardOutline),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pen Tool
                    IconButton(
                        onClick = {
                            activeTool = ActiveTool.PEN
                            showSizeSlider = false
                            showColorPalette = false
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.tool_pen),
                            tint = if (activeTool == ActiveTool.PEN) FolderTabCream else TextMuted
                        )
                    }

                    // Highlighter Tool
                    IconButton(
                        onClick = {
                            activeTool = ActiveTool.HIGHLIGHTER
                            showSizeSlider = false
                            showColorPalette = false
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoFixNormal,
                            contentDescription = stringResource(R.string.tool_highlighter),
                            tint = if (activeTool == ActiveTool.HIGHLIGHTER) TagAmber else TextMuted
                        )
                    }

                    // Eraser Tool
                    IconButton(
                        onClick = {
                            activeTool = ActiveTool.ERASER
                            showSizeSlider = false
                            showColorPalette = false
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.LayersClear,
                            contentDescription = stringResource(R.string.tool_eraser),
                            tint = if (activeTool == ActiveTool.ERASER) FolderTabCream else TextMuted
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(24.dp)
                            .background(MidnightCardOutline)
                    )
                    Spacer(modifier = Modifier.width(4.dp))

                    // Color Swatch Button (only active for Pen & Highlighter)
                    if (activeTool != ActiveTool.ERASER) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(activeColor.toInt()))
                                .border(1.5.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                                .clickable {
                                    showColorPalette = !showColorPalette
                                    showSizeSlider = false
                                }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    // Size / Options Button
                    IconButton(
                        onClick = {
                            showSizeSlider = !showSizeSlider
                            showColorPalette = false
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .border(1.dp, if (showSizeSlider) FolderTabCream else TextMuted, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            val dotSize = when (activeTool) {
                                ActiveTool.PEN -> (penSizeDp / 32f * 14f).coerceIn(3f, 14f).dp
                                ActiveTool.HIGHLIGHTER -> (highlighterSizeDp / 60f * 14f).coerceIn(4f, 14f).dp
                                ActiveTool.ERASER -> (eraserSizeDp / 60f * 14f).coerceIn(4f, 14f).dp
                            }
                            Box(
                                modifier = Modifier
                                    .size(dotSize)
                                    .clip(CircleShape)
                                    .background(if (showSizeSlider) FolderTabCream else TextMuted)
                            )
                        }
                    }
                }
            }
        }
    }
}
