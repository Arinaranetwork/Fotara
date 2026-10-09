// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.document

import android.graphics.Bitmap
import android.widget.Toast
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.AutoFixNormal
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LayersClear
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.R
import com.arinara.fotara.canvas.engine.DrawingConstants
import com.arinara.fotara.canvas.engine.DrawingTool
import com.arinara.fotara.canvas.engine.RecognizedShape
import com.arinara.fotara.canvas.engine.ShapeAutoCorrectEngine
import com.arinara.fotara.canvas.engine.StrokeProcessor
import com.arinara.fotara.canvas.model.StrokeBlendMode
import com.arinara.fotara.canvas.model.StrokeElement
import com.arinara.fotara.canvas.model.StrokePoint
import com.arinara.fotara.canvas.model.StrokeToolType
import com.arinara.fotara.canvas.model.TextBackgroundStyle
import com.arinara.fotara.canvas.model.TextLayerElement
import com.arinara.fotara.canvas.render.PhotoDrawingRenderer
import com.arinara.fotara.data.model.DocumentNote
import com.arinara.fotara.data.model.PdfPageDrawing
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import com.arinara.fotara.data.repository.DocumentRepository
import com.arinara.fotara.data.repository.PdfPageDrawingRepository
import com.arinara.fotara.theme.TagCrimson
import com.arinara.fotara.theme.FolderTabCream
import com.arinara.fotara.theme.MidnightCardOutline
import com.arinara.fotara.theme.MidnightNavy
import com.arinara.fotara.theme.HomeNearBlack
import com.arinara.fotara.theme.MidnightSurface
import com.arinara.fotara.theme.TagAmber
import com.arinara.fotara.theme.TextMuted
import com.arinara.fotara.theme.TextPrimary
import com.arinara.fotara.theme.TextSecondary
import com.arinara.fotara.util.PdfLayoutMath
import com.arinara.fotara.util.PdfPageRenderer
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private val DangerRed = TagCrimson

@Composable
fun PdfPageEditorScreen(
    documentId: Long,
    pageIndex: Int,
    documentRepository: DocumentRepository?,
    pdfPageDrawingRepository: PdfPageDrawingRepository?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val factory = remember(documentId, pageIndex, documentRepository, pdfPageDrawingRepository) {
        PdfPageEditorViewModelFactory(
            documentId = documentId,
            pageIndex = pageIndex,
            documentRepository = documentRepository,
            pdfPageDrawingRepository = pdfPageDrawingRepository
        )
    }
    val viewModel: PdfPageEditorViewModel = viewModel(
        key = "pdf_page_editor_${documentId}_$pageIndex",
        factory = factory
    )
    PdfPageEditorScreen(
        viewModel = viewModel,
        onBack = onBack,
        modifier = modifier
    )
}

@Composable
fun PdfPageEditorScreen(
    viewModel: PdfPageEditorViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val documentId = viewModel.documentId
    val pageIndex = viewModel.pageIndex
    val totalPages = uiState.totalPages
    val pagePointsWidth = uiState.pagePointsWidth
    val pagePointsHeight = uiState.pagePointsHeight
    val isDrawingVisible = uiState.isDrawingVisible

    // Drawing state
    val strokes = remember { mutableStateListOf<StrokeElement>() }
    LaunchedEffect(uiState.strokes) {
        strokes.clear()
        strokes.addAll(uiState.strokes)
    }

    var activeTool by remember { mutableStateOf(DrawingTool.PEN) }
    var activeColor by remember { mutableLongStateOf(DrawingConstants.CURATED_PALETTE.first()) }
    var penSizeDp by remember { mutableFloatStateOf(DrawingConstants.DEFAULT_PEN_SIZE_DP) }
    var highlighterSizeDp by remember { mutableFloatStateOf(DrawingConstants.DEFAULT_HIGHLIGHTER_SIZE_DP) }
    var eraserSizeDp by remember { mutableFloatStateOf(DrawingConstants.DEFAULT_ERASER_SIZE_DP) }
    var highlighterBlendMode by remember { mutableStateOf(StrokeBlendMode.MULTIPLY) }

    var showColorPalette by remember { mutableStateOf(false) }
    var showSizeSlider by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    val textLayers = uiState.textLayers
    var showTextOptions by remember { mutableStateOf(false) }
    var textInput by remember { mutableStateOf("Text Annotation") }
    var textFontSizeSp by remember { mutableFloatStateOf(18f) }
    var textFontWeight by remember { mutableIntStateOf(400) }
    var textColor by remember { mutableLongStateOf(DrawingConstants.CURATED_PALETTE.first()) }
    var textBackgroundStyle by remember { mutableStateOf(TextBackgroundStyle.TRANSPARENT) }
    var pendingTextPlacementPt by remember { mutableStateOf<Offset?>(null) }

    // Live gesture state
    var livePoints by remember { mutableStateOf<List<StrokePoint>?>(null) }
    var userScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    // Rendered page bitmaps
    var baseBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var highResBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(uiState.sizeMismatchWarning) {
        if (uiState.sizeMismatchWarning) {
            Toast.makeText(context, context.getString(R.string.pdf_drawing_size_mismatch), Toast.LENGTH_SHORT).show()
            viewModel.clearSizeMismatchWarning()
        }
    }

    BackHandler {
        viewModel.flushSave()
        onBack()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MidnightNavy)
    ) {
        // Main Drawing Canvas with 1x-6x Pinch-Zoom and Single-Finger Drawing
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
        ) {
            val containerW = constraints.maxWidth.toFloat()
            val containerH = constraints.maxHeight.toFloat()

            val aspectRatio = if (pagePointsHeight > 0f) pagePointsWidth / pagePointsHeight else 0.707f
            val (renderedW, renderedH) = remember(containerW, containerH, aspectRatio) {
                val fitW = if (containerW > 0f && containerH > 0f && containerW / aspectRatio <= containerH) {
                    containerW
                } else if (containerH > 0f) {
                    containerH * aspectRatio
                } else containerW
                val fitH = if (aspectRatio > 0f) fitW / aspectRatio else containerH
                fitW to fitH
            }
            val targetWidthPx = renderedW.toInt().coerceAtLeast(100)
            val targetHeightPx = renderedH.toInt().coerceAtLeast(100)

            val fitLeft = (containerW - renderedW) / 2f
            val fitTop = (containerH - renderedH) / 2f

            // Pixels per PDF page point at fit scale
            val fitScale = if (pagePointsWidth > 0f) renderedW / pagePointsWidth else 1f
            val densityPx = density.density

            val currentActiveTool by rememberUpdatedState(activeTool)
            val currentActiveColor by rememberUpdatedState(activeColor)
            val currentPenSizeDp by rememberUpdatedState(penSizeDp)
            val currentHighlighterSizeDp by rememberUpdatedState(highlighterSizeDp)
            val currentEraserSizeDp by rememberUpdatedState(eraserSizeDp)
            val currentHighlighterBlendMode by rememberUpdatedState(highlighterBlendMode)
            val currentIsDrawingVisible by rememberUpdatedState(isDrawingVisible)

            // 1. Initial base fit render
            LaunchedEffect(uiState.pageIndex, uiState.isLoading, targetWidthPx, targetHeightPx) {
                if (!uiState.isLoading && targetWidthPx > 0 && targetHeightPx > 0) {
                    val b = viewModel.renderPage(
                        pageIndex = uiState.pageIndex,
                        destWidth = targetWidthPx,
                        destHeight = targetHeightPx,
                        renderScale = 1.0f
                    )
                    if (b != null) baseBitmap = b
                }
            }

            // 2. Sharper re-render when zoom settles (120ms debounce)
            LaunchedEffect(userScale, uiState.pageIndex, targetWidthPx, targetHeightPx) {
                if (userScale > 1.25f && targetWidthPx > 0 && targetHeightPx > 0) {
                    delay(120)
                    val scaleToUse = userScale.coerceAtMost(3.0f)
                    val hiRes = viewModel.renderPage(
                        pageIndex = uiState.pageIndex,
                        destWidth = (targetWidthPx * scaleToUse).toInt(),
                        destHeight = (targetHeightPx * scaleToUse).toInt(),
                        renderScale = scaleToUse
                    )
                    if (hiRes != null) highResBitmap = hiRes
                } else {
                    highResBitmap = null
                }
            }

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
                            val firstDown = awaitFirstDown(requireUnconsumed = false)
                            var dragStrokesSnapshot: List<StrokeElement>? = null
                            val currentPathPoints = mutableListOf<StrokePoint>()
                            var prevPtPts: Offset? = null
                            var isShapeSnapped = false
                            var shapeHoldAnchorPt = firstDown.position
                            var shapeHoldStartTime = System.currentTimeMillis()

                            do {
                                val event = awaitPointerEvent()
                                val pressedChanges = event.changes.filter { it.pressed }

                                if (pressedChanges.size >= 2) {
                                    // 2-finger Pan & Zoom (1x to 6x smooth GPU transform)
                                    livePoints = null
                                    currentPathPoints.clear()
                                    prevPtPts = null
                                    isShapeSnapped = false

                                    val zoom = event.calculateZoom()
                                    val pan = event.calculatePan()
                                    val centroid = event.calculateCentroid(useCurrent = true)

                                    val newScale = (userScale * zoom).coerceIn(1f, 6f)
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
                                    // 1-finger Drawing / Erasing in PDF page points coordinate space
                                    val change = pressedChanges.first()
                                    val transformParams = com.arinara.fotara.canvas.engine.DrawingViewTransform.createParams(
                                        viewportWidth = containerW,
                                        viewportHeight = containerH,
                                        contentWidth = pagePointsWidth,
                                        contentHeight = pagePointsHeight,
                                        scale = userScale,
                                        panOffset = panOffset
                                    )
                                    val pagePt = transformParams.localToContent(change.position)
                                    val ptX = pagePt.x
                                    val ptY = pagePt.y
                                    val pressure = StrokeProcessor.normalizePressure(change.pressure)

                                    if (currentActiveTool == DrawingTool.TEXT) {
                                        pendingTextPlacementPt = pagePt
                                        showTextOptions = true
                                        change.consume()
                                        break
                                    }

                                    if (dragStrokesSnapshot == null) {
                                        dragStrokesSnapshot = strokes.toList()
                                    }

                                    when (currentActiveTool) {
                                        DrawingTool.PEN, DrawingTool.HIGHLIGHTER -> {
                                            val pt = StrokePoint(ptX, ptY, pressure)
                                            currentPathPoints.add(pt)
                                            livePoints = currentPathPoints.toList()

                                            if (currentActiveTool == DrawingTool.PEN && !isShapeSnapped) {
                                                val slopPx = 18f * densityPx
                                                val moveDist = (change.position - shapeHoldAnchorPt).getDistance()
                                                val now = System.currentTimeMillis()
                                                if (moveDist > slopPx) {
                                                    shapeHoldAnchorPt = change.position
                                                    shapeHoldStartTime = now
                                                } else if (now - shapeHoldStartTime >= ShapeAutoCorrectEngine.HOLD_THRESHOLD_MS && currentPathPoints.size >= 5) {
                                                    val snapRes = ShapeAutoCorrectEngine.recognizeAndSnap(currentPathPoints)
                                                    if (snapRes.shape !is RecognizedShape.None) {
                                                        isShapeSnapped = true
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        currentPathPoints.clear()
                                                        currentPathPoints.addAll(snapRes.snappedPoints)
                                                        livePoints = currentPathPoints.toList()
                                                    }
                                                }
                                            }
                                        }
                                        DrawingTool.ERASER -> {
                                            val currentPt = Offset(ptX, ptY)
                                            val prev = prevPtPts ?: currentPt
                                            val eraserStrokeWidthPts = DrawingConstants.computeStoredWidth(
                                                sliderDp = currentEraserSizeDp,
                                                density = densityPx,
                                                fitScale = fitScale
                                            )
                                            val eraserRadiusPts = eraserStrokeWidthPts / 2f

                                            val updated = DrawingConstants.eraseStrokes(
                                                strokes = strokes,
                                                prevPt = prev,
                                                currentPt = currentPt,
                                                eraserRadius = eraserRadiusPts
                                            )
                                            strokes.clear()
                                            strokes.addAll(updated)
                                            prevPtPts = currentPt
                                        }
                                        DrawingTool.TEXT -> {}
                                    }
                                    change.consume()
                                }
                            } while (event.changes.any { it.pressed })

                            // Gesture Finished (Finger Lift)
                            if (currentActiveTool == DrawingTool.ERASER) {
                                if (dragStrokesSnapshot != null && dragStrokesSnapshot != strokes.toList()) {
                                    viewModel.updateStrokesAfterEraser(strokes.toList(), dragStrokesSnapshot)
                                }
                            } else if (currentPathPoints.isNotEmpty() && currentActiveTool != DrawingTool.TEXT) {
                                val toolType = if (currentActiveTool == DrawingTool.PEN) StrokeToolType.PEN else StrokeToolType.HIGHLIGHTER
                                val sizeDp = if (currentActiveTool == DrawingTool.PEN) currentPenSizeDp else currentHighlighterSizeDp
                                val strokeWidthPts = DrawingConstants.computeStoredWidth(
                                    sliderDp = sizeDp,
                                    density = densityPx,
                                    fitScale = fitScale
                                )
                                val blendMode = if (currentActiveTool == DrawingTool.PEN) StrokeBlendMode.NORMAL else currentHighlighterBlendMode

                                val decimated = StrokeProcessor.decimatePoints(currentPathPoints, strokeWidthPts)
                                val pointsToUse = if (currentActiveTool == DrawingTool.PEN && !isShapeSnapped) {
                                    ShapeAutoCorrectEngine.smoothPointsBezier(decimated, pressure = 1.0f, isClosed = false)
                                } else {
                                    decimated
                                }
                                val bounds = StrokeProcessor.computeBounds(pointsToUse, strokeWidthPts)
                                val newStroke = StrokeElement(
                                    layerId = "pdf_page_${documentId}_$pageIndex",
                                    points = pointsToUse,
                                    color = currentActiveColor,
                                    width = strokeWidthPts,
                                    toolType = toolType,
                                    blendMode = blendMode,
                                    bounds = bounds
                                )

                                strokes.add(newStroke)
                                viewModel.addStroke(newStroke, dragStrokesSnapshot)
                                livePoints = null
                            }
                            livePoints = null
                        }
                    }
            ) {
                // PDF page bitmap and vector strokes rendered onto the same canvas
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val activeBmp = highResBitmap ?: baseBitmap
                    drawIntoCanvas { canvas ->
                        val native = canvas.nativeCanvas

                        // 1. Draw page bitmap
                        if (activeBmp != null && !activeBmp.isRecycled) {
                            val srcRect = android.graphics.Rect(0, 0, activeBmp.width, activeBmp.height)
                            val dstRect = android.graphics.RectF(fitLeft, fitTop, fitLeft + renderedW, fitTop + renderedH)
                            native.drawBitmap(activeBmp, srcRect, dstRect, null)
                        } else {
                            // Blank page placeholder
                            val paint = android.graphics.Paint().apply { color = android.graphics.Color.WHITE }
                            native.drawRect(fitLeft, fitTop, fitLeft + renderedW, fitTop + renderedH, paint)
                        }

                        // 2. Draw committed strokes
                        if (isDrawingVisible && strokes.isNotEmpty()) {
                            PhotoDrawingRenderer.renderStrokes(
                                canvas = native,
                                strokes = strokes,
                                scale = fitScale,
                                offsetX = fitLeft,
                                offsetY = fitTop
                            )
                        }

                        // 2b. Draw text layers
                        if (isDrawingVisible && textLayers.isNotEmpty()) {
                            PhotoDrawingRenderer.renderTextLayers(
                                canvas = native,
                                textLayers = textLayers,
                                scale = fitScale,
                                offsetX = fitLeft,
                                offsetY = fitTop
                            )
                        }

                        // 3. Draw live in-progress stroke
                        val live = livePoints
                        if (isDrawingVisible && live != null && live.isNotEmpty()) {
                            native.save()
                            native.translate(fitLeft, fitTop)
                            native.scale(fitScale, fitScale)

                            val toolType = if (activeTool == DrawingTool.PEN) StrokeToolType.PEN else StrokeToolType.HIGHLIGHTER
                            val sizeDp = if (activeTool == DrawingTool.PEN) penSizeDp else highlighterSizeDp
                            val strokeWidthPts = DrawingConstants.computeStoredWidth(
                                sliderDp = sizeDp,
                                density = densityPx,
                                fitScale = fitScale
                            )
                            val blendMode = if (activeTool == DrawingTool.PEN) StrokeBlendMode.NORMAL else highlighterBlendMode

                            PhotoDrawingRenderer.drawLiveStroke(
                                canvas = native,
                                points = live,
                                toolType = toolType,
                                color = activeColor,
                                width = strokeWidthPts,
                                blendMode = blendMode
                            )
                            native.restore()
                        }
                    }
                }
            }
        }

        // ==========================================
        // Top Bar: Back, Title, Undo, Redo, Eye, Menu
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
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    viewModel.flushSave()
                    onBack()
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back),
                        tint = FolderTabCream
                    )
                }

                Text(
                    text = stringResource(R.string.pdf_editor_title, pageIndex + 1, totalPages),
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                )

                // Undo
                IconButton(
                    onClick = { viewModel.undo() },
                    enabled = uiState.canUndo
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Undo,
                        contentDescription = stringResource(R.string.cd_draw_undo),
                        tint = if (uiState.canUndo) FolderTabCream else TextMuted.copy(alpha = 0.35f)
                    )
                }

                // Redo
                IconButton(
                    onClick = { viewModel.redo() },
                    enabled = uiState.canRedo
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Redo,
                        contentDescription = stringResource(R.string.cd_draw_redo),
                        tint = if (uiState.canRedo) FolderTabCream else TextMuted.copy(alpha = 0.35f)
                    )
                }

                // Eye button: show / hide drawing
                IconButton(onClick = {
                    viewModel.setVisible(!uiState.isDrawingVisible)
                }) {
                    Icon(
                        imageVector = if (uiState.isDrawingVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                        contentDescription = stringResource(
                            if (uiState.isDrawingVisible) R.string.pdf_page_menu_hide_drawing else R.string.pdf_page_menu_show_drawing
                        ),
                        tint = if (uiState.isDrawingVisible) FolderTabCream else TagAmber
                    )
                }

                // Overflow menu
                Box {
                    IconButton(onClick = { showOverflowMenu = true }) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = stringResource(R.string.cd_more_options),
                            tint = FolderTabCream
                        )
                    }

                    DropdownMenu(
                        expanded = showOverflowMenu,
                        onDismissRequest = { showOverflowMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.pdf_page_menu_clear_drawing), color = DangerRed) },
                            leadingIcon = {
                                Icon(Icons.Outlined.LayersClear, contentDescription = null, tint = DangerRed)
                            },
                            onClick = {
                                showOverflowMenu = false
                                showClearConfirmDialog = true
                            }
                        )
                    }
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
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (colorLong in DrawingConstants.CURATED_PALETTE) {
                            val isSelected = activeColor == colorLong
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(colorLong.toInt()))
                                    .border(
                                        width = 2.dp,
                                        color = if (isSelected) TextPrimary else MidnightCardOutline,
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
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = null,
                                        tint = if (colorLong == 0xFFFFFFFF) HomeNearBlack else TextPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Size & Blend Options Popup
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
                        .padding(bottom = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        val currentVal = when (activeTool) {
                            DrawingTool.PEN -> penSizeDp
                            DrawingTool.HIGHLIGHTER -> highlighterSizeDp
                            DrawingTool.ERASER -> eraserSizeDp
                            DrawingTool.TEXT -> textFontSizeSp
                        }
                        val valueRange = when (activeTool) {
                            DrawingTool.PEN -> DrawingConstants.MIN_PEN_SIZE_DP..DrawingConstants.MAX_PEN_SIZE_DP
                            DrawingTool.HIGHLIGHTER -> DrawingConstants.MIN_HIGHLIGHTER_SIZE_DP..DrawingConstants.MAX_HIGHLIGHTER_SIZE_DP
                            DrawingTool.ERASER -> DrawingConstants.MIN_ERASER_SIZE_DP..DrawingConstants.MAX_ERASER_SIZE_DP
                            DrawingTool.TEXT -> 12f..48f
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when (activeTool) {
                                    DrawingTool.PEN -> stringResource(R.string.tool_pen)
                                    DrawingTool.HIGHLIGHTER -> stringResource(R.string.tool_highlighter)
                                    DrawingTool.ERASER -> stringResource(R.string.tool_eraser)
                                    DrawingTool.TEXT -> "Text Size"
                                },
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (activeTool == DrawingTool.TEXT) "${currentVal.toInt()} sp" else "${currentVal.toInt()} dp",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }

                        Slider(
                            value = currentVal,
                            onValueChange = { newSize ->
                                when (activeTool) {
                                    DrawingTool.PEN -> penSizeDp = newSize
                                    DrawingTool.HIGHLIGHTER -> highlighterSizeDp = newSize
                                    DrawingTool.ERASER -> eraserSizeDp = newSize
                                    DrawingTool.TEXT -> textFontSizeSp = newSize
                                }
                            },
                            valueRange = valueRange,
                            colors = SliderDefaults.colors(
                                thumbColor = FolderTabCream,
                                activeTrackColor = FolderTabCream,
                                inactiveTrackColor = MidnightCardOutline
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Highlighter Blending Mode Chips
                        if (activeTool == DrawingTool.HIGHLIGHTER) {
                            HorizontalDivider(
                                color = MidnightCardOutline,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                            Text(
                                text = "Blend Mode",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val modes = listOf(
                                    StrokeBlendMode.NORMAL to "Normal",
                                    StrokeBlendMode.MULTIPLY to "Multiply",
                                    StrokeBlendMode.DARKEN to "Darken",
                                    StrokeBlendMode.SCREEN to "Screen"
                                )
                                for ((mode, label) in modes) {
                                    val isSelected = highlighterBlendMode == mode
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { highlighterBlendMode = mode },
                                        label = { Text(label, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = FolderTabCream,
                                            selectedLabelColor = HomeNearBlack,
                                            containerColor = MidnightNavy,
                                            labelColor = TextSecondary
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = isSelected,
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

            // Floating Tool Dock
            Surface(
                color = MidnightSurface.copy(alpha = 0.94f),
                shape = RoundedCornerShape(28.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MidnightCardOutline),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pen Tool
                    IconButton(
                        onClick = {
                            if (activeTool == DrawingTool.PEN) {
                                showSizeSlider = !showSizeSlider
                                showColorPalette = false
                            } else {
                                activeTool = DrawingTool.PEN
                                showSizeSlider = false
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = stringResource(R.string.tool_pen),
                            tint = if (activeTool == DrawingTool.PEN) FolderTabCream else TextMuted
                        )
                    }

                    // Highlighter Tool
                    IconButton(
                        onClick = {
                            if (activeTool == DrawingTool.HIGHLIGHTER) {
                                showSizeSlider = !showSizeSlider
                                showColorPalette = false
                            } else {
                                activeTool = DrawingTool.HIGHLIGHTER
                                showSizeSlider = false
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoFixNormal,
                            contentDescription = stringResource(R.string.tool_highlighter),
                            tint = if (activeTool == DrawingTool.HIGHLIGHTER) FolderTabCream else TextMuted
                        )
                    }

                    // Eraser Tool
                    IconButton(
                        onClick = {
                            if (activeTool == DrawingTool.ERASER) {
                                showSizeSlider = !showSizeSlider
                                showColorPalette = false
                            } else {
                                activeTool = DrawingTool.ERASER
                                showSizeSlider = false
                                showColorPalette = false
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.LayersClear,
                            contentDescription = stringResource(R.string.tool_eraser),
                            tint = if (activeTool == DrawingTool.ERASER) FolderTabCream else TextMuted
                        )
                    }

                    // Text Layers Tool [ T ]
                    IconButton(
                        onClick = {
                            if (activeTool == DrawingTool.TEXT) {
                                showTextOptions = !showTextOptions
                                showSizeSlider = false
                                showColorPalette = false
                            } else {
                                activeTool = DrawingTool.TEXT
                                showTextOptions = true
                                showSizeSlider = false
                                showColorPalette = false
                            }
                        }
                    ) {
                        Surface(
                            color = if (activeTool == DrawingTool.TEXT) FolderTabCream else Color.Transparent,
                            shape = CircleShape,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "T",
                                    color = if (activeTool == DrawingTool.TEXT) MidnightNavy else FolderTabCream,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Color Swatch Button (hidden when Eraser is selected)
                    if (activeTool != DrawingTool.ERASER) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clickable {
                                    showColorPalette = !showColorPalette
                                    showSizeSlider = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(activeColor.toInt()))
                                    .border(2.dp, TextPrimary.copy(alpha = 0.8f), CircleShape)
                            )
                        }
                    }
                }
            }
        }

        // Text Options / Placement Dialog
        if (showTextOptions) {
            AlertDialog(
                onDismissRequest = { showTextOptions = false },
                title = {
                    Text(
                        text = "Text Annotation",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            label = { Text("Annotation Text") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = FolderTabCream,
                                unfocusedBorderColor = MidnightCardOutline,
                                cursorColor = FolderTabCream
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Font Size", color = TextSecondary, fontSize = 14.sp)
                            Text("${textFontSizeSp.toInt()} sp", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = textFontSizeSp,
                            onValueChange = { textFontSizeSp = it },
                            valueRange = 12f..48f,
                            colors = SliderDefaults.colors(
                                thumbColor = FolderTabCream,
                                activeTrackColor = FolderTabCream,
                                inactiveTrackColor = MidnightNavy
                            )
                        )

                        Text("Font Weight", color = TextSecondary, fontSize = 14.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val weights = listOf(400 to "Regular", 500 to "Medium", 700 to "Bold")
                            for ((w, label) in weights) {
                                val isSel = textFontWeight == w
                                FilterChip(
                                    selected = isSel,
                                    onClick = { textFontWeight = w },
                                    label = { Text(label, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = FolderTabCream,
                                        selectedLabelColor = HomeNearBlack,
                                        containerColor = MidnightNavy,
                                        labelColor = TextSecondary
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSel,
                                        borderColor = MidnightCardOutline,
                                        selectedBorderColor = FolderTabCream
                                    )
                                )
                            }
                        }

                        Text("Card Background", color = TextSecondary, fontSize = 14.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val styles = listOf(
                                TextBackgroundStyle.TRANSPARENT to "Transparent",
                                TextBackgroundStyle.FROSTED_DARK to "Frosted Dark",
                                TextBackgroundStyle.SOLID_LIGHT to "Solid Light"
                            )
                            for ((s, label) in styles) {
                                val isSel = textBackgroundStyle == s
                                FilterChip(
                                    selected = isSel,
                                    onClick = { textBackgroundStyle = s },
                                    label = { Text(label, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = FolderTabCream,
                                        selectedLabelColor = HomeNearBlack,
                                        containerColor = MidnightNavy,
                                        labelColor = TextSecondary
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSel,
                                        borderColor = MidnightCardOutline,
                                        selectedBorderColor = FolderTabCream
                                    )
                                )
                            }
                        }

                        Text("Text Color", color = TextSecondary, fontSize = 14.sp)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(DrawingConstants.CURATED_PALETTE) { colLong ->
                                val isSel = textColor == colLong
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(colLong.toInt()))
                                        .border(
                                            width = if (isSel) 2.dp else 1.dp,
                                            color = if (isSel) TextPrimary else MidnightCardOutline,
                                            shape = CircleShape
                                        )
                                        .clickable { textColor = colLong },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSel) {
                                        Icon(
                                            imageVector = Icons.Outlined.Check,
                                            contentDescription = null,
                                            tint = if (colLong == 0xFFFFFFFF) HomeNearBlack else TextPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showTextOptions = false
                            val placement = pendingTextPlacementPt ?: Offset(pagePointsWidth / 2f, pagePointsHeight / 2f)
                            val estW = maxOf(120f, textInput.length * textFontSizeSp * 0.9f + 24f)
                            val estH = maxOf(40f, textFontSizeSp * 2.2f)
                            val newLayer = TextLayerElement(
                                id = "text_${System.currentTimeMillis()}",
                                layerId = "pdf_page_${documentId}_$pageIndex",
                                text = textInput.ifBlank { "Text" },
                                x = (placement.x - estW / 2f).coerceAtLeast(0f),
                                y = (placement.y - estH / 2f).coerceAtLeast(0f),
                                width = estW,
                                height = estH,
                                fontSizeSp = textFontSizeSp,
                                color = textColor,
                                fontWeight = textFontWeight,
                                backgroundStyle = textBackgroundStyle,
                                bounds = com.arinara.fotara.canvas.engine.CanvasRect(
                                    left = placement.x - estW / 2f,
                                    top = placement.y - estH / 2f,
                                    right = placement.x + estW / 2f,
                                    bottom = placement.y + estH / 2f
                                )
                            )
                            viewModel.addTextLayer(newLayer)
                            pendingTextPlacementPt = null
                        }
                    ) {
                        Text("Place", color = FolderTabCream, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTextOptions = false }) {
                        Text(stringResource(R.string.action_cancel), color = TextSecondary)
                    }
                },
                containerColor = MidnightSurface,
                shape = RoundedCornerShape(20.dp)
            )
        }

        // Clear Drawing Confirmation Dialog
        if (showClearConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showClearConfirmDialog = false },
                title = {
                    Text(
                        text = stringResource(R.string.pdf_clear_drawing_confirm_title),
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.pdf_clear_drawing_confirm_msg),
                        color = TextSecondary
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showClearConfirmDialog = false
                            strokes.clear()
                            viewModel.clearDrawing()
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.pdf_page_menu_clear_drawing),
                            color = DangerRed,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearConfirmDialog = false }) {
                        Text(stringResource(R.string.action_cancel), color = FolderTabCream)
                    }
                },
                containerColor = MidnightSurface,
                shape = RoundedCornerShape(20.dp)
            )
        }

        // Error message dialog (surface PDF renderer/document errors)
        if (uiState.errorMessage != null) {
            AlertDialog(
                onDismissRequest = { viewModel.clearErrorMessage() },
                title = {
                    Text(
                        text = "PDF Error",
                        fontWeight = FontWeight.Bold,
                        color = DangerRed,
                        fontSize = 16.sp
                    )
                },
                text = {
                    Text(
                        text = uiState.errorMessage ?: "Failed to open PDF document",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearErrorMessage(); onBack() }) {
                        Text("Go Back", color = FolderTabCream, fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = MidnightSurface,
                shape = RoundedCornerShape(16.dp)
            )
        }

        // Loading state
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.CircularProgressIndicator(color = FolderTabCream)
            }
        }
    }
}
