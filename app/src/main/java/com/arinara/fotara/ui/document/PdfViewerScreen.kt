// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.document

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.platform.LocalContext
import com.arinara.fotara.ui.components.NoteDetailScheduleChip
import com.arinara.fotara.ui.components.ScheduleNoteDialog
import com.arinara.fotara.util.NoteScheduleManager
import com.arinara.fotara.util.ScheduleNoteType
import com.arinara.fotara.util.ScheduleAlertType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.arinara.fotara.data.model.DocumentNote
import com.arinara.fotara.data.model.DocumentPage
import com.arinara.fotara.util.PdfCorruptException
import com.arinara.fotara.util.PdfPageRenderer
import com.arinara.fotara.util.PdfPasswordException
import com.arinara.fotara.util.PdfSplitManager
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
    onDelete: () -> Unit
) {
    var showSplitConfirmDialog by remember { mutableStateOf(false) }
    var pdfRenderer by remember { mutableStateOf<PdfPageRenderer?>(null) }
    var openErrorMessage by remember { mutableStateOf<String?>(null) }
    var currentScheduledAt by remember { mutableStateOf(documentNote.scheduledAt) }
    var currentAlertType by remember { mutableStateOf(documentNote.alertType) }
    var currentScheduleTitle by remember { mutableStateOf(documentNote.scheduleTitle) }
    var showScheduleDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenNavy)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = documentNote.name,
                        color = TabCream,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitleText,
                        color = TabCream.copy(alpha = 0.7f),
                        fontSize = 12.sp
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
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TabCream
                    )
                }
            },
            actions = {
                IconButton(onClick = { showSplitConfirmDialog = true }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.CallSplit,
                        contentDescription = "Split to Images",
                        tint = AccentGold
                    )
                }
                IconButton(onClick = onShare) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share As",
                        tint = TabCream
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Document",
                        tint = DangerRed
                    )
                }

                // Overflow menu for Schedule...
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

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(totalCount, key = { index -> "${documentNote.id}_page_$index" }) { pageIndex ->
                        val placeholderPage = pages.getOrNull(pageIndex)
                        VirtualizedPdfPageView(
                            pageIndex = pageIndex,
                            totalPages = totalCount,
                            renderer = renderer,
                            targetWidthPx = screenWidthPx,
                            placeholderUri = placeholderPage?.imageUri
                        )
                        Spacer(modifier = Modifier.height(16.dp))
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
    placeholderUri: String?
) {
    val coroutineScope = rememberCoroutineScope()
    val zoomState = remember { PdfPageZoomState() }

    var baseBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var highResBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var aspectRatio by remember { mutableFloatStateOf(0.707f) }
    var isRenderingSharp by remember { mutableStateOf(false) }

    // Read aspect ratio from cache or renderer
    LaunchedEffect(pageIndex) {
        aspectRatio = renderer.getPageAspectRatio(pageIndex)
    }

    // Prefetch adjacent pages in background without blocking UI
    LaunchedEffect(pageIndex, targetWidthPx) {
        val targetHeightPx = (targetWidthPx / aspectRatio).toInt().coerceAtLeast(100)
        coroutineScope.launch {
            if (pageIndex > 0) renderer.prefetchPage(pageIndex - 1, targetWidthPx, targetHeightPx)
            if (pageIndex < totalPages - 1) renderer.prefetchPage(pageIndex + 1, targetWidthPx, targetHeightPx)
        }
    }

    // 1. Base resolution render (1.0x display density)
    LaunchedEffect(pageIndex, targetWidthPx) {
        val targetHeightPx = (targetWidthPx / aspectRatio).toInt().coerceAtLeast(100)
        val rendered = renderer.renderPage(
            pageIndex = pageIndex,
            destWidth = targetWidthPx,
            destHeight = targetHeightPx,
            renderScale = 1.0f
        )
        if (rendered != null) {
            baseBitmap = rendered
        }
    }

    // 2. High-resolution on-demand render when zoomed in
    LaunchedEffect(pageIndex, targetWidthPx, zoomState.isZoomed) {
        if (zoomState.isZoomed) {
            isRenderingSharp = true
            delay(150) // Debounce rapid pinch operations
            val targetHeightPx = (targetWidthPx / aspectRatio).toInt().coerceAtLeast(100)
            val sharpRender = renderer.renderPage(
                pageIndex = pageIndex,
                destWidth = targetWidthPx,
                destHeight = targetHeightPx,
                renderScale = 2.0f
            )
            if (sharpRender != null) {
                highResBitmap = sharpRender
            }
            isRenderingSharp = false
        } else {
            // Free high-res bitmap immediately when returning to 1.0x to conserve RAM
            highResBitmap = null
            isRenderingSharp = false
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
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
                if (isRenderingSharp && highResBitmap == null) {
                    Text(
                        text = "Rendering…",
                        color = TabCream.copy(alpha = 0.5f),
                        fontSize = 10.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                if (zoomState.scale > 1.05f) {
                    Text(
                        text = "${(zoomState.scale * 100).toInt()}%",
                        color = AccentGold,
                        fontSize = 11.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspectRatio)
                    .background(Color.White)
                    .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                    // Double-tap detector
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = { tapOffset ->
                                val viewW = size.width.toFloat()
                                val viewH = size.height.toFloat()
                                zoomState.onDoubleTap(tapOffset, viewW, viewH)
                            }
                        )
                    }
                    // Zoom and Pan gesture detector with native scroll pass-through at 1.0x
                    .pointerInput(zoomState.scale) {
                        val viewW = size.width.toFloat()
                        val viewH = size.height.toFloat()

                        if (zoomState.isZoomed) {
                            // Zoomed in: consume both two-axis pan and pinch
                            detectTransformGestures { _, pan, zoom, _ ->
                                zoomState.onPinch(zoom, pan, viewW, viewH)
                            }
                        } else {
                            // At 1.0x scale: ONLY detect two-finger pinch.
                            // Single-finger vertical dragging passes freely to LazyColumn for smooth scrolling.
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                do {
                                    val event = awaitPointerEvent()
                                    if (event.changes.size >= 2) {
                                        val zoom = event.calculateZoom()
                                        val pan = event.calculatePan()
                                        if (zoom > 1.02f) {
                                            zoomState.onPinch(zoom, pan, viewW, viewH)
                                            event.changes.forEach { it.consume() }
                                        }
                                    }
                                } while (event.changes.any { it.pressed })
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                val currentBitmap = highResBitmap ?: baseBitmap
                if (currentBitmap != null && !currentBitmap.isRecycled) {
                    Image(
                        bitmap = currentBitmap.asImageBitmap(),
                        contentDescription = "Page ${pageIndex + 1}",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = zoomState.scale,
                                scaleY = zoomState.scale,
                                translationX = zoomState.offsetX,
                                translationY = zoomState.offsetY
                            )
                    )
                } else if (placeholderUri != null && File(placeholderUri).exists()) {
                    AsyncImage(
                        model = File(placeholderUri),
                        contentDescription = "Page ${pageIndex + 1} Preview",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = zoomState.scale,
                                scaleY = zoomState.scale,
                                translationX = zoomState.offsetX,
                                translationY = zoomState.offsetY
                            )
                    )
                } else {
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
