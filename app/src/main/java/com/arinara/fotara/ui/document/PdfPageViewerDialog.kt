// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.document

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.arinara.fotara.data.model.DocumentNote
import com.arinara.fotara.data.model.DocumentPage
import com.arinara.fotara.theme.FolderBodyBlue
import com.arinara.fotara.theme.FolderTabCream
import com.arinara.fotara.theme.MidnightCardOutline
import com.arinara.fotara.theme.MidnightNavy
import com.arinara.fotara.theme.MidnightSurface
import com.arinara.fotara.theme.TagAmber
import com.arinara.fotara.theme.TagCrimson
import com.arinara.fotara.theme.TextMuted
import com.arinara.fotara.theme.TextPrimary
import com.arinara.fotara.theme.TextSecondary
import com.arinara.fotara.ui.components.ZoomableBox
import com.arinara.fotara.util.PdfLayoutMath
import com.arinara.fotara.util.PdfPageRenderer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Fullscreen photo-style Page Viewer for PDF documents.
 * Reuses [ZoomableBox] to ensure 100% gesture parity with the photo viewer:
 * - Zoom-gated horizontal swipe paging at 1.0x
 * - Free two-axis pan when zoomed
 * - Double-tap to zoom (2.5x) and reset
 * - Density-aware sharpening on zoom
 * - Read-only collapsible OCR bottom sheet
 */
@Composable
fun PdfPageViewerDialog(
    documentNote: DocumentNote,
    pages: List<DocumentPage>,
    renderer: PdfPageRenderer,
    initialPageIndex: Int,
    onDismiss: (lastPageIndex: Int) -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val totalCount = renderer.pageCount.coerceAtLeast(1)
    val safeInitialPage = initialPageIndex.coerceIn(0, totalCount - 1)

    val pagerState = rememberPagerState(
        initialPage = safeInitialPage,
        pageCount = { totalCount }
    )

    var isCurrentPageZoomed by remember { mutableStateOf(false) }
    var isOcrExpanded by remember { mutableStateOf(false) }
    var copyMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(pagerState.currentPage) {
        isCurrentPageZoomed = false
        copyMessage = null
    }

    Dialog(
        onDismissRequest = { onDismiss(pagerState.currentPage) },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        BackHandler {
            onDismiss(pagerState.currentPage)
        }

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MidnightNavy)
        ) {
            // Main Pager with Zoom-Gated Swipe
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = !isCurrentPageZoomed,
                modifier = Modifier.fillMaxSize()
            ) { pageIdx ->
                PdfPageView(
                    pageIndex = pageIdx,
                    totalPages = totalCount,
                    renderer = renderer,
                    onZoomChanged = { isZoomed ->
                        if (pagerState.currentPage == pageIdx) {
                            isCurrentPageZoomed = isZoomed
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Top Bar
            Surface(
                color = MidnightNavy.copy(alpha = 0.92f),
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
                    IconButton(onClick = { onDismiss(pagerState.currentPage) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Close Page Viewer",
                            tint = FolderTabCream
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 6.dp)
                    ) {
                        Text(
                            text = documentNote.name,
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isCurrentPageZoomed) {
                                "Zoomed: Pan enabled · Double-tap to reset"
                            } else {
                                "Page ${pagerState.currentPage + 1} of $totalCount"
                            },
                            color = if (isCurrentPageZoomed) TagAmber else TextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    // Share Button
                    IconButton(onClick = onShare) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = FolderTabCream
                        )
                    }
                }
            }

            // Expandable Bottom OCR Sheet
            Surface(
                color = MidnightSurface.copy(alpha = 0.96f),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MidnightCardOutline),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Header of OCR drawer
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isOcrExpanded = !isOcrExpanded },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = FolderTabCream,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Recognized OCR Text",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            imageVector = if (isOcrExpanded) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                            contentDescription = if (isOcrExpanded) "Collapse" else "Expand",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    AnimatedVisibility(visible = isOcrExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                        ) {
                            val currentPageDoc = pages.firstOrNull { it.pageIndex == pagerState.currentPage }
                            val pageOcr = currentPageDoc?.ocrText
                            val textToDisplay = if (!pageOcr.isNullOrBlank()) {
                                pageOcr
                            } else {
                                "No OCR text recognized for this page."
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MidnightNavy)
                                    .border(1.dp, MidnightCardOutline, RoundedCornerShape(10.dp))
                                    .padding(10.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = textToDisplay,
                                    color = if (!pageOcr.isNullOrBlank()) TextSecondary else TextMuted,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 17.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (copyMessage != null) {
                                    Text(
                                        text = copyMessage ?: "",
                                        color = FolderTabCream,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                } else {
                                    Spacer(modifier = Modifier.width(1.dp))
                                }

                                Button(
                                    onClick = {
                                        if (!pageOcr.isNullOrBlank()) {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                            val clip = ClipData.newPlainText("Recognized OCR Text", pageOcr)
                                            clipboard?.setPrimaryClip(clip)
                                            copyMessage = "Copied to clipboard!"
                                        }
                                    },
                                    enabled = !pageOcr.isNullOrBlank(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = FolderBodyBlue,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = null,
                                        tint = FolderTabCream,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Copy OCR Text", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Single PDF page view in the Page Viewer dialog.
 * Uses [ZoomableBox] for smooth pinch, two-axis pan, and double-tap zoom.
 */
@Composable
private fun PdfPageView(
    pageIndex: Int,
    totalPages: Int,
    renderer: PdfPageRenderer,
    onZoomChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var baseBitmap by remember(pageIndex) { mutableStateOf<Bitmap?>(null) }
    var highResBitmap by remember(pageIndex) { mutableStateOf<Bitmap?>(null) }
    var renderError by remember(pageIndex) { mutableStateOf(false) }
    var retryCount by remember(pageIndex) { mutableIntStateOf(0) }
    var currentScale by remember(pageIndex) { mutableStateOf(1.0f) }

    val aspectRatio = remember(pageIndex) { renderer.getPageAspectRatio(pageIndex) }

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val density = LocalDensity.current
        val viewWidthPx = with(density) { maxWidth.roundToPx() }
        val viewHeightPx = with(density) { maxHeight.roundToPx() }

        // Fit page dimensions within the available container
        val (targetWidthPx, targetHeightPx) = remember(viewWidthPx, viewHeightPx, aspectRatio) {
            if (viewWidthPx <= 0 || viewHeightPx <= 0) {
                100 to 100
            } else {
                val containerAspect = viewWidthPx.toFloat() / viewHeightPx.toFloat()
                if (aspectRatio > containerAspect) {
                    val w = viewWidthPx
                    val h = (viewWidthPx / aspectRatio).roundToInt().coerceAtLeast(100)
                    w to h
                } else {
                    val h = viewHeightPx
                    val w = (viewHeightPx * aspectRatio).roundToInt().coerceAtLeast(100)
                    w to h
                }
            }
        }

        // Prefetch adjacent pages
        LaunchedEffect(pageIndex, targetWidthPx, targetHeightPx) {
            coroutineScope.launch {
                if (pageIndex > 0) renderer.prefetchPage(pageIndex - 1, targetWidthPx, targetHeightPx)
                if (pageIndex < totalPages - 1) renderer.prefetchPage(pageIndex + 1, targetWidthPx, targetHeightPx)
            }
        }

        // Base 1.0x rendering
        LaunchedEffect(pageIndex, targetWidthPx, targetHeightPx, retryCount) {
            if (targetWidthPx > 0 && targetHeightPx > 0) {
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
        }

        // High-resolution sharpen on zoom
        LaunchedEffect(pageIndex, targetWidthPx, targetHeightPx, currentScale > 1.2f, retryCount) {
            if (currentScale > 1.2f && targetWidthPx > 0 && targetHeightPx > 0) {
                delay(150) // Debounce rapid pinch operations
                val sharpRender = renderer.renderPage(
                    pageIndex = pageIndex,
                    destWidth = targetWidthPx,
                    destHeight = targetHeightPx,
                    renderScale = 2.0f
                )
                if (sharpRender != null) {
                    highResBitmap = sharpRender
                }
            } else {
                highResBitmap = null
            }
        }

        ZoomableBox(
            modifier = Modifier.fillMaxSize(),
            maxScale = 4.0f,
            doubleTapScale = 2.5f,
            onZoomChanged = onZoomChanged
        ) { scale ->
            currentScale = scale

            val currentBitmap = highResBitmap ?: baseBitmap
            when {
                renderError && currentBitmap == null -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = TagCrimson,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Failed to render page ${pageIndex + 1}",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { retryCount++ },
                            colors = ButtonDefaults.buttonColors(containerColor = FolderBodyBlue)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retry", fontSize = 12.sp)
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
                else -> {
                    CircularProgressIndicator(
                        color = TagAmber,
                        modifier = Modifier.size(36.dp),
                        strokeWidth = 2.5.dp
                    )
                }
            }
        }
    }
}
