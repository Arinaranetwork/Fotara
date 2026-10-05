// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.document

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import com.arinara.fotara.R
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.arinara.fotara.data.model.DocumentNote
import com.arinara.fotara.data.model.DocumentPage
import com.arinara.fotara.ui.components.NoteDetailScheduleChip
import com.arinara.fotara.ui.components.ScheduleNoteDialog
import com.arinara.fotara.util.NoteScheduleManager
import com.arinara.fotara.util.PdfCorruptException
import com.arinara.fotara.util.PdfLayoutMath
import com.arinara.fotara.util.PdfPageRenderer
import com.arinara.fotara.util.PdfPasswordException
import com.arinara.fotara.util.PdfSplitManager
import com.arinara.fotara.util.ScheduleAlertType
import com.arinara.fotara.util.ScheduleNoteType
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import com.arinara.fotara.theme.TagAmber
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

private val ScreenNavy = Color(0xFF03071E)
private val TabCream = Color(0xFFEAE3D2)
private val AccentGold = Color(0xFFF77F00)
private val CardBg = Color(0xFF141936)
private val DangerRed = Color(0xFFD62828)
private val BorderColor = Color(0xFF28325E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    documentNote: DocumentNote,
    pages: List<DocumentPage>,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onSplitToImages: () -> Unit,
    onDelete: () -> Unit,
    initialPageIndex: Int = 0,
    highlightPageIndex: Int? = null
) {
    var showSplitConfirmDialog by remember { mutableStateOf(false) }
    var isReadingMode by rememberSaveable { mutableStateOf(false) }
    var pdfRenderer by remember { mutableStateOf<PdfPageRenderer?>(null) }
    var openErrorMessage by remember { mutableStateOf<String?>(null) }
    var currentScheduledAt by remember { mutableStateOf(documentNote.scheduledAt) }
    var currentAlertType by remember { mutableStateOf(documentNote.alertType) }
    var currentScheduleTitle by remember { mutableStateOf(documentNote.scheduleTitle) }
    var showScheduleDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val file = remember(documentNote.originFileUri) { File(documentNote.originFileUri) }

    DisposableEffect(file) {
        try {
            val renderer = PdfPageRenderer(file)
            pdfRenderer = renderer
            openErrorMessage = null
        } catch (e: PdfPasswordException) {
            openErrorMessage = "This document is password-protected and cannot be opened."
        } catch (e: PdfCorruptException) {
            openErrorMessage = "This document is corrupt or invalid: ${e.message}"
        } catch (e: Exception) {
            openErrorMessage = "Failed to open document: ${e.message}"
        }

        onDispose {
            pdfRenderer?.close()
            pdfRenderer = null
        }
    }

    val totalPages = pdfRenderer?.pageCount ?: pages.size
    val subtitleText = if (totalPages == 1) "1 page" else "$totalPages pages"

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = initialPageIndex.coerceIn(0, (totalPages - 1).coerceAtLeast(0))
    )
    val zoomState = remember { PdfViewportZoomState() }

    var hasScrolledToInitialPage by remember { mutableStateOf(false) }

    LaunchedEffect(initialPageIndex, totalPages) {
        if (!hasScrolledToInitialPage && totalPages > 0 && initialPageIndex in 0 until totalPages) {
            hasScrolledToInitialPage = true
            listState.scrollToItem(initialPageIndex)
        }
    }

    val highlightAlpha = remember { Animatable(0f) }
    LaunchedEffect(highlightPageIndex) {
        if (highlightPageIndex != null) {
            highlightAlpha.snapTo(1f)
            delay(2000L)
            highlightAlpha.animateTo(0f, tween(1000, easing = FastOutSlowInEasing))
        }
    }

    LaunchedEffect(zoomState.isZoomed) {
        if (!zoomState.isZoomed) {
            pdfRenderer?.evictHighResCache()
        }
    }

    BackHandler(enabled = zoomState.isZoomed || isReadingMode) {
        if (zoomState.isZoomed) {
            coroutineScope.launch { zoomState.animateReset() }
        } else if (isReadingMode) {
            isReadingMode = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenNavy)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        TopAppBar(
            title = {
                Column(verticalArrangement = Arrangement.Center) {
                    Text(
                        text = documentNote.name,
                        color = TabCream,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (currentScheduledAt != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        NoteDetailScheduleChip(
                            scheduledAt = currentScheduledAt,
                            alertType = currentAlertType,
                            scheduleTitle = currentScheduleTitle,
                            onClick = { showScheduleDialog = true }
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = {
                    if (zoomState.isZoomed) {
                        coroutineScope.launch { zoomState.animateReset() }
                    } else if (isReadingMode) {
                        isReadingMode = false
                    } else {
                        onBack()
                    }
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TabCream
                    )
                }
            },
            actions = {
                // Book icon: Reading mode toggle
                IconButton(
                    onClick = {
                        isReadingMode = !isReadingMode
                        if (!isReadingMode && zoomState.isZoomed) {
                            coroutineScope.launch { zoomState.animateReset() }
                        } else if (isReadingMode) {
                            zoomState.reset()
                        }
                    },
                    enabled = pdfRenderer != null
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoStories,
                        contentDescription = stringResource(
                            if (isReadingMode) R.string.viewer_action_reading_mode_exit else R.string.viewer_action_reading_mode_enter
                        ),
                        tint = if (isReadingMode) AccentGold else TabCream
                    )
                }

                // Split to Images
                IconButton(onClick = { showSplitConfirmDialog = true }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.CallSplit,
                        contentDescription = "Split to Images",
                        tint = TabCream
                    )
                }

                // Share
                IconButton(onClick = onShare) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share As",
                        tint = TabCream
                    )
                }

                // Delete
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Document",
                        tint = DangerRed
                    )
                }

                // Overflow menu for Info & Schedule...
                Box {
                    var showOverflowMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showOverflowMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More Options",
                            tint = TabCream
                        )
                    }
                    DropdownMenu(
                        expanded = showOverflowMenu,
                        onDismissRequest = { showOverflowMenu = false },
                        modifier = Modifier
                            .background(CardBg)
                            .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                    ) {
                        // Info Section
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.viewer_info_header),
                                color = TabCream.copy(alpha = 0.5f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            if (zoomState.isZoomed) {
                                Text(
                                    text = stringResource(R.string.viewer_info_zoomed),
                                    color = AccentGold,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            } else if (isReadingMode) {
                                Text(
                                    text = stringResource(R.string.viewer_info_pinch_or_double_tap),
                                    color = TabCream.copy(alpha = 0.85f),
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = stringResource(R.string.viewer_info_offline),
                                    color = TabCream.copy(alpha = 0.6f),
                                    fontSize = 12.sp
                                )
                            } else {
                                Text(
                                    text = stringResource(R.string.viewer_info_reading_mode_available),
                                    color = TabCream.copy(alpha = 0.85f),
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = stringResource(R.string.viewer_info_offline),
                                    color = TabCream.copy(alpha = 0.6f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                        HorizontalDivider(
                            color = BorderColor,
                            thickness = 1.dp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        DropdownMenuItem(
                            text = { Text("Schedule...", color = TabCream) },
                            leadingIcon = { Icon(Icons.Default.Alarm, null, tint = AccentGold) },
                            onClick = {
                                showOverflowMenu = false
                                showScheduleDialog = true
                            }
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = ScreenNavy)
        )

        val err = openErrorMessage
        val renderer = pdfRenderer

        when {
            err != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(60.dp))
                    Icon(
                        imageVector = if (err.contains("password", ignoreCase = true)) Icons.Default.Lock else Icons.Default.Warning,
                        contentDescription = null,
                        tint = DangerRed,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Cannot Open PDF",
                        color = TabCream,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = err,
                        color = TabCream.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(containerColor = CardBg)
                    ) {
                        Text("Go Back", color = TabCream)
                    }
                }
            }
            renderer == null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentGold)
                }
            }
            else -> {
                val totalCount = renderer.pageCount
                val configuration = LocalConfiguration.current
                val density = LocalDensity.current
                val screenWidthPx = with(density) { (configuration.screenWidthDp.dp - 32.dp).roundToPx() }

                val zoomModifier = if (isReadingMode) {
                    Modifier
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = { tapOffset ->
                                    coroutineScope.launch {
                                        val viewW = size.width.toFloat()
                                        val viewH = size.height.toFloat()
                                        zoomState.updateViewport(viewW, viewH)
                                        zoomState.animateDoubleTap(tapOffset.x, tapOffset.y)
                                    }
                                }
                            )
                        }
                        .pointerInput(Unit) {
                            val viewW = size.width.toFloat()
                            val viewH = size.height.toFloat()
                            zoomState.updateViewport(viewW, viewH)

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
                                            zoomState.onPinch(
                                                zoomChange = zoom,
                                                panChangeX = pan.x,
                                                panChangeY = pan.y,
                                                centroidX = centroid.x,
                                                centroidY = centroid.y
                                            )
                                            event.changes.forEach { it.consume() }
                                        }
                                    } else if (pressedCount == 1 && zoomState.isZoomed) {
                                        val pan = event.calculatePan()
                                        if (pan != Offset.Zero) {
                                            zoomState.onPan(pan.x, pan.y)
                                            event.changes.forEach { it.consume() }
                                        }
                                    }
                                } while (event.changes.any { it.pressed })
                            }
                        }
                } else {
                    Modifier
                }

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .clipToBounds()
                        .then(zoomModifier)
                ) {
                    LaunchedEffect(maxWidth, maxHeight) {
                        zoomState.updateViewport(
                            with(density) { maxWidth.toPx() },
                            with(density) { maxHeight.toPx() }
                        )
                    }

                    LazyColumn(
                        state = listState,
                        userScrollEnabled = !zoomState.isZoomed,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = zoomState.scale
                                scaleY = zoomState.scale
                                translationX = zoomState.panX
                                translationY = zoomState.panY
                            },
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        items(totalCount, key = { index -> "${documentNote.id}_page_$index" }) { pageIndex ->
                            val placeholderPage = pages.getOrNull(pageIndex)
                            VirtualizedPdfPageView(
                                pageIndex = pageIndex,
                                totalPages = totalCount,
                                renderer = renderer,
                                targetWidthPx = screenWidthPx,
                                placeholderUri = placeholderPage?.imageUri,
                                isZoomed = zoomState.isZoomed,
                                currentZoomScale = zoomState.scale,
                                isHighlighted = pageIndex == highlightPageIndex && highlightAlpha.value > 0.01f,
                                highlightAlpha = highlightAlpha.value
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }

    if (showSplitConfirmDialog) {
        val totalCount = pdfRenderer?.pageCount ?: pages.size
        AlertDialog(
            onDismissRequest = { showSplitConfirmDialog = false },
            title = {
                Text(
                    text = "Split PDF into Images?",
                    fontWeight = FontWeight.Bold,
                    color = TabCream
                )
            },
            text = {
                Text(
                    text = PdfSplitManager.getConfirmationDescription(documentNote.name, totalCount),
                    color = TabCream.copy(alpha = 0.85f),
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSplitConfirmDialog = false
                        onSplitToImages()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("Split & Delete PDF", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSplitConfirmDialog = false }) {
                    Text("Cancel", color = TabCream)
                }
            },
            containerColor = CardBg,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showScheduleDialog) {
        val scheduleManager = remember { NoteScheduleManager(context) }
        ScheduleNoteDialog(
            noteTitle = documentNote.name,
            initialScheduledAt = currentScheduledAt,
            initialAlertType = try {
                ScheduleAlertType.valueOf(currentAlertType ?: "NOTIFICATION")
            } catch (_: Exception) {
                ScheduleAlertType.NOTIFICATION
            },
            initialScheduleTitle = currentScheduleTitle,
            onDismiss = { showScheduleDialog = false },
            onSaveSchedule = { scheduledAt, alertType, scheduleTitle ->
                scheduleManager.scheduleNote(
                    noteType = ScheduleNoteType.DOCUMENT,
                    noteId = documentNote.id,
                    folderId = documentNote.folderId,
                    title = documentNote.name,
                    triggerAtMillis = scheduledAt,
                    alertType = alertType,
                    scheduleTitle = scheduleTitle
                )
                currentScheduledAt = scheduledAt
                currentAlertType = alertType.name
                currentScheduleTitle = scheduleTitle
                showScheduleDialog = false
            },
            onClearSchedule = {
                scheduleManager.cancelSchedule(ScheduleNoteType.DOCUMENT, documentNote.id)
                currentScheduledAt = null
                currentAlertType = null
                currentScheduleTitle = null
                showScheduleDialog = false
            }
        )
    }
}

@Composable
private fun VirtualizedPdfPageView(
    pageIndex: Int,
    totalPages: Int,
    renderer: PdfPageRenderer,
    targetWidthPx: Int,
    placeholderUri: String?,
    isZoomed: Boolean = false,
    currentZoomScale: Float = 1.0f,
    isHighlighted: Boolean = false,
    highlightAlpha: Float = 0f
) {
    val coroutineScope = rememberCoroutineScope()
    var baseBitmap by remember(pageIndex) { mutableStateOf<Bitmap?>(null) }
    var highResBitmap by remember(pageIndex) { mutableStateOf<Bitmap?>(null) }
    var renderError by remember(pageIndex) { mutableStateOf(false) }
    var retryCount by remember(pageIndex) { mutableIntStateOf(0) }

    // Read true aspect ratio synchronously from renderer cache (no default 0.707f race)
    val aspectRatio = remember(pageIndex) { renderer.getPageAspectRatio(pageIndex) }

    // Prefetch adjacent pages in background without blocking UI
    LaunchedEffect(pageIndex, targetWidthPx) {
        val targetHeightPx = PdfLayoutMath.computeTargetSize(targetWidthPx, aspectRatio).heightPx
        coroutineScope.launch {
            if (pageIndex > 0) renderer.prefetchPage(pageIndex - 1, targetWidthPx, targetHeightPx)
            if (pageIndex < totalPages - 1) renderer.prefetchPage(pageIndex + 1, targetWidthPx, targetHeightPx)
        }
    }

    // 1. Base resolution render (1.0x display density)
    LaunchedEffect(pageIndex, targetWidthPx, retryCount) {
        val targetHeightPx = PdfLayoutMath.computeTargetSize(targetWidthPx, aspectRatio).heightPx
        renderError = false
        val rendered = renderer.renderPage(
            pageIndex = pageIndex,
            destWidth = targetWidthPx,
            destHeight = targetHeightPx,
            renderScale = 1.0f
        )
        if (rendered != null) {
            baseBitmap = rendered
        } else {
            renderError = true
        }
    }

    // 2. High-resolution on-demand render when list is zoomed (scales dynamically with zoom up to 4.0x)
    LaunchedEffect(pageIndex, targetWidthPx, isZoomed, currentZoomScale, retryCount) {
        if (isZoomed && currentZoomScale > 1.05f) {
            delay(150) // Debounce rapid pinch operations
            val targetHeightPx = PdfLayoutMath.computeTargetSize(targetWidthPx, aspectRatio).heightPx
            val sharpRender = renderer.renderPage(
                pageIndex = pageIndex,
                destWidth = targetWidthPx,
                destHeight = targetHeightPx,
                renderScale = currentZoomScale.coerceIn(1.0f, 4.0f)
            )
            if (sharpRender != null) {
                highResBitmap = sharpRender
            }
        } else {
            highResBitmap = null
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isHighlighted && highlightAlpha > 0.01f) {
                    Modifier.border(
                        width = 2.5.dp,
                        color = TagAmber.copy(alpha = highlightAlpha),
                        shape = RoundedCornerShape(12.dp)
                    )
                } else Modifier
            )
            .clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Page ${pageIndex + 1} of $totalPages",
                    color = TabCream.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.weight(1f))
                if (isZoomed && highResBitmap == null && !renderError) {
                    Text(
                        text = "Sharpening…",
                        color = TabCream.copy(alpha = 0.5f),
                        fontSize = 10.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspectRatio)
                    .background(Color.White)
                    .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)),
                contentAlignment = Alignment.Center
            ) {
                val currentBitmap = highResBitmap ?: baseBitmap
                if (isHighlighted && highlightAlpha > 0.01f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(TagAmber.copy(alpha = highlightAlpha * 0.18f))
                    )
                }
                when {
                    renderError && currentBitmap == null -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = DangerRed,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Failed to render page",
                                color = CardBg,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { retryCount++ },
                                colors = ButtonDefaults.buttonColors(containerColor = CardBg)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = TabCream,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Retry", color = TabCream, fontSize = 11.sp)
                            }
                        }
                    }
                    currentBitmap != null && !currentBitmap.isRecycled -> {
                        Image(
                            bitmap = currentBitmap.asImageBitmap(),
                            contentDescription = "Page ${pageIndex + 1}",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    placeholderUri != null && File(placeholderUri).exists() -> {
                        AsyncImage(
                            model = File(placeholderUri),
                            contentDescription = "Page ${pageIndex + 1} Preview",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    else -> {
                        CircularProgressIndicator(
                            color = AccentGold,
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            }
        }
    }
}
